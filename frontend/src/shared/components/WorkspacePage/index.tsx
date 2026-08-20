import type { ElementType, ReactElement } from "react";
import type {
  WorkspaceBadgeProps,
  WorkspaceEmptyStateProps,
  WorkspacePageProps,
  WorkspacePanelProps,
  WorkspaceSegmentedControlProps
} from "../../types/componentTypes";
import {
  Badge,
  Description,
  Empty,
  EmptyIcon,
  Header,
  HeaderActions,
  HeaderCopy,
  HeaderIcon,
  Page,
  Panel,
  PanelBody,
  PanelCopy,
  PanelHeader,
  ScrollArea,
  SegmentButton,
  Segments,
  Title
} from "./styles";

export function WorkspacePage({ eyebrow, title, description, icon: Icon, actions, children }: WorkspacePageProps): ReactElement {
  return (
    <Page>
      <Header>
        <HeaderCopy>
          <HeaderIcon><Icon size={24} weight="duotone" /></HeaderIcon>
          <div><span>{eyebrow}</span><Title>{title}</Title><Description>{description}</Description></div>
        </HeaderCopy>
        {actions && <HeaderActions>{actions}</HeaderActions>}
      </Header>
      <ScrollArea>{children}</ScrollArea>
    </Page>
  );
}

export function WorkspacePanel({ title, description, action, children, as = "section" }: WorkspacePanelProps): ReactElement {
  return (
    <Panel as={as as ElementType}>
      {(title || description || action) && (
        <PanelHeader>
          <PanelCopy>{title && <h2>{title}</h2>}{description && <p>{description}</p>}</PanelCopy>
          {action}
        </PanelHeader>
      )}
      <PanelBody>{children}</PanelBody>
    </Panel>
  );
}

export function WorkspaceBadge({ children, tone = "default" }: WorkspaceBadgeProps): ReactElement {
  return <Badge $tone={tone}>{children}</Badge>;
}

export function WorkspaceSegmentedControl<T extends string>({ label, value, options, onChange }: WorkspaceSegmentedControlProps<T>): ReactElement {
  return (
    <Segments role="group" aria-label={label}>
      {options.map((option) => (
        <SegmentButton key={option.value} type="button" $active={value === option.value} onClick={(): void => onChange(option.value)}>
          {option.label}
        </SegmentButton>
      ))}
    </Segments>
  );
}

export function WorkspaceEmptyState({ icon: Icon, title, description, action }: WorkspaceEmptyStateProps): ReactElement {
  return (
    <Empty>
      <EmptyIcon><Icon size={25} weight="duotone" /></EmptyIcon>
      <strong>{title}</strong>
      <p>{description}</p>
      {action}
    </Empty>
  );
}
