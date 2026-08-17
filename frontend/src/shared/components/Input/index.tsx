import { forwardRef, useState, type ReactElement } from "react";
import { Eye, EyeSlash } from "@phosphor-icons/react";
import type { InputProps } from "../../types/componentTypes";
import { ActionButton, Control, Error, Field, Helper, IconButton, Label, NativeInput } from "./styles";

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { id, label, icon: LeadingIcon, error, helperText, action, type = "text", disabled, ...props },
  ref
): ReactElement {
  const [isPasswordVisible, setIsPasswordVisible] = useState<boolean>(false);
  const isPassword: boolean = type === "password";
  const inputType: InputProps["type"] = isPassword && isPasswordVisible ? "text" : type;
  const errorId: string | undefined = error && id ? `${id}-error` : undefined;
  const helperId: string | undefined = helperText && id ? `${id}-helper` : undefined;
  const describedBy: string | undefined = [helperId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <Field>
      <Label htmlFor={id}>{label}</Label>
      <Control $invalid={Boolean(error)}>
        {LeadingIcon && <LeadingIcon aria-hidden size={20} weight="duotone" />}
        <NativeInput
          ref={ref}
          id={id}
          type={inputType}
          disabled={disabled}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          {...props}
        />
        {action && (
          <ActionButton type="button" disabled={disabled} onClick={action.onClick}>
            {action.label}
          </ActionButton>
        )}
        {isPassword && (
          <IconButton
            type="button"
            disabled={disabled}
            aria-label={isPasswordVisible ? "Hide password" : "Show password"}
            aria-pressed={isPasswordVisible}
            onClick={() => setIsPasswordVisible((visible) => !visible)}
          >
            {isPasswordVisible ? <EyeSlash aria-hidden size={20} /> : <Eye aria-hidden size={20} />}
          </IconButton>
        )}
      </Control>
      {helperText && <Helper id={helperId}>{helperText}</Helper>}
      {error && <Error id={errorId}>{error}</Error>}
    </Field>
  );
});
