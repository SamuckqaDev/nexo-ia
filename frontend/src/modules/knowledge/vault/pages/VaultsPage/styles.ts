import styled from "styled-components";

export const Explorer = styled.div`
  display: grid;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(14rem, 0.52fr) minmax(0, 1.8fr);
  gap: ${({ theme }) => theme.spacing.lg};
  overflow: hidden;

  > section {
    display: grid;
    min-height: 0;
    grid-template-rows: auto minmax(0, 1fr);
  }

  > section > div:last-child {
    min-height: 0;
    overflow: hidden;
  }

  > section:last-child > div:last-child {
    overflow: auto;
    overscroll-behavior: contain;
  }

  @media(max-width:48rem) {
    height: auto;
    grid-template-columns: 1fr;
    align-content: start;
    overflow: auto;
    overscroll-behavior: contain;

    > section { min-height: 20rem; }
  }
`;
export const PageActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
`;
export const Library = styled.div`display:grid;height:100%;min-height:0;grid-template-rows:auto minmax(0,1fr);gap:${({ theme }) => theme.spacing.md};padding:${({ theme }) => theme.spacing.md};overflow:hidden;`;
export const VaultList = styled.div`display:grid;min-height:0;align-content:start;gap:.35rem;overflow:auto;overscroll-behavior:contain;`;
export const VaultButton = styled.button<{ $active: boolean }>`display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:${({ theme }) => theme.spacing.sm};width:100%;border:1px solid ${({ theme, $active }) => ($active ? theme.colors.lineStrong : "transparent")};border-radius:${({ theme }) => theme.radius.control};padding:.75rem;background:${({ theme, $active }) => ($active ? theme.colors.surfaceAccent : "transparent")};color:${({ theme, $active }) => ($active ? theme.colors.primary : theme.colors.textMuted)};font:inherit;text-align:left;cursor:pointer;&:hover{background:${({ theme }) => theme.colors.surfaceAccent};}`;
export const VaultCopy = styled.span`min-width:0;strong,span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}strong{color:${({ theme }) => theme.colors.text};font-size:.72rem;}span{margin-top:.18rem;color:${({ theme }) => theme.colors.textSubtle};font-size:.58rem;text-transform:capitalize;}`;
export const Summary = styled.div`display:flex;align-items:center;justify-content:space-between;gap:${({ theme }) => theme.spacing.md};padding:${({ theme }) => theme.spacing.md};border-bottom:1px solid ${({ theme }) => theme.colors.line};@media(max-width:40rem){align-items:stretch;flex-direction:column;}`;
export const MetaGrid = styled.div`display:flex;flex-wrap:wrap;gap:${({ theme }) => theme.spacing.sm};`;
export const MetaItem = styled.div`display:grid;gap:.15rem;min-width:6.5rem;padding:.55rem .7rem;border-radius:${({ theme }) => theme.radius.control};background:${({ theme }) => theme.colors.background};span{color:${({ theme }) => theme.colors.textSubtle};font-size:.56rem;text-transform:uppercase;}strong{font-size:.7rem;text-transform:capitalize;}`;
export const SourceAction = styled.div`flex:0 0 auto;`;
export const FileInput = styled.input`position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;`;
export const SourceList = styled.div`display:grid;`;
export const SourceRow = styled.button<{ $active: boolean }>`display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:${({ theme }) => theme.spacing.sm};width:100%;padding:${({ theme }) => theme.spacing.md};border:0;border-bottom:1px solid ${({ theme }) => theme.colors.line};background:${({ theme, $active }) => $active ? theme.colors.surfaceAccent : "transparent"};color:${({ theme }) => theme.colors.primary};font:inherit;text-align:left;cursor:pointer;>div{min-width:0;}strong,span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}strong{color:${({ theme }) => theme.colors.text};font-size:.72rem;}span{margin-top:.2rem;color:${({ theme }) => theme.colors.textSubtle};font-size:.6rem;}&:hover,&:focus-visible{background:${({ theme }) => theme.colors.surfaceAccent};}`;
export const SourceStatus = styled.span<{ $local: boolean }>`display:flex!important;align-items:center;gap:.3rem!important;color:${({ theme, $local }) => ($local ? theme.colors.accentSoft : theme.colors.statusOnline)}!important;font-size:.58rem!important;font-weight:700;text-transform:capitalize;`;
export const SourceDetail = styled.section`display:grid;gap:${({ theme }) => theme.spacing.sm};margin:${({ theme }) => theme.spacing.md};padding:${({ theme }) => theme.spacing.md};border:1px solid ${({ theme }) => theme.colors.lineStrong};border-radius:${({ theme }) => theme.radius.control};background:${({ theme }) => theme.colors.background};`;
export const SourceDetailHeader = styled.header`display:flex;align-items:center;justify-content:space-between;gap:${({ theme }) => theme.spacing.sm};>div{display:flex;align-items:center;gap:${({ theme }) => theme.spacing.sm};color:${({ theme }) => theme.colors.primary};}span{display:grid;gap:.14rem;}strong{color:${({ theme }) => theme.colors.text};font-size:.72rem;}small{color:${({ theme }) => theme.colors.textSubtle};font-size:.58rem;}@media(max-width:38rem){align-items:stretch;flex-direction:column;}`;
export const SourcePreview = styled.div`max-height:18rem;overflow:auto;padding:${({ theme }) => theme.spacing.md};border:1px solid ${({ theme }) => theme.colors.line};border-radius:${({ theme }) => theme.radius.control};background:${({ theme }) => theme.colors.surfaceStrong};color:${({ theme }) => theme.colors.textMuted};font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:.68rem;line-height:1.65;white-space:pre-wrap;overflow-wrap:anywhere;>small{display:block;margin-top:${({ theme }) => theme.spacing.md};color:${({ theme }) => theme.colors.accentSoft};font-family:inherit;}`;
