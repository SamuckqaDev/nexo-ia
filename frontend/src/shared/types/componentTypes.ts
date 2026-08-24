import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
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
  size?: "default" | "compact";
};

export type WorkspacePageProps = {
  eyebrow: string;
  title: string;
  description: string;
  icon: Icon;
  contentMode?: "scroll" | "contained";
  actions?: ReactNode;
  children: ReactNode;
};

export type WorkspacePanelProps = {
  title?: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
  as?: "section" | "aside";
};

export type WorkspaceBadgeProps = {
  children: ReactNode;
  tone?: "default" | "positive" | "attention";
};

export type SegmentedOption<T extends string> = { label: string; value: T };
export type WorkspaceSegmentedControlProps<T extends string> = {
  label: string;
  value: T;
  options: Array<SegmentedOption<T>>;
  onChange: (value: T) => void;
};

export type WorkspaceEmptyStateProps = {
  icon: Icon;
  title: string;
  description: string;
  action?: ReactNode;
};
