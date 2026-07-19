# ADR-001: تصمیمات پیاده‌سازی واحد ۰۱ (Story & Override) — لایه‌ی دامنه

**تاریخ:** 2026-07-19
**وضعیت:** تأییدشده (توسط کاربر/معمار در جریان تأیید پلن قدم اجرایی واحد ۰۱)

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۰۱ طبق `docs/blueprints/01-story-and-override.md`، بدون Room (واحد ۱۵)، بدون UI، و بدون State & Versioning واقعی (واحد ۱۲). در حین پیاده‌سازی چند نقطه‌ی مبهم/متناقض در بلوپرینت پیدا شد که تصمیم صریح لازم داشتند.

## تصمیمات

### ۱. Rule 2 (محدودیت Documentary) — تفسیر بر مبنای Genre
**مشکل:** قانون می‌گوید «اگر Story Type = Documentary...» ولی enum `StoryType` در همان بلوپرینت مقدار `DOCUMENTARY` ندارد (Documentary فقط در `Genre` است)؛ کد مفهومی بلوپرینت هم این نقطه را placeholder گذاشته بود.
**گزینه‌ها:** (الف) تفسیر بر مبنای Genre؛ (ب) افزودن DOCUMENTARY به StoryType (تغییر Schema)؛ (ج) پیاده نکردن Rule 2 تا اصلاح بلوپرینت.
**تصمیم (تأییدشده):** گزینه‌ی الف — اگر `genre` شامل `DOCUMENTARY` باشد، کل لیست باید زیرمجموعه‌ی `{DOCUMENTARY, DRAMA}` باشد؛ نقض آن Blocking است.
**چرا:** بدون تغییر Schema نزدیک‌ترین معنا به نیّت قانون است. اگر معمار بعداً DOCUMENTARY را به StoryType اضافه کند، این قانون باید بازبینی شود.

### ۲. Rule 4 (completion_status) — فقط تابع محاسبه
**تصمیم (تأییدشده):** `validateStoryContext` مقدار `completionStatus` را بررسی نمی‌کند؛ به‌جایش `deriveCompletionStatus(context)` ارائه شد که مقدار درست را محاسبه می‌کند (COMPLETE ⇔ همه‌ی فیلدهای الزامی پر؛ در این نوع یعنی genre ناخالی). مصرف‌کننده (ویزارد در UI، واحدهای بعدی) موظف است مقدار را از این تابع بگیرد.

### ۳. امضای revokeOverride — نسخه‌ی خالص (انحراف تأییدشده از بلوپرینت)
**مشکل:** امضای بلوپرینت `revokeOverride(overrideId, reason): Result<Unit>` بدون Persistence هیچ منطق قابل‌تستی ندارد.
**تصمیم (تأییدشده):** `revokeOverride(override: HumanOverride, reason: String?): Result<HumanOverride>` — کپی Revoke شده (active=false، revoked=true، revokedAt، revokedReason) برمی‌گرداند و رویداد `override_revoked` را ثبت می‌کند. بازگردانی `original_value` در وضعیت واقعی پروژه متعلق به واحدهای ۱۲/۱۵ است و هنگام اتصال آن‌ها، امضای مبتنی بر Id در لایه‌ی Repository ساخته می‌شود.

### ۴. Placeholder برای logOverrideEvent
`fun interface OverrideEventLogger { fun log(event: OverrideEvent) }` با پیاده‌سازی پیش‌فرض `NoOp`. واحد ۱۲ (State & Versioning) بعداً پیاده‌سازی واقعی را ارائه می‌کند. `OverrideEvent(type, overrideId, scope?)` حداقلِ داده‌ی موردنیاز ثبت خودکار Scope را حمل می‌کند.

### ۵. توابع non-suspend (انحراف از کد مفهومی بلوپرینت)
کد مفهومی بلوپرینت `createOverride`/`revokeOverride` را `suspend` نوشته است. منطق خالص فعلی هیچ نقطه‌ی تعلیقی ندارد و kotlinx-coroutines هنوز وابستگی پروژه نیست؛ افزودن آن خارج از Scope این قدم بود. واحدهای ۱۲/۱۵ در لایه‌ی Repository تعلیق را اضافه می‌کنند.

### ۶. ValidationResult محلی واحد ۰۱
`ValidationResult`/`ValidationIssue` فعلاً در `domain/story` تعریف شده‌اند. مالک نهایی این نوع‌ها واحد ۰۷ (Validation & Consistency) است؛ هنگام پیاده‌سازی آن واحد، این نوع‌ها به محل سراسری مهاجرت می‌کنند. به همین ترتیب، خطاها فعلاً با `IllegalStateException` (مطابق کد مفهومی بلوپرینت) برگردانده می‌شوند و مهاجرت به سلسله‌مراتب `CinemaShotError` (تعریف‌شده در ai-coding-guidelines) وقتی انجام می‌شود که فایل مشترک آن ساخته شود.

### ۷. افزودن junit:junit:4.13.2 (testImplementation)
ماژول app هیچ وابستگی تستی نداشت و بدون آن اجرای واقعی `./gradlew test` ممکن نبود؛ افزودن این وابستگی توسط کاربر تأیید شد. جزئیات تزریق‌پذیر (idProvider/clock/logger با مقادیر پیش‌فرض عملیاتی) صرفاً برای تست‌پذیری بدون فریمورک Mock اضافه شدند.

## Consequences

- **آسان می‌شود:** تست واحد خالص بدون Android/Mock؛ اتصال بعدی واحد ۱۲ فقط با پیاده‌سازی `OverrideEventLogger`؛ اتصال DNA Manager با مصرف `StoryContext`.
- **سخت/بدهی:** دو مهاجرت آینده ثبت شد (ValidationResult → واحد ۰۷، خطاها → CinemaShotError)؛ Rule 2 در صورت تغییر Schema باید بازبینی شود؛ امضای Id-محور revoke در لایه‌ی Repository ساخته خواهد شد.
