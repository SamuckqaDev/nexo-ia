import styled from "styled-components";

export const Panel = styled.section`
  margin-top: ${({ theme }) => theme.spacing.lg};
  padding: ${({ theme }) => theme.spacing.lg};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};
`;

export const Header = styled.header`
  display: flex;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.md};
  align-items: start;
  margin-bottom: ${({ theme }) => theme.spacing.lg};
`;

export const Heading = styled.h2`
  margin: 0;
  font-size: 1.2rem;
`;

export const Description = styled.p`
  margin: ${({ theme }) => theme.spacing.xs} 0 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.88rem;
`;

export const Count = styled.span`
  flex: 0 0 auto;
  padding: 0.35rem 0.7rem;
  border-radius: ${({ theme }) => theme.radius.round};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.78rem;
  font-weight: 700;
`;

export const List = styled.ul`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  margin: 0;
  padding: 0;
  list-style: none;
`;

export const Item = styled.li`
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: 0.9rem;
  background: ${({ theme }) => theme.colors.surface};

  @media (max-width: 38rem) {
    grid-template-columns: auto 1fr;
  }
`;

export const DeviceIcon = styled.span`
  display: grid;
  width: 2.6rem;
  height: 2.6rem;
  place-items: center;
  border-radius: 0.8rem;
  background: ${({ theme }) => theme.colors.backgroundElevated};
  color: ${({ theme }) => theme.colors.primary};
`;

export const Details = styled.div`
  min-width: 0;
`;

export const DeviceName = styled.strong`
  display: block;
  color: ${({ theme }) => theme.colors.text};
  font-size: 0.92rem;
`;

export const Metadata = styled.span`
  display: block;
  overflow: hidden;
  margin-top: 0.25rem;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.76rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const CurrentBadge = styled.span`
  display: inline-block;
  margin-left: ${({ theme }) => theme.spacing.xs};
  padding: 0.15rem 0.45rem;
  border-radius: ${({ theme }) => theme.radius.round};
  background: ${({ theme }) => theme.colors.statusOnline};
  color: ${({ theme }) => theme.colors.background};
  font-size: 0.65rem;
  font-weight: 700;
  vertical-align: middle;
`;

export const State = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textMuted};
`;

export const Action = styled.div`
  @media (max-width: 38rem) {
    grid-column: 1 / -1;

    button {
      width: 100%;
    }
  }
`;
