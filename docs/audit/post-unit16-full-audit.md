# ممیزی جامع پس از واحد ۱۶ (کل پروژه، واحد ۰۱ تا ۱۶)

**تاریخ:** 2026-08-09
**نقش:** این فایل صرفاً یک گزارش است — هیچ کد یا بلوپرینتی در این قدم تغییر نکرد.
**محدوده:** برخلاف `docs/audit/pre-unit16-audit.md` (که فقط مطابقت بلوپرینت ۱۶ با
کد را بررسی کرد)، این ممیزی تمرکزش روی **باگ‌های واقعی، عملکردهای ناقص/نیمه‌کاره،
ناسازگاری UI/UX، و بدهی فنی واقعی** در کل پروژه (واحد ۰۱ تا ۱۶) است — نه صرفاً
مطابقت با بلوپرینت.

**روش:** یکجاسازی و بازبینی فهرست «محدودیت‌های شناخته‌شده»ی README.md؛ grep
سراسری برای TODO/FIXME/Placeholder؛ بررسی مسیرهای Navigation/ViewModel برای
فرم‌های «فقط ساخت»؛ grep برای الگوی File Picker؛ بررسی اثر واقعی سوییچ‌های
Settings/Layout Variant؛ grep برای Rule های Validation یتیم در کل `domain/`؛
بررسی جامع الگوی `AppDatabase.getInstance` مستقیم؛ grep برای Low-opacity Tinted
Fill باقی‌مانده؛ grep برای تست‌های Skip/Ignore شده؛ بررسی واقعی Export؛ بررسی
دیالوگ‌های تأیید عملیات مخرب.

---

## بخش ۱ — یکجاسازی محدودیت‌های شناخته‌شده‌ی موجود README.md (وضعیت هرکدام تأییدشده)

این بخش، برخلاف بقیه‌ی این ممیزی، یافته‌ی تازه نیست — یکجاسازی و **تأیید وضعیت
فعلی** (رفع‌شده یا هنوز باز) دو فهرست موجود است: (الف) ۱۲ یافته‌ی
`docs/audit/pre-unit16-audit.md`، (ب) ۱۲ بند «محدودیت‌های شناخته‌شده»ی فعلی
`README.md` (تجمیعی از فازهای واحد ۱۶).

### الف) وضعیت ۱۲ یافته‌ی `pre-unit16-audit.md`

| # | عنوان | وضعیت تأییدشده (این ممیزی) |
|---|---|---|
| F1 | `domain/workflow/` غایب بود | ✅ رفع شد — پکیج با تمام انواع موردنیاز موجود است |
| F2 | `Scene`↔`LocationAsset` بدون ارجاع | ✅ رفع شد — `Scene.locationAssetId` اضافه شد (ADR-038) |
| F3 | `mapMoodToLighting` رشته‌ی خام | ✅ رفع شد — ADR-039 (Type-Safe شد) |
| F4 | `getPacingFromEmotion` رشته‌ی خام | ✅ رفع شد — ADR-039 |
| F5 | `CharacterAsset.outfits` — فرم فقط TextField تکی | ⚠️ **هنوز باز** — تأیید تازه: `ui/assets/CharacterAssetFormScreen.kt:159-169` یک فیلد Outfit پیش‌فرض (نام+توضیح) دارد، اما دکمه‌ی «مدیریت Outfit ها» (`characterForm.manageOutfits`) فقط پیام `manageOutfitsComingSoon` نشان می‌دهد — بدون UI واقعی برای افزودن/ویرایش چند Outfit. جزئیات در بخش ۳ پایین (F5-جدید همان مورد است، دوباره فهرست نشد). |
| F6 | فرم Location — لیست‌های Compatibility/`environment`/`keyElements` غایب | ✅ **رفع شد** (برخلاف یادداشت قدیمی README که می‌گفت «عمداً موکول شد») — تأیید تازه: `ui/assets/LocationAssetFormScreen.kt` واقعاً `timeCompatibility`/`weatherCompatibility`/`keyElements` را به‌صورت لیست تگ (نه TextField تکی) و `environmentType`/`environmentSize` را به‌صورت دو فیلد جدا می‌گیرد (خطوط ۶۳–۱۳۳). این یک **اصلاح واقعی نسبت به مستندات موجود** است — README در بخش Stack/ساختار باید این تناقض را رفع کند (بند ⚪ در بخش ۳). |
| F7 | `type-registry.md` — `LightingSettings` فیلد جاافتاده/نام غلط | ✅ رفع شد (`type-registry.md:141`، برچسب `رفع F7`) |
| F8 | `type-registry.md` — `EnvironmentSettings` فیلد غلط/جاافتاده | ✅ رفع شد (`type-registry.md:142`، برچسب `رفع F8`) |
| F9 | Rule ۸ بلوپرینت ۰۵ (`negative_prompt_override` whitespace) پیاده نشده | ✅ رفع شد — `validateNegativePromptOverride` (`domain/shot/ShotValidation.kt:88`) اضافه و در `ValidationAggregator.kt:96` واقعاً وایر شده (تأیید تازه با grep) |
| F10 | ۱۲ کامنت هدر بدون `-v2` | ✅ رفع شد — grep تازه صفر نتیجه داد |
| F11 | `type-registry.md` — `Shot.camera/.lighting/.environment` اشتباهاً nullable | ✅ رفع شد |
| F12 | بلوپرینت ۱۶ فقط ۴ از ۵ مقدار `EntityState` را نام برده | ✅ رفع شد (هم در متن بلوپرینت، هم عملاً در UI — طبق ADR-059 بخش ب-۴) |

**نتیجه:** از ۱۲ یافته‌ی ممیزی قبلی، **۱۱ مورد کاملاً رفع شده‌اند**؛ فقط **F5
(مدیریت چند-Outfit)** هنوز باز است. F6 با شواهد تازه رفع‌شده تأیید شد — یک
یادداشت قدیمی در README (خط نزدیک قدم Asset Forms) که می‌گوید F6 «عمداً موکول
شد» دیگر دقیق نیست و باید در commit بعدی که کد ویرایش می‌شود اصلاح شود (این قدم
فقط ممیزی است، کد/README تغییر نکرد).

### ب) وضعیت ۱۲ بند «محدودیت‌های شناخته‌شده»ی فعلی README.md

تمام ۱۲ بند این فهرست **در همین نشست (۲۴ ساعت گذشته)** نوشته شده‌اند — یعنی
هیچ Drift زمانی محتمل نیست (کد از زمان نوشتن آن‌ها تغییر نکرده). همه هنوز دقیقاً
معتبرند:

1. `sendToAiConnector` هنوز `TODO()` — باز (عمدی، ADR-035).
2. Navigation بعد از AI Story Breakdown همیشه Tab «داستان» — باز (عمدی، ADR-046).
3. DNA Tab محدودیت‌ها — ✅ قبلاً رفع شد (Strikethrough در خودِ README).
4. فرم‌های Asset فقط «ساخت» — باز (بخش ۳ پایین جزئیات تازه دارد).
5. Scene Detail فقط «ساخت»، بدون حذف Scene — باز (بخش ۳ پایین).
6. Shot Composer Tab «اصلی» Placeholder + بدون آپلود تصویر واقعی + فقط ۴ از ۱۷ Rule وایرشده — باز (بخش ۳/۶ پایین جزئیات تازه‌تر دارند).
7. Output Delivery Export فقط Clipboard — باز (بخش ۵ پایین جزئیات تازه دارد).
8. Settings — سوییچ‌های Display/Layout Variant بدون اثر Runtime — باز (بخش ۴ پایین جزئیات تازه دارد).
9. Backups — Restore/Delete بدون تأیید — باز (بخش ۷ پایین جزئیات تازه دارد).
10. Snackbar مدت‌زمان دقیق — باز (محدودیت API، پذیرفته‌شده).
11. `backTargetsByRouteKey` بلااستفاده — باز (تصمیم معماری، نه باگ).
12. آپلود تصویر واقعی هیچ‌جا نیست — باز (بخش ۵ پایین جزئیات تازه دارد).

**نتیجه:** هیچ Drift ای بین README و کد واقعی پیدا نشد (منطقی، چون فهرست تازه
نوشته شده بود)؛ این بخش صرفاً یک تأیید مستقل بود، نه یک یافته‌ی جدید.

---

## بخش ۲ — Nav Drawer: ۸ از ۱۱ لینک هنوز «به‌زودی» هستند، با اینکه صفحه‌ی واقعی‌شان از فاز ۲ ساخته شده

### G1 — لینک‌های Nav Drawer برای Story/AI-Breakdown/DNA/Scenes/Shots/Validation/Prompt-Generator/Output-Delivery هرگز به‌روزرسانی نشدند

**فایل:** `app/src/main/java/com/operaboys/cinemashotgenerator/ui/navigation/NavDrawer.kt:19-48`
**نقل‌قول:**
```kotlin
// تصمیم مستند (docs/adr/044-unit16-phase1-app-shell.md):
// چون بیشتر این مقصدها هنوز مسیر Navigation واقعی ندارند، فقط «دارایی‌ها» (که با
// نوار پایین هم یکی است) واقعاً Navigate می‌کند؛ بقیه یک پیام «به‌زودی» نشان می‌دهند

private enum class DrawerAction { COMING_SOON, ASSETS, SETTINGS, BACKUPS }

private val studioLinks = listOf(
    DrawerLink("drawer.storyWizard"),      // → COMING_SOON
    DrawerLink("drawer.aiBreakdown"),      // → COMING_SOON
    DrawerLink("drawer.dnaManager"),       // → COMING_SOON
    DrawerLink("drawer.scenes"),           // → COMING_SOON
    DrawerLink("drawer.shots"),            // → COMING_SOON
    DrawerLink("drawer.assets", action = DrawerAction.ASSETS)
)
private val toolsLinks = listOf(
    DrawerLink("drawer.validation"),        // → COMING_SOON
    DrawerLink("drawer.promptGenerator"),   // → COMING_SOON
    DrawerLink("drawer.outputDelivery")     // → COMING_SOON
)
```

**توضیح مشکل:** این تصمیم در فاز ۱ (`ADR-044`) گرفته شد — در آن لحظه واقعاً درست
بود، چون هیچ‌کدام از این صفحات هنوز وجود نداشتند. اما از آن زمان، فازهای ۲ تا ۵
دقیقاً همین ۷ صفحه را ساختند و مسیر Navigation واقعی برایشان تعریف کردند:
`AiStoryBreakdown(projectId)`، `Validation(projectId, sceneId, ...)`،
`OutputDelivery(projectId, sceneId, ...)` (طبق `AppDestinations.kt`) از قبل در
`AppNavHost.kt` واقعی و کامل وایر شده‌اند — فقط از جای دیگری (دکمه‌ی داخل
Studio/ShotComposer)، نه از Nav Drawer. `DnaManager`/`Scenes`/`Shots`/`Story
Wizard` هم به‌صورت Tab داخل `StudioShell` واقعی‌اند (نه یک Route مستقل)، اما
حتی برای این‌ها هم Drawer می‌توانست به `Studio(activeProjectId)` Navigate کند
(هم‌الگو با راه‌حل واقعی `Backups`/`Settings` که از `WorkflowState.projectId`
استفاده می‌کنند) — این کار هرگز انجام نشد.

نتیجه: کاربر روی صفحه‌ی Nav Drawer که طبق سند طراحی («۱۱ آیتم، ۳ گروه») قرار
است نقطه‌ی ورود سراسری به همه‌ی بخش‌های اپ باشد، برای **۸ از ۱۱ آیتم** فقط پیام
«این بخش به‌زودی در دسترس خواهد بود» می‌بیند — درحالی‌که آن بخش واقعاً از
فازهای پیش ساخته شده و کاملاً کاربردی است، فقط از یک مسیر دیگر (نه Drawer)
قابل‌دسترس است. `drawer.promptGenerator` استثناست — طبق تصمیم مستند («Prompt
Generation» با قدم Output Delivery ادغام شد)، صفحه‌ی مستقلی برایش وجود ندارد؛
این یکی مورد انتظار است، اما بدون هیچ اصلاح متن/حذف از Drawer.

**دسته‌بندی:** 🟠 **عملکرد ناقص** — کاربر واقعاً انتظار دارد این لینک‌ها کار
کنند (سند طراحی صریحاً همین را وعده داده)، اما ۷ از ۸ مورد (به‌جز
promptGenerator) صرفاً به‌خاطر این‌که این بخش از پروژه از فاز ۱ بازبینی نشده،
غیرفعال مانده‌اند — نه یک محدودیت واقعی معماری.

---

## بخش ۳ — TODO/FIXME/Placeholder در کل `app/src/main`

grep کامل روی `TODO`, `FIXME`, `TODO()`, `NotImplementedError`، و اصطلاحات
فارسی رایج این پروژه (`به‌زودی`, `Placeholder`, `ناقص`, `در فاز بعدی`, `کار
آینده`, `هنوز پیاده نشده`) در کل `app/src/main`. مواردی که فقط توصیف یک وضعیت
**گذشته‌ی رفع‌شده** بودند (مثل «قبلاً Placeholder بود، حالا واقعی شد») فهرست
نشدند — فقط موارد واقعاً باز.

### G2 — `sendToAiConnector` عمداً `TODO()` است (قبلاً شناخته‌شده)

**فایل:** `domain/storybreakdown/AiConnector.kt:67-68`
```kotlin
suspend fun sendToAiConnector(...): Result<String> =
    TODO("پیاده‌سازی واقعی HTTP Call با Ktor Client — قدم جداگانه‌ی آینده")
```
تأیید شد هیچ کتابخانه‌ی HTTP (Ktor/Retrofit/OkHttp) در کل پروژه وجود ندارد —
اپ هرگز مستقیماً به یک سرویس AI متصل نمی‌شود. کاربر باید متن Prompt را دستی
Copy و به یک ابزار AI بیرونی بدهد (طبق `UiStrings.kt` کلید
`aiBreakdown.promptCardHint`). **دسته‌بندی:** ⚪ — قبلاً کاملاً مستند (ADR-035)،
تصمیم آگاهانه، فقط برای تکمیل تصویر اینجا تکرار شد.

### G3 — `OverrideEventLogger` واقعی هیچ‌جا پیاده نشده — رویدادهای Override هرگز ثبت نمی‌شوند

**فایل:** `domain/story/OverrideActions.kt:17-25`
```kotlin
/** NOTE: Placeholder — پیاده‌سازی واقعی ثبت رویداد متعلق به واحد ۱۲ است */
fun interface OverrideEventLogger {
    fun log(event: OverrideEvent)
    companion object { val NoOp: OverrideEventLogger = OverrideEventLogger { } }
}
```
grep تأیید کرد در کل پروژه هیچ پیاده‌سازی واقعی (غیر از `NoOp`) برای این
Interface وجود ندارد — هر بار کاربر یک Human Override واقعی می‌سازد (مثلاً
override دستی رنگ/نور یک شات)، این رویداد **هرگز در جایی ثبت/قابل‌مرور نمی‌شود**
— هیچ Audit Trail واقعی‌ای برای Override ها وجود ندارد، با اینکه خودِ
`EventLogEntity`/`EventLogDao` (واحد ۱۵) وجود دارند و برای `StateVersioning`
مصرف می‌شوند. **دسته‌بندی:** 🟡 — یک ویژگی واقعاً مستند در بلوپرینت ۰۱ که کاملاً
بی‌اثر مانده؛ کار می‌کند (Override واقعاً اعمال می‌شود) اما بدون تاریخچه.

### G4 — `OutputComposer.composeOutput`/`ExportFile` کد مرده‌اند — هرگز از `ui/`/`data/` صدا زده نمی‌شوند

**فایل:** `domain/outputdelivery/OutputComposer.kt`
```kotlin
data class ExportFile(val filename: String, val content: String, val mimeType: String)
fun composeOutput(...): OutputPackage
```
grep تأیید کرد تنها فراخوان `composeOutput` در کل پروژه تست واحدش
(`OutputComposerTest.kt`) است — نه `OutputDeliveryViewModel.kt`، نه هیچ‌جای
دیگر. این یعنی انتزاع «فایل خروجی واقعی» (نام فایل + محتوا + mimeType) که
دقیقاً زیرساخت لازم برای Export واقعی را دارد، **هرگز به لایه‌ی UI وصل نشده**
— جزئیات کامل در بخش ۱۰ (Export). **دسته‌بندی:** 🟡 — کد دامنه کامل و
تست‌شده است، فقط بی‌استفاده مانده.

### G5 — مدیریت چند-Outfit فقط دکمه‌ی Placeholder دارد (F5 قدیمی، هنوز باز)

**فایل:** `ui/assets/CharacterAssetFormScreen.kt:168-169`
```kotlin
TextButton(onClick = { onShowMessage(uiString("characterForm.manageOutfitsComingSoon", language)) }) {
    Text(uiString("characterForm.manageOutfits", language))
}
```
تکرار F5 قدیمی (بخش ۱)، برای تکمیل — یک Outfit پیش‌فرض واقعاً قابل‌ساخت است،
اما مدیریت چند-Outfit (لیست کامل `CharacterAsset.outfits`) فقط این پیام را
نشان می‌دهد. **دسته‌بندی:** 🟡 — قبلاً شناخته‌شده و مستند.

### G6 — Bottom Nav با یک `PLACEHOLDER_ACTIVE_PROJECT_ID` ثابت کار می‌کند

**فایل:** `ui/navigation/BottomNavBar.kt:39-41`
```kotlin
// فعلاً یک Placeholder ثابت است — تا وقتی مفهوم «آخرین/فعال پروژه» ساخته شود
internal const val PLACEHOLDER_ACTIVE_PROJECT_ID = "placeholder_active_project"
```
**دسته‌بندی:** 🟡 — نیازمند بررسی دقیق‌تر این‌که این مقدار در چه شرایطی واقعاً
استفاده می‌شود (این ممیزی این را تا انتها ردیابی نکرد — محدودیت پوشش، پایین
اعلام شده).

### بررسی‌شده، بدون یافته‌ی تازه (فقط تأیید وضعیت رفع‌شده)

`ui/theme/Type.kt` (فونت Placeholder رفع شد)، `ui/story/StoryTabContent.kt`،
`ui/dna/DnaTabContent.kt`، `ui/scenes/ScenesListScreen.kt`،
`ui/assets/AssetsScreen.kt` (همه: Placeholder فاز ۱ با محتوای واقعی جایگزین
شد)، `ui/navigation/AppDestinations.kt`/`MainScaffold.kt` (توصیف کلی الگوی
COMING_SOON — جزئیات دقیقش در G1 بالا تازه مستند شد).

**یافته‌ی مرتبط اما در بخش ۱۱ پوشش داده می‌شود:** `studio.tabPlaceholder`
(`ui/studio/StudioShell.kt`) و `shotComposer.tabPlaceholderNextStep` — هردو
هنوز در کد وجود دارند اما این ممیزی تأیید نکرد کدام Tab دقیقاً هنوز به آن‌ها
می‌رسد (نیاز به بررسی دقیق‌تر `StudioShell.kt`ی `when(selectedTab)` — طبق کد
فعلی خوانده‌شده در قدم قبل، فقط Tab «اصلی» Shot Composer و هیچ Tab‌ای در
StudioShell دیگر placeholder نیستند؛ Story/DNA/Scenes هر سه محتوای واقعی
دارند — تأیید مستقیم `StudioShell.kt` نشان داد تنها انشعاب `else ->
StudioTabPlaceholder` برای Tab «خروجی» (چهارمین Tab Studio که با Output
Delivery متفاوت است) باقی مانده. **دسته‌بندی:** 🟡).

---

## بخش ۴ — فرم‌های «فقط ساخت»: تأیید دقیق کدام موجودیت‌ها واقعاً مسیر ویرایش ندارند

### G7 — Character/Location/Object Asset: هیچ مسیر Navigation یا ViewModel ای برای ویرایش موجود ندارند (نه فقط UI ناقص — کارت‌ها اصلاً onClick ندارند)

**فایل‌ها:**
- `ui/navigation/AppDestinations.kt:45` — `data class AssetForm(val kind: AssetKind)` — فقط نوع Asset را حمل می‌کند، **هیچ فیلد شناسه‌ی Asset موجود ندارد**.
- `ui/assets/AssetsScreen.kt:257` —
  ```kotlin
  private fun AssetCard(name: String, tierLabel: String, description: String, continuityMeta: String) {
      Card(modifier = Modifier.fillMaxWidth()) {   // بدون onClick
  ```
  مقایسه با `ui/scenes/ScenesListScreen.kt:100`: `Card(onClick = onClick, ...)`.
- `CharacterAssetFormViewModel.kt:47,145-166` / `LocationAssetFormViewModel.kt:41,103-106` / `ObjectAssetFormViewModel.kt:36,96-98` — هر سه در `save()` یک شناسه‌ی تازه‌ی تصادفی می‌سازند (`idProvider() = { generateAssetFormId(...) }`)، بدون هیچ پارامتر `existingAssetId`.

**توضیح مشکل:** لمس یک کارت Asset موجود **هیچ اتفاقی نمی‌افتد** — نه فقط اینکه
فرم پیش‌پر نمی‌شود (آنچه README می‌گفت)، بلکه اصلاً هیچ `onClick`ای وصل نیست.
هر بار «ذخیره»، یک Asset کاملاً تازه با شناسه‌ی تصادفی جدید می‌سازد. این یعنی
هیچ راهی برای اصلاح حتی یک غلط املایی در توضیح یک کاراکتر موجود وجود ندارد —
باید از صفر دوباره ساخته شود.

**دسته‌بندی:** 🟠 — عملکرد ناقصی که کاربر واقعاً انتظارش را دارد (لمس یک کارت
باید چیزی نشان دهد)، ریسک واقعی گم‌شدن کار کاربر نیست (چون کارت‌ها حداقل قابل
مشاهده‌اند)، اما تجربه‌ی کاربری معیوب واقعی است.

### G8 — Scene: ویرایش واقعی وجود دارد اما فقط برای زیرمجموعه‌ی کوچکی از فیلدها

**فایل:** `ui/scenes/SceneDetailScreen.kt:173,213,383-455`
```kotlin
onEditClick = { showSettingsDialog = true }
...
onSave -> viewModel.saveSceneSettings(title, role, time, primary, secondary)
```
**تأیید:** برخلاف Asset، Scene واقعاً یک مسیر «بارگذاری موجود → ویرایش → ذخیره
با همان ID» دارد (`SceneDetail(projectId, sceneId)` → `SceneDetailViewModel`
→ `saveSceneSettings`) — اما `SceneSettingsDialog` فقط عنوان/نقش
روایی/زمان‌روز/جو را می‌گیرد؛ `location`/`globalVisualStyle` از این مسیر قابل
ویرایش نیستند. **دسته‌بندی:** 🟡 — ویرایش واقعی است، فقط ناقص (زیرمجموعه‌ای از
فیلدها).

### G9 — الگوی مرجع: Shot Composer واقعاً هم «ساخت» هم «ویرایش» را با یک الگوی درست پیاده کرده

**فایل:** `ui/navigation/AppDestinations.kt:75` + `ShotComposerViewModel.kt:107,293-296`
```kotlin
data class ShotComposer(..., val shotId: String? = null)
...
if (existingShotId != null) {
    val loaded = repository.loadShot(existingShotId).getOrNull()
```
این دقیقاً الگوی درستی است که Asset/Scene باید از آن پیروی کنند (نه یک یافته،
بلکه مرجع مقایسه برای G7/G8). **دسته‌بندی:** ⚪ — تأیید یک الگوی درست موجود.

---

## بخش ۵ — File Picker: هیچ زیرساخت واقعی در کل اپ وجود ندارد

grep برای `ActivityResultContracts|GetContent|PickVisualMedia|ACTION_GET_CONTENT|ACTION_OPEN_DOCUMENT|registerForActivityResult` در کل `app/src/main` — **صفر نتیجه**.

### G10 — «انتخاب تصویر» تنظیمات فقط پیام Snackbar است

**فایل:** `ui/settings/SettingsScreen.kt:128-130,304-306`
```kotlin
onChooseImage = { onShowMessage(comingSoonMessage) },
...
Button(onClick = onChooseImage, modifier = Modifier.testTag(SETTINGS_CHOOSE_IMAGE_BUTTON_TAG)) {
    Text(uiString("settings.chooseImageButton", language))
}
```
فقط «حذف تصویر» (پاک‌کردن مقدار Persist‌شده) واقعاً کار می‌کند — راهی برای
تنظیم واقعی یک تصویر اصلاً وجود ندارد. **دسته‌بندی:** 🟡 — قبلاً در README
مستند شده.

### G11 — «Attached References» شات فقط توضیح متنی است، بدون تصویر واقعی

**فایل:** `ui/shots/CameraTabContent.kt:479-498`
```kotlin
IconButton(onClick = { viewModel.addImageReference(selectedType, description.trim()) })
```
`ImageReference` فقط `{type, description}` متنی است — هیچ فایل/URI واقعی
گرفته نمی‌شود. **دسته‌بندی:** 🟡 — قبلاً مستند.

---

## بخش ۶ — سوییچ‌های Settings/Layout Variant: کدام واقعاً اثر دارند

جدول کامل (۱۱ فیلد Persist‌شده در `WorkflowPreferencesStore`/`WorkflowState`):

| فیلد | نوشته می‌شود در | خوانده/شاخه‌بندی می‌شود جای دیگر؟ | نتیجه |
|---|---|---|---|
| `language` | Settings | همه‌جا (`uiString`/RTL) | ✅ اثر واقعی |
| `theme` | Settings | `App.kt`، `HomeScreen.kt` | ✅ اثر واقعی |
| `autoSaveCadenceSeconds` | Settings | `StudioShell.kt` Timer | ✅ اثر واقعی |
| `selectedModelProfileId` | Output Delivery | `AppNavHost.kt`→`OutputDeliveryViewModel` | ✅ اثر واقعی (Session، نه DataStore) |
| `shotListViewMode` | Scene/Shot List Toggle | `ShotListScreen.kt` Grid/Timeline | ✅ اثر واقعی (Session، نه DataStore) |
| `homeLayoutVariant` | Settings | **هیچ‌جا** — `HomeScreen.kt` هرگز به آن ارجاع نمی‌دهد | ❌ فقط نوشته می‌شود |
| `composerLayoutVariant` | Settings | **هیچ‌جا** — `ShotComposerScreen.kt` هرگز به آن ارجاع نمی‌دهد | ❌ فقط نوشته می‌شود |
| `dynamicFontEnabled` | Settings | هیچ‌جا (خودِ کد اعتراف می‌کند) | ❌ فقط نوشته می‌شود |
| `minTouchTargetEnabled` | Settings | هیچ‌جا | ❌ فقط نوشته می‌شود |
| `reducedMotionEnabled` | Settings | هیچ‌جا | ❌ فقط نوشته می‌شود |
| `allowFreeStepJump` | Settings | هیچ‌جا (خودِ کد اعتراف می‌کند) | ❌ فقط نوشته می‌شود |
| `homeScreenImageUri` | Settings | هیچ‌جا — `HomeScreen.kt` هرگز آن را رندر نمی‌کند | ❌ فقط نوشته می‌شود |

### G12 — ۷ از ۱۱ سوییچ/فیلد Persist‌شده کاملاً بی‌اثرند (فقط نوشته می‌شوند)

**فایل:** `ui/workflow/WorkflowViewModel.kt:93-98,116-121,125-131`
```kotlin
// (dynamicFontEnabled/minTouchTargetEnabled/reducedMotionEnabled)
// این سه فلگ واقعاً Persist می‌شوند ... اما فعلاً هیچ اثر Runtime ای در جای
// دیگری از اپ ندارند — محدودیت شناخته‌شده و صریحاً مستند.
```
`homeLayoutVariant`/`composerLayoutVariant`/`homeScreenImageUri` هیچ کامنت
اعتراف‌گونه‌ای ندارند اما grep به همان نتیجه رسید — هیچ خواننده‌ای خارج از
`WorkflowViewModel.kt`/`SettingsScreen.kt` ندارند. **دسته‌بندی:** 🟡 — قبلاً تا
حدی در README مستند بود؛ این ممیزی فهرست کامل و دقیق ۷-موردی را برای اولین
بار مستند می‌کند (README قبلی فقط ۵ مورد را نام برده بود، بدون `homeLayoutVariant`/`composerLayoutVariant`/`homeScreenImageUri` به‌صورت جداگانه — این سه هم اکنون رسماً در همین دسته ثبت می‌شوند).

### کارت Privacy — بدون هیچ سوییچ

**فایل:** `ui/settings/SettingsScreen.kt:240-246` — `PrivacyCard` فقط ۳ ردیف
متنی نمایشی است (`PrivacyInfoRow`)، هیچ فیلد Boolean واقعی در
`WorkflowViewModel` ندارد. **دسته‌بندی:** ⚪ — طبق سند طراحی («Privacy: صرفاً
نمایشی») همین‌طور بودنش عمدی است، نه یافته.

---

## بخش ۷ — Rule های Validation یتیم در کل `domain/`

نکته‌ی مهم روش‌شناسی: `ValidationAggregator.aggregateShotValidation` (فراخوانی‌شده
از `ValidationViewModel`/صفحه‌ی Validation، واحد ۱۶ فاز ۵) یک نقطه‌ی اتصال
واقعی به بسیاری از Rule هایی است که در فازهای قبلی به‌عنوان «یتیم» ثبت شده
بودند — این یعنی ادعای قدیمی ADR-053 («اکثر ۱۳ Rule نور/محیط وایر نشده‌اند»)
دیگر برای صفحه‌ی Validation (نه Shot Composer) دقیق نیست: هر ۵ Rule نور + هر
۸ Rule محیط اکنون از طریق `aggregateShotValidation` واقعاً اجرا می‌شوند. (در
Shot Composer خودش، هنوز فقط ۴ Rule دوربین زنده‌اند — این بخش ثابت مانده.)

### G13 — کل Pipeline واحد ۱۳ (Prompt Finalization) از مسیر واقعی Output Delivery قطع است

پوشش کامل در بخش ۱۰ (Export) — چون این مهم‌ترین یافته‌ی این بخش است و مستقیم
به رفتار قابل‌مشاهده‌ی کاربر می‌رسد، آنجا با جزئیات کامل (🔴) ثبت شده، نه
اینجا تکراری.

### G14 — Rule های واقعاً یتیم (بدون هیچ فراخوان مستقیم/غیرمستقیم از `ui/`)

فهرست کامل (هرکدام با `grep -rln` تأییدشده که فقط در فایل تعریف‌شان ظاهر می‌شوند):

**دوربین/حرکت:**
- `validateSpeedIntensity` — `domain/camera/MotionIntensityValidation.kt:11`
- `validateMotionBlur` — `MotionIntensityValidation.kt:21`
- `validateCameraMovementDuration` — `domain/camera/CameraValidation.kt:73` (عمداً از `ValidationAggregator.kt:69` کنار گذاشته شده، طبق کامنت خودِ آن فایل)

**تجمیع/منطق سطح‌بالا (واحد ۰۷ اصلی):**
- `validateDataCompleteness` — `domain/validation/ValidationEngine.kt:46`

**State Machine/Lock (واحد ۱۲) — کل لایه‌ی Rule دور زده شده:**
- `validateStateTransition`، `validateReviewReadiness`، `validateDependenciesFinalized` — `domain/stateversioning/StateMachine.kt:30,43,52`
- `validateEditPermission` — `domain/stateversioning/LockMechanism.kt:68`
  توضیح: منطق واقعی قفل‌کردن Scene (`domain/scene/SceneLifecycle.kt:14` →
  `SceneDetailViewModel.kt:89`) مستقیماً از تابع پایین‌سطح `canTransition`
  (Boolean خام) استفاده می‌کند، نه از این لایه‌ی `ValidationIssue`-برگردان که
  مخصوص همین منظور ساخته شده بود.

**Output Delivery/i18n:**
- `validateProfileAvailability` — `domain/outputdelivery/ModelProfileLibrary.kt:65`
- `validateTranslationKeyFound`، `validateLanguageSupported` — `domain/outputdelivery/Bilingual.kt:43,64`
- `validateBilingualCompleteness` — `domain/outputdelivery/OutputComposer.kt:58`

**Prompt Finalization (واحد ۱۳) — کل فایل بی‌استفاده:**
- `checkTokenLimit` — `domain/promptfinalization/TokenCostCalculator.kt:22` (فقط از `finalizePrompt` صدا زده می‌شود، که خودش یتیم است — بخش ۱۰)
- `validateConflictsResolved`، `validateCompressionRatio` — `domain/promptfinalization/PromptCleaner.kt:147,157`

**Storage/Export-Import — کل قابلیت Import قطع از UI:**
- `validateProjectId`، `validateStorageAvailable`، `validateSchemaVersion` — `domain/storage/StorageValidation.kt:10,24,33`
- `validateReferentialIntegrityAsIssues` — `domain/storage/ReferentialIntegrity.kt:60` (خودِ `validateReferentialIntegrity` از `importProject` صدا زده می‌شود، اما `importProject` خودش هیچ فراخوان‌کننده‌ای در `ui/` ندارد — پوشش کامل در بخش ۱۰)

**Asset:**
- `validateImageFile`، `validateAssetIdUniqueness`، `validateAssetDeletion`، `validateReferenceImageFile`، `checkSimilarAssetName` — `domain/asset/AssetValidation.kt:24,40,53,76,101`

**Shot:**
- `validateImageReferenceFile` — `domain/shot/ShotValidation.kt:70`

**Audio:**
- `validateActionSoundTimeline` — `domain/audio/AudioValidation.kt:29`

**DNA:**
- `validateShotAspectRatio`، `checkMandatoryElementsPresent` — `domain/dna/DnaValidation.kt:91,109`

**Visual Identity (Style Matrix) — کل ماژول سازگاری سبک بی‌استفاده:**
- `validateShotDurationForCinematicMode` — `domain/visualidentity/CinematicLanguage.kt:84`
- `checkStyleCompatibility`، `validatePrimaryStyleUpdate`، `checkStyleMatrixCompatibility` — `domain/visualidentity/StyleMatrix.kt:35,68,79`
  توضیح: DNA Tab فیلدهای سبک اصلی/فرعی دارد، اما هیچ‌جای `DnaViewModel.kt`/
  `DnaTabContent.kt` این توابع سازگاری سبک را صدا نمی‌زند — یعنی کاربر
  می‌تواند دو سبک ناسازگار انتخاب کند بدون هیچ هشداری.

**AI Story Breakdown — چند Rule اجرا نمی‌شوند با اینکه Pipeline اصلی وایر است:**
- `validateFreeformStoryLength` (Rule 1) — `PromptBuilder.kt:81` — به‌جایش هیچ Gate ای روی طول داستان وجود ندارد
- `validateTargetShotCountRange` (Rule 2) — `PromptBuilder.kt:92` — `AiStoryBreakdownViewModel.kt:107` به‌جایش با `coerceIn(1,150)` بی‌صدا مقدار را کوتاه می‌کند (به‌جای نشان‌دادن خطا)
- `validateHighShotCount` (Rule 3) — `PromptBuilder.kt:106`
- `validateChunksComplete` (Rule 6) — `ChunkCombiner.kt:33`
- `validateJsonRepairResult` (Rule 7) — `JsonDoctor.kt:162`
- `validateApiKeyProvided`/`validateAiConnectorErrorMessage` (Rule 4/5) — `AiConnector.kt:72,89` — قابل‌درک، چون `sendToAiConnector` خودش TODO است (G2)

**دسته‌بندی کلی این بخش:** 🟡 برای همه — هیچ‌کدام باعث کرش یا رفتار غلط
نمی‌شوند (چون هرگز فراخوانی نمی‌شوند)، اما نشان‌دهنده‌ی مقدار زیادی منطق
دامنه‌ی کاملاً تست‌شده و کامپایل‌شونده هستند که به تجربه‌ی واقعی کاربر هرگز
نمی‌رسند. **استثنا:** `validateTargetShotCountRange` نادیده‌گرفته‌شدن‌اش به
نفع `coerceIn` بی‌صدا — این یکی 🟠 است (رفتار واقعی متفاوت از آنچه Rule
مستند می‌کند: کاربر انتظار پیام خطا دارد، نه کوتاه‌شدن خاموش عدد ورودی‌اش).

---

## بخش ۸ — DI: بررسی جامع الگوی `AppDatabase.getInstance` مستقیم

### G15 — `ProjectListViewModel.exportProject` هنوز همان الگوی باگ رفع‌شده در Backups را دارد

**فایل:** `ui/project/ProjectListViewModel.kt:76-90`
```kotlin
fun exportProject(projectId: String) {
    val database = AppDatabase.getInstance(getApplication())
    viewModelScope.launch {
        exportProjectFile(
            projectId = projectId,
            projectDao = database.projectDao(),
            ...
            backupFileStorage = DeviceBackupFileStorage(getApplication())
        ).onFailure { _lastActionMessage.value = it.message }
    }
}
```
فراخوانی‌شده از `ui/project/ProjectListSection.kt:50`
(`onExport = { projectId -> viewModel.exportProject(projectId) }`) — یک مسیر
UI واقعی و در دسترس. برخلاف بقیه‌ی این کلاس (که `repository: ProjectRepository`
تزریقی را استفاده می‌کند)، این متد مستقیماً `AppDatabase.getInstance(...)` را
داخل بدنه‌اش صدا می‌زند — دقیقاً همان کلاس باگی که در `BackupsViewModel`/
`StudioShell` (ADR-059) کشف و رفع شد. `ProjectListViewModel` هیچ پارامتر
`database: AppDatabase?` تزریقی ندارد؛ این متد نه با ViewModel واقعی تست شده
(فقط تابع دامنه‌ی زیرینش `exportProjectFile` مستقیماً در
`ExportImportRepositoryTest.kt` تست شده است).

**اثر واقعی روی Production:** هیچ — `AppDatabase.getInstance()` در مسیر واقعی
اپ همیشه همان Singleton سراسری است (دقیقاً مثل مورد Backups). این یک شکاف
تست‌پذیری/یکدستی الگو است، نه یک باگ رفتاری در حال اجرا.

**دسته‌بندی:** 🟡 — همان الگوی دقیق قبلاً رفع‌شده، این‌بار در یک نقطه‌ی دیگر
باقی مانده؛ رفعش با همان الگوی موجود (پارامتر تزریقی `database`) کم‌هزینه است.

### G16 — `AiStoryBreakdownViewModel.factory`: تزریق جزئی، همه‌چیز بی‌صدا نادیده گرفته می‌شود

**فایل:** `ui/storybreakdown/AiStoryBreakdownViewModel.kt:236-241`
```kotlin
val args = listOfNotNull(storyRepository, assetRepository, sceneRepository, shotRepository)
return if (args.size == 4) {
    AiStoryBreakdownViewModel(application, projectId, storyRepository!!, assetRepository!!, sceneRepository!!, shotRepository!!)
} else {
    AiStoryBreakdownViewModel(application, projectId)   // هر ۴ تزریقی بی‌صدا دور ریخته می‌شوند
} as T
```
اگر فراخوان (مثلاً یک تست) فقط ۱ تا ۳ از ۴ Repository را بدهد، **هر ۴ تا**
نادیده گرفته می‌شوند و به‌جایشان `AppDatabase.getInstance()` مصرف می‌شود —
حتی برای آن‌هایی که واقعاً داده شده بودند. `SceneDetailViewModel.factory`/
`ValidationViewModel.factory` این را درست پیاده کرده‌اند (هرکدام مستقل بررسی
می‌شوند). **دسته‌بندی:** 🟡 — یک باگ منطقی واقعی در کد Factory، اما فقط در
مسیر تست/تزریق جزئی رخ می‌دهد؛ در مسیر واقعی اپ (که همیشه هر ۴ را می‌دهد)
بی‌اثر است.

### بررسی‌شده، بدون یافته‌ی تازه (الگوی امن تأییدشده)

۱۸ فایل دیگر (لیست کامل در گزارش خام) از الگوی «مقدار پیش‌فرض
`AppDatabase.getInstance()` روی پارامتر `Repository?` سازنده» استفاده
می‌کنند — تأیید شد در مسیر واقعی اپ همه‌ی این‌ها Repository ساخته‌شده در
`App.kt` را دریافت می‌کنند (Default هرگز واقعاً اجرا نمی‌شود)، و تست‌ها هم
مستقیماً Repository جعلی تزریق می‌کنند. این یک الگوی متفاوت اما امن است —
یافته‌ی تازه‌ای نیست.

---

## بخش ۹ — Contrast: بررسی کامل باقی‌مانده

grep کامل `.copy(alpha` (چندین الگوی فاصله‌گذاری + Multiline) در کل `ui/` —
**فقط یک نتیجه، و آن یک کامنت است** (`ui/project/ProjectCard.kt:146`، توصیف
باگ قبلاً رفع‌شده). بررسی تکمیلی برای `Modifier.alpha(`، کلمه‌ی `scrim`، و
مقادیر Hex کم‌آلفای هاردکد (`Color(0x..)`) هم انجام شد — تنها موارد یافت‌شده
(`ui/theme/Color.kt` — `CardBorder`/`Hairline`/`HairlineStrong`/`Inset`) توکن‌های
Divider/Border مشروع‌اند، نه Fill چیپ/بج/فیلتر.

**نتیجه: هیچ یافته‌ی جدیدی در این بخش نیست — رفع Contrast قدم قبل کامل و
بدون Regression بوده است.**

---

## بخش ۱۰ — تست‌های Skip/Ignore شده

grep کامل `@Ignore|@Disabled|@Skip|Assume` در `app/src/test/` — **صفر نتیجه**.
هیچ تست غیرفعال/Skip‌شده‌ای در کل پروژه وجود ندارد. حتی کد عمداً ناقص
(`sendToAiConnector`) یک تست فعال دارد که دقیقاً انتظار `NotImplementedError`
را Assert می‌کند (`AiConnectorTest.kt:65-79`) — الگوی نظم پروژه: کد ناقص هم
باید تست فعال داشته باشد، نه Skip شود.

**نتیجه: هیچ یافته‌ای در این بخش نیست.**

---

## بخش ۱۱ — Export واقعی + یافته‌ی 🔴 بحرانی: بج «پاک‌سازی‌شده · نهایی‌شده» دروغ است

### G17 — 🔴 بحرانی: خروجی نهایی هرگز واقعاً «پاک‌سازی» نمی‌شود، با اینکه UI ادعای آن را می‌کند

**فایل ۱ (ادعا):** `ui/outputdelivery/OutputDeliveryScreen.kt:195`
```kotlin
text = uiString("outputDelivery.cleanedFinalizedBadge", language),
```
(`UiStrings.kt:689`: `"پاک‌سازی‌شده · نهایی‌شده"` / `UiStrings.kt:1426`: `"Cleaned · Finalized"`)

**فایل ۲ (واقعیت):** `ui/outputdelivery/OutputDeliveryViewModel.kt:129-132`
```kotlin
val preTruncationText = renderBlueprintToText(blueprint, profile)
val renderedOutput = render(blueprint, profile)
```
این ViewModel فقط `render`/`renderBlueprintToText` (واحد ۱۴، بخش Renderer) را
صدا می‌زند — grep تأیید کرد **هیچ‌جای این فایل** `finalizePrompt`/`cleanPrompt`/
`checkTokenLimit` (واحد ۱۳، Prompt Finalization Pipeline) را صدا نمی‌زند.

**چرا این یک باگ واقعی است، نه فقط یک محدودیت مستندشده:** `render()` صرفاً
قالب‌بندی متن از `PromptBlueprint` است — هیچ‌کدام از ۵ فاز واقعی Prompt
Cleaner (`domain/promptfinalization/PromptCleaner.kt`: Conflict Detection،
Redundancy Removal، Stop Words Filtering، Token Optimization، Final Polish)
هرگز روی متن نهایی اجرا نمی‌شوند؛ `checkTokenLimit` (بررسی واقعی طول توکن در
برابر محدودیت مدل، مجزا از `validatePromptLength` که فقط طول کاراکتر خام را
هشدار می‌دهد) هم هرگز اجرا نمی‌شود. یعنی متنی که کاربر می‌بیند، Copy می‌کند،
و «Export» می‌کند، **دقیقاً همان خروجی خام Renderer است** — نه محصول نهایی
Pipeline‌ای که واحد ۱۳ برایش ساخته شده. بج «Cleaned · Finalized» بدون قید و
شرط (نه پشت یک Flag «اگر واقعاً پاک‌سازی شد») رندر می‌شود — یک ادعای مستقیم و
گمراه‌کننده درباره‌ی کیفیت محصول اصلی این اپ (متن Prompt نهایی).

**زمینه‌ی تاریخی:** یادداشت قدیمی README («۴ واحد این زنجیره اکنون منطق
دامنه‌ی واقعی و کامپایل‌شونده دارند ... قابل زنجیره‌سازی است») از **قبل از
ساخت UI واحد ۱۶** نوشته شده بود و فقط ادعا می‌کرد این ۴ واحد *در سطح دامنه*
قابل زنجیره شدن‌اند — نه اینکه واقعاً در صفحه‌ی Output Delivery زنجیره شده‌اند.
وقتی صفحه‌ی Output Delivery در فاز ۵ ساخته شد، این اتصال هرگز واقعاً انجام
نشد؛ این ممیزی اولین بار این شکاف را کشف می‌کند.

**دسته‌بندی:** 🔴 **بحرانی** — یک ادعای غلط UI درباره‌ی محصول نهایی (تنها
خروجی واقعی کل اپ)، به‌علاوه‌ی از‌دست‌رفتن یک بررسی واقعی (Token Limit دقیق)
که کاربر روی آن حساب می‌کند.

### G18 — «Export» فقط مستعار «Copy» است (قبلاً شناخته‌شده، این‌بار با نقل‌قول دقیق)

**فایل:** `ui/outputdelivery/OutputDeliveryScreen.kt:120-128`
```kotlin
onCopy = {
    clipboardManager.setText(AnnotatedString(currentState.renderedOutput.formattedPrompt))
    onShowMessage(uiString("outputDelivery.copiedMessage", language))
},
onExport = {
    clipboardManager.setText(AnnotatedString(currentState.renderedOutput.formattedPrompt))
    onShowMessage(uiString("outputDelivery.exportedMessage", language))
}
```
دو دکمه، عملاً یک کد. grep سراسری کل `ui/`/`data/` برای
`Intent.ACTION_SEND|FileOutputStream|ContentResolver|MediaStore|DocumentFile|Intent.createChooser|startActivity`
**صفر نتیجه** برگرداند. تنها I/O فایل واقعی کل اپ
(`data/repository/BackupFileStorage.kt:47`، `file.writeText(content)`) فقط
به `filesDir` **داخلی** اپ می‌نویسد (نه External/MediaStore/Share) و فقط
برای بکاپ‌های داخلی مصرف می‌شود، نه Export کاربر. **دسته‌بندی:** 🟡 — قبلاً در
README مستند شده بود؛ این بخش فقط شواهد کد دقیق را اضافه کرد.

---

## بخش ۱۲ — دیالوگ‌های تأیید عملیات مخرب

### G19 — 🔴 بحرانی: Restore از بکاپ بدون تأیید (تکرار ADR-059، دوباره تأیید شد)

**فایل:** `ui/backups/BackupsScreen.kt:117-123`
```kotlin
onRestore = {
    viewModel.restore(backup.backupId) { success -> ... }
}
```
مستقیماً از `OutlinedButton(onClick = onRestore, ...)` صدا زده می‌شود — بدون
`AlertDialog`. **این عملیات داده‌ی فعلی پروژه را بی‌بازگشت جایگزین می‌کند.**
**دسته‌بندی:** 🔴 (ریسک واقعی از‌دست‌رفتن داده — تنها یک لمس اشتباه کل کار
جاری کاربر روی یک پروژه را با محتوای بکاپ جایگزین می‌کند).

### G20 — 🔴 بحرانی: Delete بکاپ بدون تأیید (تکرار ADR-059، دوباره تأیید شد)

**فایل:** `ui/backups/BackupsScreen.kt:124-127`
```kotlin
onDelete = {
    viewModel.delete(backup.backupId)
    ...
}
```
بدون دیالوگ. حذف یک بکاپ غیرقابل‌بازگشت است — اگر آخرین نسخه‌ی سالم پروژه در
همان بکاپ باشد، این ریسک واقعی از‌دست‌دادن تنها راه بازیابی است. **دسته‌بندی:**
🔴.

### G21 — 🔴 بحرانی (یافته‌ی تازه): آرشیو پروژه بدون تأیید و بدون هیچ مسیر بازگشت

**فایل:** `ui/project/ProjectCard.kt:128-131`
```kotlin
DropdownMenuItem(
    text = { Text(uiString("project.overflow.archive", language)) },
    onClick = { menuExpanded = false; actions.onArchive(project.projectId) }
)
```
→ `ProjectListSection.kt:49`: `onArchive = { projectId -> viewModel.archiveProject(projectId) }`
→ `ProjectListViewModel.kt:53-57` مستقیماً `repository.archiveProject(projectId)` را صدا می‌زند — **بدون هیچ دیالوگ**، برخلاف «حذف» که دقیقاً در همان منوی Overflow (سه‌نقطه) کنارش قرار دارد و دیالوگ تأیید واقعی دارد (`ProjectListSection.kt:73-81` → `DeleteProjectDialog`).

**چرا 🔴 است، نه 🟠:** خودِ State Machine (`domain/stateversioning/StateMachine.kt:11-17`)
تأیید می‌کند `ARCHIVED` یک وضعیت **کاملاً پایانی** است:
```kotlin
val ALLOWED_TRANSITIONS: Map<EntityState, List<EntityState>> = mapOf(
    ...
    EntityState.FINAL to listOf(EntityState.ARCHIVED),
    EntityState.ARCHIVED to emptyList()   // هیچ خروجی‌ای از ARCHIVED وجود ندارد
)
```
grep تأیید کرد در کل کدبیس (`ui/`/`domain/`) هیچ عملیات «Unarchive» ای وجود
ندارد — یعنی یک پروژه‌ی آرشیوشده **برای همیشه** آرشیو می‌ماند، بدون هیچ راه
UI برای بازگرداندنش. با این حال، این عملیات با یک لمس ساده روی منو، بدون
هیچ هشدار/تأییدی، اجرا می‌شود.

**دسته‌بندی:** 🔴 **بحرانی** — یک عملیات کاملاً غیرقابل‌بازگشت (طبق طراحی خودِ
State Machine)، بدون هیچ دیالوگ تأیید، در حالی که عملیات کم‌خطرتر (Delete
پروژه، که حداقل دیالوگ تأیید دارد) در همان منو محافظت‌شده است.

### G22 — Delete Scene/Shot/Asset اصلاً از UI قابل‌دسترس نیستند (نه شکاف تأیید، بلکه غیاب کامل قابلیت)

**Delete Scene، Delete Shot، Delete Asset:** هر سه در سطح DAO تعریف شده‌اند
(`SceneDao.deleteScene`، `ShotDao.deleteShot`، `AssetDao.deleteAsset`) و حتی
`domain/scene/SceneValidation.kt:60` یک تابع Validation مستقل برای
`deleteScene` دارد — اما grep تأیید کرد **هیچ‌کدام از هر جای `ui/` صدا زده
نمی‌شوند**. یعنی این ریسک «حذف بدون تأیید» عملاً وجود ندارد چون خودِ عملیات
حذف هنوز از UI اصلاً در دسترس نیست (این خودش یک محدودیت جدا است، نه یک شکاف
تأیید — کاربر اصلاً نمی‌تواند یک Scene/Shot/Asset را از داخل اپ حذف کند).
**دسته‌بندی:** 🟡 — قابلیت حذف این سه موجودیت اصلاً ساخته نشده، نه اینکه بدون
تأیید باشد.

---

## صداقت درباره‌ی محدودیت پوشش این ممیزی

این ممیزی با ترکیب بررسی مستقیم من (بخش ۱، ۲، بخش‌های تازه‌ی ۱۱/۱۲) و سه
Agent موازی (Explore) برای بخش‌های ۳ تا ۱۰ انجام شد — هرکدام با grep کامل روی
دایرکتوری‌های مشخص‌شده به‌علاوه‌ی خواندن دستی نتایج. با این حال، این موارد
**کامل بررسی نشدند** و باید صریحاً اعلام شوند:

- **`ui/shots/ShotComposerScreen.kt` وضعیت دقیق Tab «اصلی»/سایر Tab ها پس از
  آخرین تغییرات** — کامنت‌های کد ممکن است نسبت به کامیت‌های جدیدتر Stale
  باشند؛ این ممیزی فقط کامنت‌های موجود را نقل کرد، نه بررسی خط‌به‌خط
  `when(selectedTab)` نهایی هر تب.
- **`AutoSaveManager`ی مکانیزم Timer دوره‌ای واقعی** (نه فقط API
  `saveIfDirty`/`touch`) — تأیید شد این کلاس در `StudioShell.kt` صدا زده
  می‌شود، اما ردیابی کامل مسیر تا محل دقیق حلقه‌ی Timer به‌صورت مستقل توسط این
  ممیزی تکرار نشد (قدم قبل، ADR-058، این را مستند کرده بود — این ممیزی به آن
  اتکا کرد، بدون تکرار مستقل کامل).
- **هر ۹۳ فایل تست پروژه به‌صورت خط‌به‌خط خوانده نشدند** — بررسی تست‌های
  Skip‌شده صرفاً grep-based بود (بخش ۱۰)؛ یک تست غیرفعال‌شده با مکانیزم
  غیراستاندارد (مثلاً کامنت کامل شدن، یا فیلتر Gradle در
  `app/build.gradle.kts`) ممکن است از این grep جا مانده باشد — `build.gradle.kts` بررسی نشد.
- **`AndroidManifest.xml`** برای `<intent-filter>` های احتمالی (اپ به‌عنوان
  مقصد Share) بررسی نشد — خارج از دامنه‌ی درخواستی، اما یادداشت شد.
- **Rule های `Boolean`/`Result<Unit>`-برگردان** (نه `ValidationIssue`) مثل
  `canTransition`، `canDeleteAsset`، `wouldCreateCycle` — این‌ها از دامنه‌ی
  دقیق «تابع‌های ValidationIssue-برگردان» بخش ۷ خارج ماندند؛ ممکن است در
  میان‌شان هم موارد یتیم مشابه وجود داشته باشد که این ممیزی ندید.
- **مقایسه‌ی دقیق بلوپرینت‌به‌بلوپرینت Rule برای واحدهای ۰۳/۰۹/۱۰/۱۲/۱۳** (فراتر
  از بررسی «آیا صدا زده می‌شود») تکرار نشد — `pre-unit16-audit.md` قبلاً یک
  Spot-check روی ۰۹/۱۰ انجام داده بود؛ این ممیزی روی همان اتکا کرد.
- **کد `di/`** (پوشه‌ی خالی طبق README) و فایل‌های Gradle/CI بررسی نشدند —
  خارج از دامنه‌ی درخواستی (کد Kotlin کاربردی).

هیچ‌کدام از این محدودیت‌ها به این معنا نیست که یافته‌های بالا نامطمئن‌اند —
همه‌ی ۲۲ یافته با نقل‌قول مستقیم کد و/یا خروجی grep تأیید شدند. این فقط یک
اعلام صادقانه است که «هیچ یافته‌ی جدیدی در بخش X» همیشه به‌معنای «صفر ریسک»
نیست، بلکه «صفر یافته در محدوده‌ی grep/بررسی انجام‌شده» است.

---

## خلاصه‌ی اجرایی

| دسته‌بندی | تعداد | شماره‌ها |
|---|---|---|
| 🔴 بحرانی | ۴ | G17, G19, G20, G21 |
| 🟠 عملکرد ناقص | ۳ | G1, G7، + یک زیرمورد در G14 (`validateTargetShotCountRange`) |
| 🟡 بدهی فنی | ۱۴ | G3, G4, G5, G6, G8, G12, G14 (بقیه)، G15, G16, G18, G22 + یافته‌ی Tab «خروجی» Studio (بخش ۳) |
| ⚪ صرفاً مستندسازی/تأیید | ۲ | G2, G9 |
| **جمع کل یافته‌های تازه‌ی این ممیزی (بدون شمارش مجدد ۱۲ مورد بخش ۱)** | **۲۲+** | G1–G22 |
| علاوه‌بر‌این: بخش ۱ | ۱۱ از ۱۲ یافته‌ی `pre-unit16-audit.md` رفع‌شده تأیید شد (فقط F5 باز)؛ هر ۱۲ محدودیت README فعلی هم تأیید شد (بدون Drift) | — |

### پیشنهاد ترتیب رفع

**اولویت ۱ — رفع فوری، قبل از هرچیز دیگر (ریسک واقعی داده/گمراهی کاربر):**
1. **G21 (آرشیو پروژه بدون تأیید + بدون Unarchive)** — یا یک دیالوگ تأیید
   واقعی اضافه شود، یا حداقل یک عملیات Unarchive ساخته شود؛ در حالت فعلی این
   خطرناک‌ترین دکمه‌ی کل اپ است (تنها لمس، بدون هشدار، بدون بازگشت).
2. **G19/G20 (Restore/Delete بکاپ بدون تأیید)** — یک `AlertDialog` ساده،
   هم‌الگو با `DeleteProjectDialog` موجود.
3. **G17 (بج «Cleaned · Finalized» دروغ)** — یا واقعاً `finalizePrompt`/
   `checkTokenLimit` را در `OutputDeliveryViewModel` وصل کنید (کار احتمالاً
   کوچک — هر دو تابع کاملاً آماده و تست‌شده‌اند)، یا (حداقل موقت) این بج را
   بردارید تا کاربر گمراه نشود.

**اولویت ۲ — عملکرد ناقص با تجربه‌ی کاربری واقعاً معیوب:**
4. **G7 (ویرایش Asset)** — حداقل یک `onClick` روی کارت‌ها + مسیر Navigation با
   `assetId` اختیاری (دقیقاً الگوی Shot Composer، G9).
5. **G1 (Nav Drawer)** — وصل‌کردن ۷ لینک باقی‌مانده به `Studio(activeProjectId)`
   (هم‌الگو با راه‌حل Backups/Settings) — کم‌هزینه، تأثیر UX زیاد.

**اولویت ۳ — بدهی فنی، قابل موکول‌شدن:**
6. یکدست‌سازی الگوی DI باقی‌مانده (G15, G16) — با همان الگوی
   `database: AppDatabase?` که برای Backups جا افتاده.
7. تصمیم آگاهانه درباره‌ی سرنوشت هر گروه از Rule های یتیم بخش ۷ (G14) — برای
   هرکدام یا وصل شوند، یا رسماً به‌عنوان «فعلاً غیرفعال، طبق تصمیم» مستند
   شوند (نه اینکه بی‌صدا فراموش بمانند).
8. باقی موارد 🟡 (G3–G6, G8, G12, G18, G22) — بدون فوریت، هرکدام در قدم
   مرتبط خودشان (مثلاً هنگام ساخت واقعی File Picker، هردوی G10/G11 با یک قدم
   حل می‌شوند).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد — این ممیزی صرفاً با ابزارهای
پایه (Bash/Grep/Read) و سه Agent از نوع `Explore` (برای موازی‌سازی
جست‌وجوهای گسترده‌ی بخش‌های ۳ تا ۱۰، جهت جلوگیری از پر شدن Context اصلی با
خروجی خام grep) انجام شد.

---

## وضعیت نهایی رفع یافته‌ها (به‌روزرسانی ۲۰۲۶-۰۸-۱۲)

متن اصلی گزارش بالا (تا همین‌جا) بدون تغییر باقی مانده — یک سند تاریخی
معتبر از وضعیت زمان نگارش (۲۰۲۶-۰۷-۲۸) است، دقیقاً طبق همان اصلی که در
`docs/audit/pre-unit16-audit.md` رعایت شد. این بخش صرفاً **وضعیت نهایی**
هر ۲۲ یافته (G1–G22 + زیرمورد `validateTargetShotCountRange` در G14) را،
بر مبنای ۹ ADR نوشته‌شده از زمان این ممیزی (۰۶۰ تا ۰۶۹)، تأیید می‌کند.

**روش:** هر ۲۲ مورد با `grep`/خواندن مستقیم **کد فعلی** (نه فقط متن
ADR ها) راستی‌آزمایی شد — همه‌ی ۲۲ مورد، بدون استثنا. برای هر مورد،
**آخرین** وضعیت واقعی دنبال شد، نه فقط اولین ADR که به آن اشاره می‌کند
(دقیقاً طبق تذکر صریح این قدم: G4/G18 در ADR-067 «موکول شد» ثبت شده
بودند، اما در ADR-069 واقعاً رفع شدند — این الگو در جدول زیر با ذکر هر
دو ADR منعکس شده است).

| # | یافته | وضعیت نهایی | رفع‌شده‌در/ADR مرتبط |
|---|---|---|---|
| G1 | Nav Drawer — ۸ لینک COMING_SOON | ✅ **رفع شد** — ۷ از ۸ لینک به مقصد Studio/AI Breakdown واقعی وصل شدند (`NavDrawer.kt`)؛ `drawer.promptGenerator` عمداً COMING_SOON ماند (ادغام‌شده با Output Delivery، تصمیم پیشین ADR-056) | ADR-061 |
| G2 | `sendToAiConnector` عمداً `TODO()` | ⏸️ **آگاهانه موکول شد** — بدون تغییر، دقیقاً همان کد | ADR-035 (بدون تغییر از زمان ممیزی) |
| G3 | `OverrideEventLogger` هیچ‌جا پیاده/وصل نبود | ✅ **رفع شد** — `RoomOverrideEventLogger` پیاده و از `ValidationViewModel` واقعاً استفاده می‌شود؛ `createOverrideForIssue`/`revokeOverrideAction` هم از `ValidationScreen.kt` واقعی فراخوانی می‌شوند (نه فقط Logger، خودِ مسیر UI کامل شد) | ADR-067 (Logger) → کامل در ADR-068 (UI) |
| G4 | `composeOutput`/`ExportFile` کد مرده | ✅ **رفع شد** — از `OutputDeliveryViewModel.kt` واقعاً صدا زده می‌شود | ADR-067 (موکول شد) → رفع در ADR-069 |
| G5 | مدیریت چند-Outfit فقط Placeholder | ⏸️ **آگاهانه موکول شد** — پیام `manageOutfitsComingSoon` هنوز دقیقاً همان‌جاست؛ تصمیم مستقل صریح (نه یک شکاف فراموش‌شده) | ADR-067 (تصمیم موکول، نه رفع) |
| G6 | `PLACEHOLDER_ACTIVE_PROJECT_ID` ثابت | ✅ **رفع شد** — ثابت کاملاً حذف شد؛ `ActiveProject.kt` مقدار واقعی Resolve می‌کند | ADR-062 |
| G7 | Asset — بدون مسیر ویرایش/`onClick` | ✅ **رفع شد** — `AssetForm(kind, existingAssetId)`، `Card(onClick = onClick)`، هر سه ViewModel پارامتر `existingAssetId` می‌گیرند | ADR-061 |
| G8 | Scene — ویرایش فقط زیرمجموعه‌ی فیلدها | 🟡 **جزئاً رفع شد** — `location` اکنون از داخل `SceneSettingsDialog` قابل ویرایش است (زیرساخت `LocationPickerDialog` از قبل آماده بود، فقط نقطه‌ی ورود تازه اضافه شد)؛ `globalVisualStyle` هنوز فقط نمایشی است، بدون مسیر ویرایش | ADR-067 (فقط `location`) |
| G9 | الگوی مرجع Shot Composer | ⚪ بدون تغییر لازم — همچنان الگوی مرجع معتبر برای G7/G8 | — |
| G10 | «انتخاب تصویر» Settings فقط Snackbar | ✅ **رفع شد** — `onChooseImage` اکنون `ActivityResultContracts.OpenDocument()` واقعی را Launch می‌کند (عمداً نه `GetContent()` — چون `homeScreenImageUri` برخلاف Import، در DataStore Persist می‌شود و باید بین اجراها معتبر بماند؛ `takePersistableUriPermission` هم فراخوانی می‌شود). رندر واقعی این تصویر روی Home هنوز جزو G12 است، نه این یافته | ADR-075 |
| G11 | «Attached References» شات فقط متن | ❌ **یافته‌ی اصلی این ممیزی نادرست بود** — بررسی مستقل بلوپرینت ۰۶/۱۴ نشان داد این طراحی عمدی و کامل است، نه یک Gap: بلوپرینت ۱۴ صریحاً می‌گوید ارجاع به تصویر باید «یک جمله‌ی دستوری کلی» باشد، نه نام/مسیر فایل — «دقیقاً هماهنگ با تصمیم بنیادی قبلی این پروژه (`ReferenceImage`, واحد ۰۶) که هرگز نام/مسیر فایل را در متن پرامپت درج نمی‌کند» (بلوپرینت ۱۴، خط ۳۸). `buildReferenceImageInstruction` (`Renderer.kt`) دقیقاً همین را از قبل پیاده کرده و در `renderBlueprintToText` فراخوانی می‌شود (`ADR-030`). **یادداشت مرتبط اما جدا:** `validateImageReferenceFile`/`validateReferenceImageFile` (Rule های یتیم، G14) واقعاً انتظار یک فایل واقعی را دارند — اما این‌ها زیرساخت Post-MVP هنوز نساخته‌شده‌اند (قبلاً در ADR-064 همین‌طور تصمیم‌گیری و مستند شده‌اند)، نه دلیلی برای رد این تصحیح | ADR-030 (پیاده‌سازی اصلی) + ADR-075 (تصحیح این ممیزی) |
| G12 | ۷ فیلد Persist‌شده‌ی بی‌اثر | 🟡 **جزئاً رفع شد** — فقط `minTouchTargetEnabled` اثر Runtime واقعی گرفت (`CompositionLocal` در `AccessibilityLocals.kt`، مصرف در `Theme.kt`/`App.kt`/`AiStoryBreakdownScreen.kt`)؛ `reducedMotionEnabled` آگاهانه وصل نشد (طبق بررسی مستقل ADR-067: کل اپ هیچ Animation ای ندارد که به آن وصل شود)؛ ۵ فیلد دیگر (`dynamicFontEnabled`, `allowFreeStepJump`, `homeLayoutVariant`, `composerLayoutVariant`, `homeScreenImageUri`) هنوز فقط نوشته می‌شوند، بدون هیچ خواننده | ADR-067 (۱ از ۷) |
| G13 | Pipeline واحد ۱۳ قطع از Output Delivery | ✅ **رفع شد** — همان یافته‌ی G17 است (خودِ گزارش اصلی این را در بخش ۷ اعلام کرده بود)، با همان رفع پوشش داده شد | ADR-060 |
| G14 | Rule های یتیم — زیرمورد 🟠 `validateTargetShotCountRange` + بقیه‌ی گروه‌ها | ✅ زیرمورد 🟠 **رفع شد** (`validateTargetShotCountRange` اکنون در `AiStoryBreakdownViewModel.kt` واقعاً صدا زده می‌شود، به‌جای `coerceIn` بی‌صدا) — 🟡 بقیه‌ی گروه‌ها **آگاهانه مستند/موکول شدند** (نه رفع، نه فراموش‌شده): برخی «منسوخ، کاندید حذف» علامت خوردند، برخی «کاندید رفع نزدیک/آینده»، یک مورد («Prompt Finalization») تصحیح شد که از قبل کاملاً وصل بوده | ADR-064 |
| G15 | `ProjectListViewModel.exportProject` بدون DI | ✅ **رفع شد** — پارامتر تزریقی `database: AppDatabase?` اضافه شد | ADR-063 |
| G16 | `AiStoryBreakdownViewModel.factory` — تزریق همه‌یا‌هیچ | ✅ **رفع شد** — شرط `args.size == 4` با `anyInjected` (منطق OR) جایگزین شد | ADR-063 |
| G17 | 🔴 بج «Cleaned · Finalized» دروغ | ✅ **رفع شد** — `finalizePrompt` واقعاً از `OutputDeliveryViewModel.kt` صدا زده می‌شود؛ بج اکنون شرطی به `cleaningSucceeded` است، نه بدون‌قید‌وشرط | ADR-060 |
| G18 | «Export» فقط مستعار «Copy» | ✅ **رفع شد** — `Intent.ACTION_SEND` واقعی (`OutputDeliveryScreen.kt`)، نه صرفاً Clipboard | ADR-067 (موکول شد) → رفع در ADR-069 |
| G19 | 🔴 Restore بکاپ بدون تأیید | ✅ **رفع شد** — `AlertDialog` واقعی پیش از `restore` (`BackupsScreen.kt`) | ADR-060 |
| G20 | 🔴 Delete بکاپ بدون تأیید | ✅ **رفع شد** — `AlertDialog` واقعی پیش از `delete` | ADR-060 |
| G21 | 🔴 آرشیو پروژه بدون تأیید/بدون Unarchive | ✅ **رفع شد (بخش تأیید)** — دیالوگ تأیید واقعی اضافه شد (`ProjectListSection.kt`)؛ عملیات Unarchive همچنان عمداً ساخته نشده — خودِ متن دیالوگ این محدودیت را صریح اعلام می‌کند | ADR-060 |
| G22 | Delete Scene/Shot/Asset از UI در دسترس نبودند | ✅ **رفع شد** — هر سه اکنون از UI با دیالوگ تأیید واقعی قابل‌دسترسند (`SceneDetailScreen.kt`/`ShotListScreen.kt`/`AssetsScreen.kt`) | ADR-062 |

### جمع‌بندی

| وضعیت | تعداد | موارد |
|---|---|---|
| ✅ کاملاً رفع شد | ۱۵ | G1, G3, G4, G6, G7, G10, G13, G15, G16, G17, G18, G19, G20, G21, G22 |
| 🟡 جزئاً رفع شد | ۳ | G8, G12, G14 |
| ⏸️ آگاهانه موکول شد (تصمیم صریح، نه فراموش‌شده) | ۲ | G2, G5 |
| ⚠️ هنوز باز (بدون تصمیم صریح ثبت‌شده) | ۰ | — |
| ❌ یافته‌ی اصلی ممیزی نادرست بود (نه یک Gap واقعی) | ۱ | G11 |
| ⚪ بدون تغییر لازم | ۱ | G9 |
| **جمع** | **۲۲** | — |

**یادداشت کنار جدول (نه یافته‌ی تازه‌ی این قدم):** محدودیت شناخته‌شده‌ی
«ترجمه‌ی واقعی فارسی پرامپت هنوز وجود ندارد» (که ADR-069 صریحاً به آن
اشاره کرده) از قبل رسماً در README مستند است — نیازی به مستندسازی تازه
نبود. همچنین Tab «خروجی» Studio (`StudioShell.kt`، انشعاب
`else -> StudioTabPlaceholder`) که در بخش ۳ گزارش اصلی (نه یک G-شماره‌ی
مستقل) اشاره شده بود، همچنان دقیقاً همان Placeholder را دارد — بدون
تغییر از زمان نگارش.

**صداقت درباره‌ی این بازبینی:** هر ۲۲ مورد (بدون استثنا) با `grep`/خواندن
مستقیم کد فعلی تأیید شدند — نه صرفاً با اعتماد به متن ADR های ۰۶۰ تا
۰۶۹. برای هر مورد، آخرین وضعیت واقعی (نه اولین ADR مرتبط) دنبال شد.

**افزوده‌ی ۲۰۲۶-۰۸-۱۳:** G10 رفع شد (`ActivityResultContracts.OpenDocument`
واقعی، نه دیگر Snackbar)؛ G11 با بازبینی مستقل بلوپرینت ۰۶/۱۴ تصحیح شد —
یافته‌ی اصلی این ممیزی («Attached References فقط متن است») نادرست بود؛
این طراحی از ابتدا عمدی و کامل بوده. جزئیات کامل هر دو در
`docs/adr/075-settings-image-picker-and-g11-correction.md`.


