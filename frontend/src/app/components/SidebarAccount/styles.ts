import styled from "styled-components";

export const Account = styled.div`
  position: relative;
  margin-top: auto;
  padding-top: ${({ theme }) => theme.spacing.md};
  border-top: 1px solid ${({ theme }) => theme.colors.line};

`;

export const Trigger = styled.button<{ $collapsed: boolean }>`
  display: flex;
  align-items: center;
  justify-content: ${({ $collapsed }) => ($collapsed ? "center" : "flex-start")};
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ $collapsed }) => ($collapsed ? "0.4rem" : "0.5rem")};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  text-align: left;
  cursor: pointer;

  &:hover {
    border-color: ${({ theme }) => theme.colors.lineStrong};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }

  > svg {
    flex: 0 0 auto;
    color: ${({ theme }) => theme.colors.textSubtle};
  }
`;

export const Avatar = styled.span`
  position: relative;
  display: grid;
  width: 2rem;
  height: 2rem;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.backgroundElevated};
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.7rem;
  font-weight: 700;

  &::after {
    position: absolute;
    z-index: 2;
    inset: 0;
    border: 1px solid ${({ theme }) => theme.colors.line};
    border-radius: ${({ theme }) => theme.radius.control};
    content: "";
    pointer-events: none;
  }
`;

export const AvatarImage = styled.img`
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: ${({ theme }) => theme.radius.control};
  object-fit: cover;
`;

export const Identity = styled.span`
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 0.1rem;
`;

export const Name = styled.strong`
  overflow: hidden;
  font-size: 0.8rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Email = styled.span`
  overflow: hidden;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.66rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Menu = styled.div`
  position: absolute;
  z-index: 20;
  bottom: calc(100% + 0.5rem);
  left: 0;
  width: min(15rem, 78vw);
  padding: ${({ theme }) => theme.spacing.sm};
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  box-shadow: ${({ theme }) => theme.shadow};
`;

export const MenuButton = styled.button`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  width: 100%;
  border: 1px solid transparent;
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.6rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textMuted};
  font: inherit;
  font-size: 0.8rem;
  text-align: left;
  cursor: pointer;

  & + & { margin-top: 0.2rem; }

  &:hover:not(:disabled),
  &:focus-visible {
    border-color: ${({ theme }) => theme.colors.line};
    background: ${({ theme }) => theme.colors.surfaceAccent};
    color: ${({ theme }) => theme.colors.primarySoft};
  }

  &:disabled { cursor: not-allowed; opacity: 0.6; }
`;
