import { zodResolver } from "@hookform/resolvers/zod";
import { CalendarPlus, X } from "@phosphor-icons/react";
import { useForm, type SubmitHandler } from "react-hook-form";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { Select } from "../../../../../shared/components/Select";
import { scheduleDraftSchema } from "../../schemas/scheduleDraftSchema";
import type { ScheduleComposerProps, ScheduleDraftValues } from "../../types/calendarTypes";
import { Actions, Form, FormNotice, Row } from "./styles";

const today = (): string => {
  const current = new Date();
  return [current.getFullYear(), String(current.getMonth() + 1).padStart(2, "0"), String(current.getDate()).padStart(2, "0")].join("-");
};

export function ScheduleComposer({ onCreate, onCancel }: ScheduleComposerProps): ReactElement {
  const { register, handleSubmit, formState: { errors } } = useForm<ScheduleDraftValues>({
    resolver: zodResolver(scheduleDraftSchema),
    defaultValues: { title: "", date: today(), time: "09:00", kind: "automation", timezone: "America/Sao_Paulo" }
  });
  const submit: SubmitHandler<ScheduleDraftValues> = (values): void => onCreate(values);

  return (
    <Form onSubmit={handleSubmit(submit)}>
      <Input id="schedule-title" label="Objective" placeholder="What should Nexo do?" error={errors.title?.message} {...register("title")} />
      <Row>
        <Input id="schedule-date" label="Date" type="date" error={errors.date?.message} {...register("date")} />
        <Input id="schedule-time" label="Time" type="time" error={errors.time?.message} {...register("time")} />
      </Row>
      <Select
        id="schedule-kind"
        label="Calendar item"
        options={[
          { label: "Automation", value: "automation" },
          { label: "Cowork milestone", value: "cowork" },
          { label: "Approval deadline", value: "approval" }
        ]}
        {...register("kind")}
      />
      <Select
        id="schedule-timezone"
        label="Timezone"
        options={[
          { label: "São Paulo · UTC−03:00", value: "America/Sao_Paulo" },
          { label: "UTC", value: "UTC" },
          { label: "New York", value: "America/New_York" }
        ]}
        {...register("timezone")}
      />
      <FormNotice>This creates a session-only draft. Execution stays unavailable until the scheduler API is connected.</FormNotice>
      <Actions>
        <Button type="button" variant="outline" icon={X} onClick={onCancel}>Cancel</Button>
        <Button type="submit" icon={CalendarPlus}>Add draft</Button>
      </Actions>
    </Form>
  );
}
