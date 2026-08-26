import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { builtInSkills, useSkillCatalogStore } from "../../../../skill/catalog/stores/useSkillCatalogStore";
import type { StreamPhase } from "../../types/chatTypes";
import type { AgentContextSummary } from "../../types/chatViewTypes";
import { ChatComposer } from "./index";

const defaultAgentContext: AgentContextSummary = {
  selectedVaultNames: ["Nexo Knowledge Base"],
  enabledMcpConnectionNames: ["Fetch"],
  enabledMcpToolCount: 1,
  knowledgeLoading: false,
  knowledgeError: false,
  mcpLoading: false,
  mcpError: false,
  modelToolCallingSupported: true,
  modelThinkingSupported: true,
  thinkingEnabled: false
};

const renderComposer = (
  mode: "chat" | "agent",
  onModeChange = vi.fn(),
  initialContent = "",
  onSend = vi.fn(),
  isBusy = false,
  phase: StreamPhase = "idle",
  messageHistory: string[] = [],
  agentContext: AgentContextSummary = defaultAgentContext,
  imageRuntimeAvailable = false,
  onGenerateImage = vi.fn(),
  imageModels: string[] = ["v1-5-pruned.safetensors", "medical-study.safetensors"]
) => {
  render(
    <ThemeProvider theme={darkTheme}>
      <ChatComposer
        disabled={false}
        initialContent={initialContent}
        messageHistory={messageHistory}
        hasModel
        phase={phase}
        isBusy={isBusy}
        imageRuntimeAvailable={imageRuntimeAvailable}
        imageRuntimeMessage={imageRuntimeAvailable ? "ComfyUI ready" : "ComfyUI unavailable"}
        imageModels={imageRuntimeAvailable ? imageModels : []}
        defaultImageModel={imageRuntimeAvailable ? imageModels[0] ?? null : null}
        imageSubmitting={false}
        mode={mode}
        agentContext={agentContext}
        onModeChange={onModeChange}
        onInspectKnowledge={vi.fn()}
        onManageMcp={vi.fn()}
        onSend={onSend}
        onGenerateImage={onGenerateImage}
        onCancel={vi.fn()}
      />
    </ThemeProvider>
  );
  return { onSend, onGenerateImage };
};

describe("ChatComposer", () => {
  beforeEach(() => {
    useSkillCatalogStore.setState({ skills: builtInSkills });
  });

  it("keeps Chat as the active conversational surface and exposes capabilities in the composer", () => {
    renderComposer("chat");

    expect(screen.getByRole("textbox", { name: "Message" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "MCP" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Image" })).toBeDisabled();
  });

  it("requests Agent mode without replacing the conversation surface", () => {
    const onModeChange = vi.fn();
    renderComposer("chat", onModeChange);

    fireEvent.click(screen.getByRole("button", { name: "Agent" }));

    expect(onModeChange).toHaveBeenCalledWith("agent");
  });

  it("opens with a request drafted from the workspace Home", () => {
    renderComposer("chat", vi.fn(), "Review the authentication flow");

    expect(screen.getByRole("textbox", { name: "Message" }))
      .toHaveValue("Review the authentication flow");
  });

  it("enables Agent input and shows the knowledge and MCP context it will receive", () => {
    const onSend = vi.fn();
    renderComposer("agent", vi.fn(), "Inspect our principles", onSend);

    expect(screen.getByRole("textbox", { name: "Message" })).toBeEnabled();
    expect(screen.getByRole("region", { name: "Agent context" })).toBeVisible();
    expect(screen.getByText("Nexo Knowledge Base")).toBeVisible();
    expect(screen.getByText(/1 tool from Fetch/i)).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: "Send message" }));
    expect(onSend).toHaveBeenCalledOnce();
  });

  it("blocks an Agent run when Ollama reports that the selected model cannot call tools", () => {
    renderComposer("agent", vi.fn(), "Use Fetch", vi.fn(), false, "idle", [], {
      ...defaultAgentContext,
      modelToolCallingSupported: false
    });

    expect(screen.getByText(/selected model has no tool calling/i)).toBeVisible();
    expect(screen.getByRole("textbox", { name: "Message" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Send message" })).toBeDisabled();
  });

  it("switches to local image mode and queues the prompt instead of sending chat", () => {
    const onSend = vi.fn();
    const onGenerateImage = vi.fn();
    renderComposer(
      "agent", vi.fn(), "", onSend, false, "idle", [], defaultAgentContext,
      true, onGenerateImage
    );

    fireEvent.click(screen.getByRole("button", { name: "Image" }));
    const prompt = screen.getByRole("textbox", { name: "Message" });
    expect(screen.queryByRole("region", { name: "Agent context" })).not.toBeInTheDocument();
    expect(prompt).toHaveAttribute("placeholder", "Describe the image Nexo should generate…");

    fireEvent.change(prompt, { target: { value: "A cyan neural knowledge graph" } });
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));

    expect(onGenerateImage).toHaveBeenCalledWith(
      "A cyan neural knowledge graph",
      "v1-5-pruned.safetensors"
    );
    expect(onSend).not.toHaveBeenCalled();
  });

  it("lets the user choose an installed ComfyUI checkpoint for the image request", () => {
    const onGenerateImage = vi.fn();
    renderComposer(
      "chat", vi.fn(), "An anatomical fracture study", vi.fn(), false, "idle", [],
      defaultAgentContext, true, onGenerateImage
    );

    fireEvent.click(screen.getByRole("button", { name: "Image" }));
    fireEvent.change(screen.getByRole("combobox", { name: "Image model" }), {
      target: { value: "medical-study.safetensors" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));

    expect(onGenerateImage).toHaveBeenCalledWith(
      "An anatomical fracture study",
      "medical-study.safetensors"
    );
  });

  it("opens the Skill palette with slash and includes the chosen method in the message context", () => {
    const onSend = vi.fn();
    renderComposer("chat", vi.fn(), "", onSend);
    const message = screen.getByRole("textbox", { name: "Message" });

    fireEvent.change(message, { target: { value: "/pro" } });
    fireEvent.click(screen.getByRole("option", { name: /Project review/i }));

    expect(screen.getByText("Skill: /project-review")).toBeInTheDocument();
    fireEvent.change(message, { target: { value: "Review this module" } });
    fireEvent.keyDown(message, { key: "Enter" });

    expect(onSend).toHaveBeenCalledOnce();
    expect(onSend.mock.calls[0][0]).toContain("NEXO_EXPLICIT_CONTEXT");
    expect(onSend.mock.calls[0][0]).toContain("Review this module");
    expect(onSend.mock.calls[0][0]).toContain("verify each finding");
  });

  it("keeps the Skill palette visible above the conversation", () => {
    renderComposer("chat");
    const message = screen.getByRole("textbox", { name: "Message" });

    fireEvent.change(message, { target: { value: "/" } });

    expect(screen.getByRole("listbox", { name: "Available Skills" })).toBeVisible();
    expect(message.closest("form")?.parentElement).toHaveStyle({ overflow: "hidden" });
    expect(message.closest("form")?.parentElement?.parentElement).toHaveStyle({
      overflow: "visible",
      position: "relative",
      zIndex: "1"
    });
  });

  it("keeps send and stop as compact controls on the right edge", () => {
    renderComposer("chat", vi.fn(), "Hello");
    expect(screen.getByRole("button", { name: "Send message" })).toHaveStyle({
      width: "2.5rem",
      height: "2.5rem",
      marginLeft: "auto"
    });

    renderComposer("chat", vi.fn(), "", vi.fn(), true, "streaming");
    const stop = screen.getByRole("button", { name: "Stop response" });
    expect(stop).toHaveStyle({ width: "2.5rem", height: "2.5rem", marginLeft: "auto" });
    expect(stop).not.toHaveTextContent("Stop");
  });

  it("browses sent messages with ArrowUp and ArrowDown, then sends the recalled prompt", () => {
    const onSend = vi.fn();
    renderComposer("chat", vi.fn(), "", onSend, false, "idle", ["First prompt", "Second prompt", "Latest prompt"]);
    const message = screen.getByRole("textbox", { name: "Message" });

    fireEvent.keyDown(message, { key: "ArrowUp" });
    expect(message).toHaveValue("Latest prompt");
    fireEvent.keyDown(message, { key: "ArrowUp" });
    expect(message).toHaveValue("Second prompt");
    fireEvent.keyDown(message, { key: "ArrowDown" });
    expect(message).toHaveValue("Latest prompt");
    fireEvent.keyDown(message, { key: "Enter" });

    expect(onSend).toHaveBeenCalledWith("Latest prompt");
    expect(message).toHaveValue("");
  });
});
