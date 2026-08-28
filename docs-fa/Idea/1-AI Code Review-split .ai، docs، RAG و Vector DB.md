حتماً وحیدجون. هر دو مستند را **بدون تحلیل یا تغییر مفهومی** با هم Merge کردم؛ موارد تکراری فقط یک‌بار آمده‌اند و بخش‌هایی که در یکی از مستندها جزئیات بیشتری داشتند، نگه داشته شده‌اند. ساختار هم طوری یکپارچه شده که یک سند نهایی Markdown مستقل باشد.

# مدل معماری AI Code Review — تفکیک `.ai`، `docs`، RAG و Vector DB

## 1. مدل ذهنی اصلی پروژه

تفکیک مفاهیم و اجزای اصلی سیستم به شکل زیر است:

| جزء                       | چیست؟                               | نقش در سیستم                                                        | مثال                                                         |
| ------------------------- | ----------------------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------ |
| `.ai/`                    | **AI Configuration & Instructions** | به Agent می‌گوید پروژه را چطور بفهمد و چطور Review کند              | `system-context.md`، `review-policy.md`، `agent-config.yaml` |
| `docs/`                   | **Knowledge Base**                  | دانش واقعی پروژه که Agent برای تصمیم‌گیری به آن نیاز دارد           | ADRها، Domain Analysis، Performance Rules                    |
| **RAG**                   | **Retrieval Mechanism / Pattern**   | از Knowledge Base اطلاعات مرتبط با PR را پیدا می‌کند                | پیدا کردن ADR مرتبط با `AccountHeading`                      |
| **Vector DB**             | **Storage / Infrastructure**        | یکی از روش‌های ذخیره و جست‌وجوی Semantic در Knowledge Base          | ذخیره Embeddingهای ADRها                                     |
| **Embedding Model**       | **Text → Vector**                   | متن اسناد را به بردار عددی تبدیل می‌کند تا Semantic Search ممکن شود | تبدیل `ADR-0013.md` به Vector                                |
| **n8n**                   | **Orchestrator**                    | کل Workflow را اجرا و Agentها و سرویس‌ها را به هم متصل می‌کند       | GitHub → RAG → Agents → GitHub                               |
| **Architecture Agent**    | **Specialized AI Agent**            | معماری و ADRها را بررسی می‌کند                                      | Aggregate، Dependency، Onion Architecture                    |
| **Domain Agent**          | **Specialized AI Agent**            | Business Rule و Domain Design را بررسی می‌کند                       | Entity، VO، Aggregate، Policy                                |
| **Performance Agent**     | **Specialized AI Agent**            | مشکلات Performance را بررسی می‌کند                                  | N+1، DB Call، Memory، Concurrency                            |
| **Senior Reviewer Agent** | **Decision Agent**                  | نتایج Agentها را جمع می‌کند و تصمیم نهایی می‌دهد                    | `APPROVE` / `COMMENT` / `REQUEST_CHANGES`                    |
| **GitHub**                | **Source + Output**                 | PR را Trigger می‌کند و نتیجه Review را نمایش می‌دهد                 | Pull Request Review                                          |

---

# 2. ارتباط همه اجزای سیستم

```text
                         GitHub
                           │
                      Pull Request
                           │
                           ▼
                         n8n
                     Orchestrator
                           │
                           ▼
                  ┌─────────────────┐
                  │   RAG Layer     │
                  │                 │
                  │    Retrieve     │
                  │ Relevant Docs   │
                  └────────┬────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
            .ai/                       docs/
              │                         │
       Instructions / Config       Project Knowledge
              │                         │
              │                  ┌──────┴──────┐
              │                  │             │
              │                ADRs        Analysis
              │                  │             │
              │                  └──────┬──────┘
              │                         │
              └────────────┬────────────┘
                           ▼
                  Specialized Agents
                  ┌────────┼────────┐
                  ▼        ▼        ▼
             Architecture Domain Performance
                  │        │        │
                  └────────┼────────┘
                           ▼
                  Senior Reviewer
                           │
                           ▼
                  APPROVE / COMMENT
                  REQUEST_CHANGES
                           │
                           ▼
                         GitHub
```

---

# 3. نقش `.ai/`

پوشه `.ai/` **منبع دانش پروژه نیست**؛ بلکه محل تعریف نحوه رفتار و تفکر AI است.

ساختار نمونه:

```text
.ai/
├── system-context.md
├── review-policy.md
├── agent-config.yaml
└── ...
```

وظیفه `.ai/` پاسخ به این سؤال است:

> **AI چگونه باید پروژه را بفهمد و چگونه باید آن را Review کند؟**

برای مثال `.ai/` می‌تواند مشخص کند:

* چه اصول معماری باید رعایت شوند؟
* Agent چه قوانینی را باید بررسی کند؟
* خروجی Review چه Format داشته باشد؟
* چه Agentهایی فعال باشند؟
* Severity خطاها چگونه تعیین شود؟
* چه مواردی `REQUEST_CHANGES` محسوب شوند؟
* برای شناخت Domain از چه منابعی استفاده شود؟
* چه Checkهایی در Review انجام شوند؟

مثلاً:

```yaml
domain:
  sources:
    - docs/architecture/adr/
    - docs/analysis/

review:
  check:
    - aggregate-boundary
    - domain-rules
    - repository-dependency
```

معنی آن:

> **Agent عزیز، برای Review این قوانین را رعایت کن و برای شناخت Domain از این منابع استفاده کن.**

بنابراین `.ai/` **نحوه کار Agent** را مشخص می‌کند.

---

# 4. نقش `docs/`

پوشه `docs/` **Knowledge Base واقعی پروژه** است.

ساختار نمونه:

```text
docs/
├── adr/
│   ├── ADR-0001.md
│   ├── ADR-0013.md
│   └── ...
│
├── domain/
│   ├── domain-analysis.md
│   └── aggregates.md
│
├── architecture/
│   └── architecture-rules.md
│
└── performance/
    └── performance-rules.md
```

یا به شکل مفهومی:

```text
docs/
├── architecture/
│   └── adr/
│       └── ADR-0013.md
│
└── analysis/
    └── account-heading-analysis.md
```

این فایل‌ها حاوی **دانش واقعی پروژه** هستند.

برای مثال:

```text
ADR-0013
    ↓
Value Object Strategy

AccountHeading Analysis
    ↓
Aggregate Boundary
```

وظیفه `docs/` پاسخ به این سؤال است:

> **AI درباره این پروژه چه چیزهایی باید بداند؟**

برای مثال، اگر PR مربوط به `AccountHeading` باشد، Agent ممکن است به این موارد نیاز داشته باشد:

```text
ADR-0013
Domain Analysis
Aggregate Rules
Value Object Strategy
Architecture Rules
```

پس:

> **`docs/` می‌گوید پروژه چه دانشی دارد.**

---

# 5. تفاوت مهم `.ai/` و RAG

این قسمت یکی از اصول اصلی معماری است.

| مورد                     | `.ai/`                                      | RAG                                            |
| ------------------------ | ------------------------------------------- | ---------------------------------------------- |
| ماهیت                    | Configuration & Instruction                 | Retrieval Mechanism                            |
| سؤال اصلی                | **Agent چطور کار کند؟**                     | **Agent چه اطلاعاتی را برای این PR پیدا کند؟** |
| محتوا                    | دستورالعمل، Policy، Context و تنظیمات Agent | اطلاعات مرتبط استخراج‌شده از Knowledge Base    |
| منبع                     | فایل‌های مخصوص AI                           | معمولاً `docs/`، ADRها، Analysisها و Rules     |
| کار اصلی                 | تعیین رفتار Agent                           | پیدا کردن Context مناسب                        |
| خودش Knowledge Base است؟ | ❌                                           | ❌                                              |
| خودش RAG است؟            | ❌                                           | ✅                                              |
| خودش Vector DB است؟      | ❌                                           | ❌                                              |

### جمله کلیدی

> **`.ai/` می‌گوید Agent چطور کار کند؛ RAG پیدا می‌کند Agent برای این PR چه اطلاعاتی لازم دارد.**

---

# 6. RAG چیست؟

**RAG = Retrieval-Augmented Generation**

RAG خودش Database نیست.

RAG یک **Mechanism / Pattern برای بازیابی دانش مرتبط** است.

فرایند کلی:

```text
PR
 │
 ▼
Extract Context
 │
 ▼
Search Knowledge Base
 │
 ▼
Retrieve Relevant Documents
 │
 ▼
Provide Context to Agent
 │
 ▼
AI Review
```

بنابراین RAG تصمیم می‌گیرد:

> **برای این PR چه اطلاعاتی از Knowledge Base لازم است؟**

مثلاً:

```text
PR:
AccountHeading.java تغییر کرده

        │
        ▼

RAG Retrieval

        │
        ├── ADR-0013
        ├── Aggregate Rules
        ├── Value Object Strategy
        └── Architecture Rules

        │
        ▼

Architecture Agent
Domain Agent
```

RAG **دانش را تولید نمی‌کند**؛ بلکه دانش مرتبط را **Retrieve** می‌کند.

---

# 7. مثال واقعی برای تفاوت `.ai/` و RAG

فرض کنیم PR مربوط به `AccountHeading` است و چنین تغییری ایجاد شده:

```java
public class AccountHeading {

    public boolean isCodeUnique(
            AccountHeadingRepository repository) {

        return !repository.existsByCode(code);
    }
}
```

در اینجا چهار لایه نقش متفاوتی دارند.

---

## 7.1 `.ai/` چه کار می‌کند؟

مثلاً فایل:

```text
.ai/agent-config.yaml
```

می‌تواند مشخص کند:

```yaml
domain:
  sources:
    - docs/architecture/adr/
    - docs/analysis/

review:
  check:
    - aggregate-boundary
    - domain-rules
    - repository-dependency
```

معنی آن:

> **Agent عزیز، برای Review این قوانین را رعایت کن و برای شناخت Domain از این منابع استفاده کن.**

پس `.ai/` **نحوه کار Agent** را مشخص می‌کند.

---

## 7.2 `docs/` چه کار می‌کند؟

داخل `docs/` ممکن است چنین فایل‌هایی وجود داشته باشد:

```text
docs/
├── architecture/
│   └── adr/
│       └── ADR-0013.md
│
└── analysis/
    └── account-heading-analysis.md
```

این فایل‌ها حاوی **دانش واقعی پروژه** هستند.

مثلاً:

```text
ADR-0013
    ↓
Value Object Strategy

AccountHeading Analysis
    ↓
Aggregate Boundary
```

پس:

> **`docs/` می‌گوید پروژه چه دانشی دارد.**

---

## 7.3 RAG چه کار می‌کند؟

وقتی PR وارد سیستم می‌شود، RAG بررسی می‌کند:

```text
PR
 │
 ▼
AccountHeading
 │
 ▼
Search Knowledge Base
 │
 ├── ADR-0013.md
 ├── account-heading-analysis.md
 └── aggregate-rules.md
```

یعنی RAG از بین حجم زیادی از Documentation تصمیم می‌گیرد:

> **برای Review این PR، کدام اسناد مرتبط هستند؟**

---

## 7.4 Vector DB در این مثال

اگر Vector DB داشته باشیم:

```text
ADR-0013.md
       │
       ▼
    Chunking
       │
       ▼
Embedding Model
       │
       ▼
   Vector DB
```

بعد PR:

```text
AccountHeading changed
       │
       ▼
Embedding
       │
       ▼
Semantic Search
       │
       ▼
Vector DB
       │
       ▼
Relevant Documents
```

در نتیجه Vector DB کمک می‌کند RAG اسناد مرتبط را سریع‌تر و به‌صورت Semantic پیدا کند.

---

# 8. Vector DB چیست؟

Vector DB یک **Infrastructure / Storage Component** است.

وظیفه آن ذخیره و جست‌وجوی Vectorها است.

برای مثال:

```text
ADR-0013.md
      │
      ▼
Chunking
      │
      ▼
Embedding Model
      │
      ▼
Vector
      │
      ▼
Vector DB
```

سپس هنگام Review:

```text
PR Context
     │
     ▼
Embedding
     │
     ▼
Semantic Search
     │
     ▼
Vector DB
     │
     ▼
Relevant Documents
```

بنابراین:

> **Vector DB جای `docs/` را نمی‌گیرد.**

بلکه می‌تواند یک **Searchable Semantic Index** از `docs/` ایجاد کند.

---

# 9. Embedding Model چیست؟

Embedding Model وظیفه تبدیل متن به Vector را دارد.

مثلاً:

```text
ADR-0013.md
     │
     ▼
Embedding Model
     │
     ▼
[0.021, -0.382, 0.741, ...]
```

این Vector نشان‌دهنده موقعیت معنایی متن در فضای برداری است.

در نتیجه می‌توان متن‌هایی را که از نظر معنایی به یکدیگر نزدیک هستند پیدا کرد؛ حتی اگر کلمات دقیقاً یکسان نباشند.

---

# 10. Vector DB در کنار `docs/`

اگر در آینده Vector DB اضافه شود، معماری Knowledge Base به این شکل خواهد بود:

```text
                  docs/
                    │
                    ▼
                 Chunking
                    │
                    ▼
             Embedding Model
                    │
                    ▼
                Vector DB
                    │
                    ▼
              RAG Retrieval
                    │
                    ▼
                  Agent
```

در این مدل:

```text
docs/       = Source of Truth
Vector DB   = Search Index
RAG         = Retrieval Mechanism
Embedding   = Text → Vector
```

بنابراین:

> **`docs/` منبع اصلی و حقیقت پروژه است، Vector DB یک Index برای جست‌وجوی معنایی آن است و RAG مکانیزم بازیابی اطلاعات مرتبط است.**

این سه مفهوم نباید با یکدیگر اشتباه گرفته شوند.

---

# 11. نقش n8n

n8n در این معماری نقش **Orchestrator** را دارد.

یعنی خودش الزاماً مسئول تحلیل معماری نیست؛ بلکه Workflow را اجرا می‌کند و Agentها و سرویس‌ها را به هم متصل می‌کند.

مثلاً:

```text
GitHub PR
   │
   ▼
n8n
   │
   ├── دریافت تغییرات PR
   │
   ├── دریافت Context
   │
   ├── اجرای RAG
   │
   ├── اجرای Architecture Agent
   │
   ├── اجرای Domain Agent
   │
   ├── اجرای Performance Agent
   │
   ├── ارسال نتایج به Senior Reviewer
   │
   └── ارسال نتیجه به GitHub
```

در نتیجه:

> **n8n اجرا و هماهنگی Workflow را بر عهده دارد، نه تحلیل تخصصی کد را.**

---

# 12. نقش Specialized Agents

هر Agent یک مسئولیت تخصصی دارد.

## 12.1 Architecture Agent

تمرکز روی:

```text
Architecture
Dependency
Bounded Context
Aggregate Boundary
Onion Architecture
ADR Compliance
SOLID
```

نمونه مسئولیت:

> بررسی اینکه تغییرات PR با معماری و تصمیمات معماری پروژه سازگار هستند یا خیر.

---

## 12.2 Domain Agent

تمرکز روی:

```text
Entity
Value Object
Aggregate
Domain Service
Business Rule
Invariant
Policy
```

نمونه مسئولیت:

> بررسی صحت طراحی Domain و رعایت Business Ruleها و Invariantها.

---

## 12.3 Performance Agent

تمرکز روی:

```text
N+1
Database Calls
Memory
Concurrency
Caching
I/O
Latency
Scalability
```

نمونه مسئولیت:

> شناسایی مشکلات Performance و Scalability در تغییرات PR.

---

# 13. نقش Senior Reviewer Agent

Senior Reviewer مسئول **تصمیم نهایی** است.

ورودی:

```text
Architecture Agent Result
Domain Agent Result
Performance Agent Result
       │
       ▼
Senior Reviewer
```

خروجی:

```text
APPROVE
COMMENT
REQUEST_CHANGES
```

در واقع:

```text
Specialized Agents
        │
        ▼
  تخصصی تحلیل می‌کنند
        │
        ▼
Senior Reviewer
        │
        ▼
تصمیم نهایی
```

بنابراین:

> **Specialized Agentها تحلیل تخصصی انجام می‌دهند و Senior Reviewer نتایج را جمع‌بندی کرده و تصمیم نهایی را اتخاذ می‌کند.**

---

# 14. نقش GitHub

GitHub در این معماری هم **Source** و هم **Output** است.

### ورودی

```text
GitHub
   │
   ▼
Pull Request
   │
   ▼
n8n
```

Pull Request نقطه شروع Workflow است.

### خروجی

پس از انجام Review:

```text
Senior Reviewer
       │
       ▼
APPROVE
COMMENT
REQUEST_CHANGES
       │
       ▼
GitHub
```

نتیجه Review در GitHub نمایش داده می‌شود.

---

# 15. جریان کامل سیستم

کل سیستم را می‌توان به این شکل دید:

```text
                    GitHub
                       │
                  Pull Request
                       │
                       ▼
                     n8n
                Orchestrator
                       │
                       ▼
                Extract Context
                       │
                       ▼
                     RAG
                       │
              What knowledge
              is relevant?
                       │
                       ▼
                  Knowledge
                   Sources
                       │
             ┌─────────┴─────────┐
             │                   │
           .ai/                docs/
             │                   │
      Instructions /          Project
        Config              Knowledge
             │                   │
             │             ┌─────┴─────┐
             │             │           │
             │            ADRs      Analysis
             │             │           │
             │             └─────┬─────┘
             │                   │
             │             Optional Index
             │                   │
             │             ┌─────▼─────┐
             │             │ Embedding │
             │             │   Model   │
             │             └─────┬─────┘
             │                   │
             │             ┌─────▼─────┐
             │             │ Vector DB │
             │             │ Semantic  │
             │             │   Index   │
             │             └─────┬─────┘
             │                   │
             └─────────┬─────────┘
                       │
                       ▼
               Relevant Context
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
   Architecture     Domain     Performance
      Agent         Agent         Agent
          │            │            │
          └────────────┼────────────┘
                       │
                       ▼
                Senior Reviewer
                       │
                       ▼
              ┌─────────────────┐
              │    APPROVE      │
              │    COMMENT      │
              │ REQUEST_CHANGES │
              └────────┬────────┘
                       │
                       ▼
                     GitHub
```

---

# 16. تفاوت کامل اجزا در یک مثال

کل جریان را می‌توان این‌گونه دید:

```text
                    PR
                     │
                     ▼
              ┌─────────────┐
              │    .ai/     │
              │             │
              │ How to      │
              │ Review?     │
              └──────┬──────┘
                     │
                     ▼
                  RAG
                     │
              What knowledge
              is relevant?
                     │
                     ▼
                  docs/
                     │
              Project Knowledge
                     │
                     ▼
               Vector DB
             (Optional Index)
                     │
                     ▼
             Relevant Context
                     │
                     ▼
              Domain Agent
                     │
                     ▼
                 Analysis
```

---

# 17. مدل نهایی معماری

```text
                              GitHub
                                │
                           Pull Request
                                │
                                ▼
                              n8n
                         Orchestrator
                                │
                                ▼
                         ┌─────────────┐
                         │     RAG     │
                         │  Retrieval  │
                         └──────┬──────┘
                                │
                 ┌──────────────┴──────────────┐
                 │                             │
               .ai/                          docs/
                 │                             │
        AI Instructions                Project Knowledge
        & Configuration                       │
                                               │
                                      ┌────────┴────────┐
                                      │                 │
                                     ADRs            Analysis
                                      │                 │
                                      └────────┬────────┘
                                               │
                                      Optional Index
                                               │
                                      ┌────────▼────────┐
                                      │   Embedding     │
                                      │      Model      │
                                      └────────┬────────┘
                                               │
                                      ┌────────▼────────┐
                                      │   Vector DB     │
                                      │ Semantic Index  │
                                      └────────┬────────┘
                                               │
                                               ▼
                                          RAG Retrieval
                                               │
                         ┌─────────────────────┼─────────────────────┐
                         │                     │                     │
                         ▼                     ▼                     ▼
                  Architecture Agent    Domain Agent       Performance Agent
                         │                     │                     │
                         └─────────────────────┼─────────────────────┘
                                               │
                                               ▼
                                      Senior Reviewer
                                               │
                                               ▼
                              ┌─────────────────────────┐
                              │ APPROVE                 │
                              │ COMMENT                 │
                              │ REQUEST_CHANGES         │
                              └────────────┬────────────┘
                                           │
                                           ▼
                                         GitHub
```

---

# 18. پنج جمله کلیدی

این پنج جمله مدل ذهنی اصلی معماری هستند:

> **`.ai/` → به AI می‌گوید «چطور رفتار کن».**

> **`docs/` → به AI می‌گوید «چه چیزهایی درباره پروژه می‌دانیم».**

> **RAG → می‌گوید «برای این PR کدام دانش لازم است».**

> **Vector DB → یکی از ابزارهای ذخیره و جست‌وجوی Semantic آن دانش است.**

> **Agent → با استفاده از Code + Context تحلیل تخصصی انجام می‌دهد.**

---

# 19. مدل ذهنی نهایی هر جزء

```text
.ai/
  │
  └── «چطور بررسی کن؟»

docs/
  │
  └── «چه چیزهایی درباره پروژه می‌دانیم؟»

Embedding Model
  │
  └── «متن را چگونه به Vector تبدیل کنیم؟»

Vector DB
  │
  └── «چطور این دانش را Semantic Search کنیم؟»

RAG
  │
  └── «برای این PR کدام اطلاعات را بیاور؟»

n8n
  │
  └── «Workflow را چگونه اجرا و هماهنگ کنیم؟»

Specialized Agent
  │
  └── «با این اطلاعات چه مشکل تخصصی در کد وجود دارد؟»

Senior Reviewer
  │
  └── «در نهایت چه تصمیمی بگیریم؟»

GitHub
  │
  └── «PR از کجا وارد و نتیجه Review کجا نمایش داده شود؟»
```

---

# 20. اصول کلیدی معماری

مفاهیم اصلی سیستم مستقل از یکدیگر هستند:

```text
.ai/        ≠ docs/
.ai/        ≠ RAG
.ai/        ≠ Vector DB

docs/       ≠ RAG
docs/       ≠ Vector DB

RAG         ≠ Vector DB
RAG         ≠ Embedding Model

Embedding   ≠ Vector DB

n8n         ≠ Agent
Agent       ≠ Senior Reviewer
```

در نتیجه:

* `.ai/` منبع **Instructions و Configuration** مربوط به AI است.
* `docs/` منبع **Knowledge و Source of Truth** پروژه است.
* Embedding Model متن دانش را به **Vector** تبدیل می‌کند.
* Vector DB یک **Semantic Search Index** برای آن دانش است.
* RAG مکانیزم **Retrieval** دانش مرتبط برای یک PR است.
* n8n مسئول **Orchestration** Workflow است.
* Specialized Agents مسئول **تحلیل تخصصی** هستند.
* Senior Reviewer مسئول **جمع‌بندی و تصمیم نهایی** است.
* GitHub نقطه **ورودی PR و خروجی Review** است.

---

# 21. جمع‌بندی نهایی معماری

```text
.ai/
    → AI Instructions & Configuration

docs/
    → Project Knowledge / Source of Truth

Embedding Model
    → Text → Vector

Vector DB
    → Semantic Search Index

RAG
    → Retrieve Relevant Knowledge

n8n
    → Orchestrate Workflow

Specialized Agents
    → Perform Specialized Analysis

Senior Reviewer
    → Aggregate Results & Make Final Decision

GitHub
    → PR Input + Review Output
```

### اصل نهایی

```text
.ai/        → How should AI behave?
docs/       → What does the project know?
Embedding   → How is knowledge converted to vectors?
Vector DB   → How is semantic knowledge indexed/searchable?
RAG         → What knowledge is relevant to this PR?
Agent       → What does the code violate?
Senior      → What is the final decision?
n8n         → How is the workflow orchestrated?
GitHub      → Where does the PR enter and review result appear?
```

**بنابراین `.ai`، `docs`، RAG و Vector DB چهار مفهوم متفاوت هستند و هیچ‌کدام جای دیگری را نمی‌گیرد.**

**`docs/` منبع دانش و Source of Truth است، Vector DB یک Index برای جست‌وجوی معنایی آن دانش است، Embedding Model متن را به Vector تبدیل می‌کند و RAG مکانیزم بازیابی دانش مرتبط برای Agent است.**
