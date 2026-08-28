# JournalDocument  

## Purpose

`JournalDocument` is the **Aggregate Root** of an accounting document. It owns the document header and `DocumentLine[]` and must enforce all document invariants and valid state transitions.

## Aggregate Rules

* `JournalDocument` = Aggregate Root.
* `DocumentLine` = Child entity; never managed independently in Domain.
* All line operations (`addLine`, `removeLine`, `changeLine`) must go through `JournalDocument`.
* `DocumentLine` does **not** require `DocumentId` in Domain.
* `document_id` may exist as a Persistence-layer Foreign Key.

## Header

```text
DocumentId
Title
IssueDate
DocumentType: NORMAL | TEMPORARY
Status: DRAFT | ACCEPTED | REJECTED
CreatedBy
CreatedAt
ApprovedBy?
ApprovedAt?
```

* `CreatedBy` and `CreatedAt` are mandatory.
* `ApprovedBy` and `ApprovedAt` are null before Approval and required after Approval.

## DocumentLine

```text
RowNumber
Type: DEBIT | CREDIT
Amount
AccountNumber
IssueDate
Description
```

Rules:

* `Amount > 0`.
* `RowNumber` is unique within the document and managed by the Aggregate.
* Every line references a valid Account.
* Closed Accounts cannot be used for new financial documents.

## DocumentType & Status

`DocumentType` and `Status` are independent concepts.

Valid combinations:

```text
NORMAL    + ACCEPTED
TEMPORARY + DRAFT
TEMPORARY + REJECTED
```

Invalid:

```text
NORMAL + DRAFT
```

### Normal

```text
NORMAL + ACCEPTED
```

Created directly as Accepted; no manager Approval required.

### Temporary

Created as:

```text
TEMPORARY + DRAFT
```

Approval:

```text
TEMPORARY + DRAFT
        │ Approve (authorized user/manager)
        ▼
NORMAL + ACCEPTED
```

Rejection:

```text
TEMPORARY + DRAFT
        │ Reject
        ▼
TEMPORARY + REJECTED
```

## Domain Behaviors

```text
createNormal()
createTemporary()
addLine()
removeLine()
changeLine()
approve(user)
reject(user)
```

Behaviors must preserve all Aggregate invariants.

### Approval Rules

* Only `TEMPORARY + DRAFT` can be approved.
* Approval requires an authorized user/manager.
* Approval is a Domain Behavior, not direct mutation of `DocumentType`/`Status`.
* On Approval:

  * `DocumentType = NORMAL`
  * `Status = ACCEPTED`
  * `ApprovedBy != null`
  * `ApprovedAt != null`

### Rejection Rules

* Rejection is a Domain Behavior.
* A rejected temporary document remains:

  * `DocumentType = TEMPORARY`
  * `Status = REJECTED`
* Rejector identity/time are not currently mandatory Domain fields.

## Document Invariants

```text
INV-01  Document must contain lines.
INV-02  Minimum 2 lines.
INV-03  First line must be DEBIT.
INV-04  At least one line must be CREDIT.
INV-05  Total Debit == Total Credit.
INV-06  RowNumber must be unique within the document.
INV-07  CreatedBy is required.
INV-08  CreatedAt is required.
INV-09  NORMAL documents start as ACCEPTED.
INV-10  TEMPORARY documents start as DRAFT.
INV-11  Only TEMPORARY + DRAFT can be approved.
INV-12  Approval requires an authorized user/manager.
INV-13  Approval changes TEMPORARY → NORMAL.
INV-14  Approval changes DRAFT → ACCEPTED.
INV-15  ApprovedBy and ApprovedAt are required after Approval.
INV-16  Rejection results in TEMPORARY + REJECTED.
INV-17  Closed Accounts cannot be used in new financial documents.
```

## Accounting Balance Rules

Every document must satisfy:

```text
Total Debit = Total Credit
```

Minimum valid structure:

```text
Line 1 → DEBIT
Line 2 → CREDIT
```

The first line must always be `DEBIT`.

## Lifecycle

```text
NORMAL:
    NORMAL + ACCEPTED

TEMPORARY:
    TEMPORARY + DRAFT
        ├── Approve → NORMAL + ACCEPTED
        └── Reject  → TEMPORARY + REJECTED
```

## Domain Responsibilities

`JournalDocument` is responsible for:

* Document identity and header.
* `DocumentType` and `Status`.
* Creation metadata.
* Approval/Rejection.
* Managing `DocumentLine[]`.
* `RowNumber` uniqueness and ordering.
* Minimum line count.
* First-line `DEBIT` rule.
* At least one `CREDIT`.
* Debit/Credit balance.
* Valid Account usage.
* Valid lifecycle transitions.

## Agent Instructions

* Treat `JournalDocument` as the sole Aggregate boundary for `DocumentLine`.
* Never model `DocumentLine.DocumentId` as a required Domain property.
* Never bypass Domain behaviors to mutate lifecycle state.
* Never allow invalid `DocumentType + Status` combinations.
* Preserve every listed invariant when modifying the Domain model.
