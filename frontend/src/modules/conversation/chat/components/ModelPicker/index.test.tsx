import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import type { ProviderModelCatalogView } from "../../../../provider/types/providerConfigurationTypes";
import { ModelPicker } from "./index";

const ollamaCatalog: ProviderModelCatalogView = {
  providerConfigurationId: "6a8ceeb1-c16f-4071-8ca7-0ec692aa21a9",
  providerType: "OLLAMA",
  displayName: "Studio Ollama",
  selectedModel: "qwen3:8b",
  status: "AVAILABLE",
  models: [
    {
      name: "qwen3:8b",
      modifiedAt: null,
      size: 123,
      toolCallingSupported: true,
      thinkingSupported: true
    },
    {
      name: "deepseek-r1:14b",
      modifiedAt: null,
      size: 456,
      toolCallingSupported: false,
      thinkingSupported: false
    }
  ],
  message: null
};

const renderPicker = (
  catalogs: ProviderModelCatalogView[],
  onSelect = vi.fn(),
  selectedProviderId: string | null = ollamaCatalog.providerConfigurationId,
  selectedModel: string | null = "qwen3:8b",
  isSaving = false,
  errorMessage: string | null = null
) => render(
  <ThemeProvider theme={darkTheme}>
    <ModelPicker
      catalogs={catalogs}
      selectedProviderId={selectedProviderId}
      selectedModel={selectedModel}
      disabled={false}
      isSaving={isSaving}
      errorMessage={errorMessage}
      onSelect={onSelect}
    />
  </ThemeProvider>
);

describe("ModelPicker", () => {
  it("lists every discovered model under its provider", () => {
    renderPicker([ollamaCatalog]);

    expect(screen.getByRole("option", { name: "qwen3:8b · Agent ready · Thinking · default" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "deepseek-r1:14b · no tools · no Thinking" })).toBeInTheDocument();
    expect(screen.getByText("2 models reported by providers")).toBeInTheDocument();
  });

  it("selects both the provider configuration and model", () => {
    const onSelect = vi.fn();
    renderPicker([ollamaCatalog], onSelect);

    fireEvent.change(screen.getByRole("combobox", { name: /model for this conversation/i }), {
      target: { value: JSON.stringify([ollamaCatalog.providerConfigurationId, "deepseek-r1:14b"]) }
    });

    expect(onSelect).toHaveBeenCalledWith(
      ollamaCatalog.providerConfigurationId,
      "deepseek-r1:14b"
    );
  });

  it("keeps the configured default as an honest fallback while the provider is unavailable", () => {
    renderPicker([{ ...ollamaCatalog, status: "UNAVAILABLE", models: [], message: "Offline" }]);

    expect(screen.getByRole("option", { name: "qwen3:8b · configured fallback" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Provider unavailable" })).toBeDisabled();
    expect(screen.getByText(/configured fallback shown/i)).toBeInTheDocument();
  });

  it("does not invent selectable models for unsupported provider protocols", () => {
    renderPicker([{
      ...ollamaCatalog,
      providerType: "ANTHROPIC",
      selectedModel: null,
      status: "UNSUPPORTED",
      models: []
    }], vi.fn(), null, null);

    expect(screen.getByRole("combobox", { name: /model for this conversation/i })).toBeDisabled();
    expect(screen.getByRole("option", { name: "Discovery not supported" })).toBeDisabled();
  });

  it("keeps an explicitly configured model selectable when discovery is unsupported", () => {
    const onSelect = vi.fn();
    renderPicker([{
      ...ollamaCatalog,
      providerType: "ANTHROPIC",
      selectedModel: "claude-sonnet-4-5",
      status: "UNSUPPORTED",
      models: []
    }], onSelect, null, null);

    const picker = screen.getByRole("combobox", { name: /model for this conversation/i });
    expect(picker).toBeEnabled();
    expect(screen.getByRole("option", { name: "claude-sonnet-4-5 · configured fallback" })).toBeInTheDocument();

    fireEvent.change(picker, {
      target: { value: JSON.stringify([ollamaCatalog.providerConfigurationId, "claude-sonnet-4-5"]) }
    });

    expect(onSelect).toHaveBeenCalledWith(ollamaCatalog.providerConfigurationId, "claude-sonnet-4-5");
  });

  it("explains model persistence instead of silently snapping the selection back", () => {
    const { rerender } = renderPicker([ollamaCatalog], vi.fn(), null, null, true);

    expect(screen.getByText(/saving model for this conversation/i)).toBeVisible();
    expect(screen.getByRole("combobox", { name: /model for this conversation/i })).toBeDisabled();

    rerender(
      <ThemeProvider theme={darkTheme}>
        <ModelPicker
          catalogs={[ollamaCatalog]}
          selectedProviderId={null}
          selectedModel={null}
          disabled={false}
          errorMessage="Provider unavailable"
          onSelect={vi.fn()}
        />
      </ThemeProvider>
    );
    expect(screen.getByText(/could not save model/i)).toBeVisible();
  });
});
