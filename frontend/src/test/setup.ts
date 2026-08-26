import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Node 24 can expose an incomplete experimental localStorage that shadows JSDOM's Storage.
if (typeof globalThis.localStorage?.clear !== "function") {
  const values = new Map<string, string>();
  const testStorage: Storage = {
    get length(): number { return values.size; },
    clear: (): void => values.clear(),
    getItem: (key: string): string | null => values.get(key) ?? null,
    key: (index: number): string | null => [...values.keys()][index] ?? null,
    removeItem: (key: string): void => { values.delete(key); },
    setItem: (key: string, value: string): void => { values.set(key, String(value)); }
  };
  Object.defineProperty(globalThis, "localStorage", {
    configurable: true,
    value: testStorage
  });
}

// Vitest runs without global injection, so Testing Library's automatic cleanup never registers.
// Without this, every render stays mounted and later assertions match elements from earlier tests.
afterEach(cleanup);
