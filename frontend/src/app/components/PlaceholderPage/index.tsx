import type { ReactElement } from "react";
import type { PlaceholderPageProps } from "../../types/navigationTypes";
import { Content, Description, IconBox, Page, Title } from "./styles";
export function PlaceholderPage({ title, description, icon: Icon }: PlaceholderPageProps): ReactElement {
  return <Page><Content><IconBox><Icon size={30} weight="duotone" /></IconBox><Title>{title}</Title><Description>{description}</Description></Content></Page>;
}
