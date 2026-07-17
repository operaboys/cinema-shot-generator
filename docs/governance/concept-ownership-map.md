# نقشه‌ی مالکیت مفهومی (Concept Ownership Map)

**نقش:** Governance — قانون رفتاری سیستم
**وضعیت:** فعال

مشخص می‌کند هر تصمیم مفهومی دقیقاً متعلق به کدام واحد معماری است، تا از تداخل، ابهام و تصمیم‌گیری اشتباه جلوگیری شود.

---

| مفهوم | مالک اصلی (Primary) | نقش ثانویه |
|---|---|---|
| Vision کلی پروژه | Human (کاربر) | DNA Manager (Secondary) |
| سبک سینمایی | DNA Manager | Cinematic Language (Execution) |
| زبان بصری | Cinematic Language | Style Matrix (Detail Layer) |
| جزئیات استایل (رنگ، Mood، بافت) | Style Matrix | Asset Library (Reference) |
| داستان و روایت | Story Wizard | Human Override (تصمیم نهایی همیشه با انسان) |
| ساختار روایت (Beat/Arc) | Story Wizard | Scene Engine (Secondary) |
| تعریف Scene | Scene Engine | DNA Manager (Constraints) |
| تعریف Shot | Shot Engine | Scene Engine (Inheritance) |
| دوربین | Camera System | Shot Engine (Context) |
| نورپردازی | Lighting System | Scene Engine (Base Mood) |
| محیط و آب‌وهوا | Environment Engine | Scene Engine (Default) |
| کاراکتر و سوژه | Character & Subject System | DNA Manager (Constraints — Hard Lock برای Continuity) |
| پرامپت نهایی (خنثی) | Prompt Engineering Core | Output Delivery System (فرمت‌دهی مخصوص هر مدل) |
| قوانین تولید خروجی برای یک مدل خاص | Output Delivery System (Model Profile) | Prompt Engineering Core (Implementation) |
| اعتبارسنجی | Validation & Consistency Engine | — (Impact Analysis به‌عنوان بخشی از همین واحد) |
| تغییرات و اثرات جانبی | Validation & Consistency Engine (Dependency جزء) | State & Versioning (Recording) |

---

## قانون طلایی

هر مفهوم فقط یک **مالک اصلی** دارد. واحدهای دیگر فقط می‌توانند:
- ارجاع دهند
- محدود کنند
- Override شوند

اما هرگز مالکیت را از واحد اصلی نمی‌گیرند.
