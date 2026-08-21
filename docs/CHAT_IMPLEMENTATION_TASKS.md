# Nexo IA Chat — Implementation Tasks

## Objective

Finish the authenticated local-chat journey on `feat/project-scaffold`: open a conversation by typing, select a model from the saved provider, send a message, watch a progressive response, reopen history, and rename the conversation.

## Problems and required behavior

1. **New conversation**
   - New conversation clears the active selection without requiring a title dialog.
   - The composer remains writable with no `conversationId`.
   - The first submitted message creates the conversation automatically and derives its title from that message.
   - The New conversation action must work both with existing history and when the account has no conversations.
   - Starting a new chat must clear stale stream state, draft text, pending model selection, and previous messages without a full-page reload.

2. **Provider and model selection**
   - The authenticated provider configuration saved by the user is the only source of truth.
   - Never use the global YAML Ollama endpoint for chat model selection or execution.
   - Discover models through `/providers/configurations/{providerId}/models`.
   - Group models by saved provider; preserve unavailable, empty, and unsupported states; never fabricate models.
   - Persist provider configuration ID and selected model on the conversation.

3. **Message and stream UX**
   - Display the user message immediately after backend reservation succeeds.
   - Preserve started, thinking, token, usage, completed, cancelled, and error SSE events.
   - Render provider chunks through a small frontend queue/typewriter presentation while keeping final persisted content authoritative.
   - The model selector must be disabled while a request is starting, thinking, streaming, or cancelling.
   - The chat header must show a clear “model locked while Nexo is responding” state beside the model selector, with an accessible explanation.
   - Use the branded Nexo loading component or a dedicated branded thinking indicator for model reasoning; do not use a generic spinner.

4. **Visual system**
   - Style every application scrollbar (track, thumb, hover, and reduced-motion behavior) with the Nexo theme palette.
   - Keep scrollbar styling readable in both light and dark themes and avoid changing layout width when scrollbars appear.

5. **Conversation title**
   - Edit the title from the conversation item/bubble.
   - Save through the authenticated rename endpoint and update sidebar/header without reload.

6. **Identity context**
   - Resolve the authenticated username on the backend from the session user ID.
   - Include it in the system context sent to the selected provider; never trust a browser-supplied username.

7. **Persistence and privacy**
   - User and assistant messages are authoritative Nexo PostgreSQL records.
   - Ollama is only the inference engine, not the conversation history store.
   - Preserve ownership checks, usage metadata, and safe logs.

## Required verification

- Frontend: `npm run build` and `npm test -- --run`.
- Backend: `./mvnw verify` using Java 25.
- `git diff --check`.
- Manual smoke path: login -> saved provider -> model catalog -> New conversation -> type -> send -> progressive response -> reload -> history -> rename -> cancel/error checks.
- Add or update focused tests for new-conversation creation, model selection, title rename, stream rendering, ownership, and username context.
- Add frontend coverage for new-chat reset, locked model selection during streaming, branded thinking/loading state, and themed scrollbar styles where the project test setup supports it.

## Explicit non-goals

Do not implement Agent Runtime, Spring AI agent orchestration, GUI agent creation, MCP execution, Cowork execution, filesystem control, RAG ingestion, remote Secret Store credentials, or image generation in this task.

## Git rules

- Work only on `feat/project-scaffold`; never modify `main`.
- Use small Conventional Commits, such as `fix(chat): create conversations from first message`.
- Never commit `.env`, tokens, cookies, generated secrets, or unrelated user changes.
