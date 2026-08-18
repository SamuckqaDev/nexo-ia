import styled from "styled-components";

export const Loader = styled.div<{ $variant: "page" | "message" }>`
  display: grid;
  width: ${({ $variant }) => $variant === "message" ? "min(38rem, 100%)" : "100%"};
  min-height: ${({ $variant }) => $variant === "message" ? "auto" : "12rem"};
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  align-self: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ $variant, theme }) => $variant === "message" ? theme.spacing.sm : theme.spacing.lg};
  margin: ${({ $variant }) => $variant === "message" ? "0 auto" : "auto"};
  border: ${({ $variant, theme }) => $variant === "message" ? `1px solid ${theme.colors.line}` : "0"};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ $variant, theme }) => $variant === "message" ? theme.colors.surface : "transparent"};
`;

export const Pulse = styled.span`
  position: relative;
  display: grid;
  width: 2.65rem;
  height: 2.65rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};

  &::after {
    position: absolute;
    inset: -0.28rem;
    border: 1px solid ${({ theme }) => theme.colors.primary};
    border-radius: calc(${({ theme }) => theme.radius.control} + 0.28rem);
    opacity: 0;
    animation: nexo-pulse 1.8s ease-out infinite;
    content: "";
  }

  @keyframes nexo-pulse {
    0% { opacity: 0.5; transform: scale(0.88); }
    75%, 100% { opacity: 0; transform: scale(1.08); }
  }

  @media (prefers-reduced-motion: reduce) {
    &::after { animation: none; }
  }
`;

export const Face = styled.img`
  width: 1.8rem;
  height: 1.8rem;
  object-fit: contain;
`;

export const Copy = styled.div`
  display: grid;
  gap: 0.14rem;
  strong { color: ${({ theme }) => theme.colors.primarySoft}; font-size: 0.72rem; letter-spacing: 0.04em; }
  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.68rem; }
`;

export const Dots = styled.span`
  display: flex;
  align-items: center;
  gap: 0.24rem;

  i {
    width: 0.28rem;
    height: 0.28rem;
    border-radius: 50%;
    background: ${({ theme }) => theme.colors.primary};
    animation: nexo-dot 1.1s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.14s; }
    &:nth-child(3) { animation-delay: 0.28s; }
  }

  @keyframes nexo-dot {
    0%, 70%, 100% { opacity: 0.25; transform: translateY(0); }
    35% { opacity: 1; transform: translateY(-0.2rem); }
  }

  @media (prefers-reduced-motion: reduce) {
    i { animation: none; }
  }
`;
