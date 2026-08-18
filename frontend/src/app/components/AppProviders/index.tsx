import type { ReactElement } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "styled-components";
import { App } from "../../App";
import { GlobalStyle } from "../../styles/GlobalStyle";
import { darkTheme, lightTheme } from "../../styles/theme";
import { useThemeStore } from "../../stores/useThemeStore";
import type { ThemeState } from "../../types/themeTypes";
import { BrowserRouter } from "react-router-dom";

const queryClient: QueryClient = new QueryClient({ defaultOptions: { queries: { refetchOnWindowFocus: false, retry: 1 } } });
export function AppProviders(): ReactElement {
  const mode: ThemeState["mode"] = useThemeStore((state: ThemeState) => state.mode);
  return <ThemeProvider theme={mode === "dark" ? darkTheme : lightTheme}><GlobalStyle /><BrowserRouter><QueryClientProvider client={queryClient}><App /></QueryClientProvider></BrowserRouter></ThemeProvider>;
}
