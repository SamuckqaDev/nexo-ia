import styled from "styled-components";

export const Switcher = styled.div`
  display: grid;
  gap: 0.35rem;
  margin-bottom: ${({ theme }) => theme.spacing.md};

  > div {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: ${({ theme }) => theme.spacing.sm};
    min-width: 0;
    padding: 0.55rem 0.65rem;
    border: 1px solid ${({ theme }) => theme.colors.lineStrong};
    border-radius: ${({ theme }) => theme.radius.control};
    background: ${({ theme }) => theme.colors.background};
    color: ${({ theme }) => theme.colors.primary};
  }
`;

export const Label = styled.label`
  padding: 0 0.2rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
`;

export const Copy = styled.span`
  display: grid;
  min-width: 0;
  gap: 0.12rem;

  > span {
    overflow: hidden;
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.54rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

export const Select = styled.select`
  width: 100%;
  min-width: 0;
  border: 0;
  appearance: none;
  outline: none;
  background: transparent;
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  font-size: 0.7rem;
  font-weight: 700;
  cursor: pointer;
`;

export const ManageButton = styled.button`
  display: grid;
  place-items: center;
  border: 0;
  padding: 0.2rem;
  background: transparent;
  color: ${({ theme }) => theme.colors.textMuted};
  cursor: pointer;
  &:hover, &:focus-visible { color: ${({ theme }) => theme.colors.primary}; }
`;

export const CollapsedButton = styled.button`
  display: grid;
  width: 100%;
  min-height: 2.8rem;
  place-items: center;
  margin-bottom: ${({ theme }) => theme.spacing.md};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.background};
  color: ${({ theme }) => theme.colors.primary};
  cursor: pointer;
`;
