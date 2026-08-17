import { Camera } from "@phosphor-icons/react";
import type { ChangeEvent, ReactElement } from "react";
import type { ProfileAvatarProps } from "../../../types/profileTypes";
import { useProfileAvatar } from "../../hooks/useProfileAvatar";
import { Image, Input, Overlay, Wrapper } from "./styles";

export function ProfileAvatar({ name }: ProfileAvatarProps): ReactElement {
  const avatar = useProfileAvatar(name);
  const select = (event: ChangeEvent<HTMLInputElement>): void => {
    const file: File | undefined = event.target.files?.[0];
    if (file) avatar.upload(file);
    event.target.value = "";
  };
  return <Wrapper aria-label="Change profile photo" title="Change profile photo">
    {avatar.initials || "N"}
    {avatar.hasImage && <Image src={avatar.avatarUrl} alt="" onLoad={avatar.markLoaded} onError={avatar.markMissing} />}
    <Input type="file" accept="image/png,image/jpeg,image/webp" disabled={avatar.isUploading} onChange={select} />
    <Overlay><Camera size={12} weight="bold" /></Overlay>
  </Wrapper>;
}
