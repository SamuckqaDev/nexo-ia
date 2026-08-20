import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../app/styles/theme";
import type { ThemeState } from "../../../../app/types/themeTypes";
import type { AuthenticatedUser } from "../../../auth/types/authTypes";
import { usePreferenceStore } from "../../stores/usePreferenceStore";
import { SettingsPage } from "./index";

vi.mock("../../../../app/stores/useThemeStore", () => ({
  useThemeStore: (selector: (state: ThemeState) => unknown): unknown => selector({
    mode: "dark",
    setMode: vi.fn(),
    toggle: vi.fn()
  })
}));

const user: AuthenticatedUser = {
  id: "ca2bce9b-7f74-44c7-99b0-b1c55da54397",
  username: "owner",
  email: "owner@nexo.local",
  name: "Owner",
  birthDate: null,
  createdAt: "2026-08-20T12:00:00Z",
  role: "OWNER"
};

describe("SettingsPage preferences", () => {
  beforeEach((): void => {
    usePreferenceStore.persist.setOptions({
      storage: {
        getItem: (): null => null,
        setItem: vi.fn(),
        removeItem: vi.fn()
      }
    });
    usePreferenceStore.setState({ language: "en", thinkingEnabled: false });
  });

  it("keeps model Thinking off by default and lets the user enable it", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <SettingsPage user={user} section="preferences" onSectionChange={vi.fn()} />
      </ThemeProvider>
    );

    const thinkingSwitch = screen.getByRole("switch", { name: /enable model thinking/i });
    expect(thinkingSwitch).toHaveAttribute("aria-checked", "false");
    expect(screen.getByText(/never saved or sent back in later turns/i)).toBeInTheDocument();

    fireEvent.click(thinkingSwitch);

    expect(thinkingSwitch).toHaveAttribute("aria-checked", "true");
    expect(usePreferenceStore.getState().thinkingEnabled).toBe(true);
  });
});
