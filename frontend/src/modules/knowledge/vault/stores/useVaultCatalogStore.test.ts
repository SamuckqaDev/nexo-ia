import { beforeEach, describe, expect, it } from "vitest";
import { useVaultCatalogStore } from "./useVaultCatalogStore";

const firstOwnerId = "00000000-0000-4000-8000-000000000111";
const secondOwnerId = "00000000-0000-4000-8000-000000000112";

describe("useVaultCatalogStore", () => {
  beforeEach(() => useVaultCatalogStore.getState().reset());

  it("keeps Vault drafts and Chat attachments isolated when the owner changes", () => {
    useVaultCatalogStore.getState().initialize(firstOwnerId);
    const firstOwnerVault = useVaultCatalogStore.getState().createVault({
      name: "Private architecture",
      description: "Decisions that belong only to the first account.",
      scope: "personal"
    });
    useVaultCatalogStore.getState().toggleSourceAttachment("source-vision");

    useVaultCatalogStore.getState().initialize(secondOwnerId);

    expect(useVaultCatalogStore.getState().vaults).not.toContainEqual(expect.objectContaining({ id: firstOwnerVault.id }));
    expect(useVaultCatalogStore.getState().attachedSourceIds).toEqual([]);

    useVaultCatalogStore.getState().createVault({
      name: "Second account notes",
      description: "Knowledge that belongs only to the second account.",
      scope: "personal"
    });
    useVaultCatalogStore.getState().initialize(firstOwnerId);

    expect(useVaultCatalogStore.getState().vaults).toContainEqual(expect.objectContaining({ id: firstOwnerVault.id }));
    expect(useVaultCatalogStore.getState().vaults).not.toContainEqual(expect.objectContaining({ name: "Second account notes" }));
    expect(useVaultCatalogStore.getState().attachedSourceIds).toEqual(["source-vision"]);
  });

  it("removes the active owner catalog from the exposed state on reset", () => {
    useVaultCatalogStore.getState().initialize(firstOwnerId);
    useVaultCatalogStore.getState().reset();

    expect(useVaultCatalogStore.getState().ownerId).toBeNull();
    expect(useVaultCatalogStore.getState().vaults).toEqual([]);
    expect(useVaultCatalogStore.getState().attachedSourceIds).toEqual([]);
  });
});
