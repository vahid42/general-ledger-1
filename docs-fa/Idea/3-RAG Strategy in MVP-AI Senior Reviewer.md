# استراتژی RAG در MVP — AI Senior Reviewer

## 1. آیا در MVP نیاز به پیاده‌سازی جداگانه RAG داریم؟

خیر.

در MVP نیازی به ایجاد یک پروژه یا زیرسیستم مستقل و سنگین برای RAG نداریم. با این حال، **یک شکل ساده از RAG از همان ابتدا در Workflow اصلی وجود خواهد داشت.**

Workflow اولیه:

```text
PR
 ↓
Changed Files
 ↓
Determine Affected Area
 ↓
Read Relevant Markdown
 ↓
Send Relevant Context to Agent
```

این مدل را می‌توان **File-based Retrieval** در نظر گرفت.

---

## 2. معماری RAG در MVP

```text
                PR
                 │
                 ▼
         Changed Files
                 │
                 ▼
       PR Context Builder
                 │
                 ▼
       Relevant Documents
                 │
          ┌──────┼──────┐
          ▼      ▼      ▼
        .ai/    ADRs   docs/
                 │
                 ▼
          Relevant Context
                 │
                 ▼
          Architecture Agent
```

در این معماری، `PR Context Builder` وظیفه دارد بر اساس فایل‌های تغییرکرده، محدوده و مستندات مرتبط را پیدا کند و Context مناسب را برای Agent بسازد.

---

## 3. وضعیت اجزای RAG در MVP

| بخش                    | وضعیت در MVP          |
| ---------------------- | --------------------- |
| RAG                    | ✅ وجود دارد، اما ساده |
| Retrieval              | ✅ File-based          |
| Context Builder        | ✅                     |
| Relevant Context       | ✅                     |
| Markdown Documents     | ✅                     |
| Embedding              | ❌                     |
| Vector DB              | ❌                     |
| Semantic Search        | ❌                     |
| Knowledge Graph        | ❌                     |
| Relationship Retrieval | ❌                     |

---

## 4. نکته کلیدی: RAG الزاماً Vector DB نیست

RAG به معنی استفاده اجباری از Vector Database نیست.

مفهوم اصلی RAG این است:

> قبل از ارسال درخواست به LLM، اطلاعات مرتبط را از یک منبع دانش Retrieve کنیم و آن را به Context مدل اضافه کنیم.

بنابراین این معماری نیز یک RAG ساده محسوب می‌شود:

```text
PR
 ↓
Changed Files
 ↓
Relevant Markdown
 ↓
Context
 ↓
LLM
```

در این مرحله Retrieval بر اساس ساختار Repository و ارتباط فایل‌ها با مستندات انجام می‌شود، نه بر اساس Similarity Search.

---

## 5. مسیر تکامل RAG

RAG در این پروژه به‌صورت تدریجی ارتقا پیدا می‌کند.

### مرحله ۱ — MVP

```text
File-based Retrieval
        │
        ▼
Relevant Markdown
        │
        ▼
Context Builder
        │
        ▼
Architecture Agent
```

هدف این مرحله:

* ساده بودن
* قابل فهم بودن
* پیاده‌سازی سریع
* بدون زیرساخت اضافه
* امکان تست Workflow اصلی

---

### مرحله ۲ — Semantic Retrieval

زمانی که Repository و حجم مستندات افزایش پیدا کرد:

```text
File-based Retrieval
        │
        ▼
Semantic Retrieval
        │
        ▼
Relevant Documents
        │
        ▼
Context Builder
        │
        ▼
Architecture Agent
```

در این مرحله می‌توان از Embedding برای پیدا کردن مستندات مرتبط‌تر استفاده کرد.

---

### مرحله ۳ — Vector DB

در صورت نیاز:

```text
Documents
    │
    ▼
Chunking
    │
    ▼
Embedding
    │
    ▼
Vector DB
    │
    ▼
Semantic Retrieval
    │
    ▼
Context Builder
    │
    ▼
Architecture Agent
```

در این مرحله Vector DB به‌عنوان زیرساخت Retrieval اضافه می‌شود.

---

### مرحله ۴ — Relationship Retrieval

در صورت بزرگ‌تر شدن سیستم و پیچیده‌تر شدن ارتباطات:

```text
                 ┌──────────────┐
                 │  Vector DB   │
                 └──────┬───────┘
                        │
                        ▼
                Semantic Retrieval
                        │
                        │
PR ──► Changed Files ───┤
                        │
                        ▼
              Relationship Retrieval
                        │
                        ▼
                 Relevant Context
                        │
                        ▼
                Architecture Agent
```

در این مرحله علاوه بر شباهت معنایی، روابط بین موارد مختلف نیز می‌توانند در Retrieval مورد استفاده قرار بگیرند.

برای مثال:

```text
Changed File
     │
     ├──► Module
     │
     ├──► ADR
     │
     ├──► Architecture Rule
     │
     └──► Related Documentation
```

---

## 6. تصمیم معماری MVP

در MVP **RAG را به‌عنوان یک پروژه مستقل پیاده‌سازی نمی‌کنیم.**

Retrieval بخشی از Workflow اصلی AI Senior Reviewer خواهد بود:

```text
GitHub PR
   │
   ▼
Changed Files
   │
   ▼
Affected Area Detection
   │
   ▼
File-based Retrieval
   │
   ▼
Context Builder
   │
   ▼
Architecture Agent
   │
   ▼
Review Result
```

بنابراین در n8n نیز RAG در قالب همین مرحله‌های Workflow پیاده می‌شود و نیازی به ساخت یک سرویس مستقل RAG در MVP وجود ندارد.

---

## 7. تصمیم نهایی

> **در MVP از File-based Retrieval به‌عنوان ساده‌ترین شکل RAG استفاده می‌کنیم.**

در نتیجه:

```text
MVP
 │
 ├── File-based Retrieval
 ├── Context Builder
 └── Architecture Agent

Future
 │
 ├── Semantic Retrieval
 ├── Embedding
 ├── Vector DB
 └── Relationship Retrieval
```

### نتیجه

فعلاً **RAG را ساده نگه می‌داریم**.

ابتدا Workflow اصلی را در n8n با File-based Retrieval پیاده می‌کنیم. پس از اثبات عملکرد سیستم و افزایش حجم Repository و مستندات، Retrieval را بدون تغییر اساسی در معماری کلی به Semantic RAG و سپس در صورت نیاز به Vector DB و Relationship Retrieval ارتقا می‌دهیم.

**بنابراین در MVP:**

```text
RAG ≠ Vector DB
RAG = Retrieval + Context + LLM
```

و در نسخه فعلی:

```text
Retrieval = File-based Retrieval
```
