import styled from "styled-components";

export const Stack = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.lg};
`;

export const Intro = styled.header`
  display: grid;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const Heading = styled.h1`
  margin: 0;
  font-size: clamp(1.7rem, 3vw, 2.4rem);
`;

export const Description = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.88rem;
  line-height: 1.6;
`;

export const Section = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.lg};
  padding: ${({ theme }) => theme.spacing.lg};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
`;

export const SectionHeader = styled.header`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const SectionIcon = styled.span`
  display: grid;
  width: 2.7rem;
  height: 2.7rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;

export const Title = styled.h2`
  margin: 0 0 0.2rem;
  font-size: 1.05rem;
`;

export const ProfileContent = styled.div`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: ${({ theme }) => theme.spacing.lg};

  @media (max-width: 36rem) {
    grid-template-columns: 1fr;
  }
`;

export const DataGrid = styled.dl`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.sm};
  margin: 0;

  @media (max-width: 42rem) {
    grid-template-columns: 1fr;
  }
`;

export const DataItem = styled.div`
  min-width: 0;
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surface};

  span,
  strong {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    margin-bottom: 0.3rem;
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.72rem;
  }

  strong {
    font-size: 0.86rem;
  }
`;

export const Status = styled.span`
  padding: 0.35rem 0.65rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  border-color: ${({ theme }) => theme.colors.accent};
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.accentSoft};
  font-size: 0.72rem;
  font-weight: 700;
`;

export const PendingState = styled.p`
  margin: 0;
  padding: ${({ theme }) => theme.spacing.md};
  border: 1px dashed ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.84rem;
`;
