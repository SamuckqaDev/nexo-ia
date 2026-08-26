import { CaretDown, CaretRight, File, Folder } from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { useServerWorkspaceTree } from "../../hooks/useServerWorkspaces";
import type { ServerWorkspaceTree } from "../../types/serverWorkspaceTypes";
import { EmptyTree, NodeButton, NodeChildren, TreeRoot } from "./styles";

type DirectoryNodeProps = {
  workspaceId: string;
  path: string;
  name: string;
};

function DirectoryNode({ workspaceId, path, name }: DirectoryNodeProps): ReactElement {
  const [open, setOpen] = useState<boolean>(false);
  const tree = useServerWorkspaceTree(workspaceId, path, open);
  return (
    <li>
      <NodeButton type="button" onClick={(): void => setOpen((current) => !current)}>
        {open ? <CaretDown size={12} /> : <CaretRight size={12} />}
        <Folder size={14} weight={open ? "fill" : "duotone"} />
        <span>{name}</span>
      </NodeButton>
      {open && (
        <NodeChildren>
          {tree.isLoading && <li><EmptyTree>Loading…</EmptyTree></li>}
          {tree.isError && <li><EmptyTree>Directory unavailable</EmptyTree></li>}
          {tree.data?.entries.map((entry) => entry.type === "DIRECTORY" ? (
            <DirectoryNode key={entry.path} workspaceId={workspaceId} path={entry.path} name={entry.name} />
          ) : (
            <li key={entry.path}><NodeButton as="span"><i /><File size={13} /><span>{entry.name}</span></NodeButton></li>
          ))}
        </NodeChildren>
      )}
    </li>
  );
}

export function ServerWorkspaceTree({ workspaceId }: { workspaceId: string }): ReactElement {
  const root = useServerWorkspaceTree(workspaceId, "");
  if (root.isLoading) return <EmptyTree>Loading server workspace structure…</EmptyTree>;
  if (root.isError) return <EmptyTree role="alert">Nexo could not read this server workspace.</EmptyTree>;
  const tree: ServerWorkspaceTree | undefined = root.data;
  if (!tree?.entries.length) return <EmptyTree>This workspace is empty.</EmptyTree>;

  return (
    <TreeRoot>
      {tree.entries.map((entry) => entry.type === "DIRECTORY" ? (
        <DirectoryNode key={entry.path} workspaceId={workspaceId} path={entry.path} name={entry.name} />
      ) : (
        <li key={entry.path}><NodeButton as="span"><i /><File size={13} /><span>{entry.name}</span></NodeButton></li>
      ))}
    </TreeRoot>
  );
}
