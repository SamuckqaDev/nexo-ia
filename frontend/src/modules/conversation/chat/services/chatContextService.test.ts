import { describe, expect, it } from "vitest";
import type { VaultSourceReference } from "../../../knowledge/vault/types/vaultTypes";
import { builtInSkills } from "../../../skill/catalog/stores/useSkillCatalogStore";
import { buildContextualChatMessage, parseContextualChatMessage } from "./chatContextService";

const source: VaultSourceReference = {
  vaultId: "vault-1",
  vaultName: "Architecture",
  source: {
    id: "source-1",
    name: "decisions.md",
    type: "Markdown",
    size: "2 KB",
    status: "local",
    contentPreview: "Use ports and adapters around external provider boundaries."
  }
};

describe("chatContextService", () => {
  it("keeps explicit Skill and Vault context bounded while preserving a clean display message", () => {
    const encoded = buildContextualChatMessage("Review the provider module", {
      skill: builtInSkills[0],
      vaultSources: [source]
    });
    const parsed = parseContextualChatMessage(encoded);

    expect(encoded.length).toBeLessThanOrEqual(12_000);
    expect(encoded).toContain("untrusted reference data");
    expect(parsed).toEqual({
      content: "Review the provider module",
      skillName: "Project review",
      vaultSourceNames: ["decisions.md"]
    });
  });

  it("leaves ordinary messages unchanged", () => {
    expect(parseContextualChatMessage("Hello")).toEqual({
      content: "Hello",
      skillName: null,
      vaultSourceNames: []
    });
  });
});
