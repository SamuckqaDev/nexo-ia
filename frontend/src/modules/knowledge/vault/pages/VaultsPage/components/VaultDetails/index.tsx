import { Buildings, File, FileArrowUp, Files, Trash, Vault } from "@phosphor-icons/react";
import { useRef, type ChangeEvent, type ReactElement } from "react";
import { Button } from "../../../../../../../shared/components/Button";
import { Loading } from "../../../../../../../shared/components/Loading";
import {
  WorkspaceBadge,
  WorkspaceEmptyState,
  WorkspacePanel
} from "../../../../../../../shared/components/WorkspacePage";
import { CreateVaultForm } from "../../../../components/CreateVaultForm";
import type { BackendSource } from "../../../../types/backendVaultTypes";
import type { VaultDetailsProps } from "../../../../types/vaultPageTypes";
import {
  FileInput,
  MetaGrid,
  MetaItem,
  SourceAction,
  SourceList,
  SourceRow,
  Summary,
  VaultIdentity
} from "./styles";

const STATUS_LABEL: Record<BackendSource["status"], { label: string; tone: "default" | "positive" | "attention" }> = {
  READY: { label: "Ready", tone: "positive" },
  INGESTING: { label: "Processing", tone: "attention" },
  REGISTERED: { label: "Queued", tone: "attention" },
  UNSUPPORTED: { label: "Not embedded", tone: "attention" },
  FAILED: { label: "Failed", tone: "attention" }
};

const formatBytes = (bytes: number): string =>
  bytes < 1024
    ? `${bytes} B`
    : bytes < 1024 * 1024
      ? `${(bytes / 1024).toFixed(0)} KB`
      : `${(bytes / 1024 / 1024).toFixed(1)} MB`;

export function VaultDetails({
  creating,
  selected,
  sources,
  ownerOptions,
  createPending,
  sourcesLoading,
  uploadPending,
  onCreate,
  onCancelCreate,
  onRemoveVault,
  onUploadSources,
  onRemoveSource
}: VaultDetailsProps): ReactElement {
  const fileInput = useRef<HTMLInputElement>(null);
  const readyCount: number = sources.filter((source: BackendSource) => source.status === "READY").length;
  const addSources = (event: ChangeEvent<HTMLInputElement>): void => {
    onUploadSources(Array.from(event.target.files ?? []));
    event.target.value = "";
  };

  return (
    <WorkspacePanel
      title={creating ? "Create a Knowledge Vault" : selected?.name ?? "Select a Vault"}
      description={creating
        ? "Define ownership and purpose before adding sources."
        : selected?.description ?? undefined}
      action={!creating && selected
        ? selected.manageable
          ? <Button type="button" variant="outline" size="compact" icon={Trash} onClick={(): void => onRemoveVault(selected)}>Delete</Button>
          : <WorkspaceBadge>Read only</WorkspaceBadge>
        : undefined}
    >
      {creating ? (
        <CreateVaultForm
          ownerOptions={ownerOptions}
          pending={createPending}
          onCreate={onCreate}
          onCancel={onCancelCreate}
        />
      ) : selected ? (
        <>
          <Summary>
            <MetaGrid>
              <MetaItem><span>Owner</span><strong>{selected.ownerName}</strong></MetaItem>
              <MetaItem><span>Scope</span><strong>{selected.scope.toLowerCase()}</strong></MetaItem>
              <MetaItem><span>Sources</span><strong>{sources.length}</strong></MetaItem>
              <MetaItem><span>Embedded</span><strong>{readyCount} ready</strong></MetaItem>
            </MetaGrid>
            <SourceAction>
              <FileInput ref={fileInput} type="file" multiple accept=".md,.txt,.json,.csv" onChange={addSources} />
              <Button
                type="button"
                size="compact"
                icon={FileArrowUp}
                disabled={!selected.manageable || uploadPending}
                onClick={(): void => fileInput.current?.click()}
              >
                {selected.manageable ? (uploadPending ? "Uploading…" : "Add sources") : "Team admin only"}
              </Button>
            </SourceAction>
          </Summary>

          {sourcesLoading ? <Loading label="Loading sources…" /> : sources.length ? (
            <SourceList>
              {sources.map((source: BackendSource) => (
                <SourceRow
                  key={source.id}
                  type="button"
                  $active={false}
                  disabled={!selected.manageable}
                  onClick={(): void => onRemoveSource(source.id)}
                  title={selected.manageable
                    ? "Click to remove this source"
                    : "Only a Team administrator can remove this source"}
                >
                  <File size={20} weight="duotone" />
                  <div>
                    <strong>{source.displayName}</strong>
                    <span>{source.mimeType} · {formatBytes(source.byteSize)}</span>
                  </div>
                  <WorkspaceBadge tone={STATUS_LABEL[source.status].tone}>
                    {STATUS_LABEL[source.status].label}
                  </WorkspaceBadge>
                </SourceRow>
              ))}
            </SourceList>
          ) : (
            <WorkspaceEmptyState
              icon={Files}
              title="This Vault has no sources"
              description="Add Markdown, text, JSON or CSV files. They are chunked and embedded into the vector store so Chat can cite them. PDF and Office keep metadata only until a parser is connected."
              action={selected.manageable
                ? <Button type="button" variant="outline" size="compact" icon={FileArrowUp} onClick={(): void => fileInput.current?.click()}>Choose files</Button>
                : <VaultIdentity><Buildings size={15} />Shared by {selected.ownerName}</VaultIdentity>}
            />
          )}
        </>
      ) : (
        <WorkspaceEmptyState
          icon={Vault}
          title="Select a Vault"
          description="Choose a collection from the library or create a new one."
        />
      )}
    </WorkspacePanel>
  );
}
