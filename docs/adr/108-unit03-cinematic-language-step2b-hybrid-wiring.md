# ADR-108: تکمیل Rule یتیم CinematicLanguage — قدم ۲ب از ۴ زیرقدم قدم ۲ (اتصال واقعی Hybrid به resolveEffectiveCinematicMode)

## زمینه

ADR-107 (زیرقدم ۲الف) پیش‌نیاز فنی `determineHybridPacing`
(`beatIntensity`، `averageBeatIntensity`) را ساخت اما عمداً خودِ
`determineHybridPacing` را به هیچ‌جا — حتی در تست — وصل نکرد. این زیرقدم
(۲ب) همان اتصال واقعی است: `resolveEffectiveCinematicMode` (ADR-106) طوری
گسترش یافت که وقتی زنجیره‌ی سه‌سطحی به `globalMode` خام پروژه می‌رسد (یعنی
هیچ Override دستی در هیچ سطحی وجود ندارد) و آن `globalMode` برابر
`BALANCED` است، به‌جای بازگرداندن یک `BALANCED` ساده و بی‌محتوا، منطق هوشمند
Hybrid واقعاً اجرا می‌شود.

## بررسی مستقل (پیش از نوشتن کد)

**`docs/blueprints/03-visual-identity.md` بخش ب:** دوباره خوانده شد.
تعریف صریح: «Hybrid (ترکیب هوشمند بر اساس Beat Sheet)» — یعنی مفهومی
BALANCED از ابتدا مترادف Hybrid است، نه یک حالت سوم جدا. کد مفهومی
`determineHybridPacing(sceneType: String, avgBeatIntensity: Float)` دوباره
تأیید شد: آستانه‌ها (`>=7f`→FAST_CUT، `<=3f`→LONG_TAKE) دقیقاً همان‌هایی‌اند
که در ADR-107 استفاده شدند.

**یافته‌ی پیش‌بریفینگ درباره‌ی `sceneType`:** با خواندن مستقیم
`domain/shot/ShotModels.kt` تأیید شد `Shot.shotGoal: ShotGoal`
(غیر-nullable، همیشه مقداردهی‌شده) دقیقاً همان مقادیر `ESTABLISHING`,
`ACTION`, `EMOTIONAL`, `DIALOGUE`, `TRANSITION` را دارد — سه‌تای اول/میانی
(`ACTION`/`EMOTIONAL`/`DIALOGUE`) وقتی `.name.lowercase()` شوند دقیقاً با
رشته‌های `"action"`/`"emotional"`/`"dialogue"` در بدنه‌ی
`determineHybridPacing` مطابقت دارند؛ `ESTABLISHING`/`TRANSITION` به‌طور
طبیعی به شاخه‌ی `else -> BALANCED` می‌افتند (بدون نیاز به شاخه‌ی اضافه).
**تأیید کامل، بدون اختلاف با پیش‌بریفینگ.** هیچ فیلد `sceneType` مستقلی در
`Scene` یا `Shot` وجود ندارد و افزودن یکی، تکرار بی‌دلیل `shotGoal` موجود
بود — رد شد.

**بررسی مستقل ADR-106 برای فهم دقیق `resolveEffectiveCinematicMode`
فعلی:** خوانده شد. تأیید شد `resolveEffectiveMode` (سطح پایین‌تر، بدون
تغییر در این ADR) صرفاً `sceneOverrides[sceneId] ?: globalMode` است — یعنی
تشخیص «آیا واقعاً به globalMode رسیدیم یا از طریق Map متمرکز پروژه یک
مقدار صریح گرفتیم» با یک بررسی مستقیم `sceneOverrides[sceneId] == null`
ممکن است، بدون نیاز به تغییر امضای `resolveEffectiveMode`.

## تصمیم معماری — گسترش تابع مرکزی موجود (نه یک مسیر جدا)

پیشنهاد معمار (گسترش `resolveEffectiveCinematicMode` به‌جای ساخت یک تابع
اختیاری جدا) **بررسی و پذیرفته شد، بدون اصلاح.** دلیل تأییدی مستقل:
`validateShotDurationForCinematicMode` (وایرشده در ADR-106 از
`domain/validation/ValidationAggregator.kt`) دقیقاً از خروجی همین تابع
مرکزی تغذیه می‌شود. اگر منطق Hybrid در یک تابع جدا و اختیاری قرار می‌گرفت،
این Rule واقعی (که در محل مرکزی واقعی Validation وایر است) هرگز از منطق
هوشمند Hybrid بهره‌مند نمی‌شد — دقیقاً همان اشتباه «مسیر فراخوانی موازی و
قطع‌شده» که ADR-106 صریحاً هشدار داده بود پرهیز شود.

## پیاده‌سازی

```kotlin
fun resolveEffectiveCinematicMode(projectDna: ProjectDna, scene: Scene, shot: Shot): CinematicMode {
    shot.cinematicModeOverride?.let { return it }
    scene.cinematicModeOverride?.let { return it }
    val settings = projectDna.cinematicLanguage
    settings.sceneOverrides[scene.sceneId]?.let { return it }
    if (settings.globalMode != CinematicMode.BALANCED) return settings.globalMode
    return determineHybridPacing(
        sceneType = shot.shotGoal.name.lowercase(),
        avgBeatIntensity = averageBeatIntensity(shot.beats)
    )
}
```

**ترتیب اولویت (بدون تغییر نسبت به ADR-106، تصریح‌شده):**

1. `shot.cinematicModeOverride` — انتخاب صریح شات، همیشه همان مقدار (حتی اگر `BALANCED` باشد).
2. `scene.cinematicModeOverride` — انتخاب صریح صحنه، همیشه همان مقدار.
3. `projectDna.cinematicLanguage.sceneOverrides[sceneId]` — انتخاب صریح متمرکز پروژه برای این صحنه، همیشه همان مقدار.
4. **فقط اینجا** (هیچ‌کدام از بالا وجود نداشت): اگر `globalMode == BALANCED`، `determineHybridPacing` واقعاً اجرا می‌شود؛ در غیر این صورت `globalMode` خام (`LONG_TAKE`/`FAST_CUT`) بدون تغییر برمی‌گردد.

**چرا لایه‌های ۱ تا ۳ هرگز به Hybrid فرستاده نمی‌شوند، حتی اگر مقدارشان
خودشان `BALANCED` باشد:** این سه لایه هرکدام یک انتخاب دستی و آگاهانه‌ی
کاربر برای یک واحد مشخص (این شات، این صحنه، یا این صحنه از طریق تنظیمات
متمرکز پروژه) هستند. اگر کاربر صریحاً `BALANCED` را برای یک صحنه یا شات
خاص انتخاب کرده، معنای منطقی آن «همین محدوده‌ی عددی BALANCED (۳-۲۰ ثانیه)
را می‌خواهم» است — نه «بگذار سیستم حدس بزند». Hybrid فقط معنا دارد وقتی
هیچ انتخاب مشخصی وجود ندارد و ما واقعاً به پیش‌فرض خام کل پروژه می‌رسیم.

## تست

**`CinematicLanguageTest.kt` (۵ تست تازه، طبق مشخصات دقیق دستور کار):**
- `shotGoal=ACTION` + `beats` خالی (شدت خنثی ۵) + `globalMode=BALANCED` →
  `FAST_CUT` (چون `sceneType=="action"` در `determineHybridPacing` بدون
  توجه به شدت اولویت دارد).
- `shotGoal=DIALOGUE` + `beats` با میانگین `>=7f` (دو Beat
  `SUBJECT_ACTION`) + `globalMode=BALANCED` → `FAST_CUT` (شدت بالا بر نوع
  DIALOGUE اولویت دارد، طبق منطق موجود `determineHybridPacing`).
- `shotGoal=DIALOGUE` + `beats` با میانگین متوسط (یک Beat `CAMERA_MOVE`، `6f`) → `BALANCED`.
- `globalMode=LONG_TAKE` (نه BALANCED) + `shotGoal=ACTION` → همچنان
  `LONG_TAKE` (Hybrid دخالت نمی‌کند وقتی کاربر صریحاً یک حالت غیر-BALANCED
  انتخاب کرده).
- `cinematicModeOverride` دستی صریح روی شات + `globalMode=BALANCED` +
  `shotGoal=ACTION` → همچنان مقدار Override (اولویت مطلق حفظ شد).

**رگرسیون — `ValidationAggregatorTest.kt` (هر ۹ تست موجود، بدون تغییر):**
بررسی شد و تأیید شد **هیچ تستی نیاز به تغییر نداشت.** دلیل: `neutralShot()`
(`shotGoal=ESTABLISHING`، `beats` خالی) و `neutralDna()`
(`cinematicLanguage` پیش‌فرض، یعنی `globalMode=BALANCED`) با منطق تازه هم
دقیقاً به همان `BALANCED` قبلی می‌رسند — چون `ESTABLISHING` در هیچ شاخه‌ی
`action`/`emotional`/`dialogue`ی `determineHybridPacing` نیست و شدت خنثی
(۵) هم در هیچ آستانه‌ای نمی‌افتد، پس نتیجه‌ی نهایی همچنان `else ->
BALANCED` است — رفتار قابل‌مشاهده‌ی این ۹ تست عیناً یکسان ماند. این خودش یک
تست رگرسیون واقعی است، نه صرفاً یک ادعا: با اجرای واقعی این ۹ تست (بدون
هیچ تغییر کد در آن فایل) تأیید شد.

**رگرسیون — `resolveEffectiveCinematicMode` قبلی (۴ تست ADR-106، بدون
تغییر):** هر چهار تست هم دقیقاً بدون تغییر سبز ماندند — چون در هر چهار
سناریوی آن‌ها یا یک Override دستی صریح در کار است (لایه ۱/۲/۳)، یا
`globalMode` مقداری غیر از `BALANCED` است.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`CinematicLanguageTest`) | ۳۹ تست (۳۴ + ۵ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`ValidationAggregatorTest`) | ۹ تست، همه موفق، **بدون تغییر کد در آن فایل** |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۵۴ تست (۸۴۹ + ۵ تازه)، ۲ شکست نامرتبط (`BottomNavStudioResolutionTest`، `OutputDeliveryFlowTest` — هیچ‌کدام در این قدم لمس نشدند) — هر دو در اجرای مجزا (`--tests`) موفق؛ Flaky شناخته‌شده‌ی محیط Compose/Robolectric این Sandbox، هم‌الگوی مستندشده‌ی ADR-105/106/107 |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این زیرقدم (عمداً)

- `getPacingFromEmotion` — زیرقدم بعدی (۲ج یا ۲د، هنوز برنامه‌ریزی‌نشده).
- سه Rule تعارض دوربین و حرکت (`LogicConflictChecker.kt`) — قدم ۳.
- UI — قدم ۴.
- هیچ فایل UI/ViewModel لمس نشد.

## نتیجه

`resolveEffectiveCinematicMode` اکنون واقعاً هوشمند است — Hybrid دیگر یک
مقدار ساکن نیست، بلکه برای هر شات خاص (بر اساس هدف و ریتم Beat Sheet
واقعی‌اش) محاسبه می‌شود، دقیقاً طبق قصد بلوپرینت ۰۳ بخش ب. چون این تابع
همان تابعی است که `validateShotDurationForCinematicMode` را (از طریق
`ValidationAggregator.kt`) تغذیه می‌کند، این هوشمندی بدون هیچ کد اضافه‌ای
در مسیر Validation واقعی هم منعکس می‌شود.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
