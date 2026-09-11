import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../../shared/api/client";
import { createProviderConfiguration, getProviderModelCatalog } from "./providerRegistryApi";

afterEach(() => vi.restoreAllMocks());

describe("getProviderModelCatalog", () => {
  it("parses the authenticated provider model catalog contract", async () => {
    vi.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        code: 200,
        message: "Provider model catalog inspected",
        data: [{
          providerConfigurationId: "6a8ceeb1-c16f-4071-8ca7-0ec692aa21a9",
          providerType: "OLLAMA",
          displayName: "Local Ollama",
          selectedModel: "qwen3:8b",
          status: "AVAILABLE",
          models: [{
            name: "qwen3:8b",
            modifiedAt: null,
            size: 123,
            toolCallingSupported: true,
            thinkingSupported: true
          }],
          message: null
        }]
      }
    });

    const catalog = await getProviderModelCatalog("6a8ceeb1-c16f-4071-8ca7-0ec692aa21a9");

    expect(apiClient.get).toHaveBeenCalledWith(
      "/providers/configurations/6a8ceeb1-c16f-4071-8ca7-0ec692aa21a9/models"
    );
    expect(catalog.models.map((model) => model.name)).toEqual(["qwen3:8b"]);
    expect(catalog.models[0].toolCallingSupported).toBe(true);
    expect(catalog.models[0].thinkingSupported).toBe(true);
  });

  it("rejects malformed model data instead of trusting the transport", async () => {
    vi.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        code: 200,
        message: "Provider model catalog inspected",
        data: [{
          providerConfigurationId: "6a8ceeb1-c16f-4071-8ca7-0ec692aa21a9",
          providerType: "OLLAMA",
          displayName: "Local Ollama",
          selectedModel: null,
          status: "AVAILABLE",
          models: [{
            name: 42,
            modifiedAt: null,
            size: null,
            toolCallingSupported: null,
            thinkingSupported: null
          }],
          message: null
        }]
      }
    });

    await expect(getProviderModelCatalog("6a8ceeb1-c16f-4071-8ca7-0ec692aa21a9"))
      .rejects.toThrow();
  });
});

describe("createProviderConfiguration", () => {
  it("sends the credential through Axios but accepts only the redacted server response", async () => {
    vi.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        code: 200,
        message: "Provider saved",
        data: [{
          id: "6a8ceeb1-c16f-4071-8ca7-0ec692aa21a9",
          providerType: "OPENAI",
          displayName: "OpenAI",
          endpoint: "https://api.openai.com",
          selectedModel: "gpt-5.4",
          enabled: true,
          credentialConfigured: true,
          lastConnectedAt: null
        }]
      }
    });

    const provider = await createProviderConfiguration({
      providerType: "OPENAI",
      displayName: "OpenAI",
      endpoint: "https://api.openai.com",
      selectedModel: "gpt-5.4",
      apiKey: "request-only-key"
    });

    expect(apiClient.post).toHaveBeenCalledWith("/providers/configurations", expect.objectContaining({
      apiKey: "request-only-key"
    }));
    expect(provider.credentialConfigured).toBe(true);
    expect(provider).not.toHaveProperty("apiKey");
  });
});
