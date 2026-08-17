import type { ButtonHTMLAttributes, InputHTMLAttributes } from "react";
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

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon?: Icon;
  variant?: "primary" | "outline";
};
