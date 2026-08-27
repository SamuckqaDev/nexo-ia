import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../../../shared/api/client";
import { createConversation } from "./chatApi";

afterEach(() => vi.restoreAllMocks());

describe("createConversation", () => {
  it("creates the conversation with its selected workspace in one request", async () => {
    const workspaceId = "1acde033-dcd8-4ae9-bda9-834d9cab6c95";
    vi.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        code: 201,
        message: "Conversation created",
        data: [{
          id: "71322bd7-5fc0-4d1e-8e93-c002201e82bc",
          title: "Fix the workspace",
          providerConfigurationId: null,
          selectedModel: null,
          knowledgeVaultIds: [],
          workspaceId,
          workspaceBindingId: null,
          createdAt: "2026-08-27T12:00:00Z",
          updatedAt: "2026-08-27T12:00:00Z"
        }]
      }
    });

    const conversation = await createConversation({ title: "Fix the workspace", workspaceId });

    expect(apiClient.post).toHaveBeenCalledWith("/conversations", {
      title: "Fix the workspace",
      workspaceId
    });
    expect(conversation.workspaceId).toBe(workspaceId);
  });
});
