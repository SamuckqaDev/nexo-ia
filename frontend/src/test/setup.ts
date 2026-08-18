import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Vitest runs without global injection, so Testing Library's automatic cleanup never registers.
// Without this, every render stays mounted and later assertions match elements from earlier tests.
afterEach(cleanup);
