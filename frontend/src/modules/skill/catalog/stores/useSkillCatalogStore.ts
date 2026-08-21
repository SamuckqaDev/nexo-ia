import { create } from "zustand";
import type { SkillCatalogState, SkillDefinition, SkillEditorValues } from "../types/skillTypes";

export const builtInSkills: SkillDefinition[] = [
  {
    id: "built-in-project-review",
    command: "project-review",
    name: "Project review",
    description: "Inspect a codebase, identify risks and return evidence-backed recommendations.",
    scope: "built_in",
    activation: "explicit",
    instructions: "Inspect project context, review changes and verify each finding with direct evidence.",
    outputContract: "Prioritized findings with evidence.",
    dependencies: ["Workspace read"],
    version: "1.0.0",
    enabled: true,
    preview: true
  },
  {
    id: "built-in-research-brief",
    command: "research-brief",
    name: "Research brief",
    description: "Collect authorized sources and produce a cited, decision-ready synthesis.",
    scope: "built_in",
    activation: "suggested",
    instructions: "Define the question, inspect sources, compare evidence and cite the result.",
    outputContract: "Cited brief with open questions.",
    dependencies: ["Web or Vault source"],
    version: "1.0.0",
    enabled: true,
    preview: true
  }
];

export function skillCommand(name: string): string {
  return name
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 48) || "skill";
}

const skillsByOwner = new Map<string, SkillDefinition[]>();

function requireOwner(ownerId: string | null): string {
  if (!ownerId) throw new Error("A signed-in user is required to save a Skill.");
  return ownerId;
}

function activeSkills(ownerId: string): SkillDefinition[] {
  const personalSkills: SkillDefinition[] = skillsByOwner.get(ownerId) ?? [];
  return [...personalSkills.filter((skill: SkillDefinition): boolean => skill.ownerId === ownerId), ...builtInSkills];
}

export const useSkillCatalogStore = create<SkillCatalogState>((set, get) => ({
  ownerId: null,
  skills: builtInSkills,
  initialize: (ownerId: string): void => {
    if (get().ownerId === ownerId) return;
    set({ ownerId, skills: activeSkills(ownerId) });
  },
  reset: (): void => set({ ownerId: null, skills: builtInSkills }),
  saveSkill: (values: SkillEditorValues, skillId?: string): SkillDefinition => {
    const ownerId: string = requireOwner(get().ownerId);
    const editableSkill: SkillDefinition | undefined = get().skills.find((skill: SkillDefinition): boolean =>
      skill.id === skillId && skill.ownerId === ownerId);
    const definition: SkillDefinition = {
      id: editableSkill?.id ?? crypto.randomUUID(),
      ownerId,
      command: skillCommand(values.name),
      name: values.name,
      description: values.description,
      scope: values.scope,
      scopeTarget: values.scopeTarget?.trim() || undefined,
      activation: values.activation,
      instructions: values.instructions,
      outputContract: values.outputContract,
      dependencies: values.dependencies.split(",").map((value: string): string => value.trim()).filter(Boolean),
      version: "0.1.0-draft",
      enabled: true
    };
    set((state: SkillCatalogState): Pick<SkillCatalogState, "skills"> => {
      const personalSkills: SkillDefinition[] = state.skills.filter((skill: SkillDefinition): boolean =>
        skill.scope !== "built_in" && skill.ownerId === ownerId);
      const updatedPersonalSkills: SkillDefinition[] = editableSkill
        ? personalSkills.map((skill: SkillDefinition): SkillDefinition => skill.id === editableSkill.id ? definition : skill)
        : [definition, ...personalSkills];
      skillsByOwner.set(ownerId, updatedPersonalSkills);
      return { skills: [...updatedPersonalSkills, ...builtInSkills] };
    });
    return definition;
  }
}));
