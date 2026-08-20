import { beforeEach, describe, expect, it } from "vitest";
import type { SkillEditorValues } from "../types/skillTypes";
import { builtInSkills, useSkillCatalogStore } from "./useSkillCatalogStore";

const firstOwnerId = "00000000-0000-4000-8000-000000000211";
const secondOwnerId = "00000000-0000-4000-8000-000000000212";

function skillValues(name: string): SkillEditorValues {
  return {
    name,
    description: `Use ${name} only for the current account workflow.`,
    scope: "personal",
    activation: "explicit",
    instructions: "Inspect the authorized context and verify the result before responding.",
    outputContract: "A concise verified result.",
    dependencies: "Vault: private notes"
  };
}

describe("useSkillCatalogStore", () => {
  beforeEach(() => useSkillCatalogStore.getState().reset());

  it("shares built-in Skills while exposing only the active owner's drafts", () => {
    useSkillCatalogStore.getState().initialize(firstOwnerId);
    const firstOwnerSkill = useSkillCatalogStore.getState().saveSkill(skillValues("First owner review"));

    useSkillCatalogStore.getState().initialize(secondOwnerId);

    expect(useSkillCatalogStore.getState().skills).toEqual(expect.arrayContaining(builtInSkills));
    expect(useSkillCatalogStore.getState().skills).not.toContainEqual(expect.objectContaining({ id: firstOwnerSkill.id }));
    const secondOwnerSkill = useSkillCatalogStore.getState().saveSkill(skillValues("Second owner review"));

    useSkillCatalogStore.getState().initialize(firstOwnerId);

    expect(useSkillCatalogStore.getState().skills).toContainEqual(expect.objectContaining({ id: firstOwnerSkill.id, ownerId: firstOwnerId }));
    expect(useSkillCatalogStore.getState().skills).not.toContainEqual(expect.objectContaining({ id: secondOwnerSkill.id }));
  });

  it("keeps only global built-in Skills exposed after reset", () => {
    useSkillCatalogStore.getState().initialize(firstOwnerId);
    useSkillCatalogStore.getState().saveSkill(skillValues("Private review"));
    useSkillCatalogStore.getState().reset();

    expect(useSkillCatalogStore.getState().ownerId).toBeNull();
    expect(useSkillCatalogStore.getState().skills).toEqual(builtInSkills);
  });
});
