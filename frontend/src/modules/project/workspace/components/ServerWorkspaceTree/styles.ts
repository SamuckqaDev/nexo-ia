import styled from "styled-components";

export const TreeRoot = styled.ul`
  display: grid;
  max-height: 24rem;
  overflow: auto;
  margin: 0;
  padding: 0.35rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
  list-style: none;
`;

export const NodeChildren = styled.ul`
  display: grid;
  margin: 0 0 0 0.72rem;
  padding: 0 0 0 0.48rem;
  border-left: 1px solid ${({ theme }) => theme.colors.line};
  list-style: none;
`;

export const NodeButton = styled.button`
  display: grid;
  grid-template-columns: 0.8rem 1rem minmax(0, 1fr);
  align-items: center;
  gap: 0.28rem;
  width: 100%;
  min-height: 1.85rem;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.2rem 0.35rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textMuted};
  font: inherit;
  font-size: 0.66rem;
  text-align: left;
  cursor: pointer;
  > span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &:hover { background: ${({ theme }) => theme.colors.surfaceAccent}; color: ${({ theme }) => theme.colors.primary}; }
`;

export const EmptyTree = styled.p`
  margin: 0;
  padding: ${({ theme }) => theme.spacing.md};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.65rem;
`;
