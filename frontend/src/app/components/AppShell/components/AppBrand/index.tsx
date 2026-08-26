import type { ReactElement } from "react";
import type { AppBrandProps } from "../../../../types/navigationTypes";
import { Brand, BrandName, BrandTagline, Logo } from "./styles";

export function AppBrand({ collapsed }: AppBrandProps): ReactElement {
  return (
    <Brand>
      <Logo src="/assets/logo/nexo-ia-symbol.png" alt="" />
      <BrandName $hidden={collapsed}>
        Nexo IA
        <BrandTagline>Your knowledge. Your control.</BrandTagline>
      </BrandName>
    </Brand>
  );
}
