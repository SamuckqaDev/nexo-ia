import { ArrowClockwise, ArrowsIn, ArrowsOut, DotsSixVertical, X } from "@phosphor-icons/react";
import {
  useEffect,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
  type ReactPortal
} from "react";
import { createPortal } from "react-dom";
import { Button } from "../../../../../shared/components/Button";
import { Loading } from "../../../../../shared/components/Loading";
import type {
  KnowledgeGraphNode,
  VaultWorkbenchDragSnapshot,
  VaultWorkbenchModalProps,
  VaultWorkbenchPosition
} from "../../types/vaultGraphTypes";
import { VaultKnowledgeGraph } from "../VaultKnowledgeGraph";
import {
  Backdrop,
  DragHandle,
  WindowBody,
  WindowButton,
  WindowControls,
  WindowFrame,
  WindowState,
  WindowStatus,
  WindowTitle,
  WindowTitlebar
} from "./styles";

const clamp = (value: number, minimum: number, maximum: number): number =>
  Math.min(Math.max(value, minimum), maximum);

export function VaultWorkbenchModal({
  open,
  onClose,
  graphQuery,
  selectedVaultId,
  onSelectVault
}: VaultWorkbenchModalProps): ReactPortal | null {
  const [position, setPosition] = useState<VaultWorkbenchPosition>({ x: 0, y: 0 });
  const [maximized, setMaximized] = useState<boolean>(false);
  const frameRef = useRef<HTMLDivElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const dragRef = useRef<VaultWorkbenchDragSnapshot | null>(null);
  const selectedVaultName: string = graphQuery.data?.nodes.find((node: KnowledgeGraphNode) =>
    node.kind === "VAULT" && node.vaultId === selectedVaultId)?.label ?? "All Vaults";

  useEffect(() => {
    if (!open) return;
    setPosition({ x: 0, y: 0 });
    setMaximized(false);
    const focusFrame: number = window.requestAnimationFrame((): void => closeButtonRef.current?.focus());
    const closeOnEscape = (event: KeyboardEvent): void => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", closeOnEscape);
    return (): void => {
      window.cancelAnimationFrame(focusFrame);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [onClose, open]);

  if (!open || typeof document === "undefined") return null;

  const startDragging = (event: ReactPointerEvent<HTMLElement>): void => {
    if (maximized || (event.target as HTMLElement).closest("button")) return;
    const frame: HTMLDivElement | null = frameRef.current;
    if (!frame) return;
    const bounds: DOMRect = frame.getBoundingClientRect();
    dragRef.current = {
      pointerId: event.pointerId,
      pointerX: event.clientX,
      pointerY: event.clientY,
      position,
      minX: position.x + 12 - bounds.left,
      maxX: position.x + window.innerWidth - 12 - bounds.right,
      minY: position.y + 12 - bounds.top,
      maxY: position.y + window.innerHeight - 12 - bounds.bottom
    };
    event.currentTarget.setPointerCapture(event.pointerId);
    event.preventDefault();
  };

  const moveWindow = (event: ReactPointerEvent<HTMLElement>): void => {
    const drag: VaultWorkbenchDragSnapshot | null = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) return;
    setPosition({
      x: clamp(drag.position.x + event.clientX - drag.pointerX, drag.minX, drag.maxX),
      y: clamp(drag.position.y + event.clientY - drag.pointerY, drag.minY, drag.maxY)
    });
  };

  const stopDragging = (event: ReactPointerEvent<HTMLElement>): void => {
    if (dragRef.current?.pointerId !== event.pointerId) return;
    dragRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
  };

  const toggleMaximized = (): void => {
    setMaximized((current: boolean): boolean => !current);
    setPosition({ x: 0, y: 0 });
  };

  return createPortal(
    <Backdrop onPointerDown={(event): void => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <WindowFrame
        ref={frameRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="vault-workbench-title"
        $x={position.x}
        $y={position.y}
        $maximized={maximized}
      >
        <WindowTitlebar
          onPointerDown={startDragging}
          onPointerMove={moveWindow}
          onPointerUp={stopDragging}
          onPointerCancel={stopDragging}
        >
          <DragHandle aria-hidden><DotsSixVertical size={19} weight="bold" /></DragHandle>
          <WindowTitle>
            <strong id="vault-workbench-title">Semantic Knowledge Workbench</strong>
            <span>{selectedVaultName} · drag, resize or maximize the graph</span>
          </WindowTitle>
          <WindowControls>
            <WindowButton type="button" aria-label={maximized ? "Restore workbench" : "Maximize workbench"} onClick={toggleMaximized}>
              {maximized ? <ArrowsIn size={17} /> : <ArrowsOut size={17} />}
            </WindowButton>
            <WindowButton ref={closeButtonRef} type="button" aria-label="Close knowledge workbench" onClick={onClose}>
              <X size={18} />
            </WindowButton>
          </WindowControls>
        </WindowTitlebar>
        <WindowBody>
          {graphQuery.isLoading ? (
            <WindowState><Loading label="Mapping your indexed knowledge…" /></WindowState>
          ) : graphQuery.isError ? (
            <WindowState>
              <strong>Could not load the knowledge graph</strong>
              <span>{graphQuery.error.message}</span>
              <Button type="button" variant="outline" icon={ArrowClockwise} onClick={(): void => { void graphQuery.refetch(); }}>Try again</Button>
            </WindowState>
          ) : graphQuery.data ? (
            <VaultKnowledgeGraph
              graph={graphQuery.data}
              selectedVaultId={selectedVaultId}
              onSelectVault={onSelectVault}
            />
          ) : null}
        </WindowBody>
        <WindowStatus>
          <span>{graphQuery.data?.vaultCount ?? 0} Vaults · {graphQuery.data?.sourceCount ?? 0} documents</span>
          <span>{graphQuery.data?.chunkCount ?? 0} chunks{graphQuery.data?.truncated ? " · bounded view" : ""}</span>
        </WindowStatus>
      </WindowFrame>
    </Backdrop>,
    document.body
  );
}
