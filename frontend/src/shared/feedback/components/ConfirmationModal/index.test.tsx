import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../../../../app/styles/theme";
import { useConfirmationStore } from "../../stores/useConfirmationStore";
import { ConfirmationModal } from ".";

describe("ConfirmationModal", () => {
  beforeEach(() => useConfirmationStore.setState({ request: null, resolver: null }));

  it("resolves a destructive action only after explicit confirmation", async () => {
    const confirmation = useConfirmationStore.getState().ask({
      title: "Revoke this session?",
      message: "This device will immediately lose access.",
      confirmLabel: "Revoke session",
      tone: "danger"
    });

    render(<ThemeProvider theme={theme}><ConfirmationModal /></ThemeProvider>);
    fireEvent.click(screen.getByRole("button", { name: "Revoke session" }));

    await expect(confirmation).resolves.toBe(true);
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  it("cancels the action with Escape", async () => {
    const confirmation = useConfirmationStore.getState().ask({
      title: "Log out?",
      message: "Your session will end.",
      confirmLabel: "Log out",
      tone: "warning"
    });

    render(<ThemeProvider theme={theme}><ConfirmationModal /></ThemeProvider>);
    fireEvent.keyDown(document, { key: "Escape" });

    await expect(confirmation).resolves.toBe(false);
  });
});
