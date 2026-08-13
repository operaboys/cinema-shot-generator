# ADR-083: رفع ۴ یافته‌ی سطح ۲ از appendix ممیزی سراسری mockup (ADR-081)

## زمینه

دومین دسته از فهرست اولویت‌بندی‌شده‌ی یافته‌های `docs/adr/081-appendix-mockup-audit.md`.
هرکدام مستقل با grep/خواندن مستقیم `docs/design/Cinema Studio.html` قبل از شروع
دوباره تأیید شد. طبق قانون این قدم: موارد ۱ و ۲ کامل رفع شدند؛ مورد ۳ جزئی
(رشته‌ی نسخه بله، لوگوی گرافیکی موکول)؛ مورد ۴ — که ابهام‌دارتر از بقیه فرض
می‌شد — پس از خواندن دقیق mockup ابهامش کاملاً برطرف شد و کامل رفع شد.

## ۱. Badge شمارشی روی Tab های Scene Detail

### مرجع mockup
`sceneTabs` (`docs/design/Cinema Studio.html`): `[{label:'Overview', badge:''}, {label:'Shots', badge:'12'}, {label:'Assets', badge:'5'}]`
— یعنی OVERVIEW عمداً بدون Badge است، SHOTS و ASSETS هرکدام یک شمارش دارند.

### تصمیم مستقل — Badge Tab ASSETS
طبق appendix، Tab ASSETS صحنه فعلاً فقط `EmptyTabState` ثابت است (هیچ اتصال
واقعی Asset↔Scene پیاده نشده — این خودش یک یافته‌ی جدا و بزرگ‌تر appendix
است، خارج از دامنه‌ی این قدم). نمایش یک Badge برای این Tab (حتی «۰») گمراه‌کننده
بود: چون هیچ منطق شمارش واقعی پشت آن نیست، همیشه «۰» می‌ماند مستقل از تعداد
واقعی Asset های کتابخانه — یک Badge که همیشه صفر است، نه صادقانه‌تر از نبود
Badge، بلکه یک ادعای کاذب «این عدد چیزی را می‌شمارد» است. **رفع: فقط Tab
SHOTS اکنون Badge دارد؛ Tab ASSETS بدون تغییر ماند** (منتظر رفع کامل‌تر اتصال
Asset↔Scene در قدمی جداگانه).

### پیاده‌سازی
`SceneDetailViewModel.kt`: `shotCount: StateFlow<Int>` تازه — هم‌الگو دقیق با
`locationAssets` موجود (`shotRepository.loadAllShots(sceneId).map{it.size}.stateIn(...)`)،
یک Flow زنده، نه یک‌بار خواندن. `SceneDetailScreen.kt`: Tab SHOTS اکنون
`Row(label + count با رنگ کم‌رنگ‌تر fg3)` — دقیقاً هم‌چیدمان mockup.

## ۲. کاشی پیش‌نمایش تصویر ۱۴۰px در Settings

### مرجع appendix
«کارت تصویر Home — Choose/Remove واقعی، اما کاشی پیش‌نمایش ۱۴۰px رندر
نمی‌شود (فقط متن URI خام)».

### پیاده‌سازی
منطق decode بومی (`BitmapFactory.decodeStream` روی `ContentResolver.openInputStream`،
با `Dispatchers.IO`) که قبلاً فقط در `HomeBackgroundImage` (خصوصی، `HomeScreen.kt`)
بود، به یک Composable مشترک `DecodedContentImage` (اکنون `internal`) استخراج
شد — `HomeBackgroundImage` خودش اکنون یک Wrapper نازک روی همین تابع است.
`SettingsScreen.kt`'s `HomeImageCard`: کارت ۱۴۰×۱۴۰dp — وقتی URI موجود
باشد `DecodedContentImage` واقعی رندر می‌شود، وگرنه همان متن «—» قبلی
(به‌جای متن خام URI).

## ۳. لوگوی سفارشی Aperture-C + رشته‌ی نسخه (رفع جزئی، عمدی)

### بررسی
`grep -r` روی `res/drawable` هیچ فایل Vector/SVG لوگو پیدا نکرد — تأیید شد
این آیتم واقعاً نیازمند یک دارایی گرافیکی تازه است (طراحی یک نشان سفارشی
Aperture-C)، نه یک رفع صرفاً کدی.

### تصمیم — تفکیک بخش کدی از بخش طراحی
- **رفع کامل (کدی)**: رشته‌ی نسخه‌ی واقعی (`BuildConfig.VERSION_NAME`،
  طبق mockup «v1.0.0 · On-Device») به کارت «درباره» اضافه شد.
  `app/build.gradle.kts`: `buildFeatures.buildConfig = true` — AGP جدید
  این ویژگی را پیش‌فرض خاموش می‌کند، اضافه شدن این پرچم پیش‌نیاز واقعی
  `BuildConfig.VERSION_NAME` بود.
- **موکول رسمی (بدهی طراحی گرافیکی، نه کدی)**: لوگوی سفارشی Aperture-C
  (نشان SVG/Vector طراحی‌شده، طبق `#csMark` mockup) نیاز به یک دارایی
  بصری تازه دارد که خارج از توان یک قدم صرفاً کدی است — ADR-044 تصمیم
  ۱۱ قبلاً همین را با یک آیکون Material موقت (`Icons.Filled.Movie`)
  موکول کرده بود؛ این قدم آن تصمیم را رسماً تأیید/تمدید می‌کند، نه
  رفع می‌کند. هیچ کد جدیدی برای این بخش نوشته نشد.

## ۴. نوار پیشرفت ۳بخشی AI-Breakdown در Tab داستان Studio (ابهام برطرف شد)

### یافته‌ی دیباگ واقعی — چرا این مورد در واقع ابهامی نداشت
دستور کار این قدم فرض کرده بود این نوار یا وضعیت واقعی یک فرآیند AI
Breakdown در حال اجرا را نشان می‌دهد، یا شاخصی از میزان تکمیل داستان —
هر دو نیازمند تصمیم درباره‌ی اتصال به داده‌ی واقعی. اما خواندن دقیق و
خام HTML mockup (نه صرفاً خلاصه) این فرض را رد کرد: رنگ هر ۳ Segment در
خودِ mockup **Literal است، نه data-bound**:

```html
<span style="...background:#7C5CFF"></span>   <!-- Segment ۱: همیشه بنفش -->
<span style="...background:var(--hair)"></span> <!-- Segment ۲: همیشه خط باریک -->
<span style="...background:var(--hair)"></span> <!-- Segment ۳: همیشه خط باریک -->
```

برخلاف عناصر واقعاً Data-bound همان فایل (مثلاً `{{ t.color }}` در همان
بخش)، این ۳ رنگ هیچ‌جا داخل `{{ }}` نیستند — یعنی خودِ mockup هم این را
یک نشانه‌ی بصری ثابت («این کارت به یک جریان ۳فازی می‌رود») طراحی کرده،
نه یک Progress Indicator متصل به هیچ State ای. با این یافته، ابهام
دستور کار کاملاً برطرف شد: پیاده‌سازی صحیح، کپی دقیق همان رفتار ثابت
mockup است، نه اختراع یک اتصال داده‌ی جعلی که خودِ سند طراحی هم ندارد.

### پیاده‌سازی
`StoryTabContent.kt`: دکمه‌ی ساده‌ی قبلی با یک `Card` جایگزین شد —
عنوان (`drawer.aiBreakdown`) + زیرعنوان (بازاستفاده از کلید موجود
`aiBreakdown.subtitle`) + آیکون `auto_awesome` + نوار ۳بخشی ثابت (Segment
اول `MaterialTheme.colorScheme.primary`، دو Segment بعدی
`CinemaTheme.extendedColors.hairline` — دقیقاً معادل `#7C5CFF`/`var(--hair)`
mockup در این تم). همان `testTag` قبلی (`STORY_TAB_AI_BREAKDOWN_BUTTON_TAG`)
روی خودِ `Card` قابل‌کلیک ماند — رفتار Navigate قبلی بدون تغییر.

## تست

- `SettingsFlowTest.kt` (+۲ تست): کاشی پیش‌نمایش تصویر واقعی رندر می‌شود
  (نه متن خام URI)؛ رشته‌ی نسخه‌ی واقعی `BuildConfig.VERSION_NAME` نمایش
  داده می‌شود.
- `ScenesFlowTest.kt` (+۱ تست): Badge Tab «شات‌ها» با ۰ شروع می‌شود و بعد
  از ذخیره‌ی یک Shot واقعی (نه UI، مستقیم از Repository — اثبات این‌که
  StateFlow زنده است) به ۱ به‌روزرسانی می‌شود.
- `StoryTabFlowTest.kt` (+۱ تست): کارت AI-Breakdown زیرعنوان mockup را
  نشان می‌دهد و کلیک روی آن هنوز واقعاً به AI Story Breakdown Navigate
  می‌کند (رفتار قبلی حفظ شده).

## یافته‌های دیباگ واقعی (تست)

۱. Badge Tab: چون `Tab` یک Composable قابل‌کلیک است، Compose به‌طور
پیش‌فرض متن فرزندانش را در Semantics Tree ادغام می‌کند (برای
Accessibility) — بدون `useUnmergedTree = true` گره‌ی متن Badge به‌تنهایی
در درخت ادغام‌شده پیدا نمی‌شد (پیام خطای خودِ Compose دقیقاً همین راه‌حل
را پیشنهاد داد: «Are you missing useUnmergedNode = true؟»).
۲. کاشی پیش‌نمایش تصویر: در viewport کوچک تست، کارت تصویر پایین‌تر از
چند کارت دیگر Settings قرار می‌گیرد — رفع با همان الگوی مستندشده‌ی این
پروژه، `.performScrollTo()` صریح پیش از `assertIsDisplayed()`.

## راستی‌آزمایی نهایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` (شامل `generateDebugBuildConfig`) | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| ۴ تست تازه/افزوده‌شده | همگی سبز |
| `gradle :app:testDebugUnitTest` (کل Suite، `--rerun`) | **۷۳۶ تست، ۰ شکست** |
| `gradle :app:assembleDebug` | موفق |

## قدم بعدی پیشنهادی

باقی‌مانده‌ی appendix ADR-081 (یافته‌های سطح ۳ و بزرگ‌تر — مثل Tab
«خروجی» Studio که هنوز Placeholder است، نبود کامل `ModalBottomSheet`،
اتصال واقعی Asset↔Scene برای Tab ASSETS صحنه، پنل خلاصه‌ی زنده‌ی
Composer، هدر سراسری غیریکسان بین صفحات) هنوز رفع نشده و منتظر
اولویت‌بندی/تصمیم معمار پروژه‌اند. لوگوی گرافیکی Aperture-C هم به‌عنوان
یک بدهی طراحی جداگانه (نه کدی) باز ماند.

## Skills استفاده‌شده

`systematic-debugging` — یافته‌ی دیباگ Badge (ادغام Semantics Tree) و
یافته‌ی اصلی مورد ۴ (رنگ Literal، نه Data-bound، در خودِ HTML خام
mockup) هر دو با شواهد مستقیم (پیام خطای Compose، خواندن خط‌به‌خط HTML)
پیش از هر تغییر کد تأیید شدند، نه با حدس. `decision-record` — تصمیم
عدم-Badge برای Tab ASSETS و تفکیک رفع کدی/طراحی مورد ۳ هر دو با دلیل
صریح در همین سند مستند شدند.
