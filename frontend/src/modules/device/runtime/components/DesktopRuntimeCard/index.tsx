import { CheckCircle, Desktop, FolderOpen, Link, WarningCircle } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { useDesktopRuntime } from "../../hooks/useDesktopRuntime";
import type { DesktopRuntimeCardProps } from "../../types/deviceTypes";
import { Actions, Card, Copy, ErrorCopy, RuntimeState } from "./styles";

export function DesktopRuntimeCard({
  workspaceId,
  workspaceName,
  onWorkspaceBound
}: DesktopRuntimeCardProps): ReactElement {
  const runtime = useDesktopRuntime();

  const choose = (): void => {
    if (!workspaceId || !workspaceName) return;
    runtime.chooseWorkspace(workspaceId, workspaceName).then(onWorkspaceBound).catch(() => undefined);
  };

  return (
    <Card>
      <Desktop size={24} weight="duotone" />
      <Copy>
        <strong>Nexo Desktop runtime</strong>
        <span>
          {!runtime.available
            ? "Open this page inside Nexo Desktop to use a project stored on this computer."
            : runtime.state.connected
              ? "Connected. Spring AI can dispatch authorized project tools to this computer."
              : runtime.state.paired
                ? "Paired, but the local runtime is reconnecting to Nexo Server."
                : "Pair this Desktop without exposing its filesystem path to the server."}
        </span>
        <RuntimeState $connected={runtime.state.connected}>
          {runtime.state.connected
            ? <CheckCircle size={14} weight="fill" />
            : <WarningCircle size={14} weight="fill" />}
          {runtime.state.connected ? "Local runtime online" : "Local runtime offline"}
        </RuntimeState>
      </Copy>
      <Actions>
        {runtime.available && !runtime.state.paired && (
          <Button
            type="button"
            variant="outline"
            icon={Link}
            disabled={runtime.pending}
            onClick={(): void => { void runtime.pair().catch(() => undefined); }}
          >
            {runtime.pending ? "Pairing…" : "Pair Desktop"}
          </Button>
        )}
        {runtime.available && runtime.state.paired && workspaceId && (
          <Button type="button" icon={FolderOpen} disabled={runtime.pending} onClick={choose}>
            {runtime.pending ? "Inspecting…" : "Choose local folder"}
          </Button>
        )}
      </Actions>
      {runtime.error && <ErrorCopy role="alert">{runtime.error}</ErrorCopy>}
    </Card>
  );
}
