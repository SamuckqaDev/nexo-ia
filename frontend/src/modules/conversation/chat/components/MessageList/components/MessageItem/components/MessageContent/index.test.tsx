import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../../../../../app/styles/theme";
import { MessageContent } from "./index";

describe("MessageContent", () => {
  it("renders model text as formatted Markdown", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageContent content={"## Result\n\n- first\n- second\n\n`npm test`"} isStreaming={false} isUser={false} />
      </ThemeProvider>
    );

    expect(screen.getByRole("heading", { name: "Result" })).toBeVisible();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getByText("npm test")).toBeVisible();
  });

  it("renders diff blocks with additions and removals", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageContent content={'```diff\n-old value\n+new value\n```'} isStreaming={false} isUser={false} />
      </ThemeProvider>
    );

    expect(screen.getByLabelText("diff code block")).toBeVisible();
    expect(screen.getByText("-old value")).toBeVisible();
    expect(screen.getByText("+new value")).toBeVisible();
  });
});
