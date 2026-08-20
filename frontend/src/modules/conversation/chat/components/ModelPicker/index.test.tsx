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
    { name: "qwen3:8b", modifiedAt: null, size: 123 },
    { name: "deepseek-r1:14b", modifiedAt: null, size: 456 }
  ],
  message: null
};

const renderPicker = (catalogs: ProviderModelCatalogView[], onSelect = vi.fn()) => render(
  <ThemeProvider theme={darkTheme}>
    <ModelPicker
      catalogs={catalogs}
      selectedProviderId={ollamaCatalog.providerConfigurationId}
      selectedModel="qwen3:8b"
      disabled={false}
      onSelect={onSelect}
    />
  </ThemeProvider>
);

describe("ModelPicker", () => {
  it("lists every discovered model under its provider", () => {
    renderPicker([ollamaCatalog]);

    expect(screen.getByRole("option", { name: "qwen3:8b · default" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "deepseek-r1:14b" })).toBeInTheDocument();
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
    }]);

    expect(screen.getByRole("combobox", { name: /model for this conversation/i })).toBeDisabled();
    expect(screen.getByRole("option", { name: "Discovery not supported" })).toBeDisabled();
  });
});
