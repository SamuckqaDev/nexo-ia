export class ApiError extends Error {
  constructor(
    public readonly code: number,
    message: string,
    public readonly status?: number,
    cause?: unknown
  ) {
    super(message, { cause });
    this.name = "ApiError";
  }
}
