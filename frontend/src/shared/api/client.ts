import axios from "axios";
import type { AxiosInstance, AxiosResponse } from "axios";
import type { BaseResponse, RetriableRequest } from "../types/apiTypes";
import { ApiError } from "./ApiError";

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
  return !["/auth/login", "/auth/refresh", "/auth/bootstrap", "/auth/csrf"]
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

apiClient.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => response,
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

    if (status === 401 && isRefreshableRequest(request)) {
      request._nexoRefreshAttempted = true;
      return refreshTokens()
        .then(() => apiClient.request(request))
        .catch(() => Promise.reject(normalizedError));
    }

    return Promise.reject(normalizedError);
  }
);
