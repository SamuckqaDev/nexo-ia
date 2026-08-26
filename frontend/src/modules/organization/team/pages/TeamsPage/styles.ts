import styled from "styled-components";

export const Workspace = styled.div`
  display: grid;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(13rem, 0.48fr) minmax(0, 1.8fr);
  gap: ${({ theme }) => theme.spacing.lg};
  overflow: hidden;

  > section { display: grid; min-height: 0; grid-template-rows: auto minmax(0, 1fr); }
  > section > div:last-child { min-height: 0; overflow: hidden; }

  @media (max-width: 52rem) {
    height: auto;
    grid-template-columns: 1fr;
    align-content: start;
    overflow: auto;
    > section:first-child { min-height: 15rem; max-height: 22rem; }
    > section:last-child { min-height: 32rem; }
  }
`;

export const PageActions = styled.div`display: flex; gap: ${({ theme }) => theme.spacing.sm};`;
