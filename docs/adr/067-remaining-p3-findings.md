# ADR-067: باقی‌مانده‌ی اولویت ۳ بند ۸ — تصمیمات آگاهانه (G3، G4، G5، G8، G12، G18)

## زمینه

این قدم آخرین بند فهرست رسمی ممیزی جامع post-Unit16
(`docs/audit/post-unit16-full-audit.md`، «پیشنهاد ترتیب رفع»، اولویت ۳
بند ۸) است — ۶ یافته‌ی 🟡 باقی‌مانده: G3، G4، G5، G8، G12، G18. طبق
دستور کار، هرکدام رفتار متفاوتی گرفت: G3/G8/بخشی از G12 واقعاً رفع
شدند؛ G4/G18 و G5 (تصمیم مستقل) به این سند مستند و موکول شدند.

## بازبینی مستقل پیش‌بررسی‌های دستور کار (طبق دستور صریح)

- **G3 — مغایرت واقعی پیدا شد:** پیش‌بررسی فرض کرده بود یک محل واقعی در
  UI/Repository وجود دارد که `createOverride`/`revokeOverride` را با
  `OverrideEventLogger.NoOp` صدا می‌زند و فقط باید Logger واقعی جایگزین
  شود. با `grep -rln "createOverride(\|HumanOverride" app/src/main/java/.../ui/
  app/src/main/java/.../data/` **صفر نتیجه** به‌دست آمد — یعنی کل مفهوم
  «Human Override» واحد ۰۱ (نه سازوکار ساده‌تر `SourcedSettings
  source="override"` شات‌ها که چیز دیگری است) هرگز به هیچ اقدام واقعی
  کاربر در UI وصل نشده — نه فقط اینکه Logger اش NoOp است. جزئیات تصمیم
  در بخش G3 پایین.
- **G12 (reducedMotionEnabled) — مغایرت واقعی پیدا شد:** پیش‌بررسی
  می‌گفت «اگر AnimatedVisibility/animateContentSize استفاده شده، وصل
  کن». با `grep -rln "AnimatedVisibility\|animateContentSize\|
  animate.*AsState\|tween(\|Crossfade" app/src/main/java/.../ui/`
  **صفر نتیجه واقعی** به‌دست آمد (تنها تطبیق، substring «tween» داخل
  کلمه‌ی «between» بود) — یعنی این اپ اصلاً هیچ انیمیشن/Motion ای در
  هیچ‌جای UI ندارد. طبق دستور صریح خودِ کار («اگر بله ... اگر نه، ...
  نامشخص»)، این یافته وصل نشد — جزئیات پایین.
- **G8 — یافته‌ی مثبت (نه مغایرت):** با بررسی کامل `SceneDetailScreen.kt`
  کشف شد `location` از قبل یک مسیر ویرایش واقعی دارد — `LocationPickerDialog`
  (همان کامپوننت فاز ۴ که دستور کار به آن اشاره کرده بود) + `connectLocationAsset`
  — فقط از یک Quick Action جدا («اتصال به کتابخانه») در تب Overview، نه از
  داخل `SceneSettingsDialog`. یعنی زیرساخت لازم از قبل کاملاً آماده بود؛
  این قدم فقط یک نقطه‌ی ورود تازه به همان مکانیزم موجود اضافه کرد.

هیچ مغایرت دیگری پیدا نشد.

---

## بخش الف — G3: رفع واقعی، با یک محدودیت صادقانه

`RoomOverrideEventLogger` (`data/repository/RoomOverrideEventLogger.kt`،
تازه) پیاده‌سازی واقعی `OverrideEventLogger` است — از همان `EventLogDao`/
`EventLogEntity` موجود (واحد ۱۵، ساخته‌شده برای `StateVersioningEventLogger`)
بازاستفاده می‌کند، بدون هیچ Schema تازه.

**تصمیم مستقل — CoroutineScope تزریقی:** `OverrideEventLogger.log(event)`
یک تابع synchronous است (نه suspend) — این قدم عمداً این امضای موجود را
تغییر نداد (خارج از دستور کار صریح). چون `EventLogDao.logEvent` واقعاً
suspend است، `RoomOverrideEventLogger` یک `CoroutineScope` تزریقی
می‌گیرد و نوشتن را روی آن `launch` می‌کند — هم‌الگو با انضباط
`ioScopeOverride` سراسری این پروژه، نه بلاک‌کردن Thread فراخوان.

**محدودیت صادقانه (طبق بازبینی مستقل بالا):** این پیاده‌سازی اکنون
واقعی، تست‌شده، و آماده است — اما **در حال حاضر هیچ کد تولیدی در
`ui/`/`data/` آن را استفاده نمی‌کند**، چون هیچ‌جای اپ اصلاً
`createOverride`/`revokeOverride` (مفهوم واحد ۰۱ Human Override) را
فراخوانی نمی‌کند. یعنی رفع این قدم، مشکل «Logger بی‌اثر» را حل کرد، اما
مشکل بزرگ‌تر و پنهان‌تر («کل قابلیت Human Override هرگز به UI وصل
نشده») هنوز باز است — این یک یافته‌ی تازه و مهم‌تر از G3 اصلی است که
**برای تصمیم مشترک با معمار پروژه علامت‌گذاری می‌شود**: آیا وصل‌کردن
واقعی Human Override به یک اقدام کاربر (مثلاً یک دکمه‌ی «تجاوز از این
هشدار» روی صفحه‌ی Validation) یک قدم آینده‌ی مستقل باشد؟

---

## بخش ب — G12: `minTouchTargetEnabled` رفع شد، `reducedMotionEnabled` قابل‌وصل‌نبود

**`minTouchTargetEnabled`:** `ui/theme/AccessibilityLocals.kt` (تازه) —
یک `CompositionLocal` هم‌الگو دقیق با `ExtendedColors.kt` (چون این
مقدار باید به Composable های عمیقاً تودرتو مثل `PhaseCircle` برسد، نه
فقط یک لایه پایین‌تر از فراخوان — همان دلیل مستند خودِ `Theme.kt` برای
انتخاب CompositionLocal به‌جای تزریق مستقیم). `CinemaShotGeneratorTheme`
پارامتر تازه‌ی `minTouchTargetEnabled: Boolean = false` گرفت (پیش‌فرض
false — بدون تغییر رفتار برای فراخوان‌های موجود/تست‌ها)؛ `App.kt` مقدار
واقعی را از `workflowViewModel.minTouchTargetEnabled` می‌خواند و پاس
می‌دهد.

**اعمال واقعی:** با grep در کل `ui/` روی `.clickable`/`.size(` تنها یک
هدف واقعی و مشخص پیدا شد که کوچک‌تر از ۴۸dp توصیه‌شده است:
`AiStoryBreakdownScreen.kt`ی `PhaseCircle` (دایره‌های شماره‌ی مرحله، ۳۲dp).
`Modifier.minTouchTargetIfEnabled()` (تابع کمکی تازه) روی Box بیرونی
(نه شکل بصری ۳۲dp خودش) اعمال شد — وقتی سوییچ فعال است، ناحیه‌ی واقعی
لمس حداقل ۴۸dp می‌شود، بدون تغییر ظاهر بصری دایره.

**`reducedMotionEnabled` — بدون وصل، طبق یافته‌ی بالا:** چون هیچ
انیمیشنی در کل اپ وجود ندارد، چیزی برای «کم‌کردن» وجود ندارد. اختراع یک
انیمیشن تازه فقط برای این‌که این سوییچ چیزی برای غیرفعال‌کردن داشته
باشد، دقیقاً برعکس منطق درست بود (Motion باید اول به‌عنوان یک ویژگی
واقعی UI اضافه شود، بعد این سوییچ برایش معنا پیدا کند). **بدون اقدام —
هم‌ردیف با ۵ سوییچ دیگر (`dynamicFontEnabled`، `allowFreeStepJump`،
`homeLayoutVariant`، `composerLayoutVariant`، `homeScreenImageUri`) که
طبق دستور صریح کار دست‌نخورده ماندند.**

---

## بخش ج — G8: `location` از داخل SceneSettingsDialog، `globalVisualStyle` موکول شد

`SceneSettingsDialog` (`ui/scenes/SceneDetailScreen.kt`) یک ردیف تازه
گرفت: نام Location متصل‌شده‌ی فعلی (یا پیام «هنوز وصل نشده») + دکمه‌ی
«اتصال به کتابخانه» که همان `LocationPickerDialog`/`connectLocationAsset`
**موجود** (فاز ۴ واحد ۱۶) را فراخوانی می‌کند.

**تصمیم مستقل — ذخیره‌ی فوری، نه منتظر دکمه‌ی «ذخیره» عمومی Dialog:**
`connectLocationAsset` از قبل یک عملیات مستقل و فوری بود (Quick Action
موجود همین رفتار را دارد)؛ تغییر آن به بخشی از تراکنش `saveSceneSettings`
یک Refactor رفتاری موجود بود که خارج از Scope این قدم بود — رفتار
دقیقاً همان چیزی ماند که از قبل برای Quick Action وجود داشت، فقط یک
نقطه‌ی ورود تازه (از داخل Settings) به همان رفتار اضافه شد.

**`globalVisualStyle` موکول شد:** `GlobalVisualStyleRef(source: String
= "project_dna", override: String? = null)` یک جفت source/override
است (هم‌الگو با `SourcedSettings` شات‌ها) — اما معنای دقیق مقدار
`override` (کدام واژگان؟ یکی از مقادیر enum `VisualStyle` واحد ۰۲ به
شکل String؟ یا متن آزاد؟) در هیچ‌جای کد/سند طراحی روشن نشده. وصل‌کردن
آن نیازمند یک تصمیم طراحی تازه (نه فقط یک فیلد ساده مثل `title`) بود —
دقیقاً طبق اجازه‌ی صریح دستور کار («اگر واقعاً پیچیده بود، موکول کن»)،
برای یک قدم آینده مستند و موکول شد.

---

## بخش د — G4 + G18: Export واقعی فایل، یک قابلیت محصولی Post-MVP واقعی

`OutputComposer.composeOutput`/`ExportFile` (دامنه، کامل و تست‌شده) و
دکمه‌ی «Export» صفحه‌ی Output Delivery (که فعلاً عملاً مستعار «Copy» است
— هر دو فقط `clipboardManager.setText` صدا می‌زنند) هر دو تأیید شدند.
زیرساخت لازم برای Export واقعی فایل (`ExportFile(filename, content,
mimeType)`) از قبل در دامنه آماده است؛ آنچه غایب است فقط لایه‌ی
Android-محور: `Intent.ACTION_SEND` (اشتراک‌گذاری مستقیم) یا
`MediaStore`/`ACTION_CREATE_DOCUMENT` (نوشتن واقعی روی حافظه‌ی خارجی).

**تصمیم:** طبق ارزیابی خودِ دستور کار، این یک قابلیت محصولی Post-MVP
واقعی است — نیازمند طراحی UX (کدام مسیر: Share Sheet سیستم یا انتخاب
مسیر ذخیره؟) به‌علاوه‌ی زیرساخت Android جدید (اولین `Intent`/`MediaStore`
کل پروژه)، نه یک رفع کوچک. **برای یک قدم مستقل آینده مستند و موکول شد
— نه در این قدم.**

---

## بخش ه — G5: چند-Outfit — تصمیم مستقل، موکول شد (نه به‌دلیل بزرگی G4/G18، بلکه دلیل جداگانه)

با خواندن کامل `CharacterAssetFormViewModel.kt` تأیید شد وضعیت فعلی
عمیق‌تر از یک دکمه‌ی Placeholder ساده است: `_outfitName`/`_outfitDescription`
دو `MutableStateFlow<String>` **مجزا** هستند (نه یک لیست)؛ `save()`
همیشه دقیقاً یک `Outfit` می‌سازد. رفع واقعی چند-Outfit نیازمند:

1. تبدیل مدل حالت از دو رشته‌ی مجزا به یک `MutableStateFlow<List<Outfit>>`
   واقعی (تغییر ساختاری، نه افزودنی).
2. `addOutfit`/`removeOutfit`/انتخاب Outfit پیش‌فرض (Rule 5: دقیقاً یک
   `isDefault=true` باید همیشه برقرار بماند).
3. بازنویسی `loadExisting()` (اکنون فقط `default` را می‌خواند) برای
   بارگذاری کل لیست.
4. UI لیست تازه (ردیف‌های Add/Remove) — هم‌الگو مفهومی با `actionSounds`
   (Sound Tab، فاز ۴)، اما آن خودش یک قدم کاملاً مستقل بود، نه یک
   افزودن سریع.

**تصمیم من:** برخلاف ارزیابی اولیه‌ی دستور کار («کم‌هزینه‌تر از
G4/G18»)، بعد از بررسی کد واقعی این یک Refactor ساختاری واقعی روی
ViewModel + UI لیست تازه است — نه فقط چند خط. قبول این کار در همین
قدم (که از قبل شامل G3/G8/G12 است) ریسک عجله/کیفیت پایین روی همه‌ی
بخش‌ها را بالا می‌برد. **موکول شد** — اما (برخلاف G4/G18) به‌عنوان
**کوچک‌تر و مستقل‌تر، اولین کاندید طبیعی قدم اختصاصی بعدی** ثبت می‌شود،
نه هم‌سطح G4/G18 از نظر اندازه.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
