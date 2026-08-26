# Local image generation

## Implemented runtime

Nexo IA generates images locally through an explicit ComfyUI adapter. It does not send prompts or
artifacts to a remote image provider. The adapter follows ComfyUI's documented server routes:
`POST /prompt`, `GET /history/{prompt_id}`, `GET /view`, and checkpoint discovery. See the
[official ComfyUI server routes](https://docs.comfy.org/development/comfyui-server/comms_routes) and
[official manual installation guide](https://docs.comfy.org/installation/manual_install).

```text
authenticated user
  -> conversation-owned image request
  -> persisted QUEUED job
  -> local ComfyUI workflow
  -> GENERATING with runtime prompt id and checkpoint
  -> bounded local artifact write
  -> COMPLETED or FAILED audit
  -> authenticated Media rail and image content endpoint
```

The frontend polls only while a job is active. ComfyUI's basic history route does not expose a stable
portable percentage for this workflow, so Nexo shows an indeterminate progress bar and real elapsed
time instead of inventing a percentage or ETA. A completed artifact remains attached to its
conversation and is owner-filtered when listed or downloaded.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/media/images/runtime` | Inspect ComfyUI availability and selected checkpoint |
| `GET` | `/api/v1/media/images/conversations/{conversationId}` | List the owner's conversation jobs |
| `POST` | `/api/v1/media/images/conversations/{conversationId}` | Queue a prompt (HTTP 202) |
| `GET` | `/api/v1/media/images/{jobId}/content` | Read an owned completed artifact |

## Development setup

Run one platform bootstrap from the repository root:

```bash
./scripts/setup-macos.sh
./scripts/setup-linux.sh
```

```powershell
.\scripts\setup-windows.ps1
```

The scripts clone the [official ComfyUI repository](https://github.com/comfy-org/ComfyUI), create a
project-local Python environment, install its pinned requirements, download the default SD 1.5
checkpoint, start ComfyUI on port `8188`, pull the default Ollama models, and start Nexo. Runtime data
under `.nexo-runtime/` and generated data under `.nexo-data/` are ignored by Git. The container uses
`NEXO_CONTAINER_COMFYUI_BASE_URL`; a directly started backend uses `NEXO_COMFYUI_BASE_URL`.

Use `NEXO_SKIP_IMAGE_RUNTIME=1 ./scripts/setup-macos.sh` (or Linux) or
`.\scripts\setup-windows.ps1 -SkipImageRuntime` when the machine should run Nexo without image
generation.

## Current boundaries

- Prompt length is bounded to 4,000 characters and generated downloads to 25 MB.
- Artifacts are stored under the configured local media directory; PostgreSQL stores job metadata.
- The first workflow is 512×512 text-to-image with a locally installed checkpoint.
- Job cancellation, source-image editing, WebSocket percentage events, restart resumption, multiple
  concurrent GPU queues, and remote `ImageModel` providers remain later increments.
