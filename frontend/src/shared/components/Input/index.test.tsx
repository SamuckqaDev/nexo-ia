import { fireEvent, render, screen, type RenderResult } from "@testing-library/react";
import { CalendarBlank } from "@phosphor-icons/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it } from "vitest";
import { theme } from "../../../app/styles/theme";
import { Input } from ".";

function renderInput(input: React.ReactNode): RenderResult {
  return render(<ThemeProvider theme={theme}>{input}</ThemeProvider>);
}

describe("Input", () => {
  it("toggles password visibility", () => {
    renderInput(<Input id="password" label="Password" type="password" />);
    const input = screen.getByLabelText("Password");

    expect(input).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: "Show password" }));
    expect(input).toHaveAttribute("type", "text");
    expect(screen.getByRole("button", { name: "Hide password" })).toBeInTheDocument();
  });

  it("preserves native input types and accepts an icon", () => {
    const { container } = renderInput(<Input id="date" label="Date" type="date" icon={CalendarBlank} />);

    expect(screen.getByLabelText("Date")).toHaveAttribute("type", "date");
    expect(container.querySelector("svg")).toBeInTheDocument();
  });
});
