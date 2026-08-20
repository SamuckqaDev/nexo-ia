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

export const useSkillCatalogStore = create<SkillCatalogState>((set) => ({
  skills: builtInSkills,
  saveSkill: (values: SkillEditorValues, skillId?: string): SkillDefinition => {
    const definition: SkillDefinition = {
      id: skillId ?? crypto.randomUUID(),
      command: skillCommand(values.name),
      name: values.name,
      description: values.description,
      scope: values.scope,
      activation: values.activation,
      instructions: values.instructions,
      outputContract: values.outputContract,
      dependencies: values.dependencies.split(",").map((value: string): string => value.trim()).filter(Boolean),
      version: "0.1.0-draft",
      enabled: true
    };
    set((state: SkillCatalogState): Pick<SkillCatalogState, "skills"> => ({
      skills: skillId
        ? state.skills.map((skill: SkillDefinition): SkillDefinition => skill.id === skillId ? definition : skill)
        : [definition, ...state.skills]
    }));
    return definition;
  }
}));
