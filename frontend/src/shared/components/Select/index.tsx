import { forwardRef, type ReactElement } from "react";
import type { SelectProps } from "../../types/componentTypes";
import { Control, Error, Field, Helper, Label } from "./styles";

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { id, label, options, error, helperText, ...props },
  ref
): ReactElement {
  const errorId: string | undefined = error && id ? `${id}-error` : undefined;
  const helperId: string | undefined = helperText && id ? `${id}-helper` : undefined;
  const describedBy: string | undefined = [helperId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <Field>
      <Label htmlFor={id}>{label}</Label>
      <Control ref={ref} id={id} $invalid={Boolean(error)} aria-invalid={Boolean(error)} aria-describedby={describedBy} {...props}>
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </Control>
      {helperText && <Helper id={helperId}>{helperText}</Helper>}
      {error && <Error id={errorId}>{error}</Error>}
    </Field>
  );
});
