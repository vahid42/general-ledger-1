# ADR-0009 — Testing Strategy

**Status:** Accepted
**Date:** 2026-08-20
**Related ADRs:** ADR-0006, ADR-0007, ADR-0008

## Purpose

Define the mandatory, layered testing strategy for the `Clean Architecture` + `DDD` + `Modular Monolith` project using the `Test Pyramid`.

Goals:

* Prevent Regression.
* Support safe Refactoring.
* Validate business behavior.
* Enforce architectural constraints.
* Keep tests fast, deterministic, and maintainable.

## Test Strategy

Testing follows the `Test Pyramid`:

1. `Unit Test` — highest volume, lowest cost.
2. `Application Test` — Use Case/Application behavior.
3. `Integration Test` — real component interactions.
4. `API Test` — Presentation/REST contract.
5. `Architecture Test` — architectural rules.
6. `E2E Test` — limited critical end-to-end scenarios.

Higher-level tests MUST be fewer because they are more expensive and slower.

## Unit Test

`Unit Test` is the primary test type.

Rules:

* `Domain` business logic MUST be thoroughly tested.
* Tests MUST NOT depend on Database, Network, or Framework.
* Tests MUST be fast, isolated, and deterministic.
* Mocking SHOULD be minimized in `Domain`.
* Mocking MAY be used where needed, mainly outside `Domain`.

Tools:

* `JUnit 5`
* `AssertJ`
* `Mockito`

## Application Test

Tests `Application Layer` Use Cases and their behavior.

MUST cover where applicable:

* `Command` and `Query` execution.
* Application ↔ Domain interaction.
* Transaction management.
* Use Case behavior.
* Application error handling.

Tests SHOULD validate Use Case behavior without unnecessary dependency on `Infrastructure` implementation details.

## Integration Test

Validates real interaction between system components.

Applicable targets:

* `Repository`
* `JPA Mapping`
* Transactions
* Database
* `Infrastructure Components`
* Module-to-Module integration

`Testcontainers` SHOULD be used when real external dependencies such as Database are required.

## API Test

Validates `Presentation` and REST API behavior.

MUST cover applicable:

* Request validation.
* HTTP status codes.
* Response body.
* Serialization / Deserialization.
* Error handling.
* API contract.

Tools:

* `Spring MockMvc`
* `RestAssured`

## Architecture Test

Architecture rules MUST be automatically validated.

MUST cover:

* Dependency direction.
* `Clean Architecture` rules.
* Module dependency rules.
* `Domain` dependency restrictions.
* Restrictions on `Infrastructure` dependencies.
* `Bounded Context` and Module dependency rules.

Tool:

* `ArchUnit`

Architecture Tests are part of automated testing and MUST run in CI.

## E2E Test

Only a limited number of `E2E Test` scenarios SHOULD exist for critical business flows.

Examples:

* Account creation.
* Journal/document registration.
* Transaction execution.
* Report generation.

E2E Tests MUST remain limited to prevent excessive Pipeline execution time.

## Test Pyramid

```text
             E2E Tests
              Fewest
                 ▲
                 │
        Integration / API
              Medium
                 ▲
                 │
            Unit Tests
              Most
```

**Rule:** As test level increases, test quantity decreases and execution cost increases.

## Testing Rules

* Every new feature MUST have appropriate tests.
* Every Bug MUST first be reproduced by a test before being fixed.
* Business Logic MUST NOT be accepted without `Unit Test`.
* Tests MUST be independent and repeatable.
* Test execution order MUST NOT affect results.
* Tests MUST NOT depend on Development or Production data.
* Tests MUST NOT require real external services unless explicitly defined as `Integration Test` or `E2E Test`.
* Architecture Tests MUST run in CI.
* A Pull Request MUST NOT be merged when required tests fail.

## Test Coverage

Coverage is a supporting quality metric, NOT the sole measure of test quality.

| Code Type            |             Minimum / Target |
| -------------------- | ---------------------------: |
| `Domain`             |                        ≥ 90% |
| `Application`        |                        ≥ 80% |
| `Infrastructure`     | Based on importance and risk |
| `API`                |       All Critical Endpoints |
| `Architecture Rules` |            All defined rules |

Coverage MUST be evaluated together with scenario quality and behavioral coverage.

## Implementation Rules

* This testing strategy is mandatory for all Modules.
* Each Module MUST have tests appropriate to its responsibilities and interactions.
* `Unit Test` for Business Logic is mandatory.
* Other test levels are defined according to Module requirements.
* All required tests for a Pull Request MUST pass before Merge.

## Agent Instructions

When analyzing or implementing tests:

1. Treat `Unit Test` for Business Logic as mandatory.
2. Keep `Domain` tests independent from `Database`, `Network`, and Framework.
3. Prefer the lowest-cost test level capable of validating the behavior.
4. Use `Integration Test` only when real component interaction must be verified.
5. Keep `E2E Test` limited to critical business flows.
6. Validate architectural constraints with `ArchUnit`.
7. Never accept a new Bug fix without a regression test.
8. Never consider Coverage alone sufficient evidence of test quality.
9. Ensure mandatory tests pass before Merge.
