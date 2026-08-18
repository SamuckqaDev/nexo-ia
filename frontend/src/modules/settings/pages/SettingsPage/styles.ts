import styled from "styled-components";

export const Page = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.lg};
`;

export const Navigation = styled.nav`display:flex;gap:${({theme})=>theme.spacing.xs};overflow-x:auto;padding-bottom:.2rem;`;
export const NavButton = styled.button<{ $active:boolean }>`flex:0 0 auto;border:1px solid ${({theme,$active})=>$active?theme.colors.lineStrong:theme.colors.line};border-radius:${({theme})=>theme.radius.control};padding:.65rem .9rem;background:${({theme,$active})=>$active?theme.colors.surfaceAccent:theme.colors.surface};color:${({theme,$active})=>$active?theme.colors.primarySoft:theme.colors.textMuted};font:inherit;font-size:.78rem;font-weight:700;cursor:pointer;`;

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
  line-height: 0;

  svg {
    display: block;
  }
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

export const ProfileDetails = styled.div`display:grid;gap:${({theme})=>theme.spacing.lg};`;

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

export const PreferenceGrid = styled.div`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:${({theme})=>theme.spacing.md};@media(max-width:42rem){grid-template-columns:1fr;}`;
export const PreferenceField = styled.label`display:grid;gap:${({theme})=>theme.spacing.sm};padding:${({theme})=>theme.spacing.md};border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.surface};span{color:${({theme})=>theme.colors.textMuted};font-size:.74rem;}strong{font-size:.88rem;}`;
export const Select = styled.select`width:100%;border:1px solid ${({theme})=>theme.colors.lineStrong};border-radius:${({theme})=>theme.radius.control};padding:.72rem .8rem;background:${({theme})=>theme.colors.surfaceStrong};color:${({theme})=>theme.colors.text};font:inherit;font-size:.8rem;cursor:pointer;`;

export const Periods = styled.div`display:flex;flex-wrap:wrap;gap:${({theme})=>theme.spacing.xs};`;
export const PeriodButton = styled.button`border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};padding:.5rem .75rem;background:${({theme})=>theme.colors.surface};color:${({theme})=>theme.colors.textMuted};font:inherit;font-size:.72rem;cursor:pointer;&:first-child{border-color:${({theme})=>theme.colors.lineStrong};color:${({theme})=>theme.colors.primarySoft};}`;
export const MetricGrid = styled.div`display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:${({theme})=>theme.spacing.sm};@media(max-width:44rem){grid-template-columns:repeat(2,minmax(0,1fr));}`;
export const Metric = styled.div`padding:${({theme})=>theme.spacing.md};border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.surface};span{display:block;color:${({theme})=>theme.colors.textMuted};font-size:.72rem;}strong{display:block;margin-top:.35rem;font-size:1.15rem;}`;
export const Chart = styled.div`display:grid;min-height:12rem;place-items:center;border-left:1px solid ${({theme})=>theme.colors.line};border-bottom:1px solid ${({theme})=>theme.colors.line};background:repeating-linear-gradient(to bottom,transparent 0,transparent 2.95rem,${({theme})=>theme.colors.line} 3rem);`;
export const ChartEmpty = styled.span`padding:.5rem .75rem;border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.surfaceStrong};color:${({theme})=>theme.colors.textMuted};font-size:.75rem;`;
export const UsageBreakdown = styled.div`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:${({theme})=>theme.spacing.sm};@media(max-width:38rem){grid-template-columns:1fr;}`;
export const UsageItem = styled.div`display:flex;justify-content:space-between;gap:${({theme})=>theme.spacing.md};padding:${({theme})=>theme.spacing.md};border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.surface};span{color:${({theme})=>theme.colors.textMuted};font-size:.74rem;}strong{font-size:.76rem;text-align:right;}`;
