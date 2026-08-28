بله. یک دور کامل Review کردم و چند مورد را اصلاح کردم:

* با معماری نهایی **سه Agent** هماهنگ شد.
* نقش `docs/` و `.ai/` کاملاً از هم جدا شد.
* `agent-config.yaml` به‌عنوان Configuration سه Agent معرفی شد.
* Workflow با ساختاری که برای سه Agent نهایی کردیم هماهنگ شد.
* تناقض `WARNING / HIGH / BLOCKER` برطرف شد و تصمیم نهایی به `review-policy.md` سپرده شد.
* Performance Rules به `docs/performance/` منتقل شد.
* توضیحات اضافه و تکراری حذف شد.
* این فایل را به‌عنوان **README داخل `.ai/`** در نظر گرفتم.

نسخه نهایی آماده Copy:

# AI Project Guidance

این پوشه (`.ai/`) محل نگهداری **Context، Policy و Configuration مورد نیاز AI Agentها** برای کار روی پروژه است.

`.ai/` محل نگهداری دانش اصلی پروژه نیست.

**Source of Truth پروژه در `docs/` قرار دارد.**

بنابراین اصل اصلی این ساختار:

```text
docs/
  ↓
Source of Truth
  ↓
Project Knowledge
  ↓
.ai/
  ↓
AI Context + Review Policy + Agent Configuration
  ↓
AI Agents
```

---

# 1. ساختار `.ai`

ساختار نهایی:

```text
.ai/
│
├── system-context.md
├── review-policy.md
└── agent-config.yaml
```

هر فایل مسئولیت مشخصی دارد.

---

# 2. `system-context.md`

## مسئولیت

این فایل **Context کلی پروژه را برای AI** تعریف می‌کند.

AI با خواندن این فایل باید بتواند بفهمد:

* پروژه چیست؟
* هدف سیستم چیست؟
* معماری کلی چیست؟
* رویکرد طراحی چیست؟
* ساختار ماژول‌ها چگونه است؟
* Source of Truth پروژه کجاست؟
* برای هر نوع تحلیل باید به کدام بخش `docs/` مراجعه کند؟

### سؤال اصلی

> AI باید این Repository را چگونه درک کند؟

---

## اصل مهم

`system-context.md` نباید محتوای `docs/` را Duplicate کند.

این فایل فقط Context و مسیر منابع را مشخص می‌کند.

دانش واقعی پروژه باید در `docs/` باقی بماند.

---

# 3. ساختار مستندات پروژه

ساختار کلی مستندات:

```text
docs/
│
├── analysis/
│   ├── domain/
│   ├── account/
│   ├── account-heading/
│   └── journal/
│
├── architecture/
│   ├── ADR-001...
│   ├── DDD-Strategic-And-Tactical.md
│   └── domain-layer-structure.md
│
└── performance/
    └── performance-rules.md
```

بنابراین:

```text
Domain Knowledge
    → docs/analysis/

Architecture Knowledge
    → docs/architecture/

Performance Knowledge
    → docs/performance/
```

---

# 4. `review-policy.md`

## مسئولیت

این فایل مشخص می‌کند:

> AI چگونه باید یک Pull Request را Review کند؟

این فایل **Business Rule، Architecture Decision یا Performance Rule نیست**.

Business Ruleها، Architecture Decisionها و Performance Ruleها در `docs/` قرار دارند.

`review-policy.md` فقط رفتار و فرآیند Reviewer را مشخص می‌کند.

---

## اصول Review

AI باید:

1. تغییرات PR را شناسایی کند.
2. Scope تغییر را مشخص کند.
3. Module و Domain تحت تأثیر را مشخص کند.
4. مستندات مرتبط را از `docs/` پیدا کند.
5. تغییرات Code را با مستندات مقایسه کند.
6. برای هر Finding، Evidence ارائه کند.
7. بین Violation قطعی و Risk احتمالی تفاوت قائل شود.
8. Business Rule جدید اختراع نکند.
9. Findingهای کم‌اطمینان و صرفاً سلیقه‌ای ایجاد نکند.
10. در نهایت طبق Review Policy نتیجه Review را تعیین کند.

---

# 5. Evidence Rule

AI نباید بدون Evidence یک Finding مهم ایجاد کند.

هر Finding مهم باید بتواند مشخص کند:

```text
Rule
  ↓
Evidence
  ↓
Reason
  ↓
Result
```

مثال:

```text
Rule:

Domain must remain independent from Infrastructure.

Evidence:

AccountHeading.java imports an Infrastructure class.

Reason:

The Domain layer now directly depends on Infrastructure.

Result:

REQUEST_CHANGES
```

---

# 6. عدم اختراع Rule

یکی از مهم‌ترین قوانین Reviewer:

```text
AI must not invent business rules.
```

اگر Rule مشخصی در مستندات وجود نداشته باشد، AI نباید بر اساس حدس آن را به عنوان Violation اعلام کند.

در چنین شرایطی باید عدم قطعیت را مشخص کند.

مثلاً:

```text
Status:

UNCERTAIN

Reason:

No documented business rule was found
that confirms this behavior is invalid.
```

---

# 7. سه AI Agent

سیستم Review دارای سه Agent تخصصی است:

```text
Architecture Agent
Domain Agent
Performance Agent
```

هر Agent مسئول حوزه مشخصی است و از مستندات مربوط به همان حوزه استفاده می‌کند.

---

# 8. Architecture Agent

## مسئولیت

بررسی صحت معماری و مرزهای ساختاری سیستم.

### Source

```text
docs/architecture/
```

### وظایف

```text
- Check module boundaries
- Check layer dependencies
- Check dependency direction
- Check DDD structure
- Check architectural decisions
- Check ADR compliance
- Check bounded context boundaries
- Check infrastructure dependencies
```

Architecture Agent باید بررسی کند که تغییر جدید با Architecture مستندشده پروژه سازگار باشد.

---

# 9. Domain Agent

## مسئولیت

بررسی Domain Model و Business Rules.

### Source

```text
docs/analysis/
```

### وظایف

```text
- Identify affected Domain
- Read relevant Domain Analysis
- Check Entities
- Check Value Objects
- Check Aggregates
- Check Aggregate boundaries
- Check Domain Services
- Check Domain Policies
- Check Invariants
- Check Behaviors
- Check Business Rules
- Check Use Cases
```

Domain Agent نباید Business Rule جدیدی ایجاد کند.

Business Rule باید از مستندات Domain استخراج شود.

---

# 10. Performance Agent

## مسئولیت

بررسی Performance و Scalability بر اساس قوانین و الزامات مستندشده پروژه.

### Source

```text
docs/performance/
```

مهم‌ترین سند Performance:

```text
docs/performance/performance-rules.md
```

### وظایف

```text
- Check documented performance requirements
- Check performance rules
- Identify N+1 queries
- Check database access patterns
- Check batch operations
- Check collection processing
- Check repeated remote calls
- Check transaction boundaries
- Check memory usage
- Check algorithmic complexity
- Check serialization
- Check concurrency
- Check caching risks
```

Performance Agent نباید Performance Requirement جدیدی از خودش ایجاد کند.

در صورت نبود Evidence کافی، Finding باید به عنوان Risk احتمالی گزارش شود.

---

# 11. `agent-config.yaml`

## مسئولیت

این فایل Configuration مربوط به سه Agent است.

این فایل مشخص می‌کند:

* چه Agentهایی فعال هستند.
* هر Agent چه Sourceهایی دارد.
* هر Agent چه مواردی را بررسی می‌کند.
* Workflow اجرای Agentها چگونه است.
* خروجی Agentها چگونه جمع‌آوری می‌شود.

`agent-config.yaml` محل تعریف Business Rule یا Architecture Rule نیست.

---

# 12. Configuration سه Agent

ساختار منطقی Configuration:

```text
agent-config.yaml
        │
        ├── Architecture Agent
        │       └── docs/architecture/
        │
        ├── Domain Agent
        │       └── docs/analysis/
        │
        └── Performance Agent
                └── docs/performance/
```

بنابراین یک فایل Configuration داریم، اما **سه Agent مستقل** داریم.

---

# 13. AI Review Workflow

Workflow اصلی AI Review:

```text
                    Pull Request
                         │
                         ▼
                  Changed Files
                         │
                         ▼
                  Scope Detection
                         │
                         ▼
                agent-config.yaml
                         │
                         ▼
              Relevant Documentation
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
   docs/analysis/   docs/architecture/   docs/performance/
          │              │              │
          ▼              ▼              ▼
    Domain Agent   Architecture Agent  Performance Agent
          │              │              │
          └──────────────┼──────────────┘
                         │
                         ▼
                  Collect Findings
                         │
                         ▼
                Remove Duplicates
                         │
                         ▼
                review-policy.md
                         │
                         ▼
                  Final Decision
                         │
                         ▼
                    PR Review
```

---

# 14. Workflow مرحله به مرحله

## Step 1 — دریافت Pull Request

Workflow با یک Pull Request شروع می‌شود:

```text
PR
 ↓
Changed Files
```

AI ابتدا باید Scope تغییر را مشخص کند.

---

## Step 2 — تشخیص Scope

مثلاً:

```text
ledger-domain/
└── accountheading/
    └── AccountHeading.java
```

AI می‌تواند تشخیص دهد:

```text
Affected Area:
AccountHeading

Layer:
Domain
```

---

## Step 3 — خواندن Configuration

Workflow فایل زیر را می‌خواند:

```text
.ai/agent-config.yaml
```

از این فایل مشخص می‌شود که:

```text
Architecture Agent
Domain Agent
Performance Agent
```

فعال هستند و هرکدام چه منابعی دارند.

---

## Step 4 — Context Loading

AI فقط مستندات مرتبط را Load می‌کند.

برای تغییر مربوط به `AccountHeading`:

```text
docs/analysis/account-heading/
```

برای Architecture:

```text
docs/architecture/
```

برای Performance:

```text
docs/performance/
```

هدف این است که Agent تمام `docs/` را بدون نیاز دریافت نکند.

---

# 15. اجرای Agentها

Agentها بررسی تخصصی خود را انجام می‌دهند:

```text
                    Changed Code
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
       Domain       Architecture    Performance
        Agent           Agent          Agent
          │              │              │
          └──────────────┼──────────────┘
                         │
                         ▼
                      Findings
```

هر Agent فقط در محدوده مسئولیت خودش Finding تولید می‌کند.

---

# 16. جمع‌آوری Findings

بعد از اجرای Agentها، Findings جمع‌آوری می‌شوند.

مثلاً:

```text
Domain Agent
    ↓
1 Finding

Architecture Agent
    ↓
0 Findings

Performance Agent
    ↓
1 Finding
```

سپس:

```text
All Findings
```

ایجاد می‌شود.

Findingهای تکراری باید قبل از نتیجه نهایی شناسایی و ادغام شوند.

---

# 17. اعمال Review Policy

پس از جمع‌آوری Findings، `review-policy.md` روی نتایج اعمال می‌شود.

مثلاً:

```text
Business Invariant Violation
        ↓
BLOCKER / CRITICAL
        ↓
CHANGES_REQUESTED
```

یا:

```text
Potential Performance Risk
        ↓
Appropriate Severity
        ↓
Comment / Finding
```

یا:

```text
Optional Improvement
        ↓
SUGGESTION
        ↓
COMMENT
```

Severity و Status نهایی باید مطابق `review-policy.md` تعیین شوند.

---

# 18. Context باید حداقلی و مرتبط باشد

AI نباید در هر PR تمام `docs/` را دریافت کند.

Context باید بر اساس Scope تغییر انتخاب شود.

مثلاً:

```text
AccountHeading Change
        ↓
Relevant Domain Documentation
        +
Relevant Architecture Documentation
        +
Relevant Performance Rules
```

به جای:

```text
Entire docs/
```

این کار باعث می‌شود:

* Context کوچک‌تر باشد.
* Token Cost کاهش پیدا کند.
* دقت Agent افزایش پیدا کند.
* تحلیل نامرتبط کاهش پیدا کند.

---

# 19. مرز مسئولیت `docs` و `.ai`

این مرز باید همیشه حفظ شود.

```text
┌───────────────────────────────────────┐
│                docs/                  │
│                                       │
│           Source of Truth             │
│                                       │
│  Business Rules                       │
│  Domain Analysis                      │
│  Use Cases                            │
│  Architecture Decisions               │
│  ADRs                                 │
│  Performance Rules                    │
└───────────────────┬───────────────────┘
                    │
                    │ Context
                    ▼
┌───────────────────────────────────────┐
│                .ai/                   │
│                                       │
│           AI Configuration            │
│                                       │
│  system-context.md                    │
│  review-policy.md                     │
│  agent-config.yaml                    │
└───────────────────┬───────────────────┘
                    │
                    ▼
                AI Agents
                    │
                    ▼
                PR Review
```

---

# 20. اصل طلایی

```text
docs/
    = What the system knows

.ai/
    = How AI should use that knowledge

system-context.md
    = How AI understands the system

review-policy.md
    = How AI performs and evaluates Review

agent-config.yaml
    = Which Agents run and which sources they use
```

---

# 21. قانون نگهداری

اگر یک **Business Rule** تغییر کرد:

```text
Update docs/analysis/
```

اگر یک **Use Case** تغییر کرد:

```text
Update docs/analysis/
```

اگر یک **Architecture Decision** تغییر کرد:

```text
Update docs/architecture/
```

اگر یک **Performance Rule** تغییر کرد:

```text
Update docs/performance/
```

اگر نحوه رفتار AI Reviewer تغییر کرد:

```text
Update .ai/review-policy.md
```

اگر Context کلی سیستم تغییر کرد:

```text
Update .ai/system-context.md
```

اگر Agentها، Sourceها یا Workflow آن‌ها تغییر کرد:

```text
Update .ai/agent-config.yaml
```

---

# 22. توسعه‌پذیری

این معماری امکان اضافه شدن Agentهای جدید را بدون تغییر Source of Truth فراهم می‌کند.

برای مثال در آینده می‌توان Agentهای دیگری اضافه کرد:

```text
Security Agent
Testing Agent
API Agent
Database Agent
```

بدون اینکه Business Rules یا Architecture Documentation موجود جابه‌جا شوند.

Agent جدید فقط باید:

1. مسئولیت مشخص داشته باشد.
2. Source مشخص داشته باشد.
3. در `agent-config.yaml` تعریف شود.
4. در Workflow Review قرار گیرد.
5. از `review-policy.md` پیروی کند.

---

# 23. ساختار نهایی Repository

```text
Repository
│
├── docs/
│   │
│   ├── analysis/
│   │   ├── domain/
│   │   ├── account/
│   │   ├── account-heading/
│   │   └── journal/
│   │
│   ├── architecture/
│   │   ├── ADR-001...
│   │   ├── DDD-Strategic-And-Tactical.md
│   │   └── domain-layer-structure.md
│   │
│   └── performance/
│       └── performance-rules.md
│
└── .ai/
    ├── system-context.md
    ├── review-policy.md
    └── agent-config.yaml
```

---

# 24. نتیجه نهایی

این ساختار یک تفکیک روشن بین **دانش پروژه** و **تنظیمات AI** ایجاد می‌کند:

> **`docs/` حافظه و Source of Truth پروژه است.**

> **`.ai/` قرارداد، Context، Policy و Configuration مربوط به AI است.**

و سه Agent تخصصی از همین ساختار استفاده می‌کنند:

```text
                 agent-config.yaml
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
    Architecture       Domain       Performance
       Agent            Agent          Agent
          │              │              │
          ▼              ▼              ▼
docs/architecture/  docs/analysis/  docs/performance/
          │              │              │
          └──────────────┼──────────────┘
                         ▼
                    AI Review
                         │
                         ▼
                 Review Result
```

Workflow نهایی:

```text
PR
 ↓
Changed Files
 ↓
Scope Detection
 ↓
agent-config.yaml
 ↓
Relevant docs/
 ↓
Architecture Agent
Domain Agent
Performance Agent
 ↓
Collect Findings
 ↓
review-policy.md
 ↓
Final Review Result
```

این ساختار پایه‌ای است که می‌تواند بعداً بدون تغییر در Source of Truth پروژه، Agentهای تخصصی بیشتری را نیز پشتیبانی کند.
