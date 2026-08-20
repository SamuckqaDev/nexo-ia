import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { VaultsPage } from "./index";

describe("VaultsPage", () => {
  it("creates a selectable session Vault draft", async () => {
    render(<ThemeProvider theme={darkTheme}><VaultsPage /></ThemeProvider>);

    fireEvent.click(screen.getByRole("button", { name: "New Vault" }));
    fireEvent.change(screen.getByLabelText("Vault name"), { target: { value: "Architecture decisions" } });
    fireEvent.change(screen.getByLabelText("Purpose"), { target: { value: "Ground answers in accepted architecture decisions." } });
    fireEvent.click(screen.getByRole("button", { name: "Create draft" }));

    expect(await screen.findByRole("heading", { name: "Architecture decisions" })).toBeInTheDocument();
    expect(screen.getByText("This Vault has no sources")).toBeInTheDocument();
  });
});
