import styled from "styled-components";

export const ContextPanel = styled.section<{ $blocked: boolean }>`
  display: grid;
  gap: 0.35rem;
  padding: 0.45rem ${({ theme }) => theme.spacing.md};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme, $blocked }) => $blocked
    ? theme.colors.dangerSurface
    : theme.colors.surfaceAccent};
`;

export const ContextHeading = styled.header`
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.35rem;
  color: ${({ theme }) => theme.colors.primary};

  strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 0.65rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }
`;

export const ContextStatus = styled.span<{ $ready: boolean }>`
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 0.25rem;
  margin-left: auto;
  overflow: hidden;
  color: ${({ theme, $ready }) => $ready ? theme.colors.primarySoft : theme.colors.accentSoft};
  font-size: 0.58rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const ContextGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.35rem;

  @media (max-width: 34rem) {
    grid-template-columns: 1fr;
  }
`;

export const ContextAction = styled.button<{ $ready: boolean }>`
  display: grid;
  min-width: 0;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.4rem;
  border: 1px solid ${({ theme, $ready }) => $ready ? theme.colors.lineStrong : theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.38rem 0.5rem;
  background: ${({ theme }) => theme.colors.backgroundSoft};
  color: ${({ theme, $ready }) => $ready ? theme.colors.primary : theme.colors.textSubtle};
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:hover, &:focus-visible {
    border-color: ${({ theme }) => theme.colors.primary};
  }
`;

export const ContextCopy = styled.span`
  display: grid;
  min-width: 0;
  gap: 0.05rem;

  strong, span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.62rem; }
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.56rem; }
`;
