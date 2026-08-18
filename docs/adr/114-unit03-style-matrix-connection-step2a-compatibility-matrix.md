# ADR-114: اتصال کامل Style Matrix — قدم ۲الف از ۹ زیرقدم: ماتریس واقعی سازگاری سبک‌ها + تغییر امضای checkStyleCompatibility به enum

## زمینه

ADR-113 (قدم ۱الف) مفهوم «سبک ثانویه + شدت تأثیر» را به مدل واقعی
`CoreIdentity` منتقل کرد، اما `checkStyleCompatibility` در
`domain/visualidentity/StyleMatrix.kt` هنوز رفتار حداقلی قدیمی
(همیشه `MEDIUM/false`) را داشت — کامنت بالای تابع صریحاً توضیح می‌داد
این عمدی بود چون نویسنده‌ی قبلی نخواست بدون منبع مشخص یک ماتریس واقعی
حدس بزند. این قدم آن خلأ را با یک ماتریس واقعی، تحقیق‌شده پر می‌کند.

**منبع محتوایی این ماتریس تحقیق مستقل معمار پروژه در منابع صنعت
انیمیشن/هنر بصری بوده — نه حدس این پیاده‌سازی.** این یک تصمیم محتوایی
(نه فنی) است؛ پیاده‌سازی زیر عیناً همان قوانین را کدنویسی می‌کند، بدون
تغییر محتوا.

## بررسی مستقل — تأیید کامل پیش‌بریفینگ، بدون هیچ تفاوتی

با خواندن کامل `StyleMatrix.kt` فعلی، enum `VisualStyle` کامل (هر ۳۴
مقدار، با نام دقیق) در `ProjectDna.kt`، و `StyleMatrixTest.kt` فعلی
پیش از نوشتن هر خط کد: تمام ادعاهای پیش‌بریفینگ (امضای فعلی
`checkStyleCompatibility(String, String)`، رفتار همیشه `MEDIUM/false`،
نام دقیق هر ۳۴ مقدار enum از جمله همه‌ی مقادیر استفاده‌شده در Override
ها — `STUDIO_GHIBLI`، `WATERCOLOR`، `OIL_PAINTING`، `PHOTOREALISTIC`،
`PIXAR_DISNEY`، `DREAMWORKS`، `ILLUMINATION`، `ANIME`، `COMIC_BOOK`،
`CYBERPUNK`، `PENCIL_SKETCH`، `CARTOON`، `CLAYMATION`) عیناً با کد
واقعی مطابق بود — بدون هیچ خطای تایپی یا انحراف. با `grep` مستقل تأیید
شد `checkStyleCompatibility`/`checkStyleMatrixCompatibility`/
`StyleMatrix`/`validatePrimaryStyleUpdate`/`StyleReference` هیچ‌جای
دیگر کدبیس (خارج از همین دو فایل) استفاده نمی‌شوند — Scope این قدم
کاملاً ایزوله است. شماره‌ی بعدی ADR با `ls docs/adr/` مستقل تأیید شد:
ADR-113 آخرین بود، پس این ADR شماره‌ی ۱۱۴ گرفت.

## ماتریس رسمی — مرجع آینده (منبع: تحقیق معمار، نه این پیاده‌سازی)

### سطح Category (۵×۵، متقارن)

| | CINEMATIC | ANIMATION_3D | ANIMATION_2D | ARTISTIC | GENRE |
|---|---|---|---|---|---|
| **CINEMATIC** | HIGH | MEDIUM | LOW | MEDIUM | MEDIUM |
| **ANIMATION_3D** | | HIGH | HIGH | LOW | MEDIUM |
| **ANIMATION_2D** | | | HIGH | HIGH | MEDIUM |
| **ARTISTIC** | | | | HIGH | LOW |
| **GENRE** | | | | | HIGH |

### Override های دقیق (اولویت بر Category)

۱. `STUDIO_GHIBLI` + `WATERCOLOR` = HIGH
۲. `OIL_PAINTING` + `PHOTOREALISTIC` = LOW
۳. هرکدام از `{PIXAR_DISNEY, DREAMWORKS, ILLUMINATION}` + هرکدام از
   `{ANIME, STUDIO_GHIBLI, COMIC_BOOK}` = HIGH (۹ ترکیب)
۴. `CYBERPUNK` + هرکدام از `{WATERCOLOR, OIL_PAINTING, PENCIL_SKETCH}` = LOW
   (۳ ترکیب)
۵. `PHOTOREALISTIC` + هرکدام از `{CARTOON, COMIC_BOOK, CLAYMATION}` = LOW
   (۳ ترکیب)

### قواعد تکمیلی

- `primary == secondary` (دقیقاً همان مقدار enum) ⇒ همیشه HIGH، فارغ از
  بقیه‌ی قوانین.
- `warning = true` فقط وقتی سطح نهایی LOW یا INCOMPATIBLE باشد؛ برای
  HIGH/MEDIUM همیشه `false`.
- این قدم هیچ جفتی را INCOMPATIBLE نمی‌گذارد — آن سطح برای Custom Style
  های آینده نگه داشته می‌شود.

## تصمیم ۱ — ساختار داده: دو `Map` به‌جای یک `when` بزرگ

`categoryCompatibility: Map<Pair<VisualStyleCategory, VisualStyleCategory>, CompatibilityLevel>`
و `explicitStyleOverrides: Map<Pair<VisualStyle, VisualStyle>, CompatibilityLevel>` — هر
دو با یک تابع محلی `put(a, b, level)` که هر دو ترتیب جفت را همزمان ثبت
می‌کند (تقارن، بدون نیاز به نرمال‌سازی ترتیب در محل مصرف). توجیه انتخاب
`Map` به‌جای `when` تودرتو: این دو جدول داده‌ی خالص هستند (نه منطق
شرطی)، یک `when` با ۱۵+۱۷ شاخه (با احتساب هر دو ترتیب) بسیار طولانی و
مستعد خطای رونویسی می‌شد؛ `Map` امکان راستی‌آزمایی کامل‌بودن را هم در
Runtime می‌دهد (`checkNotNull` زیر) — یک `when` غیرکامل بی‌صدا از مسیر
`else` رد می‌شد، اما یک `Map` غیرکامل با `checkNotNull` بلافاصله خطا
می‌دهد. این تصمیم فنی من است (نه محتوای ماتریس، که عیناً طبق تحقیق
معمار پیاده‌سازی شد).

## تصمیم ۲ — بدون Fallback خاموش؛ `checkNotNull` روی جدول Category

```kotlin
fun checkStyleCompatibility(primary: VisualStyle, secondary: VisualStyle): CompatibilityResult {
    val level = when {
        primary == secondary -> CompatibilityLevel.HIGH
        else -> explicitStyleOverrides[primary to secondary]
            ?: checkNotNull(categoryCompatibility[primary.category to secondary.category]) { ... }
    }
    val warning = level == CompatibilityLevel.LOW || level == CompatibilityLevel.INCOMPATIBLE
    return CompatibilityResult(level = level, warning = warning)
}
```

چون `categoryCompatibility` باید هر ۲۵ ترکیب مرتب‌شده‌ی ۵ دسته را کامل
بپوشاند (و پوشش می‌دهد)، این `checkNotNull` هرگز واقعاً نباید شکست
بخورد — اما یک Fallback خاموش (مثلاً بازگشت پیش‌فرض به `MEDIUM`) می‌توانست
یک سطر جاافتاده‌ی آینده (اگر کسی دسته‌ی ششمی به `VisualStyleCategory`
اضافه کند بدون به‌روزرسانی این جدول) را برای همیشه پنهان کند — طبق
انضباط «بدون حدس زدن/بدون خطای خاموش» این پروژه، شکست بلند و صریح
ترجیح داده شد.

## تصمیم ۳ — `StyleMatrix.primaryStyle`/`secondaryStyle`: از `StyleReference` به `VisualStyle`

قدم ۱ (ADR-113) مفهوم سبک اصلی/ثانویه را از `StyleReference` جدا به
`CoreIdentity` منتقل کرد؛ ادامه‌ی استفاده از `StyleReference` در خودِ
`StyleMatrix` (که دیگر توسط هیچ‌جای واقعی کدبیس استفاده نمی‌شود، طبق
`grep`) منسوخ بود. `checkStyleMatrixCompatibility` هم به‌همان نسبت
مستقیماً `matrix.primaryStyle`/`secondary` (اکنون `VisualStyle`) را به
`checkStyleCompatibility` پاس می‌دهد، بدون `.styleId`.

`StyleReference`/`combineStyles`/`getInfluenceModifier` طبق دستور صریح
دست‌نخورده ماندند — برای `promptTokens` در قدم‌های آینده (۴ به بعد)
هنوز لازم‌اند.

## یافته‌ی خارج از Scope صریح این قدم — `validatePrimaryStyleUpdate` اکنون ناسازگار با `StyleMatrix.primaryStyle`

دستور این قدم فقط `StyleMatrix` (data class) و `checkStyleMatrixCompatibility`
را برای تغییر از `StyleReference` به `VisualStyle` فهرست کرده بود؛
`validatePrimaryStyleUpdate(newPrimaryStyle: StyleReference?)` در همان
فایل صراحتاً خارج از این فهرست بود، پس دست‌نخورده ماند. نتیجه: این تابع
اکنون یک ناسازگاری نوع واقعی با داده‌ای که ادعا می‌کند اعتبارسنجی
می‌کند دارد — پارامترش هنوز `StyleReference?` است، در حالی که
`StyleMatrix.primaryStyle` (که این تابع منطقاً برای اعتبارسنجی
به‌روزرسانی آن نوشته شده بود، طبق کامنت خودش «Rule 3») اکنون
`VisualStyle` است؛ کامپایل می‌شود (چون این تابع مستقل است و به
`StyleMatrix` ارجاع نمی‌دهد) اما دیگر واقعاً به‌درد اعتبارسنجی
به‌روزرسانی `primaryStyle` واقعی نمی‌خورد. این طبق دستور صریح تغییر
داده نشد — به‌عنوان یک یافته‌ی واقعی برای تصمیم معمار (احتمالاً امضای
آن هم در یک زیرقدم بعدی به `VisualStyle?` تغییر کند) گزارش می‌شود، نه
سکوت.

## تست

**`StyleMatrixTest.kt` (بازنویسی کامل، ۱۷ تست — از ۱۰ تست قبلی):**
- `getInfluenceModifier`/`combineStyles` (۴ تست، دست‌نخورده — چون
  `StyleReference`/`combineStyles` تغییری نکردند).
- یک جفت HIGH از قانون Category (`FILM_NOIR`+`BLOCKBUSTER`، هر دو
  `CINEMATIC`).
- یک جفت LOW از قانون Category (`FILM_NOIR`+`MANGA`،
  `CINEMATIC`×`ANIMATION_2D`).
- قاعده‌ی `primary == secondary` (`ANIME`+`ANIME` = HIGH).
- هر ۵ Override دقیق، هرکدام حداقل یک تست؛ دو مورد (`OIL_PAINTING`+
  `PHOTOREALISTIC` و `PHOTOREALISTIC`+`CLAYMATION`) عمداً طوری انتخاب
  شدند که نتیجه‌ی Override با نتیجه‌ی Category (اگر Override نبود)
  متفاوت باشد — اثبات صریح اینکه Override واقعاً بر Category اولویت
  دارد، نه فقط تصادفاً همان مقدار را می‌دهد.
- `rule1`/`rule2`/`rule3` — هم‌الگوی قبلی، به‌روزشده با `VisualStyle`
  (به‌جز `rule3` که همچنان روی `StyleReference` است، طبق یافته‌ی بالا).

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`StyleMatrixTest`) | ۱۷ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۸۹ تست (۸۸۲+۷ تازه)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) — در مسیر وابستگی این قدم نیست؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این زیرقدم (عمداً)

- `CoreIdentity`، `DnaViewModel`، `DnaTabContent`، `PromptAssembly` —
  دست‌نخورده، طبق دستور صریح.
- `validatePrimaryStyleUpdate` — طبق یافته‌ی بالا، ناسازگاری تازه‌اش
  گزارش شد اما تغییر داده نشد (خارج از فهرست صریح این قدم).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
