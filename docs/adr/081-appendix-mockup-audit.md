# پیوست ADR-081: بررسی سراسری mockup در برابر کد فعلی (۱۲ صفحه + عناصر مشترک)

این پیوست نتیجه‌ی کامل بخش دوم دستور کار قدم ADR-081 است: خواندن کامل
`docs/design/Cinema Studio.html` (تمام صفحات/وضعیت‌ها، نه فقط RESUME/ACCORDION)
و مقایسه‌ی هر بخش با کد فعلی. برای هر مورد: مرجع mockup (offset تقریبی بایت در
فایل خام ۲٬۱۱۳٬۴۸۵ بایتی + رشته‌ی نزدیک) + مرجع کد (فایل + خط/تابع) + دسته
(کامل/جزئی/پیاده‌نشده) ذکر شده است. هرجا ناهماهنگی در واقع یک تصمیم مستند و
عمدی در یک ADR قبلی بوده، ارجاع داده شده. طبق قاعده‌ی این قدم، «نبود عکس واقعی»
هرگز به‌عنوان یافته ذکر نشده (محدودیت پذیرفته‌شده و سراسری اپ).

**این فهرست عمداً خلاصه نشده** — طبق درخواست صریح معمار پروژه، چون خودش ارزش
مرجع دارد.

---

## ۱. Home (`is.home`, offset ~۱٬۹۷۱٬۲۲۹)

- **هدر (همبرگر، لوگو chip، سوییچ زبان، سوییچ تم)** — mockup offset ~۱٬۹۶۴٬۳۷۱ (بلوک هدر مشترک). کد: `ui/home/HomeScreen.kt:600-647` (`HomeHeader`). **کامل پیاده شده**، به‌جز اینکه لوگو یک آیکون `Icons.Filled.Movie` استاندارد است، نه SVG سفارشی «Aperture C» ماکاپ (`#csMark`). **جزئی** — انحراف مستند و عمدی، ADR-044 تصمیم ۱۱ ("لوگوی Aperture C... یک آیکون Material موقت").
- **عنوان/زیرعنوان خوش‌آمدگویی** — offset ~۱٬۹۷۱٬۴۷۰ (`x.k6`/`x.k7`). کد: `HomeScreen.kt:191-200`. **کامل پیاده شده.**
- **ردیف ایجاد سریع «پروژه‌ی تازه»** — offset ~۱٬۹۷۱٬۷۰۰ (`act.newProject`). کد: `HomeScreen.kt:650-678` (`QuickCreateRow`). **کامل پیاده شده.**
- **عنوان «پروژه‌های اخیر» + لینک «همه»** — offset ~۱٬۹۷۲٬۱۰۰. کد: `HomeScreen.kt:204-220`. **کامل پیاده شده.**
- **کارت‌های پروژه (نسخه‌ی HERO)** — offset ~۱٬۹۷۲٬۳۰۰ (کارت افقی فشرده‌ی ۹۶×۷۲). کد: `HomeScreen.kt` → `ProjectListSection`/`ProjectCard` (`ui/project/ProjectCard.kt:76-180`) با `showThumbnail=true`. **جزئی** — کد همان کارت بزرگ عمودی با کاور ۱۴۰dp صفحه‌ی Projects را برای Home هم رندر می‌کند (فقط یک شکل کارت در کل اپ وجود دارد، با پرچم روشن/خاموش تصویر، نه دو چیدمان مجزای per-screen مثل mockup). در هیچ ADR ای مستند نشده.
- **تصویر پس‌زمینه‌ی full-bleed پشت کل صفحه** — offset ~۱٬۹۷۱٬۲۴۰ (`heroSlotId`, hero scrim). کد: `HomeScreen.kt:144-174` (`HomeBackgroundImage`). **کامل پیاده شده** (decode واقعی `content://` بعداً اضافه شد، ADR-075/080/081) — وقتی تصویری انتخاب نشده به گرادیان جایگزین برمی‌گردد، طبق محدودیت پذیرفته‌شده‌ی محتوای تصویر.
- **نوار پایین سراسری اپ (Home/Projects/FAB/Studio/Assets)، همیشه نمایان** — offset ~۲٬۰۸۴٬۳۵۳ (`railDefs`/`rail`). کد: `ui/navigation/BottomNavBar.kt:58-115`. **کامل پیاده شده** ساختاری (DDR-002, ADR-044). به یادداشت مشترک زیر درباره‌ی FAB مرکزی مراجعه شود (همیشه no-op).
- **دو نسخه‌ی چیدمان A/B (HERO/RESUME)** — offset ~۱٬۹۶۷٬۲۱۰ (`is.homeB`). کد: `HomeScreen.kt:184-251` (`HomeLayoutVariant.HERO`/`RESUME`, `HomeResumeContent`). **کامل پیاده شده** — همین قدم (ADR-081) کامل شد، خارج از scope بررسی مجدد، حضور و اتصال تأیید شد.

## ۲. Projects (`is.projects`, offset ~۱٬۹۷۵٬۴۹۹)

- **عنوان هدر + زیرعنوان پویا («N پروژه‌ی محلی»)** — offset ~۱٬۹۷۵٬۰۰۰ (از طریق هدر مشترک `nav.title`/`nav.subtitle`). کد: `ui/project/ProjectsScreen.kt:69-88`. **کامل پیاده شده.**
- **ردیف چیپ فیلتر وضعیت** (`stateFilters`, چیپ نقطه+برچسب، اسکرول‌پذیر) — offset ~۱٬۹۷۵٬۶۲۰. کد: **کاملاً غایب** — `ProjectsScreen.kt` به‌طور کامل grep شد؛ هیچ ردیف فیلتری وجود ندارد، فهرست همیشه همه‌ی پروژه‌ها را بدون فیلتر نشان می‌دهد. **پیاده‌نشده به‌طور کامل.** هیچ ADR ای این حذف را ذکر نکرده (`grep -rn "stateFilters"` در `docs/adr/` چیزی برنمی‌گرداند).
- **کارت‌های پروژه (کاور عمودی بزرگ ۱۴۰dp + عنوان/وضعیت/متا)** — offset ~۱٬۹۷۵٬۷۰۰. کد: `ProjectCard.kt:76-180` با `showThumbnail=false` (استفاده‌شده در `ProjectsScreen.kt:98-105`). **جزئی** — کارت Projects در mockup همیشه ناحیه‌ی کاور بزرگ دارد؛ نسخه‌ی کد (پرچم thumbnail خاموش) **هیچ ناحیه‌ی تصویری** ندارد، یعنی نه Home نه Projects رفتار per-screen خود mockup را ندارند (رجوع به یافته‌ی بالا — شکل کارت دو صفحه عملاً جابه‌جا/ادغام شده در یک کامپوننت مشترک).
- **دکمه‌ی Import پروژه** (بخشی از این offset دقیق mockup نیست اما بخشی از تقارن Export/Import خود اپ است) — کد: `ProjectsScreen.kt:82-87`. **اضافه نسبت به mockup** (mockup هیچ امکان Import صریحی در این صفحه ندارد) — مستند، ADR-064/065. یک کاستی نیست.
- **منوی Overflow (تغییرنام/کپی/آرشیو/Export/حذف)** — offset ~۱٬۹۷۵٬۱۸۰ (`more_vert`). کد: `ProjectCard.kt:114-141`. **کامل پیاده شده**، با تنش مستند ماشین‌حالت Archive/DRAFT طبق ADR-044 تصمیم ۱ (عمدی، نه باگ).

## ۳. Studio Shell (`is.studio`, offset ~۱٬۹۷۷٬۷۶۴)

- **هدر (بازگشت، عنوان، زیرعنوان صحنه/شات، چیپ «ذخیره شد»)** — offset ~۱٬۹۷۷٬۶۰۰ (`nav.saved` مشترک). کد: `ui/studio/StudioShell.kt:216-249` (`StudioHeader`). **کامل پیاده شده**، کنتراست رفع‌شده طبق ADR-059 (پرکردن توپر `success` به‌جای سبز ۱۲٪-آلفای mockup).
- **۴ Tab بالایی (داستان/DNA/صحنه‌ها/خروجی)** — offset ~۱٬۹۷۷٬۷۸۰ (`studioTabs`). کد: `ui/navigation/StudioTopTabRow.kt:32,88,141-144`؛ اتصال در `StudioShell.kt:179-186`. **کامل پیاده شده.**
- **Tab داستان — فیلد عنوان، متن داستان، Stepper‌های shots هدف/ثانیه‌به‌ازای‌شات، ردیف‌های فیلد** — offset ~۱٬۹۷۷٬۹۵۰. کد: `ui/story/StoryTabContent.kt:110-213`. **جزئی** — کد فهرست ساده‌ی ردیف‌فیلد mockup را با فیلدهای واقعی `StoryContext` جایگزین/گسترش می‌دهد (نوع داستان، چیپ ژانر، احساس اصلی/فرعی، شدت روایی، قصد بصری — مستند، ADR-045)، که یک جایگزینی مثبت است، اما دو عنصر mockup کاملاً غایب‌اند:
  - **نوار پیشرفت ۳ بخشی کارت ورودی AI-Breakdown** (offset ~۱٬۹۷۸٬۶۰۰، نوارهای بنفش/مویی/مویی) — کد (`StoryTabContent.kt:186-193`) یک `Button` ساده رندر می‌کند، بدون هیچ نمایش پیشرفتی. **پیاده‌نشده.**
  - **کارت خلاصه‌ی «Human Overrides»** (فهرست فیلد+دلیل، آیکون نارنجی ویرایش) — offset ~۱٬۹۷۸٬۹۰۰ (فهرست `overrides`). کد: **غایب** از `StoryTabContent.kt`. ویژگی زیرین Human-Override واقعاً وجود دارد، اما فقط در `ValidationScreen.kt` نمایان می‌شود (رجوع به §۹)، هرگز روی Tab داستان خلاصه نمی‌شود. **پیاده‌نشده** برای این محل خاص؛ هیچ ADR این حذف را ذکر نکرده.
- **Tab DNA — بنر Soft-Lock + ۶ گروه فیلد (هویت اصلی، پالت اصلی+نوار ۵ نمونه‌رنگ، خلق‌وخوی پایه، ترجیح نور، محدودیت‌های خروجی، دستورالعمل‌های کیفیت)** — offset ~۱٬۹۷۹٬۳۰۰ به بعد. کد: `ui/dna/DnaTabContent.kt:155 (SoftLockBanner)، 157 (coreIdentity)، 187 (masterPalette + نمونه‌رنگ‌ها ~189-203)، 233 (globalMoodBase)، 263 (lightingPreference)، 286 (outputConstraints)، 305 (qualityDirectives)`. **کامل پیاده شده**، شامل رفع کنتراست بنر (`DnaTabContent.kt:330-360`، نارنجی توپر، ADR-059 — عمداً از نسخه‌ی کم‌آلفای ۲۲٪ mockup بهتر است تا الزام خود سند طراحی «باید واضحاً فوری خوانده شود» را برآورده کند).
- **Tab صحنه‌ها — کارت‌های صحنه (تصویر بندانگشتی، عنوان، نقش/زمان/هوا، تعداد شات + چیپ وضعیت)** — offset ~۱٬۹۸۰٬۹۰۰. کد: `ui/scenes/ScenesListScreen.kt` (اتصال از طریق `StudioShell.kt:204-210`). **کامل پیاده شده** (طبق ADR-050 تصمیم ۳، رنگ‌های چیپ وضعیت از `ProjectCard` کپی شده).
- **Tab خروجی — کارت خلاصه‌ی اعتبارسنجی (آیکون خطا + متن blocking/warning)، کارت وضعیت تولید prompt (check_circle)، دکمه‌ی CTA تمام‌عرض «Export»** — offset ~۱٬۹۸۱٬۳۰۰-۱٬۹۸۱٬۹۰۰ (`st.output`, `act.goValidation`, `act.goOutput`). کد: `StudioShell.kt:187-212` — `StudioTab.OUTPUT` به شاخه‌ی عمومی `else -> StudioTabPlaceholder` می‌افتد (`StudioShell.kt:211,251-260`)، فقط متن «این بخش در فاز بعدی تکمیل می‌شود» را نشان می‌دهد. **پیاده‌نشده به‌طور کامل.** این به‌صراحت در ADR-050 به‌عنوان کاستی هنوز باز تأیید شده («Tab «خروجی» (تنها Tab باقی‌مانده‌ی واقعاً Placeholder)»)، و هیچ ADR بعدی (۰۵۷ Output Delivery، ۰۵۹ فاز نهایی Backups/review، یا هر ADR بررسی پس از Unit-16) دوباره به آن نپرداخته — تا پایان build مستندشده به‌صورت placeholder باقی مانده است.

## ۴. AI Story Breakdown (`is.breakdown`, offset ~۱٬۹۹۰٬۶۶۴)

- **هدر (بازگشت، عنوان، زیرعنوان «قدم ۱ب — سه فاز»)** — offset ~۱٬۹۹۰٬۷۰۰. کد: `ui/storybreakdown/AiStoryBreakdownScreen.kt:177-197`. **کامل پیاده شده.**
- **نوار Stepper سه‌فازی (دایره‌های شماره‌دار + خط اتصال باریک، فعال=بنفش توپر / غیرفعال=کاشی خنثای حاشیه‌دار)** — offset ~۱٬۹۹۰٬۷۵۰. کد: `AiStoryBreakdownScreen.kt:204-264` (`PhaseStepperRow`/`PhaseCircle`). **کامل پیاده شده**، و به‌صراحت طبق یادداشت پیاده‌سازی #۲ عمل می‌کند (elevation روی لایه‌ی محیطی بلور در Compose بی‌معنا است؛ کامنت کد مستقیماً به آن ارجاع می‌دهد).
- **فاز ۱ — کارت داستان، Stepper‌های shots هدف/ثانیه‌به‌ازای‌شات، CTA «تولید Prompt»، پیش‌نمایش prompt تولیدشده + کپی** — offset ~۱٬۹۹۱٬۰۰۰. کد: `AiStoryBreakdownScreen.kt:266-362`. **کامل پیاده شده.**
- **فاز ۲ — Textarea پیست، عمل «تکه‌ی تازه (ChunkCombiner)»، شمارش تکه، ردیف خطای JSON → مودال تعمیر** — offset ~۱٬۹۹۲٬۹۰۰. کد: `AiStoryBreakdownScreen.kt:364-423`. **کامل پیاده شده.**
- **مودال تعمیر JSON (توصیف خطا + «ویرایش دستی» / «تعمیر خودکار»)** — offset ~۱٬۹۹۳٬۶۰۰ (`jsonModal`). کد: `AiStoryBreakdownScreen.kt:425-449` (`JsonRepairDialog`). **کامل پیاده شده** — دکمه‌ی auto-fix به‌شرط `diagnosis.autoFixable==false` مخفی می‌شود، اصلاحی معقول و دامنه‌محور نسبت به دکمه‌ی همیشه‌نمایان mockup.
- **فاز ۳ — خلاصه‌ی شمارش Assets/Scenes/Shots + هشدارها + CTA «تأیید و ادامه»** — offset ~۱٬۹۹۴٬۲۰۰. کد: `AiStoryBreakdownScreen.kt:451-495`. **کامل پیاده شده.**

## ۵. Scene Detail (`is.sceneDetail`, offset ~۱٬۹۹۹٬۷۹۲)

- **تصویر Hero با چیپ قفل روی آن (بالا-راست)** — offset ~۱٬۹۹۹٬۸۵۰ (`cs-scene-hero`, چیپ قفل `y.m7`). کد: `ui/scenes/SceneDetailScreen.kt` — **هیچ کانتینر تصویر hero رندر نمی‌شود** (قابل‌قبول، طبق قاعده‌ی جای‌گیر تصویر)؛ نشانگر وضعیتی که باید روی چیپ قفل بنشیند در عوض به‌صورت درون‌خطی بالای Tab Overview رندر می‌شود (`EntityStateChip`, `SceneDetailScreen.kt:324`). **جزئی** — عملکرد نمایش وضعیت حفظ شده اما از overlay روی تصویر به یک چیپ ساده در بالای فهرست منتقل شده (جایگزینی معقول با توجه به نبود تصویر hero، به‌عنوان محتوای تصویر پرچم‌گذاری نشده).
- **۳ Tab (Overview / Shots(badge) / Assets(badge))** — offset ~۲٬۰۰۰٬۰۰۰ (`sceneTabs`, `t.badge`). کد: `SceneDetailScreen.kt:156-174` (`SecondaryTabRow`). **جزئی** — ۳ Tab وجود دارد و به‌درستی محدود شده (ADR-050 تصمیم ۴ به‌صراحت تأیید می‌کند «۳ Tab، نه ۴ — بدون Tab یادداشت‌ها» مطابق سند طراحی)، اما **شمارنده‌ی عددی badge کنار برچسب‌های Shots/Assets رندر نمی‌شود** — `Tab(text = { Text(uiString(...)) })` هیچ پسوند شمارشی جایی ندارد. ADR-050 عبارت «(شمارش badge)» mockup را نقل می‌کند اما هرگز پیاده یا بازبینی نمی‌کند.
- **ردیف‌های اطلاعات Overview (مکان، نوع، زمان روز، هوا، فضا، نقش روایی، سبک بصری سراسری)** — offset ~۲٬۰۰۰٬۳۰۰. کد: `SceneDetailScreen.kt:307-357` (`OverviewTab`/`InfoRow`). **جزئی — انحراف مستند**: `هوا` (Weather) حذف شده چون در سطح Shot در `EnvironmentSettings` قرار دارد نه Scene (ADR-050 تصمیم ۵، صریح و مستدل).
- **ردیف Quick Actions (ویرایش صحنه، افزودن شات، کپی، قفل صحنه)** — offset ~۲٬۰۰۰٬۷۰۰ (`quickActions`). کد: `SceneDetailScreen.kt:367-398` (`QuickActionsRow`/`QuickActionTile`). **کامل پیاده شده.**
- **Tab Shots (فهرست هر شات با کد/نوع-هدف/مدت-حرکت/chevron)** — offset ~۲٬۰۰۱٬۳۰۰. کد: مسیریابی به `ui/shots/ShotListScreen.kt` (رجوع به §۶). **کامل پیاده شده** به‌عنوان مقصد مسیریابی (یافته‌های خود زیرصفحه در ادامه).
- **Tab Assets — کارت‌های asset پیوندشده (آیکون، نام، متا، `continuityLockLevel`) + ردیف «پیوند Asset» (`act.openSheet`)** — offset ~۲٬۰۰۱٬۹۰۰. کد: `SceneDetailScreen.kt:207` → `SceneDetailTab.ASSETS -> EmptyTabState(...)`. **پیاده‌نشده به‌طور کامل** — این Tab بدون‌شرط یک رشته‌ی empty-state ثابت نشان می‌دهد صرف‌نظر از اینکه واقعاً assetی به صحنه پیوند خورده باشد؛ هیچ فهرست asset پیوندشده، هیچ امکان «افزودن»/باز کردن sheet وجود ندارد. هیچ ADR این Tab را بحث یا بازبینی نکرده (`grep` در `docs/adr/*.md` برای `sceneDetail.assetsEmptyState`/`SceneDetailTab.ASSETS` چیزی برنمی‌گرداند).
- **مودال تنظیمات (عنوان، نقش روایی، زمان روز، فضای اصلی/فرعی، سبک بصری سراسری، تغییر مکان)** — عنصر مجزای mockup نیست بلکه تجمیع کدی فیلدهای قابل‌ویرایش Overview است. کد: `SceneDetailScreen.kt:450-564`. **کامل پیاده شده** (فراتر از mockup، با قابل‌ویرایش‌کردن واقعی سبک بصری سراسری، ADR-076/G8 fix).

## ۶. Shots List (`is.shots`, offset ~۲٬۰۰۷٬۵۵۲)

- **سوییچ دوحالته‌ی Grid/Timeline** — offset ~۲٬۰۰۷٬۶۰۰ (`viewModes`). کد: `ui/shots/ShotListScreen.kt:88-102` (`OpaqueChip` × ۲). **کامل پیاده شده**، با الگوی چیپ توپر کنتراست-امن خود اپ.
- **حالت Grid — کارت‌های ۲ستونه، تصویر ۱۰۴dp بالا، فقط کد/نوع/مدت** — offset ~۲٬۰۰۷٬۷۵۰. کد: `ShotListScreen.kt:112-129` همان `ShotCard` (`ShotListScreen.kt:206-249`) را داخل `LazyVerticalGrid` رندر می‌کند. **جزئی** — هیچ کارت فشرده‌ی مجزای «تصویر-اول، فقط نوع/مدت» وجود ندارد؛ Grid همان کارت عریض حالت فهرست/Timeline را استفاده می‌کند.
- **حالت Timeline — ریل نقطه+خط‌اتصال، ردیف سرصفحه‌ی کد/مدت، خط متای نوع·هدف·حرکت** — offset ~۲٬۰۰۷٬۹۵۰-۲٬۰۰۸٬۹۰۰. کد: `ShotListScreen.kt:130-146` همان `ShotCard` را داخل `LazyColumn` استفاده می‌کند، **بدون هیچ ریل نقطه/خط‌اتصالی** و بدون ترکیب متای مجزا (هدف/حرکت اصلاً در `ShotCard` مشترک نشان داده نمی‌شود، فقط نوع شات + مدت، `ShotListScreen.kt:238-246`). **جزئی** — Grid و Timeline از نظر عملکردی قابل‌سوییچ‌اند اما از نظر بصری یکسان‌اند، برخلاف دو طراحی کارت مجزای mockup؛ در ADR-051 بحث نشده.
- **FAB «شات تازه»** — offset صریحی در این بخش نیست (طبق README ضمنی است) — کد: `ShotListScreen.kt:149-157`. **کامل پیاده شده** (به‌همراه دیالوگ حذف-با-تأیید و آیکون نشانگر override (`Icons.Filled.Tune`) که افزوده‌ی خاص اپ فراتر از mockup است، `ShotListScreen.kt:212-219`).

## ۷. Shot Composer — نسخه‌ی TABS (`is.composer`, offset ~۲٬۰۱۵٬۲۳۳)

- **نوار پیش‌نمایش Hero (۱۶۰dp)** — offset ~۲٬۰۱۵٬۳۵۰ (`cs-shot-hero`). کد: `ui/shots/ShotComposerScreen.kt:152-158` — یک `Box` تخت با رنگ inset، بدون هیچ جای‌گیر تصویری (قابل‌قبول طبق قاعده‌ی جای‌گیر تصویر، اما حتی به‌سبک گرادیان جایگزین سایر نواحی hero اپ هم استایل نشده — فقط پرکردن یکدست `inset`). **جزئی** (رفتار بصری با الگوی گرادیان جایگزین سراسری اپ، مثلاً `ProjectCard.kt:95-100`، متفاوت است).
- **۴ Tab (اصلی/دوربین/نور/صدا)** — offset ~۲٬۰۱۵٬۳۰۰ (`composerTabs`). کد: `ShotComposerScreen.kt:178-202`. **کامل پیاده شده.**
- **Tab اصلی — فیلدهای سطح‌بالا (عنوان/توصیف/هدف/نوع/مدت/حرکت)** — offset ~۲٬۰۱۵٬۵۰۰ (`composerFields`). کد: `ShotComposerScreen.kt:253-311` (`MainFieldsSection`). **کامل پیاده شده.**
- **Tab اصلی — ردیف چیپ «مراجع پیوست‌شده» (شخصیت/سبک/ترکیب‌بندی) + دکمه‌ی نقطه‌چین «افزودن» → bottom sheet** — offset ~۲٬۰۱۵٬۹۰۰-۲٬۰۱۶٬۳۰۰ (`cmp.main`, `references`, `act.openSheet`). کد: پیاده شده، اما **به Tab دوربین منتقل شده، نه اصلی** — `ui/shots/CameraTabContent.kt:434-500` (`AttachedReferencesSection`)، و جریان افزودن یک dropdown+dialog درون‌خطی است، نه bottom sheet (رجوع به یافته‌ی مشترک «sheet» زیر). **جزئی — انحراف مستند** (ADR-052 تصمیم ۵: فقط نوع + توصیف متن‌آزاد، بدون انتخابگر فایل واقعی، `ImageReference.localFilePath=""`).
- **بخش قابل‌گسترش «پیشرفته» به‌ازای هر Tab (دوربین، نور)** — offset ~۲٬۰۱۶٬۴۰۰. کد: `CameraTabContent.kt:376-433` (`AdvancedSection`) و معادل آن در `ui/shots/LightingEnvironmentTabContent.kt`. **کامل پیاده شده.**
- **Tab صدا (دیالوگ فقط‌خواندنی + کپی، درخواست «افزودن صدای محیط»، یادداشت مرتبط با هوا با نشان کهربایی)** — offset ~۲٬۰۱۶٬۹۰۰-۲٬۰۱۷٬۷۰۰ (`cmp.audio`). کد: `ui/shots/AudioTabContent.kt`. **کامل پیاده شده**، شامل رفتار Rule-۵ «فقط دستی / هرگز تولید خودکار» که در ADR-053 تصمیم ۴ تأیید شده.
- **Tab دوربین — یادداشت زمان‌بندی «خودکار از روی هوا» (ردیف نقطه‌چین)** — offset ~۲٬۰۱۷٬۷۵۰ (`cmp.camera`, `x.k41`). این در واقع قاعده‌ی صدای محیطی را مستند می‌کند؛ **کامل پیاده شده** طبق ADR-053.
- **پنل پایین دائمی — نوار blocking/warning اعتبارسنجی (کلیک → Validation)، قطعه پیش‌نمایش خروجی درون‌خطی، چیپ‌های دسترسی سریع مدل با کپی هر چیپ** — offset ~۲٬۰۱۷٬۹۰۰-۲٬۰۱۹٬۰۰۰ (`act.goValidation`, `x.k44/x.k45`, `modelChips`). کد: `ShotComposerScreen.kt:129-150` — فقط دو `TextButton` ساده («اعتبارسنجی»، «تحویل خروجی») نزدیک بالای صفحه، بدون شمارش زنده‌ی blocking/warning، بدون متن پیش‌نمایش درون‌خطی، بدون هیچ ردیف چیپ کپی سریع مدل در Composer. **پیاده‌نشده** (مسیریابی وجود دارد؛ سطح خلاصه/پیش‌نمایش غنی وجود ندارد). هیچ ADR این پنل را بحث نکرده.
- **نسخه‌ی A/B ACCORDION** — offset ~۲٬۰۱۱٬۲۷۷ (`is.composerB`). کد: `ShotComposerScreen.kt:230-444` (`ComposerAccordion`). **کامل پیاده شده** — همین قدم (ADR-081) کامل شد، خارج از scope بررسی مجدد، حضور تأیید شد.

## ۸. Assets Library (`is.assets`, offset ~۲٬۰۲۶٬۶۱۱)

- **فیلتر بالای ۳حالته (شخصیت‌ها/مکان‌ها/اشیاء)، توپر+opaque+حاشیه‌دار** — offset ~۲٬۰۲۶٬۷۰۰ (`assetFilters`). کد: `ui/assets/AssetsScreen.kt:251-276,284-301` (`KindFilterRow`/`OpaqueSegmentedButton`). **کامل پیاده شده**، به‌صراحت طبق مشخصات رفع کنتراست یادداشت پیاده‌سازی #۱ ساخته شده (مستند در کامنت سرفایل، ADR-048).
- **ردیف زیرفیلتر (خاص هر Tier)** — offset ~۲٬۰۲۶٬۹۵۰ (`assetSubFilters`). کد: `AssetsScreen.kt:418-583` (`CharacterAssetList`/`LocationAssetList`/`ObjectAssetList`, با `OpaqueChip`). **کامل پیاده شده.**
- **کارت‌های Asset (تصویر بندانگشتی، نام، نشان Tier نارنجی، توصیف، خط متای `continuityLockLevel`)** — offset ~۲٬۰۲۷٬۱۵۰. کد: `AssetsScreen.kt:330-404` (`AssetCard`/`TierBadge`/`ThumbnailPlaceholder`). **کامل پیاده شده.**
- **FAB «افزودن asset»** — کد: `AssetsScreen.kt:203-211`. **کامل پیاده شده**، مسیریابی به فرم‌های واقعی خاص هر نوع طبق ADR-049 (نه یک snackbar «به‌زودی» مثل فاز قبلی).

## ۹. Validation (`is.validation`, offset ~۲٬۰۲۹٬۵۶۸)

- **هدر + زیرعنوان «اعتبارسنجی ۳سطحی»** — offset ~۲٬۰۲۹٬۶۰۰. کد: `ui/validation/ValidationScreen.kt:121-127`. **کامل پیاده شده.**
- **۲ کارت شمارشی توپر/حاشیه‌دار (N BLOCKING قرمز، N WARNING کهربایی)** — offset ~۲٬۰۲۹٬۷۰۰. کد: `ValidationScreen.kt:217-264` (`SummaryCountRow`/`CountCard`). **کامل پیاده شده**، به‌صراحت طبق رفع کنتراست یادداشت پیاده‌سازی #۱ بازسازی شده (ADR-055).
- **۳ بخش سطح اعتبارسنجی، هرکدام با آیکون+برچسب شدت، فیلد، پیام، پیشنهاد** — offset ~۲٬۰۲۹٬۹۰۰. کد: `ValidationScreen.kt:266-371` (`ValidationLevelSection`/`IssueCard`). **کامل پیاده شده.**
- **جریان Human-Override / بازگشت (Revoke)** — اصلاً در mockup وجود ندارد. کد: `ValidationScreen.kt:100-206,373-466`. **اضافه نسبت به mockup** — یک ویژگی واقعی و دامنه‌محور (ADR-067/068) که در سند طراحی غایب است؛ کاستی نیست، به‌عنوان افزوده‌ی مثبت فراتر از سند طراحی ارزش ذکر دارد.

## ۱۰. Output Delivery (`is.output`, offset ~۲٬۰۳۲٬۲۳۰)

- **انتخابگر مدل — شبکه چیپ wrapping، هر ۱۳ مدل + هزینه‌ی token روی هر چیپ، توپر/opaque غیرانتخاب، بنفش توپر انتخاب‌شده** — offset ~۲٬۰۳۲٬۳۰۰ (`models`). کد: `ui/outputdelivery/OutputDeliveryScreen.kt:176-196` (`ModelPickerSection`, بر پایه‌ی `ALL_MODEL_PROFILES`). **کامل پیاده شده**، کنتراست‌رفع‌شده با بازاستفاده از `OpaqueChip` (تصمیم مستند ADR-057 در سرفایل، بدون ساخت چیپ گرادیان سفارشی — انتخاب عمدی برای یکپارچگی، نه نقص).
- **کارت پیش‌نمایش خروجی — «پیش‌نمایش خروجی — {مدل}» + نشان «تمیزشده·نهایی‌شده»، بلوک prompt فقط‌خواندنی، خط شمارش token + هشدار تجاوز از حد، ردیف کپی/بازتولید، CTA تمام‌عرض Export** — offset ~۲٬۰۳۲٬۵۰۰-۲٬۰۳۳٬۲۰۰. کد: `OutputDeliveryScreen.kt:198-268` (`OutputPreviewCard`). **کامل پیاده شده**، و نشان + خط شمارش token مشخصاً فراتر از نسخه‌ی ثابت/hardcoded mockup اصلاح شده تا نتیجه‌ی واقعی pipeline `cleaningSucceeded`/`tokenCheck` را منعکس کند (G17 fix، `OutputDeliveryScreen.kt:220-250`).
- **رفتار دکمه‌ی Export** — handler `act.copy` در mockup به‌طور تحت‌اللفظی Export را به Copy alias می‌کند (offset ~۲٬۰۳۳٬۱۵۰). کد: **بهبودیافته فراتر از mockup** — Export/Share واقعی فایل با `Intent.ACTION_SEND` (`OutputDeliveryScreen.kt:86-100`)، مستند به‌عنوان رفع عمدی یک نقص معماری mockup (ADR-069). کاستی نیست.
- **بخش هشدارها** — کد: `OutputDeliveryScreen.kt:270-289`. **کامل پیاده شده.**

## ۱۱. Settings (`is.settings`, offset ~۲٬۰۳۶٬۰۰۸)

- **کارت زبان و تم (جفت چیپ سبک radio)** — offset ~۲٬۰۳۶٬۱۰۰ (`langOpts`/`themeOpts`). کد: `ui/settings/SettingsScreen.kt:280-296` (`LanguageThemeCard`). **کامل پیاده شده.**
- **کارت تصویر صفحه‌ی خانه — کاشی پیش‌نمایش ۱۴۰dp + دکمه‌های انتخاب/حذف** — offset ~۲٬۰۳۶٬۹۰۰ (کاشی ۱۴۰px `heroSlotId`، `act.pickHero`/`act.clearHero`). کد: `SettingsScreen.kt:306-334` (`HomeImageCard`). **جزئی** — عمل‌های انتخاب/حذف کاملاً واقعی‌اند (`SettingsScreen.kt:97-105`، `OpenDocument()` + `takePersistableUriPermission`، ADR-075)، اما خود «کاشی پیش‌نمایش ۱۴۰px» رندر نمی‌شود — کارت رشته‌ی خام content-URI را به‌صورت متن نشان می‌دهد (`SettingsScreen.kt:318-323`) به‌جای کاشی پیش‌نمایش بصری.
- **کارت نسخه‌های چیدمان (Home A/B، Composer A/B)** — offset ~۲٬۰۳۷٬۳۰۰ (`variants`). کد: `SettingsScreen.kt:336-377` (`LayoutVariantsCard`). **کامل پیاده شده** — همین قدم (ADR-081) کامل شد.
- **کارت درباره/لوگو** — offset ~۲٬۰۳۷٬۸۵۰ (لوگو ۴۴px `#csMark` + نام اپ/شعار/نسخه). کد: `SettingsScreen.kt:379-385` (`AboutCard`) — **بدون نشان لوگو، بدون رشته‌ی نسخه**، فقط متن عنوان + شعار. **جزئی** (همان کاستی لوگوی Aperture-C مثل Home، ADR-044 تصمیم ۱۱، به‌علاوه‌ی خط «v1.0.0 · On-Device» mockup که کاملاً غایب است).
- **کارت نمایش (فونت پویا، حداقل هدف لمسی، حرکت کاهش‌یافته)** — offset ~۲٬۰۳۸٬۱۰۰ (`settingsSections`). کد: `SettingsScreen.kt:200-215` (`DisplayCard`). **جزئی — انحراف مستند**: سوییچ‌ها در DataStore ذخیره می‌شوند اما هیچ اثر واقعی در باقی اپ ندارند به‌جز حداقل هدف لمسی (که واقعاً از طریق `minTouchTargetIfEnabled()` وصل شده، مثلاً `AiStoryBreakdownScreen.kt:245`)؛ فونت پویا و حرکت کاهش‌یافته به‌صراحت بی‌اثر اعلام شده‌اند (کامنت کد، `SettingsScreen.kt:193-199`، ADR-058).
- **کارت گردش‌کار (نمای پیش‌فرض فهرست شات، بازه‌ی Auto-Save، پرش بین قدم‌ها)** — کد: `SettingsScreen.kt:217-258`. **کامل پیاده شده.**
- **کارت حریم‌خصوصی (رمزنگاری ذخیره‌سازی، Cloud Sync خاموش، بدون آنالیتیکس)** — کد: `SettingsScreen.kt:260-276`. **کامل پیاده شده** (صرفاً اطلاع‌رسانی، مطابق قصد mockup/README).

## ۱۲. Backups (`is.backups`, offset ~۲٬۰۴۴٬۵۵۹)

- **بنر اطلاعاتی حریم‌خصوصی «بدون Cloud Sync»** (سبز، آیکون `cloud_off`) — offset ~۲٬۰۴۴٬۶۵۰ (`x.k49`). کد: **کاملاً غایب** از `BackupsScreen.kt` (`grep` برای "cloud" در `ui/backups/*.kt` چیزی برنمی‌گرداند). **پیاده‌نشده به‌طور کامل**، در ADR-059 بحث نشده.
- **فهرست پشتیبان (نام فایل، حجم، نوع، سن) + عمل بازگردانی** — offset ~۲٬۰۴۴٬۷۵۰. کد: `ui/backups/BackupsScreen.kt:187-223` (`BackupRow`, `BackupKindBadge`). **کامل پیاده شده**، به‌علاوه‌ی عمل حذف و دیالوگ‌های تأیید که mockup نشان نمی‌دهد (G19/G20 fixes، ADR-059/060 — افزوده‌ی مثبت).
- **ردیف عمل پایینی «Export All» (دانلود) / «Import Backup» (آپلود)** — offset ~۲٬۰۴۵٬۲۰۰-۲٬۰۴۵٬۶۰۰ (`y.m26`/`y.m27`). کد: **غایب** — `BackupsScreen.kt` فقط دکمه‌ی «ایجاد پشتیبان دستی» دارد (`BackupsScreen.kt:106-115`)، هیچ عمل export-all/import-backup گروهی در این صفحه نیست. **پیاده‌نشده به‌طور کامل**، در ADR-059 بحث نشده (export/import سطح-پروژه جای دیگری وجود دارد، `ProjectCard.kt`/`ProjectsScreen.kt`، اما آن یک ویژگی متفاوت در سطحی متفاوت است).

---

## عناصر مشترک (Cross-cutting)

- **محتوا/گروه‌بندی Nav Drawer** — mockup offset ~۲٬۰۵۲٬۳۱۹ (`drawerGroups`: STUDIO — Story Wizard/AI Breakdown/DNA Manager/Scenes/Shots/Assets؛ TOOLS — Validation/Prompt Generator/Output Delivery؛ SYSTEM — Settings/Backups). کد: `ui/navigation/NavDrawer.kt:40-66` (`studioLinks`/`toolsLinks`/`systemLinks`). **کامل پیاده شده** ساختاری (چیدمان یکسان ۳گروهی، ۱۱موردی)، اما **جزئی** عملکردی: بیشتر لینک‌ها (Shots، Validation، Output Delivery) به مقصد واقعی خود deep-link نمی‌کنند — چون Drawer هیچ context صحنه/شات فعالی برای مسیریابی ندارد، به Tab «صحنه‌ها»ی Studio برمی‌گردند (مستند، تصمیم ADR-061، صریح و مستدل). «Prompt Generator» عمداً snackbar «به‌زودی» باقی می‌ماند چون در Output Delivery ادغام شده (ADR-056)، در `NavDrawer.kt:30-32` مستند شده.
- **Bottom-sheet («پیوند Asset»)** — mockup offset ~۲٬۰۴۷٬۷۹۴ (حالت `sheet`، `act.openSheet`/`act.pickAsset`/`act.closeSheet`). کد: **هیچ `ModalBottomSheet` (یا معادل) در کل کدبیس وجود ندارد** — تنها نتیجه‌ی grep برای "Sheet" یک کامنت کد پراکنده در `ui/theme/Theme.kt:23` است ("Card/BottomSheet/Header با blur/gradient کار فازهای بعدی است")، نه یک پیاده‌سازی. **پیاده‌نشده به‌طور کامل.** هر جایی که mockup این sheet را باز می‌کرد (Tab Assets صحنه، «افزودن» مراجع پیوست‌شده‌ی Composer) به‌جایش با یک جریان dropdown/dialog درون‌خطی جایگزین شده (ADR-052 تصمیم ۵) یا، در مورد Scene Detail، اصلاً چیزی وجود ندارد (§۵ بالا).
- **مودال تعمیر JSON (AI Story Breakdown)** — mockup offset ~۲٬۰۵۰٬۲۸۰ (`jsonModal`). کد: `AiStoryBreakdownScreen.kt:425-449`. **کامل پیاده شده** (رجوع به §۴).
- **Toast/Snackbar (قرص پایین‌لنگر، حذف خودکار ~۲ثانیه، برای تأییدها)** — mockup offset ~۲٬۰۴۷٬۱۶۵ (`st.toast`). کد: `ui/navigation/MainScaffold.kt:191-201` (یک `SnackbarHost` مشترک، `RoundedCornerShape(50)` = قرص واقعی، `containerColor = solidSurface`). **جزئی — انحراف مستند**: شکل قرص و رفع کنتراست توپر (ADR-059) انجام شده، اما `SnackbarDuration` در Material3 فقط Short/Long/Indefinite ارائه می‌دهد، نه مدت دلخواه «~۲ثانیه» — کد از `Short` به‌عنوان نزدیک‌ترین گزینه‌ی موجود استفاده می‌کند (به‌صراحت به‌عنوان کاستی شناخته‌شده و پذیرفته‌شده در `MainScaffold.kt:181-190` مستند شده).
- **دسترسی نوار پایین/Drawer همبرگر** — هدر مشترک mockup (offset ~۱٬۹۶۴٬۶۰۰) همبرگر + لوگو + سوییچ زبان + سوییچ تم را در **همه‌ی** صفحات قرار می‌دهد. کد **هیچ کامپوننت هدر مشترک واحدی ندارد**: فقط `HomeHeader` در `HomeScreen.kt` مجموعه‌ی کامل (همبرگر + زبان + تم) را دارد؛ هر صفحه‌ی دیگر (`StudioHeader`، `SceneDetailHeader`، `BreakdownHeader`، `AssetFormHeader` که در Assets/Validation/Output/Settings/Backups/Composer بازاستفاده می‌شود) یک ردیف ساده‌ی بازگشت+عنوان+زیرعنوان است **بدون همبرگر، بدون سوییچ درون‌خطی زبان/تم**. **جزئی** یافته‌ی مشترک — سوییچ سریع زبان/تم فقط از Home و از صفحه‌ی Settings در دسترس است، نه از هدر هر صفحه طبق مشخصات mockup («از همان کنترل‌های هدر به‌علاوه‌ی Settings در دسترس»). در هیچ ADR ای بحث نشده.
- **FAB مرکزی («ایجاد سریع») روی نوار پایین همیشه‌روشن** — mockup یک عمل ایجاد سریع کارآمد را فرض می‌کند (README §۱ «Home»؛ بخش تعاملات). کد: `ui/navigation/BottomNavBar.kt:63-64,86-113` `onQuickCreate` را متصل می‌کند، اما `MainScaffold.kt:220-225` یک **لامبدای کاملاً خالی** پاس می‌دهد (`onQuickCreate = { /* comment only */ }`). **پیاده‌نشده به‌طور کامل** — FAB مرکزی از نظر بصری روی هر صفحه‌ی ریشه وجود دارد (دایره‌ی بنفش، آیکون `+`) اما با کلیک هیچ کاری در کل اپ انجام نمی‌دهد. این در یک کامنت کد به‌عنوان کار آینده‌ی موکول‌شده تأیید شده اما هیچ ورودی ADR رسمی ندارد.
