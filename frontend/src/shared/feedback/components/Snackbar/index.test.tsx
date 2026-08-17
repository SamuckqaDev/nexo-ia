import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../../../../app/styles/theme";
import { useSnackbarStore } from "../../stores/useSnackbarStore";
import { Snackbar } from ".";

describe("Snackbar", () => {
  beforeEach(() => useSnackbarStore.setState({ messages: [] }));

  it("shows and dismisses global feedback", () => {
    useSnackbarStore.getState().show("Account created.", { variant: "success", duration: 0 });
    render(<ThemeProvider theme={theme}><Snackbar /></ThemeProvider>);

    expect(screen.getByRole("status")).toHaveTextContent("Account created.");
    fireEvent.click(screen.getByRole("button", { name: "Dismiss notification" }));
    expect(screen.queryByText("Account created.")).not.toBeInTheDocument();
  });
});
