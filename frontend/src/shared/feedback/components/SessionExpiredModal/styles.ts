import styled from "styled-components";

export const Backdrop = styled.div`
  position: fixed;
  z-index: 40;
  inset: 0;
  background: rgba(3, 11, 33, 0.78);
  backdrop-filter: blur(0.35rem);
`;

export const Dialog = styled.div`
  position: fixed;
  z-index: 41;
  top: 50%;
  left: 50%;
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  width: min(28rem, calc(100vw - 2rem));
  transform: translate(-50%, -50%);
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.xl};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};
`;

export const Icon = styled.div`
  display: grid;
  width: 3rem;
  height: 3rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;

export const Title = styled.h2`margin: 0;font-size: 1.15rem;`;
export const Message = styled.p`margin: 0;color: ${({ theme }) => theme.colors.textMuted};font-size: .8rem;line-height: 1.6;`;
export const Form = styled.form`display: grid;gap: ${({ theme }) => theme.spacing.md};`;
export const Actions = styled.div`display: flex;justify-content: flex-end;gap: ${({ theme }) => theme.spacing.sm};`;
