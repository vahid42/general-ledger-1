# Performance Rules — Ultra Compact

## Purpose

Define enforceable Performance rules for General Ledger to prevent performance-risky designs, support human/automated/AI review, and guide the Performance Agent. This document complements ADRs, Domain Analysis, and Business Rules.

## Scope

Applies to:

* Domain Logic
* Application Services
* Repository/Database Access
* External Service Calls
* Collection Processing
* Transactions
* Memory
* Concurrency
* Batch Operations

## Core Rules

| ID    | Rule                                                                                                                           |
| ----- | ------------------------------------------------------------------------------------------------------------------------------ |
| P-001 | Avoid N+1 queries; prefer batch queries.                                                                                       |
| P-002 | Avoid unnecessary/repeated Database calls.                                                                                     |
| P-003 | Avoid Database access inside large loops.                                                                                      |
| P-004 | Prefer Batch operations where Business Rules and Transaction boundaries permit.                                                |
| P-005 | Avoid unnecessary materialization of large Collections.                                                                        |
| P-006 | Avoid repeating expensive operations when results can be reused/precomputed.                                                   |
| P-007 | Avoid repeated Remote Calls, especially inside loops.                                                                          |
| P-008 | Keep Transaction boundaries controlled; avoid unnecessarily long transactions.                                                 |
| P-009 | Avoid Blocking I/O in Performance-critical paths unless justified.                                                             |
| P-010 | Avoid unnecessary algorithmic complexity; consider `Map`, `Set`, `Index`, and pre-computation.                                 |
| P-011 | Avoid unnecessary Object/Collection creation and repeated conversions.                                                         |
| P-012 | Avoid unnecessary Serialization/Deserialization.                                                                               |
| P-013 | Avoid unnecessary or overly broad Synchronization/Locking.                                                                     |
| P-014 | Consider Caching for stable, read-heavy, expensive-to-read data, but evaluate Consistency, Invalidation, TTL, and Memory Cost. |

## Performance Requirements

Numeric requirements must be defined separately from general rules, e.g.:

* Maximum Response Time
* Expected Throughput
* Maximum Batch Size
* Maximum Collection Size

Performance Agent must not invent new Performance Requirements.

## Performance Review

Performance Agent must:

1. Analyze changed code **and relevant Context**.
2. Check explicit Performance Requirements.
3. Check applicable Performance Rules.
4. Check relevant ADRs.
5. Consider Business Rules affecting Performance.
6. Detect N+1, repeated DB/Remote calls, large-loop I/O, excessive materialization, high complexity, unnecessary allocations/serialization, long transactions, and excessive locking.
7. Provide Evidence and distinguish `CONFIRMED`, `POTENTIAL`, and `SUGGESTION`.
8. Assign Severity.
9. Explain the Finding and Risk.
10. Suggest remediation when possible.

## Evidence & Confidence

* `CONFIRMED`: Explicit Rule/Requirement violation is established.
* `POTENTIAL`: Risk-indicating Code Pattern exists, but Runtime/data-scale evidence is unavailable.
* `SUGGESTION`: Optional optimization without an established violation.

Agent must not make definitive Performance claims without sufficient Evidence.

## Severity

* `BLOCKER`: Likely severe system failure or violation of a specific Performance Requirement.
* `HIGH`: Significant likely impact on Latency, Database Load, Memory, or Network Traffic.
* `MEDIUM`: Likely concern at higher scale.
* `LOW`: Non-blocking optimization.

## Finding Format

```text
Finding: N+1 Database Query
Severity: HIGH
Confidence: HIGH
Status: POTENTIAL
Location: AccountHeadingService.java:42
Evidence: repository.findByCode() is called inside a loop.
Risk: Database calls increase with the number of records.
Suggested Action: Replace individual lookups with a batch query.
Rule: P-001, P-003
```

## Optimization Constraints

Performance optimization must not violate:

* Business Rules
* Domain Invariants
* Architecture Rules
* Data Consistency
* Transaction Semantics

Performance goal:

```text
Correctness + Scalability + Resource Efficiency + Predictable Performance
```

## Agent Collaboration

```text
Business Agent
      │
Architecture Agent
      │
Performance Agent
      │
Code Quality Agent
      ▼
Senior Reviewer
```

`Senior Reviewer` makes the final `APPROVE`, `COMMENT`, or `REQUEST_CHANGES` decision.

## Rule Convention

All Performance Rules use unique IDs:

```text
P-XXX
```

Current rules: `P-001` through `P-014`.

## Source of Truth

Priority order:

1. Explicit Performance Requirements
2. Approved ADRs
3. Project-specific Performance Rules
4. General Performance Rules
5. AI-generated Suggestions

AI-generated Suggestions alone must not trigger `REQUEST_CHANGES`.
