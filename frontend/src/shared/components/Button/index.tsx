import type { ReactElement } from "react";
import type { ButtonProps } from "../../types/componentTypes";
import { Button as StyledButton } from "./styles";

export function Button({
  children,
  icon: LeadingIcon,
  variant = "primary",
  size = "default",
  ...props
}: ButtonProps): ReactElement {
  return (
    <StyledButton $variant={variant} $size={size} {...props}>
      {LeadingIcon && <LeadingIcon aria-hidden size={size === "compact" ? 13 : 16} weight="bold" />}
      {children}
    </StyledButton>
  );
}
