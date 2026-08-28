# سیاست بازبینی کد (AI Code Review Policy)

## 1. هدف

این سند قوانین و Workflow مورد استفاده AI Agent برای Code Review را مشخص می‌کند.

هدف Review این است که مشخص شود تغییر انجام‌شده:

* صحیح است.
* با Architecture پروژه سازگار است.
* با Domain Model سازگار است.
* قوانین کسب‌وکار را نقض نمی‌کند.
* رفتار ناخواسته ایجاد نمی‌کند.
* قابل نگهداری است.
* تست مناسب دارد.

Review باید بر اساس مستندات پروژه انجام شود، نه بر اساس سلیقه شخصی Reviewer.

---

# 2. محدوده Review

در صورت مرتبط بودن، موارد زیر باید بررسی شوند:

1. Architecture
2. Module Boundary
3. Domain Model
4. Business Rules
5. Application Behavior
6. Infrastructure
7. Presentation / API
8. Persistence
9. Error Handling
10. Security
11. Performance
12. Transaction
13. Concurrency
14. Tests
15. Maintainability

هر بخش فقط زمانی باید Finding تولید کند که واقعاً مشکلی وجود داشته باشد.

---

# 3. Workflow بازبینی

Review باید طبق ترتیب زیر انجام شود:

```text
START
  │
  ▼
خواندن System Context
  │
  ▼
خواندن مستندات مرتبط
  │
  ▼
درک تغییرات
  │
  ▼
تشخیص Module / Bounded Context
  │
  ▼
بررسی Architecture
  │
  ▼
بررسی Domain
  │
  ▼
بررسی Business Rules
  │
  ▼
بررسی Application
  │
  ▼
بررسی Infrastructure
  │
  ▼
بررسی Presentation
  │
  ▼
بررسی Tests
  │
  ▼
بررسی Security و Performance در صورت ارتباط
  │
  ▼
طبقه‌بندی Findings
  │
  ▼
تعیین Status
  │
  ▼
تولید گزارش Review
  │
  ▼
END
```

---

# 4. مرحله اول — درک تغییرات

قبل از بررسی جزئیات Code باید مشخص شود:

* هدف تغییر چیست؟
* چه مشکلی را حل می‌کند؟
* کدام Module تغییر کرده است؟
* کدام Layer تغییر کرده است؟
* کدام Domain Concept تحت تأثیر قرار گرفته است؟
* چه Behaviorای اضافه، حذف یا تغییر کرده است؟

Agent نباید صرفاً چند خط تغییرکرده را بدون درک Context بررسی کند.

---

# 5. بررسی Architecture

Agent باید بررسی کند که تغییر جدید مرزهای معماری را نقض نکند.

موارد مهم:

* وابستگی Domain به Infrastructure
* وابستگی Domain به Presentation
* وابستگی نامناسب به Framework
* قرار گرفتن Business Logic در Presentation
* قرار گرفتن Business Decision در Infrastructure
* تبدیل Application Service به محل اصلی Business Logic
* ایجاد Dependency نامناسب بین Moduleها
* نقض Bounded Context Boundary

نقض جدی Architecture معمولاً باید با Severity بالا گزارش شود.

---

# 6. بررسی Domain

اگر تغییر مربوط به Domain است، موارد زیر بررسی شوند:

* Entity
* Value Object
* Aggregate
* Aggregate Root
* Domain Service
* Domain Policy
* Domain Event
* Repository Abstraction
* Invariant
* Domain Behavior

Agent باید این سؤال را مطرح کند:

> این Behavior متعلق به Entity، Aggregate، Domain Service، Policy یا Application است؟

Business Behavior نباید صرفاً به دلیل راحت‌تر بودن Implementation در Application قرار گیرد.

---

# 7. بررسی Business Rule

Implementation باید با مستندات Domain و Use Caseهای مرتبط مقایسه شود.

Agent باید موارد زیر را بررسی کند:

* آیا Ruleهای مستندشده رعایت شده‌اند؟
* آیا Ruleای جا افتاده است؟
* آیا Behavior با Ruleها تناقض دارد؟
* آیا Rule در Layer اشتباه قرار گرفته است؟
* آیا Implementation یک فرض مستندنشده ایجاد کرده است؟

Agent نباید برای توجیه Implementation، Business Rule جدید اختراع کند.

---

# 8. بررسی Application

Application Service باید وظیفه Orchestration داشته باشد.

موارد قابل انتظار:

* دریافت Request مربوط به Use Case
* Load کردن Domain Objectهای موردنیاز
* فراخوانی Domain Behavior
* هماهنگی Repositoryها
* هماهنگی External Dependencyها
* مدیریت جریان اجرای Use Case
* برگرداندن Result مناسب

Application Service نباید به محل انباشته شدن Business Ruleها تبدیل شود.

---

# 9. بررسی Infrastructure

موارد زیر بررسی شوند:

* Repository Implementation
* Database Access
* External Service
* Messaging
* Serialization
* Transaction
* Configuration
* Framework Integration

Infrastructure باید جزئیات فنی را پیاده‌سازی کند و نباید Business Rule اصلی را مالک شود.

---

# 10. بررسی Presentation

موارد زیر بررسی شوند:

* Request Validation
* Response Structure
* DTO Boundary
* Mapping
* HTTP Semantics
* Error Handling
* Authentication
* Authorization

Controller باید Thin باشد.

Core Business Logic نباید داخل Controller قرار بگیرد.

---

# 11. بررسی Test

برای هر تغییر رفتاری بررسی شود که Test مناسب وجود داشته باشد.

موارد قابل بررسی:

* Domain Unit Test
* Application Test
* Integration Test
* API Test
* Regression Test

نبود Test فقط زمانی Finding محسوب شود که تغییر واقعاً به Test نیاز داشته باشد.

---

# 12. Severity

هر Finding باید یکی از Severityهای زیر را داشته باشد.

## BLOCKER

تغییر نباید پذیرفته شود.

نمونه‌ها:

* Data Corruption
* نقض جدی Core Invariant
* مشکل امنیتی بسیار جدی
* نقض شدید Architecture
* Business Behavior کاملاً اشتباه
* نقض صریح Requirement

---

## CRITICAL

مشکل بسیار جدی که احتمال بالایی برای ایجاد Failure یا رفتار نادرست در Production دارد.

---

## MAJOR

مشکل مهمی که معمولاً باید قبل از Acceptance اصلاح شود.

نمونه‌ها:

* نقض مهم Business Rule
* مشکل مهم معماری
* Validation مهم که وجود ندارد
* Transaction اشتباه

---

## MINOR

مشکل واقعی اما با اثر محدود.

نمونه‌ها:

* مشکل محدود Maintainability
* ناسازگاری جزئی با Design
* Defensive Handling ناقص با اثر محدود

---

## SUGGESTION

پیشنهاد اختیاری برای بهبود.

Suggestion نباید به عنوان Defect یا الزام معرفی شود.

---

# 13. ساختار Finding

هر Finding باید شامل موارد زیر باشد:

```text
Severity
Location
Problem
Why it matters
Recommended action
```

مثال:

```text
[MAJOR]

Location:
AccountHeadingService.java:42

Problem:
Business validation داخل Application Service قرار گرفته است.

Why it matters:
این Validation یک Domain Invariant است و ممکن است سایر مسیرهای
دسترسی به Domain بتوانند آن را دور بزنند.

Recommended action:
Invariant را به AccountHeading یا Domain Service مناسب منتقل کنید.
```

---

# 14. Evidence

هر Finding باید دارای Evidence قابل بررسی باشد.

Evidence می‌تواند شامل موارد زیر باشد:

* Code تغییرکرده
* Code موجود
* Domain Documentation
* Architecture Documentation
* Use Case
* Test

Agent نباید Findingهای صرفاً حدسی ایجاد کند.

اگر Evidence کافی وجود ندارد، باید عدم قطعیت را صریح اعلام کند.

---

# 15. جلوگیری از False Positive

Agent باید Findingهای کمتر اما با اطمینان بالا تولید کند.

موارد زیر به‌تنهایی Finding محسوب نمی‌شوند:

* وجود یک روش پیاده‌سازی متفاوت
* تفاوت در Coding Style شخصی
* امکان Refactoring تئوریک
* استفاده نکردن از یک Pattern صرفاً به دلیل سلیقه Reviewer
* پیشنهاد Abstraction جدید بدون نیاز واقعی

Finding باید یک Risk، Defect، Violation یا مغایرت مستندشده را نشان دهد.

---

# 16. تعیین نتیجه نهایی Review

در پایان Review یکی از Statusهای زیر انتخاب شود.

## ACCEPTED

زمانی که:

* هیچ BLOCKER وجود ندارد.
* هیچ CRITICAL یا MAJOR حل‌نشده‌ای وجود ندارد.
* تغییر با Architecture و Business Rules سازگار است.

---

## CHANGES_REQUESTED

زمانی که حداقل یکی از موارد زیر وجود داشته باشد:

* BLOCKER
* CRITICAL
* MAJOR
* نقض مهم یک Rule مستندشده

---

## ACCEPTED_WITH_SUGGESTIONS

زمانی که:

* مشکل جدی وجود ندارد.
* فقط MINOR یا SUGGESTION وجود دارد.

---

# 17. ساختار گزارش نهایی

گزارش Review باید ساختاری مشابه زیر داشته باشد:

```text
# Review Result

Status: ACCEPTED | CHANGES_REQUESTED | ACCEPTED_WITH_SUGGESTIONS

## Findings

### [SEVERITY] عنوان Finding

Location:
...

Problem:
...

Why it matters:
...

Recommended action:
...

## Summary

- Blockers: N
- Critical: N
- Major: N
- Minor: N
- Suggestions: N
```

---

# 18. قوانین Workflow

Agent نباید مستقیماً از Code تغییرکرده به نتیجه نهایی برسد.

حداقل Workflow باید این باشد:

```text
Context
→ Documentation
→ Change
→ Architecture
→ Domain
→ Business Rules
→ Application
→ Infrastructure
→ Presentation
→ Tests
→ Findings
→ Decision
```

اگر مرحله‌ای واقعاً مرتبط نیست، می‌توان آن را به صورت زیر مشخص کرد:

```text
Not Applicable
```

اما مرحله‌ای که می‌تواند روی Correctness اثر بگذارد نباید بدون بررسی نادیده گرفته شود.

---

# 19. تعارض بین مستندات و Implementation

اگر Implementation با Documentation مغایرت داشت:

1. تعارض را شناسایی کن.
2. منبع معتبرتر را طبق Source-of-Truth مشخص کن.
3. تعارض را به عنوان Finding گزارش کن.
4. صرفاً بر اساس Implementation، Rule جدید ایجاد نکن.

اگر دو سند معتبر با یکدیگر تعارض داشتند، ابهام را گزارش کن و بدون مبنای کافی تصمیم‌گیری نکن.

---

# 20. اصل نهایی

> تغییرات را بر اساس Intent مستندشده سیستم بررسی کن، نه بر اساس سلیقه شخصی Reviewer.

هدف اصلی Review عبارت است از:

* حفظ Correctness
* حفظ Architecture
* حفظ Domain Integrity
* حفظ Business Rules
* کاهش Risk
* جلوگیری از Regression

Review باید به بهبود کیفیت سیستم کمک کند، نه اینکه صرفاً تعداد زیادی Comment تولید کند.
