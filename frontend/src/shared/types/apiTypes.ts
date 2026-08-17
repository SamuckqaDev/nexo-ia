import type { InternalAxiosRequestConfig } from "axios";

export type BaseResponse<T> = {
  code: number;
  message: string;
  data: T[] | null;
};

export type RetriableRequest = InternalAxiosRequestConfig & {
  _nexoRefreshAttempted?: boolean;
};
