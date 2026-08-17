import { render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { theme } from "../../../../../app/styles/theme";
import { useSystemStatus } from "../../hooks/useSystemStatus";
import { SystemStatus } from ".";

vi.mock("../../hooks/useSystemStatus");

describe("SystemStatus", () => {
  beforeEach(() => {
    vi.mocked(useSystemStatus).mockReturnValue({
      data: undefined,
      isPending: true
    } as ReturnType<typeof useSystemStatus>);
  });

  it("shows the backend startup state", () => {
    render(
      <ThemeProvider theme={theme}>
        <SystemStatus />
      </ThemeProvider>
    );

    expect(screen.getByRole("status")).toHaveTextContent("Waiting for the Nexo backend");
  });
});
