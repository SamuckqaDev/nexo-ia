import type { NexoTheme } from "../types/themeTypes";

const foundation = {
  typography: { family: '"Poppins", ui-sans-serif, system-ui, sans-serif' },
  spacing: { xs: "0.5rem", sm: "0.75rem", md: "1rem", lg: "1.5rem", xl: "2.5rem" },
  radius: { control: "0.8rem", md: "1.2rem", round: "999px" }
};

export const darkTheme: NexoTheme = { ...foundation, colors: {
  background: "#030b21", backgroundSoft: "#06143a", backgroundElevated: "#0a2052",
  surface: "rgba(10, 32, 82, 0.45)", surfaceStrong: "rgba(6, 20, 58, 0.92)", surfaceAccent: "rgba(10, 32, 82, 0.72)",
  primary: "#05cce8", primarySoft: "#53e5f4", accent: "#ff654a", accentSoft: "#ff9a86",
  text: "#f4fbfd", textMuted: "#a9b8c9", textSubtle: "#687b91", line: "rgba(83, 229, 244, 0.16)",
  lineStrong: "rgba(83, 229, 244, 0.34)", danger: "#ff9a86", dangerSurface: "rgba(255, 101, 74, 0.14)",
  statusOnline: "#34d399", statusOffline: "#64748b", statusOnlineGlow: "rgba(52, 211, 153, 0.8)"
}, shadow: "0 24px 80px rgba(0, 0, 0, 0.32)" };

export const lightTheme: NexoTheme = { ...foundation, colors: {
  background: "#eef7fa", backgroundSoft: "#e1f0f5", backgroundElevated: "#d4ebf2",
  surface: "rgba(255, 255, 255, 0.72)", surfaceStrong: "rgba(255, 255, 255, 0.94)", surfaceAccent: "rgba(5, 204, 232, 0.10)",
  primary: "#008ca4", primarySoft: "#00758b", accent: "#df4c34", accentSoft: "#f17661",
  text: "#07152b", textMuted: "#506477", textSubtle: "#718393", line: "rgba(0, 117, 139, 0.14)",
  lineStrong: "rgba(0, 117, 139, 0.30)", danger: "#c63f2d", dangerSurface: "rgba(223, 76, 52, 0.10)",
  statusOnline: "#16865d", statusOffline: "#7a8794", statusOnlineGlow: "rgba(22, 134, 93, 0.35)"
}, shadow: "0 24px 70px rgba(25, 68, 82, 0.15)" };

export const theme: NexoTheme = darkTheme;
