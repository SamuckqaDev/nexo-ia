import { createHash, randomUUID } from "node:crypto";
import { execFile } from "node:child_process";
import { isUtf8 } from "node:buffer";
import {
  lstat,
  readdir,
  readFile,
  rename,
  realpath,
  stat,
  unlink,
  writeFile
} from "node:fs/promises";
import { isAbsolute, relative, resolve, sep } from "node:path";
import { promisify } from "node:util";
import type { LocalWorkspaceBinding } from "../protocol/types.js";
import type { SecureRuntimeStore } from "./secureStore.js";

const runFile = promisify(execFile);
const MAX_FILE_BYTES = 1_048_576;
const MAX_SEARCH_MATCHES = 100;
const MAX_TREE_ENTRIES = 500;
const MAX_FINGERPRINT_ENTRIES = 20_000;
const ignoredDirectories = new Set([
  ".git", ".gradle", ".idea", ".next", ".nexo-runtime", ".turbo", ".vite",
  "build", "coverage", "dist", "node_modules", "target"
]);
const sensitiveNames = /(^|\/)(\.env($|\.)|id_rsa|id_ed25519|.*\.(pem|key|p12|pfx|jks|keystore))$/i;

type ToolPayload = {
  localBindingId?: unknown;
  input?: unknown;
};

type ToolInput = Record<string, unknown>;

export class WorkspaceTools {
  constructor(private readonly store: SecureRuntimeStore) {}

  async execute(method: string, rawPayload: unknown): Promise<unknown> {
    const payload = this.object(rawPayload) as ToolPayload;
    if (typeof payload.localBindingId !== "string") {
      throw new RuntimeToolError("WORKSPACE_NOT_BOUND", "No local workspace binding was provided");
    }
    const workspace = this.store.workspace(payload.localBindingId);
    if (!workspace) {
      throw new RuntimeToolError("WORKSPACE_NOT_BOUND", "The local workspace binding no longer exists");
    }
    const input = this.object(payload.input);
    if (method === "workspace.listFiles") return this.listFiles(workspace, input);
    if (method === "workspace.readFile") return this.readTextFile(workspace, input);
    if (method === "workspace.readFileRaw") return this.readRawTextFile(workspace, input);
    if (method === "workspace.writeFile") return this.writeTextFile(workspace, input);
    if (method === "workspace.deleteFile") return this.deleteTextFile(workspace, input);
    if (method === "workspace.search") return this.search(workspace, input);
    if (method === "workspace.inspect") return this.inspect(workspace);
    if (method === "git.status") return this.gitStatus(workspace);
    if (method === "git.diff") return this.gitDiff(workspace, input);
    throw new RuntimeToolError("TOOL_UNAVAILABLE", "The requested local tool is not available");
  }

  async inspectBinding(rootPath: string): Promise<Pick<
    LocalWorkspaceBinding,
    "structureFingerprint" | "gitHead" | "gitBranch"
  >> {
    const fingerprint = await this.fingerprint(rootPath);
    const git = await this.gitSummary(rootPath);
    return { structureFingerprint: fingerprint, gitHead: git.head, gitBranch: git.branch };
  }

  private async listFiles(workspace: LocalWorkspaceBinding, input: ToolInput): Promise<unknown> {
    const rawPath = typeof input.path === "string" ? input.path : "";
    const directory = await this.safeExistingPath(workspace.rootPath, rawPath, false);
    if (!(await stat(directory)).isDirectory()) {
      throw new RuntimeToolError("PATH_NOT_FOUND", "The requested workspace directory does not exist");
    }
    const requestedLimit = typeof input.limit === "number" ? input.limit : 100;
    const limit = Math.max(1, Math.min(requestedLimit, MAX_TREE_ENTRIES));
    const names = (await readdir(directory)).sort((left: string, right: string): number => left.localeCompare(right));
    const entries: unknown[] = [];
    const omissions: Array<{ name: string; reason: string }> = [];
    for (const name of names.slice(0, limit)) {
      if (ignoredDirectories.has(name)) {
        omissions.push({ name, reason: "ignored" });
        continue;
      }
      const fullPath = resolve(directory, name);
      const info = await lstat(fullPath);
      if (info.isSymbolicLink()) {
        omissions.push({ name, reason: "symbolic-link" });
        continue;
      }
      const relativePath = relative(workspace.rootPath, fullPath).split(sep).join("/");
      entries.push({
        path: relativePath,
        name,
        type: info.isDirectory() ? "DIRECTORY" : "FILE",
        sizeBytes: info.isFile() ? info.size : null,
        modifiedAt: info.mtime.toISOString()
      });
    }
    return {
      status: "COMPLETED",
      path: rawPath,
      entries,
      omissions,
      truncated: names.length > limit,
      nextCursor: null,
      message: "Workspace directory listed successfully."
    };
  }

  private async readTextFile(workspace: LocalWorkspaceBinding, input: ToolInput): Promise<unknown> {
    const path = this.requiredPath(input);
    this.assertReadableName(path);
    const fullPath = await this.safeExistingPath(workspace.rootPath, path, true);
    const info = await stat(fullPath);
    if (!info.isFile() || info.size > MAX_FILE_BYTES) {
      throw new RuntimeToolError("SENSITIVE_FILE_DENIED", "The requested file cannot be read safely");
    }
    const buffer = await readFile(fullPath);
    if (buffer.includes(0) || !isUtf8(buffer)) {
      throw new RuntimeToolError("SENSITIVE_FILE_DENIED", "Binary workspace files are not exposed to the model");
    }
    const content = buffer.toString("utf8");
    const lines = content.split(/\r?\n/);
    const startLine = this.boundedInteger(input.startLine, 1, Math.max(1, lines.length), 1);
    const endLine = this.boundedInteger(input.endLine, startLine, lines.length, Math.min(lines.length, startLine + 399));
    const numberedContent = lines.slice(startLine - 1, endLine)
      .map((line: string, index: number): string => `${startLine + index}: ${line}`)
      .join("\n");
    return {
      status: "COMPLETED",
      path,
      numberedContent,
      startLine,
      endLine,
      totalLines: lines.length,
      sha256: createHash("sha256").update(buffer).digest("hex"),
      truncated: endLine < lines.length,
      message: "Workspace file read successfully."
    };
  }

  private async readRawTextFile(workspace: LocalWorkspaceBinding, input: ToolInput): Promise<unknown> {
    const path = this.requiredPath(input);
    this.assertReadableName(path);
    const fullPath = await this.safeExistingPath(workspace.rootPath, path, true);
    const info = await stat(fullPath);
    if (!info.isFile() || info.size > MAX_FILE_BYTES) {
      throw new RuntimeToolError("SENSITIVE_FILE_DENIED", "The requested file cannot be read safely");
    }
    const buffer = await readFile(fullPath);
    if (buffer.includes(0) || !isUtf8(buffer)) {
      throw new RuntimeToolError("SENSITIVE_FILE_DENIED", "Binary workspace files are not exposed to the server");
    }
    return {
      status: "COMPLETED",
      path,
      content: buffer.toString("utf8"),
      sha256: createHash("sha256").update(buffer).digest("hex"),
      message: "Workspace file loaded for a server-generated change preview."
    };
  }

  private async writeTextFile(workspace: LocalWorkspaceBinding, input: ToolInput): Promise<unknown> {
    const path = this.requiredPath(input);
    this.assertReadableName(path);
    const content = typeof input.content === "string" ? input.content : null;
    if (content === null) {
      throw new RuntimeToolError("COMMAND_DENIED", "Workspace writes require text content");
    }
    const bytes = Buffer.from(content, "utf8");
    if (bytes.byteLength > MAX_FILE_BYTES || bytes.includes(0)) {
      throw new RuntimeToolError("COMMAND_DENIED", "Workspace writes are limited to bounded UTF-8 text");
    }

    const { target, created } = await this.safeWritablePath(workspace.rootPath, path);
    if (!created) {
      const expectedSha256 = typeof input.expectedSha256 === "string"
        ? input.expectedSha256.toLowerCase()
        : "";
      const current = await readFile(target);
      const currentSha256 = createHash("sha256").update(current).digest("hex");
      if (!expectedSha256 || expectedSha256 !== currentSha256) {
        throw new RuntimeToolError(
          "WRITE_CONFLICT",
          "Existing files require the current SHA-256 from workspace_read_file"
        );
      }
    }

    const temporary = `${target}.nexo-${randomUUID()}.tmp`;
    try {
      await writeFile(temporary, bytes, { flag: "wx" });
      await rename(temporary, target);
    } finally {
      await unlink(temporary).catch((): undefined => undefined);
    }
    return {
      status: "COMPLETED",
      path,
      created,
      sizeBytes: bytes.byteLength,
      sha256: createHash("sha256").update(bytes).digest("hex"),
      message: created ? "Workspace file created successfully." : "Workspace file replaced successfully."
    };
  }

  private async deleteTextFile(workspace: LocalWorkspaceBinding, input: ToolInput): Promise<unknown> {
    const path = this.requiredPath(input);
    this.assertReadableName(path);
    const target = await this.safeExistingPath(workspace.rootPath, path, true);
    const info = await stat(target);
    if (!info.isFile() || info.size > MAX_FILE_BYTES) {
      throw new RuntimeToolError("COMMAND_DENIED", "Only one bounded regular file can be deleted");
    }
    const current = await readFile(target);
    const currentSha256 = createHash("sha256").update(current).digest("hex");
    const expectedSha256 = typeof input.expectedSha256 === "string"
      ? input.expectedSha256.toLowerCase()
      : "";
    if (!expectedSha256 || expectedSha256 !== currentSha256) {
      throw new RuntimeToolError("WRITE_CONFLICT", "The file changed after the approved preview");
    }
    await unlink(target);
    return {
      status: "COMPLETED",
      path,
      previousSha256: currentSha256,
      message: "Workspace file deleted after server approval."
    };
  }

  private async search(workspace: LocalWorkspaceBinding, input: ToolInput): Promise<unknown> {
    const query = typeof input.query === "string" ? input.query : "";
    if (!query || query.length > 1_000) {
      throw new RuntimeToolError("COMMAND_DENIED", "Workspace search requires a bounded literal query");
    }
    const basePath = typeof input.path === "string" ? input.path : "";
    const root = await this.safeExistingPath(workspace.rootPath, basePath, false);
    const requestedLimit = typeof input.limit === "number" ? input.limit : 20;
    const limit = Math.max(1, Math.min(requestedLimit, MAX_SEARCH_MATCHES));
    const matches: Array<{ path: string; lineNumber: number; excerpt: string }> = [];
    await this.walkFiles(root, workspace.rootPath, async (path: string): Promise<boolean> => {
      const relativePath = relative(workspace.rootPath, path).split(sep).join("/");
      if (sensitiveNames.test(relativePath)) return true;
      const info = await stat(path);
      if (info.size > MAX_FILE_BYTES) return true;
      const buffer = await readFile(path);
      if (buffer.includes(0)) return true;
      buffer.toString("utf8").split(/\r?\n/).forEach((line: string, index: number): void => {
        if (matches.length <= limit && line.includes(query)) {
          matches.push({ path: relativePath, lineNumber: index + 1, excerpt: line.slice(0, 500) });
        }
      });
      return matches.length > limit;
    });
    const truncated = matches.length > limit;
    const bounded = matches.slice(0, limit);
    return {
      status: bounded.length ? "FOUND" : "NO_RESULTS",
      matches: bounded,
      truncated,
      message: bounded.length ? "Workspace matches found." : "No matching workspace text was found."
    };
  }

  private async inspect(workspace: LocalWorkspaceBinding): Promise<unknown> {
    const names = new Set(await readdir(workspace.rootPath));
    const stack = [
      names.has("pom.xml") ? "Maven" : null,
      names.has("build.gradle") || names.has("build.gradle.kts") ? "Gradle" : null,
      names.has("package.json") ? "Node.js" : null,
      names.has("Cargo.toml") ? "Rust" : null,
      names.has("pyproject.toml") || names.has("requirements.txt") ? "Python" : null,
      names.has("go.mod") ? "Go" : null
    ].filter((value: string | null): value is string => value !== null);
    const git = await this.gitSummary(workspace.rootPath);
    return {
      status: "COMPLETED",
      workspaceName: workspace.displayName,
      detectedStack: stack,
      git: git.head ? { branch: git.branch, head: git.head, detached: git.branch === null } : null,
      message: "Local project inspected successfully."
    };
  }

  private async gitStatus(workspace: LocalWorkspaceBinding): Promise<unknown> {
    const git = await this.gitSummary(workspace.rootPath);
    if (!git.head) {
      return { status: "UNAVAILABLE", branch: null, head: null, changedPaths: [], truncated: false,
        message: "This workspace is not a Git repository." };
    }
    const output = await this.git(workspace.rootPath, ["status", "--porcelain=v1", "--untracked-files=all"]);
    const changedPaths = output.split(/\r?\n/).filter(Boolean)
      .map((line: string): string => line.slice(3).replace(/^"|"$/g, ""))
      .slice(0, 500);
    return {
      status: "COMPLETED",
      branch: git.branch,
      head: git.head,
      changedPaths,
      truncated: changedPaths.length >= 500,
      message: changedPaths.length ? "Git working tree has changes." : "Git working tree is clean."
    };
  }

  private async gitDiff(workspace: LocalWorkspaceBinding, input: ToolInput): Promise<unknown> {
    const path = this.requiredPath(input);
    this.assertReadableName(path);
    await this.safeExistingPath(workspace.rootPath, path, true);
    const diff = await this.git(workspace.rootPath, ["diff", "--no-ext-diff", "--", path]);
    const bounded = diff.slice(0, 64_000);
    return {
      status: bounded ? "FOUND" : "NO_RESULTS",
      path,
      diff: bounded,
      truncated: diff.length > bounded.length,
      message: bounded ? "Git diff found." : "No unstaged diff was found for this file."
    };
  }

  private async gitSummary(rootPath: string): Promise<{ branch: string | null; head: string | null }> {
    return Promise.all([
      this.git(rootPath, ["rev-parse", "--abbrev-ref", "HEAD"]),
      this.git(rootPath, ["rev-parse", "HEAD"])
    ]).then(([branch, head]): { branch: string | null; head: string | null } => ({
      branch: branch.trim() === "HEAD" ? null : branch.trim(),
      head: head.trim() || null
    })).catch((): { branch: null; head: null } => ({ branch: null, head: null }));
  }

  private git(rootPath: string, args: string[]): Promise<string> {
    return runFile("git", ["-C", rootPath, ...args], {
      timeout: 30_000,
      maxBuffer: 1_048_576,
      encoding: "utf8",
      windowsHide: true
    }).then((result): string => result.stdout);
  }

  private async fingerprint(rootPath: string): Promise<string> {
    const entries: string[] = [];
    await this.walkFiles(rootPath, rootPath, async (path: string): Promise<boolean> => {
      const info = await stat(path);
      entries.push(`${relative(rootPath, path).split(sep).join("/")}\0${info.size}\0${info.mtimeMs}`);
      return entries.length >= MAX_FINGERPRINT_ENTRIES;
    });
    entries.sort();
    return createHash("sha256").update(entries.join("\n")).digest("hex");
  }

  private async walkFiles(
    directory: string,
    rootPath: string,
    visitor: (path: string) => Promise<boolean>
  ): Promise<boolean> {
    const entries = await readdir(directory, { withFileTypes: true });
    for (const entry of entries) {
      if (entry.isSymbolicLink() || (entry.isDirectory() && ignoredDirectories.has(entry.name))) continue;
      const path = resolve(directory, entry.name);
      if (!this.contained(rootPath, path)) continue;
      if (entry.isDirectory()) {
        if (await this.walkFiles(path, rootPath, visitor)) return true;
      } else if (entry.isFile() && await visitor(path)) {
        return true;
      }
    }
    return false;
  }

  private async safeExistingPath(rootPath: string, requested: string, requireFile: boolean): Promise<string> {
    if (isAbsolute(requested) || requested.split(/[\\/]+/).includes("..")) {
      throw new RuntimeToolError("PATH_OUTSIDE_WORKSPACE", "Only workspace-relative paths are allowed");
    }
    const realRoot = await realpath(rootPath);
    const candidate = resolve(realRoot, requested || ".");
    if (!this.contained(realRoot, candidate)) {
      throw new RuntimeToolError("PATH_OUTSIDE_WORKSPACE", "The requested path leaves the workspace");
    }
    const resolvedPath = await realpath(candidate).catch(() => {
      throw new RuntimeToolError("PATH_NOT_FOUND", "The requested workspace path does not exist");
    });
    if (!this.contained(realRoot, resolvedPath)) {
      throw new RuntimeToolError("SYMLINK_ESCAPE", "Symbolic links cannot leave the workspace");
    }
    const info = await lstat(resolvedPath);
    if (info.isSymbolicLink() || (requireFile && !info.isFile())) {
      throw new RuntimeToolError("SENSITIVE_FILE_DENIED", "The requested workspace entry cannot be read safely");
    }
    return resolvedPath;
  }

  private async safeWritablePath(
    rootPath: string,
    requested: string
  ): Promise<{ target: string; created: boolean }> {
    if (isAbsolute(requested) || requested.split(/[\\/]+/).includes("..")) {
      throw new RuntimeToolError("PATH_OUTSIDE_WORKSPACE", "Only workspace-relative paths are allowed");
    }
    const realRoot = await realpath(rootPath);
    const target = resolve(realRoot, requested);
    if (target === realRoot || !this.contained(realRoot, target)) {
      throw new RuntimeToolError("PATH_OUTSIDE_WORKSPACE", "The requested path leaves the workspace");
    }
    const parent = await realpath(resolve(target, "..")).catch(() => {
      throw new RuntimeToolError("PATH_NOT_FOUND", "The target directory does not exist");
    });
    if (!this.contained(realRoot, parent)) {
      throw new RuntimeToolError("SYMLINK_ESCAPE", "Symbolic links cannot leave the workspace");
    }
    const info = await lstat(target).catch(() => null);
    if (info?.isSymbolicLink() || (info && !info.isFile())) {
      throw new RuntimeToolError("SENSITIVE_FILE_DENIED", "The target cannot be written safely");
    }
    if (info) {
      const resolvedTarget = await realpath(target);
      if (!this.contained(realRoot, resolvedTarget)) {
        throw new RuntimeToolError("SYMLINK_ESCAPE", "Symbolic links cannot leave the workspace");
      }
    }
    return { target, created: info === null };
  }

  private contained(rootPath: string, candidate: string): boolean {
    return candidate === rootPath || candidate.startsWith(`${rootPath}${sep}`);
  }

  private requiredPath(input: ToolInput): string {
    if (typeof input.path !== "string" || !input.path.trim()) {
      throw new RuntimeToolError("PATH_NOT_FOUND", "A workspace-relative file path is required");
    }
    return input.path.trim().split("\\").join("/");
  }

  private assertReadableName(path: string): void {
    if (sensitiveNames.test(path)) {
      throw new RuntimeToolError("SENSITIVE_FILE_DENIED", "Sensitive workspace files are not exposed to the model");
    }
  }

  private boundedInteger(value: unknown, minimum: number, maximum: number, fallback: number): number {
    if (typeof value !== "number" || !Number.isInteger(value)) return fallback;
    return Math.max(minimum, Math.min(value, maximum));
  }

  private object(value: unknown): ToolInput {
    return value !== null && typeof value === "object" && !Array.isArray(value)
      ? value as ToolInput
      : {};
  }
}

export class RuntimeToolError extends Error {
  constructor(readonly code: string, message: string) {
    super(message);
  }
}
