import { describe, expect, it } from "vitest";
import type { BackendKnowledgeGraph } from "../types/vaultGraphTypes";
import { buildVaultKnowledgeGraph } from "./vaultGraphService";

const vaultId = "11111111-1111-4111-8111-111111111111";
const firstSourceId = "22222222-2222-4222-8222-222222222222";
const secondSourceId = "33333333-3333-4333-8333-333333333333";

const graph: BackendKnowledgeGraph = {
  nodes: [
    { id: `vault:${vaultId}`, kind: "VAULT", vaultId, sourceId: null, ordinal: null, label: "Nexo Knowledge Base", detail: "2 sources", excerpt: "Product knowledge", status: "PERSONAL" },
    { id: `source:${firstSourceId}`, kind: "SOURCE", vaultId, sourceId: firstSourceId, ordinal: null, label: "Vision.md", detail: "1 chunk", excerpt: null, status: "READY" },
    { id: `source:${secondSourceId}`, kind: "SOURCE", vaultId, sourceId: secondSourceId, ordinal: null, label: "Principles.md", detail: "1 chunk", excerpt: null, status: "READY" },
    { id: "chunk:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", kind: "CHUNK", vaultId, sourceId: firstSourceId, ordinal: 0, label: "Chunk 1", detail: "20 estimated tokens", excerpt: "Local-first knowledge", status: "EMBEDDED" },
    { id: "chunk:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", kind: "CHUNK", vaultId, sourceId: secondSourceId, ordinal: 0, label: "Chunk 1", detail: "18 estimated tokens", excerpt: "Private local context", status: "EMBEDDED" }
  ],
  edges: [
    { id: "contains:vault:first", relation: "CONTAINS", fromId: `vault:${vaultId}`, toId: `source:${firstSourceId}`, similarity: null },
    { id: "contains:vault:second", relation: "CONTAINS", fromId: `vault:${vaultId}`, toId: `source:${secondSourceId}`, similarity: null },
    { id: "contains:first:chunk", relation: "CONTAINS", fromId: `source:${firstSourceId}`, toId: "chunk:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", similarity: null },
    { id: "contains:second:chunk", relation: "CONTAINS", fromId: `source:${secondSourceId}`, toId: "chunk:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", similarity: null },
    { id: "semantic:chunks", relation: "SEMANTIC", fromId: "chunk:aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", toId: "chunk:bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", similarity: 0.91 }
  ],
  vaultCount: 1,
  sourceCount: 2,
  chunkCount: 2,
  truncated: false
};

describe("buildVaultKnowledgeGraph", () => {
  it("lays out real Vault, source and chunk nodes with semantic links", () => {
    const layout = buildVaultKnowledgeGraph(graph, true);

    expect(layout.nodes).toHaveLength(5);
    expect(layout.nodes.every((node) => Number.isFinite(node.x) && Number.isFinite(node.y))).toBe(true);
    expect(layout.edges).toContainEqual(expect.objectContaining({ id: "semantic:chunks", similarity: 0.91 }));
  });

  it("collapses chunk relationships into source-level semantic links", () => {
    const layout = buildVaultKnowledgeGraph(graph, false);

    expect(layout.nodes.some((node) => node.kind === "CHUNK")).toBe(false);
    expect(layout.edges).toContainEqual(expect.objectContaining({
      relation: "SEMANTIC",
      fromId: `source:${firstSourceId}`,
      toId: `source:${secondSourceId}`,
      similarity: 0.91
    }));
  });
});
