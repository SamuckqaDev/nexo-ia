import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { MessageList } from "./index";

describe("MessageList", () => {
  it("uses the Nexo loading identity while conversation history is restored", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[]}
          isLoading
          hasConversation
          hasModel
          phase="idle"
          streamingContent=""
          errorMessage={null}
          mode="chat"
        />
      </ThemeProvider>
    );

    expect(screen.getByRole("status")).toHaveTextContent("Opening conversation");
  });

  it("shows an honest thinking state before the first streamed token", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[]}
          isLoading={false}
          hasConversation
          hasModel
          phase="starting"
          streamingContent=""
          errorMessage={null}
          mode="chat"
        />
      </ThemeProvider>
    );

    expect(screen.getByRole("status")).toHaveTextContent("Thinking");
    expect(screen.getByText(/preparing the selected model/i)).toBeInTheDocument();
  });
});
