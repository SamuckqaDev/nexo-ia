import axios from "axios";
import type { AxiosInstance, AxiosResponse } from "axios";
import type { BaseResponse, RetriableRequest } from "../types/apiTypes";
import { ApiError } from "./ApiError";
import { useSessionExpiredStore } from "../auth/sessionExpiredStore";

export const apiClient: AxiosInstance = axios.create({
  baseURL: "/api/v1",
  timeout: 30_000,
  withCredentials: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: {
    Accept: "application/json"
  }
});

const frontLog = (message: string, details?: unknown): void => {
  if (import.meta.env.DEV) console.info(`[NEXO-FRONT] ${message}`, details ?? "");
};

apiClient.interceptors.request.use((request) => {
  frontLog(`HTTP → ${(request.method ?? "GET").toUpperCase()} ${request.url ?? ""}`);
  return request;
});

const refreshClient: AxiosInstance = axios.create({
  baseURL: "/api/v1",
  timeout: 30_000,
  withCredentials: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN"
});

let refreshInFlight: Promise<void> | null = null;

const isRefreshableRequest = (request?: RetriableRequest): request is RetriableRequest => {
  if (!request || request._nexoRefreshAttempted) return false;
  return !["/auth/login", "/auth/refresh", "/auth/bootstrap", "/auth/csrf", "/auth/me"]
    .some((path) => request.url?.startsWith(path));
};

const refreshTokens = (): Promise<void> => {
  if (!refreshInFlight) {
    refreshInFlight = refreshClient.post("/auth/refresh")
      .then(() => undefined)
      .finally(() => { refreshInFlight = null; });
  }
  return refreshInFlight;
};

/** Shares one refresh operation across REST calls and long-lived chat stream requests. */
export const refreshAuthenticatedSession = (): Promise<void> => refreshTokens()
  .catch((error: unknown) => {
    useSessionExpiredStore.getState().open();
    return Promise.reject(error);
  });

apiClient.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    frontLog(`HTTP ← ${response.status} ${response.config.url ?? ""}`);
    return response;
  },
  (error: unknown) => {
    if (!axios.isAxiosError<BaseResponse<never>>(error)) {
      return Promise.reject(new ApiError(500, "An unexpected client error occurred", undefined, error));
    }

    const status = error.response?.status;
    const response = error.response?.data;
    const request = error.config as RetriableRequest | undefined;
    const normalizedError = new ApiError(
      response?.code ?? status ?? 0,
      response?.message ?? "Nexo IA is currently unavailable",
      status,
      error
    );

    frontLog(`HTTP ✕ ${status ?? "NETWORK"} ${request?.url ?? ""}`, normalizedError.message);

    if (status === 401 && isRefreshableRequest(request)) {
      request._nexoRefreshAttempted = true;
      return refreshAuthenticatedSession().then(
        () => apiClient.request(request),
        () => Promise.reject(normalizedError)
      );
    }

    if (status === 401 && request?._nexoRefreshAttempted) {
      useSessionExpiredStore.getState().open();
    }

    return Promise.reject(normalizedError);
  }
);
