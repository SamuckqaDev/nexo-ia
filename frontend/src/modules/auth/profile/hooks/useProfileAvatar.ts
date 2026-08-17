import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import type { ProfileAvatarResult } from "../../types/profileTypes";
import { uploadProfileAvatar } from "../api/profileAvatarApi";

export function useProfileAvatar(name: string): ProfileAvatarResult {
  const client: QueryClient = useQueryClient();
  const versionQuery = useQuery({ queryKey: ["auth", "profile", "avatar-version"], queryFn: (): number => 0, initialData: 0, staleTime: Infinity });
  const version: number = versionQuery.data;
  const [hasImage, setHasImage] = useState<boolean>(true);
  useEffect((): void => setHasImage(true), [version]);
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const mutation = useMutation({
    mutationFn: uploadProfileAvatar,
    onSuccess: (): void => { client.setQueryData(["auth", "profile", "avatar-version"], Date.now()); show("Profile photo updated.", { variant: "success" }); },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });
  const initials: string = name.trim().split(/\s+/).slice(0, 2)
    .map((part: string): string => part[0]?.toUpperCase() ?? "").join("");
  return { avatarUrl: `/api/v1/auth/profile/avatar?v=${version}`, hasImage,
    isUploading: mutation.isPending, initials, upload: (file: File): void => mutation.mutate(file),
    markLoaded: (): void => setHasImage(true), markMissing: (): void => setHasImage(false) };
}
