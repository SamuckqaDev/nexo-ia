type DirectoryPickerWindow = Window & {
  showDirectoryPicker?: (options?: { id?: string; mode?: "read" | "readwrite" }) => Promise<FileSystemDirectoryHandle>;
};

export function supportsLocalDirectoryPicker(): boolean {
  const pickerWindow: DirectoryPickerWindow = window;
  const loopbackOrigin: boolean = window.location.hostname === "localhost"
    || window.location.hostname === "127.0.0.1"
    || window.location.hostname === "[::1]";
  // Chromium browsers (including Brave) treat loopback as a trusted local origin. Some embedded
  // previews report a false `isSecureContext` even though the directory picker is available.
  return typeof pickerWindow.showDirectoryPicker === "function"
    && (window.isSecureContext || loopbackOrigin);
}

export function chooseLocalWorkspaceDirectory(): Promise<FileSystemDirectoryHandle> {
  const pickerWindow: DirectoryPickerWindow = window;

  if (!supportsLocalDirectoryPicker() || !pickerWindow.showDirectoryPicker) {
    return Promise.reject(new Error("Persistent folder selection requires Brave, Chrome or Edge on HTTPS, localhost, or 127.0.0.1."));
  }

  return pickerWindow.showDirectoryPicker({ id: "nexo-project-workspace", mode: "read" });
}

export function isPickerCancellation(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
