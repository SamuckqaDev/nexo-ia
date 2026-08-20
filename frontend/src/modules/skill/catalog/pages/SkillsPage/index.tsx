import { Check, Code, FileText, Plus, ShieldCheck, Sparkle } from "@phosphor-icons/react";
import { useMemo, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { WorkspaceBadge, WorkspacePage, WorkspacePanel, WorkspaceSegmentedControl } from "../../../../../shared/components/WorkspacePage";
import { SkillEditor } from "../../components/SkillEditor";
import type { SkillDefinition, SkillEditorValues, SkillFilter } from "../../types/skillTypes";
import { Dependency, DependencyList, Library, LibraryToolbar, SkillButton, SkillCopy, SkillList, Workbench } from "./styles";

const builtInPreview: SkillDefinition[] = [
  { id: "built-in-project-review", name: "Project review", description: "Inspect a codebase, identify risks and return evidence-backed recommendations.", scope: "built_in", activation: "explicit", instructions: "Inspect project context, review changes and verify each finding with direct evidence.", outputContract: "Prioritized findings with evidence.", dependencies: ["Workspace read"], version: "1.0.0", enabled: true, preview: true },
  { id: "built-in-research-brief", name: "Research brief", description: "Collect authorized sources and produce a cited, decision-ready synthesis.", scope: "built_in", activation: "suggested", instructions: "Define the question, inspect sources, compare evidence and cite the result.", outputContract: "Cited brief with open questions.", dependencies: ["Web or Vault source"], version: "1.0.0", enabled: true, preview: true }
];

export function SkillsPage(): ReactElement {
  const [skills, setSkills] = useState<SkillDefinition[]>(builtInPreview);
  const [filter, setFilter] = useState<SkillFilter>("all");
  const [selected, setSelected] = useState<SkillDefinition | undefined>();
  const [editorKey, setEditorKey] = useState<number>(0);
  const visible = useMemo<SkillDefinition[]>(() => skills.filter((skill) => filter === "all" || skill.scope === filter), [filter, skills]);

  const newSkill = (): void => { setSelected(undefined); setEditorKey((value) => value + 1); };
  const saveSkill = (values: SkillEditorValues, skillId?: string): void => {
    const definition: SkillDefinition = {
      id: skillId ?? crypto.randomUUID(),
      name: values.name,
      description: values.description,
      scope: values.scope,
      activation: values.activation,
      instructions: values.instructions,
      outputContract: values.outputContract,
      dependencies: values.dependencies.split(",").map((value) => value.trim()).filter(Boolean),
      version: "0.1.0-draft",
      enabled: false
    };
    setSkills((current) => skillId ? current.map((skill) => skill.id === skillId ? definition : skill) : [definition, ...current]);
    setSelected(definition);
  };

  return (
    <WorkspacePage
      eyebrow="Reusable methods"
      title="Skills"
      description="Create repeatable workflows with explicit ownership, activation rules, output contracts and dependencies that never become permissions."
      icon={Sparkle}
      actions={<Button type="button" icon={Plus} onClick={newSkill}>New Skill</Button>}
    >
      <Workbench>
        <WorkspacePanel title="Skill library" description="Built-in references and drafts owned by your current scope.">
          <Library>
            <LibraryToolbar>
              <WorkspaceSegmentedControl
                label="Filter Skills"
                value={filter}
                options={[{ label: "All", value: "all" }, { label: "Built-in", value: "built_in" }, { label: "Personal", value: "personal" }, { label: "Project", value: "project" }]}
                onChange={setFilter}
              />
            </LibraryToolbar>
            <SkillList>
              {visible.map((skill) => (
                <SkillButton key={skill.id} type="button" $active={selected?.id === skill.id} onClick={(): void => { setSelected(skill); setEditorKey((value) => value + 1); }}>
                  {skill.scope === "built_in" ? <Code size={20} weight="duotone" /> : <FileText size={20} weight="duotone" />}
                  <SkillCopy><strong>{skill.name}</strong><span>{skill.description}</span><div><WorkspaceBadge tone={skill.preview ? "positive" : "attention"}>{skill.preview ? "Built-in preview" : "Session draft"}</WorkspaceBadge><small>{skill.scope.replace("_", " ")} · {skill.version}</small></div></SkillCopy>
                </SkillButton>
              ))}
            </SkillList>
          </Library>
        </WorkspacePanel>

        <WorkspacePanel
          title={selected?.preview ? "Use as a starting point" : selected ? "Edit Skill draft" : "Create a Skill"}
          description="Define the behavior here; execution access is resolved independently at run time."
          action={<WorkspaceBadge tone="attention">Not published</WorkspaceBadge>}
        >
          {selected?.dependencies.length ? <DependencyList><span>Declared dependencies</span>{selected.dependencies.map((dependency) => <Dependency key={dependency}><Check size={13} />{dependency}</Dependency>)}</DependencyList> : null}
          <SkillEditor key={`${selected?.id ?? "new"}-${editorKey}`} initialSkill={selected} onSave={saveSkill} />
        </WorkspacePanel>
      </Workbench>
      <WorkspacePanel title="Governance boundary" description="What a Skill can and cannot do.">
        <Library>
          <DependencyList><Dependency><ShieldCheck size={14} />Instructions do not grant tools</Dependency><Dependency><ShieldCheck size={14} />Sharing does not share Vaults</Dependency><Dependency><ShieldCheck size={14} />Every dependency is reauthorized</Dependency></DependencyList>
        </Library>
      </WorkspacePanel>
    </WorkspacePage>
  );
}
