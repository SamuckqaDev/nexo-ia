import styled from "styled-components";
import * as DialogPrimitive from "@radix-ui/react-dialog";

export const Backdrop = styled(DialogPrimitive.Overlay)`
  position: fixed;
  inset: 0;
  background: rgba(3, 11, 33, 0.6);
`;

export const Dialog = styled(DialogPrimitive.Content)`
  position: fixed;
  top: 50%;
  left: 50%;
  width: min(26rem, calc(100vw - 2rem));
  transform: translate(-50%, -50%);
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.lg};
  background: ${({ theme }) => theme.colors.backgroundElevated};
  box-shadow: ${({ theme }) => theme.shadow};
`;

export const Title = styled(DialogPrimitive.Title)`
  margin: 0 0 ${({ theme }) => theme.spacing.xs};
  font-size: 1.05rem;
`;

export const Message = styled(DialogPrimitive.Description)`
  margin: 0 0 ${({ theme }) => theme.spacing.md};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.85rem;
`;

export const Form = styled.form`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacing.sm};
`;
