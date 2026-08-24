import { useMutation, useQuery, useQueryClient, type UseMutationResult, type UseQueryResult } from "@tanstack/react-query";
import { listPersonalMemories, removePersonalMemory } from "../api/personalMemoryApi";
import type { PersonalMemory } from "../types/personalMemoryTypes";

export const personalMemoriesKey = ["personal-memories"] as const;

export type PersonalMemoriesResult = {
  memories: UseQueryResult<PersonalMemory[], Error>;
  remove: UseMutationResult<void, Error, string>;
};

export function usePersonalMemories(enabled = true): PersonalMemoriesResult {
  const queryClient = useQueryClient();
  const memories = useQuery({
    queryKey: personalMemoriesKey,
    queryFn: listPersonalMemories,
    enabled
  });
  const remove = useMutation({
    mutationFn: removePersonalMemory,
    onSuccess: (): void => { queryClient.invalidateQueries({ queryKey: personalMemoriesKey }); }
  });
  return { memories, remove };
}
