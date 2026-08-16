# ADR-096: تکمیل G5 — گسترش انتخاب خودکار Outfit/Expression به timeOfDay/locationType

## زمینه

ADR-095 (قدم قبلی، G5) صادقانه یک محدودیت را مستند کرده بود: `selectOutfitForScene`
فقط `condition.weather` را مقایسه می‌کرد؛ `timeOfDay`/`locationType` در UI
جمع‌آوری و ذخیره می‌شدند اما در انتخاب خودکار هیچ اثری نداشتند. این قدم آن
محدودیت را می‌بندد.

## بررسی مستقل (پیش از نوشتن کد)

- `AssetSelection.kt`: تأیید شد `selectOutfitForScene`/`selectExpressionForScene`
  هر دو دقیقاً فقط `it.condition?.weather == sceneWeather` را چک می‌کردند.
- `docs/blueprints/06-asset-and-continuity-v2.md` («قوانین شرطی برای
  Outfit/Expression»): خودِ «کد مفهومی» بلوپرینت هم فقط `sceneWeather` را
  پارامتر گرفته و فقط weather را مقایسه می‌کند — با اینکه `OutfitCondition`
  از همان بلوپرینت سه فیلد دارد. یعنی بلوپرینت هیچ منطق ترکیب چندفیلدی مشخص
  نکرده؛ این تصمیم عمداً (یا سهواً) به پیاده‌سازی واگذار شده بود.
- `selectExpressionForScene`: با `grep -rn "selectExpressionForScene"` تأیید
  شد **صفر فراخوان‌کننده‌ی واقعی** دارد — فقط در `AssetSelectionTest.kt`
  صدا زده می‌شود. کد مرده‌ی احتمالی، نه یک باگ فعال.
- `CharacterContinuity.kt`: `enforceCharacterContinuity(characters, scene,
  sceneWeather)` — `scene` کامل از قبل پارامتر است؛ `scene.timeOfDay` و
  `scene.location.type` هر دو در همان Scope در دسترس بودند اما استفاده
  نمی‌شدند.
- `SceneModels.kt` (خوانده شد کامل، نه فرض): `Scene.timeOfDay: TimeOfDay`
  فیلد مستقیم است؛ اما `LocationType` از طریق `Scene.location: SceneLocation`
  (یک `data class` جدا با `type`/`description`) در دسترس است — یعنی مسیر درست
  `scene.location.type` است، نه `scene.locationType` مستقیم (که اصلاً چنین
  فیلدی روی `Scene` وجود ندارد).
- `PromptAssembly.kt`: تأیید شد امضای `enforceCharacterContinuity` تغییری
  نمی‌کرد (فقط بدنه‌اش کامل‌تر شد)، پس این فایل نیازی به تغییر نداشت — تصمیم
  خودم، نه فرض کورکورانه‌ی دستور کار.
- `CharacterAssetFormViewModel.kt`: تأیید شد `setOutfitConditionWeather/
  TimeOfDay/LocationType` هر سه با `.name.lowercase()` ذخیره می‌کنند — دقیقاً
  هم‌قرارداد با `sceneWeather` واقعی (`weatherType.name.lowercase()` در
  `PromptAssembly.kt`) و با آنچه این قدم برای `scene.timeOfDay`/
  `scene.location.type` هم اعمال کرد.

## تصمیم — منطق تطبیق: AND روی فیلدهای پرشده، نه امتیازدهی یا اولویت ثابت

بلوپرینت خودش ترجیحی مشخص نکرده بود (بخش «بررسی مستقل» بالا). سه گزینه
بررسی شد:

- **(الف) AND روی هر فیلد پرشده** — انتخاب‌شده.
- **(ب) امتیازدهی (بیشترین شرط منطبق برنده)** — رد شد: یک `condition` معنایش
  «این شرط دقیق باید برقرار باشد» است، نه «نزدیک‌ترین تطابق». با امتیازدهی،
  یک Rain Coat می‌توانست فقط چون `locationType` اتفاقی مطابقت داشت، در هوای
  غیربارانی انتخاب شود — نقض مستقیم انتظار کاربر از یک Rain Coat.
- **(ج) اولویت ثابت weather > timeOfDay > locationType** — رد شد: هیچ مبنایی
  در بلوپرینت یا کد موجود برای این ترتیب خاص نبود؛ یک قرارداد دلخواه اضافه
  می‌کرد بدون توجیه.

**منطق نهایی** (`matchesSceneCondition`، تابع خصوصی مشترک جدید در
`AssetSelection.kt`، هم برای Outfit هم Expression چون هر دو از همان
`OutfitCondition` استفاده می‌کنند): هر فیلد پرشده‌ی `condition` باید دقیقاً با
مقدار متناظر صحنه یکی باشد؛ فیلد `null` در `condition` یعنی «قیدی روی این بعد
نیست» (Wildcard)، نه «صحنه هم باید null باشد». یک `OutfitCondition` غیر-null
اما با هر سه فیلد `null` هم مثل `condition == null` هرگز تطبیق نمی‌دهد —
چنین حالتی عملاً هم از UI نمی‌رسد (`CharacterAssetFormViewModel` این حالت را
به `null` نرمال می‌کند، طبق ADR-095)، اما دفاع صریح در سطح دامنه ایمن‌تر است.

اولویت انتخاب بین چند Outfit منطبق (وقتی بیش از یکی AND را ارضا می‌کند)
همچنان رفتار قبلی `firstOrNull` است — اولین مورد در ترتیب لیست، بدون تغییر.

## پیاده‌سازی

- `AssetSelection.kt`: هر دو تابع `sceneTimeOfDay`/`sceneLocationType` (هر دو
  `String? = null`) بین `sceneWeather` و `manualOverrideId` گرفتند — پیش‌فرض
  `null` یعنی هر فراخوان‌کننده‌ای که این دو را پاس نمی‌دهد، رفتار قبلی
  weather-only را دقیقاً حفظ می‌کند (Backward Compatible، تست‌های موجود بدون
  تغییر سبزند).
- `CharacterContinuity.kt`: `enforceCharacterContinuity` بدون تغییر امضا؛
  بدنه‌اش اکنون `scene.timeOfDay.name.lowercase()` و
  `scene.location.type.name.lowercase()` را استخراج و به
  `selectOutfitForScene` پاس می‌دهد.
- `selectExpressionForScene`: امضا طبق دستور صریح این قدم برای تقارن گسترش
  یافت، اما **به هیچ مصرف‌کننده‌ی واقعی وصل نشد** — همچنان کد مرده‌ی احتمالی؛
  وصل‌کردنش (اگر اصلاً لازم باشد) خارج از Scope همین قدم است.
- `PromptAssembly.kt`: **دست‌نخورده ماند** — امضای `enforceCharacterContinuity`
  تغییر نکرد، پس نیازی به تغییر در این فایل نبود.
- `CharacterAssetFormViewModel.kt`: فقط کامنت `فعلاً فقط weather در
  selectOutfitForScene مقایسه می‌شود` به‌روزرسانی شد تا واقعیت جدید را منعکس
  کند — هیچ منطق این فایل تغییر نکرد (طبق محدودیت صریح دامنه‌ی این قدم).

## یافته‌ی خارج از Scope (گزارش‌شده، دست‌نزده)

`domain/shot/ShotOutfitSelection.kt` (`selectOutfitForShot`) همان محدودیت
weather-only را دارد و منطق خودش را مستقل تکرار می‌کند (نه با فراخوانی
`selectOutfitForScene`). دامنه‌ی مجاز این قدم صریحاً این فایل را شامل نمی‌شد —
دست‌نزده ماند. اگر لازم است، یک قدم مستقل بعدی است.

## تست

- `AssetSelectionTest.kt`: ۴ تست تازه — تطابق فقط بر اساس weather (وقتی
  condition فقط weather دارد)، تطابق سه‌فیلدی کامل، رد یک condition سه‌فیلدی
  با تطابق جزئی (Fallback به Default)، Fallback کامل وقتی هیچ‌چیز منطبق
  نیست.
- `CharacterContinuityTest.kt`: ۱ تست تازه — یک Outfit با
  `condition(timeOfDay, locationType)` (بدون weather) وقتی `scene.timeOfDay`/
  `scene.location.type` منطبق‌اند انتخاب می‌شود، حتی وقتی `sceneWeather`
  اصلاً منطبق نیست.
- `CharacterAssetFormViewModelTest.kt`/`AssetFormFlowTest.kt` (قدم قبلی):
  اجرا شدند، بدون تغییر لازم — سبز.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`AssetSelectionTest`، `CharacterContinuityTest`، `CharacterAssetFormViewModelTest`، `AssetFormFlowTest`) | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۷۷۷ تست، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`)، flaky شناخته‌شده‌ی محیط Sandbox؛ در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
