import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import type { AuthenticatedUser } from "../../types/authTypes";
import { userSchema } from "../../shared/schemas/authSchemas";
import { ensureCsrf } from "../../shared/api/authApi";
import type { UpdateProfileFormValues } from "../schemas/updateProfileSchema";

export function updateProfile(input: UpdateProfileFormValues): Promise<AuthenticatedUser> {
  return ensureCsrf()
    .then(() => apiClient.put<BaseResponse<unknown>>("/auth/profile", input))
    .then(({ data }) => {
      const user: unknown = data.data?.[0];
      if (!user) throw new Error("Nexo returned an empty profile response");
      return userSchema.parse(user);
    });
}
