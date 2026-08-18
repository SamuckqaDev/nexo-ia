import type { ReactElement } from "react";
import type { ProviderConfiguration } from "../../../../provider/types/providerConfigurationTypes";
import { Picker } from "./styles";

type ModelPickerProps = {
  providers: ProviderConfiguration[];
  selectedProviderId: string | null;
  disabled: boolean;
  onSelect: (providerConfigurationId: string, selectedModel: string) => void;
};

/**
 * Lists only the user's own providers that already have a model selected, so the conversation can
 * never be pointed at another person's configuration.
 */
export function ModelPicker({
  providers,
  selectedProviderId,
  disabled,
  onSelect
}: ModelPickerProps): ReactElement {
  const change = (providerConfigurationId: string): void => {
    const provider = providers.find((item: ProviderConfiguration) => item.id === providerConfigurationId);
    if (provider?.selectedModel) onSelect(provider.id, provider.selectedModel);
  };

  return (
    <Picker
      aria-label="Model for this conversation"
      value={selectedProviderId ?? ""}
      disabled={disabled || providers.length === 0}
      onChange={(event): void => change(event.target.value)}
    >
      <option value="">{providers.length ? "Select a model" : "No model configured"}</option>
      {providers.map((provider: ProviderConfiguration) => (
        <option key={provider.id} value={provider.id}>
          {provider.displayName} · {provider.selectedModel}
        </option>
      ))}
    </Picker>
  );
}
