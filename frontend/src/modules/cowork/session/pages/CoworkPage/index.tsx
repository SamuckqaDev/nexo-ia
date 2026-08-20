import { Brain, CheckCircle, Circle, Clock, Flag, Plus, ShieldCheck, UserFocus } from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { WorkspaceBadge, WorkspacePage, WorkspacePanel } from "../../../../../shared/components/WorkspacePage";
import { Activity, ActivityItem, Board, Composer, Milestone, Objective, ObjectiveCard, Plan, Progress } from "./styles";

export function CoworkPage(): ReactElement {
  const [creating, setCreating] = useState<boolean>(false);

  return (
    <WorkspacePage
      eyebrow="Durable objectives"
      title="Cowork"
      description="Coordinate a concrete objective through visible milestones, permission checkpoints, artifacts and verification without losing the collaboration history."
      icon={Brain}
      actions={<Button type="button" icon={Plus} onClick={(): void => setCreating(true)}>New Cowork</Button>}
    >
      {creating && (
        <WorkspacePanel title="Start a Cowork objective" description="Define outcome and authorized context before any plan is proposed." action={<WorkspaceBadge tone="attention">Draft only</WorkspaceBadge>}>
          <Composer>
            <Input id="cowork-objective" label="Objective" placeholder="What result should this session deliver?" />
            <Select id="cowork-project" label="Project context" options={[{ label: "No authorized project", value: "none" }, { label: "Nexo IA · preview", value: "preview" }]} />
            <Input id="cowork-done" label="Completion evidence" placeholder="Build passes, report delivered, decision recorded…" />
            <Button type="button" disabled>Cowork runtime required</Button>
          </Composer>
        </WorkspacePanel>
      )}
      <Board>
        <WorkspacePanel title="Active objective" description="A product-level preview of the durable session workspace." action={<WorkspaceBadge tone="attention">Interface preview</WorkspaceBadge>}>
          <Objective>
            <ObjectiveCard>
              <div><Brain size={23} weight="duotone" /><span><small>Objective</small><strong>Prepare Nexo frontend for the next product slice</strong></span></div>
              <p>Coordinate scope, design, implementation evidence and the decisions that still require a human.</p>
              <Progress><span>2 of 4 milestones ready</span><div><i /></div></Progress>
            </ObjectiveCard>
            <Plan aria-label="Cowork plan">
              <Milestone $state="done"><CheckCircle size={19} /><div><strong>Understand the product contract</strong><span>Vision and governance mapped</span></div></Milestone>
              <Milestone $state="done"><CheckCircle size={19} /><div><strong>Design the workspace surfaces</strong><span>Interaction model reviewed</span></div></Milestone>
              <Milestone $state="active"><Clock size={19} /><div><strong>Implement approved work</strong><span>Waiting for runtime connection</span></div></Milestone>
              <Milestone $state="waiting"><Circle size={19} /><div><strong>Verify and deliver</strong><span>Build, tests and evidence</span></div></Milestone>
            </Plan>
          </Objective>
        </WorkspacePanel>

        <WorkspacePanel as="aside" title="Activity & decisions" description="Human checkpoints remain separate from execution permission.">
          <Activity>
            <ActivityItem><UserFocus size={18} /><div><strong>Objective clarified</strong><span>Owner defined the expected frontend outcome.</span><small>Preview · 09:20</small></div></ActivityItem>
            <ActivityItem><Flag size={18} /><div><strong>Milestone ready</strong><span>Product surfaces mapped to documented journeys.</span><small>Preview · 09:34</small></div></ActivityItem>
            <ActivityItem><ShieldCheck size={18} /><div><strong>Permission checkpoint</strong><span>Real actions remain unavailable until the runtime is connected.</span><small>Requires backend</small></div></ActivityItem>
          </Activity>
        </WorkspacePanel>
      </Board>
    </WorkspacePage>
  );
}
