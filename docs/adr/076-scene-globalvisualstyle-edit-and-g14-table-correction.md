# ADR-076: رفع G8 (`globalVisualStyle` قابل‌ویرایش) + تصحیح جدول G14

## بخش الف — رفع G8

`SceneSettingsDialog` (`SceneDetailScreen.kt`) قبلاً `title`/`narrativeRole`/
`timeOfDay`/`atmosphere*` را می‌گرفت اما `globalVisualStyle` را فقط در
`OverviewTab` نمایش می‌داد، بدون هیچ مسیر ویرایش. `Scene.globalVisualStyle`
از نوع `GlobalVisualStyleRef(source: String = "project_dna", override:
String? = null)` است — یک رشته‌ی خام، نه `VisualStyle` enum مستقیم (طبق
شکل JSON بلوپرینت).

پیاده‌سازی: یک `AssetFormEnumDropdownField` تازه (همان الگوی
`narrativeRole`/`timeOfDay`/`atmosphere` در همین Dialog) با گزینه‌ی اول
«از DNA پروژه» (کلید موجود `sceneDetail.overview.globalVisualStyleFromDna`
— بازاستفاده شد، کلید تازه لازم نبود) که `override` را `null` می‌کند، سپس
هر ۳۴ مقدار `VisualStyle` (واحد ۰۲) که `override` را با `.name` رشته‌ای
ذخیره می‌کند — بدون تغییر نوع دیتامدل. `source` همیشه `"project_dna"`
می‌ماند. `SceneSettingsDialog.onSave` و `SceneDetailViewModel.saveSceneSettings`
هر دو پارامتر `globalVisualStyleOverride: String?` تازه گرفتند.

**محدودیت باقی‌مانده، آگاهانه (طبق قانون صریح این قدم):** خط نمایش
`OverviewTab` (`scene.globalVisualStyle.override ?: uiString(...)`)
دست‌نخورده ماند — یعنی بعد از انتخاب یک سبک، Overview مقدار خام `.name`
(مثلاً `"FILM_NOIR"`) را نشان می‌دهد، نه `visualStyleLabel` ترجمه‌شده.
این یک نقص UX کوچک و قابل‌رفع در یک قدم بعدی است، نه بخشی از این رفع.

## بخش ب — تصحیح جدول G14

بازبینی مستقل (بدون اعتماد به توضیح دستور کار) هر دو ادعا را با خواندن
مستقیم کد تأیید کرد:

1. **State Machine/Lock:** `domain/scene/SceneLifecycle.kt` (`lockScene`)
   و `domain/project/ProjectLifecycle.kt` (`archiveProject`) هر دو
   کامنت `MIGRATED (رفع بدهی فنی مستند در ADR-064/ADR-066)` دارند و
   واقعاً `validateStateTransition(current, target, customMessage)` را
   صدا می‌زنند، نه `canTransition` خام.
2. **Storage/Import:** `ProjectsScreen.kt` (`importLauncher`،
   `ActivityResultContracts.GetContent()`) واقعاً `projectListViewModel.importProject(...)`
   را صدا می‌زند — رفع اصل مشکلی که ADR-064 برای تصمیم مشترک علامت زده
   بود («`importProject` هیچ فراخوان‌کننده‌ای در کل پروژه ندارد»).

هر دو مورد قبلاً در ADR-064 («تصمیم ۳» و «تصمیم ۶») صریحاً برای تصمیم
مشترک علامت‌گذاری شده بودند؛ ADR-066 و ADR-065 (به ترتیب) همان قدم‌های
رفع بودند — اما `docs/audit/post-unit16-full-audit.md` (بخش «وضعیت نهایی
رفع یافته‌ها»، افزوده‌ی قدم قبلی) هنوز آن‌ها را زیر ردیف G14 معلق نشان
می‌داد.

**یافته‌ی دقیق‌تر (نه رد ادعا):** این تأیید به‌معنای وایرشدن تک‌تک Rule
های نام‌برده در متن اصلی G14 نیست. `validateProjectId`/
`validateStorageAvailable`/`validateSchemaVersion`/
`validateReferentialIntegrityAsIssues` (`domain/storage/`) همچنان با
`grep` سراسری هیچ فراخوان‌کننده‌ای ندارند — فقط `validateReferentialIntegrity`
(نسخه‌ی خواهر، نه `...AsIssues`) از طریق `ExportImportRepository.importProject`
واقعاً اجرا می‌شود. جدول اصلاح شد تا این نکته را هم صریح بگوید، نه فقط
«رفع شد» بگوید.

**دامنه‌ی تغییر:** فقط ردیف G14 و G8 در جدول «وضعیت نهایی» تصحیح شدند؛
بقیه‌ی گروه‌های G14 (دوربین/حرکت، Output Delivery/i18n، Prompt
Finalization، Asset، Shot، Audio، DNA، Visual Identity، AI Story
Breakdown) و متن اصلی گزارش (بخش‌های اولیه) دست‌نخورده ماندند.

## راستی‌آزمایی

- `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  موفق.
- `gradle :app:testDebugUnitTest` روی `ui/scenes` → ۴ تست، ۰ شکست
  (یک تست تازه: انتخاب سبک از Dropdown، ذخیره، و تأیید نمایش — با
  Assertion صادقانه روی رفتار واقعی فعلی نمایش نام خام Enum، نه رفتار
  ایده‌آل).

## Skills استفاده‌شده

`zero-hallucination-coder` — هر دو بخش صریحاً خواستند بررسی مستقل: بخش
الف با خواندن دقیق نوع `GlobalVisualStyleRef` و الگوی موجود
`AssetFormEnumDropdownField` پیش از نوشتن کد؛ بخش ب با خواندن مستقیم
`SceneLifecycle.kt`/`ProjectLifecycle.kt`/`ProjectsScreen.kt` و
grep سراسری روی چهار تابع `domain/storage/` (نه فقط پذیرفتن ادعای
دستور کار).
