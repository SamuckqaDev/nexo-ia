import styled from "styled-components";
import type { DiffLineTone } from "./types";

export const MarkdownBody = styled.div<{ $user: boolean }>`
  min-width: 0;
  overflow-wrap: anywhere;
  color: ${({ theme }) => theme.colors.text};
  font-size: ${({ $user }) => ($user ? "0.85rem" : "0.9rem")};
  line-height: 1.72;

  > :first-child { margin-top: 0; }
  > :last-child { margin-bottom: 0; }
  p { margin: 0.65rem 0; }
  h1, h2, h3, h4 { margin: 1.1rem 0 0.45rem; line-height: 1.3; letter-spacing: -0.02em; }
  h1 { font-size: 1.25rem; }
  h2 { font-size: 1.08rem; }
  h3, h4 { font-size: 0.96rem; }
  ul, ol { margin: 0.65rem 0; padding-left: 1.35rem; }
  li + li { margin-top: 0.25rem; }
  blockquote {
    margin: 0.75rem 0;
    border-left: 3px solid ${({ theme }) => theme.colors.primary};
    padding-left: ${({ theme }) => theme.spacing.sm};
    color: ${({ theme }) => theme.colors.textMuted};
  }
  table { display: block; width: 100%; margin: 0.75rem 0; overflow-x: auto; border-collapse: collapse; font-size: 0.78rem; }
  th, td { border: 1px solid ${({ theme }) => theme.colors.line}; padding: 0.45rem 0.55rem; text-align: left; }
  th { background: ${({ theme }) => theme.colors.surfaceAccent}; color: ${({ theme }) => theme.colors.primarySoft}; }
  a { color: ${({ theme }) => theme.colors.primarySoft}; text-underline-offset: 0.18em; }
  hr { border: 0; border-top: 1px solid ${({ theme }) => theme.colors.line}; margin: 1rem 0; }
`;

export const StreamingCaret = styled.span`
  display: inline-block;
  width: 0.5rem;
  color: ${({ theme }) => theme.colors.primary};
  animation: blink 1s steps(2, start) infinite;
  @keyframes blink { to { visibility: hidden; } }
  @media (prefers-reduced-motion: reduce) { animation: none; }
`;

export const InlineCode = styled.code`
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: 0.35rem;
  padding: 0.08rem 0.3rem;
  background: ${({ theme }) => theme.colors.backgroundSoft};
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.82em;
`;

export const CodeSurface = styled.section`
  min-width: 0;
  margin: 0.8rem 0;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
`;

export const CodeHeader = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: 0.42rem 0.65rem;
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.backgroundSoft};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
`;

export const CodeCopy = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  border: 0;
  padding: 0.2rem 0.3rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  font: inherit;
  font-size: 0.6rem;
  cursor: pointer;
  &:hover, &:focus-visible { color: ${({ theme }) => theme.colors.primarySoft}; }
`;

export const CodeBody = styled.pre`
  margin: 0;
  overflow: auto;
  padding: 0.7rem 0;
  color: ${({ theme }) => theme.colors.text};
  font-size: 0.74rem;
  line-height: 1.58;
  tab-size: 2;
`;

export const CodeLine = styled.span<{ $tone: DiffLineTone }>`
  display: block;
  min-width: max-content;
  padding: 0 0.75rem;
  background: ${({ theme, $tone }) => {
    if ($tone === "added") return "rgba(65, 196, 142, 0.13)";
    if ($tone === "removed") return theme.colors.dangerSurface;
    if ($tone === "meta") return theme.colors.surfaceAccent;
    return "transparent";
  }};
  color: ${({ theme, $tone }) => {
    if ($tone === "added") return "#7be0b5";
    if ($tone === "removed") return theme.colors.accentSoft;
    if ($tone === "meta") return theme.colors.primarySoft;
    return theme.colors.text;
  }};
  white-space: pre;
`;
