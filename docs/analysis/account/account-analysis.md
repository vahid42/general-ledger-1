# Account 

## Purpose

`Account` is a financial Entity created under a `Leaf AccountHeading` to hold the balance for a specific `Branch` and `Currency`.

## Core Rules

* Every `Account` belongs to exactly one `Leaf AccountHeading`.
* `AccountHeading.isAccountAcceptable() == true` is required for creation.
* `AccountCode` is system-generated, unique, and immutable.
* `AccountId` is the internal identity and immutable.
* After creation, **only `Name` is mutable**.
* `Account` cannot be deleted; it can only be closed.
* Status: `OPEN | CLOSED`.

## Account Code

```text
AccountCode = Branch + Currency + AccountHeading + AccountSequence
```

`AccountSequence` is sequential per:

```text
Branch + Currency + AccountHeading
```

The system must identify the last used sequence for the combination and generate the next one.

## Immutability

The following cannot change after creation:

```text
AccountId
AccountCode
Branch
Currency
AccountHeading
```

Only:

```text
Account.rename(newName)
```

is allowed.

## Balance Rules

* `Balance` is owned and maintained by `Account`.
* `Balance` must never be directly assigned by `Application` or Client.
* Balance changes only result from a valid financial operation such as `Debit/Credit`.
* `Account` owns Balance state but does not create or validate Transactions.
* `CLOSED` Accounts cannot participate in financial operations or change Balance.
* Negative Balance is governed by the owning `Leaf AccountHeading`; `Account` must not redefine this rule.

```text
Transaction
  → Financial Operation
    → Account
      → Balance Change
```

## Closing Rules

```text
Balance == 0 → Account.close() → CLOSED
Balance != 0 → Reject
```

A closed Account remains persisted for history and cannot be reused for new financial operations.

## Domain Invariants

* **INV-01:** Account must belong to a `Leaf AccountHeading`.
* **INV-02:** `AccountHeading.isAccountAcceptable()` must be true.
* **INV-03:** `AccountCode` is system-generated.
* **INV-04:** `AccountCode` must be unique.
* **INV-05:** Sequence is generated per `Branch + Currency + AccountHeading`.
* **INV-06:** Only `Name` is mutable.
* **INV-07:** Account cannot be deleted.
* **INV-08:** Account can close only when `Balance == 0`.
* **INV-09:** `CLOSED` Account cannot participate in new financial operations.
* **INV-10:** Balance cannot be directly assigned.
* **INV-11:** `CLOSED` Account cannot change Balance.
* **INV-12:** Negative Balance follows the `AccountHeading` rule.

## Domain Behaviors

```text
create()
rename()
close()
applyDebit()
applyCredit()
```

`applyDebit()` / `applyCredit()` require final definition at the `Transaction` / `Financial Operation` design level.

## Responsibilities

### Account Owns

* Identity and immutable identity data
* `AccountCode`
* `Name`
* `Branch` / `Currency`
* `Balance`
* `Status`
* Account-level invariants
* Closed-state protection
* Zero-balance closing rule
* `AccountHeading`-derived negative-balance rule

### Account Does Not Own

* Transaction creation
* Overall Transaction validation
* Multi-Account transaction coordination
* Accounting document generation
* System-level financial workflow

These belong to the relevant `Transaction` / `Financial Operation` Domain.
