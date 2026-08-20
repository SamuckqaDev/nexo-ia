import styled from "styled-components";

export const Form = styled.form`display:grid;gap:${({ theme }) => theme.spacing.md};padding:${({ theme }) => theme.spacing.md};`;
export const Actions = styled.div`display:flex;flex-wrap:wrap;justify-content:flex-end;gap:${({ theme }) => theme.spacing.sm};`;
export const TextareaField = styled.div`display:grid;gap:.4rem;label{font-size:.84rem;font-weight:600;}span{color:${({ theme }) => theme.colors.danger};font-size:.78rem;}`;
export const Textarea = styled.textarea`min-height:7rem;resize:vertical;border:1px solid ${({ theme }) => theme.colors.lineStrong};border-radius:${({ theme }) => theme.radius.control};padding:${({ theme }) => theme.spacing.md};background:${({ theme }) => theme.colors.background};color:${({ theme }) => theme.colors.text};font:inherit;outline:none;&:focus{border-color:${({ theme }) => theme.colors.primary};box-shadow:0 0 0 3px ${({ theme }) => theme.colors.line};}`;
