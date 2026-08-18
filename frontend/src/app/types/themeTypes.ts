export type ThemeMode = "dark" | "light";
export type NexoTheme = {
  typography: { family: string };
  colors: Record<"background" | "backgroundSoft" | "backgroundElevated" | "surface" | "surfaceStrong" | "surfaceAccent" | "primary" | "primarySoft" | "accent" | "accentSoft" | "text" | "textMuted" | "textSubtle" | "line" | "lineStrong" | "danger" | "dangerSurface" | "statusOnline" | "statusOffline" | "statusOnlineGlow", string>;
  shadow: string;
  spacing: Record<"xs" | "sm" | "md" | "lg" | "xl", string>;
  radius: Record<"control" | "md" | "round", string>;
};
export type ThemeState = { mode: ThemeMode; toggle: () => void; setMode: (mode: ThemeMode) => void };
