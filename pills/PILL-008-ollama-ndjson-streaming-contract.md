# PILL-008 — The Ollama chat streaming and token accounting contract

- **Status:** accepted
- **Discovered:** 2026-08-18
- **Last reviewed:** 2026-08-18
- **Area:** AI | Spring | Java

## Question

What exactly does Ollama return from a streamed `/api/chat` request, and where do the token counts
that Nexo IA must attribute to a user come from?

## Finding

Ollama streams newline-delimited JSON with one line per content delta, and reports token usage only
on the final line, where `done` is true: `prompt_eval_count` is the input and `eval_count` is the
output.

## Evidence

Observed directly against the local installation on 2026-08-18 with `qwen3:8b`:

```json
{"model":"qwen3:8b","message":{"role":"assistant","content":"Hello"},"done":false}
{"model":"qwen3:8b","message":{"role":"assistant","content":"."},"done":false}
{"model":"qwen3:8b","message":{"role":"assistant","content":""},"done":true,"done_reason":"stop",
 "total_duration":248440083,"load_duration":137248667,"prompt_eval_count":20,
 "prompt_eval_duration":35658000,"eval_count":3,"eval_duration":71001000}
```

- [Ollama API reference](https://github.com/ollama/ollama/blob/main/docs/api.md)
- `OllamaChatCompletionClientTest` reproduces the format with a deterministic fake.
- `OllamaChatCompletionSmokeTest` verifies it against a real installation
  (`./mvnw test -Dexcluded.test.groups= -Dgroups=ollama`).

## Explanation

The stream is NDJSON, not Server-Sent Events: plain JSON objects separated by newlines, with no
`data:` prefix and no event names. A reader consumes it line by line.

Three details drive the implementation. The final line still carries a `message` object, but its
`content` is an empty string, so a parser must tolerate an empty delta rather than treating it as a
protocol error. Token counts appear only on that final line, which means a cancelled generation has
no provider-reported usage at all — Nexo IA must either estimate and label the estimate, or record
none. And `done_reason` distinguishes a natural stop from other terminations, so it belongs in the
persisted record.

Closing the response stream is what stops generation. Ollama notices the disconnected reader and
aborts the run, so cancellation needs no separate API call; the client only has to stop reading.

Reasoning models add a separate `thinking` field alongside `content`. Nexo IA ignores it until
reasoning display has a product contract, so it is never concatenated into the answer.

One consequence is easy to miss: `RestClient.exchange` hands over the raw response without applying
default status handling. A `500` from the provider would otherwise be read as a successful empty
answer, so the status must be checked explicitly inside the exchange function.

## Impact on Nexo IA

- `OllamaChatCompletionClient` reads NDJSON line by line and forwards each non-empty delta.
- `prompt_eval_count` and `eval_count` are stored with `TokenSource.PROVIDER`; an estimate would be
  stored as `TokenSource.ESTIMATE` and labelled in the interface.
- Cancellation stops the read loop and persists the partial answer with no token counts.
- A provider error status raises `ProviderStreamException` and never becomes an empty completion.
- Nexo IA reads this protocol directly instead of using a higher-level model abstraction; see
  [D-021](../docs/DECISIONS.md).

## Limits and review triggers

Observed on the installation available in August 2026. Review this pill when Ollama changes its API
contract, when Nexo IA starts displaying reasoning output, when tool calling is introduced — tool
calls arrive as a separate field on the message — or when a second provider protocol makes a shared
usage-normalisation rule necessary.
