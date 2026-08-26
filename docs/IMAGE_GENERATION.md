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
  -> exact installed checkpoint selection
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

The runtime response includes the complete checkpoint catalog reported by ComfyUI and the configured
default. Image mode shows that catalog in a compact selector and sends the exact selected filename
with the prompt. The backend validates the choice against a fresh runtime snapshot before queuing it,
so a stale or fabricated checkpoint name cannot enter the workflow.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/media/images/runtime` | Inspect availability, default checkpoint, and installed models |
| `GET` | `/api/v1/media/images/conversations/{conversationId}` | List the owner's conversation jobs |
| `POST` | `/api/v1/media/images/conversations/{conversationId}` | Queue a prompt and checkpoint (HTTP 202) |
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

Additional checkpoints can be copied into:

```text
.nexo-runtime/comfyui/models/checkpoints/
```

ComfyUI reports every installed checkpoint through its model route; refresh Nexo after adding one
and it appears in the Image model selector. A specialized anatomy or medical-illustration checkpoint
can be selected for university material, while the bundled SD 1.5 checkpoint remains the portable
default. The chosen checkpoint controls visual capability and is recorded on every job; selection
does not guarantee that a checkpoint was trained for a particular subject.

The Compose stack runs a one-shot `media-init` service before the backend. It creates the generated
image directory and restores ownership to the unprivileged Nexo process, including on existing named
volumes. This prevents a successfully generated ComfyUI output from being mislabeled as a runtime
failure merely because the artifact directory was created by root.

Use `NEXO_SKIP_IMAGE_RUNTIME=1 ./scripts/setup-macos.sh` (or Linux) or
`.\scripts\setup-windows.ps1 -SkipImageRuntime` when the machine should run Nexo without image
generation.

## Current boundaries

- Prompt length is bounded to 4,000 characters and generated downloads to 25 MB.
- Artifacts are stored under the configured local media directory; PostgreSQL stores job metadata.
- The first workflow is 512×512 text-to-image with a user-selected, locally installed checkpoint.
- Job cancellation, source-image editing, WebSocket percentage events, restart resumption, multiple
  concurrent GPU queues, and remote `ImageModel` providers remain later increments.
