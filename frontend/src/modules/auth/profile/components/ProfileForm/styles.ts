import styled from "styled-components";

export const Form = styled.form`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:${({theme})=>theme.spacing.md};@media(max-width:42rem){grid-template-columns:1fr;}`;
export const Actions = styled.div`display:flex;grid-column:1/-1;justify-content:flex-end;gap:${({theme})=>theme.spacing.sm};`;
export const AgeField = styled.div`display:flex;align-items:center;align-self:end;gap:.45rem;min-height:2.7rem;margin-bottom:.15rem;padding:.25rem 0 .25rem .85rem;border-left:2px solid ${({theme})=>theme.colors.primary};color:${({theme})=>theme.colors.textMuted};font-size:.72rem;line-height:1.2;span{color:${({theme})=>theme.colors.textMuted};font-size:.68rem;font-weight:700;letter-spacing:.08em;text-transform:uppercase;}strong{color:${({theme})=>theme.colors.text};font-size:.82rem;}`;
