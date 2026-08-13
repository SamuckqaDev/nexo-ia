# Initial scope

## In scope

- Conversation interface.
- Integration with local models.
- Response streaming.
- History and context management.
- RAG over authorized documents.
- Controlled local tools.
- Permission Engine.
- MCP client and catalog.
- Agent loop, memory, and auditing.
- Scheduled tasks with isolated runs and pre-authorized permission scopes.
- Image generation through controlled local or explicitly enabled remote providers.
- Tests and evaluations for responses and actions.

## Outside the first development cycle

- Video generation and advanced image editing.
- Speech recognition and synthesis.
- Unrestricted operating-system automation.
- Mobile applications.
- Distributed architecture and large-scale infrastructure.
- Multi-user support or public deployment.

These capabilities may be studied later. Keeping them outside the first cycle protects the learning
goal and reduces the number of simultaneous problems.

## First usable product

A local application that can communicate with a model through Ollama, save conversations, stream
responses, and clearly report which model was used.
