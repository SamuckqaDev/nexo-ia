import type { z } from "zod";
import type { skillEditorSchema } from "../schemas/skillEditorSchema";

export type SkillScope = "built_in" | "organization" | "team" | "project" | "workspace" | "personal" | "session";
export type SkillActivation = "explicit" | "suggested";
export type SkillFilter = "all" | "built_in" | "personal" | "project";

export type SkillDefinition = {
  id: string;
  ownerId?: string;
  command: string;
  name: string;
  description: string;
  scope: SkillScope;
  activation: SkillActivation;
  instructions: string;
  outputContract: string;
  dependencies: string[];
  version: string;
  enabled: boolean;
  preview?: boolean;
};

export type SkillEditorValues = z.infer<typeof skillEditorSchema>;
export type SkillEditorProps = {
  initialSkill?: SkillDefinition;
  onSave: (values: SkillEditorValues, skillId?: string) => void;
};

export type SkillCatalogState = {
  ownerId: string | null;
  skills: SkillDefinition[];
  initialize: (ownerId: string) => void;
  reset: () => void;
  saveSkill: (values: SkillEditorValues, skillId?: string) => SkillDefinition;
};
