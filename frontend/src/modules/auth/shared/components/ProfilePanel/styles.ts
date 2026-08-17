import styled from "styled-components";

export const Panel = styled.section`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.lg};
  margin-top: ${({ theme }) => theme.spacing.xl};
  padding: ${({ theme }) => theme.spacing.lg};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surface};
`;

export const Copy = styled.div``;

export const Label = styled.p`
  margin: 0 0 0.25rem;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
`;

export const Name = styled.strong`
  display: block;
  font-size: 1.2rem;
`;

export const Meta = styled.span`
  color: ${({ theme }) => theme.colors.primary};
  font-size: 0.85rem;
`;
