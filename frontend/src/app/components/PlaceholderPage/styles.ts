import styled from "styled-components";

export const Page = styled.section`
  display: grid;
  width: 100%;
  min-height: 100vh;
  align-content: start;
  gap: ${({ theme }) => theme.spacing.lg};
  padding: clamp(1rem, 3vw, 2.5rem);
  background: radial-gradient(circle at 80% 5%, ${({ theme }) => theme.colors.surfaceAccent}, transparent 28rem);
`;

export const Header = styled.header`
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.lg};
`;

export const Eyebrow = styled.span`
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
`;

export const Title = styled.h1`margin:.3rem 0 0;font-size:clamp(1.8rem,4vw,3rem);letter-spacing:-.04em;`;
export const Release = styled.span`padding:.4rem .65rem;border-radius:${({theme})=>theme.radius.round};background:${({theme})=>theme.colors.dangerSurface};color:${({theme})=>theme.colors.accentSoft};font-size:.65rem;font-weight:700;text-transform:uppercase;letter-spacing:.08em;`;

export const Hero = styled.section`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xl};
  min-height: 16rem;
  padding: clamp(1.25rem, 4vw, 3rem);
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};

  @media (max-width: 38rem) { grid-template-columns: 1fr; }
`;

export const Content = styled.div`display:grid;justify-items:start;max-width:50rem;gap:${({theme})=>theme.spacing.lg};`;
export const IconBox = styled.div`display:grid;width:5.5rem;height:5.5rem;place-items:center;border-radius:${({theme})=>theme.radius.md};background:${({theme})=>theme.colors.surfaceAccent};color:${({theme})=>theme.colors.primary};`;
export const Description = styled.p`margin:0;color:${({theme})=>theme.colors.textMuted};font-size:clamp(.9rem,2vw,1.1rem);line-height:1.75;`;

export const CapabilityGrid = styled.div`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:${({theme})=>theme.spacing.md};@media(max-width:44rem){grid-template-columns:1fr;}`;
export const Capability = styled.article`display:grid;grid-template-columns:auto minmax(0,1fr);gap:${({theme})=>theme.spacing.sm};padding:${({theme})=>theme.spacing.lg};border-radius:${({theme})=>theme.radius.md};background:${({theme})=>theme.colors.surface};color:${({theme})=>theme.colors.primary};strong{display:block;color:${({theme})=>theme.colors.text};font-size:.84rem;}span{display:block;margin-top:.3rem;color:${({theme})=>theme.colors.textMuted};font-size:.72rem;line-height:1.55;}`;

export const Stages = styled.div`display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:${({theme})=>theme.spacing.md};@media(max-width:48rem){grid-template-columns:1fr;}`;
export const Stage = styled.div`display:flex;align-items:flex-start;gap:${({theme})=>theme.spacing.sm};padding:${({theme})=>theme.spacing.md};color:${({theme})=>theme.colors.primary};strong{display:block;color:${({theme})=>theme.colors.text};font-size:.78rem;}span{display:block;margin-top:.2rem;color:${({theme})=>theme.colors.textSubtle};font-size:.68rem;}`;
export const Notice = styled.div`display:flex;align-items:center;gap:${({theme})=>theme.spacing.sm};padding:${({theme})=>theme.spacing.md};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.dangerSurface};color:${({theme})=>theme.colors.accentSoft};font-size:.72rem;line-height:1.5;`;
