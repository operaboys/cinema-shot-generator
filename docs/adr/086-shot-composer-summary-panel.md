# ADR-086: پنل خلاصه‌ی زنده‌ی پایین Shot Composer (یافته‌ی #۱۳ appendix ADR-081، آخرین یافته‌ی سطح ۴ باقی‌مانده)

## زمینه

طبق appendix ADR-081، Shot Composer فاقد یک پنل خلاصه‌ی زنده در پایین صفحه بود
که وضعیت اعتبارسنجی (Blocking/Warning) و مدل هدف فعلی را در همان لحظه، بدون
نیاز به باز کردن صفحه‌ی Validation، نشان دهد. این آخرین یافته‌ی سطح ۴ باقی‌مانده
از appendix بود.

## ۱. استخراج دقیق متن mockup (x.k42–k45)

خواندن مستقیم `docs/design/Cinema Studio.html` (دیکشنری ترجمه + بخش
`composerB` صفحه‌ی Shot Composer) نشان داد:
- `x.k42`/`x.k43` قالب‌های شمارش Blocking/Warning‌اند (`"{count} Blocking"`/
  `"{count} Warning"`، دقیقاً به لاتین، حتی در دیکشنری فارسی — مثل بسیاری از
  اصطلاحات فنی این اپ که عمداً ترجمه نشده‌اند).
- `x.k44` («Full Video Prompt») یک **برچسب ثابت بخش**، نه متن زنده‌ی Prompt.
- `x.k45` («رفع خطاهای بالا لازم است») هشدار شرطی کارت، فقط وقتی Blocking>۰.

## ۲. یافته‌ی اصلاح‌کننده‌ی فرض اول — کارت پیش‌نمایش Prompt متن زنده نیست

خواندن دقیق markup خام (نه فرض از روی نام `x.k44`) ثابت کرد این کارت فقط یک
برچسب بخش + هشدار شرطی رندر می‌کند، نه خروجی واقعی `renderBlueprintToText`.
بنابراین آیتم ۵ دستور کار (بررسی نیاز Debounce برای رندر Prompt) **موضوعیت
ندارد** — یافته‌ای که به‌جای اجرای کورکورانه‌ی دستور کار، صریحاً در همین سند
ثبت می‌شود؛ کارت این پنل فقط از `validationSummary.blockingCount` (همان داده‌ی
بخش ۳) برای تصمیم نمایش هشدار استفاده می‌کند.

## ۳. یافته‌ی اصلاح‌کننده‌ی فرض دوم — چیپ مدل قابل‌کلیک نیست

`docs/design/Cinema Studio.html`: خودِ `<div>` بیرونی هر چیپ مدل **هیچ**
attribute ای از نوع `sc-camel-on-click` ندارد — فقط `<span>` آیکون کپی
داخلش این attribute را دارد. یعنی برخلاف فرض اولیه‌ی دستور کار («کلیک روی چیپ
شاید مدل هدف را عوض کند»)، چیپ صرفاً **نمایشگر بصری** مدل سراسری فعلی
(`WorkflowState.selectedModelProfileId`، همان مفهوم استفاده‌شده در
`OutputDeliveryScreen`) است؛ فقط آیکون کپی داخلش تعاملی است. گرد‌آوری شواهد از
grep مستقیم `OutputDeliveryViewModel`/`OutputDeliveryScreen` هم تأیید کرد این
دو مفهوم (مدل سراسری Session در برابر انتخاب هرشات) از قبل یکی‌اند — چیپ این
پنل چیز تازه‌ای اختراع نمی‌کند.

چهار چیپ نمایش‌داده‌شده دقیقاً همان زیرمجموعه‌ی `chipKeys` mockup‌اند
(`veo_3_1`/`kling_3_0`/`runway_gen_4_5`/`seedance_2_5`)، نه هر ۱۳ پروفایل
`ModelProfileLibrary` — طبق `ComposerSummaryModelChipKeys`.

`chev` که ابتدا یک نشانگر Expand/Collapse به‌نظر می‌رسید، بعد از خواندن دقیق
markup، یک متغیر آیکون سراسری/زبان‌محور (`chevron_left`/`chevron_right` بر
اساس `st.lang`) است که در ردیف‌های نامرتبط زیادی بازاستفاده شده — نه یک کنترل
واقعی مختص این پنل؛ در پیاده‌سازی فقط یک آیکون تزئینی انتهای نوار
Blocking/Warning است.

## ۴. تصمیم — جای‌گیری پنل و دکمه‌ی Output Delivery

طبق دستور کار، اجازه‌ی تصمیم صریح درباره‌ی جایگزینی دو دکمه‌ی قبلی
(«اعتبارسنجی این شات» / «Output Delivery») با پنل تازه داده شده بود. تصمیم:
- دکمه‌ی Validation قبلی (`SHOT_COMPOSER_VALIDATION_BUTTON_TAG`) **حذف** و
  رفتارش (`onNavigateToValidation`) به کل نوار Blocking/Warning پنل تازه
  منتقل شد — هم‌الگو با `act.goValidation` مسیر mockup.
- دکمه‌ی Output Delivery (`SHOT_COMPOSER_OUTPUT_DELIVERY_BUTTON_TAG`) **حفظ**
  شد، درست زیر پنل تازه — هرچند mockup این پنل هیچ دکمه‌ی مستقیم Output
  Delivery‌ای نشان نمی‌دهد (فقط نوار Validation + چیپ‌های مدل). حذف این دکمه
  یک تصمیم مستند قبلی (ADR-057: «نقطه‌ی ورود می‌تواند هم از Validation و هم
  مستقیم از Shot Composer باشد») را بی‌سروصدا نقض می‌کرد، پس بدون تغییر نگه
  داشته شد.

## ۵. یافته‌ی واقعی دیباگ — چیدمان Pinned باعث فشرده‌شدن محتوای اصلی به ۰dp می‌شد

طراحی اولیه، پنل + دکمه‌ی Output Delivery را به‌صورت واقعاً Pinned (`flex:none`
دقیق mockup) بیرون از ناحیه‌ی Scroll میانی (`weight(1f)` + `verticalScroll`
جدا) قرار می‌داد. تست End-to-End واقعی (`ShotComposerAccordionFlowTest`، حالت
ACCORDION) روی Viewport واقعی تست (اندازه‌گیری مستقیم: `320×470dp`) این
ترکیب را اجرا کرد و مستقیماً اثبات کرد که مجموع ارتفاع ثابت (Header + Hero +
پنل تازه + دکمه‌ی Output Delivery) از کل ارتفاع صفحه بیشتر می‌شود و ناحیه‌ی
Scroll میانی را به‌طور کامل به `Rect.fromLTRB(0,0,0,0)` فشرده می‌کند —
اندازه‌گیری مستقیم `boundsInRoot` گره MAIN، نه فرض. یعنی محتوای فرم اصلی
(عنوان/توصیف/هدف/...) اصلاً در درخت نمایش‌داده‌نشده باقی می‌ماند؛ چون ناحیه‌ی
Scroll خودش ارتفاع صفر داشت، `performScrollTo()` هم کمکی نمی‌کرد.

**رفع**: به‌جای دو ناحیه‌ی Scroll جدا (میانی + Pinned)، همه‌چیز بعد از Header
(Hero + محتوای Tab/Accordion + پنل خلاصه + دکمه‌ی Output Delivery) در یک
Column با یک Scroll واحد قرار گرفت. این یک **انحراف مستند از `flex:none`
دقیق mockup** است — پنل دیگر به‌صورت فنی Pinned نیست — اما تضمین می‌کند هیچ
محتوایی هرگز واقعاً غیرقابل‌دسترس نشود. روی هر دستگاه واقعی (که طول صفحه‌اش
خیلی بیشتر از ۴۷۰dp این Viewport تستی است) پنل همچنان بلافاصله بعد از محتوا
و نزدیک پایین صفحه دیده می‌شود — تفاوت فقط زیر شرایط شدیداً محدود (که هیچ
دستگاه واقعی هدف این اپ ندارد) قابل‌مشاهده است.

این تغییر ساختاری باعث شد دکمه‌های Validation/Output Delivery در دو تست
موجود دیگر (`ValidationFlowTest`، `OutputDeliveryFlowTest`) هم نیاز به
`performScrollTo()` پیدا کنند؛ هر دو تست رفع شدند (`clickViaSemantics()`
به‌جای `performClick()` مختصاتی، طبق یافته‌ی قبلاً مستندشده‌ی این پروژه برای
عناصر Clickable-Row/Card).

## ۶. کپی نام مدل به Clipboard

آیکون `content_copy` داخل هر چیپ با `LocalClipboardManager.current.setText(
AnnotatedString(displayName))` مستقیماً در محل کلیک — دقیقاً هم‌الگو با
`OutputDeliveryScreen.kt` (نه از طریق یک callback بالادستی که این نوشتن را
تأخیر بیندازد).

**یافته‌ی واقعی دیباگ تست**: فراخوانی `composeRule.waitForIdle()` بلافاصله
بعد از این کلیک باعث `OutOfMemoryError` واقعی می‌شد (`ArrayDeque` در
`ShadowTrace.endSection` رباتیک، بعد از بیش از ۴ دقیقه Pump شدن فریم) — چون
این کلیک هم‌زمان `onShowMessage` را هم صدا می‌زند که یک Snackbar واقعی
(`snackbarHostState.showSnackbar`) باز می‌کند؛ تایمر/Animation آن Snackbar
زیر ساعت فریم مجازی تست هرگز به Idle واقعی نمی‌رسید. رفع لازم نبود چون نوشتن
Clipboard خودش همگام و *قبل* از فراخوانی `onShowMessage` در همان Lambda کلیک
انجام می‌شود — تست فقط کافی است بعد از کلیک (بدون `waitForIdle()`) مستقیماً
Clipboard را بخواند.

## پیاده‌سازی

- **`ui/shots/ShotComposerViewModel.kt`**: `refreshValidationSummary` تازه
  (بازاستفاده‌ی مستقیم `aggregateShotValidation` موجود، همان الگوی
  `ValidationViewModel`/`StudioOutputViewModel`) + کش یک‌باره‌ی
  Scene/DNA/۳ فهرست Asset در `init` (چون `characterIds`/`objectIds`/
  `locationIds` در هیچ Tab دیگری از Composer ویرایش نمی‌شوند) + فراخوانی از
  همان نقطه‌ی `save()` موجود (بدون Debounce تازه — دلیل در بخش ۷).
- **`ui/shots/ShotComposerScreen.kt`**: `ComposerSummaryFooter` تازه؛ تغییر
  ساختار Scroll (بخش ۵)؛ کپی Clipboard مستقیم.
- **`ui/i18n/UiStrings.kt`**: ۶ کلید fa+en تازه.
- **`ui/navigation/AppNavHost.kt`**: تزریق `sceneRepository`/
  `projectDnaRepository`/`assetRepository`/`selectedModelProfileId`/
  `onShowMessage` به `ShotComposerScreen`.

## ۷. تصمیم مستند — بدون Debounce

طبق قانون صریح دستور کار («اگر اعتبارسنجی زنده روی هر Keystroke واقعاً کند
شود، این را صریح در گزارش بگو»)، این مورد ارزیابی شد: `aggregateShotValidation`
یک تابع خالص و درون‌حافظه‌ای است (بدون I/O) که دقیقاً روی همان نقطه‌ی
تریگر (`save()`) سوار می‌شود که از قبل هر بار یک نوشتن واقعی I/O روی
`repository.saveShot` انجام می‌دهد. یعنی هزینه‌ی این محاسبه‌ی تازه، به‌طور
قطع کمتر از هزینه‌ی I/O ای است که از قبل، بدون شکایت، روی هر Keystroke اجرا
می‌شد. نتیجه: **جنک واقعی وجود ندارد**، نیازی به Debounce نیست.

## تست

- `ShotComposerSummaryFlowTest.kt` (تست تازه، ۲ مورد End-to-End واقعی):
  شمارش صحیح Blocking برای شاتی با نقض واقعی «نور خورشید در شب» (همان
  فیکسچر `ValidationFlowTest.kt`)؛ کپی چیپ مدل → محتوای واقعی Clipboard
  سیستم.
- رفع رگرسیون در `ShotComposerAccordionFlowTest.kt`/`ShotsFlowTest.kt`:
  تزریق صریح `projectDnaRepository` تست‌محور (وگرنه `ShotComposerViewModel.
  factory` بی‌صدا به `AppDatabase.getInstance()` واقعی Production برمی‌گشت
  — همان الگوی باگ تاریخی G16).
- رفع رگرسیون در `ValidationFlowTest.kt`/`OutputDeliveryFlowTest.kt`: افزودن
  `performScrollTo()`/`clickViaSemantics()` برای دکمه‌های دیگر Pinned-نبودن
  (بخش ۵).

## راستی‌آزمایی نهایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest --tests "...ui.shots.*"` | **۱۶ تست، ۰ شکست** |
| `gradle :app:testDebugUnitTest` (کل Suite) | **۷۴۴ تست، ۰ شکست واقعی** (۳ شکست اولیه‌ی گذرا زیر بار سنگین کل Suite — `AssetFormFlowTest`/`AppNavigationTest`، هر دو بدون ارتباط با این قدم — در اجرای مجزا ۱۰۰٪ سبز، تأییدشده) |
| `gradle :app:assembleDebug` | موفق |

## قدم بعدی پیشنهادی

آخرین یافته‌ی appendix ADR-081 (پنل خلاصه) با این قدم بسته شد. باقی‌مانده‌ی
شناخته‌شده‌ی appendix: نبود هدر سراسری یکسان بین صفحات (تصمیم معماری بزرگ‌تر،
منتظر اولویت‌بندی معمار پروژه).

## Skills استفاده‌شده

`zero-hallucination-coder` — دو فرض اولیه‌ی دستور کار (متن زنده‌ی Prompt،
چیپ قابل‌کلیک) هر دو با خواندن مستقیم markup خام رد شدند، نه با حدس از روی
نام کلید. `verification-before-completion` — رگرسیون واقعی چیدمان Pinned با
اندازه‌گیری مستقیم `boundsInRoot` (نه فرض) کشف و رفع شد؛ همه‌ی تست‌های
مرتبط (`ui.shots.*`، `ValidationFlowTest`، `OutputDeliveryFlowTest`) هم در
ایزوله هم در کل Suite تأیید شدند. `decision-record` — هر ۷ تصمیم/یافته‌ی
این سند (شامل انحراف از `flex:none`) با دلیل صریح ثبت شدند.
