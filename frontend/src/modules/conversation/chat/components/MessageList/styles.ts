import styled from "styled-components";

export const Messages = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.lg};
  overflow-y: auto;
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
