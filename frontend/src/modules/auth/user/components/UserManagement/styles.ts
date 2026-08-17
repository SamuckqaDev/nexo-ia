import styled from "styled-components";

export const Panel = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.lg};
  width: 100%;
  margin-top: ${({ theme }) => theme.spacing.xl};
  padding: ${({ theme }) => theme.spacing.lg};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
`;

export const Header = styled.header`
  display: grid;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const Title = styled.h2`
  margin: 0;
  font-size: 1.25rem;
`;

export const Description = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.9rem;
  line-height: 1.6;
`;

export const Form = styled.form`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.md};
  align-items: start;
  padding-bottom: ${({ theme }) => theme.spacing.lg};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};

  > button {
    grid-column: 1 / -1;
    width: 100%;
  }

  @media (max-width: 38rem) {
    grid-template-columns: 1fr;
  }
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
  grid-template-columns: minmax(0, 1fr) 8.5rem 8.5rem;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: 0.9rem;
  background: ${({ theme }) => theme.colors.surface};

  > button {
    width: 100%;
  }

  @media (max-width: 38rem) {
    grid-template-columns: 1fr;
  }
`;

export const Identity = styled.div`
  min-width: 0;
`;

export const Name = styled.strong`
  display: block;
`;

export const Meta = styled.span`
  display: block;
  overflow: hidden;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.76rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Badge = styled.span<{ $active: boolean }>`
  display: inline-block;
  margin-left: ${({ theme }) => theme.spacing.xs};
  padding: 0.15rem 0.45rem;
  border-radius: ${({ theme }) => theme.radius.round};
  background: ${({ theme, $active }) =>
    $active ? theme.colors.statusOnline : theme.colors.dangerSurface};
  color: ${({ theme, $active }) =>
    $active ? theme.colors.background : theme.colors.danger};
  font-size: 0.65rem;
  font-weight: 700;
`;
