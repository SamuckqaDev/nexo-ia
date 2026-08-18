import type { ReactElement } from "react";
import { Core, Label, Orbit, Visual, Wrapper } from "./styles";

type LoadingProps = {
  label?: string;
  size?: number;
};

export function Loading({ label = "Working…", size = 52 }: LoadingProps): ReactElement {
  return (
    <Wrapper role="status" aria-live="polite">
      <Visual $size={size} aria-hidden="true">
        <Orbit $delay="0s" $inset="8%" />
        <Orbit $delay="-0.6s" $inset="20%" />
        <Core />
      </Visual>
      <Label>{label}</Label>
    </Wrapper>
  );
}
