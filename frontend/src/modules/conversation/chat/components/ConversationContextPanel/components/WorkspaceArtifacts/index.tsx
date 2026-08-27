import { Check, Prohibit, ArrowCounterClockwise } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../../../shared/components/Button";
import type { WorkspaceChange } from "../../../../../../project/workspace/types/workspaceChangeTypes";
import {
  ArtifactActions,
  ArtifactCard,
  ArtifactHeader,
  ArtifactNotice,
  ArtifactStack,
  DiffGrid,
  DiffSide,
  StatusBadge
} from "./styles";

type WorkspaceArtifactsProps = {
  changes: WorkspaceChange[];
  busy: boolean;
  onApprove: (id: string) => void;
  onDeny: (id: string) => void;
  onRevert: (id: string) => void;
};

const operationLabel = (change: WorkspaceChange): string => {
  if (change.operation === "CREATE") return "Create file";
  if (change.operation === "DELETE") return "Delete file";
  return change.replacementCount && change.replacementCount > 1
    ? `Edit ${change.replacementCount} matches`
    : "Edit exact match";
};

export function WorkspaceArtifacts({
  changes,
  busy,
  onApprove,
  onDeny,
  onRevert
}: WorkspaceArtifactsProps): ReactElement {
  return (
    <ArtifactStack aria-label="Workspace change artifacts">
      {changes.map((change: WorkspaceChange) => (
        <ArtifactCard key={change.id}>
          <ArtifactHeader>
            <div>
              <strong title={change.path}>{change.path}</strong>
              <small>{operationLabel(change)}</small>
            </div>
            <StatusBadge>{change.status.replaceAll("_", " ")}</StatusBadge>
          </ArtifactHeader>
          <DiffGrid aria-label={`Server diff for ${change.path}`}>
            <DiffSide>
              <span>Before</span>
              <pre>{change.beforeContent ?? "(new file)"}</pre>
            </DiffSide>
            <DiffSide $after>
              <span>After</span>
              <pre>{change.afterContent ?? "(deleted file)"}</pre>
            </DiffSide>
          </DiffGrid>
          {change.previewTruncated && (
            <ArtifactNotice>The preview is bounded; approval still applies the complete server artifact.</ArtifactNotice>
          )}
          {change.failureCode && <ArtifactNotice role="alert">{change.failureCode}</ArtifactNotice>}
          {change.status === "PENDING_APPROVAL" && (
            <ArtifactActions>
              <Button size="compact" icon={Check} disabled={busy} onClick={(): void => onApprove(change.id)}>
                Apply
              </Button>
              <Button size="compact" variant="outline" icon={Prohibit} disabled={busy} onClick={(): void => onDeny(change.id)}>
                Deny
              </Button>
            </ArtifactActions>
          )}
          {change.status === "APPLIED" && (
            <ArtifactActions>
              <Button size="compact" variant="outline" icon={ArrowCounterClockwise} disabled={busy} onClick={(): void => onRevert(change.id)}>
                Revert
              </Button>
            </ArtifactActions>
          )}
        </ArtifactCard>
      ))}
    </ArtifactStack>
  );
}
