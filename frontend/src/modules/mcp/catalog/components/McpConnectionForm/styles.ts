import styled from "styled-components";

export const Form = styled.form`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.md};
`;

export const Hint = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.textSubtle};
  font-size: 0.64rem;
  line-height: 1.55;
`;

export const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacing.sm};
`;
