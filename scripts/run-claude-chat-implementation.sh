#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

BRANCH="$(git branch --show-current)"
if [[ "$BRANCH" != "feat/project-scaffold" ]]; then
  echo "[NEXO-CLAUDE] Refusing to run on branch: $BRANCH" >&2
  echo "[NEXO-CLAUDE] Checkout feat/project-scaffold first." >&2
  exit 1
fi

LOG_DIR="$PROJECT_ROOT/.claude-logs"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/chat-implementation-$(date +%Y%m%d-%H%M%S).jsonl"

echo "[NEXO-CLAUDE] Branch: $BRANCH"
echo "[NEXO-CLAUDE] Raw live log: $LOG_FILE"
echo "[NEXO-CLAUDE] Mandatory skill: skills/build-nexo/SKILL.md"

claude -p "$(cat <<'PROMPT'
You are implementing Nexo IA's next scoped increment.

Before inspecting or editing code, read these files completely:
- skills/build-nexo/SKILL.md (mandatory project skill; follow its code, frontend, testing, security, and Git rules)
- docs/CHAT_IMPLEMENTATION_TASKS.md
- docs/CLAUDE_CODE_IMPLEMENTATION_PLAN.md
- docs/IMPLEMENTATION_STATUS.md

Work only on the current feat/project-scaffold branch. Never touch main. Start by reporting the
branch, dirty files, and a concise implementation plan. Then execute the task list incrementally.
Show progress as you go: files inspected, hypothesis, edits, commands, test results, and remaining
risks. Preserve unrelated existing changes and never overwrite .env, tokens, cookies, or secrets.
Use the saved provider configuration as the source of truth; do not reintroduce a global YAML
provider fallback into chat/model selection. Keep controllers thin, services authoritative, DTOs
as separate top-level files, global personalized exceptions, typed React, BaseResponse, cookie
authentication, the existing SSE lifecycle, and the Nexo visual system.

Run the required backend/frontend checks. If a check is blocked by the environment, report the exact
command and reason, then continue with safe checks. Do not implement the explicit non-goals. Review
the diff for unrelated changes before committing.

When the task is genuinely complete and tests support it, create small Conventional Commits on
feat/project-scaffold. Never commit secrets or unrelated files. End with a report containing
changed files, commits, tests, unresolved blockers, and exact manual smoke-test steps.
PROMPT
)" \
  --permission-mode acceptEdits \
  --allowed-tools Read Edit Write Glob Grep Bash \
  --output-format stream-json \
  --include-partial-messages \
  --verbose \
  --debug-file "$LOG_DIR/chat-implementation-debug.log" \
  | tee "$LOG_FILE"

echo "[NEXO-CLAUDE] Claude finished. Review: git status --short --branch"
echo "[NEXO-CLAUDE] Log saved at: $LOG_FILE"
