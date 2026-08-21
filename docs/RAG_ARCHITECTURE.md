# RAG and retrieval architecture

This document describes how Nexo IA answers from a user's own documents: how a file becomes a
searchable, embedded chunk, how a chat message pulls authorized excerpts back in as grounded context,
and the single authorization rule the whole design depends on. It is the mechanism behind
[Knowledge Vaults](KNOWLEDGE_VAULTS.md); the Vault document describes the product concept, this one
describes the pipeline.

There are two flows — ingestion, which runs once per source, and retrieval, which runs on every
message that names Vaults — and one governance line that separates one user's knowledge from
another's.

## Data model

```text
Knowledge Vault          owner, scope, name
   | 1 — *
Knowledge Source         a registered file; REGISTERED -> READY | FAILED | UNSUPPORTED
   | 1 — *
Knowledge Chunk          text + embedding vector(768); immutable, ordinal-keyed

Conversation Message     assistant reply
   +-- citations[]       vault / source # ordinal, kept with the message
```

A **Vault** owns sources; a **Source** owns ordered **Chunks**; each chunk carries its embedding
vector. Chunks are immutable — re-ingesting a changed source deletes and recreates its chunks rather
than updating them in place. The vector column is fixed at 768 dimensions for the default
`nomic-embed-text` model; changing the embedding model requires a new migration altering that column
plus full re-ingestion.

## Flow 1 — Ingestion

Ingestion turns a file into embedded chunks. It runs synchronously when a source is registered, and a
source walks a visible status: `REGISTERED -> READY`, or stops at `FAILED` (a processing error, with
an error code) or `UNSUPPORTED` (a type without a parser yet — metadata only, never chunked or
embedded).

```text
Upload a file        Normalize          Split into          Embed each chunk        Store
into a Vault    -->  to plain text  --> ordered chunks  -->  Ollama · 768d      -->  chunk + vector
                     (by type)          (immutable)          nomic-embed-text        knowledge_chunk
                                                                                     (pgvector)
```

- **Normalize** extracts plain text according to the file type; an unrecognized type ends at
  `UNSUPPORTED`.
- **Chunk** splits the normalized text into ordered, immutable pieces keyed by ordinal.
- **Embed** calls local Ollama `POST /api/embed` with the default `nomic-embed-text` model, which
  returns one 768-dimension vector per chunk. Embeddings run on the same local provider as chat, with
  the same privacy posture.
- **Store** persists each chunk's text and vector in `knowledge_chunk`; the source is then marked
  `READY` with its chunk count.

## Flow 2 — Retrieval and generation

Retrieval grounds an answer at chat time. It runs on every message that names Vaults (up to eight per
message). Retrieval only ranks and trims — it never invents context and never reaches beyond what the
caller is authorized to see.

```text
Your question          Embed the         Nearest search           Rank & trim          Citations
+ up to 8 Vaults  -->  query        -->  pgvector · cosine   -->   min-score       -->  vault/src#n
                       (same model)      authorized JOIN            token budget         + score
                                              |
                                              v
                                    (only your authorized chunks
                                     can be ranked — see below)

Citations  -->  System message + conversation history  -->  Ollama chat model  -->  Answer
                (assembled within a token budget)                                   + citation badges
```

1. The question text is embedded with the same model used at ingestion.
2. A nearest-neighbour search over `pgvector` (cosine distance) returns the closest chunks — but only
   from Vaults the caller owns and selected (the authorization boundary below).
3. Results are scored by cosine similarity, dropped below a minimum score, and sorted best-first.
4. Excerpts (capped at 600 characters each) fill a citation token budget until it is exhausted; the
   rest are discarded.
5. The kept excerpts enter the prompt as a labelled `system` message, tagged `[vault/source#ordinal]`,
   alongside the conversation history assembled within its own context budget.
6. The model answers, and the assistant message stores its citations, which the interface renders as
   citation badges the reader can trace back to the original source.

If the embedding provider is unavailable, retrieval yields **zero citations and the chat still
answers** — it degrades, it does not fail. Only an explicit isolation boundary (an unauthorized or
archived Vault) is allowed to silently produce no citations; any other retrieval defect is treated as
a bug, not a reason to answer without grounding.

## The authorization boundary

This is the governance claim of the whole design ([D-026](DECISIONS.md)): the ownership check is a
**JOIN inside the retrieval SQL**, not a filter applied to results afterward.

```text
Retrieval query                         knowledge_chunk  (one table, every user)
  WHERE v.owner_id = you                +-------------------------------------------+
  AND   v.id IN (picked vaults)         |  Vaults you own & picked   <-- JOIN reaches |
  AND   v.archived = false              |  (PERSONAL · WORKSPACE)                     |
  ORDER BY cosine_distance              |                                             |
  LIMIT top-K                           |  Every other user's Vaults  --X never crossed
                                        +-------------------------------------------+
```

The query joins `chunk -> source -> vault` and constrains ownership in the `WHERE` clause. A chunk
from a Vault the caller does not own is therefore never ranked, never scored, and never returned — it
cannot reach the model even as a defect in later application code, because it was never a candidate in
the first place. Authorization is evaluated by the database, before ranking, not by application code
after it.

Scopes `PROJECT`, `TEAM`, and `ORGANIZATION` exist in the contract but are rejected today: only
`PERSONAL` and `WORKSPACE` Vaults can be retrieved, because no backend project, team, or organization
entity exists yet to authorize against.

## Bounds and decisions

- **Authorization in SQL.** Ownership is a JOIN predicate, not a post-filter — the core isolation
  guarantee ([D-026](DECISIONS.md)).
- **Exact search, no ANN index yet.** A plain cosine scan runs until corpus size and benchmarks
  justify an approximate index such as HNSW ([D-004](DECISIONS.md)).
- **Local embeddings.** `nomic-embed-text` on Ollama, 768 dimensions fixed to the chunk column.
- **Graceful degradation.** An embedding-provider outage produces zero citations while chat keeps
  working.
- **Bounded context.** At most eight Vaults per message, 600-character excerpts, capped by a citation
  token budget; the assembled history has its own separate token budget.
- **Immutable chunks.** Re-ingestion replaces a source's chunks rather than editing them, so the
  index stays rebuildable from the original files.
