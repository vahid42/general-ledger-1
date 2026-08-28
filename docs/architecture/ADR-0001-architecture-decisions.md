# ADR-0001 — Architecture Decision Records

**Status:** Accepted
**Date:** 2026-08-20

## Context

`general-ledger` is a modular accounting/ledger system with important architectural decisions involving:

`Architecture | Module Boundaries | Layer Dependencies | DDD | Domain Modeling | Persistence | Database | API | Transactions | Testing | Accounting Concepts`

Without documented decisions:

* Decision rationale may be lost.
* Future decisions may conflict with existing architecture.
* New developers may not understand why the system is structured this way.

## Decision

Use **Architecture Decision Records (ADR)** to document important, architecture-impacting decisions.

ADR location:

```text id="3ptj4v"
docs/architecture/
```

Each architectural decision MUST have its own versioned file:

```text id="2n8mcf"
0001-record-architecture-decisions.md
0002-<decision-name>.md
0003-<decision-name>.md
```

Each ADR MUST contain at least:

`Context | Decision | Consequences`

Allowed ADR statuses:

`Proposed | Accepted | Deprecated | Superseded`

When a decision is replaced:

* The old ADR MUST NOT be deleted.
* Set its status to `Superseded`.
* Reference the replacing ADR.

ADRs are for **significant architectural decisions**, not minor implementation details.

## Consequences

### Positive

* Architectural decisions become traceable.
* Decision rationale stays with the source code.
* Architectural history is preserved.
* Architectural changes can be reviewed and compared.
* Team onboarding becomes easier.
* Conflicting architectural decisions are less likely.

### Negative

* ADR creation and maintenance require discipline.
* Unnecessary ADRs increase documentation complexity.
* Important architectural changes require corresponding ADR updates.

## Implementation

Architecture documentation follows:

```text id="f5g8hz"
general-ledger/
└── docs/
    └── architecture/
        └── 0001-record-architecture-decisions.md
```

Project modules include:

`ledger-domain | ledger-application | ledger-infrastructure | ledger-presentation | ledger-test | ledger-bootstrap`

Every significant architectural decision SHOULD be documented as an ADR **before or together with its implementation**.

## Agent Rules

When reviewing architecture:

* Check relevant ADRs in `docs/architecture/`.
* Treat `Accepted` ADRs as authoritative architectural decisions unless a higher-priority source overrides them.
* Do not ignore `Superseded` ADRs when historical context is relevant.
* Do not treat `Deprecated` or `Superseded` decisions as current architecture.
* Do not invent architectural rules absent from authoritative documentation.
* If implementation conflicts with an applicable ADR, report the mismatch as a Finding.
