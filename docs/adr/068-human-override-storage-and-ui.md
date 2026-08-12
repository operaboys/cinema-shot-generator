# ADR-068: Human Override — لایه‌ی ذخیره‌سازی + نقطه‌ی ورود UI (صفحه‌ی Validation)

## زمینه

این قدم یافته‌ی معماری بزرگ کشف‌شده در ADR-067 (بخش G3) را رفع می‌کند:
کل مفهوم Human Override (واحد ۰۱، بخش ب) هرگز به هیچ اقدام واقعی کاربر
وصل نشده بود — `createOverride`/`revokeOverride` دامنه از قبل کامل و
تست‌شده بودند، اما هیچ UI ای آن‌ها را صدا نمی‌زد و هیچ Entity/Dao
مستقلی وضعیت فعلی یک Override (active/revoked) را نگه نمی‌داشت.

## بازبینی مستقل پیش‌بررسی دستور کار (طبق دستور صریح)

**یافته‌ی مهم — مغایرت با پیش‌بررسی دستور کار:** پیش‌بررسی فرض کرده بود
هیچ Entity/Dao مستقلی برای HumanOverride وجود ندارد و این قدم احتمالاً
باید یک لایه‌ی ذخیره‌سازی تازه بسازد (هم‌الگو با Migration StoryContext
فاز ۲ واحد ۱۶). با `grep -rln "OverrideDao\|OverrideEntity"
app/src/main/java/.../` تأیید شد **`OverrideEntity`/`OverrideDao`
(`data/entity/OverrideEntity.kt`, `data/dao/OverrideDao.kt`) از قدم اول
واحد ۱۵ از قبل ساخته شده و در `AppDatabase.kt` ثبت شده بودند** — دقیقاً
همان الگوی `EventLogDao` (قدم قبل، G3) که ساخته شد اما هرگز وصل نشد.
تأییدشده با grep در `ui/`/`data/repository/`/`app/src/test/`: صفر
استفاده. یعنی «بخش الف» این قدم به هیچ Entity/Dao/Migration تازه‌ای
نیاز نداشت — فقط یک Repository لازم بود که `HumanOverride` دامنه را به
همان `OverrideEntity` موجود نگاشت کند. این یک ساده‌سازی واقعی نسبت به
آنچه دستور کار پیش‌بینی کرده بود.

`OverrideEntity` (schema موجود، تغییرنکرده): `overrideId, entityType,
entityId, overrideDataJson, active` — **هیچ فیلد `projectId` مستقلی
ندارد** (طبق کامنت خودش، `entityId` عمداً Polymorphic طراحی شده). این
همان سؤال دستور کار («آیا `Scope.entityId` کافی است یا `projectId`
مستقل هم لازم است؟») را حل می‌کند: چون این Schema از قبل قفل‌شده
(واحد ۱۵) و تغییر آن خارج از Scope این قدم بود، تصمیم گرفتم آن را
دست‌نخورده نگه دارم — `getOverridesForEntity(shotId)` برای نیاز واقعی
صفحه‌ی Validation (که همیشه دقیقاً یک shot را نشان می‌دهد) کاملاً کافی
است.

## بخش الف — `HumanOverrideRepository`

`data/repository/HumanOverrideRepository.kt` (تازه) — `saveOverride`/
`loadActiveOverridesForEntity`، دقیقاً هم‌الگو با DTO محلی
`@Serializable` سایر Repository ها (`ProjectEntityDto`/`ShotDto`):
`OverrideDataDto` فیلدهایی از `HumanOverride` را نگه می‌دارد که در
ستون‌های مسطح `OverrideEntity` جا نمی‌شوند (`overrideType`, `createdAt`,
`field`, `originalValue`, `overrideValue`, `reason`, `usageCount`,
`lastApplied`, `revoked`, `revokedAt`, `revokedReason`) و در
`overrideDataJson` ذخیره می‌شود.

## بخش ب — نقطه‌ی ورود UI (صفحه‌ی Validation)

**تصمیم مستقل — نگاشت Scope برای این نقطه‌ی ورود (مهم‌ترین تصمیم این
قدم):** `OverrideScope` طبق بلوپرینت (`docs/blueprints/01-story-and-
override-v2.md`) و `docs/governance/override-policy.md` برای «تغییر
واقعی یک مقدار فیلد» طراحی شده (مثال خودِ سند: `camera.angle: eye-level
→ dutch-angle`، با یک هشدار مرتبط که به‌عنوان اثر جانبی سرکوب می‌شود).
دکمه‌ی «تجاوز از این هشدار» روی خودِ صفحه‌ی Validation (طبق دستور کار)
هیچ مقدار تازه‌ای برای هیچ فیلدی نمی‌گیرد — کاربر فقط یک هشدار موجود را
آگاهانه می‌پذیرد، نه یک مقدار فیلد را عوض می‌کند. این یک ناهم‌خوانی
معنایی واقعی بین Scope دامنه و این نقطه‌ی ورود درخواستی است.

**راه‌حل انتخابی (سازش صادقانه، نه سوءاستفاده‌ی خاموش از Scope):**
- `field = issueKey(issue)` — `issue.field ?: issue.message` (چون
  `ValidationIssue.field` اغلب `null` است — بسیاری از توابع `checkXxx`
  آن را پرنکرده‌اند — `message` که از خودِ شرط واقعی نقض‌شده مشتق
  می‌شود Fallback پایدارتری از Index لیست است، که بین بارگذاری‌های
  مختلف می‌تواند جابه‌جا شود).
- `originalValue = issue.message` — خودِ متن هشداری که Override شد.
- `overrideValue = "acknowledged_by_user"` — یک Sentinel ثابت، **نه** یک
  مقدار واقعی فیلد.

این جایگزین بهتری نسبت به جعل یک مقدار فیلد ساختگی بود (که Scope را
گمراه‌کننده می‌کرد) و کاملاً صادقانه در Audit Trail (`EventLogDao`)
ثبت می‌شود.

**`RuleSeverity` در برابر `Severity`:** این دو enum ساختاری کاملاً
یکسان (`{BLOCKING, WARNING}`) اما مستقل‌اند — یکی در `domain.story`
(واحد ۰۱)، دیگری در `domain.validation` (واحد ۰۷) — یک تکرار
از‌پیش‌موجود که این قدم آن را نساخته. نگاشت با
`RuleSeverity.valueOf(issue.severity.name)` (تطبیق نام رشته‌ای) حل شد؛
یکی‌سازی این دو enum خارج از Scope این قدم است.

**نمایش «Overridden» (نه ناپدیدشدن):** `IssueCard` هنگام وجود یک
Override فعال، متن هشدار را با `TextDecoration.LineThrough` نشان
می‌دهد و یک برچسب «Override شده» + دکمه‌ی «لغو Override» اضافه می‌کند
— دقیقاً طبق الزام صریح دستور کار («متمایز شود، نه ناپدید»). طراحی
انتخاب‌شده (Strikethrough + Badge) از توکن‌های موجود تم استفاده می‌کند،
بدون نیاز به رنگ/کامپوننت تازه.

**تصمیم مستقل — Revoke Inline، نه یک صفحه‌ی مستقل:** طبق اجازه‌ی صریح
دستور کار («اگر حجم زیاد بود، فقط Revoke Screen مستقل را می‌توانی به
یک قدم بعدی موکول کنی»)، یک صفحه‌ی جداگانه‌ی «Browse همه‌ی Override های
پروژه» ساخته نشد. دلیل: (۱) `OverrideEntity` هیچ `projectId` ای ندارد،
پس یک Query کارآمد «همه‌ی Override های این پروژه» بدون یک ستون تازه
یا Join گران روی همه‌ی Shot/Scene ممکن نیست؛ (۲) صفحه‌ی Validation از
قبل دقیقاً محدوده‌ی طبیعی برای دیدن/لغو Override های یک شات است. Revoke
Inline (دکمه‌ی «لغو Override» مستقیم روی همان IssueCard، با یک Dialog
ساده‌ی reason اختیاری) کاملاً الزام «حداقل ساده» را برآورده می‌کند.
صفحه‌ی مستقل Browse-همه‌ی-Override (اگر لازم شد) به یک قدم آینده موکول
شد — نیازمند یک تصمیم طراحی جدید درباره‌ی افزودن `projectId` یا Query
تجمیعی، نه فقط UI.

## یافته‌ی واقعی دیباگ — دو باگ Async واقعی کشف و رفع شد

۱. **`refreshActiveOverrides` اولیه یک `ioScope.launch` جداگانه (نه
   `suspend`) بود** — Job برگشتی `createOverrideForIssue`/
   `revokeOverrideAction` فقط منتظر تکمیل کد بیرونی می‌ماند، نه این
   Launch داخلی un-awaited؛ `.join()` تست بدون خطا برمی‌گشت اما
   `activeOverrides` هنوز به‌روز نشده بود. رفع: `refreshActiveOverrides`
   اکنون `suspend` است و مستقیم (نه با `launch` تازه) از همان Coroutine
   بیرونی صدا زده می‌شود، پس بخشی از همان Job است.
۲. **`ValidationScreen` هیچ‌جا `viewModel.activeOverrides` را
   `collectAsStateWithLifecycle()` نمی‌کرد** — `overrideLookup` مستقیماً
   `_activeOverrides.value` (یک `MutableStateFlow` خام) را می‌خواند،
   بدون Observe کردن هیچ Compose State ای؛ تغییر واقعی داده هرگز باعث
   Recomposition نمی‌شد و بج «Override شده» تا ابد ظاهر نمی‌شد. رفع:
   `val activeOverrides by viewModel.activeOverrides.collectAsStateWithLifecycle()`
   اضافه شد و `overrideLookup` اکنون مستقیماً از همین State جمع‌آوری‌شده
   می‌خواند.

هر دو باگ فقط با تست End-to-End واقعی UI (`ValidationOverrideFlowTest`,
نه فقط تست مستقیم ViewModel) کشف شدند — دلیل اضافی برای اینکه دستور کار
هر دو نوع تست را خواسته بود.

## `database` به `ValidationScreen`/`AppNavHost` اضافه شد

`ValidationScreen`/`AppNavHost.kt` قبلاً هیچ‌کدام پارامتر `database`
تزریقی نداشتند — بدون آن، `HumanOverrideRepository`/
`RoomOverrideEventLogger` همیشه به `AppDatabase.getInstance` (Singleton
تولیدی) برمی‌گشتند، حتی وقتی سایر Repository ها (`shotRepository` و
غیره) برای تست به یک پایگاه‌داده‌ی in-memory تزریق شده بودند — یک نشتی
واقعی بین محیط تست و تولید. `database: AppDatabase? = null` هم‌الگو با
`BackupsScreen`/`ProjectListViewModel` (ADR-059/063) به `ValidationScreen`
اضافه و از `AppNavHost.kt` (که از قبل این پارامتر را داشت) پاس داده شد.

## تست‌ها

- `ValidationViewModelOverrideTest.kt` (۳ تست، مستقیم روی ViewModel، هم‌الگو
  با `OutputDeliveryViewModelTest.kt`): Round-Trip واقعی روی Room
  (خواندن مستقل از `OverrideDao`، نه یک ViewModel دوم — طبق یافته‌ی
  دیباگ بالا، ساخت یک ViewModel دوم برای «شبیه‌سازی بازکردن دوباره»
  خودش یک مسابقه‌ی Async غیرقابل‌اعتماد بود)؛ تلاش Override یک Issue با
  `Severity.BLOCKING` → `canOverride` false و چیزی ذخیره نمی‌شود؛
  EventLog واقعی برای create و revoke هر دو از مسیر کامل ViewModel.
- `ValidationOverrideFlowTest.kt` (۱ تست End-to-End، Mount مستقیم
  `ValidationScreen` هم‌الگو با `DnaSoftLockWarningTest.kt`): کلیک واقعی
  روی دکمه‌ی Override → Dialog → تأیید → بج «Override شده» ظاهر می‌شود
  → خواندن مستقل `OverrideDao` تأیید می‌کند ردیف واقعی ذخیره شده → کلیک
  Revoke → Dialog → تأیید → دکمه‌ی Override دوباره ظاهر می‌شود.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
