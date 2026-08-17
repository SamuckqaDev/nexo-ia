import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import type { ActiveSession } from "../../types/sessionTypes";
import { ensureCsrf } from "../../shared/api/authApi";
import { sessionListSchema } from "../schemas/sessionSchema";

export function getActiveSessions(): Promise<ActiveSession[]> {
  return apiClient.get<BaseResponse<unknown>>("/auth/sessions")
    .then(({ data }) => sessionListSchema.parse(data.data ?? []));
}

export function revokeSession(sessionId: string): Promise<void> {
  return ensureCsrf()
    .then(() => apiClient.delete(`/auth/sessions/${sessionId}`))
    .then(() => undefined);
}

export function revokeOtherSessions(): Promise<void> {
  return ensureCsrf()
    .then(() => apiClient.post("/auth/sessions/revoke-others"))
    .then(() => undefined);
}
