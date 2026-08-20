import { ArrowRight, CheckCircle, CircleNotch, LockKey, ShieldCheck } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import { Button } from "../../../shared/components/Button";
import type { PlaceholderPageProps } from "../../types/navigationTypes";
import {
  Capability,
  CapabilityGrid,
  Content,
  Description,
  Eyebrow,
  Header,
  Hero,
  IconBox,
  Notice,
  Page,
  Release,
  Stage,
  Stages,
  Title
} from "./styles";

export function PlaceholderPage({
  title,
  eyebrow,
  description,
  release,
  icon: Icon,
  capabilities,
  onStartInChat
}: PlaceholderPageProps): ReactElement {
  return (
    <Page>
      <Header>
        <div><Eyebrow>{eyebrow}</Eyebrow><Title>{title}</Title></div>
        <Release>{release}</Release>
      </Header>

      <Hero>
        <IconBox><Icon size={34} weight="duotone" /></IconBox>
        <Content>
          <Description>{description}</Description>
          <Button type="button" icon={ArrowRight} onClick={onStartInChat}>Start with Chat</Button>
        </Content>
      </Hero>

      <CapabilityGrid>
        {capabilities.map((capability) => (
          <Capability key={capability.title}>
            <CheckCircle size={18} weight="duotone" />
            <div><strong>{capability.title}</strong><span>{capability.description}</span></div>
          </Capability>
        ))}
      </CapabilityGrid>

      <Stages aria-label={`${title} planned workflow`}>
        <Stage><LockKey size={18} /><div><strong>Authorize</strong><span>Select exact context and scope.</span></div></Stage>
        <Stage><CircleNotch size={18} /><div><strong>Work visibly</strong><span>Follow plans, progress and checkpoints.</span></div></Stage>
        <Stage><ShieldCheck size={18} /><div><strong>Verify</strong><span>Review evidence before completion.</span></div></Stage>
      </Stages>

      <Notice>
        <ShieldCheck size={18} weight="duotone" />
        <span>This surface is mapped from the Nexo roadmap. Its execution runtime is not enabled yet, so no action is simulated.</span>
      </Notice>
    </Page>
  );
}
