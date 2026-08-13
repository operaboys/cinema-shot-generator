# ADR-081: پیاده‌سازی homeLayoutVariant.RESUME + composerLayoutVariant.ACCORDION طبق mockup موجود

## زمینه

ADR-080 این دو Variant را موکول کرد با این فرض که «نیازمند تصمیم
طراحی/تأیید بصری، نه فقط اتصال». این فرض **اشتباه** بود: یک mockup کامل و
تعاملی از قبل در `docs/design/Cinema Studio.html` وجود دارد که هر دو حالت
جایگزین را دقیق طراحی کرده — فقط با grep سطحی یا خواندن
`docs/design/README.md` قابل کشف نبود، چون محتوای اصلی داخل رشته‌های
JS تودرتوی این فایل ۲.۱ مگابایتی (عملاً یک خط واحد) پنهان است. این قدم
با خواندن مستقیم محتوای خام HTML/JS (نه صرفاً README خلاصه) هر دو بخش
(`homeB`، `composerB`) را پیدا، تحلیل، و پیاده کرد.

## بخش الف — `homeLayoutVariant.RESUME`

### طراحی واقعی mockup (خط تقریبی ۲۰۸۲۷۶۵ فایل خام، `is.homeB`)

یک کارت پیشرفت («ادامه از جایی که ماندید») + نوار ۹ بخشی (یکی به‌ازای هر
`WorkflowStep` — تأیید شد دقیقاً ۹ مرحله در `domain/workflow/WorkflowModels.kt`)
+ دکمه‌ی «ادامه» (گرادیان بنفش، آیکون play_arrow) که به آخرین پروژه‌ی
فعال می‌رود؛ سپس یک شبکه‌ی ۲×۲ کاشی میان‌بر (`homeTiles`: AI
Breakdown/DNA/Validation/Output)؛ سپس فهرست افقی «پروژه‌های دیگر».

### پیاده‌سازی

`HomeScreen.kt`: تابع `HomeResumeContent` تازه، فقط وقتی
`homeLayoutVariant == RESUME` رندر می‌شود (چیدمان HERO موجود کاملاً
دست‌نخورده ماند). `mostRecent = summaries.firstOrNull()` — چون
`projectSummaries` از قبل `ORDER BY lastModified DESC` است
(`ProjectDao.kt`)، همان «آخرین پروژه‌ی فعال» mockup را بدون هیچ Query
تازه‌ای می‌دهد.

**تصمیم مستقل مهم — محدودیت صادقانه‌ی نوار پیشرفت:** mockup فرض می‌کند
یک «مرحله‌ی جاری» per-project روی سرور/دیسک ذخیره شده (نمونه‌اش:
`X.k3`: «مرحله ۵ — Shot Composer»). این اپ چنین چیزی ندارد —
`WorkflowState` عمداً یک‌بار-مصرفِ همان نشست است (طبق ADR-042)، نه
Persisted per-project. اختراع یک مرحله‌ی جعلی برای پروژه‌ای که این نشست
هنوز باز نشده، گمراه‌کننده بود. رفع: وقتی `workflowViewModel.workflowState`
واقعی موجود و برای دقیقاً همین پروژه است (سناریوی واقعی «همین الان از
Studio برگشتم»)، نوار/زیرعنوان واقعی و دقیق نمایش داده می‌شود
(`currentStep.ordinal + 1` بخش پُر، `workflowStepLabel` تازه برای نام
مرحله). در غیر این صورت (اپ تازه باز شده)، نوار خالی می‌ماند و زیرعنوان
از `project.metaTemplate` (شمارش صحنه/شات، همان کلید موجود
`StudioHeader`) استفاده می‌کند — واقعی، نه فرضی.

**کاشی‌های میان‌بر:** ۲ مورد اول (AI Breakdown، DNA) دقیقاً مسیر
Project-محور mockup را دارند — بدون نیاز به یک Shot مشخص، پس کاملاً
واقعی وصل شدند (`onOpenAiBreakdown`/`onOpenProjectTab` تازه، پارامتر
اختیاری روی `HomeScreen` با پیش‌فرض بی‌خطر که به `onOpenProject` قبلی
برمی‌گردد — هیچ فراخوان موجودی نشکست). **تصمیم مستقل:** Validation و
Output در mockup به یک «شات جاری» فرضی می‌روند که این اپ (بر خلاف
mockup) ندارد — نزدیک‌ترین مقصد واقعی و بدون‌خطا انتخاب شد: ورود به
Studio روی Tab «صحنه‌ها» (برای Validation، جایی که کاربر یک شات واقعی
انتخاب می‌کند) و Tab «خروجی» (برای Output).

**«پروژه‌های دیگر»:** `LazyRow` از یک کارت کوچک تازه (`ResumeOtherProjectCard`)
— **تصمیم مستقل:** به‌جای بازاستفاده‌ی کامل `ProjectCard` (که منوی
Overflow کامل رفع‌شده/تغییرنام/حذف دارد)، یک کارت ساده‌تر ساخته شد چون
mockup خودش برای این ردیف افقی هیچ منوی Overflow ای نشان نمی‌دهد —
مدیریت کامل پروژه از قبل در صفحه‌ی Projects موجود است.

فایل‌های تغییریافته: `ui/home/HomeScreen.kt`، `ui/navigation/AppNavHost.kt`
(دو Callback تازه)، `ui/i18n/UiStrings.kt` (کلیدهای تازه: `home.resume.*`،
`home.quickTile.*`، `workflowStep.*` — ۹+۹ کلید fa/en).

## بخش ب — `composerLayoutVariant.ACCORDION`

### طراحی واقعی mockup (خط تقریبی ۲۰۱۱۲۸۰، `composerGroups`)

دقیقاً ۴ گروه هم‌ارز: «اصلی — Shot» (آیکون movie)، «دوربین» (videocam)،
«نور و محیط» (wb_sunny)، «صدا» (graphic_eq) — هرکدام کارت با آیکون+عنوان+
یک خط خلاصه (`rows.slice(0,3).map(r=>r.value).join(' · ')`)+Chevron.
تصمیم دقیق mockup برای باز/بسته: `group: st.group === i ? -1 : i` —
**همیشه فقط یک گروه باز**؛ کلیک روی همان گروه بازِ فعلی آن را کاملاً
می‌بندد (نه فقط سوییچ).

### پیاده‌سازی

`ShotComposerScreen.kt`: فیلدهای «اصلی» (که قبلاً همیشه بالای نوار Tab
رندر می‌شدند، مستقل از Tab انتخابی — رفتار TABS بدون تغییر) به یک تابع
`MainFieldsSection` مجزا استخراج شدند تا هم در TABS (بدون تغییر) و هم
به‌عنوان بدنه‌ی گروه «اصلی» در ACCORDION بازاستفاده شوند. `ComposerAccordion`
تازه: ۴ کارت `ComposerAccordionGroupCard`، وضعیت `expandedGroup:
ComposerAccordionGroup?` (`rememberSaveable`، پیش‌فرض `MAIN` — دقیقاً
طبق `group: 0` اولیه‌ی mockup)، `toggle()` با منطق دقیق mockup
(کلیک روی گروه باز → `null`، کلیک روی گروه دیگر → همان گروه).

**تصمیم مستقل — محاسبه‌ی خلاصه‌ی هر گروه:** چون بدنه‌ی هر گروه در این اپ
یک Composable خودمختار است (نه فهرست ساده‌ی Row های `label`/`value` مثل
mockup)، برای هر گروه ۳ مقدار واقعی و نماینده مستقیماً از
`ShotComposerViewModel` جمع‌آوری شد: MAIN → هدف/نوع/میزان حرکت (۳ Enum
معنادار، نه عنوان/توصیف متن‌آزاد)؛ CAMERA → زاویه/فاصله/نوع لنز (دقیقاً
اولین ۳ فیلد واقعی `CameraTabContent`)؛ LIGHTING (نور+محیط ترکیبی در
این اپ) → سبک نور/موقعیت نور اصلی/نوع آب‌وهوا؛ AUDIO چون فهرست‌محور است،
شمارش هرکدام (`shotComposer.accordion.audioSummaryTemplate` کلید تازه).

فایل‌های تغییریافته: `ui/shots/ShotComposerScreen.kt` (بازساختاردهی +
اضافه‌ی حدود ۲۰۰ خط)، `ui/navigation/AppNavHost.kt` (پارامتر
`composerLayoutVariant` تازه، خوانده‌شده از `workflowViewModel`)،
`ui/i18n/UiStrings.kt` (یک کلید تازه، fa/en).

## راستی‌آزمایی دیباگ واقعی (مستند، نه فرضی)

آزمایش اول تست‌های ACCORDION با `.performClick()` روی سربرگ گروه شکست
خورد (`ComposeTimeoutException`، محتوای گروه هدف هرگز ظاهر نشد) —
دقیقاً همان کلاس مشکل مستندشده‌ی خودِ این پروژه در `ShotsFlowTest.kt`
(«performClick() روی FAB/Card این خانواده از صفحات غیرقابل‌اعتماد است»).
رفع با همان راه‌حل قبلاً مستندشده‌ی این پروژه: `performSemanticsAction
(SemanticsActions.OnClick)` به‌جای `performClick()`. جداگانه،
`testTag` سربرگ گروه از `Card` بیرونی (بدون `onClick` خودش) به داخل
`Row` دارای `.clickable()` منتقل شد تا testTag و Action روی دقیقاً یک
گره باشند.

**یافته‌ی دوم (بعد از رفع اول، هنگام اجرای مکرر با `ANDROID_HOME`
صحیح):** با اجرای واقعی (نه Gradle UP-TO-DATE Cache — که ابتدا با ۱ ثانیه
build اشتباهی «سبز» نشان می‌داد بدون اجرای واقعی تست‌ها)، ۲ از ۳ تست به‌طور
Flaky با دو خطای متفاوت شکست می‌خوردند: `ComposeTimeoutException` روی
`waitUntilAtLeastOneExists(hasText(uiString("studioTab.story", ...)))` و
`IllegalStateException: State must be at least 'CREATED' to be moved to
'DESTROYED'` (Lifecycle داخلی `NavBackStackEntry`). هر دو دقیقاً همان
Stack Trace و همان ریشه‌ی از قبل تشخیص‌داده‌شده‌ی ADR-078/079
(`HomeProjectsStudioFlowTest`): شرط انتظار مبهم بعد از تأیید ایجاد پروژه
(`hasText` روی متنی که هم ممکن است در فهرست Home و هم بعد از Navigate
واقعی به Studio ظاهر شود) با Race بین Navigate آسنکرون و Dispose تست
رقابت می‌کند. چون `openShotComposer()` (تابع Helper مشترک هر ۳ تست این
فایل) این شرط را از صفر نوشته بود، همان اشتباه از قبل رفع‌شده‌ی ADR-079
دوباره تکرار شده بود. رفع: همان راه‌حل نهایی خودِ ADR-079 — جایگزینی
شرط با نشانه‌ی غیرمبهم و منحصر به Studio:
`composeRule.waitUntilAtLeastOneExists(hasTestTag(studioTabTestTag(StudioTab.STORY)),
timeoutMillis = 5_000)` به‌جای `hasText(uiString("studioTab.story", ...))`.
راستی‌آزمایی: **۱۰ اجرای پیاپی واقعی** (با `--rerun` صریح، برای دور زدن
Cache گمراه‌کننده‌ی Gradle که در غیر این صورت یک build غلط «موفق در ۱
ثانیه» بدون اجرای واقعی تست نشان می‌داد)، هر ۱۰ اجرا موفق، هم‌تراز
استاندارد ADR-072/079.

## تست

- `HomeResumeLayoutFlowTest.kt` (تازه، ۳ تست): نمایش کارت RESUME به‌جای
  HERO؛ دکمه‌ی «ادامه» واقعاً پروژه را باز می‌کند؛ کاشی DNA واقعاً Tab
  DNA را باز می‌کند (اثبات با محتوای واقعی `dna.group.coreIdentity`).
- `ShotComposerAccordionFlowTest.kt` (تازه، ۳ تست): گروه «اصلی» پیش‌فرض
  باز است با فیلدهای واقعی؛ باز کردن گروه دیگر گروه قبلی را می‌بندد
  (فقط یک گروه هم‌زمان)؛ کلیک دوباره روی گروه باز آن را کاملاً می‌بندد.

## راستی‌آزمایی نهایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | **۷۲۷ تست، ۰ شکست** |
| `gradle :app:assembleDebug` | موفق |

## بخش پ — بررسی سراسری mockup (دستور کار بخش دوم)

نتیجه‌ی کامل این بررسی (۱۲ صفحه + ۴ عنصر مشترک، با مرجع دقیق mockup و
کد فعلی برای هر مورد) در پیوست جداگانه‌ی همین سند آمده:
`docs/adr/081-appendix-mockup-audit.md` — این فهرست عمداً خلاصه نشده،
چون خودش ارزش مرجع دارد. طبق قانون این قدم، هیچ‌کدام از یافته‌های آن
بررسی (جز همین دو مورد RESUME/ACCORDION که موضوع اصلی این قدم بودند)
در این قدم رفع نشدند — منتظر تصمیم/تأیید معمار.

## Skills استفاده‌شده

`systematic-debugging` — فرضیه‌ی اول (`performClick()` مستقیم) با شواهد
واقعی (Timeout مکرر با دو رفع جزئی مختلف) رد شد پیش از پذیرش راه‌حل
مستندشده‌ی خودِ این پروژه. `decision-record` — هر تصمیم مستقل (محدودیت
صادقانه‌ی نوار پیشرفت، مقصد جایگزین Validation/Output، منبع خلاصه‌ی هر
گروه Accordion) با دلیل صریح مستند شد.
