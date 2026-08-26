import styled from "styled-components";

export const Brand = styled.div`
  position: relative;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  margin-bottom: ${({ theme }) => theme.spacing.md};
  padding-bottom: ${({ theme }) => theme.spacing.md};
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  font-size: 1.05rem;
  font-weight: 700;

  &::after {
    position: absolute;
    right: 0;
    bottom: -1px;
    left: 0;
    height: 1px;
    background: linear-gradient(90deg, ${({ theme }) => theme.colors.primary}, ${({ theme }) => theme.colors.accent}, transparent 85%);
    content: "";
  }
`;

export const BrandName = styled.span<{ $hidden: boolean }>`
  display: ${({ $hidden }) => ($hidden ? "none" : "flex")};
  flex-direction: column;
  line-height: 1.15;
  white-space: nowrap;
`;

export const BrandTagline = styled.span`
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.62rem;
  font-weight: 500;
  letter-spacing: 0.02em;
`;

export const Logo = styled.img`
  width: 2.4rem;
  height: 2.4rem;
  object-fit: contain;
`;
