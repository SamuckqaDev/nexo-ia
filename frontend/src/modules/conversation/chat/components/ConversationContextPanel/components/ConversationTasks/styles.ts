import styled from "styled-components";

export const TaskStack = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const TaskGroup = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const TaskGroupHeader = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};

  span {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    color: ${({ theme }) => theme.colors.text};
    font-size: 0.66rem;
    font-weight: 700;
  }

  small {
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.52rem;
  }
`;
