# AI Code Review Policy

## Core Rule

Review the PR against **documented system intent**, not personal preference.

Focus on:

`Correctness | Architecture | Domain Integrity | Business Rules | Risk | Regression | Tests`

Review only relevant areas. Do not create Findings without a real problem.

## Review Workflow

```text
System Context
→ Relevant Documentation
→ Understand Change
→ Module / Bounded Context
→ Architecture
→ Domain / Business Rules
→ Application
→ Infrastructure
→ Presentation
→ Tests
→ Security / Performance if relevant
→ Findings
→ Decision
→ Report
```

Skip a stage only when genuinely irrelevant; mark it `Not Applicable`.

## Change Understanding

Before evaluating implementation, determine:

* Purpose and intended behavior
* Changed Module and Layer
* Affected Domain concepts
* Added, removed, or changed behavior

Review changes in their surrounding context, not isolated lines.

## Architecture

Check for:

* Domain → Infrastructure / Presentation dependencies
* Improper framework dependencies
* Business Logic in Presentation
* Business Decisions in Infrastructure
* Excessive Core Business Logic in Application
* Invalid Module dependencies
* Bounded Context violations

Significant architectural violations should receive high severity.

## Domain & Business Rules

For Domain changes evaluate:

`Entity | Value Object | Aggregate | Aggregate Root | Domain Service | Policy | Event | Repository Abstraction | Invariant`

Determine where the behavior belongs and compare it with relevant Domain Documentation and Use Cases.

Verify:

* Documented Rules are respected
* Required behavior is implemented
* No documented Rule is contradicted
* Business behavior is in the appropriate Layer
* No undocumented Business Rule is invented

**Never infer or invent Business Rules.**

## Application

Application primarily orchestrates Use Cases:

`Load → Invoke Domain → Coordinate → Control Flow → Return Result`

It MUST NOT become the primary owner of Core Business Rules.

## Infrastructure

Infrastructure owns technical concerns:

`Persistence | Repository Implementation | External Services | Messaging | Serialization | Transactions | Configuration | Framework Integration`

It MUST NOT own Core Business Rules.

## Presentation

Check:

`Validation | DTO Boundary | Mapping | HTTP Semantics | Error Handling | Authentication | Authorization`

Controllers MUST remain thin and contain no Core Business Logic.

## Tests

For behavioral changes, evaluate whether appropriate tests are required:

`Unit | Application | Integration | API | Regression`

Missing tests are Findings only when the change requires meaningful test coverage.

## Evidence

Every Finding MUST be supported by verifiable evidence from:

`Changed Code | Existing Code | Documentation | Use Cases | Tests`

If evidence is insufficient, do not create a speculative Finding.

## False Positive Control

Prefer **fewer, high-confidence Findings**.

Do NOT report:

* Personal coding style
* Different but valid implementation approaches
* Theoretical refactoring
* Preferred patterns without project justification
* Abstractions without a demonstrated need

A valid Finding must represent:

`Risk | Defect | Violation | Documented Mismatch`

## Severity

| Severity   | Meaning                                                                                                      |
| ---------- | ------------------------------------------------------------------------------------------------------------ |
| BLOCKER    | Must not be accepted; severe correctness, security, invariant, architecture, or explicit requirement failure |
| CRITICAL   | High-probability serious production failure or incorrect behavior                                            |
| MAJOR      | Important defect normally requiring correction before acceptance                                             |
| MINOR      | Real issue with limited impact                                                                               |
| SUGGESTION | Optional improvement                                                                                         |

Do not inflate severity.

Suggestions MUST NOT be presented as mandatory defects.

## Documentation Conflicts

When Implementation conflicts with Documentation:

1. Identify the conflict.
2. Determine the applicable Source of Truth.
3. Report the mismatch.
4. Do not derive a new Rule from implementation.

If authoritative documents conflict, report the ambiguity instead of making an unsupported decision.

## Finding Format

```text
[SEVERITY]

Location:
<file:line>

Problem:
<what is wrong>

Why it matters:
<impact / risk>

Recommended action:
<fix>
```

## Decision

```text
ACCEPTED
```

When no BLOCKER, CRITICAL, or MAJOR exists and architecture/business intent is respected.

```text
CHANGES_REQUESTED
```

When any BLOCKER, CRITICAL, MAJOR, or important documented Rule violation exists.

```text
ACCEPTED_WITH_SUGGESTIONS
```

When only MINOR or SUGGESTION findings exist.

## Final Output

```text
# Review Result

Status: ACCEPTED | CHANGES_REQUESTED | ACCEPTED_WITH_SUGGESTIONS

## Findings

### [SEVERITY] <Title>

Location:
<file:line>

Problem:
<problem>

Why it matters:
<impact / risk>

Recommended action:
<fix>

## Summary

- Blockers: N
- Critical: N
- Major: N
- Minor: N
- Suggestions: N
```

## Final Principles

* Review documented intent, not preference.
* Use relevant documentation before judging behavior.
* Use evidence for every Finding.
* Prefer high-confidence Findings.
* Do not invent Business Rules.
* Do not inflate Severity.
* Do not turn Suggestions into mandatory changes.
* Focus on real Risk, Defect, Violation, or documented Mismatch.

**Review quality matters more than comment count.**
