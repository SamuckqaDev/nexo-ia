import * as DialogPrimitive from "@radix-ui/react-dialog";
import styled from "styled-components";
import type { ConfirmationTone } from "../../types/confirmationTypes";

export const Backdrop = styled(DialogPrimitive.Overlay)`
  position: fixed;
  z-index: 1000;
  inset: 0;
  background: rgba(3, 11, 33, 0.72);
  backdrop-filter: blur(8px);
`;

export const Dialog = styled(DialogPrimitive.Content)`
  position: fixed;
  z-index: 1001;
  top: 50%;
  left: 50%;
  display: grid;
  width: min(30rem, calc(100vw - 2rem));
  grid-template-columns: auto minmax(0, 1fr);
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.xl};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};
  transform: translate(-50%, -50%);
`;

export const CloseButton = styled(DialogPrimitive.Close)`
  position: absolute;
  top: ${({ theme }) => theme.spacing.md};
  right: ${({ theme }) => theme.spacing.md};
  display: grid;
  width: 2.3rem;
  height: 2.3rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surface};
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;

  svg { display: block; }
`;

export const Icon = styled.span<{ $tone: ConfirmationTone }>`
  display: grid;
  width: 3rem;
  height: 3rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.accent};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  line-height: 0;

  svg { display: block; }
`;

export const Copy = styled.div`padding-right: ${({ theme }) => theme.spacing.xl};`;
export const Title = styled(DialogPrimitive.Title)`margin: 0; font-size: 1.15rem;`;
export const Message = styled(DialogPrimitive.Description)`margin: ${({ theme }) => theme.spacing.sm} 0 0; color: ${({ theme }) => theme.colors.textMuted}; font-size: .84rem; line-height: 1.65;`;
export const Actions = styled.div`display: flex; grid-column: 1 / -1; justify-content: flex-end; gap: ${({ theme }) => theme.spacing.sm}; margin-top: ${({ theme }) => theme.spacing.sm};`;
export const CancelButton = styled.button`border: 1px solid ${({ theme }) => theme.colors.line}; border-radius: ${({ theme }) => theme.radius.control}; padding: .7rem 1rem; background: ${({ theme }) => theme.colors.surface}; color: ${({ theme }) => theme.colors.text}; font: inherit; font-size: .8rem; font-weight: 700; cursor: pointer;`;
export const ConfirmButton = styled.button<{ $tone: ConfirmationTone }>`border: 1px solid ${({ theme }) => theme.colors.accent}; border-radius: ${({ theme }) => theme.radius.control}; padding: .7rem 1rem; background: ${({ theme }) => theme.colors.accent}; color: ${({ theme }) => theme.colors.background}; font: inherit; font-size: .8rem; font-weight: 700; cursor: pointer;`;
