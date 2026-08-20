import styled from "styled-components";

export const ProgressCard = styled.article`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
`;

export const ProgressHeader = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.7rem; }
  span { color: ${({ theme }) => theme.colors.primarySoft}; font-size: 0.62rem; font-variant-numeric: tabular-nums; }
`;

export const ProgressTrack = styled.progress`
  width: 100%;
  height: 0.48rem;
  overflow: hidden;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.round};
  background: ${({ theme }) => theme.colors.line};
  appearance: none;
  &::-webkit-progress-bar { background: ${({ theme }) => theme.colors.line}; }
  &::-webkit-progress-value { border-radius: inherit; background: ${({ theme }) => theme.colors.primary}; }
  &::-moz-progress-bar { border-radius: inherit; background: ${({ theme }) => theme.colors.primary}; }
  &:indeterminate { animation: pulse 1.2s ease-in-out infinite; }
  @keyframes pulse { 50% { opacity: 0.45; } }
  @media (prefers-reduced-motion: reduce) { &:indeterminate { animation: none; } }
`;

export const ProgressMeta = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.35rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.58rem;
  font-variant-numeric: tabular-nums;
`;

export const Prompt = styled.p`
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.62rem;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
`;
