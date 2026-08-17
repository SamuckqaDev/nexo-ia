import styled from "styled-components";
export const Page = styled.section`display:grid;min-height:28rem;place-items:center;text-align:center;`;
export const Content = styled.div`max-width:32rem;`;
export const IconBox = styled.div`display:grid;width:4rem;height:4rem;margin:0 auto ${({theme})=>theme.spacing.lg};place-items:center;border:1px solid ${({theme})=>theme.colors.lineStrong};border-radius:${({theme})=>theme.radius.md};background:${({theme})=>theme.colors.surfaceAccent};color:${({theme})=>theme.colors.primary};`;
export const Title = styled.h1`margin:0;font-size:2rem;`;
export const Description = styled.p`margin:${({theme})=>theme.spacing.md} 0 0;color:${({theme})=>theme.colors.textMuted};line-height:1.7;`;
