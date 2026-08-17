import { apiClient } from "../../../../shared/api/client";
import { ensureCsrf } from "../../shared/api/authApi";

export function uploadProfileAvatar(file: File): Promise<void> {
  const formData = new FormData();
  formData.append("avatar", file);
  return ensureCsrf().then(() => apiClient.put("/auth/profile/avatar", formData)).then(() => undefined);
}

export function removeProfileAvatar(): Promise<void> {
  return ensureCsrf().then(() => apiClient.delete("/auth/profile/avatar")).then(() => undefined);
}
