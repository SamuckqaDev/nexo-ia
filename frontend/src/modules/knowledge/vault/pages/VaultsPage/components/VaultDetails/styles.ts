import styled from "styled-components";

export const Summary = styled.div`display:flex;align-items:center;justify-content:space-between;gap:${({ theme }) => theme.spacing.md};padding:${({ theme }) => theme.spacing.md};border-bottom:1px solid ${({ theme }) => theme.colors.line};@media(max-width:40rem){align-items:stretch;flex-direction:column;}`;
export const MetaGrid = styled.div`display:flex;flex-wrap:wrap;gap:${({ theme }) => theme.spacing.sm};`;
export const MetaItem = styled.div`display:grid;gap:.15rem;min-width:6.5rem;padding:.55rem .7rem;border-radius:${({ theme }) => theme.radius.control};background:${({ theme }) => theme.colors.background};span{color:${({ theme }) => theme.colors.textSubtle};font-size:.56rem;text-transform:uppercase;}strong{font-size:.7rem;text-transform:capitalize;}`;
export const SourceAction = styled.div`flex:0 0 auto;`;
export const FileInput = styled.input`position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;`;
export const SourceList = styled.div`display:grid;`;
export const SourceRow = styled.div`display:grid;grid-template-columns:auto minmax(0,1fr) auto auto;align-items:center;gap:${({ theme }) => theme.spacing.sm};width:100%;padding:${({ theme }) => theme.spacing.md};border-bottom:1px solid ${({ theme }) => theme.colors.line};color:${({ theme }) => theme.colors.primary};>div{min-width:0;}strong,span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}strong{color:${({ theme }) => theme.colors.text};font-size:.72rem;}span{margin-top:.2rem;color:${({ theme }) => theme.colors.textSubtle};font-size:.6rem;}@media(max-width:40rem){grid-template-columns:auto minmax(0,1fr) auto;>button{grid-column:2 / -1;justify-self:start;}}`;
export const VaultIdentity = styled.span`display:inline-flex;align-items:center;gap:.35rem;color:${({ theme }) => theme.colors.primarySoft};font-size:.62rem;font-weight:700;`;
