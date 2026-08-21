import styled from "styled-components";

export const Messages = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  gap: ${({ theme }) => theme.spacing.lg};
  padding: ${({ theme }) => `${theme.spacing.lg} ${theme.spacing.xl}`};
  overflow-y: auto;
  overflow-x: hidden;

  @media (max-width: 48rem) {
    padding: ${({ theme }) => theme.spacing.md};
  }
`;

/**
 * Announces streaming lifecycle changes once, without re-reading the growing answer. The message
 * bubble itself is no longer a live region, so a screen reader hears "responding" or "complete"
 * instead of the entire partial text on every token.
 */
export const StatusLive = styled.p`
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  border: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
`;

export const TokenSummary = styled.div`
  position: sticky;
  z-index: 1;
  top: 0;
  display: flex;
  width: fit-content;
  align-self: center;
  align-items: center;
  gap: 0.55rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.round};
  padding: 0.34rem 0.65rem;
  background: ${({ theme }) => theme.colors.backgroundElevated};
  box-shadow: 0 7px 20px rgba(0, 0, 0, 0.14);
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  font-variant-numeric: tabular-nums;
  strong { color: ${({ theme }) => theme.colors.primarySoft}; }
  @media (max-width: 32rem) { flex-wrap: wrap; justify-content: center; }
`;

export const RunStatus = styled.div`
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  align-self: center;
  align-items: center;
  gap: 0.3rem;
  border: 0;
  padding: 0 0.25rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.primary};
  opacity: 0.82;
  > svg { flex: 0 0 auto; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  @media (prefers-reduced-motion: reduce) { > svg { animation: none; } }
`;

export const ThinkingIndicator = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
`;

export const ThinkingLogo = styled.img`
  width: 1.25rem;
  height: 1.25rem;
  object-fit: contain;
`;

export const ThinkingDots = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;

  i {
    width: 0.28rem;
    height: 0.28rem;
    border-radius: 50%;
    background: ${({ theme }) => theme.colors.accent};
    animation: nexo-thinking-dot 1.1s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.14s; }
    &:nth-child(3) { animation-delay: 0.28s; }
  }

  @keyframes nexo-thinking-dot {
    0%, 70%, 100% { opacity: 0.25; transform: translateY(0); }
    35% { opacity: 1; transform: translateY(-0.18rem); }
  }

  @media (prefers-reduced-motion: reduce) { i { animation: none; } }
`;

export const RunTimer = styled.time`
  display: inline;
  flex: 0 0 auto;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.6rem;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  letter-spacing: 0.02em;
`;

export const Empty = styled.div`
  display: grid;
  width: min(42rem, 100%);
  justify-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
  margin: auto;
  color: ${({ theme }) => theme.colors.textMuted};
  text-align: center;
  >span { max-width: 34rem; font-size: .78rem; line-height: 1.65; }
`;

export const EmptyIcon = styled.span<{ $agent:boolean }>`display:grid;width:3.8rem;height:3.8rem;margin-bottom:${({theme})=>theme.spacing.sm};place-items:center;border:1px solid ${({theme,$agent})=>$agent?theme.colors.accent:theme.colors.lineStrong};border-radius:${({theme})=>theme.radius.md};background:${({theme,$agent})=>$agent?theme.colors.dangerSurface:theme.colors.surfaceAccent};color:${({theme,$agent})=>$agent?theme.colors.accentSoft:theme.colors.primary};box-shadow:0 12px 35px ${({theme,$agent})=>$agent?theme.colors.dangerSurface:theme.colors.surfaceAccent};`;
export const EmptyKicker = styled.span`color:${({theme})=>theme.colors.primarySoft}!important;font-size:.64rem!important;font-weight:700;letter-spacing:.13em;text-transform:uppercase;`;

export const EmptyTitle = styled.p`
  margin: 0 0 ${({ theme }) => theme.spacing.xs};
  color: ${({ theme }) => theme.colors.text};
  font-size: clamp(1.3rem, 3vw, 2rem);
  font-weight: 700;
  letter-spacing: -.035em;
`;

export const FeatureGrid = styled.div`display:grid;width:100%;grid-template-columns:repeat(3,minmax(0,1fr));gap:${({theme})=>theme.spacing.sm};margin-top:${({theme})=>theme.spacing.lg};@media(max-width:40rem){grid-template-columns:1fr;}`;
export const Feature = styled.div`display:grid;justify-items:start;gap:.3rem;padding:${({theme})=>theme.spacing.sm};border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.surface};color:${({theme})=>theme.colors.primary};text-align:left;strong{color:${({theme})=>theme.colors.text};font-size:.72rem;}small{color:${({theme})=>theme.colors.textSubtle};font-size:.62rem;line-height:1.45;}`;

export const StreamError = styled.p`
  align-self: center;
  max-width: 32rem;
  border: 1px solid ${({ theme }) => theme.colors.danger};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => `${theme.spacing.xs} ${theme.spacing.sm}`};
  margin: 0;
  background: ${({ theme }) => theme.colors.dangerSurface};
  color: ${({ theme }) => theme.colors.danger};
  font-size: 0.8rem;
  text-align: center;
`;

export const ThinkingTrace = styled.details`
  width: min(46rem, calc(100% - 3rem));
  align-self: flex-start;
  padding-left: ${({ theme }) => theme.spacing.md};
  border-left: 2px solid ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.textMuted};

  summary {
    display: flex;
    align-items: center;
    gap: .4rem;
    color: ${({ theme }) => theme.colors.primarySoft};
    font-size: .72rem;
    font-weight: 700;
    cursor: pointer;
    list-style: none;

    &::-webkit-details-marker { display: none; }
    small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: .62rem; font-weight: 500; }
  }

  p {
    max-height: 11rem;
    margin: ${({ theme }) => `${theme.spacing.xs} 0 0`};
    overflow: auto;
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: .72rem;
    line-height: 1.65;
    white-space: pre-wrap;
  }

  @media (max-width: 48rem) { width: calc(100% - 1rem); }
`;
