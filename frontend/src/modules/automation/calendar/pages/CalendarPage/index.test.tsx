import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { CalendarPage } from "./index";

describe("CalendarPage", () => {
  it("creates and selects a session schedule draft", async () => {
    render(<ThemeProvider theme={darkTheme}><CalendarPage /></ThemeProvider>);

    fireEvent.click(screen.getByRole("button", { name: "New schedule" }));
    fireEvent.change(screen.getByLabelText("Objective"), { target: { value: "Prepare weekly product report" } });
    fireEvent.click(screen.getByRole("button", { name: "Add draft" }));

    expect(await screen.findByRole("heading", { name: "Prepare weekly product report" })).toBeInTheDocument();
    expect(screen.getByText("Session draft")).toBeInTheDocument();
  });
});
