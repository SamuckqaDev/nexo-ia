import { PencilSimple, Trash } from "@phosphor-icons/react";
import { useEffect, useState, type ReactElement } from "react";
import { Button } from "../../../../shared/components/Button";
import { ApiError } from "../../../../shared/api/ApiError";
import { useSessionExpiredStore } from "../../../../shared/auth/sessionExpiredStore";
import { Loading } from "../../../../shared/components/Loading";
import { useConfirmationStore } from "../../../../shared/feedback/stores/useConfirmationStore";
import type { ConfirmationState } from "../../../../shared/feedback/types/confirmationTypes";
import { useProviderRegistry } from "../../hooks/useProviderRegistry";
import type { ProviderConfiguration } from "../../types/providerConfigurationTypes";
import { ProviderSetup } from "../ProviderSetup";
import { Actions, Card, Error, Header, Item, List, Meta, Title } from "./styles";

export function ProviderRegistry(): ReactElement {
  const { registry, remove } = useProviderRegistry();
  const [editing, setEditing] = useState<ProviderConfiguration | undefined>();
  const [adding, setAdding] = useState<boolean>(false);
  const ask: ConfirmationState["ask"] = useConfirmationStore((state: ConfirmationState) => state.ask);
  const openSessionExpired: () => void = useSessionExpiredStore((state) => state.open);

  useEffect((): void => {
    if (registry.error instanceof ApiError && registry.error.status === 401) openSessionExpired();
  }, [openSessionExpired, registry.error]);

  if (registry.isLoading) return <Loading label="Finding your providers…" />;
  if (registry.isError) {
    return <Error>We could not load your providers. <Button type="button" variant="outline" onClick={(): void => { registry.refetch(); }}>Try again</Button></Error>;
  }
  if (!registry.data?.length) return <ProviderSetup />;

  return (
    <Card>
      <Header>
        <div>
          <Title>Your providers</Title>
          <Meta>Each provider belongs only to your account.</Meta>
        </div>
        <Button type="button" variant="outline" onClick={(): void => { setEditing(undefined); setAdding(true); }}>Add provider</Button>
      </Header>
      {editing && <ProviderSetup provider={editing} onSaved={(): void => setEditing(undefined)} />}
      {adding && <ProviderSetup onSaved={(): void => setAdding(false)} />}
      <List>
        {registry.data.map((provider) => (
          <Item key={provider.id}>
            <div>
              <strong>{provider.displayName}</strong>
              <Meta>{provider.providerType} · {provider.endpoint}</Meta>
              <Meta>Model: {provider.selectedModel || "Not selected"}</Meta>
            </div>
            <Actions>
              <Button type="button" variant="outline" icon={PencilSimple} onClick={(): void => { setAdding(false); setEditing(provider); }}>Edit</Button>
              <Button type="button" variant="outline" icon={Trash} onClick={(): void => {
                ask({ title: "Remove this provider?", message: "The provider configuration will be removed from your account.", confirmLabel: "Remove provider", tone: "danger" })
                  .then((confirmed: boolean) => { if (confirmed) remove(provider.id); });
              }}>Remove</Button>
            </Actions>
          </Item>
        ))}
      </List>
    </Card>
  );
}
