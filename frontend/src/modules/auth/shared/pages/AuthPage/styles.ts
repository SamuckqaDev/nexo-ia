import styled from "styled-components";

export const Page = styled.main`
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(16rem, 1fr) minmax(20rem, 35rem);
  align-items: center;
  gap: clamp(2rem, 8vw, 8rem);
  padding: clamp(1.25rem, 5vw, 5rem);
  background:
    radial-gradient(circle at 15% 25%, ${({ theme }) => theme.colors.surfaceAccent}, transparent 32rem),
    radial-gradient(circle at 85% 75%, ${({ theme }) => theme.colors.dangerSurface}, transparent 30rem),
    ${({ theme }) => theme.colors.background};

  @media (max-width: 760px) {
    grid-template-columns: 1fr;
    align-content: center;
  }
`;

export const Brand = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.lg};
`;

export const Logo = styled.img`
  width: clamp(5.5rem, 12vw, 9rem);
  height: clamp(5.5rem, 12vw, 9rem);
  object-fit: contain;
  filter: drop-shadow(0 0 2.5rem ${({ theme }) => theme.colors.lineStrong});
`;

export const BrandContent = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const BrandName = styled.strong`
  font-size: clamp(2rem, 6vw, 4rem);
  letter-spacing: -0.06em;
`;

export const BrandPromise = styled.span`
  max-width: 25rem;
  color: ${({ theme }) => theme.colors.textMuted};
  line-height: 1.5;
`;

export const Card = styled.section`
  padding: clamp(1.4rem, 4vw, 2.5rem);
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};
  backdrop-filter: blur(18px);
`;
