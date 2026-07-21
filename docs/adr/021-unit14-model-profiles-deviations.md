# ADR-021: تصمیمات پیاده‌سازی واحد ۱۴ — تکمیل Model Profile Library (۱۳ مدل واقعی بازار)

**تاریخ:** 2026-07-21
**وضعیت:** تکمیل بخش الف واحد ۱۴ (Output Delivery) — افزودن داده (نه تغییر معماری/بلوپرینت)

## Context

واحد ۱۴ تا این قدم فقط `universalDefaultProfile` را داشت. دستور کار خواست ۱۳ پروفایل مدل واقعی (بر اساس داده‌ی بازار جولای ۲۰۲۶ تأییدشده توسط معمار) اضافه شود، بدون تغییر `docs/blueprints/14-output-delivery.md` و بدون گسترش `Renderer.kt` مگر در حد لازم برای یک تست مشخص.

## تصمیم: بلوپرینت ۱۴ دست‌نخورده ماند

بلوپرینت ۱۴ صراحتاً جدول مدل‌هایش را «نقطه‌ی شروع، نه فهرست نهایی» اعلام کرده و معماری را Data-driven می‌خواهد. این قدم دقیقاً همان اصل را با افزودن یک فایل داده‌ی جدید (`ModelProfiles.kt`، فقط `val` های Kotlin) دنبال کرد — سند تغییر نکرد.

## تصمیم: Sora (OpenAI) عمداً حذف شد

طبق دستور صریح معمار، هیچ پروفایلی برای Sora ساخته نشد.

## یادداشت صداقت داده (اعمال‌شده روی هر ۱۳ پروفایل)

فقط `veoProfile.constraints.maxPromptLength = 2000` یک عدد واقعی است — مستقیماً از نمونه‌ی کامل Veo در خودِ بلوپرینت ۱۴ گرفته شد. هر عدد فنی دیگر (`maxTokens` همه‌ی مدل‌ها، و `maxPromptLength` هر ۱۲ مدل دیگر) یک **تخمین محافظه‌کارانه‌ی مستندشده** است، نه عدد رسمی API — طبق پیش‌تأیید صریح دستور کار که این نوع عدم‌قطعیت عددی «ابهام نیست، حدس مستند کن و ادامه بده» است. الگوی `maxTokens ≈ maxPromptLength / 4` از همان heuristic داخلی پروژه (`estimateTokensFromCharacters`، واحد ۱۳) برای هماهنگی گرفته شد، نه چون این استاندارد رسمی صنعتی است.

به همین ترتیب، هر جا `supportsNegativePrompt` بدون منبع رسمی مشخص بود (Seedance، HappyHorse، Luma، LTX، Vidu)، مقدار `false` (محافظه‌کارانه‌ترین حالت) انتخاب شد؛ جایی که مستندات API رسمی پارامتر Negative Prompt را تأیید می‌کرد (Kling، Runway، Hailuo/MiniMax، Wan، HunyuanVideo، Midjourney، Stable Diffusion)، `true` گذاشته شد.

## تصمیم: `ModelFormat.type` دو مقدار محلی جدید گرفت (بدون تغییر enum — چون از اول رشته‌ای بود)

- `"structured_text"` برای Runway Gen-4.5 — چون Runway به‌جای JSON خام یا Command String، فرمت متنی ساختاریافته‌ی خودش را می‌خواهد. چون `ModelFormat.type` از قبل یک `String` آزاد بود (نه enum بسته)، افزودن مقدار جدید نیازی به تغییر تایپ نداشت.
- Stable Diffusion از `type` جدید استفاده نکرد؛ در عوض `type = "plain_text"` با `structure = "tags"` گذاشته شد (توضیح کامل در بخش بعد).

## تصمیم: Stable Diffusion — فرمت «Tag Soup» با بازاستفاده از منطق موجود، نه کد جدید

دستور کار صراحتاً پرسید فرمت SD باید JSON باشد یا Command-style. پاسخ: هیچ‌کدام دقیقاً — پرامپت‌های واقعی SD/A1111/ComfyUI فهرستی از برچسب‌های جداشده با کاما هستند («tag soup»)، نه پاراگراف و نه JSON ساختاریافته.

با grep در `Renderer.kt` تأیید شد `renderBlueprintToText` از قبل این منطق را دارد:
```kotlin
segments.joinToString(if (profile.format.structure == "paragraph") ". " else ", ")
```
یعنی هر `structure` غیر از `"paragraph"` بخش‌ها را با کاما می‌چسباند — دقیقاً معادل سبک واقعی SD. پس به‌جای افزودن کد جدید به Renderer، `stableDiffusionProfile.format = ModelFormat(type = "plain_text", structure = "tags")` گذاشته شد تا از این رفتار موجود عیناً بازاستفاده کند. این یک تصمیم آگاهانه‌ی بازاستفاده از شرط موجود است، نه یک Placeholder ناقص.

## محدودیت شناخته‌شده ۱: `render()` هیچ فرمت JSON واقعی تولید نمی‌کند

با grep در `Renderer.kt` تأیید شد تابع `render()`:
```kotlin
when (profile.format.type) {
    "command_string" -> "${profile.format.commandPrefix} $optimizedText"
    "plain_text" -> optimizedText
    else -> optimizedText
}
```
فقط `"command_string"` را ویژه می‌کند؛ هر مقدار دیگری از جمله `"json"` و `"structured_text"` از همان مسیر `else -> optimizedText` عبور می‌کند — یعنی فعلاً **هیچ ساختار JSON واقعی تولید نمی‌شود**، حتی برای پروفایل‌هایی که `format.type = "json"` دارند (۹ مدل از ۱۳ مدل این قدم). این طبق دستور کار **عمداً اصلاح نشد** — فقط مستند شد. تست `renderBlueprintToText and render run without crashing across json and command_string formats` (در `ModelProfilesTest.kt`) این را بررسی و مستند می‌کند: فقط عدم کرش و اعمال واقعی `commandPrefix` برای Midjourney را تأیید می‌کند، نه یک تفاوت ساختاری JSON واقعی برای Veo.

## محدودیت شناخته‌شده ۲: `applyWeightSyntax` فقط برای `platform == "midjourney"` واقعاً کار می‌کند

با grep تأیید شد:
```kotlin
fun applyWeightSyntax(text: String, tag: String, weight: Float, profile: ModelProfile): String =
    if (profile.platform == "midjourney") text.replace(tag, "$tag::$weight") else text
```
این یافته دو تصمیم مستقیم را تعیین کرد:
1. `midjourneyProfile.platform` عمداً دقیقاً رشته‌ی `"midjourney"` گذاشته شد تا این مسیر واقعاً فعال شود — تنها پروفایل این فهرست که `supportsWeightedTags=true` واقعاً به رفتار متفاوت در Renderer منجر می‌شود.
2. `stableDiffusionProfile.supportsWeightedTags = true` گذاشته شد چون این ویژگی واقعاً در SD وجود دارد (نحو معروف `(tag:1.2)`)، اما با یادداشت صریح که فعلاً **غیرفعال (inert)** است — چون `applyWeightSyntax` فقط میجرنی را چک می‌کند. طبق دستور کار، `Renderer.kt` گسترش داده نشد تا نحو واقعی SD را پیاده کند؛ این یک بدهی مستند است، نه باگ رفع‌نشده.

## فایل‌های تغییریافته/افزوده‌شده (Scope دقیق طبق دستور کار)

- `app/src/main/java/com/operaboys/cinemashotgenerator/domain/outputdelivery/ModelProfiles.kt` (جدید) — ۱۳ `val ModelProfile` + `ALL_MODEL_PROFILES`.
- `app/src/test/java/com/operaboys/cinemashotgenerator/domain/outputdelivery/ModelProfilesTest.kt` (جدید).
- `docs/adr/021-unit14-model-profiles-deviations.md` (همین فایل).
- `README.md` (فهرست ۱۳ مدل پشتیبانی‌شده).

`Renderer.kt`/`ModelProfileLibrary.kt`/بلوپرینت ۱۴ **دست‌نخورده** ماندند — طبق تصمیم صریح معمار و پیش‌تأیید محدودیت‌های شناخته‌شده‌ی بالا.

## Consequences

- **آسان می‌شود:** انتخاب پروفایل (`selectProfile`/`validateProfileAvailability`) اکنون برای ۱۳ مدل واقعی بازار (به‌علاوه‌ی universal_default) کار می‌کند؛ افزودن مدل بعدی فقط نیاز به یک `val` جدید و افزودن به `ALL_MODEL_PROFILES` دارد — بدون تغییر کد دیگر.
- **بدهی مستند باقی‌مانده:** `render()` فرمت JSON واقعی تولید نمی‌کند (۹ پروفایل با `type="json"` فعلاً خروجی متنی ساده می‌گیرند)؛ `applyWeightSyntax` فقط برای Midjourney فعال است (نحو وزن‌دهی SD غیرفعال)؛ اعداد `maxPromptLength`/`maxTokens`/`supportsNegativePrompt` برای اکثر مدل‌ها تخمینی‌اند و باید هر زمان مستندات رسمی API در دسترس قرار گرفت به‌روزرسانی شوند — این کار خارج از Scope این قدم است.
