import styled from "styled-components";

export const Context = styled.div<{ $active: boolean }>`
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
  max-width: 18rem;
  overflow: hidden;
  border: 1px solid ${({ theme, $active }) => ($active ? theme.colors.lineStrong : theme.colors.line)};
  border-radius: ${({ theme }) => theme.radius.round};
  padding: 0.35rem 0.28rem 0.35rem 0.6rem;
  background: ${({ theme, $active }) => ($active ? theme.colors.surfaceAccent : "transparent")};
  color: ${({ theme, $active }) => ($active ? theme.colors.primarySoft : theme.colors.textSubtle)};
  font-size: 0.74rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  &:hover, &:focus-within { color: ${({ theme }) => theme.colors.primary}; }
`;

export const WorkspaceSelect = styled.select`
  max-width: 13rem;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  font-size: inherit;
  font-weight: inherit;
  outline: 0;
  cursor: pointer;
  text-overflow: ellipsis;
  &:disabled { cursor: not-allowed; opacity: 0.55; }
`;

export const FolderButton = styled.button<{ $pending: boolean }>`
  display: inline-grid;
  place-items: center;
  align-self: stretch;
  border: 0;
  border-left: 1px solid ${({ theme }) => theme.colors.line};
  padding: 0.15rem 0.32rem 0.15rem 0.5rem;
  background: transparent;
  color: inherit;
  cursor: pointer;
  &:hover:not(:disabled), &:focus-visible { color: ${({ theme }) => theme.colors.primary}; }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: 1px; }
  &:disabled { cursor: not-allowed; opacity: 0.55; }
  svg { animation: ${({ $pending }) => $pending ? "nexo-workspace-spin 0.9s linear infinite" : "none"}; }
  @keyframes nexo-workspace-spin { to { transform: rotate(360deg); } }
`;
