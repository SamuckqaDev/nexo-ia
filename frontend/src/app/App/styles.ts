import styled from "styled-components";

export const AppRoot = styled.div`
  min-height: 100vh;
`;

export const State = styled.main`
  min-height: 100vh;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.textMuted};
`;
