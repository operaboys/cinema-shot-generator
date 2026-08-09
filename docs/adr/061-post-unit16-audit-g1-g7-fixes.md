# ADR-061: رفع دو یافته‌ی 🟠 ممیزی جامع post-Unit16 — G1 (Nav Drawer) و G7 (مسیر ویرایش Asset)

## زمینه

`docs/audit/post-unit16-full-audit.md` دو یافته‌ی 🟠 ثبت کرده بود —
هرکدام مستقل، در یک قدم رفع شدند:

- **G1** — از ۱۱ لینک Nav Drawer، ۸ تا هنوز `DrawerAction.COMING_SOON`
  نشان می‌دادند با اینکه مسیرهای واقعی‌شان (Studio Tabs، AI Story
  Breakdown) از فازهای ۲ تا ۵ همین واحد از قبل موجود بودند.
- **G7** — کارت‌های Asset (Character/Location/Object) هیچ `onClick`ای
  نداشتند؛ هیچ راه واقعی برای ویرایش یک Asset موجود وجود نداشت — هر ذخیره
  همیشه یک `assetId` تصادفی تازه می‌ساخت.

طبق دستور صریح معمار برای این قدم، هر پیش‌بررسی داده‌شده با grep مستقیم
بازبینی شد پیش از هر تغییری؛ نتیجه‌ی این بازبینی‌ها زیر آمده.

## بازبینی مستقل پیش‌فرض‌های دستور کار (طبق دستور صریح)

- `StudioTab` (`StudioTopTabRow.kt:26`) واقعاً فقط ۴ مقدار دارد
  (`STORY, DNA, SCENES, OUTPUT`) — تأیید شد، هیچ Tab «شات‌ها»یی وجود ندارد.
- `AppDestinations.kt`: `Validation`/`OutputDelivery` واقعاً هر دو
  `shotId: String` غیر-nullable دارند (نه nullable) — تأیید شد با خواندن
  کامل فایل.
- الگوی `WorkflowState.projectId` برای Backups/Settings دقیقاً هم‌الگویی
  که در `AppNavHost.kt`'s `composable<Backups>` استفاده شده — تأیید شد
  (`val activeProjectId = workflowState?.projectId`).
- `AssetsScreen.kt`'s `AssetCard` (خط ۲۶۴ در نسخه‌ی پیش از این قدم) واقعاً
  هیچ پارامتر `onClick`ای نداشت — تأیید شد با خواندن کامل فایل.
- `PLACEHOLDER_ACTIVE_PROJECT_ID` (`BottomNavBar.kt:41`) یک رشته‌ی هاردکد
  مستقل از `WorkflowState.projectId` است — تأیید شد؛ عمداً خارج از Scope
  این قدم نگه داشته شد (G7 فقط «افزودن مسیر ویرایش» است، نه «رفع اینکه
  Asset به کدام پروژه تعلق دارد»).

هیچ مغایرتی بین پیش‌بررسی‌های داده‌شده و کد واقعی پیدا نشد — همه‌ی
پیش‌فرض‌ها تأیید شدند.

## تصمیم ۱ — G1: `DrawerAction` گسترش یافت، نه یک الگوی تازه

به‌جای ۴ Callback جداگانه‌ی تازه (یکی برای هر Studio Tab)، یک Callback
پارامتردار واحد اضافه شد: `onNavigateStudio: (initialTab: String) -> Unit`
+ `onNavigateAiBreakdown: () -> Unit`. دلیل: سه‌تا از هفت لینک
(`storyWizard`/`dnaManager`/`scenes`) دقیقاً به همان `Studio(projectId,
initialTab)` می‌روند، فقط با Tab متفاوت — تکرار سه Callback تقریباً یکسان
Scope Creep بود.

**تصمیم فرعی — «شات‌ها» → Tab «صحنه‌ها»:** چون `StudioTab` تب مستقل
«شات‌ها» ندارد (شات‌ها فقط زیرمجموعه‌ی `SceneDetailTab.SHOTS` بعد از
انتخاب یک Scene مشخص در دسترس‌اند)، و Drawer هیچ Scene ای نمی‌داند، تنها
مقصد واقع‌بینانه همان Tab «صحنه‌ها» است — کاربر از آنجا صحنه‌ی مقصد را
خودش انتخاب می‌کند.

**تصمیم فرعی — «اعتبارسنجی»/«تحویل خروجی» → Tab «صحنه‌ها» (همان مقصد):**
چون `Validation`/`OutputDelivery` هر دو `shotId` غیر-nullable لازم دارند
که Drawer نمی‌داند، و بلوپرینت مسیر مشخصی برای «بدون Shot معلوم» تعریف
نکرده، منطقی‌ترین Fallback بدون اختراع یک مسیر تازه، همان Tab «صحنه‌ها»
است (دقیقاً هم‌مقصد با «شات‌ها») — از آنجا کاربر به Scene→Shot→Validation/
Output Delivery واقعی می‌رسد. جایگزین رد‌شده: یک Snackbar «به‌زودی»
برایشان نگه داشته شود — رد شد چون هر دو مسیر واقعی از قبل کاملاً
پیاده‌سازی‌شده‌اند (فاز ۵)، نگه‌داشتن COMING_SOON برایشان دقیقاً همان باگ
G1 (مسیر واقعی موجود، Drawer نمی‌داند) را تکرار می‌کرد.

**تصمیم فرعی — بدون پروژه‌ی فعال → Projects:** چون `Studio(projectId:
String)`/`AiStoryBreakdown(projectId: String)` هر دو `projectId`
غیر-nullable لازم دارند و `WorkflowState.projectId` می‌تواند `null` باشد
(هیچ Session Studio فعالی)، این ۶ لینک بدون پروژه‌ی فعال قابل‌ساخت
نیستند. طبق یکی از دو گزینه‌ی پیشنهادی خودِ دستور کار، کاربر به `Projects`
هدایت می‌شود (نه غیرفعال‌کردن لینک — چون Disabled در این پروژه طبق
ADR-044 ترجیح داده نشده) + یک Snackbar توضیحی تازه (`drawer.noActiveProject`).

**تصمیم فرعی — `drawer.promptGenerator` دست‌نخورده ماند:** طبق ADR-056
این مفهوم در «تحویل خروجی» ادغام شده؛ متن فعلی («تولید پرامپت») گمراه‌کننده
نیست (هنوز مفهوم درستی را توصیف می‌کند)، پس نه برچسب و نه رفتارش تغییر
کرد — طبق تصریح صریح دستور کار که این لینک استثناست.

## تصمیم ۲ — G7: الگوی `existingAssetId` عیناً کپی از `ShotComposer.shotId`

`AssetForm(kind: AssetKind)` گسترش یافت به
`AssetForm(kind: AssetKind, existingAssetId: String? = null)`. هر سه
ViewModel (`CharacterAssetFormViewModel`/`LocationAssetFormViewModel`/
`ObjectAssetFormViewModel`) پارامتر `existingAssetId: String? = null`
گرفتند + یک `init` block که (اگر non-null) از `AssetRepository`'s
`loadXxxAssets(listOf(id))` موجود (نه یک متد load-by-id تازه) بارگذاری و
فرم را با `applyLoadedAsset(...)` پیش‌پر می‌کند. `save()` هر سه به
`existingAssetId ?: idProvider()` تغییر کرد — یعنی حالت ویرایش همان
شناسه‌ی موجود را دوباره ذخیره می‌کند (با `OnConflictStrategy.REPLACE`ی
`AssetDao`، این یک به‌روزرسانی واقعی است، نه رکورد تکراری).

`AssetCard` یک پارامتر `onClick: () -> Unit` گرفت (`Card(onClick =
onClick, ...)`، هم‌الگو دقیق با `SceneCard`ی `ScenesListScreen.kt`).
`AssetsScreen`ی سطح‌بالا یک `onOpenAsset: (AssetKind, String) -> Unit`
گرفت که در `AppNavHost.kt` به
`navController.navigate(AssetForm(kind, existingAssetId = assetId))`
وصل شد.

**تصمیم فرعی — بدون تغییر در `PLACEHOLDER_ACTIVE_PROJECT_ID`:** طبق
بازبینی بالا، این محدودیت شناخته‌شده‌ی جداگانه (ADR-048) عمداً دست‌نخورده
ماند — خارج از Scope دقیق G7.

## تست‌ها

- `NavDrawerNavigationTest.kt` (تازه): ۷ تست End-to-End واقعی — یکی برای
  هر لینک (`storyWizard`→Tab Story، `dnaManager`→Tab DNA، `scenes`→Tab
  Scenes، `shots`→Tab Scenes، `validation`→Tab Scenes،
  `outputDelivery`→Tab Scenes، `aiBreakdown`→صفحه‌ی واقعی AI Story
  Breakdown) + یک تست جداگانه برای مسیر «بدون پروژه‌ی فعال» (Redirect به
  Projects، با اثبات از طریق انتخاب‌شدگی آیتم نوار پایین Projects — نه
  `hasText`ی متن عنوان، چون آن متن با برچسب نوار پایین که همیشه در
  Composition حاضر است Ambiguous بود؛ یافته‌ی دیباگ واقعی این قدم).
- `AssetFormFlowTest.kt`: ۳ تست تازه‌ی Round-Trip کامل (یکی برای هر نوع
  Asset) — ساخت → بازگشت به لیست → کلیک روی کارت → پیش‌پرشدگی واقعی →
  تغییر یک فیلد → ذخیره → همان رکورد به‌روزرسانی‌شده در لیست (نه یک رکورد
  تکراری، با `assertDoesNotExist` روی نام قدیمی).

## نتیجه‌ی Build

`gradle :app:testDebugUnitTest :app:assembleDebug` → ۶۷۲ تست (۶۶۱→۶۷۲، ۱۱
تست جدید)، ۰ Failure، ۰ Error. APK واقعی `assembleDebug` هم ساخته شد. بدون
هیچ Flake ای در این اجرا (Flake شناخته‌شده‌ی `ScenesFlowTest` این بار رخ
نداد).

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
