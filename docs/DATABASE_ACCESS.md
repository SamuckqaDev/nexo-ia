# Governed project database access

## Purpose

Nexo IA may inspect and modify a Project's database when an authorized user requests it. Project
connections are distinct from Nexo IA's internal PostgreSQL database, ownership, migrations, and
credentials.

```text
User request
  -> Project and environment selection
  -> execution plan and impact analysis
  -> Database Safety Engine
  -> access and privacy policy
  -> recovery checkpoint and preview
  -> approval
  -> transaction or database-specific safe workflow
  -> validation
  -> commit or rollback
  -> evidence and audit
```

Read-only is the default connection capability, not a permanent product limitation. Explicit grants
may allow data mutation, schema change, migrations, procedures, backup, restore, or administration.

## Connection and environment

A connection belongs to an organization, Project, environment, and owner. Credentials are secret
references resolved only inside the database adapter. Nexo IA stores no password in prompts, Skills,
plans, logs, Vaults, or ordinary connection records.

Initial relational support uses governed JDBC adapters, beginning with PostgreSQL and expanding by
tested database semantics. A database-specific MCP server may add an independent integration, but it
uses the same permissions, privacy, safety, evidence, and audit rules.

Development, test, staging, and production are explicit policy dimensions. Production selection is
always visible and may require a separate approver, maintenance window, verified recovery point, and
stronger capability grant.

## Capabilities

Capabilities are granted independently for metadata discovery, `SELECT`, data insert, update, delete,
procedure execution, schema change, migration execution, backup, restore, and administration. Grants
may be restricted by user, team, Project, environment, connection, catalog, schema, table, operation,
row estimate, duration, time window, plan, and run.

The Nexo frontend exposes schemas, tables, views, columns, keys, constraints, relationships, indexes,
database version, migrations, query results, execution plans, and access policy according to those
grants.

## Data changes

Before a material `UPDATE` or `DELETE`, the Database Safety Engine:

- parses and classifies the statement rather than trusting the model's description;
- derives or requires an equivalent bounded preview query;
- estimates affected rows and displays a redacted sample;
- rejects missing or unbounded predicates by default;
- verifies row, time, and resource limits;
- creates a recoverable copy or checkpoint when required and feasible;
- executes inside a transaction when the database and operation support safe transactional behavior;
- validates invariants, affected rows, and requested outcome before commit;
- rolls back on failed validation and records recovery evidence.

Bulk mutations, `TRUNCATE`, destructive procedures, and database-wide operations require dedicated
capabilities and fresh approval. `DROP DATABASE` is denied by default.

## Schema changes and migrations

Schema evolution prefers the Project's migration system, such as Flyway, over unversioned direct DDL.
Nexo IA analyzes dependencies, locks, compatibility, data conversion, application rollout, downtime,
and rollback or forward-recovery strategy. Destructive changes use expand, migrate, validate,
contract steps where practical.

A migration is prepared and reviewed separately from permission to execute it in each environment.
Development approval never grants staging or production execution.

## Recovery

Depending on database, environment, and risk, recovery may use transaction rollback, affected-row
backup, logical dump, snapshot, point-in-time recovery marker, inverse migration, or forward repair.
Nexo IA verifies that the selected method exists, completed, is retained long enough, and is usable.
High-risk production policy may require a tested restore before execution.

No mechanism guarantees zero data loss for every database and operation. Nexo IA therefore reports
unsupported transactional or recovery semantics explicitly and blocks work when the approved safety
contract cannot be satisfied.

## Privacy and audit

Query results remain subject to column, row, personal-data, secret, export, provider, and retention
policy. Results never reach a remote model merely because the connection is authorized. Previews and
logs redact protected values.

Audit links the requester, approver, Project, environment, connection identity, normalized statement
and hash, plan, task, migration, recovery artifact, estimated and actual effect, validation, outcome,
model, provider, processing location, execution location, duration, and evidence. Raw secrets and
unnecessary row data are excluded.
