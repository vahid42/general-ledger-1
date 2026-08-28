# AI Senior Reviewer — Architecture & Roadmap Reference

> **Purpose**
>
> این سند مرجع معماری، طراحی اولیه و نقشه راه پروژه **AI Senior Reviewer / Architecture Guardian** است.

---

# 1. ایده اصلی

هدف پروژه ساخت یک سیستم **AI-Powered Senior Code Reviewer / Architecture Guardian** است که Pull Requestهای یک Repository را به‌صورت خودکار بررسی کند.

تمرکز سیستم فقط روی پیدا کردن Bug یا اجرای Unit Test نیست؛ بلکه می‌خواهد بخشی از وظایف یک Senior Developer / Software Architect را در Review انجام دهد.

سیستم باید بتواند بررسی کند که تغییرات یک Pull Request با موارد زیر سازگار هستند یا خیر:

* Architecture
* DDD
* Domain Rules
* Business Invariants
* Aggregate Boundaries
* Domain Service / Policy
* ADRها
* Performance Rules
* Dependency Direction
* Repository-specific Engineering Rules

### ایده کلیدی

> **Unit Tests verify the code.
> AI Guardian verifies the engineering decisions.**

---

# 2. مسئله‌ای که حل می‌شود

Unit Testها برای بسیاری از مسائل مناسب هستند، اما نمی‌توانند همه تصمیمات معماری و طراحی را بررسی کنند.

برای مثال:

* آیا یک Policy باید داخل Aggregate باشد یا Domain Service؟
* آیا Aggregate مجاز است مستقیماً به Repository وابسته باشد؟
* آیا Dependency Direction رعایت شده؟
* آیا یک Business Rule در لایه مناسب قرار گرفته؟
* آیا یک تغییر با ADRهای پروژه مغایرت دارد؟
* آیا طراحی جدید باعث N+1 Query شده؟
* آیا یک تصمیم طراحی با Performance Requirements پروژه مغایرت دارد؟

این مسائل معمولاً نیازمند قضاوت یک Senior Engineer هستند.

هدف پروژه این است که AI بتواند با استفاده از **دانش و تصمیمات ثبت‌شده پروژه** بخشی از این Review را انجام دهد.

---

# 3. تفاوت با AI Code Review معمولی

هدف این پروژه صرفاً این نیست:

```text
AI
 ↓
Read Code
 ↓
Find Bug
```

بلکه:

```text
Pull Request
      ↓
Changed Code
      ↓
Repository Knowledge
      ↓
Architecture / Domain / Performance Rules
      ↓
ADR
      ↓
Specialized AI Agents
      ↓
Senior Review
```

بنابراین سیستم **Architecture-Aware** و **Repository-Aware** است.

AI نباید صرفاً بر اساس دانش عمومی خودش تصمیم بگیرد؛ بلکه باید تصمیمات پروژه را نیز در نظر بگیرد.

---

# 4. Repository Knowledge

برای اینکه Agent بتواند Repository را درست درک کند، پروژه دارای دو نوع اطلاعات است:

## 4.1. `.ai/`

این قسمت شامل **قرارداد، رفتار و تنظیمات AI Agentها** است.

ساختار:

```text
.ai/
├── system-context.md
├── review-policy.md
└── agent-config.yaml
```

`.ai` می‌تواند شامل موارد زیر باشد:

* Context پروژه
* Review Policy
* Agent Configuration
* System Prompts
* Coding Guidelines

> `.ai` = **AI Contract & Configuration**
>
> `.ai` مشخص می‌کند **AI چگونه باید کد را Review کند.**

---

## 4.2. `docs/`

این قسمت شامل **دانش واقعی و Source of Truth پروژه** است.

مثلاً:

```text
docs/
├── analysis/
├── architecture/
└── performance/
```

موارد موجود در `docs` می‌توانند شامل موارد زیر باشند:

* ADRs
* Domain Analysis
* Architecture Documents
* Performance Guidelines
* Coding Standards
* Business Knowledge

> `docs/` = **Project Knowledge / Source of Truth**

---

# 5. `.ai/system-context.md`

این فایل Context کلی پروژه را برای Agent تعریف می‌کند.

مواردی که باید در آن مشخص شوند:

* پروژه چیست؟
* هدف سیستم چیست؟
* معماری کلی چیست؟
* ساختار Moduleها چیست؟
* DDD چه نقشی دارد؟
* Clean / Onion Architecture چگونه استفاده شده؟
* Bounded Contextها کجا هستند؟
* اسناد مرجع کجا قرار دارند؟
* Agent برای تحلیل هر موضوع باید به کدام منابع مراجعه کند؟
* اولویت منابع هنگام وجود تضاد چیست؟

این فایل شامل Business Ruleهای جزئی نیست.

مثلاً قوانین AccountHeading باید در `docs/analysis/` باقی بمانند.

---

# 6. `.ai/review-policy.md`

این فایل مشخص می‌کند Agent **چگونه Review انجام دهد**.

مثلاً:

```text
Architecture
    ↓
Domain Rules
    ↓
Application
    ↓
Infrastructure
    ↓
Tests
```

همچنین مشخص می‌کند:

* چه چیزی Blocker است؟
* چه چیزی Warning است؟
* چه چیزی Suggestion است؟
* چه چیزی باعث `REQUEST_CHANGES` می‌شود؟
* چه چیزی فقط Comment است؟
* Agent چگونه Finding را گزارش کند؟
* Agent چگونه Evidence ارائه کند؟
* Agent چه زمانی اجازه دارد یک Rule را Violation اعلام کند؟
* Agent چگونه از False Positive جلوگیری کند؟

یک قانون مهم:

> Agent نباید Business Rule جدیدی اختراع کند.

مثلاً:

```text
Never invent a business rule.

A business violation must be supported by:
- ADR
- Domain Documentation
- Business Rule
- Test
- Explicit Project Rule

If evidence is insufficient:
mark the finding as UNCERTAIN.
```

---

# 7. `.ai/agent-config.yaml`

این فایل Configuration اجرایی Agentهاست.

مثلاً مشخص می‌کند:

* چه Agentهایی فعال هستند.
* هر Agent چه منابعی را بخواند.
* ترتیب بررسی چگونه باشد.
* Input Agent چیست.
* Output مورد انتظار چیست.
* Severityها چه مقادیری دارند.
* آیا Agent اجازه پیشنهاد Code Change دارد یا فقط Review می‌کند.

نمونه مفهومی:

```yaml
agents:

  domain:
    enabled: true
    sources:
      - docs/analysis/

  architecture:
    enabled: true
    sources:
      - docs/architecture/

  performance:
    enabled: true
    sources:
      - docs/performance/
```

---

# 8. PR Context Builder

**PR Context Builder** قبل از Retrieval، Pull Request را تحلیل می‌کند.

مسئولیت‌ها:

* Analyze changed files
* Determine review scope
* Extract relevant entities
* Build retrieval queries
* Prepare context for RAG

> PR Context Builder فرآیند Retrieval را آماده و Orchestrate می‌کند، اما خودش Retrieval را انجام نمی‌دهد.

---

# 9. RAG

RAG یعنی:

> Retrieval-Augmented Generation

هدف RAG این است که به جای ارسال تمام Repository و تمام Documentation به Model، فقط دانش موردنیاز برای Pull Request فعلی را پیدا کرده و در اختیار Agent قرار دهد.

فرآیند:

```text
Pull Request
      ↓
Changed Files
      ↓
Determine affected area
      ↓
Find relevant documentation
      ↓
Retrieve
      ↓
Send relevant context to LLM
      ↓
Generate Review
```

RAG مسئول:

* Query کردن Knowledge Base
* Retrieve کردن Documentation مرتبط
* ساخت Context موردنیاز برای AI Agents

> RAG مسئول **Retrieval** است، نه Analysis.

---

# 10. Retrieval

Retrieval می‌تواند شامل دو بخش باشد:

```text
RAG
 │
 ├── Semantic Retrieval
 │
 └── Relationship Retrieval
```

## 10.1. Semantic Retrieval

هدف:

پیدا کردن Documentهایی که از نظر معنایی با موضوع Pull Request مرتبط هستند.

پیاده‌سازی معمول:

* Vector Database
* Embedding Search

مثال منابع:

* ADRs
* Architecture Documents
* Domain Analyses
* Coding Guidelines

> Vector Database یک Implementation Detail برای Semantic Retrieval است و جزء الزامی معماری نیست.

---

## 10.2. Relationship Retrieval

هدف:

پیدا کردن Relationship بین مفاهیم پروژه.

مثال:

* Aggregate ↔ Repository
* Aggregate ↔ Value Object
* Aggregate ↔ Domain Rules
* Aggregate ↔ ADR
* Bounded Context relationships

پیاده‌سازی‌های ممکن:

* Metadata
* Knowledge Graph
* Dependency Graph

> Relationship Retrieval در کنار Semantic Retrieval برای درک روابط ساختاری پروژه قرار دارد.

---

# 11. Relevant Context

خروجی ترکیبی Retrieval است:

```text
Semantic Retrieval
        +
Relationship Retrieval
        ↓
Relevant Context
```

این Context در اختیار Agentهای تخصصی قرار می‌گیرد.

---

# 12. سه Agent تخصصی

تصمیم فعلی این است که سیستم از **سه Agent تخصصی** تشکیل شود.

```text
                    Pull Request
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
   Architecture       Domain       Performance
      Agent            Agent           Agent
          │              │              │
          └──────────────┼──────────────┘
                         ▼
                  Review Result
```

---

# 13. Architecture Agent

مسئول بررسی مسائل معماری است.

موارد اصلی:

* Onion Architecture
* Clean Architecture
* Module Boundary
* Dependency Direction
* Layer Responsibilities
* Bounded Context
* Infrastructure Dependency
* Aggregate Boundary
* ADR Compliance
* Design principles
* DDD consistency

منابع اصلی:

```text
.ai/system-context.md
docs/architecture/
```

مثال Finding:

```text
Architecture Violation

The Domain layer directly depends on Infrastructure.

Evidence:
ADR-XXXX
architecture documentation

Expected:
Domain → Application abstraction
Infrastructure → Implementation
```

---

# 14. Domain Agent

مسئول بررسی منطق و طراحی Domain است.

موارد اصلی:

* Entity
* Value Object
* Aggregate
* Aggregate Root
* Domain Service
* Policy
* Invariant
* Business Rules
* Use Case
* Domain Behavior
* Domain Model

منابع اصلی:

```text
.ai/system-context.md
docs/analysis/
```

---

## مثال مهم: Policy

فرض کنیم Developer این کد را اضافه کند:

```java
public class AccountHeading extends AggregateRoot<AccountHeadingId> {

    public void validateUniqueCode(
            AccountHeadingRepository repository) {

        if (repository.existsByCode(code)) {
            throw new DuplicateCodeException();
        }
    }
}
```

Unit Test ممکن است کاملاً موفق باشد.

اما Domain Agent می‌تواند تشخیص دهد:

```text
The Aggregate directly depends on repository state.

The uniqueness rule depends on external state.

According to project architecture/domain rules,
this responsibility should be handled outside the Aggregate.

Suggested approach:
Domain Policy / Domain Service
+
Repository
+
Aggregate
```

این دقیقاً یکی از مسائلی است که Unit Test به‌تنهایی نمی‌تواند به‌خوبی آن را کنترل کند.

---

# 15. Performance Agent

مسئول بررسی Performance است.

موارد اصلی:

* N+1 Query
* Database Calls
* Batch Processing
* Collection Size
* Remote Calls
* Transaction Boundaries
* Memory Usage
* Algorithmic Complexity
* Serialization
* Concurrency
* Cache
* Scalability
* Resource usage
* Optimization opportunities

مثلاً:

```java
for (AccountHeading heading : headings) {
    repository.findByCode(heading.getCode());
}
```

Agent می‌تواند تشخیص دهد:

```text
Potential N+1 Query

A repository call is executed inside a loop.

If N records are processed,
up to N database calls may be executed.
```

منبع اصلی:

```text
docs/performance/
```

---

# 16. نقش n8n

n8n در این معماری **مغز سیستم نیست**.

نقش n8n:

> **Orchestrator**

است.

مسئولیت‌ها:

* دریافت Pull Request
* گرفتن اطلاعات PR
* گرفتن Changed Files
* ساخت Context
* پیدا کردن منابع مرتبط
* اجرای Agentها
* جمع‌آوری نتایج
* ارسال نتیجه به GitHub

---

# 17. Workflow کلی

```text
                         GitHub
                           │
                           │ PR
                           ▼
                    ┌─────────────┐
                    │     n8n     │
                    │ Orchestrator│
                    └──────┬──────┘
                           │
                    PR Context Builder
                           │
                           ▼
                         RAG
                           │
              ┌────────────┴────────────┐
              │                         │
     Semantic Retrieval       Relationship Retrieval
              │                         │
       Vector DB /                Metadata /
       Embedding Search           Graph / Dependency Graph
              │                         │
              └────────────┬────────────┘
                           ▼
                    Relevant Context
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
      Architecture      Domain         Performance
         Agent           Agent             Agent
          │                │                │
          └────────────────┼────────────────┘
                           ▼
                    Senior Reviewer
                           │
                           ▼
                         GitHub
```

---

# 18. Senior Reviewer / Aggregation

سه Agent نتیجه خودشان را تولید می‌کنند.

مثلاً:

```json
{
  "architecture": [],
  "domain": [],
  "performance": []
}
```

سپس نتیجه‌ها باید تجمیع و ارزیابی شوند.

Senior Reviewer مسئول:

* Collect findings from all agents
* Remove duplicates
* Prioritize issues
* Produce the final review

خروجی نهایی می‌تواند شامل موارد زیر باشد:

```text
APPROVE
COMMENT
REQUEST_CHANGES
```

و Severity:

```text
BLOCKER
ARCHITECTURE
PERFORMANCE
SUGGESTION
```

---

# 19. نمونه خروجی نهایی

برای یک Pull Request:

```text
AI Senior Review
────────────────────────

❌ REQUEST CHANGES

Overall Score: 72/100

🔴 1 Domain Violation
🟠 2 Architecture Violations
🟡 1 Performance Concern
🔵 3 Suggestions
```

برای هر Finding:

```text
Architecture Violation

Problem:
Aggregate directly accesses repository.

Evidence:
ADR-003
account-heading-analysis.md

Why:
The rule depends on external state.

Suggested approach:
Domain Policy / Domain Service
```

نکته مهم:

> Finding باید تا حد امکان Evidence داشته باشد.

---

# 20. RAG و Vector Database در MVP

تصمیم فعلی:

> **برای MVP نیازی به Vector Database نداریم.**

Documentation پروژه ساختار مشخصی دارد:

```text
docs/
├── analysis/
├── architecture/
└── performance/
```

بنابراین نسخه اول می‌تواند از **File-based Retrieval** استفاده کند:

```text
PR
 ↓
Changed Files
 ↓
Determine affected area
 ↓
Read relevant Markdown
 ↓
Send to Agent
```

مثلاً اگر این فایل تغییر کرده باشد:

```text
AccountHeading.java
```

سیستم می‌تواند منابع مرتبط را پیدا کند:

```text
docs/analysis/account-heading/
docs/architecture/
docs/performance/performance-rules.md
.ai/system-context.md
.ai/review-policy.md
```

---

# 21. Vector Database در آینده

اگر پروژه بزرگ شد، Retrieval می‌تواند ارتقا پیدا کند:

```text
File Retrieval
      ↓
Vector Database
      ↓
Semantic Search
      ↓
RAG
```

در نتیجه:

> RAG یک قابلیت قابل توسعه است، نه الزام MVP.

و:

> Vector Database یک Implementation Detail برای Semantic Retrieval است، نه یک جزء اجباری معماری.

---

# 22. Model و هزینه

هدف مسابقه این است که Demo تا حد امکان رایگان باشد.

بنابراین پیشنهاد فعلی:

```text
n8n
+
Ollama
+
Local Coding Model
```

مزیت:

* بدون نیاز به API پولی
* مناسب برای Demo
* قابل اجرا Local
* عدم وابستگی به Provider خاص

مدل نیز باید قابل تعویض باشد.

معماری مفهومی:

```text
             AI Provider
                  │
        ┌─────────┼─────────┐
        ▼         ▼         ▼
     Ollama    Provider B  Provider C
        │
        ▼
   Coding Model
```

در نتیجه سیستم **Model-Agnostic** باقی می‌ماند.

---

# 23. MVP پیشنهادی

نباید از ابتدا کل سیستم را بسازیم.

MVP:

```text
GitHub PR
   ↓
n8n
   ↓
Get Changed Files
   ↓
Read relevant .ai + docs
   ↓
Architecture Agent
   ↓
GitHub Review Comment
```

بعد:

```text
+ Domain Agent
+ Performance Agent
+ Review Aggregator
+ Severity
+ Decision Engine
```

و در مراحل بعد:

```text
+ RAG
+ Vector DB
+ Advanced Context Retrieval
+ Score
+ Automated REQUEST_CHANGES
```

---

# 24. بهترین Demo برای مسابقه

بهترین Demo این است که روی همان Repository مربوط به **General Ledger** اجرا شود.

یک Pull Request عمداً ایجاد می‌کنیم که یک تصمیم معماری یا Domain اشتباه داشته باشد.

مثلاً:

```text
Aggregate
   ↓
Direct Repository Access
```

سپس Demo:

```text
Pull Request Created
        ↓
AI analyzing...
        ↓
Reading project context...
        ↓
Reading ADRs...
        ↓
Reading domain documentation...
        ↓
Analyzing changed classes...
        ↓
Architecture Agent...
        ↓
Domain Agent...
        ↓
Performance Agent...
        ↓
Senior Review...
        ↓
GitHub Review Comment
```

و در نهایت:

```text
❌ REQUEST CHANGES

Architecture violation detected.

Evidence:
ADR-003
Domain Analysis

Reason:
The Aggregate depends directly on repository state.

Suggested approach:
Move external-state validation to
Domain Policy / Domain Service.
```

این Demo نشان می‌دهد که سیستم فقط **Bug Finder** نیست؛ بلکه **Engineering Decision Reviewer** است.

---

# 25. معماری نهایی مفهومی

```text
                         GitHub
                           │
                           │ Pull Request
                           ▼
                    ┌─────────────┐
                    │     n8n     │
                    │ Orchestrator│
                    └──────┬──────┘
                           │
                    PR Context Builder
                           │
                           ▼
                          RAG
                           │
              ┌────────────┴────────────┐
              │                         │
   Semantic Retrieval        Relationship Retrieval
              │                         │
       Vector Database            Graph / Metadata
              │                         │
              └────────────┬────────────┘
                           ▼
                    Relevant Context
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         Architecture    Domain     Performance
            Agent         Agent        Agent
              │            │            │
              └────────────┼────────────┘
                           ▼
                    Review Aggregator
                           │
                           ▼
                    Senior Reviewer
                           │
                           ▼
                  APPROVE / COMMENT /
                   REQUEST_CHANGES
                           │
                           ▼
                         GitHub
```

---

# 26. اصول کلیدی معماری

* `.ai` مشخص می‌کند **AI چگونه رفتار کند**.
* `docs` شامل **دانش پروژه و Source of Truth** است.
* `PR Context Builder` Pull Request را برای Retrieval آماده می‌کند.
* `RAG` فقط دانش مرتبط با Review فعلی را Retrieve می‌کند.
* `Semantic Retrieval` اسناد را بر اساس شباهت معنایی پیدا می‌کند.
* `Relationship Retrieval` روابط و وابستگی‌های ساختاری بین مفاهیم پروژه را پیدا می‌کند.
* `Relevant Context` خروجی ترکیبی Retrieval است که در اختیار Agentها قرار می‌گیرد.
* `Specialized Agents` تحلیل تخصصی انجام می‌دهند.
* `Senior Reviewer` تصمیم نهایی Review را تولید می‌کند.
* Vector Database یکی از پیاده‌سازی‌های Semantic Retrieval است و جزء اجباری معماری نیست.
* Relationship Retrieval می‌تواند با Metadata، Dependency Graph یا Knowledge Graph پیاده‌سازی شود.
* Agent نباید Business Rule جدید اختراع کند.
* Finding باید تا حد امکان Evidence داشته باشد.

---

# 27. تصمیمات فعلی پروژه

| موضوع                       | تصمیم                                    |
| --------------------------- | ---------------------------------------- |
| Orchestrator                | n8n                                      |
| AI Architecture             | Multi-Agent                              |
| تعداد Agent اولیه           | 3                                        |
| Architecture Agent          | بله                                      |
| Domain Agent                | بله                                      |
| Performance Agent           | بله                                      |
| `.ai/`                      | Context + Policy + Configuration         |
| `docs/`                     | Project Knowledge / Source of Truth      |
| PR Context Builder          | مسئول تحلیل PR و آماده‌سازی Retrieval    |
| RAG                         | Retrieval Layer                          |
| Semantic Retrieval          | Semantic Document Search                 |
| Relationship Retrieval      | Relationship / Dependency Discovery      |
| Vector DB                   | فعلاً در MVP استفاده نمی‌شود             |
| Retrieval در MVP            | File-based                               |
| Vector DB در آینده          | Semantic Search                          |
| Model                       | قابل تعویض                               |
| اجرای اولیه Model           | Local                                    |
| Ollama                      | گزینه اصلی MVP                           |
| خروجی                       | GitHub PR Review                         |
| هدف                         | Architecture-aware / Domain-aware Review |
| Business Rule hallucination | ممنوع                                    |
| Evidence برای Finding       | الزامی                                   |
| Senior Review               | مرحله نهایی تصمیم‌گیری                   |

---

# 28. جمله اصلی برای ارائه مسابقه

> **We are not building another AI Code Reviewer.
> We are building an AI Senior Engineer that understands the project's architecture, domain rules, ADRs, and performance constraints — and reviews Pull Requests based on them.**

یا نسخه کوتاه‌تر:

> **Unit tests verify the code. AI Guardian verifies the engineering decisions.**

---

# 29. وضعیت فعلی و قدم بعدی

تا اینجا Concept مشخص شده است:

```text
Repository Knowledge
        +
AI Configuration
        +
n8n
        +
PR Context Builder
        +
Three Specialized Agents
        +
Local LLM
        +
GitHub
```

### قدم بعدی عملی

اولین Workflow واقعی باید فقط این باشد:

```text
GitHub
   ↓
Pull Request
   ↓
n8n
   ↓
Get PR
   ↓
Get Changed Files
   ↓
Read .ai/system-context.md
   ↓
Read .ai/review-policy.md
   ↓
Read relevant architecture docs
   ↓
Architecture Agent
   ↓
GitHub Comment
```

بعد از اینکه این مسیر End-to-End کار کرد، دو Agent دیگر و Review Aggregator را اضافه می‌کنیم.

این روش باعث می‌شود خیلی سریع یک **Demo واقعی و قابل نمایش** داشته باشیم، بدون اینکه از ابتدا درگیر Vector DB، RAG پیچیده یا زیرساخت سنگین شویم.

---

# 30. Terminology

| Term                     | Purpose                                         |
| ------------------------ | ----------------------------------------------- |
| `.ai`                    | AI behavior and configuration                   |
| `docs`                   | Project knowledge base                          |
| `PR Context Builder`     | Pull Request analysis and retrieval preparation |
| `RAG`                    | Retrieval layer                                 |
| `Semantic Retrieval`     | Semantic document search                        |
| `Relationship Retrieval` | Relationship and dependency discovery           |
| `Relevant Context`       | Final retrieved knowledge                       |
| `Specialized Agents`     | Technical reviewers                             |
| `Senior Reviewer`        | Final decision maker                            |

---

# 31. Notes

* RAG یک الگوی معماری برای Retrieval-Augmented Generation است.
* Vector Database یک Implementation Detail برای Semantic Retrieval است، نه یک جزء الزامی معماری.
* Relationship Retrieval می‌تواند با Metadata، Dependency Graph یا Knowledge Graph پیاده‌سازی شود.
* اصطلاح **RVG** در نسخه قبلی با اصطلاح استاندارد **RAG** جایگزین شده است تا با Terminology رایج Agentic RAG هم‌راستا باشد.
