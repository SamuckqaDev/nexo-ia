import type { ReactElement } from "react";
import type { ButtonProps } from "../../types/componentTypes";
import { Button as StyledButton } from "./styles";

export function Button({ children, icon: LeadingIcon, variant = "primary", ...props }: ButtonProps): ReactElement {
  return (
    <StyledButton $variant={variant} {...props}>
      {LeadingIcon && <LeadingIcon aria-hidden size={20} weight="bold" />}
      {children}
    </StyledButton>
  );
}
