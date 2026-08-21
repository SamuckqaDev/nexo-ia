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
    scrollbar-color: ${({ theme }) => `${theme.colors.primary} ${theme.colors.backgroundSoft}`};
    scrollbar-width: thin;
  }

  * {
    scrollbar-color: ${({ theme }) => `${theme.colors.primary} ${theme.colors.backgroundSoft}`};
    scrollbar-width: thin;
  }

  *::-webkit-scrollbar {
    width: 0.62rem;
    height: 0.62rem;
  }

  *::-webkit-scrollbar-track {
    background: ${({ theme }) => theme.colors.backgroundSoft};
  }

  *::-webkit-scrollbar-thumb {
    border: 2px solid ${({ theme }) => theme.colors.backgroundSoft};
    border-radius: ${({ theme }) => theme.radius.round};
    background: linear-gradient(180deg, ${({ theme }) => theme.colors.primary}, ${({ theme }) => theme.colors.accent});
  }

  *::-webkit-scrollbar-thumb:hover {
    background: ${({ theme }) => theme.colors.primarySoft};
  }

  body {
    min-width: 320px;
    min-height: 100vh;
    margin: 0;
    overflow-x: hidden;
  }
`;
