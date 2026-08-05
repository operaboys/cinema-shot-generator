# ADR-050: واحد ۱۶ — فاز ۴، قدم ۱: لیست صحنه‌ها + Scene Detail

**تاریخ:** 2026-08-06
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL` (۶۰۶ تست، ۰ شکست، ۰ خطا).

## Context

اولین قدم فاز ۴ واحد ۱۶ (طبق پلن اجرایی، پیچیده‌ترین فاز). منابع حقیقت:
`docs/blueprints/16-user-workflow-v2.md` بخش «مرحله ۴: Scene Creation» و
`docs/design/README.md` بخش «۵. Scene Detail».

## تصمیم ۰: پاسخ به یادآوری معمار درباره‌ی مورد ۶ (اتصال Asset↔Scene)

طبق یادآوری صریح دستور کار، این قدم جایی است که اتصال Asset به Scene (از طریق
`Scene.locationAssetId`، ساخته‌شده در Migration پیش از واحد ۱۶ — ADR-038) باید
نهایی شود. این قدم دقیقاً همین کار را انجام داد: دکمه‌ی «اتصال به کتابخانه» در
Overview صفحه‌ی Scene Detail، `AssetRepository.loadAllLocationAssets` (فاز ۳ قدم
۱) را می‌خواند، کاربر یک `LocationAsset` انتخاب می‌کند، `locationAssetId` واقعاً
ذخیره می‌شود، و Overview نام همان Asset متصل را (نه فقط توصیف متنی آزاد) نمایش
می‌دهد — تست الزامی این قدم دقیقاً همین مسیر end-to-end را اثبات می‌کند.

## تصمیم ۱: کمبود Repository رفع شد — `loadAllScenes`

`SceneDao.getScenesForProject` از قبل موجود بود (واحد ۱۵) اما هیچ معادل سطح
دامنه‌ای نداشت. `SceneRepository.loadAllScenes(projectId): Flow<List<Scene>>`
اضافه شد — دقیقاً هم‌الگو با `AssetRepository.loadAllCharacterAssets`/...
(ADR-048): `Flow` (نه suspend `Result`) چون UI باید با ذخیره‌ی Scene جدید خودکار
به‌روز شود.

## تصمیم ۲ (تناقض واقعی معماری، حل‌شده): `Scene.state` وجود نداشت

با grep مستقیم `domain/scene/SceneModels.kt` تأیید شد `Scene` هیچ فیلد
`EntityState` نداشت — با اینکه `docs/blueprints/16-user-workflow-v2.md` (خط ۷۶)
صریحاً می‌گوید «هر Entity (Project/Scene/Shot/Asset) یک وضعیت `EntityState` دارد»
— دقیقاً همان جمله‌ای که در فاز ۱ برای افزودن `Project.state` استناد شد
(ADR-044). سند طراحی هم صریحاً می‌گوید چیپ وضعیت کارت Scene باید «matching
project states» باشد. این یک شکاف واقعی و مستقیماً مستند بود، نه فرض من:
`state: EntityState = EntityState.DRAFT` به `Scene` (و `SceneDto`/مپرها) اضافه
شد — دقیقاً هم‌الگو با `Project.state`؛ `domain/scene/SceneLifecycle.kt` (جدید،
تابع `lockScene`) هم‌الگو با `domain/project/ProjectLifecycle.kt` (`archiveProject`)
نوشته شد — قانون واقعی `canTransition` واحد ۱۲ را اجرا می‌کند، نه یک Bypass.

## تصمیم ۳: کلیدهای ترجمه‌ی موجود `project.state.*` برای Scene هم بازاستفاده شدند

با اینکه نامشان پیشوند «project» دارد، `EntityState` مفهومی سراسری و مشترک است؛
سند طراحی صریحاً می‌گوید چیپ وضعیت Scene باید با وضعیت Project یکسان باشد. ساختن
کلیدهای موازی «scene.state.*» با همان متن، تکرار بی‌دلیل رشته بود. رنگ‌های چیپ
(`entityStateColor` در `ScenesListScreen.kt`) هم عیناً از `ProjectCard.stateChipColor`
کپی شدند (آن تابع `private` است، بازاستفاده‌ی مستقیم ممکن نبود).

## تصمیم ۴ (یافته‌ی واقعی، اصلاح دستور کار): ۳ Tab، نه ۴ («Notes» وجود ندارد)

دستور کار از «Overview/Shots/Assets/Notes» پرسیده بود. با خواندن مستقیم
`docs/design/README.md` بخش «۵. Scene Detail» تأیید شد سند طراحی صریحاً می‌گوید
«3 tabs: Overview / Shots (badge count) / Assets (badge count)» — هیچ Tab
چهارمی («Notes») در سند طراحی یا بلوپرینت ۱۶ ذکر نشده. صفحه با ۳ Tab (Overview/
Shots/Assets) پیاده شد، طبق منبع حقیقت واقعی.

## تصمیم ۵: بلوک «اطلاعات صحنه» با فیلدهای واقعی Scene، نه فرض دستور کار

دستور کار «Location/Time/Weather/Mood» پیشنهاد داده بود. با grep تأیید شد
`Scene` هیچ فیلد Weather ندارد (Weather متعلق به `EnvironmentSettings` واحد ۰۸،
سطح Shot است، نه Scene) — این فیلد از Overview حذف شد (نه اختراع یک فیلد جدید
بدون پشتوانه‌ی دامنه). فیلدهای واقعی نمایش داده شدند: Location (نام
`LocationAsset` متصل یا توصیف آزاد `SceneLocation`)، Type (`SceneLocation.type`)،
Time of Day، Atmosphere Primary/Secondary («Mood» دستور کار)، Narrative Role،
Global Visual Style (`source`/`override` خام — `GlobalVisualStyleRef` عمداً یک
نمایندگی ساده است، نه اتصال واقعی به DNA، طبق کامنت خودِ این نوع).

## تصمیم ۶: «تنظیمات Scene» به‌صورت Dialog درون‌صفحه‌ای، نه مسیر Navigation جدا

بلوپرینت ۱۶ می‌گوید بخش «تنظیمات Scene» «با ضربه روی سربرگ Scene» در دسترس است،
بدون تصریح یک صفحه‌ی مستقل. برای این قدم اول فاز ۴، یک `AlertDialog` (هم‌الگو با
Modal تعمیر JSON در `AiStoryBreakdownScreen.kt`) شامل Title/NarrativeRole/
TimeOfDay/AtmospherePrimary/AtmosphereSecondary + دکمه‌ی صریح «ذخیره تنظیمات
صحنه» (طبق تصریح بلوپرینت، نه Auto-Save بی‌صدای Tab «DNA») پیاده شد — یک مسیر
Navigation جدا برای این قدم اول overkill بود؛ Location جداگانه ویرایش نمی‌شود
(همان دکمه‌ی «اتصال به کتابخانه»ی Overview) چون بلوپرینت صریحاً می‌گوید فیلد
Location این بخش «Dropdown از کتابخانه، نه متن آزاد» است.

## تصمیم ۷: بازاستفاده‌ی Cross-Feature از `AssetFormEnumDropdownField`/`AssetFormFlatEntries`

این دو تابع (`ui/assets/AssetFormSupport.kt`، فاز ۳ قدم ۲) کاملاً عمومی‌اند (نه
مختص Asset) و از قبل `internal` بودند — دقیقاً همان تصمیم بازاستفاده‌ی
`OpaqueChip` در همان قدم. به‌جای نوشتن نسخه‌ی سوم همین Dropdown، مستقیماً از
`ui.assets` وارد شدند. گزینه‌ی «بدون ترجیح» Atmosphere Secondary هم از کلید
موجود `dna.lightingPreference.none` بازاستفاده کرد (همان معنا).

## تصمیم ۸: `Duplicate` واقعاً پیاده شد (نه Placeholder)

برخلاف «Add Shot» (که واقعاً منتظر Shot Composer قدم بعدی است)، «تکثیر» یک
عملیات ساده و کم‌ریسک است که مستقیماً از الگوی موجود `ProjectRepository.duplicateProject`
پیروی می‌کند: کپی Scene با `sceneId` جدید، `sceneNumber` بعدی (از شمار فعلی
صحنه‌های پروژه)، `sceneTitle=null` (بازگشت به fallback «Scene N»)، و
`state=DRAFT`. پیاده‌سازی واقعی این عملیات هزینه‌ی کمی داشت و هم‌راستایی
معماری را با Project حفظ می‌کند.

## تصمیم ۹: پیش‌فرض‌های خنثی Scene تازه‌ساخته‌شده

طبق 🆕v4 بلوپرینت («یک Scene خالی با نام پیش‌فرض بلافاصله ساخته می‌شود، بدون
Modal»): `sceneTitle=null` (نمایش fallback «Scene N» با `sceneDisplayTitle`،
localized)، `NarrativeRole.DEVELOPMENT` (نه `INTRODUCTION` — فرض غلط «همیشه
صحنه‌ی اول» نمی‌کند)، `TimeOfDay.NOON`، `Atmosphere.CALM` (هم‌راستا با پیش‌فرض
خنثی مشابه `Mood.CALM` در `DnaViewModel`)، `LocationType.CUSTOM` با توصیف خالی.

## تصمیم ۱۰ (محدودیت شناخته‌شده‌ی پذیرفته‌شده): بازگشت از Scene Detail، Tab «داستان» را نشان می‌دهد

`SceneDetail→Studio(projectId)` یک نمونه‌ی تازه‌ی `StudioShell` می‌سازد؛
`selectedTab` آن `rememberSaveable` محلی است و به پیش‌فرض `STORY` بازمی‌گردد —
دقیقاً همان محدودیت پذیرفته‌شده‌ی مستندشده در ADR-049 برای فیلتر صفحه‌ی Assets
(نه یک محدودیت تازه). کاربر باید دستی روی Tab «صحنه‌ها» دوباره لمس کند.

## یافته‌ی تست: `performClick()` روی FloatingActionButton این صفحه غیرقابل‌اعتماد بود

هم‌خانواده با یافته‌ی مستندشده در ADR-045/047 برای FilterChip/DropdownMenuItem/
Card (نه یک یافته‌ی تازه، همان کلاس مشکل): `performClick()` (لمس مبتنی بر
مختصات) روی FAB «صحنه‌ی جدید» — که داخل یک `Box` با
`Modifier.align(Alignment.BottomEnd)` قرار دارد — هیچ کلیکی Trigger نمی‌کرد؛
تأییدشده با دیباگ مستقیم (گره پیدا می‌شود، `assertHasClickAction()` هم `true`
است، اما لامبدای `onClick` هرگز اجرا نمی‌شد). رفع با همان راه‌حل تثبیت‌شده‌ی
پروژه: `performSemanticsAction(SemanticsActions.OnClick)` به‌جای `performClick()`
مبتنی بر مختصات.

## Consequences

- `HomeProjectsStudioFlowTest.kt` (تست موجود از فاز ۱) به‌روزرسانی شد: چون Tab
  «صحنه‌ها» دیگر Placeholder نیست، بررسی «هنوز Placeholder» به Tab «خروجی»
  (تنها Tab باقی‌مانده‌ی واقعاً Placeholder) منتقل شد؛ `sceneRepository` هم به
  `MainScaffold` این تست اضافه شد.
- کار آینده‌ی شناخته‌شده: قدم ۲ فاز ۴ (Shot List + اسکلت Shot Composer)، ویرایش
  Asset موجود (نه فقط ساخت)، مدیریت کامل چند-Outfit، و Delete Scene (خارج از
  Scope این قدم به‌عنوان یک تصمیم — نه درخواست‌شده، نه تست‌شده).
