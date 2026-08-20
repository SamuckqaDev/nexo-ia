import styled from "styled-components";

export const Form = styled.form`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
`;
export const Row = styled.div`display:grid;grid-template-columns:1fr 1fr;gap:${({ theme }) => theme.spacing.sm};@media(max-width:32rem){grid-template-columns:1fr;}`;
export const Actions = styled.div`display:flex;flex-wrap:wrap;justify-content:flex-end;gap:${({ theme }) => theme.spacing.sm};`;
export const FormNotice = styled.p`margin:0;padding:${({ theme }) => theme.spacing.sm};border-radius:${({ theme }) => theme.radius.control};background:${({ theme }) => theme.colors.dangerSurface};color:${({ theme }) => theme.colors.accentSoft};font-size:.66rem;line-height:1.5;`;
