import type { UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import type { z } from "zod";
import type { sessionSchema } from "../session/schemas/sessionSchema";

export type ActiveSession = z.infer<typeof sessionSchema>;
export type SessionListQuery = UseQueryResult<ActiveSession[], Error>;
export type RevokeSessionMutation = UseMutationResult<void, Error, string>;

export type SessionManagementResult = {
  query: SessionListQuery;
  revokeMutation: RevokeSessionMutation;
  revoke: (sessionId: string) => void;
  revokeOthers: () => void;
  isRevokingOthers: boolean;
};
