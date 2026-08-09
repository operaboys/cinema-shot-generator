# ADR-060: رفع ۴ یافته‌ی 🔴 بحرانی ممیزی جامع post-Unit16

## زمینه

`docs/audit/post-unit16-full-audit.md` چهار یافته‌ی 🔴 بحرانی ثبت کرده بود —
هرکدام مستقل، اما در یک قدم رفع شدند چون هرکدام کوچک است:

- **G17** — بج «پاک‌سازی‌شده· نهایی‌شده» در Output Delivery همیشه نمایش داده
  می‌شد، بدون اینکه Pipeline واقعی واحد ۱۳ (Prompt Cleaner + Token Cost
  Calculator) اصلاً اجرا شود.
- **G21** — آرشیو پروژه بدون هیچ دیالوگ تأیید اجرا می‌شد؛ طبق
  `ALLOWED_TRANSITIONS[ARCHIVED] = emptyList()` (`StateMachine.kt`)، این یک
  عملیات کاملاً پایانی/غیرقابل‌بازگشت است.
- **G19** — Restore از بکاپ بدون تأیید، جایگزینی بی‌بازگشت داده‌ی جاری پروژه.
- **G20** — Delete بکاپ بدون تأیید، حذف غیرقابل‌بازگشت.

## تصمیم ۱ — G17: اتصال واقعی واحد ۱۳ به `OutputDeliveryViewModel`

`finalizePrompt(rendered, profile)` (که تا این قدم صفر فراخوان‌کننده در کل
پروژه داشت) اکنون در `regenerate()` بعد از `render()` صدا زده می‌شود —
دقیقاً روی `renderedOutput.formattedPrompt` (نه `preTruncationText`)، چون
طبق کامنت مستندشده‌ی `Renderer.kt` (ADR-026)، مسیر JSON-آگاه Cleaner باید
روی خروجی نهایی (که ممکن است JSON باشد) عمل کند، نه متن مسطح‌نشده‌ی پیش از
Render.

**تصمیم فرعی: تغییر امضای `finalizePrompt`.** این تابع قبلاً
`Pair<RenderedOutput, TokenCheckResult>` برمی‌گرداند و `CleaningReport`
میانی را دور می‌ریخت. چون `validateCompressionRatio` (یکی از دو Rule
دیگری که این قدم باید وصل می‌کرد) به یک `CleaningReport` واقعی نیاز دارد،
یک `data class FinalizationResult(rendered, tokenCheck, cleaningReport)`
جایگزین `Pair` شد. این یک Breaking Change واقعی نیست چون این تابع تا این
لحظه هیچ فراخوان‌کننده‌ای در `ui/`/`data/` نداشت (فقط تست واحدش) — فقط
`PromptFinalizationPipelineTest.kt` به‌روزرسانی شد.

**تصمیم فرعی: `cleanJsonPromptValues` هم یک `CleaningReport` تجمیعی
برمی‌گرداند.** قبلاً هر فیلد JSON را جدا Clean می‌کرد و `CleaningReport`
هر فیلد را دور می‌ریخت (`.first`). اکنون شمارنده‌های هر فیلد جمع می‌شوند
(`conflictsDetected`, `redundancyRemoved`, `stopWordsRemoved`) و
`compressionRatio` از طول کل رشته‌ی JSON (نه مجموع طول فیلدها) محاسبه
می‌شود — همان چیزی که واقعاً به کاربر/مدل تحویل داده می‌شود.

**تصمیم فرعی: بج فقط شرطی نمایش داده می‌شود.** `finalizePrompt` داخل
`runCatching` صدا زده می‌شود (محافظ دفاعی، نه انتظار خطای واقعی) — نتیجه
در یک فیلد تازه‌ی `OutputDeliveryState.Ready.cleaningSucceeded: Boolean`
ذخیره می‌شود؛ `OutputPreviewCard` بج را فقط وقتی این `true` است رندر
می‌کند. اگر Cleaning شکست بخورد، صفحه به متن خام `render()` Fallback
می‌کند (نه Crash کل صفحه).

**تصمیم فرعی: خط «هزینه‌ی توکن» واقعی شد.** قبلاً همیشه فقط `maxTokens`
ثابت پروفایل را نشان می‌داد (بدون هیچ عدد Estimated واقعی). چون
`TokenCheckResult` اکنون در `state` موجود است، این خط به
`estimatedTokens/maxTokens` تغییر کرد (با رنگ خطا وقتی `withinLimit=false`)
— یک بهبود کوچک و مستقیماً مرتبط، نه Scope Creep.

`validateConflictsResolved`/`validateCompressionRatio` (هر دو
`PromptCleaner.kt`) به `warnings` موجود اضافه شدند؛ `tokenCheck.warning`
هم (اگر موجود) به یک `ValidationIssue` تازه تبدیل و اضافه شد.

**تأیید صریح مورد ۵ دستور کار:** grep تأیید کرد `render()` همیشه
`RenderedOutput(..., language = "en")` هاردکد می‌سازد (`Renderer.kt:161`)
— فرض «فقط انگلیسی» `PromptCleaner.kt` هنوز برقرار است، بدون محدودیت
جدید.

## تصمیم ۲ — G21: دیالوگ تأیید آرشیو، هم‌الگو با Rename/Delete موجود

`ProjectListSection.kt` از قبل الگوی `renameTarget`/`deleteTarget`
(`ProjectSummary?` نگه‌داشته‌شده تا کلیک واقعی، سپس `AlertDialog`) داشت.
`archiveTarget` دقیقاً همان الگو گرفت — `onArchive` دیگر مستقیم
`viewModel.archiveProject` را صدا نمی‌زند، فقط `archiveTarget` را ست
می‌کند؛ `ArchiveProjectDialog` (کپی دقیق ساختار `DeleteProjectDialog`) با
کلیدهای تازه‌ی `project.archive.title/message/confirm/cancel` اضافه شد.
متن پیام صریحاً به غیرقابل‌بازگشت‌بودن اشاره می‌کند (طبق دستور کار).

## تصمیم ۳ — G19/G20: دیالوگ‌های تأیید Restore/Delete بکاپ

همان الگوی بالا، در `BackupsScreen.kt`: `restoreTarget`/`deleteTarget`
(`BackupSummary?`) + دو `AlertDialog` تازه (`RestoreBackupDialog`/
`DeleteBackupDialog`) با کلیدهای `backups.restoreConfirm*`/
`backups.deleteConfirm*`. دکمه‌های موجود ردیف بکاپ (`onRestore`/`onDelete`)
دیگر مستقیم `viewModel.restore`/`viewModel.delete` را صدا نمی‌زنند.

## تصمیم ۴ — الگوی مشترک: هیچ الگوی جدید اختراع نشد

هر سه دیالوگ تازه (Archive/Restore/Delete بکاپ) دقیقاً از الگوی موجود
`AlertDialog` با `onConfirm`/`onDismiss` تبعیت می‌کنند (طبق پیش‌بررسی صریح
معمار — `RenameProjectDialog`/`DeleteProjectDialog` در `HomeScreen.kt`/
`ProjectListSection.kt`) — بدون Composable مشترک جدید (چون هر سه فقط
عنوان/پیام متفاوت دارند، استخراج یک Composable مشترک برای سه خط تکراری
Scope Creep غیرضروری بود).

## تست‌ها

- `PromptFinalizationPipelineTest.kt`: تست موجود به‌روزرسانی شد
  (`FinalizationResult` به‌جای `Pair`) + یک تست تازه برای `CleaningReport`
  تجمیعی JSON.
- `OutputDeliveryViewModelTest.kt` (تازه، مستقیم روی ViewModel نه از طریق
  کل Compose Tree — سریع‌تر برای این سناریوی خاص): تضاد آب‌وهوایی واقعی
  حل می‌شود + بج فقط با موفقیت واقعی نمایش داده می‌شود؛ یک مدل JSON
  (`veo_3_1`) با متن طولانی واقعاً از سقف توکن عبور می‌کند (یافته‌ی دیباگ:
  پروفایل‌های غیر-JSON مثل SD3 به `maxPromptLength` کوتاه می‌شوند، و طبق
  Heuristic خودِ پروژه `maxTokens ≈ maxPromptLength/4`، متن کوتاه‌شده تقریباً
  همیشه نزدیک سقف می‌ماند نه بالاتر — JSON که هرگز کوتاه نمی‌شود تنها راه
  قابل‌اتکای تست این سناریو بود).
  - یافته‌ی دیباگ دوم: `SceneEntity` یک FK واقعی به `ProjectEntity` دارد —
    بدون ساخت واقعی یک Project اول، `saveScene`/`saveShot` بی‌صدا با
    `SQLiteConstraintException` شکست می‌خوردند (Result چک نشده بود).
- `HomeProjectsStudioFlowTest.kt`: تست تازه‌ی Archive — Cancel (بدون اثر) +
  Confirm (فراخوان واقعی، قابل‌مشاهده از طریق Snackbar واقعی
  `HomeScreen.kt` که از قبل به `lastActionMessage` وصل است — Poll مستقیم
  یک StateFlow خام بدون معادل UI قابل‌اتکا نبود، طبق یافته‌ی دیباگ).
- `BackupsFlowTest.kt`: دو تست موجود (Restore/Delete) برای عبور از دیالوگ
  تازه به‌روزرسانی شدند + دو تست تازه برای مسیر Cancel هرکدام.

## نتیجه‌ی Build

`gradle :app:testDebugUnitTest :app:assembleDebug` → ۶۶۱ تست (۶۵۵→۶۶۱)، ۶۶۰
موفق. یک شکست (`ScenesFlowTest > connecting a location asset from the
library saves it and shows it in the Overview`، `SQLiteConnectionPool`)
در دو اجرای مجزای کامل Test Suite به‌طور یکسان تکرار شد، اما در دو اجرای
مستقل و مجزا (فقط همان یک کلاس تست، بدون بقیه‌ی Suite) هر دو بار ۱۰۰٪
موفق بود — یک Flake شناخته‌شده‌ی محیط Robolectric (اتصال SQLite تحت بار
کامل Suite)، تأییدشده بدون ارتباط با کد این قدم (این تست هیچ فایل
تغییریافته‌ی این قدم — Output Delivery/Backups/Project Archive/Prompt
Finalization — را لمس نمی‌کند). APK واقعی `assembleDebug` هم ساخته شد.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
