import styled from "styled-components";

export const Sessions = styled.div`display:grid;grid-column:1/-1;gap:${({theme})=>theme.spacing.sm};padding-top:${({theme})=>theme.spacing.sm};border-top:1px solid ${({theme})=>theme.colors.line};`;
export const Session = styled.div`display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:center;gap:${({theme})=>theme.spacing.md};`;
export const Detail = styled.span`min-width:0;overflow:hidden;color:${({theme})=>theme.colors.textMuted};font-size:.76rem;text-overflow:ellipsis;white-space:nowrap;`;
export const Empty = styled.span`color:${({theme})=>theme.colors.textMuted};font-size:.8rem;`;
