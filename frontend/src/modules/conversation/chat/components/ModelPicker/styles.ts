import styled from "styled-components";

export const Field = styled.div`
  display: grid;
  min-width: 0;
  justify-items: end;
  gap: 0.16rem;
`;

export const Picker = styled.select`
  width: 100%;
  min-width: 10rem;
  max-width: 18rem;
  border: 1px solid ${({ theme }) => theme.colors.line};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.42rem 0.65rem;
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  font-size: 0.74rem;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 1px;
  }

  &:disabled {
    opacity: 0.6;
  }
`;

export const Status = styled.small`
  overflow: hidden;
  max-width: 18rem;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.58rem;
  text-overflow: ellipsis;
  white-space: nowrap;
`;
