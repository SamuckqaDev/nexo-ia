import { ArrowClockwise, CheckCircle, Cpu, WarningCircle } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../../shared/components/Button";
import { Loading } from "../../../../shared/components/Loading";
import { useQueryClient, type QueryClient } from "@tanstack/react-query";
import { useProviderModelCatalogs } from "../../hooks/useProviderModelCatalogs";
import { useProviderRegistry } from "../../hooks/useProviderRegistry";
import type { ProviderConfiguration, ProviderModelCatalogView } from "../../types/providerConfigurationTypes";
import { Actions, Content, Dot, Empty, Endpoint, Error, Model, ModelList, StatusLine } from "./styles";

export function ProviderStatusCard(): ReactElement {
  const { registry } = useProviderRegistry();
  const queryClient: QueryClient = useQueryClient();
  const providers: ProviderConfiguration[] = registry.data?.filter((provider) => provider.enabled && provider.providerType === "OLLAMA") ?? [];
  const catalogs: ProviderModelCatalogView[] = useProviderModelCatalogs(providers);
  const provider: ProviderConfiguration | undefined = providers[0];
  const catalog: ProviderModelCatalogView | undefined = catalogs[0];
  const loading: boolean = registry.isLoading || catalog?.status === "LOADING";
  const connected: boolean = catalog?.status === "AVAILABLE";
  const unavailable: boolean = registry.isError || catalog?.status === "UNAVAILABLE";

  return (
    <Content>
      <StatusLine>
        {unavailable ? <WarningCircle size={19} weight="duotone" /> : <CheckCircle size={19} weight="duotone" />}
        <Dot $connected={connected} />
        {loading ? "Checking Ollama…" : unavailable ? "Unavailable" : connected ? "Connected" : "Disconnected"}
      </StatusLine>
      {provider && <Endpoint><Cpu size={14} /> {provider.endpoint}</Endpoint>}
      {unavailable && <Error>Ollama could not be reached using the saved provider configuration.</Error>}
      {loading && <Loading label="Discovering installed models…" size={44} />}
      {catalog && catalog.models.length === 0 && <Empty>No models were discovered.</Empty>}
      {catalog && catalog.models.length > 0 && <ModelList>{catalog.models.map((model) => <Model key={model.name}><span>{model.name}</span><small>{model.size ? `${Math.round(model.size / 1_000_000_000 * 10) / 10} GB` : "Size unknown"}</small></Model>)}</ModelList>}
      <Actions><Button type="button" variant="outline" icon={ArrowClockwise} disabled={loading} onClick={(): void => {
        queryClient.invalidateQueries({ queryKey: ["providers"] });
      }}>Refresh</Button></Actions>
    </Content>
  );
}
