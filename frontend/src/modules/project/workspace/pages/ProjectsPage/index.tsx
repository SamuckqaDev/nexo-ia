import { CheckCircle, FolderOpen, GitBranch, HardDrives, Plus, ShieldCheck, TerminalWindow } from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { WorkspaceBadge, WorkspaceEmptyState, WorkspacePage, WorkspacePanel } from "../../../../../shared/components/WorkspacePage";
import { AccessGrid, AccessItem, Authorize, ProjectCard, ProjectList, ProjectMeta, ProjectTitle, ProjectsGrid, SafetyNote } from "./styles";

export function ProjectsPage(): ReactElement {
  const [authorizing, setAuthorizing] = useState<boolean>(false);

  return (
    <WorkspacePage
      eyebrow="Governed workspaces"
      title="Projects"
      description="Bind objectives, exact filesystem roots, Git state, permissions and verification evidence to one durable project context."
      icon={FolderOpen}
      actions={<Button type="button" icon={Plus} onClick={(): void => setAuthorizing(true)}>Authorize project</Button>}
    >
      <ProjectsGrid>
        <WorkspacePanel title="Authorized projects" description="Only canonical roots explicitly granted to your current account appear here.">
          <ProjectList>
            <ProjectCard type="button">
              <FolderOpen size={23} weight="duotone" />
              <ProjectTitle><strong>Nexo IA</strong><span>/workspace/nexo-ia</span><ProjectMeta><WorkspaceBadge tone="attention">Interface preview</WorkspaceBadge><small>Git · main</small></ProjectMeta></ProjectTitle>
            </ProjectCard>
            <WorkspaceEmptyState icon={FolderOpen} title="Connect your real workspace" description="The preview demonstrates the project card. Authorize an exact root to make a project discoverable when the Companion API is available." />
          </ProjectList>
        </WorkspacePanel>

        <WorkspacePanel as="aside" title={authorizing ? "Authorize a workspace" : "Project access model"} description={authorizing ? "Choose an exact root and the smallest useful capability set." : "Nexo separates reading, editing and command execution."}>
          {authorizing ? (
            <Authorize>
              <Input id="project-name" label="Project name" placeholder="My application" />
              <Input id="project-root" label="Exact workspace root" placeholder="/home/user/projects/application" helperText="Broad home folders and unresolved paths are rejected by the backend." />
              <Select id="project-access" label="Initial capability" options={[{ label: "Read-only inspection", value: "read" }, { label: "Read and propose edits", value: "propose" }, { label: "Commands require approval", value: "commands" }]} />
              <SafetyNote><ShieldCheck size={18} /><span>Authorization is unavailable until the project/Companion API exists. This form does not access your filesystem.</span></SafetyNote>
              <Button type="button" disabled>Companion API required</Button>
            </Authorize>
          ) : (
            <AccessGrid>
              <AccessItem><HardDrives size={19} /><div><strong>Inspect</strong><span>Files and metadata inside one canonical root.</span></div><CheckCircle size={16} /></AccessItem>
              <AccessItem><GitBranch size={19} /><div><strong>Propose changes</strong><span>Visible patches and diffs before consequential effects.</span></div><CheckCircle size={16} /></AccessItem>
              <AccessItem><TerminalWindow size={19} /><div><strong>Execute</strong><span>Separate command grants, timeout and observable evidence.</span></div><ShieldCheck size={16} /></AccessItem>
            </AccessGrid>
          )}
        </WorkspacePanel>
      </ProjectsGrid>
    </WorkspacePage>
  );
}
