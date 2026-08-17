import styled from "styled-components";

export const Form = styled.form`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const Intro = styled.header`
  margin-bottom: ${({ theme }) => theme.spacing.sm};
`;

export const Eyebrow = styled.p`
  margin: 0 0 ${({ theme }) => theme.spacing.sm};
  color: ${({ theme }) => theme.colors.primary};
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
`;

export const Title = styled.h1`
  margin: 0;
  font-size: clamp(2rem, 6vw, 3rem);
  letter-spacing: -0.04em;
`;

export const Description = styled.p`
  margin: ${({ theme }) => theme.spacing.sm} 0 0;
  color: ${({ theme }) => theme.colors.textMuted};
  line-height: 1.6;
`;

export const TextButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
  justify-self: start;
  border: 0;
  padding: 0;
  background: transparent;
  color: ${({ theme }) => theme.colors.primarySoft};
  font: inherit;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: ${({ theme }) => theme.colors.accentSoft};
  }
`;
