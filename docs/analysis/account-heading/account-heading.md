# AccountHeading 

## Purpose

`AccountHeading` is a hierarchical accounting-heading Aggregate Root. It owns internal consistency, state, and behaviors; cross-Aggregate rules are enforced through Domain Policy/Service or external Domain logic.

## Core Model

* **Identity:** `AccountHeadingId`
* **State:** `parentId`, `code`, `name`, `nature`, `allowNegativeBalance`, `level`
* **Hierarchy:** `Level 0..5`

  * `0` = Root
  * `5` = Leaf
  * `0..4` = Non-Leaf
* `leaf` is **derived from `level`**, not stored separately.
* Root has `parentId = null` and `nature = null`.
* Non-Root always has a Parent.
* `child.level = parent.level + 1`.

## Invariants

1. `0 <= level <= 5`.
2. Root: `level = 0`, `parentId = null`.
3. Non-Root must have a Parent.
4. Child level must equal `parent.level + 1`.
5. `level == 5` means Leaf.
6. Leaf cannot create Child.
7. `code` must not be `null` or blank.
8. `name` must not be `null` or blank.
9. `code` is immutable after creation.
10. `parentId` is immutable after creation.
11. `nature`:

    * Root → `null`
    * First Level → defined by caller
    * Child → inherited from Parent
12. `allowNegativeBalance`:

    * First Level → defined by caller
    * Child → inherited from Parent
13. Child cannot independently override inherited `nature` or `allowNegativeBalance`.

## Behaviors

* `createRoot(...)`

  * `parentId = null`
  * `level = 0`
  * `nature = null`
  * `allowNegativeBalance = false`
* `createFirstLevel(...)`

  * Only valid on Root.
  * Non-Root invocation → `IllegalStateException`.
  * Caller defines `nature` and `allowNegativeBalance`.
* `createChild(id, code, name)`

  * Uses current Aggregate as Parent.
  * `parentId = current.id`
  * `level = current.level + 1`
  * Inherits `nature` and `allowNegativeBalance`.
  * Cannot execute on Leaf.
* `rename(newName)`

  * Changes only `name`.
  * Preserves `id`, `parentId`, `code`, `nature`, `allowNegativeBalance`, and `level`.
  * Uses immutable-state replacement.

## Queries / Derived Behavior

* `isRoot()` → `level == 0`
* `isLeaf()` → `level == 5`
* These are read-only derived behaviors, not state-changing commands.

## Domain Rule Ownership

### Aggregate-Owned Rules

Rules requiring only `AccountHeading` state must be enforced by the Aggregate:

* Level range
* Root/Parent rules
* Level progression
* Leaf detection
* Leaf cannot create Child
* `code` / `name` validation
* Immutable `code` / `parentId`
* Nature inheritance
* `allowNegativeBalance` inheritance

### Policy

A Policy represents Business decision logic; it **does not necessarily mean logic outside the Aggregate**.

A Policy may be:

* enforced inside the Aggregate;
* implemented as an independent Domain Policy;
* enforced by a Domain Service when external information is required.

### Domain Service

Use a Domain Service when Domain logic:

* does not belong to one Aggregate;
* requires external Aggregate/state information;
* involves multiple Domain Objects/Aggregates.

Repository usage alone does **not** make a Domain Service an Application Service.

## AccountHeadingDomainService

Current responsibility:

```text
Code Uniqueness
        ↓
AccountHeadingDomainService
        ↓
AccountHeadingRepository
```

`AccountHeading.code` must be globally unique.

The rule cannot be enforced from one `AccountHeading` alone because it requires repository queries such as:

* `existsByCode(...)`
* `existsByCodeAndIdNot(...)`

`AccountHeading` must not directly depend on `AccountHeadingRepository`.

## External Domain Rules

### Delete Heading

A Heading cannot be deleted if it has:

* Child
* Account

This is still a **Domain Rule**, but its enforcement requires external information. Therefore Delete is currently **not** an `AccountHeading` behavior and remains outside the Aggregate/orchestrated externally.

### Account Creation

An `Account` may only be created under a Leaf.

`AccountHeading` exposes only:

```java
isLeaf()
```

The Account-creation logic decides:

```text
isLeaf() == true  → allowed
isLeaf() == false → rejected
```

No `canCreateAccount()` behavior is required in `AccountHeading`.

## Aggregate Boundary

```text
AccountHeading Aggregate
├── Identity
│   └── AccountHeadingId
├── State
│   ├── parentId
│   ├── code
│   ├── name
│   ├── nature
│   ├── allowNegativeBalance
│   └── level
├── Behaviors
│   ├── createRoot()
│   ├── createFirstLevel()
│   ├── createChild()
│   └── rename()
├── Queries
│   ├── isRoot()
│   └── isLeaf()
└── Internal Invariants

External Domain Logic
├── Code Uniqueness → Domain Service + Repository
├── Delete → external checks for Children/Accounts
└── Account Creation → Account logic + isLeaf()
```

## Implementation Constraints

* State is immutable (`final` fields).
* State changes create a new `AccountHeading` instance.
* `leaf` must not be stored as independent state; derive it from `level` to avoid inconsistent duplicate state.
* `rename()` preserves Identity and all immutable properties except `name`.

## Decision Rules

```text
Rule requires only Aggregate state
→ AccountHeading

Business decision that may be modeled independently
→ Policy

Rule/Policy requires external state, Repository, or multiple Domain Objects
→ Domain Service / external Domain logic
```

The classification is based on the **nature and scope of the Business Rule**, not merely where its code happens to reside.

## Agent Instructions

* Treat `AccountHeading` as the owner of its internal consistency.
* Never move an internal invariant to Repository/Application logic.
* Do not introduce Repository dependencies into `AccountHeading`.
* Do not store `leaf`; derive it from `level`.
* Preserve `code` and `parentId` immutability.
* Preserve inheritance of `nature` and `allowNegativeBalance`.
* Keep global `code` uniqueness outside the Aggregate via `AccountHeadingDomainService`.
* Keep Delete outside the Aggregate when Child/Account existence must be checked externally.
* Enforce Account creation through `AccountHeading.isLeaf()`.
* Do not assume every Policy requires a separate Policy class.
* Choose Aggregate, Policy, or Domain Service according to Rule scope and required information.
