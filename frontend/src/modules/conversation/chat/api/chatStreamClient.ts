import { ApiError } from "../../../../shared/api/ApiError";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { streamEventSchema } from "../schemas/streamEventSchemas";
import type { ChatStreamHandlers, StreamEvent } from "../types/chatTypes";

/**
 * Reads the model response stream.
 *
 * <p>Axios cannot expose an incremental response body, so streaming uses fetch directly. The session
 * stays cookie-based: credentials are included and the server-issued CSRF token is echoed back, with
 * no bearer token constructed anywhere in the browser.
 */
const CSRF_COOKIE = "XSRF-TOKEN";
const FRAME_SEPARATOR = /\r?\n\r?\n/;

const csrfToken = (): string => {
  const entry = document.cookie.split("; ").find((value) => value.startsWith(`${CSRF_COOKIE}=`));
  return entry ? decodeURIComponent(entry.slice(CSRF_COOKIE.length + 1)) : "";
};

const parseFrame = (frame: string): StreamEvent | null => {
  let name = "";
  const payload: string[] = [];

  for (const line of frame.split(/\r?\n/)) {
    if (line.startsWith("event:")) name = line.slice(6).trim();
    else if (line.startsWith("data:")) payload.push(line.slice(5).trim());
  }
  if (!name || payload.length === 0) return null;

  const parsed = streamEventSchema.safeParse({ event: name, data: JSON.parse(payload.join("\n")) });
  return parsed.success ? parsed.data : null;
};

const dispatch = (event: StreamEvent, handlers: ChatStreamHandlers): void => {
  if (event.event === "started") handlers.onStarted(event.data);
  else if (event.event === "thinking") handlers.onThinking(event.data);
  else if (event.event === "token") handlers.onToken(event.data);
  else if (event.event === "usage") handlers.onUsage(event.data);
  else if (event.event === "completed") handlers.onCompleted(event.data);
  else if (event.event === "cancelled") handlers.onCancelled(event.data);
  else handlers.onError(event.data);
};

const readFrames = (response: Response, handlers: ChatStreamHandlers): Promise<void> => {
  const reader = response.body?.getReader();
  if (!reader) return Promise.reject(new ApiError(500, "This browser cannot read a model stream"));

  const decoder = new TextDecoder();
  let buffer = "";

  const pump = (): Promise<void> => reader.read().then(({ done, value }) => {
    if (done) return undefined;

    buffer += decoder.decode(value, { stream: true });
    const frames = buffer.split(FRAME_SEPARATOR);
    buffer = frames.pop() ?? "";

    frames.forEach((frame: string) => {
      const event = parseFrame(frame);
      if (event) dispatch(event, handlers);
    });

    return pump();
  });

  return pump();
};

const rejectWithBackendError = (response: Response): Promise<never> =>
  response.json()
    .then((body: BaseResponse<never>) => Promise.reject(
      new ApiError(body.code ?? response.status, body.message ?? "Nexo IA could not start this request",
        response.status)))
    .catch((error: unknown) => Promise.reject(error instanceof ApiError
      ? error
      : new ApiError(response.status, "Nexo IA could not start this request", response.status)));

export const streamMessage = (
  conversationId: string,
  content: string,
  thinkingEnabled: boolean,
  handlers: ChatStreamHandlers,
  signal: AbortSignal
): Promise<void> =>
  fetch(`/api/v1/conversations/${conversationId}/messages/stream`, {
    method: "POST",
    credentials: "include",
    signal,
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream, application/json",
      "X-XSRF-TOKEN": csrfToken()
    },
    body: JSON.stringify({ content, thinkingEnabled })
  }).then((response: Response) => response.ok
    ? readFrames(response, handlers)
    : rejectWithBackendError(response));
