import type { ReactElement } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Button } from "../../../../shared/components/Button";
import { Input } from "../../../../shared/components/Input";
import { Select } from "../../../../shared/components/Select";
import { useProviderRegistry } from "../../hooks/useProviderRegistry";
import { providerConfigurationSchema, type ProviderConfigurationFormValues } from "../../schemas/providerConfigurationSchema";
import type { ProviderConfiguration } from "../../types/providerConfigurationTypes";
import { Fields, Form, Help } from "./styles";

type ProviderSetupProps = { provider?: ProviderConfiguration; onSaved?: () => void };

export function ProviderSetup({ provider, onSaved }: ProviderSetupProps): ReactElement {
  const { create, update } = useProviderRegistry();
  const { register, handleSubmit, formState: { errors } } = useForm<ProviderConfigurationFormValues>({
    resolver: zodResolver(providerConfigurationSchema),
    defaultValues: {
      providerType: provider?.providerType ?? "OLLAMA",
      displayName: provider?.displayName ?? "Ollama",
      endpoint: provider?.endpoint ?? "http://host.containers.internal:11434",
      selectedModel: provider?.selectedModel ?? ""
    }
  });
  const submit = (values: ProviderConfigurationFormValues): void => {
    if (provider) {
      update.mutate({ id: provider.id, input: values }, { onSuccess: (): void => onSaved?.() });
      return;
    }
    create.mutate(values, { onSuccess: (): void => onSaved?.() });
  };

  return (
    <Form onSubmit={handleSubmit(submit)} noValidate>
      <Help>Choose a provider and endpoint. Credentials will be added through the protected Secret Store before remote providers can be activated.</Help>
      <Fields>
        <Input id="provider-name" label="Provider name" error={errors.displayName?.message} {...register("displayName")} />
        <Input id="provider-endpoint" label="Endpoint URL" error={errors.endpoint?.message} {...register("endpoint")} />
        <Input id="provider-model" label="Default model (optional)" error={errors.selectedModel?.message} {...register("selectedModel")} />
        <Select
          id="provider-type"
          label="Provider type"
          error={errors.providerType?.message}
          options={[
            { value: "OLLAMA", label: "Ollama" },
            { value: "OPENAI", label: "OpenAI" },
            { value: "GOOGLE_GEMINI", label: "Google Gemini" },
            { value: "ANTHROPIC", label: "Anthropic" },
            { value: "OPENAI_COMPATIBLE", label: "OpenAI-compatible server" }
          ]}
          {...register("providerType")}
        />
      </Fields>
      <Button type="submit" disabled={create.isPending || update.isPending}>
        {provider ? update.isPending ? "Updating…" : "Update provider" : create.isPending ? "Saving…" : "Save provider"}
      </Button>
    </Form>
  );
}
