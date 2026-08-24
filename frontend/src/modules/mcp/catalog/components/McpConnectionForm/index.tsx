import { zodResolver } from "@hookform/resolvers/zod";
import { CloudArrowUp, X } from "@phosphor-icons/react";
import { useForm, type SubmitHandler } from "react-hook-form";
import type { ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import { Input } from "../../../../../shared/components/Input";
import { remoteMcpConnectionSchema, type RemoteMcpConnectionValues } from "../../schemas/mcpSchemas";
import { Actions, Form, Hint } from "./styles";

type McpConnectionFormProps = {
  pending: boolean;
  onSubmit: (values: RemoteMcpConnectionValues) => void;
  onCancel: () => void;
};

export function McpConnectionForm({ pending, onSubmit, onCancel }: McpConnectionFormProps): ReactElement {
  const { register, handleSubmit, formState: { errors } } = useForm<RemoteMcpConnectionValues>({
    resolver: zodResolver(remoteMcpConnectionSchema),
    defaultValues: { displayName: "", endpoint: "" }
  });
  const submit: SubmitHandler<RemoteMcpConnectionValues> = (values): void => onSubmit(values);

  return (
    <Form onSubmit={handleSubmit(submit)}>
      <Input
        id="mcp-display-name"
        label="Server name"
        placeholder="My local tools"
        error={errors.displayName?.message}
        {...register("displayName")}
      />
      <Input
        id="mcp-endpoint"
        label="Streamable HTTP endpoint"
        placeholder="https://mcp.example.com/mcp"
        error={errors.endpoint?.message}
        {...register("endpoint")}
      />
      <Hint>
        Nexo stores the endpoint, never credentials. Private network addresses stay blocked unless
        the backend operator explicitly enables them.
      </Hint>
      <Actions>
        <Button size="compact" type="button" variant="outline" icon={X} onClick={onCancel}>Cancel</Button>
        <Button size="compact" type="submit" icon={CloudArrowUp} disabled={pending} aria-busy={pending}>
          {pending ? "Inspecting…" : "Connect & inspect"}
        </Button>
      </Actions>
    </Form>
  );
}
