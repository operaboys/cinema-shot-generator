# ADR-116: اتصال کامل Style Matrix — قدم ۴-محتوا از ۸ زیرقدم: promptTokens غنی و واقعی برای هر ۳۴ مقدار VisualStyle

## زمینه

`domain/visualidentity/StyleMatrix.kt` از ابتدا (پیش از ADR-113)
`StyleReference(styleId, name, promptTokens)` و
`combineStyles(primary, secondary, influence)` را داشت، اما تا این قدم
**هیچ `StyleReference` واقعی برای هیچ‌کدام از ۳۴ مقدار `VisualStyle`
در کل کدبیس ساخته نمی‌شد** — `combineStyles` تابعی بدون هیچ ورودی
واقعی بود. این قدم آن خلأ را پر می‌کند: یک `promptTokens` غنی و
واقعی (متن انگلیسی Keyword-style، مستقیماً قابل‌مصرف در پرامپت مدل‌های
تولید تصویر/ویدیو) برای هر ۳۴ مقدار.

**منبع محتوایی این متن‌ها تحقیق مستقل معمار پروژه در منابع صنعت prompt
engineering (راهنماهای مدل‌های تولید تصویر/ویدیوی این پروژه — Veo، Kling،
Runway و بقیه‌ی ۱۰ مدل طبق `docs/blueprints/14-output-delivery-v2.md`؛
Sora عمداً خارج از دامنه) و منابع هنر بصری است — نه حدس این پیاده‌سازی.**
فهرست کامل و مرجع رسمی هر ۳۴ مقدار، عیناً همان متنی است که معمار در
دستور کار این قدم ارائه کرد (محفوظ در تاریخچه‌ی گفتگوی این نشست)؛ این
ADR همان فهرست را به‌عنوان مرجع دائمی کد ثبت می‌کند.

## بررسی مستقل — تأیید کامل پیش‌بریفینگ، بدون هیچ تفاوتی

با خواندن مستقل `enum class VisualStyle` کامل در `ProjectDna.kt` (هر ۳۴
نام دقیق) و `StyleMatrix.kt` فعلی (تعریف `StyleReference`/`combineStyles`)
پیش از نوشتن هر خط کد: هر ۳۴ نامی که معمار در دستور کار نوشته بود، دقیقاً
با یکی از ۳۴ enum entry واقعی یکی بود — بدون هیچ خطای تایپی، بدون کم یا
زیاد. ترتیب دقیقاً هم‌ترتیب enum (۷ سینمایی، ۶ انیمیشن ۳D، ۶ انیمیشن ۲D،
۷ هنری، ۸ ژانر = ۳۴). **بدون هیچ انحراف واقعی.**

## پیاده‌سازی

```kotlin
fun VisualStyle.toStyleReference(): StyleReference = StyleReference(
    styleId = name,
    name = readableName(),
    promptTokens = when (this) {
        VisualStyle.CINEMATIC_STYLE -> "cinematic style, professional color grading, ..."
        // ... هر ۳۴ مقدار، بدون else
    }
)
```

## تصمیم ۱ — `when` بدون `else`، نه `Map`

برخلاف قدم ۲ (که برای `categoryCompatibility`/`explicitStyleOverrides`
از `Map` + `checkNotNull` Runtime استفاده کرد، چون آن جداول باید هر
ترکیب دو‌متغیره را می‌پوشاندند)، اینجا یک نگاشت تک‌متغیره‌ی enum→مقدار
است — دقیقاً حالتی که `when` اکسپرشن بدون `else` روی یک enum برایش
طراحی شده: **کامپایلر خودش** کامل‌بودن را تضمین می‌کند، نه یک بررسی
Runtime. اگر `VisualStyle` در آینده مقدار تازه‌ای بگیرد، این تابع دیگر
اصلاً کامپایل نمی‌شود — قوی‌تر از `checkNotNull` قدم ۲ (که فقط وقتی کد
واقعاً اجرا می‌شود خطا می‌دهد، نه در زمان کامپایل).

## تصمیم ۲ — `name` به‌جای ۳۴ رشته‌ی دستی، از خودِ enum مشتق می‌شود

`StyleReference.name` در `combineStyles` مصرف نمی‌شود (فقط
`promptTokens` مصرف می‌شود) — طبق توصیف خودِ دستور کار، «صرفاً
مرجع/کامنت». نوشتن ۳۴ رشته‌ی دستی جدا برای فیلدی که هیچ منطقی به آن
وابسته نیست، فقط ریسک تایپی بدون سود عملکردی اضافه می‌کرد؛ به‌جایش
`readableName()` آن را از خودِ نام enum مشتق می‌کند
(`"CINEMATIC_STYLE"` → `"Cinematic Style"`). عمداً از `visualStyleLabel`
(لایه‌ی UI/i18n، `ui/dna/DnaLabels.kt`) هم استفاده نشد — همان انضباط
جداسازی «domain ← UI ممنوع» که در ADR-115 هم رعایت شد؛ `promptTokens`
هم عمداً انگلیسی است (مستقیماً وارد پرامپت مدل می‌شود، نه UI کاربر —
که از `visualStyleLabel` جدا استفاده می‌کند).

## تست

**`StyleMatrixTest.kt` (۶ تست تازه):** یک تست رشته‌ی دقیق (نه فقط
non-null/non-empty) برای یک نمونه از هرکدام از ۵ دسته
(`CINEMATIC_STYLE`، `PIXAR_DISNEY`، `ANIME`، `WATERCOLOR`، `CYBERPUNK`)
— `promptTokens` برگشتی عیناً با متن مرجع بالا مقایسه می‌شود؛ یک تست
کامل‌بودن که تأیید می‌کند `VisualStyle.entries.size == 34`،
`toStyleReference()` روی هر ۳۴ مقدار بدون خطا اجرا می‌شود، هر ۳۴
`styleId` یکتاست، و هیچ `promptTokens` خالی نیست.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق (تأیید عملی کامل‌بودن `when` — بدون `else`، هر ۳۴ شاخه لازم بود) |
| `gradle :app:testDebugUnitTest` (`StyleMatrixTest`، مجزا) | ۲۳ تست (۱۷+۶ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۹۸ تست (۸۹۲+۶ تازه)، ۳ شکست نامرتبط (`AssetFormFlowTest`، `AppNavigationTest`، `OutputDeliveryFlowTest`) — هیچ‌کدام در مسیر وابستگی این قدم؛ همگی در اجرای مجزا موفق؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (عمداً)

- `combineStyles`، `getInfluenceModifier`، `checkStyleCompatibility`،
  `StyleMatrix` (data class) دست‌نخورده ماندند.
- `PromptAssembly.kt` — اتصال واقعی `toStyleReference()`/`combineStyles`
  به زنجیره‌ی تولید پرامپت نهایی، طبق دستور صریح، قدم بعدی است («قدم
  ۴-اتصال»، جدا از این قدم «۴-محتوا»).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
