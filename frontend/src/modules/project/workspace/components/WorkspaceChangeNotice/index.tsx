import { ArrowClockwise, CheckCircle, FolderOpen, WarningDiamond } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type { WorkspaceChangeNoticeProps } from "../../types/workspaceHookTypes";
import { Actions, Copy, Notice, Samples, Summary } from "./styles";

function changeLabel(value: number, singular: string): string {
  return `${value} ${value === 1 ? singular : `${singular}s`}`;
}

export function WorkspaceChangeNotice({
  check,
  workspaceName,
  onManage,
  onRecheck,
  onAccept
}: WorkspaceChangeNoticeProps): ReactElement | null {
  if (check.status === "idle" || check.status === "unchanged") return null;

  if (check.status === "checking") {
    return (
      <Notice $tone="info" role="status">
        <ArrowClockwise size={20} weight="bold" />
        <Copy><strong>Checking {workspaceName}</strong><span>Nexo is comparing the saved project structure before Chat starts.</span></Copy>
      </Notice>
    );
  }

  if (check.status === "changed" && check.changes) {
    return (
      <Notice $tone="warning" role="alert">
        <WarningDiamond size={22} weight="fill" />
        <Copy>
          <strong>{workspaceName} changed since your last Chat session</strong>
          <span>Review the changes before relying on the previously loaded project context.</span>
          <Summary>
            {changeLabel(check.changes.added, "addition")} · {changeLabel(check.changes.removed, "removal")} · {changeLabel(check.changes.modified, "modified file")}
            {check.changes.truncated ? " · Large folders were bounded" : ""}
          </Summary>
          {check.changes.samples.length > 0 && <Samples>{check.changes.samples.join(" · ")}</Samples>}
        </Copy>
        <Actions>
          <Button type="button" variant="outline" icon={FolderOpen} onClick={onManage}>Review workspace</Button>
          <Button type="button" icon={CheckCircle} onClick={onAccept}>Use updated structure</Button>
        </Actions>
      </Notice>
    );
  }

  return (
    <Notice $tone="warning" role="alert">
      <WarningDiamond size={22} weight="fill" />
      <Copy>
        <strong>Workspace check needs attention</strong>
        <span>{check.message ?? "Nexo could not verify the selected project folder."}</span>
      </Copy>
      <Actions>
        <Button type="button" variant="outline" icon={FolderOpen} onClick={onManage}>Open Projects</Button>
        {(check.status === "permission-required" || check.status === "error")
          && <Button type="button" icon={ArrowClockwise} onClick={onRecheck}>Confirm and check again</Button>}
      </Actions>
    </Notice>
  );
}
