import type { ReactElement } from "react";
import type {
  ProviderModel,
  ProviderModelCatalogView
} from "../../../../provider/types/providerConfigurationTypes";
import { Field, Picker, Status } from "./styles";

type ModelPickerProps = {
  catalogs: ProviderModelCatalogView[];
  selectedProviderId: string | null;
  selectedModel: string | null;
  disabled: boolean;
  isSaving?: boolean;
  errorMessage?: string | null;
  onSelect: (providerConfigurationId: string, selectedModel: string) => void;
};

const selectionValue = (providerConfigurationId: string, model: string): string =>
  JSON.stringify([providerConfigurationId, model]);

const catalogModels = (
  catalog: ProviderModelCatalogView,
  selectedProviderId: string | null,
  selectedModel: string | null
): ProviderModel[] => {
  const models: ProviderModel[] = [...catalog.models];
  const fallback: string | null = catalog.status === "LOADING"
    || catalog.status === "UNAVAILABLE"
    || catalog.status === "UNSUPPORTED"
    ? catalog.selectedModel
    : null;
  const current: string | null = catalog.providerConfigurationId === selectedProviderId
    && catalog.status !== "EMPTY"
    ? selectedModel
    : null;

  [fallback, current].forEach((name: string | null): void => {
    if (name && !models.some((model: ProviderModel) => model.name === name)) {
      models.push({
        name,
        modifiedAt: null,
        size: null,
        toolCallingSupported: null,
        thinkingSupported: null
      });
    }
  });
  return models;
};

const statusLabel = (catalog: ProviderModelCatalogView): string | null => {
  if (catalog.status === "LOADING") return "Loading models…";
  if (catalog.status === "EMPTY") return "No installed models";
  if (catalog.status === "UNSUPPORTED") return "Discovery not supported";
  if (catalog.status === "UNAVAILABLE") return "Provider unavailable";
  return null;
};

export function ModelPicker({
  catalogs,
  selectedProviderId,
  selectedModel,
  disabled,
  isSaving = false,
  errorMessage = null,
  onSelect
}: ModelPickerProps): ReactElement {
  const selectedValue: string = selectedProviderId && selectedModel
    ? selectionValue(selectedProviderId, selectedModel)
    : "";
  const selectableCount: number = catalogs.reduce((count: number, catalog: ProviderModelCatalogView) =>
    count + catalogModels(catalog, selectedProviderId, selectedModel).length, 0);
  const discoveredCount: number = catalogs.reduce((count: number, catalog: ProviderModelCatalogView) =>
    count + catalog.models.length, 0);
  const loading: boolean = catalogs.some((catalog: ProviderModelCatalogView) => catalog.status === "LOADING");
  const unavailable: boolean = catalogs.some((catalog: ProviderModelCatalogView) => catalog.status === "UNAVAILABLE");
  const summary: string = isSaving
    ? "Saving model for this conversation…"
    : errorMessage
      ? `Could not save model · ${errorMessage}`
      : loading
    ? "Refreshing provider models"
    : discoveredCount > 0
      ? `${discoveredCount} model${discoveredCount === 1 ? "" : "s"} reported by providers`
      : unavailable && selectableCount > 0
        ? "Provider unavailable · configured fallback shown"
        : "No models reported by providers";

  const change = (value: string): void => {
    if (!value) return;
    try {
      const parsed: unknown = JSON.parse(value);
      if (!Array.isArray(parsed) || parsed.length !== 2) return;
      const [providerConfigurationId, model]: unknown[] = parsed;
      if (typeof providerConfigurationId === "string" && typeof model === "string") {
        onSelect(providerConfigurationId, model);
      }
    } catch {
      return;
    }
  };

  return (
    <Field>
      <Picker
        id="conversation-model"
        aria-label="Model for this conversation"
        value={selectedValue}
        disabled={disabled || isSaving || selectableCount === 0}
        onChange={(event): void => change(event.target.value)}
      >
        <option value="">
          {!catalogs.length ? "No provider configured" : selectableCount ? "Select a model" : "No model available"}
        </option>
        {catalogs.map((catalog: ProviderModelCatalogView) => {
          const models: ProviderModel[] = catalogModels(catalog, selectedProviderId, selectedModel);
          const catalogStatus: string | null = statusLabel(catalog);
          return (
            <optgroup key={catalog.providerConfigurationId} label={catalog.displayName}>
              {models.map((model: ProviderModel) => (
                <option
                  key={model.name}
                  value={selectionValue(catalog.providerConfigurationId, model.name)}
                >
                  {model.name}{model.toolCallingSupported === true
                    ? " · Agent ready"
                    : model.toolCallingSupported === false ? " · no tools" : ""}{model.thinkingSupported === true
                    ? " · Thinking"
                    : model.thinkingSupported === false ? " · no Thinking" : ""}{model.name === catalog.selectedModel
                    ? catalog.status === "AVAILABLE" ? " · default" : " · configured fallback"
                    : ""}
                </option>
              ))}
              {catalogStatus && <option disabled>{catalogStatus}</option>}
            </optgroup>
          );
        })}
      </Picker>
      <Status aria-live="polite" $error={Boolean(errorMessage)}>{summary}</Status>
    </Field>
  );
}
