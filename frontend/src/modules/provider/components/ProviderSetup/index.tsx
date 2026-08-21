import type { ReactElement } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Button } from "../../../../shared/components/Button";
import { Input } from "../../../../shared/components/Input";
import { Select } from "../../../../shared/components/Select";
import { useProviderRegistry } from "../../hooks/useProviderRegistry";
import { providerConfigurationSchema, type ProviderConfigurationFormValues } from "../../schemas/providerConfigurationSchema";
import type { ProviderConfiguration } from "../../types/providerConfigurationTypes";
import { Fields, Form, Help, TestResult } from "./styles";

type ProviderSetupProps = { provider?: ProviderConfiguration; onSaved?: () => void };

const TEST_STATUS_COPY: Record<string, string> = {
  AVAILABLE: "Connected. Nexo can reach this provider and discovered its models.",
  EMPTY: "Connected, but no models were reported by this provider yet.",
  UNAVAILABLE: "This provider could not be reached at the given endpoint.",
  UNSUPPORTED: "Connection testing is not available for this provider type yet."
};

export function ProviderSetup({ provider, onSaved }: ProviderSetupProps): ReactElement {
  const { create, update, test } = useProviderRegistry();
  const { register, handleSubmit, watch, formState: { errors } } = useForm<ProviderConfigurationFormValues>({
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
  const testConnection = (): void => {
    test.mutate({ providerType: watch("providerType"), endpoint: watch("endpoint") });
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
      <Button type="button" variant="outline" disabled={test.isPending} onClick={testConnection}>
        {test.isPending ? "Testing…" : "Test connection"}
      </Button>
      {test.data && (
        <TestResult $ok={test.data.status === "AVAILABLE"}>
          {test.data.message ?? TEST_STATUS_COPY[test.data.status]}
        </TestResult>
      )}
      <Button type="submit" disabled={create.isPending || update.isPending}>
        {provider ? update.isPending ? "Updating…" : "Update provider" : create.isPending ? "Saving…" : "Save provider"}
      </Button>
    </Form>
  );
}
