import { render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it } from "vitest";
import { darkTheme } from "../../../app/styles/theme";
import { Button } from "./index";

const renderButton = (busy = false): void => {
  render(
    <ThemeProvider theme={darkTheme}>
      <Button type="button" disabled aria-busy={busy}>Connect</Button>
    </ThemeProvider>
  );
};

describe("Button", () => {
  it("distinguishes an unavailable action from a pending operation", () => {
    renderButton();

    expect(screen.getByRole("button", { name: "Connect" })).toHaveStyle({
      cursor: "not-allowed"
    });
  });

  it("shows progress only when the disabled action is actually pending", () => {
    renderButton(true);

    expect(screen.getByRole("button", { name: "Connect" })).toHaveStyle({
      cursor: "progress"
    });
  });

  it("offers a compact size for dense tool surfaces", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <Button type="button" size="compact">Inspect</Button>
      </ThemeProvider>
    );

    expect(screen.getByRole("button", { name: "Inspect" })).toHaveStyle({
      padding: "0.46rem 0.66rem",
      fontSize: "0.7rem"
    });
  });
});
