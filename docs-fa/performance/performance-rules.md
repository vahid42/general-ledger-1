# Performance Rules — General Ledger

## 1. Purpose

این سند مجموعه‌ای از قواعد و الزامات Performance برای پروژه General Ledger است.

هدف این سند:

* جلوگیری از طراحی‌ها و پیاده‌سازی‌های دارای ریسک Performance
* تعریف قواعد قابل بررسی توسط انسان، تست‌های خودکار و AI Agent
* ایجاد مرجع مشخص برای بررسی Pull Requestها
* کمک به AI Performance Agent برای تشخیص مشکلات احتمالی Performance

این سند مکمل ADRها، Domain Analysisها و Business Rules است و جایگزین آن‌ها نیست.

---

# 2. Scope

این قواعد در بررسی موارد زیر اعمال می‌شوند:

* Domain Logic
* Application Services
* Repository Access
* Database Queries
* External Service Calls
* Collection Processing
* Transaction Boundaries
* Memory Usage
* Concurrency
* Batch Operations

---

# 3. Performance Principles

## P-001 — Avoid N+1 Queries

نباید برای پردازش مجموعه‌ای از داده‌ها، برای هر عنصر یک Query مستقل به Database اجرا شود، مگر اینکه این رفتار به‌صورت آگاهانه و با دلیل فنی پذیرفته شده باشد.

### Bad

```java
for (AccountHeading heading : headings) {
    repository.findByCode(heading.getCode());
}
```

### Preferred

```java
repository.findByCodes(codes);
```

### Agent Rule

Performance Agent باید الگوهای احتمالی N+1 را شناسایی کند و در صورت وجود، آن را گزارش کند.

---

# 4. Database Access

## P-002 — Avoid Unnecessary Database Calls

هر عملیات Database باید دارای دلیل مشخص باشد.

نباید یک داده چندین بار بدون نیاز واقعی از Database خوانده شود.

### Example

```java
Account account = repository.findById(id);

if (repository.existsById(id)) {
    ...
}
```

در صورتی که Query اول وجود Entity را مشخص کرده باشد، Query دوم ممکن است غیرضروری باشد.

### Agent Rule

Agent باید Queryهای تکراری یا غیرضروری را شناسایی کند.

---

## P-003 — Avoid Database Access Inside Large Loops

Database Access داخل Loopهای بزرگ باید با احتیاط انجام شود.

### Risk

```java
for (...) {
    repository.find(...);
}
```

تعداد Queryها می‌تواند متناسب با تعداد عناصر افزایش پیدا کند.

### Agent Rule

اگر تعداد عملیات Database به تعداد عناصر یک Collection وابسته باشد، Agent باید آن را به‌عنوان Performance Concern گزارش کند.

---

# 5. Batch Processing

## P-004 — Prefer Batch Operations

در پردازش تعداد زیادی Entity، در صورت امکان باید از عملیات Batch استفاده شود.

### Bad

```java
for (Account account : accounts) {
    repository.save(account);
}
```

### Preferred

```java
repository.saveAll(accounts);
```

البته استفاده از Batch نباید باعث نقض Business Rule یا Transaction Boundary شود.

### Agent Rule

Agent باید عملیات تکراری قابل تبدیل به Batch را شناسایی کند.

---

# 6. Collection Processing

## P-005 — Avoid Unnecessary Collection Materialization

نباید بدون نیاز واقعی مجموعه‌های بزرگ را به‌صورت کامل در Memory بارگذاری کرد.

### Risk

```java
List<Account> accounts = repository.findAll();
```

در صورتی که تعداد رکوردها می‌تواند بسیار زیاد باشد، این روش می‌تواند باعث مصرف بالای Memory شود.

### Agent Rule

در مواجهه با Queryهایی که ممکن است حجم زیادی داده برگردانند، Agent باید Memory Risk را بررسی کند.

---

## P-006 — Avoid Repeated Expensive Operations

عملیات محاسباتی پرهزینه نباید بدون نیاز چندین بار تکرار شوند.

### Bad

```java
for (...) {
    calculateExpensiveResult();
}
```

در صورتی که نتیجه مستقل از Iteration باشد، باید امکان محاسبه یک‌باره بررسی شود.

---

# 7. Remote Calls

## P-007 — Avoid Repeated Remote Calls

فراخوانی سرویس‌های خارجی داخل Loop باید با احتیاط انجام شود.

### Risk

```java
for (Account account : accounts) {
    remoteService.getDetails(account.getId());
}
```

این الگو می‌تواند باعث:

* افزایش Latency
* افزایش Network Traffic
* افزایش Load روی سرویس مقصد
* کاهش Throughput

شود.

### Agent Rule

Performance Agent باید Remote Callهای تکراری و وابسته به تعداد عناصر را شناسایی کند.

---

# 8. Transaction Management

## P-008 — Keep Transaction Boundaries Controlled

Transaction نباید بدون دلیل برای مدت طولانی باز بماند.

### Risks

Transactionهای طولانی می‌توانند باعث:

* افزایش Lock Duration
* کاهش Throughput
* افزایش Resource Consumption
* افزایش احتمال Deadlock

شوند.

### Agent Rule

Agent باید Transactionهای بسیار بزرگ یا Transactionهایی که شامل عملیات غیرضروری خارج از Database هستند را به‌عنوان Concern بررسی کند.

---

# 9. External I/O

## P-009 — Avoid Blocking I/O in Critical Paths

عملیات Blocking I/O در مسیرهای حساس Performance باید با دقت بررسی شوند.

مواردی مانند:

* File I/O
* Network I/O
* Remote API
* Database Access

نباید بدون دلیل در Critical Path قرار بگیرند.

---

# 10. Algorithmic Complexity

## P-010 — Avoid Unnecessary High Complexity

پیاده‌سازی‌هایی که Complexity بالاتری از نیاز واقعی دارند باید بررسی شوند.

### Example

```java
for (A a : listA) {
    for (B b : listB) {
        ...
    }
}
```

این الگو ممکن است Complexity برابر با:

```text
O(n × m)
```

داشته باشد.

در صورت امکان باید راهکارهایی مانند:

* Map
* Set
* Index
* Pre-computation

بررسی شوند.

### Agent Rule

Agent باید Loopهای تو در تو و عملیات Repeated Search را بررسی کند.

---

# 11. Memory Usage

## P-011 — Avoid Unnecessary Object Creation

نباید در مسیرهای پرتکرار، Objectهای غیرضروری به تعداد زیاد ایجاد شوند.

Agent باید مواردی مانند:

* ایجاد مکرر Collection
* ایجاد Objectهای موقت
* تبدیل‌های غیرضروری
* Serialization/Deserialization تکراری

را بررسی کند.

---

# 12. Serialization

## P-012 — Avoid Unnecessary Serialization

Serialization و Deserialization عملیات نسبتاً پرهزینه‌ای هستند.

نباید یک Object بدون نیاز واقعی چندین بار:

```text
Object
→ JSON
→ Object
→ JSON
```

تبدیل شود.

### Agent Rule

Performance Agent باید تبدیل‌های غیرضروری و تکراری را شناسایی کند.

---

# 13. Concurrency

## P-013 — Avoid Unnecessary Synchronization

Synchronization و Locking باید فقط در صورت نیاز استفاده شوند.

Lockهای غیرضروری یا بیش از حد گسترده می‌توانند باعث:

* کاهش Throughput
* افزایش Contention
* افزایش Latency

شوند.

---

# 14. Caching

## P-014 — Consider Caching for Stable, Frequently Accessed Data

برای داده‌هایی که:

* Read-heavy هستند
* تغییرات کمی دارند
* هزینه خواندن آن‌ها زیاد است

امکان استفاده از Cache باید بررسی شود.

### Important

Caching نباید بدون بررسی:

* Consistency
* Invalidation
* TTL
* Memory Cost

اضافه شود.

### Agent Rule

Agent نباید صرفاً به دلیل وجود Read زیاد پیشنهاد Cache بدهد؛ باید هزینه و Consistency نیز بررسی شود.

---

# 15. Performance Requirements

قواعد عمومی Performance باید از الزامات عددی Performance جدا باشند.

برای مثال:

```text
Maximum Response Time: 500 ms
Expected Throughput: 1000 requests/sec
Maximum Batch Size: 1000
Maximum Collection Size: 10000
```

الزامات عددی باید در بخش Performance Requirements پروژه ثبت شوند.

---

# 16. Performance Severity

Performance Agent باید Findings را به این شکل طبقه‌بندی کند.

## BLOCKER

مشکلی که احتمالاً می‌تواند باعث Failure جدی سیستم یا نقض یک Performance Requirement مشخص شود.

Example:

```text
Known N+1 query in a high-volume operation
```

---

## HIGH

مشکلی با احتمال قابل توجه برای افزایش شدید:

* Latency
* Database Load
* Memory Usage
* Network Traffic

---

## MEDIUM

مشکلی که ممکن است در Scale بالاتر مشکل‌ساز شود.

---

## LOW

بهبود یا Optimization پیشنهادی که Blocking نیست.

---

# 17. Evidence Requirement

Performance Agent نباید بدون شواهد کافی ادعای قطعی Performance Problem کند.

Agent باید بین این موارد تفاوت بگذارد:

```text
CONFIRMED
POTENTIAL
SUGGESTION
```

### CONFIRMED

زمانی که Rule یا Requirement مشخصی نقض شده باشد.

### POTENTIAL

زمانی که Code Pattern نشان‌دهنده ریسک باشد ولی اطلاعات Runtime یا حجم واقعی داده در دسترس نباشد.

### SUGGESTION

صرفاً یک Optimization پیشنهادی باشد.

---

# 18. AI Performance Review Rules

Performance Agent هنگام بررسی Pull Request باید:

1. فقط فایل‌های تغییرکرده را بررسی نکند؛ Context مرتبط را نیز در نظر بگیرد.
2. Performance Requirements پروژه را بررسی کند.
3. Performance Rules این سند را بررسی کند.
4. ADRهای مرتبط را بررسی کند.
5. Business Rules را در صورت تأثیر بر Performance در نظر بگیرد.
6. از ساختن Performance Requirement جدید خودداری کند.
7. در صورت نبود Evidence کافی، Finding را `POTENTIAL` اعلام کند.
8. Severity را مشخص کند.
9. دلیل Finding را توضیح دهد.
10. در صورت امکان راهکار اصلاحی ارائه دهد.

---

# 19. Finding Format

خروجی Performance Agent باید ساختاری مشابه زیر داشته باشد:

```text
Finding:
N+1 Database Query

Severity:
HIGH

Confidence:
HIGH

Status:
POTENTIAL

Location:
AccountHeadingService.java:42

Evidence:
repository.findByCode() is called inside a loop.

Risk:
Database calls increase linearly with the number
of AccountHeading records.

Suggested Action:
Replace individual lookups with a batch query.

Rule:
P-001
P-003
```

---

# 20. Important Principle

Performance Agent نباید صرفاً به دنبال سریع‌تر کردن Code باشد.

هدف Performance Review:

```text
Correctness
+
Scalability
+
Resource Efficiency
+
Predictable Performance
```

است.

Optimization نباید باعث نقض:

* Business Rules
* Domain Invariants
* Architecture Rules
* Data Consistency
* Transaction Semantics

شود.

---

# 21. Relationship with Other Agents

Performance Agent مستقل از سایر Agentها عمل می‌کند، اما نتیجه آن باید توسط Senior Reviewer در کنار سایر بررسی‌ها ارزیابی شود.

```text
Business Agent
        │
Architecture Agent
        │
Performance Agent
        │
Code Quality Agent
        │
        ▼
Senior Reviewer
```

Senior Reviewer تصمیم نهایی درباره:

```text
APPROVE
COMMENT
REQUEST_CHANGES
```

را اتخاذ می‌کند.

---

# 22. Rule Identifier Convention

تمام Performance Rules باید دارای شناسه یکتا باشند.

Format:

```text
P-XXX
```

Examples:

```text
P-001  Avoid N+1 Queries
P-002  Avoid Unnecessary Database Calls
P-003  Avoid Database Access Inside Large Loops
P-004  Prefer Batch Operations
P-005  Avoid Unnecessary Collection Materialization
P-006  Avoid Repeated Expensive Operations
P-007  Avoid Repeated Remote Calls
P-008  Keep Transaction Boundaries Controlled
P-009  Avoid Blocking I/O in Critical Paths
P-010  Avoid Unnecessary High Complexity
```

این IDها برای AI Agent مهم هستند، چون Agent می‌تواند در Review دقیقاً به Rule مربوطه Reference بدهد.

---

# 23. Source of Truth

Performance Agent باید منابع را با اولویت زیر در نظر بگیرد:

1. Explicit Performance Requirements
2. Approved ADRs
3. Project-specific Performance Rules
4. General Performance Rules
5. AI-generated Suggestions

AI-generated Suggestions به‌تنهایی نباید باعث `REQUEST_CHANGES` شوند.
