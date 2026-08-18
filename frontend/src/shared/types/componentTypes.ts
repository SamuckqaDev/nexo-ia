import type { ButtonHTMLAttributes, InputHTMLAttributes, SelectHTMLAttributes } from "react";
import type { Icon } from "@phosphor-icons/react";

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  icon?: Icon;
  error?: string;
  helperText?: string;
  action?: InputAction;
};

export type InputAction = {
  label: string;
  onClick: () => void;
};

export type SelectOption<T extends string = string> = { label: string; value: T };

export type SelectProps<T extends string = string> = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string;
  options: Array<SelectOption<T>>;
  error?: string;
  helperText?: string;
};

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: Icon;
  variant?: "primary" | "outline";
};
