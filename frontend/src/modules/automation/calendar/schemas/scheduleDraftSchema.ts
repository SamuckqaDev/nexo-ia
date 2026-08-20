import { z } from "zod";

export const scheduleDraftSchema = z.object({
  title: z.string().trim().min(3, "Describe the scheduled work."),
  date: z.string().min(1, "Choose a date."),
  time: z.string().min(1, "Choose a time."),
  kind: z.enum(["automation", "cowork", "approval"]),
  timezone: z.string().trim().min(1, "Choose a timezone.")
});
