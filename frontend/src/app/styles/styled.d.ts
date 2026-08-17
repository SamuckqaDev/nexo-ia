import "styled-components";
import type { NexoTheme } from "../types/themeTypes";

declare module "styled-components" {
  export interface DefaultTheme extends NexoTheme {}
}
