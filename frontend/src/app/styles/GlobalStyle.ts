import { createGlobalStyle } from "styled-components";

export const GlobalStyle = createGlobalStyle`
  *,
  *::before,
  *::after {
    box-sizing: border-box;
  }

  :root {
    color: ${({ theme }) => theme.colors.text};
    background: ${({ theme }) => theme.colors.background};
    font-family: ${({ theme }) => theme.typography.family};
    font-synthesis: none;
    text-rendering: optimizeLegibility;
  }

  body {
    min-width: 320px;
    min-height: 100vh;
    margin: 0;
  }
`;
