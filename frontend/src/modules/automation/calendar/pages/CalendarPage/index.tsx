import {
  ArrowLeft,
  ArrowRight,
  CalendarCheck,
  CalendarPlus,
  Clock,
  Lightning,
  UserFocus
} from "@phosphor-icons/react";
import { useMemo, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import {
  WorkspaceBadge,
  WorkspaceEmptyState,
  WorkspacePage,
  WorkspacePanel,
  WorkspaceSegmentedControl
} from "../../../../../shared/components/WorkspacePage";
import { ScheduleComposer } from "../../components/ScheduleComposer";
import type { CalendarItem, CalendarView, ScheduleDraftValues } from "../../types/calendarTypes";
import {
  AgendaItem,
  AgendaList,
  CalendarGrid,
  CalendarToolbar,
  Day,
  DayEvent,
  DayMore,
  DayNumber,
  Detail,
  DetailGrid,
  DetailRow,
  Grid,
  IconButton,
  MonthLabel,
  PreviewNote,
  Weekday
} from "./styles";

const dateKey = (date: Date): string => [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-");
const sameMonth = (left: Date, right: Date): boolean => left.getMonth() === right.getMonth() && left.getFullYear() === right.getFullYear();

const previewItems = (base: Date): CalendarItem[] => [
  { id: "preview-1", title: "Provider health review", date: dateKey(new Date(base.getFullYear(), base.getMonth(), 6)), time: "09:00", kind: "automation", status: "scheduled", timezone: "America/Sao_Paulo", description: "Review configured providers and prepare a health summary.", preview: true },
  { id: "preview-2", title: "Cowork checkpoint", date: dateKey(new Date(base.getFullYear(), base.getMonth(), 13)), time: "14:30", kind: "cowork", status: "scheduled", timezone: "America/Sao_Paulo", description: "Review progress, evidence and decisions required from the team.", preview: true },
  { id: "preview-3", title: "Approve release notes", date: dateKey(new Date(base.getFullYear(), base.getMonth(), 21)), time: "16:00", kind: "approval", status: "attention", timezone: "America/Sao_Paulo", description: "Human approval deadline before the scheduled release workflow.", preview: true }
];

export function CalendarPage(): ReactElement {
  const [cursor, setCursor] = useState<Date>(new Date());
  const [view, setView] = useState<CalendarView>("month");
  const [items, setItems] = useState<CalendarItem[]>(() => previewItems(new Date()));
  const [selectedId, setSelectedId] = useState<string | undefined>(items[0]?.id);
  const [composing, setComposing] = useState<boolean>(false);
  const selected: CalendarItem | undefined = items.find((item) => item.id === selectedId);
  const days = useMemo<Date[]>(() => {
    const first = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
    return Array.from({ length: 42 }, (_, index: number) => new Date(cursor.getFullYear(), cursor.getMonth(), index - first.getDay() + 1));
  }, [cursor]);
  const monthItems: CalendarItem[] = items.filter((item) => {
    const date = new Date(`${item.date}T12:00:00`);
    return sameMonth(date, cursor);
  }).sort((left, right) => `${left.date}${left.time}`.localeCompare(`${right.date}${right.time}`));

  const createDraft = (values: ScheduleDraftValues): void => {
    const item: CalendarItem = { id: crypto.randomUUID(), ...values, status: "draft", description: "Session-only schedule draft." };
    setItems((current) => [...current, item]);
    setCursor(new Date(`${values.date}T12:00:00`));
    setSelectedId(item.id);
    setComposing(false);
  };

  return (
    <WorkspacePage
      eyebrow="Scheduled work"
      title="Tasks & calendar"
      description="See automations, Cowork milestones and approval deadlines together without turning the calendar into a second scheduler."
      icon={CalendarCheck}
      contentMode="contained"
      actions={<Button type="button" icon={CalendarPlus} onClick={(): void => setComposing(true)}>New schedule</Button>}
    >
      <PreviewNote><span>Interface preview</span> Example occurrences are marked and never executed. New items remain local to this session.</PreviewNote>
      <Grid>
        <WorkspacePanel
          title="Calendar"
          description="Month and agenda views share the same filtered occurrence set."
          action={<WorkspaceSegmentedControl label="Calendar view" value={view} options={[{ label: "Month", value: "month" }, { label: "Agenda", value: "agenda" }]} onChange={setView} />}
        >
          <CalendarToolbar>
            <div>
              <IconButton type="button" aria-label="Previous month" onClick={(): void => setCursor(new Date(cursor.getFullYear(), cursor.getMonth() - 1, 1))}><ArrowLeft size={16} /></IconButton>
              <Button type="button" variant="outline" onClick={(): void => setCursor(new Date())}>Today</Button>
              <IconButton type="button" aria-label="Next month" onClick={(): void => setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1))}><ArrowRight size={16} /></IconButton>
            </div>
            <MonthLabel>{cursor.toLocaleDateString(undefined, { month: "long", year: "numeric" })}</MonthLabel>
          </CalendarToolbar>
          {view === "month" ? (
            <CalendarGrid>
              {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((name) => <Weekday key={name}>{name}</Weekday>)}
              {days.map((day) => {
                const dayItems: CalendarItem[] = items.filter((item) => item.date === dateKey(day));
                return (
                  <Day key={dateKey(day)} $outside={!sameMonth(day, cursor)}>
                    <DayNumber>{day.getDate()}</DayNumber>
                    {dayItems.slice(0, 2).map((item) => (
                      <DayEvent key={item.id} type="button" $kind={item.kind} $active={selectedId === item.id} onClick={(): void => { setSelectedId(item.id); setComposing(false); }}>
                        <span>{item.time}</span>{item.title}
                      </DayEvent>
                    ))}
                    {dayItems.length > 2 && <DayMore>+{dayItems.length - 2} more in agenda</DayMore>}
                  </Day>
                );
              })}
            </CalendarGrid>
          ) : monthItems.length ? (
            <AgendaList>
              {monthItems.map((item) => (
                <AgendaItem key={item.id} type="button" $active={selectedId === item.id} onClick={(): void => { setSelectedId(item.id); setComposing(false); }}>
                  <strong>{new Date(`${item.date}T12:00:00`).toLocaleDateString(undefined, { weekday: "short", day: "2-digit", month: "short" })}</strong>
                  <span>{item.time}</span><div><b>{item.title}</b><small>{item.kind}</small></div>
                </AgendaItem>
              ))}
            </AgendaList>
          ) : <WorkspaceEmptyState icon={CalendarCheck} title="Nothing in this month" description="Move to another month or add a session-only schedule draft." />}
        </WorkspacePanel>

        <WorkspacePanel as="aside" title={composing ? "New schedule" : "Occurrence details"} description={composing ? "Define when the work should appear." : "Inspect timing, type and current state."}>
          {composing ? <ScheduleComposer onCreate={createDraft} onCancel={(): void => setComposing(false)} /> : selected ? (
            <Detail>
              <div><WorkspaceBadge tone={selected.preview ? "attention" : "default"}>{selected.preview ? "Preview data" : "Session draft"}</WorkspaceBadge><h2>{selected.title}</h2><p>{selected.description}</p></div>
              <DetailGrid>
                <DetailRow><CalendarCheck size={18} /><span>Date<strong>{new Date(`${selected.date}T12:00:00`).toLocaleDateString()}</strong></span></DetailRow>
                <DetailRow><Clock size={18} /><span>Time<strong>{selected.time} · {selected.timezone}</strong></span></DetailRow>
                <DetailRow>{selected.kind === "cowork" ? <UserFocus size={18} /> : <Lightning size={18} />}<span>Type<strong>{selected.kind}</strong></span></DetailRow>
              </DetailGrid>
              <Button type="button" variant="outline" disabled>Scheduler API required</Button>
            </Detail>
          ) : <WorkspaceEmptyState icon={CalendarCheck} title="Select an occurrence" description="Choose an item in the calendar to inspect its schedule and permissions." />}
        </WorkspacePanel>
      </Grid>
    </WorkspacePage>
  );
}
