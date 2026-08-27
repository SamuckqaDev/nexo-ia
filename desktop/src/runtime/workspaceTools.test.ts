import { mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { LocalWorkspaceBinding } from "../protocol/types.js";
import type { SecureRuntimeStore } from "./secureStore.js";
import { WorkspaceTools } from "./workspaceTools.js";

describe("WorkspaceTools", (): void => {
  let rootPath: string;
  let tools: WorkspaceTools;
  let workspace: LocalWorkspaceBinding;

  beforeEach(async (): Promise<void> => {
    rootPath = await mkdtemp(join(tmpdir(), "nexo-workspace-tools-"));
    workspace = {
      localBindingId: "local-test",
      workspaceId: "workspace-test",
      displayName: "Test project",
      rootPath,
      structureFingerprint: "fingerprint",
      gitHead: null,
      gitBranch: null
    };
    const store = {
      workspace: (localBindingId: string): LocalWorkspaceBinding | null =>
        localBindingId === workspace.localBindingId ? workspace : null
    } as SecureRuntimeStore;
    tools = new WorkspaceTools(store);
  });

  afterEach(async (): Promise<void> => {
    await rm(rootPath, { recursive: true, force: true });
  });

  it("lists bounded workspace entries without ignored directories", async (): Promise<void> => {
    await writeFile(join(rootPath, "README.md"), "Nexo\n", "utf8");
    await mkdir(join(rootPath, "src"));
    await mkdir(join(rootPath, "node_modules"));

    const result = await tools.execute("workspace.listFiles", {
      localBindingId: workspace.localBindingId,
      input: { path: "", limit: 20 }
    }) as {
      entries: Array<{ name: string }>;
      omissions: Array<{ name: string; reason: string }>;
    };

    expect(result.entries.map((entry): string => entry.name)).toEqual(["README.md", "src"]);
    expect(result.omissions).toContainEqual({ name: "node_modules", reason: "ignored" });
  });

  it("reads and numbers a bounded text range", async (): Promise<void> => {
    await writeFile(join(rootPath, "README.md"), "one\ntwo\nthree", "utf8");

    const result = await tools.execute("workspace.readFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "README.md", startLine: 2, endLine: 3 }
    }) as { numberedContent: string; startLine: number; endLine: number };

    expect(result.numberedContent).toBe("2: two\n3: three");
    expect(result.startLine).toBe(2);
    expect(result.endLine).toBe(3);
  });

  it("creates a bounded text file and returns verifiable evidence", async (): Promise<void> => {
    const result = await tools.execute("workspace.writeFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "hello.html", content: "<h1>Hello</h1>\n" }
    }) as { status: string; path: string; created: boolean; sha256: string };

    expect(result).toMatchObject({ status: "COMPLETED", path: "hello.html", created: true });
    expect(result.sha256).toMatch(/^[a-f0-9]{64}$/);
    await expect(readFile(join(rootPath, "hello.html"), "utf8")).resolves.toBe("<h1>Hello</h1>\n");
  });

  it("requires the current hash before replacing an existing file", async (): Promise<void> => {
    await writeFile(join(rootPath, "hello.html"), "old", "utf8");

    await expect(tools.execute("workspace.writeFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "hello.html", content: "new" }
    })).rejects.toMatchObject({ code: "WRITE_CONFLICT" });

    const read = await tools.execute("workspace.readFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "hello.html" }
    }) as { sha256: string };
    await expect(tools.execute("workspace.writeFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "hello.html", content: "new", expectedSha256: read.sha256 }
    })).resolves.toMatchObject({ status: "COMPLETED", created: false });
    await expect(readFile(join(rootPath, "hello.html"), "utf8")).resolves.toBe("new");
  });

  it("returns exact raw text to the server for preview generation", async (): Promise<void> => {
    await writeFile(join(rootPath, "README.md"), "old value\n", "utf8");

    const result = await tools.execute("workspace.readFileRaw", {
      localBindingId: workspace.localBindingId,
      input: { path: "README.md" }
    }) as { status: string; path: string; content: string; sha256: string };

    expect(result).toMatchObject({
      status: "COMPLETED",
      path: "README.md",
      content: "old value\n"
    });
    expect(result.sha256).toMatch(/^[a-f0-9]{64}$/);
  });

  it("deletes only the exact file version approved by the server", async (): Promise<void> => {
    await writeFile(join(rootPath, "obsolete.txt"), "remove me", "utf8");
    const current = await tools.execute("workspace.readFileRaw", {
      localBindingId: workspace.localBindingId,
      input: { path: "obsolete.txt" }
    }) as { sha256: string };

    await expect(tools.execute("workspace.deleteFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "obsolete.txt", expectedSha256: "0".repeat(64) }
    })).rejects.toMatchObject({ code: "WRITE_CONFLICT" });
    await expect(readFile(join(rootPath, "obsolete.txt"), "utf8")).resolves.toBe("remove me");

    await expect(tools.execute("workspace.deleteFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "obsolete.txt", expectedSha256: current.sha256 }
    })).resolves.toMatchObject({ status: "COMPLETED", path: "obsolete.txt" });
    await expect(readFile(join(rootPath, "obsolete.txt"), "utf8"))
      .rejects.toMatchObject({ code: "ENOENT" });
  });

  it("rejects traversal outside the selected workspace", async (): Promise<void> => {
    await expect(tools.execute("workspace.readFile", {
      localBindingId: workspace.localBindingId,
      input: { path: "../secret.txt" }
    })).rejects.toMatchObject({ code: "PATH_OUTSIDE_WORKSPACE" });
  });

  it("denies credentials even when they are inside the workspace", async (): Promise<void> => {
    await writeFile(join(rootPath, ".env"), "TOKEN=secret\n", "utf8");

    await expect(tools.execute("workspace.readFile", {
      localBindingId: workspace.localBindingId,
      input: { path: ".env" }
    })).rejects.toMatchObject({ code: "SENSITIVE_FILE_DENIED" });
  });
});
