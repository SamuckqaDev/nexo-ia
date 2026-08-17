import type { ReactElement } from "react";
import { useSystemStatus } from "../../hooks/useSystemStatus";
import type { SystemResponse, SystemStatusResult } from "../../types/systemTypes";
import { Status, StatusDot } from "./styles";

export function SystemStatus(): ReactElement {
  const status: SystemStatusResult = useSystemStatus();
  const system: SystemResponse | undefined = status.data;
  const isPending: boolean = status.isPending;

  return (
    <Status role="status">
      <StatusDot $online={Boolean(system)} />
      {system
        ? `${system.name} ${system.version} is available`
        : isPending
          ? "Waiting for the Nexo backend"
          : "Nexo backend is unavailable"}
    </Status>
  );
}
