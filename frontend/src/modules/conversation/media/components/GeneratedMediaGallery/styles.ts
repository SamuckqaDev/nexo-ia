import styled from "styled-components";

export const MediaGrid = styled.section`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 12rem), 1fr));
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const MediaCard = styled.article`
  min-width: 0;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
`;

export const MediaPreview = styled.a`
  position: relative;
  display: block;
  aspect-ratio: 1;
  overflow: hidden;
  background: ${({ theme }) => theme.colors.backgroundSoft};

  > img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 160ms ease;
  }

  > span {
    position: absolute;
    right: 0.45rem;
    bottom: 0.45rem;
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    padding: 0.28rem 0.42rem;
    border-radius: ${({ theme }) => theme.radius.button};
    background: ${({ theme }) => theme.colors.surfaceStrong};
    color: ${({ theme }) => theme.colors.primary};
    font-size: 0.55rem;
    font-weight: 700;
    opacity: 0;
    transform: translateY(0.2rem);
    transition: opacity 160ms ease, transform 160ms ease;
  }

  &:hover > img, &:focus-visible > img { transform: scale(1.025); }
  &:hover > span, &:focus-visible > span { opacity: 1; transform: translateY(0); }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: -2px; }

  @media (prefers-reduced-motion: reduce) {
    > img, > span { transition: none; }
  }
`;

export const MediaCopy = styled.div`
  display: grid;
  gap: 0.18rem;
  padding: ${({ theme }) => theme.spacing.sm};

  strong {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 0.35rem;
    overflow: hidden;
    color: ${({ theme }) => theme.colors.text};
    font-size: 0.64rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span, time {
    overflow: hidden;
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.52rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;
