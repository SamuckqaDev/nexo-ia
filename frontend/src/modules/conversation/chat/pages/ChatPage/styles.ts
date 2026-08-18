import styled from "styled-components";

export const Layout = styled.section`
  display: grid;
  grid-template-columns: 17rem minmax(0, 1fr);
  min-height: calc(100vh - 7rem);
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surface};
  overflow: hidden;

  @media (max-width: 48rem) {
    grid-template-columns: 1fr;
  }
`;

export const Chat = styled.div`
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 0;
`;

export const Header = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => `${theme.spacing.md} ${theme.spacing.lg}`};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};

  @media (max-width: 48rem) {
    align-items: flex-start;
    flex-direction: column;
  }
`;

export const Title = styled.h2`
  overflow: hidden;
  margin: 0;
  font-size: 1rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Controls = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const ModeButton = styled.button<{ $active: boolean }>`
  border: 1px solid ${({ theme, $active }) => ($active ? theme.colors.primary : theme.colors.line)};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.42rem 0.7rem;
  background: ${({ theme, $active }) => ($active ? theme.colors.surfaceAccent : "transparent")};
  color: ${({ theme, $active }) => ($active ? theme.colors.primarySoft : theme.colors.textMuted)};
  font: inherit;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 1px;
  }
`;

export const CapabilityNote = styled.p`
  margin: 0;
  padding: ${({ theme }) => `${theme.spacing.xs} ${theme.spacing.lg}`};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.74rem;
`;

export const LoadFailure = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  justify-items: center;
  margin: auto;
  padding: ${({ theme }) => theme.spacing.xl};
  color: ${({ theme }) => theme.colors.textMuted};
  text-align: center;
`;
