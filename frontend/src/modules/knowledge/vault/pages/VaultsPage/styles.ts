import styled from "styled-components";

export const Explorer = styled.div`
  display: grid;
  height: 100%;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(14rem, 0.52fr) minmax(0, 1.8fr);
  gap: ${({ theme }) => theme.spacing.lg};
  overflow: hidden;

  > section {
    display: grid;
    min-height: 0;
    grid-template-rows: auto minmax(0, 1fr);
  }

  > section > div:last-child {
    min-height: 0;
    overflow: hidden;
  }

  > section:last-child > div:last-child {
    overflow: auto;
    overscroll-behavior: contain;
  }

  @media(max-width:48rem) {
    height: auto;
    grid-template-columns: 1fr;
    align-content: start;
    overflow: auto;
    overscroll-behavior: contain;

    > section { min-height: 20rem; }
  }
`;

export const PageActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
`;
