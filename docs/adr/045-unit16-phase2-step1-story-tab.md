# ADR-045: واحد ۱۶ — فاز ۲، قدم ۱: لایه‌ی ذخیره‌سازی StoryContext + Story Tab واقعی

**تاریخ:** 2026-08-04
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL`.

## Context

قبل از این قدم، `domain.story.StoryContext` (واحد ۰۱) هیچ مسیر ذخیره‌سازی‌ای
نداشت (تأییدشده با grep) — داستانی که کاربر در Story Wizard می‌ساخت با بستن اپ
گم می‌شد؛ دقیقاً همان کلاس باگ که پیش از این با `SceneConditionsDto`/فیلد `state`
پروژه (ADR-036/044) کشف و رفع شده بود. این قدم اول لایه‌ی ذخیره‌سازی را می‌سازد،
سپس Tab «داستان» استودیو (Placeholder فاز ۱) را با محتوای واقعی جایگزین می‌کند.

## تصمیم ۱: فیلدهای مسطح (نه `xDataJson`) برای `StoryContextEntity`

برخلاف `ProjectDnaEntity` (که یک بستهٔ JSON تک‌ستونی است، چون `ProjectDna` ساختار
عمیقاً تودرتو دارد)، `StoryContext` فقط فیلدهای Scalar/Enum ساده دارد؛ بسته‌بندی
JSON برای این مقدار داده هزینه‌ی بی‌دلیل اضافه می‌کرد. تنها فیلد غیر-Scalar،
`genre: List<Genre>`، به‌صورت رشته‌ی جداشده با کاما در یک ستون TEXT ذخیره می‌شود
(نه Room `TypeConverter` — پروژه در هیچ‌جای دیگری از TypeConverter استفاده
نمی‌کند؛ Mapping دستی در `StoryMappers.kt` هم‌الگو با تمام Repository های دیگر
است).

## تصمیم ۲: `projectId` مستقیماً `@PrimaryKey` است (نه یک `storyContextId` مصنوعی)

رابطه‌ی Project↔StoryContext واقعاً یک‌به‌یک است (خروجی Story Wizard، نه فهرستی
از StoryContext های متعدد)، و خودِ `StoryContext` دامنه هیچ فیلد `id` ای ندارد.
ساختن یک شناسه‌ی مصنوعی بدون معادل دامنه دقیقاً همان نوع اختراع بی‌دلیل بود که
این پروژه از آن پرهیز می‌کند (طبق type-registry.md).

## تصمیم ۳: پیش‌فرض `StoryType.NARRATIVE`/`Mood.CALM` برای پروژه‌ی تازه

`StoryContext` دو فیلد الزامی بدون پیش‌فرض دارد. برای یک پروژه‌ی تازه (هیچ
StoryContext ذخیره‌نشده)، باید مقداری اولیه انتخاب شود تا فرم قابل نمایش باشد:
`StoryType.NARRATIVE` (عمومی‌ترین/رایج‌ترین نوع) و `Mood.CALM` (خنثی‌ترین گزینه،
نه یک Mood بارگذاری‌شده‌ی احساسی/ژانری خاص) — کاربر معمولاً همان قدم اول این هر
دو را واقعاً انتخاب می‌کند؛ `genre` خالی می‌ماند (طبق `deriveCompletionStatus`،
یعنی `CompletionStatus.PARTIAL`).

## تصمیم ۴: «فیلد عنوان» همان `Project.projectName` است

با grep تأیید شد `StoryContext` هیچ فیلد عنوان ندارد. دو گزینه بود: فیلد جدید
مستقل، یا بازاستفاده از `Project.projectName` موجود. تصمیم: بازاستفاده — فیلد
عنوان Story Tab مستقیماً `ProjectListViewModel.renameProject(projectId, newName)`
(زیرساخت کامل و تست‌شده‌ی فاز ۱) را صدا می‌زند؛ افزودن یک فیلد «عنوان» مستقل روی
StoryContext یعنی دو منبع حقیقت برای همان مفهوم (نام پروژه)، دقیقاً نوع
Duplication ای که این پروژه بارها (ADR-036/038) از آن پرهیز کرده.

## تصمیم ۵: Stepper های «هدف تعداد شات»/«ثانیه به‌ازای شات» — State محلی، بدون ذخیره‌سازی Room

با grep تأیید شد این دو فیلد در واقع اعضای `StoryBreakdownRequest` (واحد ۰۱ب،
`PromptBuilder.kt`) هستند، نه `StoryContext`. طبق تصریح صریح
`docs/design/README.md` («Story tab: ... story textarea (**read-only display in
prototype**)»)، ویرایش واقعی داستان آزاد در «تفکیک داستان با AI» (قدم ۲ همین فاز،
هنوز ساخته نشده) اتفاق می‌افتد؛ Story Tab فقط یک پیش‌نمایش/جمع‌بندی است. بر همین
اساس: این دو Stepper در `StoryTabContent` فقط `rememberSaveable` محلی‌اند (بدون
ذخیره‌سازی Room)، آماده برای پاس‌شدن به فاز AI Breakdown وقتی آن صفحه ساخته شود —
نه یک محدودیت پنهان، یک تصمیم صریح مطابق مرز واقعی این دو واحد.

## تصمیم ۶: VisualIntent/NarrativeIntensity به‌صورت Dropdown با enum واقعی (نه Textarea/Slider آزاد طبق یادداشت v4 بلوپرینت)

`docs/blueprints/16-user-workflow-v2.md` بخش v4 «مشخصات دقیق فرم» می‌گوید:
«Visual Intent (Textarea کوتاه، رشته‌ی آزاد)» و «Narrative Intensity (Slider یا
Dropdown **سه‌سطحی**)». اما نوع دامنه‌ی واقعی (`domain.story.StoryContext.kt`)
هر دو را enum تعریف کرده — `VisualIntent` (۵ مقدار: REALISTIC/CINEMATIC/
STYLIZED/ARTISTIC/ABSTRACT) و `NarrativeIntensity` (**۴** مقدار: MINIMAL/
MODERATE/HIGH/EXTREME، نه سه). این یک تناقض واقعی بین یادداشت قدیمی‌تر بلوپرینت و
نوع Kotlin واقعی است — طبق انضباط «نوع دامنه‌ی واقعی منبع حقیقت است»، هر دو
به‌صورت Dropdown با تمام مقادیر enum واقعی رندر شدند، نه رشته‌ی آزاد/سه گزینه.

## تصمیم ۷: `storyRepository` تزریق‌پذیر — رفع باگ واقعی کشف‌شده حین تست

**یافته‌ی واقعی، نه فرضی:** `StoryViewModel` با `viewModel(factory = StoryViewModel.factory(application, projectId))` داخل `StoryTabContent` ساخته می‌شود. بدون
پارامتر Repository تزریق‌پذیر، این Factory از `AppDatabase.getInstance(application)`
(Singleton **واقعی دستگاه**) استفاده می‌کرد — نه دیتابیس In-Memory تست. در تست
اول (`StoryTabFlowTest`)، نوشتن‌ها بی‌صدا «موفق» می‌شدند اما به دیتابیس اشتباهی
می‌رفتند؛ بررسی مستقیم دیتابیس In-Memory تست (`database.storyDao().loadStoryContextForProject(...)`)
بعد از کلیک، مقدار `null` برگرداند — حتی با ۵ ثانیه Polling — که این باگ را قطعی
اثبات کرد.

**رفع:** `storyRepository: StoryRepository?` (پیش‌فرض `null`) به‌عنوان پارامتر
تزریق‌پذیر در زنجیره‌ی کامل اضافه شد: `App.kt` (ساخت یک نمونه‌ی مشترک با
`remember`، هم‌الگو با `workflowViewModel`/`projectListViewModel`) →
`MainScaffold` → `AppNavHost` → `StudioShell` → `StoryTabContent` →
`StoryViewModel.factory`. این دقیقاً همان الگوی تست‌پذیری‌ای است که فاز ۰/۱ برای
`WorkflowViewModel`/`ProjectListViewModel` برقرار کرده بودند (ساخت یک‌بار در بالا،
تزریق به‌جای Lookup داخلی Singleton) — این قدم فقط همان الگو را برای اولین
ViewModel ساخته‌شده با `viewModel(factory=...)` **داخل** یک Composable مصرف‌کننده
(نه در ریشه‌ی درخت) تکرار کرد.

## تصمیم ۸: `performSemanticsAction` به‌جای `performClick` برای FilterChip/آیتم‌های ExposedDropdownMenu در تست

**یافته‌ی واقعی دوم، حین تست:** `performClick()` (تزریق لمس واقعی با مختصات
محاسبه‌شده از Layout) روی `FilterChip`(ژانر) و آیتم‌های `ExposedDropdownMenu`
(Mood) به‌طور کاملاً تکرارپذیر «موفق» گزارش می‌شد (بدون خطا) اما وضعیت واقعی
(`Selected`) هرگز تغییر نمی‌کرد — با اینکه گره دقیقاً درون محدوده‌ی Viewport بود و
`Actions=[OnClick]` واقعی داشت (تأییدشده با dump کامل درخت Semantics قبل/بعد
کلیک). جایگزینی با `performSemanticsAction(SemanticsActions.OnClick)` (فراخوانی
مستقیم Action ثبت‌شده، بدون شبیه‌سازی لمس مبتنی بر مختصات — یک API رسمی Compose
Testing، نه Workaround) مشکل را قطعی حل کرد. برای تمام تعامل‌های
`StoryTabFlowTest.kt` با این دو نوع عنصر استفاده شد.

## تست

- `StoryRepositoryTest.kt` (۴ تست، Room واقعی In-Memory): Round-Trip کامل؛
  `loadStoryContext` روی پروژه‌ی بدون StoryContext ذخیره‌شده `null` برمی‌گرداند؛
  `genre` خالی/`moodSecondary` تهی به‌درستی Round-Trip می‌شوند؛ ذخیره‌ی دوباره
  (REPLACE) مقدار قبلی را جایگزین می‌کند.
- `StoryTabFlowTest.kt` (۲ تست End-to-End با `MainScaffold` کامل):
  - تغییر یک ژانر → بستن Studio (رفتن به Home) → ورود دوباره از کارت پروژه (یک
    `NavBackStackEntry`/`StoryViewModel` کاملاً تازه) → داده باقی می‌ماند
    (Regression مستقیم برای کمبود ذخیره‌سازی اصلی این قدم).
  - Rule 1 (Blocking، ژانر خالی) بدون تعامل نمایش داده می‌شود؛ Rule 3 (Warning،
    ترکیب Horror+Hopeful) بعد از تعامل واقعی نمایش داده می‌شود.
- `HomeProjectsStudioFlowTest.kt`/`AppNavigationTest.kt` (موجود): به‌روزرسانی
  شدند تا `storyRepository` تزریق‌شده را هم پاس بدهند (بدون نوشتن بی‌صدا به
  دیتابیس واقعی دستگاه حین تست) و از `waitUntilAtLeastOneExists` (نه
  `waitUntilExactlyOneExists`) بعد از ایجاد پروژه استفاده کنند — چون از این قدم
  به بعد، هم Header استودیو و هم فیلد عنوان Story Tab نام پروژه را هم‌زمان نشان
  می‌دهند.

## Consequences

- **آسان می‌شود:** قدم ۲ فاز ۲ (AI Story Breakdown + DNA Tab) می‌تواند مستقیماً
  از `StoryRepository`/الگوی تزریق‌پذیری تثبیت‌شده استفاده کند؛ Stepper های
  محلی این قدم آماده‌ی پاس‌شدن به آن صفحه‌اند.
- **کار آینده‌ی شناخته‌شده:** ویرایش واقعی داستان آزاد (AI Story Breakdown)؛
  اتصال Stepper ها به یک درخواست واقعی `StoryBreakdownRequest`؛ DNA Tab (هنوز
  Placeholder).
