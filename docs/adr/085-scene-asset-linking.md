# ADR-085: اتصال واقعی Asset↔Scene برای Tab «دارایی‌ها» (یافته‌ی #۱۱ appendix ADR-081)

## زمینه

طبق appendix ADR-081 («Tab Assets — کارت‌های asset پیوندشده ... **پیاده‌نشده
به‌طور کامل**» — `docs/adr/081-appendix-mockup-audit.md` خط ۶۲)،
`SceneDetailTab.ASSETS` تا این قدم همیشه یک `EmptyTabState` ثابت بود، صرف‌نظر
از این‌که واقعاً Asset ای به صحنه وصل شده باشد یا نه — هیچ رابطه‌ی ذخیره‌شده‌ای
بین Scene و Character/Object Asset وجود نداشت (فقط `locationAssetId`، مخصوص
Location، از ADR-038). خودِ متن empty-state موجود («دارایی‌های این صحنه از
طریق شات‌ها متصل می‌شوند») هم یک ادعای نادرست بود که هیچ ADR ای پشتش نبود —
appendix صریحاً تأیید کرد (`grep` در `docs/adr/*.md`) که هیچ‌جا این جمله
تصمیم‌گیری نشده بود.

## ۱. یافته‌ی کلیدی — چرا این کار Migration رسمی Room نمی‌خواست

`SceneEntity.kt` هیچ ستون مجزای SQL ندارد؛ فقط یک `sceneDataJson: String`
(کل Scene به‌صورت JSON سریالایز‌شده). `SceneDto.kt` (خط ۲۵-۲۹) قبلاً دقیقاً
همین الگو را برای `locationAssetId` ثبت کرده بود (ADR-038، بدون Migration
رسمی). با اضافه‌کردن `linkedAssetIds: List<String> = emptyList()` (هم به
`domain.scene.Scene` هم به `SceneDto`، با `Json { ignoreUnknownKeys = true }`
موجود در `SceneRepository`)، رفتار kotlinx.serialization برای کلید غایب در
JSON قدیمی به‌طور خودکار مقدار پیش‌فرض را برمی‌گرداند — بدون خطا. این با یک
تست واقعی اثبات شد (نه فقط فرض نظری)،
`SceneMappersTest.decoding an old SceneDataJson without the linkedAssetIds key succeeds with an empty default`:
یک رشته‌ی JSON دستی، دقیقاً بدون کلید `linkedAssetIds` (شبیه یک ردیف واقعی
Room ذخیره‌شده پیش از این قدم)، با موفقیت Decode می‌شود.

## ۲. مرجع دقیق mockup (`docs/design/Cinema Studio.html`، `sc.assets`/`linkedAssets`، آفست ۲٬۰۰۶٬۰۸۵ و ۲٬۰۹۵٬۲۳۳)

نمونه‌ی واقعی داده‌ی mockup:
```js
const linkedAssets = [
  { icon: 'person', name: 'Elias', meta: 'CHARACTER · MAIN', lock: 'FULL' },
  { icon: 'forest', name: 'Northern Forest', meta: 'LOCATION', lock: 'STYLE' },
  { icon: 'radio', name: 'Signal Receiver', meta: 'OBJECT · PERSONAL_PROP', lock: 'FORM' },
];
```
این نمونه سه یافته‌ی تعیین‌کننده دارد:
1. **یک فهرست واحد و ناهمگون** — هر سه نوع Asset (Character/Location/Object) در یک آرایه، نه سه فهرست جدا.
2. **`lock` دقیقاً `continuityLockLevel` موجود هر نوع است** — مقادیر نمونه (`FULL`/`STYLE`/`FORM`) با enum های `CharacterContinuityLevel.FULL`/`LocationContinuityLevel.STYLE`/`PropContinuityLevel.FORM` (`domain/asset/AssetModels.kt`) دقیقاً یکی‌اند — نه یک مفهوم تازه.
3. **`icon` تابعی از نوع Asset است، نه یک فیلد تازه روی دامنه** — `person`/`forest`/`radio` دقیقاً با سه نوع `AssetType` منطبق‌اند، نه مقداری per-instance.

## ۳. تصمیم — دکمه‌ی «افزودن» یک Dialog باز می‌کند، نه ModalBottomSheet

mockup این دکمه (`act.openSheet`، `x.k34`) یک ModalBottomSheet باز می‌کرد.
طبق appendix ADR-081 («عناصر مشترک» — `ModalBottomSheet` در کل کدبیس
پیاده‌سازی نشده؛ هرجا mockup Sheet باز می‌کرد، این پروژه با یک Dialog درون‌خطی
جایگزین کرده) و پیش‌زمینه‌ی کد موجود (`LocationPickerDialog` در همین فایل —
`SceneDetailScreen.kt` — که دقیقاً همین الگو را برای اتصال Location از قبل
دارد)، `AssetLinkPickerDialog` تازه هم همان الگو را دنبال می‌کند: یک
`AlertDialog` با فهرست Asset های متصل‌نشده‌ی پروژه.

**تصمیم مستقل جانبی**: متن دقیق `x.k34` mockup («اتصال Asset (BottomSheet)»)
عمداً به‌صورت کلمه‌به‌کلمه استفاده نشد — این رشته، برخلاف بیشتر مقادیر
دیکشنری mockup، شکل یک یادداشت پیاده‌سازی می‌دهد (شبیه `k33`/`k41` که
صریحاً یادداشت‌های «آینده»/جزئیات فنی‌اند، نه متن واقعی UI)، و نمایش تحت‌اللفظی
«(BottomSheet)» به کاربر واقعی گمراه‌کننده بود (این پروژه اصلاً BottomSheet
ندارد). به‌جایش یک برچسب تمیز نوشته شد: «افزودن Asset» / «Add Asset».

## ۴. تصمیم — حذف اتصال (`unlinkAsset`) اضافه شد، هرچند mockup آن را نشان نداده

mockup فقط دکمه‌ی «افزودن» را نشان می‌دهد؛ هیچ اکشن Remove ای در طراحی
دیده نمی‌شود. اما بدون یک راه واقعی برای رفع یک اتصال اشتباه، این ویژگی
یک‌طرفه و عملاً ناقص می‌بود (یک Asset اشتباه انتخاب‌شده برای همیشه در Scene
می‌ماند) — یک نیاز عملکردی بدیهی، نه تفسیر آزاد از دستور کار. یک `IconButton`
(آیکون Close) روی هر کارت متصل‌شده اضافه شد؛ `SceneDetailViewModel.unlinkAsset`
از همان `updateAndSave` خصوصی موجود استفاده می‌کند (بدون تکرار منطق ذخیره).

## ۵. Badge شمارشی Tab «دارایی‌ها» (بستن یافته‌ی #۶ appendix)

ADR-083 عمداً Tab ASSETS را بدون Badge رها کرد («چون هیچ منطق شمارش واقعی
پشت آن نیست ... یک Badge که همیشه صفر است»). حالا که اتصال واقعی پیاده شده،
Badge اضافه شد — هم‌الگو دقیق با Badge Tab SHOTS (ADR-083): برخلاف `shotCount`
(که نیازمند یک StateFlow جدا از `ShotRepository` بود، چون شمار Shot از
Scene خودش نمی‌آمد)، اینجا نیازی به Flow جدا نبود — `scene.linkedAssetIds.size`
مستقیماً از StateFlow موجود `scene` خوانده می‌شود.

## پیاده‌سازی

- **`domain/scene/SceneModels.kt`**: `Scene.linkedAssetIds: List<String> = emptyList()`.
- **`data/repository/SceneDto.kt`**: فیلد متناظر در DTO (پیش‌فرض یکسان).
- **`data/repository/SceneMappers.kt`**: نگاشت دوطرفه‌ی `toDomain()`/`toDto()`.
- **`ui/assets/AssetLabels.kt`**: `assetTypeLabel(AssetType, Language)` تازه —
  اولین مصرف‌کننده‌ی این Tab؛ بقیه‌ی Label های لازم (`characterTierLabel`،
  `objectSubtypeLabel`، `characterContinuityLevelLabel`،
  `locationContinuityLevelLabel`، `propContinuityLevelLabel`) از قبل موجود
  بودند و بازاستفاده شدند — هیچ رشته‌ی تازه‌ای اختراع نشد.
- **`ui/scenes/SceneDetailViewModel.kt`**: `characterAssets`/`objectAssets`
  (StateFlow، هم‌الگو با `locationAssets` موجود) + `linkAsset`/`unlinkAsset`.
- **`ui/scenes/SceneDetailScreen.kt`**: `AssetsTab` (لیست کارت‌های متصل +
  دکمه‌ی افزودن)، `LinkedAssetCard`، `AssetLinkPickerDialog`، Badge روی Tab.

## تست

- `SceneMappersTest.kt` (+۲ تست): round-trip کامل `linkedAssetIds`
  (پرشده/خالی)؛ Decode موفق JSON قدیمی بدون این کلید (اثبات مستقیم عدم‌نیاز
  به Migration).
- `ScenesFlowTest.kt` (+۱ تست End-to-End واقعی، Character/Location/Object
  هر سه Seed شده): Tab خالی + Badge «۰» → کلیک «افزودن» → هر سه نوع Asset در
  دیالوگ قابل‌انتخاب → انتخاب Character → کارت واقعی + Badge «۱» → حذف اتصال
  → بازگشت به حالت خالی + Badge «۰».

## راستی‌آزمایی نهایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `ScenesFlowTest.kt` + `SceneMappersTest.kt` | **۹ تست، ۰ شکست** |
| `gradle :app:testDebugUnitTest` (کل Suite، `--rerun`) | **۷۴۲ تست، ۰ شکست** |
| `gradle :app:assembleDebug` | موفق |

## قدم بعدی پیشنهادی

باقی‌مانده‌ی appendix ADR-081 (نبود کامل هدر سراسری یکسان بین صفحات، پنل
خلاصه‌ی زنده‌ی Composer) هنوز رفع نشده و منتظر اولویت‌بندی/تصمیم معمار
پروژه‌اند.

## Skills استفاده‌شده

`zero-hallucination-coder` — قبل از هر تصمیم، ساختار واقعی `SceneEntity`/
`SceneDto`/سریالایزر پروژه با grep مستقیم بررسی شد (نه فرض گرفته شد) تا
ریسک Migration واقعاً ارزیابی شود، نه حدس زده شود؛ Label های موجود
(`AssetLabels.kt`) پیش از نوشتن هر رشته‌ی تازه جست‌وجو شدند. `decision-record`
— هر سه تصمیم مستقل (جایگزینی متن k34، افزودن unlinkAsset، منبع Badge) با
دلیل صریح در همین سند مستند شدند. `proportional-effort` — پیاده‌سازی به یک
فیلد + چند تابع UI محدود ماند، بدون هیچ زیرسیستم/Migration تازه.
