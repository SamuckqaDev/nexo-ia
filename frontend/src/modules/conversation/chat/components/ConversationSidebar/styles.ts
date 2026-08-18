import styled from "styled-components";

export const Sidebar = styled.aside`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
  border-right: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  overflow-y: auto;

  @media (max-width: 48rem) {
    border-right: 0;
    border-bottom: 1px solid ${({ theme }) => theme.colors.line};
    max-height: 14rem;
  }
`;

export const List = styled.div`
  display: grid;
  gap: 0.3rem;
`;

export const Item = styled.div<{ $active: boolean }>`
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.25rem;
  border: 1px solid ${({ theme, $active }) => ($active ? theme.colors.lineStrong : "transparent")};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme, $active }) => ($active ? theme.colors.surfaceStrong : "transparent")};

  &:hover {
    background: ${({ theme }) => theme.colors.surfaceStrong};
  }
`;

export const Open = styled.button<{ $active: boolean }>`
  overflow: hidden;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.65rem 0.7rem;
  background: transparent;
  color: ${({ theme, $active }) => ($active ? theme.colors.primarySoft : theme.colors.text)};
  font: inherit;
  font-size: 0.85rem;
  font-weight: ${({ $active }) => ($active ? 700 : 400)};
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: -2px;
  }
`;

export const Archive = styled.button`
  display: flex;
  align-items: center;
  padding: 0.5rem;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.danger};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
  }
`;

export const Empty = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.78rem;
`;
