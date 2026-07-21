# ADR-022: تصمیمات پیاده‌سازی واحد ۱۵ — تکمیل `AutoSaveManager`

**تاریخ:** 2026-07-21
**وضعیت:** تکمیل یکی از دو مورد «فقط امضا» باقی‌مانده از ADR-017 (`AutoSaveManager`؛ `BackupManager` قدمی جداست)

## Context

در قدم اول واحد ۱۵ (`docs/adr/017-...md`)، `AutoSaveManager(projectDao, intervalSeconds)` با `suspend fun saveIfDirty(project, isDirty)` فقط به‌عنوان امضای TODO ثبت شد، بدون بدنه‌ی واقعی — چون در آن قدم `ProjectDao` تازه ساخته شده بود و هیچ اتصال دامنه‌ای وجود نداشت. حالا که `ProjectDao`/`ProjectEntity` کامل و پایدارند، این قدم بدنه‌ی واقعی `saveIfDirty` را می‌سازد.

## تصمیم اصلی: بدون Timer/Debounce واقعی در این لایه

کد مفهومی بلوپرینت ۱۵ برای `AutoSaveManager` فقط `saveIfDirty` را تعریف می‌کند — بدون هیچ `delay`/Coroutine Loop واقعی — اما توضیح بالای کد می‌گوید Auto-Save باید «هر ۳۰ ثانیه یا بلافاصله بعد از تغییرات ساختاری مهم» اجرا شود.

**گزینه‌ها:**
1. فقط منطق تصمیم (`saveIfDirty`) در این لایه؛ فراخوانی دوره‌ای به لایه‌ی بالاتر موکول شود.
2. یک تابع کمکی جداگانه (مثلاً `autoSaveLoop(scope, intervalSeconds, getCurrentState)`) با `delay` واقعی در همین کلاس/فایل اضافه شود.

**تصمیم:** گزینه‌ی ۱. دلیل: زمان‌بندی «هر ۳۰ ثانیه یا بعد از تغییر مهم» به Lifecycle واقعی اپ (کِی صفحه در Foreground است، کِی کاربر تغییر می‌دهد) وابسته است — این اطلاعات فقط در لایه‌ی ViewModel/UI (واحد ۱۶) موجود است، که هنوز در این پروژه ساخته نشده. یک Coroutine Loop در لایه‌ی `data/repository/` بدون این اطلاعات یا باید همیشه در پس‌زمینه اجرا شود (نشتی منبع بدون مدیریت Lifecycle) یا باید همان اطلاعات UI را به‌صورت پارامتر از بیرون بگیرد — که عملاً منطق را به همان لایه‌ی بالاتر منتقل می‌کند، فقط با یک لایه‌ی واسط اضافه‌ی بی‌فایده. طبق دستور کار صریح («به احتمال زیاد Timer واقعی به لایه‌ی UI/ViewModel تعلق دارد») و اصل Minimal Scope، تابع کمکی Timer اضافه نشد.

این با الگوی مشابه رد شده در `VersioningRepository` (پرهیز از `GlobalScope.launch`/`runBlocking` برای مسائل Concurrency خارج از محدوده‌ی طبیعی Repository — ADR-018) هم‌راستاست: این لایه فقط I/O و منطق تصمیم را انجام می‌دهد، نه Orchestration زمانی.

## تصمیم فرعی: `intervalSeconds` نگه داشته شد، اما `public`

چون Timer واقعی در این کلاس ساخته نشد، `intervalSeconds` دیگر داخل خودِ کلاس مصرف نمی‌شود. حذف کامل آن از امضا با کد مفهومی بلوپرینت (که آن را در Constructor نگه می‌دارد) مغایرت داشت. به‌جای حذف، `private val` قبلی به `val` عمومی تغییر کرد — یک مقدار پیکربندی که لایه‌ی UI آینده (وقتی خودش Timer را می‌سازد) می‌تواند از همین نمونه بخواند، بدون نیاز به تعریف دوباره‌ی عدد ۳۰ در جای دیگر.

## `saveIfDirty`: خروجی `Result<Boolean>` (نه `Unit` مطابق کد مفهومی خام)

بلوپرینت `saveIfDirty` را بدون مقدار بازگشتی نوشته؛ دستور کار صریحاً `Result<Boolean>` خواست (نشانگر اینکه آیا واقعاً ذخیره انجام شد، برای تست/UI بعدی). این افزایش امضا نسبت به بلوپرینت، خواسته‌ی صریح دستور کار است، نه انحراف مستقل.

- `isDirty=false` → `Result.success(false)` بدون هیچ فراخوانی `projectDao.saveProject` (تست شده با بارگذاری مجدد و تأیید عدم تغییر `lastModified`/`projectName`).
- `isDirty=true` → `projectDao.saveProject(project.copy(lastModified = clock()))` واقعی، سپس `Result.success(true)`؛ شکست SQLite واقعی (در عمل بعید، چون `saveProject` با `REPLACE` هرگز واقعاً Constraint را نقض نمی‌کند) با `runCatching` به `Result.failure` تبدیل می‌شود.

## `clock` تزریق‌پذیر (هم‌الگو با `VersioningRepository`/`OverrideActions`/`LockMechanism`)

`clock: () -> String = ::nowIso8601` دقیقاً همان الگوی موجود در سه فایل دیگر پروژه است — نه یک تصمیم جدید، بازتولید یک قرارداد تثبیت‌شده برای تست‌پذیری بدون `Instant.now()` واقعی.

## تست: Round-Trip واقعی روی Room درون‌حافظه (Robolectric)

سه تست در `AutoSaveManagerTest.kt`، هم‌الگو با `AssetRepositoryTest`/`VersioningRepository` تست‌های قبلی: `isDirty=true` واقعاً ذخیره و `lastModified` را با `clock` تزریقی جایگزین می‌کند؛ `isDirty=false` هیچ تغییری در ردیف موجود ایجاد نمی‌کند؛ `isDirty=true` روی یک `projectId` کاملاً جدید هم درست درج می‌کند (نه فقط Update). هر سه در همان اجرای اول موفق شدند — بدون باگ جدید.

## خارج از Scope (بدون تغییر)

- `BackupManager`، Export/Import — قدم‌های بعدی جداگانه (طبق ADR-017).
- هرگونه ViewModel/UI که واقعاً `isDirty` را تشخیص دهد یا `AutoSaveManager.saveIfDirty` را دوره‌ای فراخوانی کند — واحد ۱۶.
- `ProjectDao`/`ProjectEntity` دست‌نخورده ماندند؛ هیچ نیازی به تغییرشان نبود.

## Consequences

- **آسان می‌شود:** `AutoSaveManager` اکنون یک کلاس واقعی و تست‌شده است که لایه‌ی UI آینده مستقیماً می‌تواند فراخوانی کند؛ منطق تصمیم (چه‌وقت ذخیره شود) از منطق زمان‌بندی (کِی فراخوانی شود) تفکیک شده — تست‌پذیری بدون نیاز به `delay` واقعی یا `TestScope`.
- **بدهی مستند باقی‌مانده:** فراخوانی دوره‌ای واقعی هنوز پیاده نشده (منتظر واحد ۱۶ می‌ماند)؛ `BackupManager`/Export-Import هنوز فقط یادداشت TODO هستند (ADR-017).
