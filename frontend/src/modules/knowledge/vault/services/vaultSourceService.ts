import type { VaultSource } from "../types/vaultTypes";

const MAX_TEXT_PREVIEW_BYTES = 48 * 1024;
const readableExtensions: ReadonlySet<string> = new Set(["md", "txt", "json", "csv"]);

function formatSize(bytes: number): string {
  return bytes < 1024 * 1024
    ? `${Math.max(1, Math.round(bytes / 1024))} KB`
    : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function extensionOf(fileName: string): string {
  return fileName.split(".").pop()?.toLowerCase() ?? "";
}

export function createVaultSource(file: File): Promise<VaultSource> {
  const base: VaultSource = {
    id: crypto.randomUUID(),
    name: file.name,
    type: file.type || extensionOf(file.name).toUpperCase() || "Document",
    size: formatSize(file.size),
    status: "local"
  };

  if (!readableExtensions.has(extensionOf(file.name))) {
    return Promise.resolve({
      ...base,
      previewUnavailableReason: "Content preview requires the future document ingestion service. Only metadata is kept for this file."
    });
  }

  return file.slice(0, MAX_TEXT_PREVIEW_BYTES).text()
    .then((contentPreview: string): VaultSource => ({
      ...base,
      contentPreview,
      previewTruncated: file.size > MAX_TEXT_PREVIEW_BYTES
    }))
    .catch((): VaultSource => ({
      ...base,
      previewUnavailableReason: "Nexo could not read this text file in the current browser session."
    }));
}
