import styled, { keyframes } from "styled-components";

const orbit = keyframes`
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
`;

const pulse = keyframes`
  0%, 100% { transform: scale(.82); opacity: .72; }
  50% { transform: scale(1); opacity: 1; }
`;

export const Wrapper = styled.div`
  display: grid;
  justify-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.lg} 0;
  color: ${({ theme }) => theme.colors.textMuted};
`;

export const Visual = styled.span<{ $size: number }>`
  position: relative;
  display: block;
  width: ${({ $size }) => `${$size}px`};
  height: ${({ $size }) => `${$size}px`};
  isolation: isolate;
`;

export const Core = styled.span`
  position: absolute;
  inset: 34%;
  border-radius: 38% 62% 55% 45%;
  background: linear-gradient(135deg, ${({ theme }) => theme.colors.primary}, ${({ theme }) => theme.colors.accent});
  box-shadow: 0 0 1.1rem ${({ theme }) => theme.colors.lineStrong};
  animation: ${pulse} 1.6s ease-in-out infinite;
`;

export const Orbit = styled.span<{ $delay: string; $inset: string }>`
  position: absolute;
  inset: ${({ $inset }) => $inset};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: 50%;
  animation: ${orbit} 1.8s linear infinite;
  animation-delay: ${({ $delay }) => $delay};

  &::after {
    position: absolute;
    top: -0.2rem;
    left: 50%;
    width: 0.38rem;
    height: 0.38rem;
    border-radius: 50%;
    background: ${({ theme }) => theme.colors.primarySoft};
    box-shadow: 0 0 0.55rem ${({ theme }) => theme.colors.primary};
    content: "";
  }
`;

export const Label = styled.span`
  font-size: 0.76rem;
  letter-spacing: 0.01em;
`;
