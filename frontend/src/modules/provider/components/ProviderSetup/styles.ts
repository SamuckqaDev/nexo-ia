import styled from "styled-components";
export const Form = styled.form`display:grid;gap:${({theme})=>theme.spacing.md};max-width:42rem;`;
export const Fields = styled.div`display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:${({theme})=>theme.spacing.md};@media(max-width:42rem){grid-template-columns:1fr;}`;
export const Help = styled.p`margin:0;color:${({theme})=>theme.colors.textMuted};font-size:.76rem;line-height:1.6;`;
