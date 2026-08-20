import type { z } from "zod";
import type { scheduleDraftSchema } from "../schemas/scheduleDraftSchema";

export type CalendarView = "month" | "agenda";
export type CalendarItemKind = "automation" | "cowork" | "approval";
export type CalendarItemStatus = "scheduled" | "attention" | "draft";

export type CalendarItem = {
  id: string;
  title: string;
  date: string;
  time: string;
  kind: CalendarItemKind;
  status: CalendarItemStatus;
  timezone: string;
  description: string;
  preview?: boolean;
};

export type ScheduleDraftValues = z.infer<typeof scheduleDraftSchema>;
export type ScheduleComposerProps = {
  onCreate: (values: ScheduleDraftValues) => void;
  onCancel: () => void;
};
