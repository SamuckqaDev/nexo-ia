import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { ensureCsrf } from "../../shared/api/authApi";
import type { CreateMemberValues, ManagedUser, UserStatus } from "../../types/userManagementTypes";
import { managedUserListSchema, managedUserSchema } from "../schemas/userManagementSchema";
import type { ActiveSession } from "../../types/sessionTypes";
import { sessionListSchema } from "../../session/schemas/sessionSchema";

const first = (response: BaseResponse<unknown>): ManagedUser =>
  managedUserSchema.parse(response.data?.[0]);

export function getManagedUsers(): Promise<ManagedUser[]> {
  return apiClient.get<BaseResponse<unknown>>("/admin/users")
    .then(({ data }) => managedUserListSchema.parse(data.data ?? []));
}

export function createMember(input: CreateMemberValues): Promise<ManagedUser> {
  return ensureCsrf().then(() => apiClient.post<BaseResponse<unknown>>("/admin/users", input))
    .then(({ data }) => first(data));
}

export function updateUserStatus(userId: string, status: UserStatus): Promise<ManagedUser> {
  return ensureCsrf()
    .then(() => apiClient.patch<BaseResponse<unknown>>(`/admin/users/${userId}/status`, { status }))
    .then(({ data }) => first(data));
}

export function getMemberSessions(userId: string): Promise<ActiveSession[]> {
  return apiClient.get<BaseResponse<unknown>>(`/admin/users/${userId}/sessions`)
    .then(({ data }) => sessionListSchema.parse(data.data ?? []));
}

export function revokeMemberSession(userId: string, sessionId: string): Promise<void> {
  return ensureCsrf()
    .then(() => apiClient.delete(`/admin/users/${userId}/sessions/${sessionId}`))
    .then(() => undefined);
}
