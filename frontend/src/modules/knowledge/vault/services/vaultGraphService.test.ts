import { describe, expect, it } from "vitest";
import type { KnowledgeVault } from "../types/vaultTypes";
import { buildVaultKnowledgeGraph } from "./vaultGraphService";

const vaults: KnowledgeVault[] = [
  {
    id: "architecture",
    name: "Architecture",
    description: "Approved decisions",
    scope: "project",
    sources: [{
      id: "decision-source",
      name: "service-boundaries.md",
      type: "Markdown",
      size: "4 KB",
      status: "local",
      contentPreview: "Service ownership and authorization boundaries for every workspace."
    }]
  },
  {
    id: "security",
    name: "Security",
    description: "Security references",
    scope: "personal",
    sources: [{
      id: "security-source",
      name: "authorization.md",
      type: "Markdown",
      size: "3 KB",
      status: "local",
      contentPreview: "Authorization boundaries define ownership for every protected workspace."
    }]
  }
];

describe("buildVaultKnowledgeGraph", () => {
  it("builds collection, source and shared-term connections from current Vault data", () => {
    const graph = buildVaultKnowledgeGraph(vaults, ["security-source"]);

    expect(graph.nodes).toHaveLength(4);
    expect(graph.edges.filter((edge) => edge.relation === "contains")).toHaveLength(2);
    expect(graph.edges.filter((edge) => edge.relation === "related")).toHaveLength(1);
    expect(graph.nodes).toContainEqual(expect.objectContaining({ sourceId: "security-source", attached: true }));
  });
});
