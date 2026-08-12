# ADR-070: بررسی ریشه‌ای ناپایداری Flake در Test Suite (تشخیصی — بدون تغییر کد محصول)

## زمینه

این کلاس Flake (`SQLiteConnectionPool has been closed` / `attempt to
re-open an already-closed object` / `ComposeTimeoutException` بدون شرط
مشخص) در ADR-044/059/060/062/069 هربار «Flake محیطی بی‌ضرر» رد شده،
بدون بررسی ریشه‌ای واقعی. این قدم **صرفاً تشخیصی است — هیچ کد
`app/src/main/` تغییر نکرد**؛ همه‌ی نتیجه‌گیری‌های پایین از شواهد واقعی
(اجراهای تکراری تازه، grep روی کد واقعی) به‌دست آمده‌اند، نه حدس.

## بازبینی مستقل پیش‌بررسی‌های دستور کار

همه‌ی ۴ ادعای زیرساختی دستور کار با grep مستقیم تأیید شدند، بدون
مغایرت:
- `testOptions.unitTests` فقط `isIncludeAndroidResources = true` دارد؛
  بدون `maxParallelForks`/`forkEvery`/`maxHeapSize`.
- `gradle.properties`: `-Xmx2048m`.
- بدون `robolectric.properties`.
- `AppDatabase.getInstance` — تأیید شد `@Volatile var instance` است.
  اما برخلاف فرض اولیه‌ی دستور کار («تست‌ها درست از In-Memory استفاده
  می‌کنند، پس این Singleton احتمالاً ریشه نیست»)، بررسی مستقل عمیق‌تر
  (پایین) نشان داد این خط تحقیق واقعاً به بیراهه نمی‌رفت — فقط ریشه‌ی
  دقیق جای دیگری بود (بخش «یافته‌ی اصلی» پایین).

## بررسی مستقل: آیا Singleton سراسری `AppDatabase.getInstance` هنوز مسیر فعالی دارد؟

با grep روی هر ۱۰۶ فایل تست: تنها ۱ فایل (`WorkflowViewModelTest.kt` —
یک ViewModel فقط‌DataStore، بدون Room) از `Room.inMemoryDatabaseBuilder`
استفاده نمی‌کند؛ همه‌ی ۱۰۵ فایل دیگر (شامل هر `*FlowTest.kt`/
`*ViewModelTest.kt`) درست عمل می‌کنند. علاوه‌بر‌این، هر ۱۵ تست End-to-End
که `MainScaffold` می‌سازند، همگی صراحتاً `database = <in-memory>` را هم
پاس می‌دهند (تأییدشده با grep) — یعنی رگرسیون G15/G16 (ADR-063، «Silent
Fallback به Singleton سراسری») **کاملاً رفع‌شده باقی مانده**؛ این مسیر
دیگر فعال نیست. ۴ اشاره‌ی باقی‌مانده به `AppDatabase.getInstance` در
تست‌ها همگی کامنت/اثبات-منفی مربوط به همان رفع قبلی‌اند، نه کد فعال.

**نتیجه: فرضیه‌ی Singleton سراسری رد شد.**

## بررسی مستقل: الگوهای State مشترک دیگر

- نام فایل DataStore: هر ۱۵ تست End-to-End از `UUID.randomUUID()`
  استفاده می‌کنند (تأییدشده با grep روی هر خط `dataStoreFileName =`) —
  بدون تصادم.
- هیچ `object`/`companion object` دیگری با `@Volatile var` مشابه
  `AppDatabase` در `main/` پیدا نشد (فقط ۴ `object` UI-محور بی‌ضرر:
  `Color.kt`, `AccessibilityLocals.kt`, `ExtendedColors.kt`,
  `WorkflowPreferencesStore.kt` — هیچ‌کدام State قابل‌جهش سراسری ندارند).

## یافته‌ی اصلی — ریشه‌ی واقعی (اطمینان بالا)

**ریشه یک Race درون‌کلاسی بین `@After { database.close() }` و
Coroutine های ناتمام `viewModelScope` است — نه تداخل بین کلاس‌های تست یا
فشار حافظه.**

خودِ کدبیس این را از قبل تا حدی مستند کرده بود:
`ShotsFlowTest.kt` (خط ۹۴–۱۰۲ و ۱۶۹–۱۷۸) صریحاً می‌گوید: «هر تغییر فیلد
یک Auto-Save ناهمگام مستقل (`ioScope.launch`) صف می‌کند؛ بدون
`composeRule.waitForIdle()`، `database.close()` ممکن است پیش از تکمیل
واقعی آخرین Coroutine صف‌شده اجرا شود» — و `waitForIdle()` به‌عنوان رفع
اضافه شد (طبق `docs/adr/052-...`). اما شواهد این قدم نشان داد **این رفع
ناقص است**:

- `waitForIdle()` فقط Idling Resource های ردیابی‌شده‌ی Compose (کارهای
  متصل به Main Dispatcher/فریم‌های Compose) را می‌سنجد.
- Room برای توابع `suspend` DAO از Executor داخلی خودش استفاده می‌کند
  (نه لزوماً `Dispatchers.Main`) — این کار پس‌زمینه برای `waitForIdle()`
  کاملاً نامرئی است.
- طبق ترتیب اجرای JUnit4 (متد `@After` کاربر **پیش از** خودِ Teardown
  داخلی Rule اجرا می‌شود)، `database.close()` می‌تواند دقیقاً در وسط
  یک کوئری SQLite هنوز درحال‌اجرا رخ دهد.

## شواهد تجربی (اجراهای تازه‌ی این قدم، بدون هیچ تغییر کد)

| اجرا | تنظیم | نتیجه | تست‌های شکست‌خورده |
|---|---|---|---|
| ۱ | پایه (بدون تغییر) | ۲ شکست از ۷۱۱ | `ShotsFlowTest` (۲ مورد متفاوت) |
| ۲ | پایه (بدون تغییر) | ۲ شکست از ۷۱۱ | `AssetFormFlowTest` + `ShotsFlowTest` |
| ۳ | پایه (بدون تغییر) | ۲ شکست از ۷۱۱ | `ShotsFlowTest` + `AppNavigationTest` |
| ۴ | `-Xmx4096m` | ۱ شکست از ۷۱۱ | فقط `ShotsFlowTest` |
| ۵ | `-Xmx4096m` | ۳ شکست از ۷۱۱ | `AssetFormFlowTest` + `ShotsFlowTest` + `AppNavigationTest` |
| ۶ | `maxParallelForks=1` | ۱ شکست از ۷۱۱ | فقط `ShotsFlowTest` |
| ۷ | `maxParallelForks=1` | ۳ شکست از ۷۱۱ | `OutputDeliveryFlowTest` (**همان `SQLiteConnectionPool closed`**) + `ShotsFlowTest` + `AppNavigationTest` |

**تفسیر:**
- **افزایش Heap (اجرای ۴/۵):** بدون روند ثابت (۱ سپس ۳ شکست) — رد
  می‌کند «فشار حافظه» را به‌عنوان علت غالب.
- **`maxParallelForks=1` (اجرای ۶/۷):** حیاتی‌ترین شاهد — با اجرای کاملاً
  Sequential (بدون هیچ هم‌زمانی بین کلاس‌های تست)، دقیقاً همان امضای
  `SQLiteConnectionPool has been closed` باز هم رخ داد (اجرای ۷). این
  **قطعاً** رد می‌کند «تداخل بین Process/JVM های موازی» را به‌عنوان
  مکانیزم اصلی — این یک Race تماماً درون یک کلاس تست تکی است، حتی وقتی
  هیچ تست دیگری هم‌زمان اجرا نمی‌شود.
- هر ۵ تست نام‌برده‌شده در جدول بالا (`ShotsFlowTest`، `AssetFormFlowTest`،
  `AppNavigationTest`، `OutputDeliveryFlowTest`ی «model picker chips»)
  دقیقاً همان‌هایی هستند که یا کلاً فاقد `composeRule.waitForIdle()` پیش
  از `database.close()` بودند، یا — مثل `ShotsFlowTest`/
  `OutputDeliveryFlowTest` — آن را دارند اما طبق تحلیل بالا این محافظ
  ناقص است.

## جمع‌بندی نهایی

**ریشه (اطمینان بالا):** Race بین `tearDown()`'s `database.close()` و
Coroutine های ناتمام `viewModelScope` روی Room Executor داخلی —
`composeRule.waitForIdle()` (رفع نیمه‌کاره‌ی موجود از ADR-052) این کار
پس‌زمینه را نمی‌بیند.

**رفع پیشنهادی (کوچک، کم‌ریسک — اجرا نشد، منتظر تأیید):**
حذف کامل `database.close()` از `tearDown()` هر تست End-to-End
(`Room.inMemoryDatabaseBuilder` است — بدون فایل واقعی روی دیسک؛ خودِ
Garbage Collector آن را با نابودی نمونه‌ی کلاس تست جمع می‌کند؛ هر تست
در `@Before` یک دیتابیس کاملاً تازه می‌سازد، پس چیزی برای «نشتی منابع
بین تست‌ها» باقی نمی‌ماند). این دقیقاً منبع Race را حذف می‌کند (چیزی
برای «زودتر از موعد Close شدن» باقی نمی‌ماند)، بدون نیاز به هیچ تغییر
در کد `app/src/main/`.

**جایگزین/تکمیلی (بزرگ‌تر، نیازمند تصمیم معماری):** به هر ViewModel
مصرف‌شده در این تست‌ها یک `ioScopeOverride` تزریق شود که خودِ تست بتواند
در `tearDown()` صراحتاً `cancel()` و منتظر بماند — این رفتار Async
واقعی صفحات را در تست تغییر می‌دهد (ممکن است حالت‌های Loading واقعی که
این تست‌ها قصد سنجیدنشان را دارند را از بین ببرد)، پس ریسک بیشتری دارد
و نیازمند بررسی مورد‌به‌مورد هر فایل است.

**هیچ‌کدام از این دو در همین قدم اجرا نشد** — طبق دستور صریح کار.

## Skills استفاده‌شده

`systematic-debugging` — برای ساختاردهی این تحقیق (خواندن کامل خطا،
تکرار واقعی، یک فرضیه در هر مرحله، تغییر یک متغیر در هر آزمایش).
