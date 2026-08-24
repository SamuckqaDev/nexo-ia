import { ApiError } from "../../../../shared/api/ApiError";
import { refreshAuthenticatedSession } from "../../../../shared/api/client";
import { useSessionExpiredStore } from "../../../../shared/auth/sessionExpiredStore";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { streamEventSchema } from "../schemas/streamEventSchemas";
import type { ChatStreamHandlers, ConversationMode, StreamEvent } from "../types/chatTypes";

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
  else if (event.event === "agent_state") handlers.onAgentState(event.data);
  else if (event.event === "tool_started") handlers.onToolStarted(event.data);
  else if (event.event === "tool_completed") handlers.onToolCompleted(event.data);
  else if (event.event === "plan_updated") handlers.onPlanUpdated(event.data);
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

  const dispatchFrame = (frame: string): void => {
    if (!frame.trim()) return;
    const event = parseFrame(frame);
    if (event) dispatch(event, handlers);
  };

  const pump = (): Promise<void> => reader.read().then(({ done, value }) => {
    if (done) {
      buffer += decoder.decode();
      dispatchFrame(buffer);
      return undefined;
    }

    buffer += decoder.decode(value, { stream: true });
    const frames = buffer.split(FRAME_SEPARATOR);
    buffer = frames.pop() ?? "";

    frames.forEach(dispatchFrame);

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

const requestStream = (
  conversationId: string,
  content: string,
  thinkingEnabled: boolean,
  mode: ConversationMode,
  handlers: ChatStreamHandlers,
  signal: AbortSignal,
  canRefresh: boolean
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
    body: JSON.stringify({ content, thinkingEnabled, mode })
  }).then((response: Response): Promise<void> => {
    if (response.status === 401 && canRefresh) {
      return refreshAuthenticatedSession().then(
        (): Promise<void> => requestStream(
          conversationId, content, thinkingEnabled, mode, handlers, signal, false),
        (): Promise<never> => rejectWithBackendError(response)
      );
    }
    if (response.status === 401) useSessionExpiredStore.getState().open();
    return response.ok ? readFrames(response, handlers) : rejectWithBackendError(response);
  });

export const streamMessage = (
  conversationId: string,
  content: string,
  thinkingEnabled: boolean,
  mode: ConversationMode,
  handlers: ChatStreamHandlers,
  signal: AbortSignal
): Promise<void> => requestStream(
  conversationId, content, thinkingEnabled, mode, handlers, signal, true);
