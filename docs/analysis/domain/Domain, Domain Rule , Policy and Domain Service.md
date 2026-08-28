# Domain, Domain Rule, Policy and Domain Service 

## Purpose

Define precise ownership and enforcement of `Domain`, `Domain Rule`, `Invariant`, `Policy`, `Aggregate`, `Domain Service`, and `Application Service`.

**Core principle:** Do not decide rule placement by class type. First determine **where the Business Rule can be correctly owned and enforced**.

```text
Policy ≠ always a separate class
Domain Rule ≠ always a Service
Domain Service ≠ every Service in Domain
```

---

## Core Concepts

### Domain

The business problem space modeled by the software, including:

* Business concepts
* Business rules
* Business behavior
* Constraints
* Business decisions
* Relationships between concepts

The Domain Model should express this knowledge through a behavior-rich model and shared Domain language.

### Domain Rule

A Business Rule that must hold within the Domain.

Examples:

```text
AccountHeading at Level 5 cannot have children.
AccountHeading.Code must be unique.
Account can only be created under a Leaf.
```

A Domain Rule's enforcement location depends on its scope.

### Invariant

A Domain Rule that must always hold for an Aggregate to remain valid.

```text
Invariant → Aggregate-owned consistency rule
```

Examples for `AccountHeading`:

```text
Level must be 0..5.
Level 5 cannot have children.
Child.Level = Parent.Level + 1.
```

If the rule depends only on the Aggregate's own state, the Aggregate must enforce it.

### Aggregate

An Aggregate is a consistency boundary containing related Domain Objects and one `Aggregate Root`.

```text
AccountHeading Aggregate
├── Identity
├── State
├── Invariants
└── Behaviors
```

The `Aggregate Root` owns consistency and enforcement of internal Invariants.

---

## Policy

A `Policy` is a Business Rule / Decision Rule answering:

```text
Given these conditions, what decision should be made?
```

Examples:

```text
Can this Document be approved?
Is this AccountHeading.Code allowed?
Is this operation permitted under the current conditions?
```

### Policy Placement

A Policy is a **Domain concept**, not necessarily a separate class.

```text
Policy ≠ Separate Class
```

It may be:

* Enforced directly by an Aggregate
* Modeled as an independent Domain Object
* Implemented through a `Domain Service`

Use a separate Policy object when the decision is complex, reused across Use Cases, independent of one Entity, or valuable as an explicit Domain concept.

Example:

```java
public class DocumentApprovalPolicy {
    public boolean canApprove(
        Document document,
        FiscalPeriod period
    ) {
        ...
    }
}
```

---

## Domain Service

A `Domain Service` performs Domain Logic that does not naturally belong to one Entity or Value Object.

Typical signal:

```text
Business Rule
+
multiple Domain Objects / Aggregates / external Domain information
→ possible Domain Service
```

A Domain Service is appropriate when the logic is genuinely Domain logic but cannot be naturally owned by a single Domain Object.

### Example: Code Uniqueness

```text
Rule:
AccountHeading.Code must be unique across all AccountHeadings.
```

A single `AccountHeading` cannot determine whether another Aggregate already has the same Code.

Therefore:

```text
Code Uniqueness
    ↓
Domain Rule / Policy
    ↓
AccountHeadingDomainService
    ↓
AccountHeadingRepository
```

Example:

```java
public void ensureCodeIsUnique(String code) {
    if (repository.existsByCode(code)) {
        throw new IllegalStateException(...);
    }
}
```

**Repository usage alone does not make something a Domain Service.**

First ask:

```text
Is this Business Logic?
```

If not, it is likely an Application or Infrastructure concern.

If it is:

```text
Business Rule
+
information outside the Aggregate
→ Domain Service may be appropriate
```

---

## Aggregate-Owned Rules

For `AccountHeading`, assume:

```text
id
parentId
code
name
nature
allowNegativeBalance
level
```

Internal rules:

```text
Level ∈ 0..5.
Root is Level 0.
Root has no Parent.
Child.Level = Parent.Level + 1.
Level 5 is a Leaf.
Leaf cannot have children.
Code is not empty.
Name is not empty.
Child.Nature inherits from Parent.
Child.allowNegativeBalance inherits from Parent.
```

These are internal consistency rules and therefore belong to the `AccountHeading Aggregate`.

```text
AccountHeading
└── Invariants
    ├── Level
    ├── Root rules
    ├── Parent rules
    ├── Leaf rules
    ├── Code validation
    ├── Name validation
    └── inheritance rules
```

Example:

```java
public AccountHeading createChild(...) {
    if (isLeaf()) {
        throw new IllegalStateException(...);
    }
    ...
}
```

No separate `LeafPolicy` is required when the Aggregate itself can enforce the rule.

---

## Rules Requiring External State

### Code Uniqueness

```text
AccountHeading.Code must be globally unique.
```

This requires checking other Aggregates:

```java
repository.existsByCode(code)
```

Therefore it is not an internal `AccountHeading` Invariant.

Possible owner:

```text
AccountHeadingDomainService
or
Policy + Domain Service
```

---

### Heading Deletion

Rules:

```text
Heading with children cannot be deleted.
Heading with Accounts cannot be deleted.
```

`AccountHeading` alone does not know whether external children or Accounts exist.

Therefore:

```text
Delete Use Case
├── check children
├── check accounts
└── delete
```

The rule remains a **Domain Rule** even though its enforcement requires information outside the Aggregate.

```text
Domain Rule
└── Enforcement outside Aggregate because external state is required
```

---

## Account Creation Under Leaf

Rule:

```text
Account can only be created under a Leaf.
```

`AccountHeading` should expose the relevant Domain fact:

```java
public boolean isLeaf() {
    return level == MAX_LEVEL;
}
```

It should not necessarily own the entire Account creation decision:

```text
AccountHeading
└── isLeaf()

Account Creation Logic
└── uses isLeaf()
```

`isLeaf()` expresses the Aggregate's own state; the Account creation Use Case decides how that fact participates in the overall operation.

---

## Document Approval Example

Rule:

```text
Document can be approved only when:
1. All Lines are valid.
2. FiscalPeriod is open.
3. Document is in an approvable state.
```

If all required information belongs to `Document`:

```text
Document
└── approve()
```

If the decision requires:

```text
Document
+
FiscalPeriod
+
UserAuthorization
```

then the rule is not naturally owned by `Document` alone.

Possible modeling:

```text
DocumentApprovalPolicy
```

or:

```text
DocumentApprovalDomainService
```

```text
Document ─────────────┐
                      │
FiscalPeriod ─────────┼──→ Approval Decision
                      │
Authorization ────────┘
```

---

## Domain Service vs Policy

They are related but not equivalent.

### Policy

Focuses on the **Business Decision / Rule**:

```text
Is this operation allowed?
```

### Domain Service

Focuses on **where independent Domain Logic is executed** when it does not naturally belong to one Entity or Value Object.

Therefore:

```text
Policy
  ↓
may be implemented by a Domain Service
```

but:

```text
Policy ≠ Domain Service
```

Example:

```text
DocumentApprovalPolicy
```

may be an explicit Policy object, while:

```text
AccountHeadingDomainService
```

may enforce one or more Domain Rules using repository information.

---

## Domain Service vs Application Service

### Domain Service

Owns or executes Domain Logic:

```text
Business Logic
Business Rules
Domain Decisions
```

### Application Service

Coordinates a Use Case:

```text
Use Case orchestration
Transaction coordination
Calling Domain Objects
Calling Repositories
```

The `Application Service` should not become the primary owner of Business Rules. It should delegate Domain behavior and decisions to the Domain Model.

---

## Create AccountHeading

Use Case:

```text
Create AccountHeading
```

Application orchestration:

```text
CreateAccountHeadingApplicationService
├── coordinate code-uniqueness check
├── create AccountHeading
└── save
```

Domain ownership:

```text
AccountHeadingDomainService
└── Code Uniqueness

AccountHeading
├── Level rules
├── Parent rules
├── Name rules
├── Code intrinsic validation
└── other Invariants
```

The `Application Service` coordinates; the Domain owns Business Rules.

---

## Decision Matrix

| Business Question                                                  | Appropriate Owner                       |
| ------------------------------------------------------------------ | --------------------------------------- |
| Is `Level` valid?                                                  | `AccountHeading`                        |
| Can a Leaf have a child?                                           | `AccountHeading`                        |
| Must Child inherit `Nature` from Parent?                           | `AccountHeading`                        |
| Is `Code` intrinsically valid/non-empty?                           | `AccountHeading`                        |
| Is `Code` globally unique?                                         | `Domain Service` / `Policy`             |
| Can a Heading with external Children be deleted?                   | Domain Logic requiring Repository state |
| Can an Account be created under this Heading?                      | Account Creation Logic using `isLeaf()` |
| Can Document be approved using only Document state?                | `Document` Aggregate                    |
| Does approval require `Document` + `FiscalPeriod` / Authorization? | `Policy` / `Domain Service`             |
| Coordinate Repository + Aggregate for a Use Case?                  | `Application Service`                   |

---

## Decision Rules

Never start with:

```text
"Which Service should contain this Rule?"
```

Start with:

```text
1. Is this a Business Rule?

2. Does it depend only on the state of one Aggregate?

   Yes → Aggregate owns/enforces it.

3. If not, is there an independent Business Decision?

   Yes → model it as a Policy when appropriate.

4. Does the Domain Logic require multiple Objects,
   Aggregates, or external Domain information?

   Yes → Domain Service may be appropriate.

5. Is the responsibility only Use Case orchestration?

   Yes → Application Service.
```

---

## AccountHeading Model

```text
AccountHeading
├── Aggregate Root
├── Invariants
│   ├── Level 0..5
│   ├── Root rules
│   ├── Parent rules
│   ├── Leaf rules
│   ├── Code validation
│   ├── Name validation
│   └── inheritance rules
├── Behaviors
│   ├── createRoot()
│   ├── createFirstLevel()
│   ├── createChild()
│   └── rename()
└── Queries
    ├── isRoot()
    └── isLeaf()

AccountHeadingDomainService
└── Code Uniqueness
    └── AccountHeadingRepository

Delete Heading
└── Domain Rule requiring external Aggregate state

Account Creation
└── Domain Rule using AccountHeading.isLeaf()
```

---

## Agent Instructions

When deciding where a Domain Rule belongs:

```text
DO:
- Identify the Business Rule first.
- Identify the required information.
- Determine the consistency boundary.
- Prefer Aggregate-owned enforcement when only Aggregate state is required.
- Use Policy when an explicit Business Decision is useful.
- Use Domain Service when Domain Logic does not naturally belong to one Entity/Value Object.
- Use external state only when the rule inherently requires it.
- Keep Application Service focused on Use Case orchestration.

DO NOT:
- Create a separate class for every Policy.
- Put every Domain Rule in a Domain Service.
- Treat every Domain Service as a generic Service.
- Put core Business Rules primarily in Application Services.
- Assume Repository usage automatically implies Domain Service.
- Move a Domain Rule outside the Domain merely because its enforcement needs Repository data.
- Make an Aggregate responsible for decisions requiring state it cannot observe.
```

## Final Definitions

| Concept               | Definition                                                                                                                                           |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Domain`              | Business problem space and its concepts, behavior, rules, constraints, and decisions.                                                                |
| `Domain Rule`         | A Business Rule that must hold within the Domain.                                                                                                    |
| `Invariant`           | A Domain Rule that must always hold for an Aggregate to remain valid; the Aggregate enforces it.                                                     |
| `Policy`              | A Business/Decision Rule determining what decision should be made under specific conditions; it need not be a separate class.                        |
| `Aggregate`           | A consistency boundary with an `Aggregate Root` responsible for its integrity and Invariants.                                                        |
| `Domain Service`      | Domain Logic not naturally owned by one Entity or Value Object, often involving multiple Domain Objects, Aggregates, or external Domain information. |
| `Application Service` | Use Case coordinator that orchestrates Domain Objects and infrastructure interactions without becoming the primary owner of Business Rules.          |

## Core Principle

```text
Business Rule
    ↓
Identify required state/information
    ↓
Only one Aggregate's state?
    ├── Yes → Aggregate / Invariant
    └── No
         ↓
Independent Business Decision?
    ├── Yes → Policy
    └── No
         ↓
Domain Logic spanning Objects/Aggregates/external state?
    ├── Yes → Domain Service
    └── No → Re-evaluate ownership
```
