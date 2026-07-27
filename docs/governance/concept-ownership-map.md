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

🆕 **شفاف‌سازی مرز «مالک» در برابر «زیرمجموعه» (سبک سینمایی):** DNA Manager (۰۲) مالک **تصمیم کلان** است — این‌که پروژه اصلاً چه Visual Style ای دارد (`enum VisualStyle`، ۳۴ مقدار) و آیا این تصمیم Soft-Lock شده یا نه. Visual Identity (۰۳)، که هم Style Matrix و هم Cinematic Language را در بر می‌گیرد، مالک **جزئیات اجرایی** است — یعنی این‌که آن تصمیم کلان چطور به پارامترهای دقیق‌تر (مثل `CinematicMode`: Long-take/Fast-cut/Balanced، یا میزان انطباق یک Shot با سبک انتخاب‌شده) تبدیل می‌شود. به عبارت دیگر: ۰۲ **چه چیزی** انتخاب شده را نگه می‌دارد؛ ۰۳ **چگونه** آن انتخاب در سطح Scene/Shot اجرا می‌شود را تعیین می‌کند. این دقیقاً همان رابطه‌ای است که بین «جزئیات استایل» (Style Matrix) و «Asset Library» در ردیف پایین‌تر هم برقرار است — یک الگوی تکرارشونده در این نقشه: مالک کلان تصمیم می‌گیرد، مالک جزئیات اجرا می‌کند.
| تجزیه‌ی داستان کامل به Scene/Shot/Asset (با کمک AI بیرونی) | AI Story Breakdown (واحد ۰۱ب) | Human Override (تأیید نهایی قبل از اعمال قطعی همیشه با انسان) |
| ساختار روایت (Beat/Arc) | Story Wizard | Scene Engine (Secondary) |
| تعریف Scene | Scene Engine | DNA Manager (Constraints) |
| تعریف Shot | Shot Engine | Scene Engine (Inheritance) |
| دوربین | Camera System | Shot Engine (Context) |
| نورپردازی | Lighting System | Scene Engine (Base Mood) |
| محیط و آب‌وهوا | Environment Engine | Scene Engine (Default) |
| کاراکتر و سوژه | Character & Subject System | DNA Manager (Constraints — Hard Lock برای Continuity) |
| پرامپت نهایی (خنثی) | Prompt Engineering Core | Output Delivery System (فرمت‌دهی مخصوص هر مدل) |
| 🆕 جمع‌آوری داده برای پرامپت (`collectData`) | **استثنا از الگوی معمول:** مالکیت مفهومی با Prompt Engineering Core (۱۱) است — یعنی شکل و محتوای `PromptGenerationInput` را ۱۱ تعریف می‌کند — اما **پیاده‌سازی واقعی** این تابع (خواندن از دیتابیس) در لایه‌ی Repository واحد ۱۵ (Project Storage) قرار دارد، چون فقط آن لایه به Room دسترسی دارد | Shot Engine (۰۵، صاحب توابع `resolve*Settings` که `collectData` از آن‌ها استفاده می‌کند) |
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
