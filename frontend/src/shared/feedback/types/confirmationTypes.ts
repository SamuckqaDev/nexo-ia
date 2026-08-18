export type ConfirmationTone = "danger" | "warning";

export type ConfirmationRequest = {
  title: string;
  message: string;
  confirmLabel: string;
  tone: ConfirmationTone;
};

export type ConfirmationState = {
  request: ConfirmationRequest | null;
  resolver: ((confirmed: boolean) => void) | null;
  ask: (request: ConfirmationRequest) => Promise<boolean>;
  answer: (confirmed: boolean) => void;
};
