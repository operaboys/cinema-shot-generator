# ADR-026: تصمیمات پیاده‌سازی — Renderer JSON Truncation + Prompt Finalization آگاه از JSON

**تاریخ:** 2026-07-21
**وضعیت:** رفع دو مشکل مرتبط کشف‌شده در مرز Renderer (واحد ۱۴، JSON از ADR-025) و Prompt Finalization Pipeline (واحد ۱۳).

## Context

بعد از ADR-025 (رندر JSON واقعی)، دو مشکل مرتبط باقی مانده بود:
1. `render()` برای `format.type=="json"` مستقیماً `buildJsonPrompt` را صدا می‌زند، نه از `optimizeForProfile` (که کوتاه‌سازی رشته‌ای دارد) — JSON خروجی می‌تواند بدون محدودیت طول رشد کند.
2. `finalizePrompt` (واحد ۱۳) مستقیماً `rendered.formattedPrompt` خام را به `cleanPrompt` می‌دهد؛ `cleanPrompt` فرض می‌کند ورودی یک جمله‌ی متن آزاد انگلیسی است — روی یک رشته‌ی JSON واقعی، `applyFinalPolish` (Phase 5) بعد از `}` پایانی یک نقطه اضافه می‌کند، که JSON را نامعتبر می‌کند.

## نظر تخصصی درباره‌ی پلن معمار

معمار پیشنهاد داده بود بین دو گزینه انتخاب شود: (الف) کلاً `cleanPrompt` روی JSON اجرا نشود (فقط `checkTokenLimit`)، یا (ب) JSON را Parse کرده، `cleanPrompt` را فقط روی مقادیر رشته‌ای هر فیلد اجرا کند و دوباره Serialize کند.

**توصیه: گزینه‌ی (ب)، با یک اصلاح مهم.** دلیل رد گزینه‌ی (الف): ۹ از ۱۳ پروفایل مدل موجود `format.type=="json"` دارند — یعنی گزینه‌ی (الف) عملاً کل ارزش واقعی واحد ۱۳ (تشخیص/حل تضاد، حذف مترادف تکراری، حذف کلمات پرکننده) را برای اکثریت پروفایل‌های واقعی غیرفعال می‌کرد، نه فقط یک لبه‌ی نادر. این خیلی بیشتر از یک «محافظه‌کاری امن» است — یک رگرسیون واقعی در کیفیت خروجی برای بیشتر مدل‌ها.

**اصلاح مهم نسبت به پلن خام معمار:** اجرای مستقیم `cleanPrompt` (بدون تغییر) روی هر مقدار فیلد به‌تنهایی مشکل‌ساز است، چون Phase 5 آن (`applyFinalPolish`) حرف اول را بزرگ می‌کند و نقطه‌ی پایانی الزامی می‌کند — این برای یک **جمله‌ی کامل رندرشده** منطقی است، اما برای یک **مقدار تکی فیلد** (مثل `"camera": "eye level, medium shot"`) بی‌معنا و حتی گمراه‌کننده است (نتیجه می‌شد `"Eye level, medium shot."` — انگار یک جمله‌ی مستقل است، نه یک فیلد ساختاریافته). راه‌حل: یک فیلد جدید `CleaningOptions.applyFinalPolish: Boolean = true` اضافه شد (پیش‌فرض `true` رفتار موجود را کاملاً حفظ می‌کند) تا Phase 5 هنگام Clean کردن مقادیر تکی فیلد JSON غیرفعال شود (`options.copy(applyFinalPolish = false)`)، در حالی‌که Phase های ۱ تا ۴ (تشخیص/حل تضاد، حذف مترادف، حذف Stop Word، فشرده‌سازی) که مستقل از «آیا این یک جمله‌ی کامل است یا یک عبارت» هستند، طبیعتاً روی هر فیلد اجرا می‌شوند.

**محدودیت پذیرفته‌شده‌ی این رویکرد (باید صادقانه گفته شود):** چون هر فیلد جدا Clean می‌شود، `detectConflicts` دیگر نمی‌تواند تضاد **بین‌فیلدی** را ببیند (مثلاً «آفتابی» در `environment` و «بارانی» در `lighting`، دو فیلد جدا) — فقط تضاد **درون یک فیلد واحد** تشخیص داده می‌شود. این یک محدودیت واقعی و جدید (نسبت به مسیر متن مسطح قبلی) است که رفع نشد — پذیرفته شد چون رفعش نیاز به یک لایه‌ی تشخیص تضاد سراسری (روی همه‌ی فیلدها هم‌زمان، جدا از حذف/Clean کردن هرکدام) دارد که خارج از تناسب این قدم بود.

**پلن Truncation (بخش ۳ پلن معمار):** کاملاً پذیرفته شد — با بررسی کد، مشخص شد `render()` از قبل (از خودِ ADR-025) دقیقاً همین رفتار را داشت: خروجی JSON هرگز کوتاه نمی‌شد. یعنی هیچ تغییر کد واقعی در این بخش لازم نبود — فقط این رفتار عمداً بودن آن مستند و با تست قفل شد (پایین).

## تصمیم ۱: `render()` — بدون تغییر کد، فقط مستندسازی + تست

بررسی نشان داد `render()` از قدم قبل (ADR-025) از قبل مسیر JSON را کوتاه نمی‌کند (شاخه‌ی `"json" -> buildJsonPrompt(...)`، بدون `optimizedText`). `validatePromptLength` موجود (بدون هیچ تغییری — فقط `renderedText.length` را می‌سنجد، به شکل داخلی متن کاری ندارد) از قبل به‌درستی روی طول یک رشته‌ی JSON هم کار می‌کند. پس اقدام واقعی فقط این بود: یک کامنت توضیحی در `Renderer.kt` اضافه شد که این رفتار را صریحاً «تصمیم» اعلام می‌کند (نه یک شکاف تصادفی)، و دو تست جدید در `RendererTest.kt` این رفتار را قفل می‌کنند (JSON طولانی هرگز بریده نمی‌شود و معتبر می‌ماند؛ `validatePromptLength` هنوز Warning درست می‌دهد).

## تصمیم ۲: `finalizePrompt` — تشخیص `format.type=="json"` + Parse/Clean/Re-serialize با Fallback ایمن

```kotlin
val cleanedText = if (profile.format.type == "json") {
    cleanJsonPromptValues(rendered.formattedPrompt, options)
        ?: cleanPrompt(rendered.formattedPrompt, options).first
} else {
    cleanPrompt(rendered.formattedPrompt, options).first
}
```

`cleanJsonPromptValues` (تابع خصوصی جدید در `PromptFinalizationPipeline.kt`) با `kotlinx.serialization.json` (`Json.parseToJsonElement`, `JsonObject`, `JsonPrimitive`) — نه String Manipulation دستی — JSON را Parse می‌کند، `mapValues` را روی هر فیلد اجرا می‌کند: اگر مقدار یک `JsonPrimitive` رشته‌ای باشد، `cleanPrompt(value.content, options.copy(applyFinalPolish=false)).first` جایگزینش می‌کند؛ در غیر این صورت (مثل Object تودرتوی `weightedEmphasis` که مقادیرش Float هستند) دست‌نخورده کپی می‌شود. نتیجه دوباره با `JsonObject(...).toString()` Serialize می‌شود.

## تصمیم ۳: Fallback ایمن وقتی Parse شکست بخورد (نه Throw)

**یافته‌ی حیاتی هنگام بررسی رگرسیون:** یک تست موجود (`PromptFinalizationPipelineTest.finalizePrompt cleans the rendered text and reports token usage...`) یک `profile` با `format.type=="json"` دارد اما `rendered.formattedPrompt` را عمداً **متن آزاد** (نه JSON واقعی) می‌گذارد — این تست از قبل از ADR-025 نوشته شده بود، از زمانی که هیچ پروفایلی واقعاً JSON تولید نمی‌کرد. اگر `cleanJsonPromptValues` روی Parse شکست‌خورده Throw می‌کرد، این تست موجود می‌شکست — که مستقیماً قانون صریح این قدم را نقض می‌کرد («تمام تست‌های موجود ... باید بدون تغییر pass شوند»).

**تصمیم:** `cleanJsonPromptValues` با `runCatching { Json.parseToJsonElement(...) }.getOrNull() as? JsonObject` پیاده شد — اگر Parse شکست بخورد یا نتیجه یک Object نباشد، `null` برمی‌گرداند؛ `finalizePrompt` در این حالت به مسیر متن آزاد قبلی (`cleanPrompt` روی کل رشته) Fallback می‌کند، نه Crash. این هم آن تست قدیمی را دقیقاً بدون تغییر Pass نگه می‌دارد، هم به‌طور کلی رفتاری دفاعی و منطقی است: یک `RenderedOutput` که ادعا می‌کند JSON است (طبق پروفایلش) ولی واقعاً نیست، یک وضعیت غیرمنتظره است که بهتر است با تنزل درجه (Graceful Degradation) مدیریت شود، نه شکست برنامه.

## تصمیم ۴: `checkTokenLimit` بدون تغییر

`estimateTokensFromWords` (تخمین کلمه‌محور) روی یک رشته‌ی JSON بازسازی‌شده هم اجرا می‌شود — دقتش برای متن JSON (که علائم نگارشی ساختاری مثل `{`, `"`, `:` بدون فاصله به کلمات می‌چسبند) از قبل هم یک تخمین (نه عدد رسمی توکنایزر واقعی) بود؛ این قدم آن را نه بدتر و نه بهتر نکرد — تغییری در `TokenCostCalculator.kt` لازم نبود.

## Backward Compatibility

- `CleaningOptions.applyFinalPolish` مقدار پیش‌فرض `true` دارد — هیچ فراخوانی موجود `cleanPrompt`/`finalizePrompt` (که این فیلد را صریح نمی‌دهد) رفتارش تغییر نمی‌کند.
- تست موجود با پروفایل json+متن‌آزاد (بالا) بدون تغییر Pass می‌شود (از طریق Fallback).
- یک تست رگرسیون صریح جدید برای یک پروفایل کاملاً غیر-JSON (`plain_text`) اضافه شد که ثابت می‌کند خروجی `finalizePrompt` کلمه‌به‌کلمه با فراخوانی مستقیم `cleanPrompt` یکسان است.

## تست

- `RendererTest.kt` (۲ تست جدید): JSON طولانی هرگز بریده نمی‌شود و معتبر می‌ماند؛ `validatePromptLength` هنوز روی JSON طولانی Warning درست می‌دهد.
- `PromptCleanerTest.kt` (۲ تست جدید): `applyFinalPolish=false` واقعاً Phase 5 را غیرفعال می‌کند؛ پیش‌فرض بدون تغییر.
- `PromptFinalizationPipelineTest.kt` (۵ تست جدید + ۱ تست موجود بدون تغییر): JSON واقعی بعد از `finalizePrompt` همچنان معتبر/Parse‌پذیر است؛ مترادف تکراری داخل یک فیلد پاک می‌شود بدون خراب‌کردن فیلدهای دیگر؛ هیچ Polish اجباری (بزرگ‌کردن حرف اول/نقطه) روی مقدار تکی فیلد اعمال نمی‌شود؛ Object تودرتوی `weightedEmphasis` دست‌نخورده می‌ماند؛ رگرسیون صریح پروفایل غیر-JSON.

همه در همان اجرای اول موفق شدند — بدون باگ جدید (این قدم عمدتاً یک بازبینی معماری/مستندسازی + یک رفع واقعی بود، نه اکتشاف باگ‌های غیرمنتظره‌ی جدید).

## خارج از Scope (بدون تغییر)

- تشخیص تضاد بین‌فیلدی برای JSON (بالا، محدودیت پذیرفته‌شده).
- تخمین توکن دقیق‌تر برای متن JSON.
- `ModelProfile`/`ModelFormat`/`RenderedOutput`/`buildJsonPrompt` (ADR-025) — بدون تغییر ساختار.
- UI.

## Consequences

- **آسان می‌شود:** هر ۹ پروفایل JSON اکنون هم از رندر JSON واقعی (ADR-025) هم از Cleaning واقعی فیلدهای متنی (این ADR) بهره می‌برند، بدون خطر خراب‌شدن ساختار JSON در مسیر Prompt Finalization.
- **بدهی مستند جدید:** عدم تشخیص تضاد بین‌فیلدی برای پروفایل‌های JSON (بالا).
