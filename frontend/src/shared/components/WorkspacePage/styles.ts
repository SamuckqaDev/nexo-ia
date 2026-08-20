import styled, { css } from "styled-components";

export const Page = styled.section`
  display: grid;
  min-height: 100vh;
  align-content: start;
  gap: ${({ theme }) => theme.spacing.lg};
  padding: clamp(1rem, 2.4vw, 2rem);
  background:
    radial-gradient(circle at 92% 0, ${({ theme }) => theme.colors.surfaceAccent}, transparent 24rem),
    ${({ theme }) => theme.colors.background};
`;

export const Header = styled.header`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.lg};

  @media (max-width: 48rem) { flex-direction: column; }
`;

export const HeaderCopy = styled.div`
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: ${({ theme }) => theme.spacing.md};

  > div > span {
    color: ${({ theme }) => theme.colors.primarySoft};
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.13em;
    text-transform: uppercase;
  }
`;

export const HeaderIcon = styled.span`
  display: grid;
  width: 3rem;
  height: 3rem;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;

export const Title = styled.h1`
  margin: 0.2rem 0 0;
  font-size: clamp(1.5rem, 3vw, 2.15rem);
  line-height: 1.1;
  letter-spacing: -0.04em;
`;

export const Description = styled.p`
  max-width: 48rem;
  margin: 0.45rem 0 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.8rem;
  line-height: 1.55;
`;

export const HeaderActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const Panel = styled.section`
  min-width: 0;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surface};
`;

export const PanelHeader = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
`;

export const PanelCopy = styled.div`
  min-width: 0;
  h2 { margin: 0; font-size: 0.9rem; }
  p { margin: 0.2rem 0 0; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.68rem; line-height: 1.45; }
`;

export const PanelBody = styled.div`min-width: 0;`;

export const Badge = styled.span<{ $tone: "default" | "positive" | "attention" }>`
  display: inline-flex;
  align-items: center;
  width: max-content;
  border-radius: ${({ theme }) => theme.radius.round};
  padding: 0.3rem 0.55rem;
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.62rem;
  font-weight: 700;
  white-space: nowrap;

  ${({ theme, $tone }) => $tone === "positive" && css`background: color-mix(in srgb, ${theme.colors.statusOnline} 14%, transparent); color: ${theme.colors.statusOnline};`}
  ${({ theme, $tone }) => $tone === "attention" && css`background: ${theme.colors.dangerSurface}; color: ${theme.colors.accentSoft};`}
`;

export const Segments = styled.div`
  display: inline-flex;
  gap: 0.2rem;
  padding: 0.22rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
`;

export const SegmentButton = styled.button<{ $active: boolean }>`
  border: 0;
  border-radius: 0.6rem;
  padding: 0.45rem 0.65rem;
  background: ${({ theme, $active }) => ($active ? theme.colors.surfaceAccent : "transparent")};
  color: ${({ theme, $active }) => ($active ? theme.colors.primarySoft : theme.colors.textMuted)};
  font: inherit;
  font-size: 0.66rem;
  font-weight: 700;
  cursor: pointer;
`;

export const Empty = styled.div`
  display: grid;
  justify-items: center;
  min-height: 15rem;
  place-content: center;
  padding: ${({ theme }) => theme.spacing.xl};
  text-align: center;

  strong { margin-top: ${({ theme }) => theme.spacing.sm}; font-size: 0.88rem; }
  p { max-width: 28rem; margin: 0.35rem 0 ${({ theme }) => theme.spacing.md}; color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.72rem; line-height: 1.55; }
`;

export const EmptyIcon = styled.span`
  display: grid;
  width: 3.2rem;
  height: 3.2rem;
  place-items: center;
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;
