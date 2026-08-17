export type ProfileAvatarProps = { name: string };
export type ProfileAvatarResult = {
  avatarUrl: string;
  hasImage: boolean;
  isUploading: boolean;
  initials: string;
  upload: (file: File) => void;
  markLoaded: () => void;
  markMissing: () => void;
};
