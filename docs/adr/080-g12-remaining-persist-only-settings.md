# ADR-080: رفع ۳ فیلد بی‌اثر باقی‌مانده‌ی G12 (dynamicFontEnabled، allowFreeStepJump، homeScreenImageUri) — موکول ۲ مورد باقی‌مانده

## زمینه

ADR-067 (بخش ب) از ۵ فلگ Persist-only Settings، فقط `minTouchTargetEnabled`
را واقعاً وصل کرد و ۵ مورد دیگر (`dynamicFontEnabled`، `reducedMotionEnabled`،
`allowFreeStepJump`، `homeScreenImageUri`، `homeLayoutVariant`،
`composerLayoutVariant`) را عمداً بی‌اثر گذاشت — `reducedMotionEnabled` چون
هیچ انیمیشنی در کل اپ وجود ندارد (هنوز درست است، خارج از Scope این قدم).
این قدم دستور کار صریح داشت هر ۵ مورد باقی‌مانده را بررسی و رفع کند، مگر
موردی واقعاً نیازمند تصمیم طراحی/تأیید بصری باشد.

## راستی‌آزمایی مستقل پیش‌بررسی‌های دستور کار

با `grep -rn "dynamicFontEnabled\|allowFreeStepJump\|homeScreenImageUri\|
homeLayoutVariant\|composerLayoutVariant" app/src/main` هر ۵ مورد
مستقلاً تأیید شدند: در `WorkflowViewModel.kt` هرکدام یک `MutableStateFlow`
واقعی Persist‌شده در DataStore دارند، در `SettingsScreen.kt` یک سوییچ/رادیو
واقعی UI دارند، اما در هیچ Composable دیگری از کل `ui/` مصرف نمی‌شوند —
دقیقاً هم‌راستا با یافته‌ی ADR-067.

برای `allowFreeStepJump` یک نکته‌ی اضافی (نه در پیش‌بررسی دستور کار) با خواندن
مستقیم `StudioTopTabRow.kt` کشف شد: `evaluateStudioTabJump`/`canJumpToStep`
(طبق ADR-037) هرگز خودِ پرش را Block نمی‌کنند — فقط یک پیام هشدار برمی‌گردانند
که همیشه با `onWarning` (Snackbar) نمایش داده و بلافاصله دنبال می‌شود. یعنی
پرش آزاد از قبل همیشه رفتار پیش‌فرض واقعی بود، مستقل از این فلگ.

## بخش الف — `dynamicFontEnabled`: رفع شد

هم‌الگو دقیق با `minTouchTargetEnabled` (`ui/theme/AccessibilityLocals.kt`)،
اما با یک تفاوت معماری آگاهانه: `minTouchTargetEnabled` یک `Modifier` محلی
روی یک هدف مشخص (`PhaseCircle`) بود؛ «فونت پویا» باید کل اپ را بپوشاند بدون
دست‌کاری تک‌تک Composable ها — راه رسمی Compose برای این کار Override کردن
`LocalDensity.fontScale` است (`Theme.kt`، `CinemaShotGeneratorTheme`)، نه
یک Modifier جدید. تمام `Text`/`TextField` موجود و آینده خودکار از آن پیروی
می‌کنند چون همه‌شان از `LocalDensity.current` برای تبدیل `sp→px` استفاده
می‌کنند.

**ضریب ۱.۳ (تصمیم مستقل، مستند):** نزدیک‌ترین معادل به پیش‌فرض «Large Text»
رسمی تنظیمات Accessibility اندروید، به‌جای اختراع یک مقدار دلبخواه تازه.
ضرب در `density.fontScale` موجود (نه Override کامل آن) عمدی است — تنظیم
اندازه‌ی فونت سطح سیستم کاربر هم محترم شمرده می‌شود؛ این سوییچ فقط یک ضریب
اضافه روی همان مقدار پایه است.

فایل‌های تغییریافته: `ui/theme/Theme.kt` (پارامتر تازه `dynamicFontEnabled: Boolean = false`
با پیش‌فرض false — رفتار فراخوان‌های موجود/تست‌ها بدون تغییر)، `ui/App.kt`
(خواندن مقدار واقعی از `workflowViewModel.dynamicFontEnabled` و پاس دادن).

## بخش ب — `allowFreeStepJump`: رفع شد، با تفسیر معنایی مستند

**معنای واقعی این سوییچ (تصمیم مستقل، طبق پیشنهاد دستور کار):** چون
`canJumpToStep` هرگز خودِ پرش را Block نمی‌کند، این فلگ نمی‌تواند «آیا پرش
ممکن است» را کنترل کند (که همیشه بله است) — پس کنترل می‌کند «آیا هشدار
موجود، پیش از پرش، به یک تأیید صریح کاربر نیاز دارد یا نه»:
- `true` (پیش‌فرض فعلی، هم‌تراز دقیق رفتار قبلی): هشدار فقط به‌صورت
  غیرمزاحم (Snackbar از طریق `onWarning`) نمایش داده می‌شود و پرش بلافاصله
  انجام می‌شود.
- `false`: یک `AlertDialog` تأیید صریح باز می‌شود (عنوان + متن هشدار واقعی)؛
  فقط با کلیک روی دکمه‌ی تأیید، `onTabSelected` واقعاً صدا زده می‌شود؛ Cancel
  هیچ تغییری اعمال نمی‌کند.

وقتی اصلاً هشداری وجود ندارد (مثلاً `workflowState == null`، یا همه‌ی مراحل
قبلی کامل‌اند)، پرش در هر دو حالت بلافاصله انجام می‌شود — تأیید صریح فقط
وقتی معنا دارد که واقعاً چیزی برای هشدار دادن باشد.

فایل‌های تغییریافته: `ui/navigation/StudioTopTabRow.kt` (پارامتر تازه
`allowFreeStepJump: Boolean = true`؛ `State` محلی `pendingJump` برای تب در
انتظار تأیید؛ `AlertDialog` تازه با کلیدهای UiStrings تازه
`studioTab.jump.confirmTitle`/`confirm`/`cancel`، هم‌الگو نام‌گذاری با
`project.archive.confirm`/`cancel`)، `ui/studio/StudioShell.kt` (خواندن
`workflowViewModel.allowFreeStepJump` و پاس دادن)، `ui/i18n/UiStrings.kt`
(۳ کلید تازه، fa+en).

## بخش ج — `homeScreenImageUri`: رفع شد

`HomeImageCard` (Settings) از قبل یک Picker واقعی داشت
(`ActivityResultContracts.OpenDocument()` + `takePersistableUriPermission`،
طبق ADR-075/G10) — فقط خودِ مقدار Persist می‌شد، هیچ‌جای دیگر رندر نمی‌شد.
`HomeScreen.kt` اکنون وقتی این مقدار غیر-null است، آن را به‌عنوان پس‌زمینه‌ی
Full-bleed واقعی رندر می‌کند (دقیقاً طبق `docs/design/README.md`)، با یک
Scrim گرادیانی روی آن برای خوانایی متن؛ وقتی null است، رفتار قبلی (فقط
گرادیان تخت Placeholder) بدون تغییر می‌ماند.

**تصمیم مستقل — بدون افزودن کتابخانه‌ی تصویر تازه (Coil/Glide):** با `grep`
تأیید شد کل کدبیس فعلاً هیچ کتابخانه‌ی تصویری ندارد. به‌جای افزودن یک
Dependency تازه فقط برای این یک قابلیت، دیکود مستقیماً با API بومی اندروید
انجام شد (`ContentResolver.openInputStream` + `BitmapFactory.decodeStream`،
روی `Dispatchers.IO` داخل `produceState` تا از Jank جلوگیری شود؛ خطای
دیکود/Uri نامعتبر با `runCatching` به `null` تبدیل می‌شود — بدون Crash، بدون
رندر). هم‌راستا با اصل «بدون Dependency تازه مگر لازم» این پروژه.

فایل تغییریافته: `ui/home/HomeScreen.kt` (`HomeBackgroundImage` Composable
تازه + شرط رندر در بدنه‌ی اصلی `HomeScreen`).

## بخش د — `homeLayoutVariant`/`composerLayoutVariant`: موکول شد

برخلاف ۳ مورد بالا (که یا یک الگوی معماری موجود را تکرار می‌کردند، یا فقط
یک مقدار از‌قبل‌موجود را به یک محل رندر متصل می‌کردند)، این دو یک طراحی UI
واقعی و تازه نیاز دارند، نه صرفاً اتصال:

- **`homeLayoutVariant.RESUME`** طبق `docs/design/README.md`
  («B "Resume" — continue-where-you-left-off emphasis») باید یک تأکید
  بصری تازه روی «آخرین پروژه‌ی فعال» بسازد — نیازمند یک کامپوننت تازه
  (نوع کارت، محتوا، سلسله‌مراتب بصری متفاوت از Hero فعلی)، نه فقط
  چیدن‌دوباره‌ی عناصر موجود `HomeScreen.kt`.
- **`composerLayoutVariant.ACCORDION`** طبق سند («B "Accordion" —
  collapsible groups») باید ۴ گروه فیلد `ShotComposerScreen.kt` (اکنون
  Tab-محور با `SecondaryTabRow`) را به گروه‌های قابل‌جمع‌شدن تبدیل کند —
  نیازمند تصمیم روی حالت پیش‌فرض باز/بسته، اینکه آیا چند گروه هم‌زمان باز
  می‌مانند، فاصله‌گذاری/سربرگ هر گروه — یک بازطراحی واقعی ساختار صفحه، نه
  یک تغییر کم‌ریسک.

طبق قانون این پروژه («فقط UI که بدون mockup/تأیید بصری قبلی پیش نمی‌رود»)
و اجازه‌ی صریح دستور کار این قدم، این دو مورد **پیاده‌سازی نشدند** — منتظر
یک تصمیم طراحی/تأیید بصری از معمار پروژه، نه یک محدودیت فنی.

## تست

- `docs/adr/../ThemeTest.kt` (تازه): ثابت می‌کند `dynamicFontEnabled=true`
  واقعاً `LocalDensity.fontScale` مؤثر را در ۱.۳ ضرب می‌کند؛ `false` آن را
  دست‌نخورده می‌گذارد.
- `StudioTopTabRowJumpConfirmationTest.kt` (تازه): ۴ سناریو — `true` بدون
  Dialog می‌پرد (با هشدار Snackbar)؛ `false` Dialog نشان می‌دهد و تا تأیید
  صریح نمی‌پرد؛ Cancel هیچ پرشی انجام نمی‌دهد؛ بدون هشدار واقعی (بدون
  `WorkflowState`) حتی با `false` بلافاصله می‌پرد.
- `HomeScreenBackgroundImageTest.kt` (تازه): با یک فایل PNG واقعی (نه
  Mock) و یک Uri واقعی `file://`، ثابت می‌کند تصویر واقعاً دیکود و رندر
  می‌شود؛ حالت بدون URI هیچ گره‌ی تصویری نمی‌سازد.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | **۷۲۱ تست، ۰ شکست** |
| `gradle :app:assembleDebug` | موفق |

## تصمیمات مستقل (خلاصه)

1. ضریب فونت پویا ۱.۳ (نه مقدار دیگر) — معادل پیش‌فرض «Large Text» اندروید.
2. `allowFreeStepJump` به «آیا هشدار موجود نیاز به تأیید صریح دارد» تفسیر
   شد، نه «آیا پرش مجاز است» — چون دومی از قبل همیشه true بوده (ADR-037).
3. دیکود تصویر با API بومی اندروید، بدون افزودن Coil/Glide.
4. `homeLayoutVariant`/`composerLayoutVariant` موکول شدند — نیازمند تصمیم
   طراحی/تأیید بصری واقعی، نه فقط اتصال کد.

## Skills استفاده‌شده

`decision-record` — هر تصمیم مستقل (ضریب ۱.۳، تفسیر معنایی
`allowFreeStepJump`، عدم افزودن Dependency تصویر، موکول‌کردن ۲ مورد طراحی)
با دلیل صریح مستند شد. `verification-before-completion` — قبل از هر ادعای
موفقیت، خروجی واقعی compile/test/assemble در همین قدم اجرا و خوانده شد
(۷۲۱/۷۲۱ موفق، نه فقط ادعا).
