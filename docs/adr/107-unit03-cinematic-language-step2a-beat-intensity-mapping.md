# ADR-107: تکمیل Rule یتیم CinematicLanguage — قدم ۲الف از ۴ زیرقدم قدم ۲ (نگاشت شدت Beat)

## زمینه

ADR-106 (قدم ۱ از ۴ کل فیچر) زنجیره‌ی سه‌سطحی `resolveEffectiveCinematicMode`
را وصل کرد، اما عمداً `determineHybridPacing`/`getPacingFromEmotion` (منطق
هوشمند Hybrid) را خارج از Scope گذاشت — قدم ۲. بررسی عمیق‌تر قدم ۲ نشان داد
خودش به‌مراتب بزرگ‌تر از یک اتصال ساده است، پس مثل الگوی موفق G2 (پنج قدم
مستقل برای پنج پروفایل AI)، به زیرقدم‌های مستقل تقسیم شد. **این ADR فقط
زیرقدم ۲الف (پیش‌نیاز فنی) است، نه خودِ اتصال `determineHybridPacing`.**

## بررسی مستقل (پیش از نوشتن کد)

**`docs/blueprints/05-shot-engine-v2.md` (بخش «ساختار داده» و «پیاده‌سازی
مفهومی»):** خوانده شد. `data class Beat(timestampSeconds: Float, eventType:
BeatEventType, description: String, subjectId: String? = null)` — دقیقاً
همان چیزی که `domain/shot/ShotModels.kt` واقعی هم دارد (تأییدشده با خواندن
مستقیم). **هیچ فیلد شدت/intensity/energy ای در `Beat` مفهومی بلوپرینت ۰۵
هم وجود ندارد** — یافته‌ی پیش‌بریفینگ («Beat فیلد intensity ندارد») **کاملاً
تأیید شد، بدون اختلاف.**

**`docs/blueprints/03-visual-identity.md` بخش ب (کد مفهومی
`determineHybridPacing`):** خوانده شد. آستانه‌ها دقیقاً همان چیزی است که در
پیش‌بریفینگ آمده بود: `avgBeatIntensity >= 7f` → `FAST_CUT`،
`avgBeatIntensity <= 3f` → `LONG_TAKE`، در غیر این صورت (`dialogue` یا
هر حالت دیگر) → `BALANCED`. بدون مقیاس عددی صریح‌تر از این دو آستانه در
بلوپرینت — یعنی طراحی بازه‌ی داخلی (۱ تا ۱۰) و نگاشت هر `BeatEventType`
واقعاً یک تصمیم طراحی این قدم بود، نه چیزی که بلوپرینت مستقیماً مشخص کرده
باشد.

**نتیجه:** هیچ اختلافی بین پیش‌بریفینگ و کد/بلوپرینت واقعی یافت نشد.

## تصمیم ۱ — محل کد: همان فایل `CinematicLanguage.kt` (نه فایل جدید)

هر دو تابع در همان `domain/visualidentity/CinematicLanguage.kt` اضافه شدند،
درست قبل از `determineHybridPacing` (که مصرف‌کننده‌ی بعدی‌شان خواهد بود، در
زیرقدم ۲ب). دلیل: این‌ها منطقاً بخشی از همان مجموعه‌ی توابع Cinematic
Language هستند، دقیقاً هم‌الگو با تصمیم مشابه در ADR-106 (که
`resolveEffectiveCinematicMode` را هم به همین فایل اضافه کرد، نه فایل
جدید) — فایل هنوز به‌اندازه‌ای نیست که تفکیک آن به فایل مجزا توجیه‌پذیر
باشد.

## تصمیم ۲ — نگاشت `beatIntensity(eventType: BeatEventType): Float`

```kotlin
fun beatIntensity(eventType: BeatEventType): Float = when (eventType) {
    BeatEventType.SUBJECT_ACTION -> 8f
    BeatEventType.CAMERA_MOVE -> 6f
    BeatEventType.ENVIRONMENTAL -> 4f
    BeatEventType.LIGHTING_CHANGE -> 2f
}
```

بازه‌ی ۱ تا ۱۰ انتخاب شد — هم‌راستا با نوع مقایسه‌ی `determineHybridPacing`
(`avgBeatIntensity` یک عدد ساده، نه یک بازه‌ی نرمال‌شده‌ی ۰-۱). منطق
رتبه‌بندی (سینمایی/داستانی، نه اعداد دلبخواهی):

| `BeatEventType` | شدت | دلیل |
|---|---|---|
| `SUBJECT_ACTION` | ۸ | کاراکتر واقعاً در حال انجام یک کنش فیزیکی است — مستقیم‌ترین نشانه‌ی صحنه‌ی اکشن (دویدن، مبارزه، حمله). بالاترین شدت. |
| `CAMERA_MOVE` | ۶ | پویایی بصری واقعی (Dolly/Crane/Handheld)، اما خودِ حرکت دوربین در صحنه‌های آرام هم به‌کار می‌رود (مثلاً یک Pan آهسته در یک مکالمه‌ی احساسی) — کمی پایین‌تر از کنش مستقیم کاراکتر. |
| `ENVIRONMENTAL` | ۴ | افکت محیطی (رعد، باد، باران) معمولاً فضاسازی است، نه خودِ کنش روایی — شدت متوسط-پایین. |
| `LIGHTING_CHANGE` | ۲ | معمولاً یک تأکید ظریف احساسی/فضایی (مثلاً کم‌نور شدن در یک لحظه‌ی آرام) است، نه یک محرک برش سریع — پایین‌ترین شدت. |

**بررسی سازگاری با پیشنهاد اولیه‌ی معمار:** پیشنهاد اولیه («CAMERA_MOVE و
SUBJECT_ACTION معمولاً پرانرژی‌تر از ENVIRONMENTAL/LIGHTING_CHANGE») به‌طور
کامل تأیید و پذیرفته شد؛ تفکیک ظریف‌تر درون هر دو‌تایی (SUBJECT_ACTION >
CAMERA_MOVE، و ENVIRONMENTAL > LIGHTING_CHANGE) تصمیم تکمیلی خودِ این قدم
بود، با دلیل مستقل بالا.

**اثبات سازگاری با آستانه‌های `determineHybridPacing`:** یک Beat Sheet
عمدتاً `SUBJECT_ACTION` به میانگین ۸ می‌رسد (`>= 7f` → `FAST_CUT` مورد
انتظار برای صحنه‌ی اکشن). یک Beat Sheet عمدتاً `LIGHTING_CHANGE` به میانگین
۲ می‌رسد (`<= 3f` → `LONG_TAKE` مورد انتظار برای صحنه‌ی احساسی/آرام).

## تصمیم ۳ — `averageBeatIntensity(beats: List<Beat>): Float` و حالت مرزی لیست خالی

```kotlin
fun averageBeatIntensity(beats: List<Beat>): Float {
    if (beats.isEmpty()) return 5f
    return beats.map { beatIntensity(it.eventType) }.average().toFloat()
}
```

**تصمیم لیست خالی:** مقدار ۵ (وسط دقیق بازه‌ی ۱-۱۰) بازگردانده می‌شود.
دلیل: یک Shot/Scene بدون Beat Sheet فعال (بدون هیچ داده‌ی ریتمی) نباید
به‌طور تصادفی به یک سوی افراطی (FAST_CUT یا LONG_TAKE) سوق داده شود؛ ۵ طبق
طراحی همیشه در بازه‌ی خنثی (`3f < 5f < 7f`) `determineHybridPacing` می‌افتد
و به `else -> BALANCED` موجود آن تابع منجر می‌شود — بدون نیاز به تغییر خودِ
`determineHybridPacing`.

## خارج از Scope این زیرقدم (عمداً، طبق تصریح دستور کار)

- **`determineHybridPacing` در این زیرقدم فراخوانی یا وصل نشد** — نه در
  کد تولید، و نه حتی در تست‌ها (تست‌های این ADR فقط عدد میانگین را در برابر
  آستانه‌های مستندشده‌ی آن تابع می‌سنجند، بدون فراخوانی خودِ تابع).
- هیچ ViewModel یا `ValidationAggregator.kt` ای لمس نشد.
- `getPacingFromEmotion`، سه Rule تعارض دوربین و حرکت، و UI —
  همچنان در قدم‌های ۲ب/۳/۴ بعدی.

## تست

**`CinematicLanguageTest.kt` (۱۱ تست تازه):**
- نگاشت هر چهار مقدار `BeatEventType` به شدت مورد انتظار (۴ تست مجزا).
- رتبه‌بندی اکید (`SUBJECT_ACTION > CAMERA_MOVE > ENVIRONMENTAL >
  LIGHTING_CHANGE`، یک تست جامع).
- `averageBeatIntensity` با یک لیست چهارتایی (میانگین ۵ = `(8+6+4+2)/4`).
- `averageBeatIntensity` با یک لیست تک‌آیتمی (خروجی برابر همان یک شدت).
- `averageBeatIntensity` با لیست خالی (خروجی ۵، طبق تصمیم بالا).
- سه تست سازگاری آستانه (بدون فراخوانی `determineHybridPacing`؛ فقط
  `avg >= 7f`/`avg <= 3f`/`3f < avg < 7f` مستقیماً سنجیده می‌شوند).

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`CinematicLanguageTest`) | ۳۴ تست (۲۳ + ۱۱ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۴۹ تست (۸۳۸ + ۱۱ تازه)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest` — هیچ‌کدام در این قدم لمس نشدند) — هر دو در اجرای مجزا (`--tests`) موفق؛ Flaky شناخته‌شده‌ی محیط Compose/Robolectric این Sandbox، هم‌الگوی مستندشده‌ی ADR-105/106 |
| `gradle :app:assembleDebug` | موفق |

## نتیجه

پیش‌نیاز فنی زیرقدم ۲ب (اتصال واقعی `determineHybridPacing` به یک نقطه‌ی
فراخوانی واقعی) آماده است. خودِ آن اتصال، و تصمیم اینکه چه چیزی
`sceneType` را برای `determineHybridPacing` تأمین کند (که فعلاً یک `String`
خام است، نه یک enum واقعی — یافته‌ی احتمالی برای زیرقدم بعدی)، در زیرقدم
۲ب بررسی و پیاده‌سازی می‌شود.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
