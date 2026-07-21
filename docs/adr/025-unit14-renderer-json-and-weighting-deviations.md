# ADR-025: تصمیمات پیاده‌سازی واحد ۱۴ — رفع دو محدودیت شناخته‌شده‌ی Renderer (ADR-021)

**تاریخ:** 2026-07-21
**وضعیت:** رفع محدودیت ۱ (رندر JSON واقعی) و محدودیت ۲ (وزن‌دهی واقعی Stable Diffusion) که در `docs/adr/021-unit14-model-profiles-deviations.md` مستند شده بودند.

## Context

ADR-021 دو محدودیت شناخته‌شده‌ی `Renderer.kt` را عمداً بدون رفع مستند کرد (چون آن قدم فقط باید داده اضافه می‌کرد، نه Renderer را گسترش می‌داد): (۱) `render()` هیچ ساختار JSON واقعی تولید نمی‌کرد — حتی برای پروفایل‌هایی با `format.type == "json"`، خروجی از همان مسیر متن ساده (`else -> optimizedText`) عبور می‌کرد؛ (۲) `applyWeightSyntax` فقط `platform == "midjourney"` را ویژه می‌کرد، پس نحو وزن‌دهی واقعی Stable Diffigate (`(tag:weight)`) هرگز اعمال نمی‌شد با اینکه `stableDiffusionProfile.supportsWeightedTags = true` بود.

**یافته‌ی مقدماتی مهم:** با grep در `docs/blueprints/14-output-delivery.md`، تأیید شد که کد مفهومی خودِ بلوپرینت هم دقیقاً همین دو محدودیت را دارد — یعنی `render()`/`applyWeightSyntax` بلوپرینت هم کلمه‌به‌کلمه همین رفتار (`else -> optimizedText`، فقط `midjourney` در `applyWeightSyntax`) را دارند. تمام مثال‌های JSON بلوپرینت (خط ۳۴، ۶۹) مربوط به **تعریف پروفایل مدل** هستند (`{"profile_id": ..., "capabilities": {...}}`)، نه نمونه‌ای از پرامپت نهایی رندرشده. یعنی بلوپرینت هیچ‌جا شکل دقیق JSON خروجی نهایی را مشخص نکرده — ساختار زیر یک طراحی مستقل و معقول است، نه استخراج از یک نمونه‌ی مستند.

## تصمیم ۱: ساختار JSON خروجی

`buildJsonPrompt` (تابع خصوصی جدید در `Renderer.kt`) مستقیماً از `blueprint.structuredParts` (نه از متن مسطح‌شده‌ی `renderBlueprintToText`) یک `JsonObject` واقعی می‌سازد، با `kotlinx.serialization.json` (`buildJsonObject`/`put`/`putJsonObject`) — نه String concatenation دستی، طبق دستور صریح، تا Escape کاراکترهای خاص (نقل‌قول، خط جدید) درست و تضمین‌شده انجام شود:

```json
{
  "subject": "...", "scene": "...", "shot": "...", "camera": "...", "lighting": "...",
  "environment": "...", "style": "...", "timeline": "...", "audio": "...",
  "weightedEmphasis": { "tag": 1.2 }
}
```

- `environment`/`timeline`/`audio` فقط وقتی مقدارشان `null` نباشد اضافه می‌شوند (طبق nullability واقعی `StructuredParts`).
- `timeline`/`audio` فقط وقتی `profile.capabilities.supportsVideo` باشند اضافه می‌شوند — دقیقاً هم‌راستا با شرط مشابه در `renderBlueprintToText` (خط ۳۲/۳۳ همین فایل)، نه یک قانون جدید مستقل.

## تصمیم ۲: چرا از `structuredParts` خام، نه از متن `optimizeForProfile`

اگر JSON را از متن مسطح‌شده (`optimizedText`) می‌ساختیم، یا باید کل پاراگراف را در یک فیلد واحد می‌گذاشتیم (بی‌فایده برای یک API واقعی JSON که فیلدهای جدا می‌خواهد) یا باید Parse معکوس پاراگراف به فیلدها می‌کردیم (غیرممکن با اطمینان). ساخت مستقیم از `structuredParts` فیلدهای جدا و معنادار تضمین می‌کند — دقیقاً همان چیزی که یک API واقعی مثل Veo (که پارامترهای ساختاریافته می‌پذیرد، نه یک بلوک متن خام) انتظار دارد.

**اثر جانبی پذیرفته‌شده:** `optimizeForProfile` (که هنوز قبل از `when` در `render()` فراخوانی می‌شود) برای پروفایل‌های JSON محاسبه می‌شود ولی مقدارش در شاخه‌ی `"json"` استفاده نمی‌شود — این یعنی کوتاه‌سازی خودکار `maxPromptLength` روی خروجی JSON اعمال نمی‌شود (فقط روی مسیر متن مسطح که دیگر استفاده نمی‌شود). این خارج از Scope این قدم دانسته شد (دستور کار صراحتاً نگفت رفتار Truncation باید برای JSON هم طراحی شود)؛ اگر در آینده لازم شد، به یک استراتژی Truncation فیلد-به-فیلد جدا نیاز دارد که این قدم آن را طراحی نکرد — به‌عنوان بدهی مستند ثبت شد.

## تصمیم ۳: `weightedEmphasis` در JSON — Object جدا، نه اعمال `applyWeightSyntax` روی متن

طبق دستور صریح، تصمیم زیر گرفته شد: برای پروفایل‌های JSON، `applyWeightSyntax` (جایگزینی رشته‌ای درون متن، طراحی‌شده برای Midjourney/SD) **اصلاً روی فیلدهای JSON اجرا نمی‌شود**. در عوض، اگر `profile.capabilities.supportsWeightedTags == true` و `blueprint.weightedEmphasis` غیرخالی باشد، یک `weightedEmphasis: {tag: weight, ...}` جدا به JSON اضافه می‌شود.

**دلیل:** برای یک فرمت JSON ساختاریافته، «وزن» باید یک پارامتر مجزا باشد (چیزی که یک API واقعی به‌طور طبیعی می‌پذیرد)، نه متنی که با `::` یا `(...)` دستکاری شده — دستکاری متنی فقط برای فرمت‌های Command String/Tag-based (Midjourney/SD) معنا دارد، جایی که کل پرامپت یک رشته‌ی واحد است. با grep در `ModelProfiles.kt` تأیید شد **هیچ پروفایل موجودی هم‌زمان `format.type=="json"` و `supportsWeightedTags=true` ندارد** — پس این مسیر فعلاً برای هیچ پروفایل واقعی فعال نمی‌شود؛ این یک تصمیم مستند برای پروفایل‌های احتمالی آینده است، نه رفع یک باگ فعلی. (`optimizedText` محاسبه‌شده در `render()` همچنان ممکن است نحو وزن‌دهی متنی داشته باشد اگر `supportsWeightedTags=true` باشد — اما چون شاخه‌ی `"json"` از `optimizedText` استفاده نمی‌کند، هیچ تداخل واقعی‌ای رخ نمی‌دهد.)

## تصمیم ۴: `applyWeightSyntax` — افزودن `stable_diffusion`، `when` صریح نه Registry عمومی

```kotlin
fun applyWeightSyntax(text: String, tag: String, weight: Float, profile: ModelProfile): String {
    return when (profile.platform) {
        "midjourney" -> text.replace(tag, "$tag::$weight")
        "stable_diffusion" -> text.replace(tag, "($tag:$weight)")
        else -> text
    }
}
```

نحو `(tag:weight)` دقیقاً همان نحو معروف A1111/ComfyUI (SD) است که در ADR-021 به‌عنوان «غیرفعال (inert)» مستند شده بود. یک `when` صریح با `else` (نه Map/Registry قابل‌تنظیم) انتخاب شد چون فقط دو پلتفرم واقعی نحو دارند — افزودن پلتفرم سوم در آینده فقط یک `case` جدید نیاز دارد؛ ساخت یک Registry عمومی برای دو مورد Over-engineering بود (طبق اصل تناسب تلاش با اندازه‌ی تغییر).

## Backward Compatibility (راستی‌آزمایی رگرسیون)

- `renderBlueprintToText`/`optimizeForProfile` **هیچ تغییری نکردند** — فقط `applyWeightSyntax` (که از داخل `renderBlueprintToText` صدا زده می‌شود) یک `case` جدید گرفت؛ رفتار قبلی `midjourney`/بقیه‌ی پلتفرم‌ها دست‌نخورده ماند.
- `render()` فقط یک `"json" ->` جدید به `when` اضافه کرد؛ شاخه‌های `"command_string"`/`"plain_text"`/`else` دقیقاً همان کد قبلی‌اند.
- تست‌های رگرسیون صریح جدید در `RendererTest.kt` تأیید می‌کنند خروجی `universal_default` (plain_text) و یک پروفایل `command_string` **کلمه‌به‌کلمه** با قبل یکسان است (با فراخوانی مجدد `optimizeForProfile` و مقایسه‌ی مستقیم).
- تمام تست‌های موجود `RendererTest`/`ModelProfilesTest` بدون تغییر در انتظارات (assertions) موجود، pass شدند — فقط کامنت توضیحی یک تست در `ModelProfilesTest.kt` که این محدودیت را «شناخته‌شده» می‌خواند، برای صحت به‌روزرسانی شد (بدون تغییر در خودِ assertion ها).

## تست

`RendererTest.kt` (۸ تست جدید): JSON واقعی و Parse‌پذیر با کلیدهای اصلی پر؛ حذف `timeline`/`audio` وقتی پروفایل ویدیو پشتیبانی نمی‌کند؛ عدم وجود `weightedEmphasis` وقتی `supportsWeightedTags=false`؛ وجود `weightedEmphasis` به‌عنوان Object جدا برای یک پروفایل فرضی json+weightedTags؛ نحو `(tag:weight)` برای `stable_diffusion`؛ نحو میجرنی بدون تغییر؛ بدون نحو برای پلتفرم ناشناخته؛ دو تست رگرسیون صریح (`universal_default`، `command_string`).

## خارج از Scope (بدون تغییر)

- `ModelProfile`/`ModelFormat`/`ModelCapabilities`/`ModelConstraints` — بدون تغییر ساختار.
- JSON یا Weighted Syntax برای فرمت‌های دیگر (`structured_text` برای Runway) — خارج از این دو محدودیت مستندشده.
- Truncation فیلد-به-فیلد برای خروجی JSON (بدهی مستند، بالا).
- UI.

## Consequences

- **آسان می‌شود:** هر ۹ پروفایل با `format.type=="json"` (Veo، Kling، Seedance، HappyHorse، Luma، Hailuo، Wan، HunyuanVideo، LTX، Vidu) اکنون واقعاً JSON معتبر و Parse‌پذیر تولید می‌کنند؛ Stable Diffusion اکنون نحو وزن‌دهی واقعی‌اش را دارد (دیگر inert نیست).
- **بدهی مستند جدید:** Truncation برای خروجی JSON طراحی نشده (بالا)؛ ساختار JSON یک طراحی مستقل است، نه تأییدشده توسط API واقعی هیچ مدل (چون این پروژه به هیچ API واقعی وصل نمی‌شود — طبق محدودیت کلی پروژه، فقط تولید متن/JSON محلی).
