# ADR-028: Migration واحد ۰۵ (Shot Engine) — negativePromptOverride

**تاریخ:** 2026-07-28
**وضعیت:** Migration کوچک کامل شد؛ یک بررسی معماری (اتصال به `PromptGenerationInput`) انجام شد و نتیجه‌اش گزارش شد، بدون اجرا (طبق تصریح معمار).

## Context

`docs/blueprints/05-shot-engine-v2.md` (نسخه ۳) یک فیلد جدید به `Shot` اضافه می‌کند: `negativePromptOverride: String? = null`. منبع ارث‌بری‌اش (برخلاف camera/lighting/environment که از Scene ارث می‌برند) `domain.dna.QualityDirectives.negativePrompt` (واحد ۰۲، Migration ADR-027) است.

## پیش‌بررسی (grep، طبق دستور کار)

- `data class Shot` در `domain/shot/ShotModels.kt` از قبل فیلدهای `camera`/`lighting`/`environment` را داشت (تأیید شد؛ این‌ها در یک Migration قبلی — ADR-013 — اضافه شده بودند، نه در این قدم).
- `resolveCameraSettings`/`resolveLightingSettings`/`resolveEnvironmentSettings` در `domain/shot/ShotSettingsResolution.kt` بودند (تأیید مطابق ADR-013، همان‌جایی که بلوپرینت هم پیش‌بینی کرده بود) — `resolveNegativePrompt` هم به همین فایل اضافه شد.
- **یافته‌ی جانبی (بدون نیاز به اقدام):** `SourcedSettings<T>` واقعی پروژه شکلی متفاوت از نمونه‌ی «مفهومی» بلوپرینت v3 دارد (`source: String`/`overrideValue: T?` در برابر `source: SettingsSource`/`settings: T` بلوپرینت) — این یک واگرایی از پیش‌شناخته‌شده و حل‌شده در ADR-013 است («کد واقعی و تست‌شده اولویت دارد»)، نه یافته‌ی جدید این قدم؛ دست‌نخورده ماند، کاملاً خارج از Scope این Migration کوچک.

## پیاده‌سازی

- `Shot.negativePromptOverride: String? = null` — بلافاصله بعد از `soundProfile`، قبل از `characterIds` (دقیقاً موقعیت بلوپرینت).
- `resolveNegativePrompt(shot: Shot, dnaNegativePrompt: String): String` — امضا با `String` (نه `ProjectDna` کامل)، دقیقاً طبق پیشنهاد صریح بلوپرینت («تا وابستگی غیرضروری ایجاد نشود»).

## بررسی سازگاری با سازنده‌های موجود `Shot(...)`

با grep در کل پروژه (`\bShot(` با word boundary، برای پرهیز از تطبیق کاذب با `ShotEntity(`/`ShotDao(` و غیره)، ۵ محل واقعی ساخت `Shot` پیدا شد: `data/repository/DtoMappers.kt` (تبدیل `ShotDto.toDomain()`) و ۴ فایل تست (`SettingsResolutionRepositoryTest.kt`, `PromptGenerationRepositoryTest.kt`, `ShotSettingsResolutionTest.kt`, `PromptAssemblyTest.kt`). **هر پنج محل صرفاً از Named Arguments استفاده می‌کنند** — هیچ فراخوانی Positional ای وجود نداشت، پس افزودن فیلد جدید در وسط لیست پارامترها (نه فقط انتهای آن) هیچ خطر Silent Breakage ای نداشت.

## بررسی معماری درخواستی: آیا `PromptGenerationInput` باید این مقدار را داشته باشد؟ (فقط بررسی، بدون اجرا)

با grep در `PromptGenerationRepository.kt` (واحد ۱۵) و `domain/promptengine/`/`domain/outputdelivery/` (واحد ۱۱/۱۴) تأیید شد:

- `collectData` (واحد ۱۵) از قبل هم `shot` کامل (شامل `negativePromptOverride` تازه‌افزوده) و هم `dna` کامل (شامل `qualityDirectives.negativePrompt`) را بارگذاری و در `PromptGenerationInput` می‌گذارد — یعنی **هر دو ماده‌ی خام لازم برای `resolveNegativePrompt` از قبل در دسترس مصرف‌کننده‌ی نهایی هستند**، بدون نیاز به تغییر `collectData`.
- اما `PromptGenerationInput` (`domain/promptengine/PromptEngineModels.kt`) هیچ فیلد مجزایی برای «Negative Prompt نهایی Resolve‌شده» ندارد.
- **یافته‌ی مهم‌تر:** با grep در کل `domain/promptengine/` و `domain/outputdelivery/` (شامل `PromptAssembly.kt`, `StructuredParts`, `Renderer.kt`, `buildJsonPrompt`) هیچ ارجاعی به `negativePrompt`/`negativePromptOverride` پیدا نشد — یعنی مفهوم Negative Prompt فعلاً **در هیچ نقطه‌ای از مسیر واقعی Blueprint→Render مصرف نمی‌شود**، نه فقط این‌که به `PromptGenerationInput` نرسیده. این بزرگ‌تر از یک اتصال ساده‌ی «یک فیلد جا افتاده» است — کل زنجیره (`StructuredParts` → `Renderer`/`buildJsonPrompt` → مدل‌هایی که `supportsNegativePrompt=true` دارند) هنوز برای Negative Prompt طراحی/سیم‌کشی نشده.

**پیشنهاد (اجرا نشد، طبق تصریح معمار):** یک قدم Migration جداگانه لازم است که:
1. `StructuredParts` (یا `PromptGenerationInput`) واحد ۱۱ را با یک فیلد `negativePrompt: String` (حاصل `resolveNegativePrompt`) گسترش دهد — این فراخوانی باید در `assemblePromptBlueprint` یا در خودِ `collectData` انجام شود (به تصمیم مجزا نیاز دارد: کدام لایه مسئول Resolve باشد).
2. `Renderer`/`buildJsonPrompt` (واحد ۱۴) این مقدار را فقط برای پروفایل‌هایی که `capabilities.supportsNegativePrompt=true` دارند اعمال کند (طبق تصریح بلوپرینت ۰۲: «در مدل‌هایی که پشتیبانی نمی‌کنند، بی‌صدا نادیده گرفته می‌شود، نه خطا») — این تصمیم به Renderer واگذار شده، نه به این واحد.

## تست

۳ تست جدید در `ShotSettingsResolutionTest.kt`: Override دستی (حتی رشته‌ی خالی) اولویت دارد؛ Override دستی غیرخالی اولویت دارد؛ نبود Override → fallback به مقدار DNA.

## خارج از Scope این قدم (بدون تغییر)

- اتصال `PromptGenerationInput`/`StructuredParts`/`Renderer` به `resolveNegativePrompt` (بالا، پیشنهاد شد، اجرا نشد).
- Rule ۸ بلوپرینت («`negative_prompt_override` نباید کاملاً whitespace-only باشد») — دستور کار این قدم صراحتاً فقط دو مورد را خواسته بود (فیلد + تابع resolve)؛ Rule ۸ (و Rule های ۶/۷ که از پیش هم پیاده نشده بودند) جزو این دستور کار نبودند، پس اضافه نشدند — اگر لازم است، باید صریحاً در یک قدم جدا خواسته شود.

## Consequences

- **آسان می‌شود:** `Shot` اکنون دقیقاً با بلوپرینت ۰۵ نسخه ۳ هم‌راستاست؛ `resolveNegativePrompt` آماده‌ی استفاده توسط هر لایه‌ی بالاتر است.
- **بدهی مستند جدید:** کل مسیر واقعی مصرف Negative Prompt (واحد ۱۱→۱۴) هنوز طراحی/پیاده نشده (بالا).
