import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { SkillsPage } from "./index";

describe("SkillsPage", () => {
  it("creates a governed session Skill draft", async () => {
    render(<ThemeProvider theme={darkTheme}><SkillsPage /></ThemeProvider>);

    fireEvent.change(screen.getByLabelText("Skill name"), { target: { value: "Architecture review" } });
    fireEvent.change(screen.getByLabelText("When should Nexo use it?"), { target: { value: "Use when reviewing a project architecture decision." } });
    fireEvent.change(screen.getByLabelText("Workflow instructions"), { target: { value: "Inspect the architecture context, compare constraints, and verify every recommendation." } });
    fireEvent.change(screen.getByLabelText("Expected output"), { target: { value: "A prioritized architecture report." } });
    fireEvent.click(screen.getByRole("button", { name: "Save Skill draft" }));

    expect(await screen.findByText("Architecture review")).toBeInTheDocument();
    expect(screen.getByText("Session draft")).toBeInTheDocument();
  });
});
