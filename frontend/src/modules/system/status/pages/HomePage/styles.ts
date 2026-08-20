import styled from "styled-components";

export const Page = styled.section`
  display: grid;
  width: 100%;
  min-height: calc(100vh - ${({ theme }) => theme.spacing.xl});
  gap: ${({ theme }) => theme.spacing.lg};
  align-content: start;
  padding: ${({ theme }) => theme.spacing.lg};

  @media (max-width: 40rem) {
    padding: ${({ theme }) => theme.spacing.md};
  }
`;

/* ---------- hero ---------- */

export const Hero = styled.section`
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(14rem, auto);
  align-items: center;
  gap: ${({ theme }) => theme.spacing.lg};
  padding: ${({ theme }) => theme.spacing.lg};
  border-radius: ${({ theme }) => theme.radius.md};
  background:
    radial-gradient(circle at 92% 0, ${({ theme }) => theme.colors.surfaceAccent}, transparent 18rem),
    ${({ theme }) => theme.colors.surfaceStrong};

  @media (max-width: 44rem) {
    grid-template-columns: 1fr;
  }
`;

export const Intro = styled.div`
  min-width: 0;
`;

export const Eyebrow = styled.p`
  margin: 0 0 ${({ theme }) => theme.spacing.xs};
  color: ${({ theme }) => theme.colors.primarySoft};
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
`;

export const Greeting = styled.h1`
  margin: 0;
  font-size: clamp(1.5rem, 3.5vw, 2.1rem);
  line-height: 1.1;
  letter-spacing: -0.03em;
`;

export const Summary = styled.p`
  max-width: 40rem;
  margin: ${({ theme }) => theme.spacing.sm} 0 0;
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.86rem;
  line-height: 1.6;
`;

export const CommandComposer = styled.form`
  display: grid;
  gap: ${({ theme }) => theme.spacing.sm};
  max-width: 52rem;
  margin-top: ${({ theme }) => theme.spacing.lg};
  padding: ${({ theme }) => theme.spacing.sm};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.background};
  box-shadow: inset 0 0 0 1px ${({ theme }) => theme.colors.lineStrong};
`;

export const CommandInput = styled.textarea`
  width: 100%;
  min-height: 7rem;
  resize: vertical;
  border: 0;
  outline: 0;
  padding: ${({ theme }) => theme.spacing.sm};
  background: transparent;
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  font-size: 0.92rem;
  line-height: 1.6;

  &::placeholder { color: ${({ theme }) => theme.colors.textSubtle}; }
`;

export const CommandFooter = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const CommandHint = styled.button`
  display: flex;
  align-items: center;
  gap: 0.35rem;
  overflow: hidden;
  border: 0;
  padding: 0;
  background: transparent;
  color: ${({ theme }) => theme.colors.textSubtle};
  font: inherit;
  font-size: 0.66rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  &:hover, &:focus-visible { color: ${({ theme }) => theme.colors.primary}; }
`;

export const CommandSubmit = styled.button`
  display: flex;
  align-items: center;
  gap: 0.35rem;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.55rem 0.8rem;
  background: ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.background};
  font: inherit;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;

  &:disabled { cursor: not-allowed; opacity: 0.45; }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primarySoft}; outline-offset: 2px; }
`;

export const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing.sm};
  margin-top: ${({ theme }) => theme.spacing.md};
`;

export const ActionButton = styled.button<{ $secondary?: boolean }>`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.xs};
  border: 1px solid ${({ theme, $secondary }) => ($secondary ? theme.colors.line : theme.colors.primary)};
  border-radius: ${({ theme }) => theme.radius.control};
  padding: 0.6rem 0.9rem;
  background: ${({ theme, $secondary }) => ($secondary ? "transparent" : theme.colors.primary)};
  color: ${({ theme, $secondary }) => ($secondary ? theme.colors.primarySoft : theme.colors.background)};
  font: inherit;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;

  &:hover {
    border-color: ${({ theme }) => theme.colors.lineStrong};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

export const SystemCard = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  padding: ${({ theme }) => `${theme.spacing.sm} ${theme.spacing.md}`};
  border: 1px solid ${({ theme }) => theme.colors.lineStrong};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surface};
  color: ${({ theme }) => theme.colors.primary};

  strong {
    display: block;
    margin-bottom: 0.15rem;
    color: ${({ theme }) => theme.colors.text};
    font-size: 0.78rem;
  }
`;

/* ---------- quick stats ---------- */

export const StatStrip = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.md};

  @media (max-width: 44rem) {
    grid-template-columns: 1fr;
  }
`;

export const StatTile = styled.button`
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.md};
  border: 0;
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.md};
  background: ${({ theme }) => theme.colors.surface};
  color: ${({ theme }) => theme.colors.textMuted};
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease;

  &:hover {
    background: ${({ theme }) => theme.colors.surfaceAccent};
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

export const StatIcon = styled.span<{ $accent?: boolean }>`
  display: grid;
  width: 2.4rem;
  height: 2.4rem;
  place-items: center;
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme, $accent }) => ($accent ? theme.colors.dangerSurface : theme.colors.surfaceAccent)};
  color: ${({ theme, $accent }) => ($accent ? theme.colors.accentSoft : theme.colors.primary)};
  line-height: 0;

  svg { display: block; }
`;

export const StatBody = styled.span`
  display: grid;
  gap: 0.1rem;
  min-width: 0;

  span {
    color: ${({ theme }) => theme.colors.textSubtle};
    font-size: 0.68rem;
    font-weight: 600;
    letter-spacing: 0.03em;
    text-transform: uppercase;
  }

  strong {
    overflow: hidden;
    color: ${({ theme }) => theme.colors.text};
    font-size: 1.02rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

/* ---------- main columns ---------- */

export const Columns = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: ${({ theme }) => theme.spacing.lg};
  align-items: start;

  @media (max-width: 60rem) {
    grid-template-columns: 1fr;
  }
`;

export const Stack = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.lg};
  align-content: start;
`;

export const Panel = styled.section`
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.lg};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.surface};
`;

export const PanelHeader = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.md};
`;

export const PanelCopy = styled.div`
  display: flex;
  min-width: 0;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};

  > div > span {
    display: block;
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.7rem;
  }
`;

export const PanelIcon = styled.span<{ $accent?: boolean }>`
  display: grid;
  width: 2.4rem;
  height: 2.4rem;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid ${({ theme, $accent }) => ($accent ? theme.colors.accent : theme.colors.lineStrong)};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme, $accent }) => ($accent ? theme.colors.dangerSurface : theme.colors.surfaceAccent)};
  color: ${({ theme, $accent }) => ($accent ? theme.colors.accentSoft : theme.colors.primary)};
  line-height: 0;

  svg { display: block; }
`;

export const PanelTitle = styled.h2`
  margin: 0 0 0.15rem;
  font-size: 0.92rem;
`;

export const PanelAction = styled.button`
  display: flex;
  align-items: center;
  gap: 0.35rem;
  width: max-content;
  border: 0;
  padding: 0;
  background: transparent;
  color: ${({ theme }) => theme.colors.primarySoft};
  font: inherit;
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;

  &:hover { color: ${({ theme }) => theme.colors.primary}; }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

/* ---------- provider ---------- */

export const ProviderState = styled.strong`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing.sm};
  color: ${({ theme }) => theme.colors.textMuted};
  font-size: 0.95rem;
`;

export const StatusDot = styled.span<{ $online: boolean }>`
  width: 0.6rem;
  height: 0.6rem;
  border-radius: ${({ theme }) => theme.radius.round};
  background: ${({ theme, $online }) => ($online ? theme.colors.statusOnline : theme.colors.statusOffline)};
`;

export const ProviderDetails = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.xs};

  span {
    display: flex;
    justify-content: space-between;
    gap: ${({ theme }) => theme.spacing.md};
    padding-bottom: ${({ theme }) => theme.spacing.xs};
    border-bottom: 1px solid ${({ theme }) => theme.colors.line};
    color: ${({ theme }) => theme.colors.textMuted};
    font-size: 0.74rem;
  }

  span:last-child { border-bottom: 0; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.76rem; }
`;

/* ---------- usage mini ---------- */

export const MetricRow = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: ${({ theme }) => theme.spacing.sm};
`;

export const Metric = styled.div`
  display: grid;
  gap: 0.2rem;
  padding: ${({ theme }) => theme.spacing.sm};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceStrong};

  span { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.66rem; text-transform: uppercase; letter-spacing: 0.04em; }
  strong { font-size: 1.05rem; font-variant-numeric: tabular-nums; }
`;

/* ---------- empty & conversations ---------- */

export const EmptyState = styled.div`
  display: grid;
  justify-items: center;
  align-content: center;
  gap: ${({ theme }) => theme.spacing.xs};
  padding: ${({ theme }) => theme.spacing.lg};
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.textMuted};
  text-align: center;

  strong { color: ${({ theme }) => theme.colors.text}; font-size: 0.84rem; }
  span { max-width: 24rem; font-size: 0.72rem; line-height: 1.5; }
`;

export const EmptyIcon = styled.span`
  display: grid;
  width: 2.6rem;
  height: 2.6rem;
  margin-bottom: 0.15rem;
  place-items: center;
  border-radius: ${({ theme }) => theme.radius.control};
  background: ${({ theme }) => theme.colors.surfaceAccent};
  color: ${({ theme }) => theme.colors.primary};
`;

export const ConversationList = styled.div`
  display: grid;
  gap: ${({ theme }) => theme.spacing.xs};
`;

export const ConversationRow = styled.button`
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.15rem ${({ theme }) => theme.spacing.md};
  border: 0;
  border-radius: ${({ theme }) => theme.radius.control};
  padding: ${({ theme }) => theme.spacing.sm};
  background: ${({ theme }) => theme.colors.surfaceStrong};
  color: ${({ theme }) => theme.colors.text};
  font: inherit;
  text-align: left;
  cursor: pointer;

  span, small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span { font-size: 0.8rem; font-weight: 600; }
  small { color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.66rem; }
  svg { grid-column: 2; grid-row: 1 / span 2; color: ${({ theme }) => theme.colors.primary}; }
  &:hover { background: ${({ theme }) => theme.colors.surfaceAccent}; }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: 2px; }
`;

export const WorkspaceSection = styled.section`
  display: grid;
  gap: ${({ theme }) => theme.spacing.md};
  padding: ${({ theme }) => theme.spacing.lg} 0 0;
`;

export const SectionHead = styled.header`
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing.lg};

  h2 { margin: 0; font-size: 1.2rem; }
  > span { max-width: 22rem; color: ${({ theme }) => theme.colors.textSubtle}; font-size: 0.7rem; text-align: right; }

  @media (max-width: 44rem) {
    align-items: start;
    flex-direction: column;
    gap: ${({ theme }) => theme.spacing.xs};
    > span { text-align: left; }
  }
`;

export const CapabilityGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing.md};

  @media (max-width: 44rem) {
    grid-template-columns: 1fr;
  }
`;

export const CapabilityItem = styled.button<{ $available?: boolean }>`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: ${({ theme }) => theme.spacing.md};
  border: 0;
  border-radius: ${({ theme }) => theme.radius.md};
  padding: ${({ theme }) => theme.spacing.lg};
  background: ${({ theme, $available }) => $available ? theme.colors.surfaceAccent : theme.colors.surface};
  color: ${({ theme, $available }) => $available ? theme.colors.primary : theme.colors.textSubtle};
  font: inherit;
  text-align: left;
  cursor: pointer;

  strong { display: block; color: ${({ theme }) => theme.colors.text}; font-size: 0.9rem; }
  small { display: block; margin-top: 0.25rem; color: ${({ theme }) => theme.colors.textMuted}; font-size: 0.7rem; line-height: 1.5; }
  &:hover { background: ${({ theme }) => theme.colors.surfaceAccent}; }
  &:focus-visible { outline: 2px solid ${({ theme }) => theme.colors.primary}; outline-offset: 2px; }
`;

export const CapabilityTag = styled.span<{ $available?: boolean }>`
  display: inline-block;
  margin-top: ${({ theme }) => theme.spacing.xs};
  color: ${({ theme, $available }) => $available ? theme.colors.primarySoft : theme.colors.textSubtle};
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
`;
