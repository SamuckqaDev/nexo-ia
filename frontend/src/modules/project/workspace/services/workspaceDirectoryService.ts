type DirectoryPickerWindow = Window & {
  showDirectoryPicker?: (options?: { id?: string; mode?: "read" | "readwrite" }) => Promise<FileSystemDirectoryHandle>;
};

export function supportsLocalDirectoryPicker(): boolean {
  const pickerWindow: DirectoryPickerWindow = window;
  return window.isSecureContext && typeof pickerWindow.showDirectoryPicker === "function";
}

export function chooseLocalWorkspaceDirectory(): Promise<FileSystemDirectoryHandle> {
  const pickerWindow: DirectoryPickerWindow = window;

  if (!supportsLocalDirectoryPicker() || !pickerWindow.showDirectoryPicker) {
    return Promise.reject(new Error("Persistent folder selection requires Chrome or Edge on HTTPS or localhost."));
  }

  return pickerWindow.showDirectoryPicker({ id: "nexo-project-workspace", mode: "read" });
}

export function isPickerCancellation(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
