import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import type { AuthenticatedUser } from "../../../../auth/types/authTypes";
import { TeamsPage } from "./index";

const listTeams = vi.fn();
const listTeamMembers = vi.fn();
const listTeamCandidates = vi.fn();
const listBackendVaults = vi.fn();

vi.mock("../../api/teamApi", () => ({
  listTeams: (): Promise<unknown> => listTeams(),
  listTeamMembers: (): Promise<unknown> => listTeamMembers(),
  listTeamCandidates: (): Promise<unknown> => listTeamCandidates(),
  createTeam: vi.fn(),
  addTeamMember: vi.fn(),
  createTeamVault: vi.fn()
}));

vi.mock("../../../../knowledge/vault/api/vaultApi", () => ({
  listBackendVaults: (): Promise<unknown> => listBackendVaults(),
  createBackendVault: vi.fn(),
  updateBackendVault: vi.fn(),
  archiveBackendVault: vi.fn()
}));

const user: AuthenticatedUser = {
  id: "11111111-1111-4111-8111-111111111111",
  username: "samuel",
  email: "samuel@nexo.local",
  name: "Samuel",
  birthDate: null,
  role: "OWNER",
  createdAt: "2026-08-25T10:00:00Z"
};

const team = {
  id: "22222222-2222-4222-8222-222222222222",
  name: "Platform",
  createdBy: user.id,
  defaultProfile: "RESEARCHER",
  tokenBudgetLimit: 100000,
  teamRole: "ADMIN",
  assignedProfile: "OPERATOR",
  manageable: true,
  createdAt: "2026-08-25T10:00:00Z",
  updatedAt: "2026-08-25T10:00:00Z"
};

const renderPage = (): ReturnType<typeof render> => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={darkTheme}><TeamsPage user={user} /></ThemeProvider>
    </QueryClientProvider>
  );
};

describe("TeamsPage", () => {
  it("shows the user's Team, members and Team-owned Vaults", async () => {
    listTeams.mockResolvedValue([team]);
    listTeamMembers.mockResolvedValue([{
      userId: user.id,
      username: user.username,
      name: user.name,
      email: user.email,
      teamRole: "ADMIN",
      assignedProfile: "OPERATOR",
      joinedAt: "2026-08-25T10:00:00Z"
    }]);
    listTeamCandidates.mockResolvedValue([]);
    listBackendVaults.mockResolvedValue([{
      id: "33333333-3333-4333-8333-333333333333",
      name: "Engineering handbook",
      description: "Shared architecture and delivery knowledge.",
      scope: "PERSONAL",
      workspaceId: null,
      ownerId: team.id,
      ownerType: "TEAM",
      ownerName: team.name,
      manageable: true,
      writable: false,
      createdAt: "2026-08-25T10:00:00Z",
      updatedAt: "2026-08-25T10:00:00Z"
    }]);

    renderPage();

    expect(await screen.findByText("Engineering handbook")).toBeVisible();
    expect(await screen.findByText("Samuel")).toBeVisible();
    expect(screen.getByText("Team administration")).toBeVisible();
    expect(screen.getByText("100,000")).toBeVisible();
  });
});
