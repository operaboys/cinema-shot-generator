# ADR-039: واحد ۰۳/۰۸ — Type Safety کامل `getPacingFromEmotion`/`mapMoodToLighting`

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد و مستقل قابل‌کامپایل/تست است.

## Context

این Migration مستقیماً یافته‌های **F3** و **F4** ممیزی `docs/audit/pre-unit16-audit.md`
را رفع می‌کند: هر دو تابع از قبل از Migration واحد ۰۲ (`docs/adr/027-...md`) باقی
مانده بودند و رشته‌ی خام می‌گرفتند/می‌دادند — با مقادیری که دیگر با enum های واقعی
(`Mood`، `LightingStyle`، ۲۵/۲۲ مقدار) مطابقت نداشتند.

`docs/reference/type-registry.md` (خطوط ۱۱۲ و ۲۰۸/۲۱۱) از قبل امضای هدف را دقیقاً
مستند کرده بود (`getPacingFromEmotion(emotion: Mood): CinematicMode`،
`mapMoodToLighting(mood: Mood): LightingPreset` غیر-nullable، `LightingPreset` با
فیلدهای enum) — دقیقاً همان الگوی F1 (`domain/workflow/`): جدول از قبل درست بود،
فقط کد جا مانده بود. پس هیچ ابهامی درباره‌ی امضای هدف نبود؛ فقط بدنه‌ی نگاشت باید
دوباره نوشته می‌شد.

## بخش الف — `LightingPreset` و `mapMoodToLighting` (F3)

### `LightingPreset` کاملاً Type-Safe شد

هر ۶ فیلد از `String` به enum واقعی متناظرشان تغییر کردند (`LightingModels.kt`):
`style: LightingStyle`، `keyLightPosition: KeyLightPosition`، `fillLight: FillLight`،
`contrastRatio: ContrastRatio`، `shadowQuality: ShadowQuality`،
`colorTemperature: ColorTemperature` (نسخه‌ی `sceneconditions` این enum، نه
`domain.dna` — طبق کامنت موجود همان فایل، واژگانشان یکسان نیست: اینجا
`{WARM, NEUTRAL, COLD, MIXED}`، آنجا `{WARM, COOL, NEUTRAL}`).

با grep تأیید شد `LightingPreset` هیچ مصرف‌کننده‌ی دیگری در کل پروژه ندارد به‌جز
خودِ `LightingMoodMapping.kt` و تعریفش در `LightingModels.kt` — پس Blast Radius
واقعی فقط همین دو فایل + تست بود؛ هیچ‌جای دیگری (production) این تغییر را نشکست.

### نگاشت دقیق ۶ مقدار اصلی بلوپرینت

هر ۶ رشته‌ی قبلی Mood با enum واقعی `Mood` یکی است (فقط تفاوت حروف):
`"tense"→Mood.TENSE`, `"calm"→Mood.CALM`, `"dark"→Mood.DARK`,
`"mysterious"→Mood.MYSTERIOUS`, `"hopeful"→Mood.HOPEFUL`,
`"emotional"→Mood.EMOTIONAL` — هر ۶ تا با grep در `Mood` enum (`domain/dna/ProjectDna.kt`)
تأیید شدند؛ هیچ‌کدام نیاز به یک تصمیم نگاشت مبهم نداشتند.

برای مقادیر `style`/`...` رشته‌ای قبلی، معادل enum واقعی:

| مقدار قدیم | معادل enum جدید | یادداشت |
|---|---|---|
| `"dramatic"` | `LightingStyle.DRAMATIC_LIGHT` | تطابق مستقیم |
| `"soft"` | `LightingStyle.SOFT_LIGHT` | تطابق مستقیم |
| `"noir"` | `LightingStyle.LOW_KEY` | طبق نگاشت رسمی Migration واحد ۰۲ (`docs/adr/027`، همان NOIR→LOW_KEY که در `LightingModels.kt` هم مستند است) |
| `"cinematic"` | `LightingStyle.HIGH_KEY` | **تصمیم مستقل** — `LightingStyle` هیچ مقدار `CINEMATIC` ندارد (۲۲ مقدار enum بررسی شدند با grep). `"hopeful"` (Mood مثبت/امیدوارکننده) با نور روشن و کم‌کنتراست بهتر توصیف می‌شود تا هر گزینه‌ی دیگر؛ `HIGH_KEY` دقیقاً همین را در `LightingCategory.STUDIO` نمایندگی می‌کند. |

سایر فیلدها (`keyLightPosition`, `fillLight`, `contrastRatio`, `shadowQuality`,
`colorTemperature`) تطابق رشته‌به‌enum مستقیم و بدون ابهام داشتند (مثلاً
`"side"→SIDE`, `"hard_shadows"→HARD_SHADOWS`, `"cold"→COLD`).

### پوشش کامل ۲۵ مقدار Mood (گزینه‌ی ترجیح‌داده‌شده در دستور کار)

به‌جای نگه‌داشتن `null` برای ۱۹ مقدار پوشش‌نیافته، تابع اکنون **Total** است
(`LightingPreset` غیر-nullable): ۶ مقدار اصلی همان پیش‌فرض نوانس‌دار قبلی خودشان را
حفظ کردند؛ برای بقیه، `defaultLightingPresetForCategory(mood.category)` یک
پیش‌فرض معقول در سطح `MoodCategory` برمی‌گرداند — هرکدام برگرفته از پیش‌فرض یکی از
۶ مقدار اصلی که نماینده‌ی طبیعی همان دسته است (مثلاً پیش‌فرض `DARK` از پیش‌فرض
`Mood.DARK` الگو گرفته). این با `type-registry.md` (که از قبل امضای غیر-nullable
را مستند کرده بود) هم‌راستا است.

## بخش ب — `getPacingFromEmotion` (F4)

### یافته‌ی اصلاحی نسبت به پیش‌بررسی دستور کار

دستور کار ادعا کرده بود معادل واقعی `Mood` برای `"serene"`/`"contemplative"` وجود
ندارد. **با grep مستقیم در `Mood` enum (`domain/dna/ProjectDna.kt:111-115`)، این
ادعا نادرست بود** — `Mood.SERENE` و `Mood.CONTEMPLATIVE` هر دو دقیقاً با همین نام
در دسته‌ی `CALM` موجودند. آنچه واقعاً معادل ندارد: `"terrified"`، `"furious"`،
و `"melancholy"` (نزدیک‌ترین‌ها `Mood.SCARY`، بدون معادل خشم مستقیم، و
`Mood.MELANCHOLIC` با املای متفاوت). خودِ ممیزی هم این عدم‌قطعیت را صریحاً اعلام
کرده بود («این ممیزی این تطبیق دقیق enum-به-enum را انجام نداد»، `pre-unit16-audit.md:157`)
— این ADR آن را نهایی می‌کند: بدون نیاز به تصمیم معمار، چون رویکرد
`MoodCategory`-محور زیر اصلاً به تطبیق تک‌تکِ رشته‌ها وابسته نیست.

### نگاشت جدید — `MoodCategory`، نه فهرست دستی رشته‌ها

طبق پیشنهاد صریح دستور کار، تابع اکنون Total روی `Mood.category` است:

```kotlin
fun getPacingFromEmotion(emotion: Mood): CinematicMode = when (emotion.category) {
    MoodCategory.HIGH_ENERGY, MoodCategory.DARK -> CinematicMode.FAST_CUT
    MoodCategory.EMOTIONAL, MoodCategory.CALM -> CinematicMode.LONG_TAKE
    MoodCategory.POSITIVE -> CinematicMode.BALANCED
}
```

**دلیل این ترکیب دسته‌ها (تصمیم مستقل):**
- `HIGH_ENERGY` (EPIC/ACTION/EXCITING/ENERGETIC) و `DARK` (DARK/MYSTERIOUS/TENSE/
  SCARY/EERIE) هر دو ذاتاً ریتم تندتر/برشی می‌طلبند — انرژی بالا برای هیجان و
  تعلیق، تاریکی برای تنش/ترس. این با رفتار قبلی («tense» → FAST_CUT) هم‌راستا است.
- `EMOTIONAL` (EMOTIONAL/MELANCHOLIC/NOSTALGIC/BITTERSWEET/DRAMATIC) و `CALM`
  (CALM/PEACEFUL/SERENE/CONTEMPLATIVE/DREAMY_MOOD) هر دو با نماها و ریتم آهسته‌تر
  بهتر بیان می‌شوند — احساس نیاز به فضا برای نفس‌کشیدن دارد، آرامش هم به‌طور طبیعی
  کند است. رفتار قبلی («melancholy»/«serene»/«contemplative» → LONG_TAKE) دقیقاً
  همین جهت را داشت.
- `POSITIVE` (HAPPY/JOYFUL/PLAYFUL/HOPEFUL/WARM/ROMANTIC) روی `BALANCED` ماند —
  نه به‌اندازه‌ی HIGH_ENERGY تند، نه به‌اندازه‌ی CALM آرام؛ رفتار قبلی هم برای این
  مقادیر (که اصلاً در فهرست دستی قدیم نبودند) به‌طور ضمنی BALANCED بود (شاخه‌ی `else`).

تابع اکنون **Total** است (بدون شاخه‌ی `else`) — کامپایلر Kotlin تضمین می‌کند هر
۵ مقدار `MoodCategory` (و درنتیجه هر ۲۵ مقدار `Mood`) پوشش داده شده‌اند.

## تست

- `LightingMoodMappingTest.kt`: ۶ تست موجود برای مقادیر اصلی به‌روزرسانی شدند
  (فراخوانی با `Mood`/بررسی enum واقعی، نه رشته)؛ ۵ تست جدید برای پوشش دسته‌ای
  (یک نماینده از هر ۵ `MoodCategory` که نگاشت اختصاصی ندارد، تأیید می‌کند پیش‌فرض
  دسته صحیح اعمال می‌شود).
- `CinematicLanguageTest.kt`: ۳ تست قدیمی رشته‌ای حذف و با ۵ تست `Mood`-محور
  (یکی به‌ازای هر دسته) + ۱ تست جامع (`Mood.entries.forEach` — تضمین Total بودن
  بدون Exception برای هر ۲۵ مقدار) جایگزین شدند.

جمع خالص تغییر تعداد تست‌ها: `LightingMoodMappingTest` از ۷ به ۱۱ (+۴، چون تست
`unknown mood returns null` حذف و ۵ تست دسته‌ای اضافه شد)؛ `CinematicLanguageTest`
از ۳ تست `getPacingFromEmotion` به ۶ (+۳).

## Consequences

- **آسان می‌شود:** Dropdown های «Lighting Style» و «Cinematic Mode» بلوپرینت ۱۶
  اکنون می‌توانند مستقیماً پیش‌فرض خودکار Type-Safe بگیرند، بدون نیاز به تبدیل
  رشته‌ی دستی در لایه‌ی UI.
- **بدون بدهی جدید شناخته‌شده:** هر دو تابع اکنون Total هستند (بدون nullability
  یا شاخه‌ی `else` غیرشفاف)؛ `type-registry.md` بدون نیاز به تغییر از قبل دقیقاً
  همین وضعیت هدف را مستند کرده بود.
