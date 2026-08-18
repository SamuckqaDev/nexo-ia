import styled from "styled-components";

export const Content = styled.div`display:grid;gap:${({theme})=>theme.spacing.md};`;
export const StatusLine = styled.div`display:flex;align-items:center;gap:${({theme})=>theme.spacing.sm};font-size:.82rem;font-weight:700;`;
export const Dot = styled.span<{ $connected:boolean }>`width:.65rem;height:.65rem;border-radius:${({theme})=>theme.radius.round};background:${({theme,$connected})=>$connected?theme.colors.statusOnline:theme.colors.statusOffline};`;
export const Endpoint = styled.p`margin:0;color:${({theme})=>theme.colors.textMuted};font-size:.74rem;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;`;
export const ModelList = styled.div`display:grid;gap:${({theme})=>theme.spacing.xs};`;
export const Model = styled.div`display:flex;align-items:center;justify-content:space-between;gap:${({theme})=>theme.spacing.sm};padding:${({theme})=>theme.spacing.sm};border:1px solid ${({theme})=>theme.colors.line};border-radius:${({theme})=>theme.radius.control};background:${({theme})=>theme.colors.surfaceStrong};font-size:.75rem;span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}small{color:${({theme})=>theme.colors.textMuted};}`;
export const Empty = styled.p`margin:0;color:${({theme})=>theme.colors.textMuted};font-size:.76rem;`;
export const Error = styled(Empty)`color:${({theme})=>theme.colors.accentSoft};`;
export const Actions = styled.div`display:flex;justify-content:flex-end;`;
