# JournalDocument — Domain Analysis

## 1. تعریف

**JournalDocument (سند حسابداری)** یک سند مالی است که شامل اطلاعات کلی سند و مجموعه‌ای از بندهای سند است.

هر سند شامل:

* Header
* نوع سند
* وضعیت سند
* اطلاعات ایجادکننده
* اطلاعات تأییدکننده در صورت تأیید
* یک یا چند `DocumentLine`

ساختار کلی:

```text
JournalDocument
│
├── DocumentId
├── Title
├── IssueDate
├── DocumentType
├── Status
├── CreatedBy
├── CreatedAt
├── ApprovedBy
├── ApprovedAt
│
└── DocumentLine[]
```

---

# 2. Aggregate

`JournalDocument` یک **Aggregate Root** است.

`DocumentLine` یک Child داخل Aggregate است.

بنابراین `DocumentLine` به صورت مستقل از `JournalDocument` در Domain مدیریت نمی‌شود.

```text
JournalDocument (Aggregate Root)
        │
        └── DocumentLine[]
```

تمام عملیات مربوط به ایجاد، تغییر و حذف بندهای سند باید از طریق Aggregate انجام شوند تا Invariantهای سند نقض نشوند.

---

# 3. Document Header

Header شامل اطلاعات کلی سند است.

## Attributes

### DocumentId

شناسه یکتای سند.

---

### Title

عنوان سند.

---

### IssueDate

تاریخ صدور سند.

---

### DocumentType

نوع سند.

مقادیر:

```text
NORMAL
TEMPORARY
```

---

### Status

وضعیت فعلی سند.

مقادیر:

```text
DRAFT
ACCEPTED
REJECTED
```

---

### CreatedBy

شناسه کاربری که سند را ایجاد کرده است.

برای تمام اسناد الزامی است.

---

### CreatedAt

تاریخ و زمان ایجاد سند.

برای تمام اسناد الزامی است.

---

### ApprovedBy

شناسه کاربری که سند موقت را تأیید کرده است.

فقط بعد از Approval مقدار دارد.

---

### ApprovedAt

تاریخ و زمان تأیید سند.

فقط بعد از Approval مقدار دارد.

---

# 4. DocumentType

سیستم دارای دو نوع سند است:

```text
NORMAL
TEMPORARY
```

`DocumentType` مشخص می‌کند سند از چه نوعی است.

`DocumentType` و `Status` دو مفهوم مستقل هستند.

---

## 4.1 Normal Document

سند عادی نیازی به تأیید مدیر ندارد و از ابتدا به صورت پذیرفته‌شده ایجاد می‌شود:

```text
DocumentType = NORMAL
Status       = ACCEPTED
```

---

## 4.2 Temporary Document

سند موقت ابتدا به صورت Draft ایجاد می‌شود:

```text
DocumentType = TEMPORARY
Status       = DRAFT
```

سند موقت برای تبدیل شدن به سند عادی باید توسط کاربر مجاز/مدیر تأیید شود.

---

# 5. Document Status

وضعیت سند چرخه فعلی آن را مشخص می‌کند.

## DRAFT

سند ایجاد شده ولی هنوز تأیید نشده است.

مثال:

```text
TEMPORARY + DRAFT
```

---

## ACCEPTED

سند تأیید شده و معتبر است.

سند عادی از ابتدا در این وضعیت است:

```text
NORMAL + ACCEPTED
```

سند موقت نیز پس از تأیید مدیر به این وضعیت می‌رسد:

```text
TEMPORARY + DRAFT
        │
        │ Approve
        ▼
NORMAL + ACCEPTED
```

---

## REJECTED

سند موقت توسط مدیر رد شده است:

```text
TEMPORARY + DRAFT
        │
        │ Reject
        ▼
TEMPORARY + REJECTED
```

در صورت رد، نوع سند همچنان `TEMPORARY` باقی می‌ماند.

---

# 6. چرخه عمر سند

## Normal

```text
NORMAL + ACCEPTED
```

سند عادی از ابتدا Accepted است.

---

## Temporary

### ایجاد

```text
TEMPORARY + DRAFT
```

### تأیید

```text
TEMPORARY + DRAFT
        │
        │ Manager Approve
        ▼
NORMAL + ACCEPTED
```

### رد

```text
TEMPORARY + DRAFT
        │
        │ Manager Reject
        ▼
TEMPORARY + REJECTED
```

---

# 7. Approval

تأیید سند یک **Domain Behavior** است و نباید صرفاً با تغییر مستقیم `DocumentType` یا `Status` انجام شود.

Behavior:

```text
document.approve(user)
```

در هنگام تأیید:

```text
DocumentType = NORMAL
Status       = ACCEPTED
ApprovedBy   = user
ApprovedAt   = currentDateTime
```

---

# 8. Rejection

رد سند نیز یک Domain Behavior است.

Behavior:

```text
document.reject(user)
```

در صورت رد سند موقت:

```text
DocumentType = TEMPORARY
Status       = REJECTED
```

اطلاعات کاربر ردکننده و تاریخ رد در مدل فعلی به عنوان Rule قطعی تعریف نشده‌اند و در صورت نیاز باید به Domain اضافه شوند.

---

# 9. Document Lines

هر سند شامل `N` بند است.

حداقل تعداد بندها:

```text
2
```

و تعداد بندها می‌تواند بیشتر باشد:

```text
1..N
```

ساختار:

```text
JournalDocument
    │
    ├── DocumentLine 1
    ├── DocumentLine 2
    └── DocumentLine N
```

---

# 10. DocumentLine

هر بند سند شامل اطلاعات زیر است:

```text
DocumentLine
├── RowNumber
├── Type
├── Amount
├── AccountNumber
├── IssueDate
└── Description
```

---

## 10.1 RowNumber

`RowNumber` شماره ردیف بند در سند است.

مثال:

```text
RowNumber | Type
----------|--------
1         | DEBIT
2         | CREDIT
3         | CREDIT
```

`RowNumber` توسط `JournalDocument` مدیریت می‌شود و Client نباید بتواند ترتیب نامعتبر برای بندها ایجاد کند.

در محدوده یک سند، `RowNumber` باید یکتا باشد.

---

## 10.2 Type

نوع بند:

```text
DEBIT
CREDIT
```

---

## 10.3 Amount

مبلغ بند سند.

مبلغ باید معتبر و بزرگ‌تر از صفر باشد.

---

## 10.4 AccountNumber

شماره حسابی که بند سند روی آن ثبت می‌شود.

Account باید معتبر و قابل استفاده باشد.

حساب بسته‌شده نمی‌تواند در عملیات مالی جدید استفاده شود.

---

## 10.5 IssueDate

تاریخ مربوط به بند سند.

---

## 10.6 Description

توضیحات مربوط به بند سند.

---

# 11. DocumentLine و Parent

در Domain، `DocumentLine` نیازی به نگهداری `DocumentId` ندارد.

رابطه Parent/Child از طریق Aggregate مشخص است:

```text
JournalDocument
    └── DocumentLine[]
```

بنابراین مدل Domain:

```text
JournalDocument
├── DocumentId
└── DocumentLine
    ├── RowNumber
    ├── Type
    ├── Amount
    ├── AccountNumber
    ├── IssueDate
    └── Description
```

`DocumentLine` نباید در Domain به صورت مستقل دارای `DocumentId` باشد.

---

# 12. Persistence Relationship

در لایه Persistence، برای ارتباط جداول می‌توان `document_id` را در جدول بندها نگهداری کرد.

```text
journal_document
----------------
id
title
issue_date
document_type
status
created_by
created_at
approved_by
approved_at

document_line
-------------
document_id   ← Foreign Key
row_number
type
amount
account_number
issue_date
description
```

این `document_id` یک Concern مربوط به Persistence است و الزاماً بخشی از Domain Model `DocumentLine` نیست.

---

# 13. ترتیب بندها

اولین بند سند باید حتماً بدهکار باشد.

```text
Line 1 → DEBIT
```

بنابراین این حالت معتبر نیست:

```text
Line 1 → CREDIT
Line 2 → DEBIT
```

---

# 14. وجود بند بستانکار

هر سند باید حداقل یک بند بستانکار داشته باشد.

بنابراین حداقل ساختار سند:

```text
Line 1 → DEBIT
Line 2 → CREDIT
```

است.

---

# 15. حداقل تعداد بند

هر سند حداقل باید دو بند داشته باشد.

```text
Minimum Lines = 2
```

زیرا سند باید حداقل شامل:

```text
1 DEBIT
1 CREDIT
```

باشد.

---

# 16. Balance / تراز سند

سند باید همیشه Balanced باشد.

جمع مبالغ بدهکار و بستانکار باید برابر باشد:

```text
Total Debit = Total Credit
```

یا:

```text
Total Debit - Total Credit = 0
```

مثال معتبر:

```text
DEBIT     1,000
CREDIT      600
CREDIT      400

Total Debit  = 1,000
Total Credit = 1,000
```

مثال نامعتبر:

```text
DEBIT     1,000
CREDIT      900

Total Debit  = 1,000
Total Credit =   900
```

---

# 17. Account در DocumentLine

هر `DocumentLine` به یک Account مربوط است.

```text
DocumentLine
      │
      └── Account
```

Account باید:

* وجود داشته باشد.
* متعلق به یک سرفصل معتبر باشد.
* بسته نشده باشد.

Account بسته‌شده نباید در سند مالی جدید استفاده شود.

---

# 18. Created Information

هر سند باید اطلاعات ایجادکننده را نگهداری کند:

```text
CreatedBy
CreatedAt
```

این اطلاعات برای تمام اسناد الزامی است.

---

# 19. Approval Information

برای سند تأییدشده باید اطلاعات تأیید نگهداری شود:

```text
ApprovedBy
ApprovedAt
```

قبل از تأیید:

```text
ApprovedBy = null
ApprovedAt = null
```

بعد از تأیید:

```text
ApprovedBy != null
ApprovedAt != null
```

---

# 20. Domain Behaviors

Behaviorهای اصلی `JournalDocument`:

```text
createNormal()
createTemporary()

addLine()
removeLine()
changeLine()

approve()
reject()
```

تمام این Behaviorها باید Invariantهای Aggregate را حفظ کنند.

---

# 21. Domain Invariants

## INV-01 — Document Must Have Lines

سند بدون بند معتبر نیست.

---

## INV-02 — Minimum Two Lines

سند باید حداقل دو بند داشته باشد.

---

## INV-03 — First Line Must Be Debit

```text
Line[0].Type == DEBIT
```

---

## INV-04 — At Least One Credit

حداقل یک بند باید `CREDIT` باشد.

---

## INV-05 — Document Must Be Balanced

```text
Total Debit == Total Credit
```

---

## INV-06 — RowNumber Must Be Unique

در یک سند، دو بند نباید `RowNumber` یکسان داشته باشند.

---

## INV-07 — CreatedBy Is Required

هر سند باید دارای ایجادکننده باشد.

---

## INV-08 — CreatedAt Is Required

هر سند باید دارای تاریخ/زمان ایجاد باشد.

---

## INV-09 — Normal Document Is Accepted

سند عادی باید با وضعیت زیر ایجاد شود:

```text
NORMAL + ACCEPTED
```

---

## INV-10 — Temporary Document Starts as Draft

سند موقت باید با وضعیت زیر ایجاد شود:

```text
TEMPORARY + DRAFT
```

---

## INV-11 — Only Temporary Draft Can Be Approved

فقط سندی که:

```text
DocumentType = TEMPORARY
Status = DRAFT
```

باشد قابل تأیید است.

---

## INV-12 — Approval Requires Authorized User

تأیید سند موقت فقط توسط کاربر مجاز/مدیر انجام می‌شود.

---

## INV-13 — Approval Converts Temporary to Normal

پس از تأیید:

```text
TEMPORARY → NORMAL
```

---

## INV-14 — Approval Changes Status

پس از تأیید:

```text
DRAFT → ACCEPTED
```

---

## INV-15 — Approval Information Is Required

پس از تأیید:

```text
ApprovedBy != null
ApprovedAt != null
```

---

## INV-16 — Rejection Keeps Document Temporary

در صورت رد:

```text
TEMPORARY + DRAFT
        ↓
TEMPORARY + REJECTED
```

---

## INV-17 — Closed Account Cannot Be Used

Account بسته‌شده نباید در سند مالی جدید استفاده شود.

---

# 22. DocumentType vs Status

این دو مفهوم مستقل هستند.

### DocumentType

نوع سند:

```text
NORMAL
TEMPORARY
```

### Status

وضعیت سند:

```text
DRAFT
ACCEPTED
REJECTED
```

ترکیب‌های معتبر:

```text
NORMAL + ACCEPTED

TEMPORARY + DRAFT

TEMPORARY + REJECTED
```

و پس از تأیید:

```text
TEMPORARY + DRAFT
        ↓
NORMAL + ACCEPTED
```

ترکیب نامعتبر:

```text
NORMAL + DRAFT
```

زیرا سند عادی از ابتدا Accepted است.

---

# 23. Domain Responsibilities

`JournalDocument` مسئول است برای:

* حفظ Identity سند
* حفظ Header
* حفظ DocumentType
* حفظ Status
* حفظ CreatedBy و CreatedAt
* مدیریت Approval
* مدیریت Rejection
* نگهداری DocumentLineها
* مدیریت RowNumber
* جلوگیری از RowNumber تکراری
* اطمینان از وجود حداقل یک Debit
* اطمینان از وجود حداقل یک Credit
* اطمینان از اینکه بند اول Debit است
* اطمینان از Balanced بودن سند
* جلوگیری از Transitionهای نامعتبر

---

# 24. Domain Model Summary

```text
JournalDocument
│
├── DocumentId
├── Title
├── IssueDate
│
├── DocumentType
│      ├── NORMAL
│      └── TEMPORARY
│
├── Status
│      ├── DRAFT
│      ├── ACCEPTED
│      └── REJECTED
│
├── CreatedBy
├── CreatedAt
├── ApprovedBy
├── ApprovedAt
│
└── DocumentLine[]
       │
       ├── RowNumber
       ├── Type
       │    ├── DEBIT
       │    └── CREDIT
       ├── Amount
       ├── AccountNumber
       ├── IssueDate
       └── Description
```

---

# 25. خلاصه قواعد کسب‌وکار

1. `JournalDocument` یک Aggregate Root است.
2. `DocumentLine` Child این Aggregate است.
3. هر سند دارای Header و `N` بند است.
4. هر سند حداقل دو بند دارد.
5. بند اول حتماً بدهکار است.
6. هر سند حداقل یک بند بستانکار دارد.
7. مجموع بدهکار و بستانکار باید برابر باشد.
8. هر بند دارای `RowNumber` است.
9. `RowNumber` در محدوده یک سند باید یکتا باشد.
10. `DocumentLine` در Domain نیازی به `DocumentId` ندارد.
11. ارتباط Parent/Child در Domain توسط Aggregate مشخص می‌شود.
12. `document_id` می‌تواند در Persistence به عنوان Foreign Key نگهداری شود.
13. هر بند به یک Account مربوط است.
14. Account بسته‌شده نمی‌تواند در سند مالی جدید استفاده شود.
15. هر سند دارای `CreatedBy` و `CreatedAt` است.
16. سند دارای دو نوع `NORMAL` و `TEMPORARY` است.
17. سند عادی از ابتدا `NORMAL + ACCEPTED` است.
18. سند موقت از ابتدا `TEMPORARY + DRAFT` است.
19. سند موقت فقط توسط کاربر مجاز/مدیر قابل تأیید است.
20. با تأیید سند موقت، نوع سند `NORMAL` و وضعیت `ACCEPTED` می‌شود.
21. هنگام تأیید، `ApprovedBy` و `ApprovedAt` ثبت می‌شوند.
22. سند موقت می‌تواند توسط مدیر رد شود.
23. سند ردشده `TEMPORARY + REJECTED` باقی می‌ماند.
24. `DocumentType` و `Status` دو مفهوم مستقل هستند.
25. تغییرات `DocumentLine` باید از طریق `JournalDocument` انجام شود تا Invariantهای Aggregate حفظ شوند.
