import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { previewVaults, useVaultCatalogStore } from "../../stores/useVaultCatalogStore";
import { VaultsPage } from "./index";

describe("VaultsPage", () => {
  beforeEach(() => useVaultCatalogStore.setState({ vaults: previewVaults, attachedSourceIds: [] }));

  it("creates a selectable session Vault draft", async () => {
    render(<ThemeProvider theme={darkTheme}><VaultsPage /></ThemeProvider>);

    fireEvent.click(screen.getByRole("button", { name: "New Vault" }));
    fireEvent.change(screen.getByLabelText("Vault name"), { target: { value: "Architecture decisions" } });
    fireEvent.change(screen.getByLabelText("Purpose"), { target: { value: "Ground answers in accepted architecture decisions." } });
    fireEvent.click(screen.getByRole("button", { name: "Create draft" }));

    expect(await screen.findByRole("heading", { name: "Architecture decisions" })).toBeInTheDocument();
    expect(screen.getByText("This Vault has no sources")).toBeInTheDocument();
  });

  it("opens source knowledge and attaches readable content to Chat", () => {
    render(<ThemeProvider theme={darkTheme}><VaultsPage /></ThemeProvider>);

    fireEvent.click(screen.getByRole("button", { name: /PRODUCT_VISION.md/i }));
    expect(screen.getByText(/local-first, team-ready AI workspace/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Attach to Chat" }));
    expect(screen.getByRole("button", { name: "Attached to Chat" })).toBeInTheDocument();
  });
});
