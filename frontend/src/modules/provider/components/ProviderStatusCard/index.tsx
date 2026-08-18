import { ArrowClockwise, CheckCircle, Cpu, WarningCircle } from "@phosphor-icons/react";
import { useEffect, type ReactElement } from "react";
import { Button } from "../../../../shared/components/Button";
import { Loading } from "../../../../shared/components/Loading";
import { useSessionExpiredStore } from "../../../../shared/auth/sessionExpiredStore";
import { ApiError } from "../../../../shared/api/ApiError";
import { useProviderStatus } from "../../hooks/useProviderStatus";
import { Actions, Content, Dot, Empty, Endpoint, Error, Model, ModelList, StatusLine } from "./styles";

export function ProviderStatusCard(): ReactElement {
  const query = useProviderStatus();
  const status = query.data;
  const openSessionExpired: () => void = useSessionExpiredStore((state) => state.open);

  useEffect((): void => {
    if (query.error instanceof ApiError && query.error.status === 401) openSessionExpired();
  }, [openSessionExpired, query.error]);

  return (
    <Content>
      <StatusLine>
        {query.isError ? <WarningCircle size={19} weight="duotone" /> : <CheckCircle size={19} weight="duotone" />}
        <Dot $connected={Boolean(status?.connected)} />
        {query.isLoading ? "Checking Ollama…" : query.isError ? "Unavailable" : status?.connected ? "Connected" : "Disconnected"}
      </StatusLine>
      {status && <Endpoint><Cpu size={14} /> {status.endpoint}</Endpoint>}
      {query.isError && <Error>Ollama could not be reached. Check that it is running on the configured host.</Error>}
      {query.isLoading && <Loading label="Discovering installed models…" size={44} />}
      {status && status.models.length === 0 && <Empty>No models were discovered.</Empty>}
      {status && status.models.length > 0 && <ModelList>{status.models.map((model) => <Model key={model.name}><span>{model.name}</span><small>{model.size ? `${Math.round(model.size / 1_000_000_000 * 10) / 10} GB` : "Size unknown"}</small></Model>)}</ModelList>}
      <Actions><Button type="button" variant="outline" icon={ArrowClockwise} disabled={query.isFetching} onClick={(): void => { query.refetch(); }}>Refresh</Button></Actions>
    </Content>
  );
}
