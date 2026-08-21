import styled, { css } from "styled-components";

export const Backdrop = styled.div`
  position: fixed;
  z-index: 100;
  inset: 0;
  background: rgba(3, 11, 33, 0.66);
  backdrop-filter: blur(4px);
`;

export const WindowFrame = styled.div<{ $x: number; $y: number; $maximized: boolean }>`
  position: absolute;
  left: 50%;
  top: 50%;
  display: grid;
  width: min(72rem, calc(100vw - 3rem));
  height: min(44rem, calc(100dvh - 3rem));
  min-width: min(38rem, calc(100vw - 1rem));
  min-height: min(24rem, calc(100dvh - 1rem));
  max-width: calc(100vw - 1.5rem);
  max-height: calc(100dvh - 1.5rem);
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  transform: translate(calc(-50% + ${({ $x }) => $x}px), calc(-50% + ${({ $y }) => $y}px));
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.background};
  box-shadow: 0 2rem 6rem rgba(0, 0, 0, 0.62), ${({ theme }) => theme.shadow};
  resize: both;

  ${({ $maximized }) => $maximized && css`
    inset: 0.75rem;
    width: auto;
    height: auto;
    max-width: none;
    max-height: none;
    transform: none;
    resize: none;
  `}

  @media(max-width:48rem) {
    width: calc(100vw - 1rem);
    height: calc(100dvh - 1rem);
    min-width: 0;
    min-height: 0;
    max-width: none;
    max-height: none;
    resize: none;
  }
`;

export const WindowTitlebar = styled.header`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: 0.65rem 0.75rem;
  border-bottom: 1px solid ${({ theme }) => theme.colors.line};
  background: linear-gradient(90deg, ${({ theme }) => theme.colors.backgroundElevated}, ${({ theme }) => theme.colors.surfaceStrong});
  cursor: grab;
  touch-action: none;
  user-select: none;

  &:active { cursor: grabbing; }
`;

export const DragHandle = styled.span`
  display: grid;
  color: ${({ theme }) => theme.colors.textSubtle};
`;

export const WindowTitle = styled.div`
  min-width: 0;

  strong, span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.78rem; }
  span { margin-top: 0.12rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.58rem; }
`;

export const WindowControls = styled.div`
  display: flex;
  gap: 0.35rem;
`;

export const WindowButton = styled.button`
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: 0.55rem;
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;

  &:hover, &:focus-visible {
    border-color: ${({ theme }) => theme.colors.primary};
    color: ${({ theme }) => theme.colors.primary};
    outline: none;
  }
`;

export const WindowBody = styled.div`
  min-width: 0;
  min-height: 0;
  overflow: hidden;
`;

export const WindowState = styled.div`
  display: grid;
  height: 100%;
  min-height: 14rem;
  place-content: center;
  justify-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => theme.spacing.xl};
  color: ${({ theme }) => theme.colors.textSubtle};
  text-align: center;

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.82rem; }
  span { max-width: 30rem; font-size: 0.64rem; line-height: 1.55; }
`;

export const WindowStatus = styled.footer`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: 0.42rem 0.75rem;
  border-top: 1px solid ${({ theme }) => theme.colors.line};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.56rem;
`;
