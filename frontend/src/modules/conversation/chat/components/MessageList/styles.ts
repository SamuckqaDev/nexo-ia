import styled from "styled-components";

export const Messages = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.lg};
  overflow-y: auto;
`;

export const Empty = styled.div`
  margin: auto;
  max-width: 26rem;
  color: ${({ theme }) => theme.colors.textMuted};
  text-align: center;
`;

export const EmptyTitle = styled.p`
  margin: 0 0 ${({ theme }) => theme.spacing.xs};
  color: ${({ theme }) => theme.colors.text};
  font-weight: 700;
`;

export const StreamError = styled.p`
  align-self: center;
  max-width: 32rem;
  border: 1px solid ${({ theme }) => theme.colors.danger};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => `${theme.spacing.xs} ${theme.spacing.sm}`};
  margin: 0;
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.danger};
  font-size: 0.8rem;
  text-align: center;
`;
