import styled from "styled-components";

export const Status = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  color: ${({ theme }) => theme.colors.text};
  font-size: 0.9rem;
`;

export const StatusDot = styled.span<{ $online: boolean }>`
  width: 0.65rem;
  height: 0.65rem;
  border-radius: ${({ theme }) => theme.radius.round};
  background: ${({ theme, $online }) =>
    $online ? theme.colors.statusOnline : theme.colors.statusOffline};
  box-shadow: ${({ theme, $online }) => ($online ? `0 0 1rem ${theme.colors.statusOnlineGlow}` : "none")};
`;
