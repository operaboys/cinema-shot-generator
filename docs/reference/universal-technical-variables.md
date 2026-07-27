# کتابخانه‌ی متغیرهای فنی و سینمایی

**نقش:** Reference — کتابخانه‌ی مرجع مستقل از هر مدل خاص
**وضعیت:** فعال

**نسخه:** ۳ — بازنویسی برای رفع تناقض «منبع واحد حقیقت» کشف‌شده در بازبینی معماری مستقل: اعداد اعلام‌شده در این سند (۳۵ برای `VisualStyle`، ۲۱ برای `LightingStyle`، ۲۴ برای `Mood`) با شمارش دقیق enum های واقعی در `02-dna-manager-v2.md` (که خودِ آن سند هم در نسخه‌ی قبلی‌اش اعداد تقریبی نادرستی داشت) ناهماهنگ بود. اعداد دقیق و نهایی: `VisualStyle`=۳۴ (۵ دسته)، `LightingStyle`=۲۲ (۴ دسته)، `Mood`=۲۵ (۵ دسته) — این سند اکنون دقیقاً با نسخه‌ی ۵ بلوپرینت ۰۲ یکی است. **تغییرات با «🆕v3» علامت‌گذاری شده‌اند.**

مجموعه‌ای از مقادیر و اصطلاحات سینمایی که سایر واحدها (Camera & Motion، Scene Conditions، Character & Subject) از آن استفاده می‌کنند. این مقادیر واقعاً جهانی‌اند — مستقل از این‌که خروجی نهایی برای کدام مدل AI رندر می‌شود.

---

## ۱. سبک‌های بصری (Visual Styles)

🆕v3 هسته‌ی اصلی DNA پروژه از این متغیرها تشکیل می‌شود. این فهرست اکنون دقیقاً با `enum VisualStyle` در `02-dna-manager-v2.md` (نسخه ۵) یکی است (۵ دسته، ۳۴ مقدار — نه ۶ دسته/۳۵ مقدار که در نسخه‌ی قبلی این سند اشتباهاً ذکر شده بود) — منبع واحد حقیقت.

- **سینمایی:** Cinematic، Photorealistic، Film Noir، Vintage/Retro، Documentary، Blockbuster، Indie/Arthouse
- **انیمیشن ۳D:** Pixar/Disney، DreamWorks، Illumination، Low Poly، Claymation، Isometric
- **انیمیشن ۲D:** Anime، Studio Ghibli، Disney Classic، Cartoon، Comic Book، Manga
- **هنری:** Watercolor، Oil Painting، Pencil Sketch، Impressionist، Pop Art، Art Nouveau، Minimalist
- **ژانر:** Epic Fantasy، Sci-Fi، Cyberpunk، Steampunk، Gothic، Horror، Surreal، Dreamy

## ۲. نورپردازی (Lighting Matrix)

🆕v3 این فهرست اکنون دقیقاً با `enum LightingStyle` در `02-dna-manager-v2.md` (نسخه ۵) یکی است (۴ دسته، ۲۲ مقدار — نه ۲۱ که در نسخه‌ی قبلی این سند اشتباهاً ذکر شده بود) — منبع واحد حقیقت.

- **طبیعی:** Natural Light، Daylight، Overcast، Golden Hour، Blue Hour، Sunset، Sunrise
- **شب:** Moonlight، Starlight، City Night
- **استودیویی:** Soft Light، Dramatic Light، High Key، Low Key، Rim Light، Backlit، Side Light
- **خاص:** Neon، Volumetric، Candlelight، Firelight، Bioluminescent

## ۳. دوربین و لنز (Camera & Optics)

**فاصله‌ی کانونی (لنز):**
| نوع | محدوده | کاربرد |
|---|---|---|
| Ultra-Wide | 12mm–14mm | مناظر عظیم |
| Wide | 24mm–35mm | تعاملات محیطی |
| Standard | 50mm | دید طبیعی انسان |
| Portrait/Tele | 85mm–135mm | تمرکز روی احساسات |
| Macro | — | جزئیات بسیار ریز (چشم، قطره آب) |

**زوایا:** Low Angle، High Angle، Bird's Eye، Worm's Eye، Dutch Angle، POV، Over-the-shoulder

**عمق میدان:** Deep Focus، Shallow DoF، Bokeh، Rack Focus

## ۴. حرکت و فیزیک (Motion & Action)

مخصوص هدایت مدل‌های ویدیوساز:

- **حرکات دوربین:** Dolly In/Out، Pan Left/Right، Tilt Up/Down، Crane Shot، Handheld Shake، Orbit
- **اکشن سوژه:** Walking، Running، Flying، Floating، Dissolving، Transforming
- **کنترل سرعت:** Slow Motion، Time-lapse، Real-time، Fast Motion

## ۵. محیط و جو (Atmosphere & Weather)

- **آب‌وهوا:** Heavy Rain، Light Drizzle، Snowstorm، Foggy/Misty، Sandstorm، Sunny
- **ذرات:** Dust Motes، Fireflies، Falling Leaves، Floating Petals، Smoke، Steam
- **زمان روز:** Dawn، Morning، Noon، Afternoon، Golden Hour، Sunset، Night، Midnight

## ۶. طیف احساسی کاراکتر (Character Emotional Range)

- **حالت‌های چهره:** Joyful، Melancholy، Furious، Terrified، Determined، Skeptical، Serene
- **حالت‌های فیزیکی:** Exhausted، Energetic، Wounded، Magical Glow، Shadowy Aura

## 🆕v2 ۷. حال‌وهوای پروژه (Project Mood)

این فهرست دقیقاً با `enum Mood` در `02-dna-manager-v2.md` (نسخه ۵) یکی است (۵ دسته، ۲۵ مقدار — نه ۲۴ که در نسخه‌ی قبلی این سند اشتباهاً ذکر شده بود) — منبع واحد حقیقت. **توجه:** این با «طیف احساسی کاراکتر» (بخش ۶ بالا) متفاوت است — بخش ۶ حالت لحظه‌ای یک کاراکتر خاص در یک Shot است، این بخش حال‌وهوای کلی و سراسری کل پروژه (سطح DNA) است.

- **انرژی بالا:** Epic، Action، Exciting، Energetic
- **مثبت:** Happy، Joyful، Playful، Hopeful، Warm، Romantic
- **احساسی:** Emotional، Melancholic، Nostalgic، Bittersweet، Dramatic
- **تاریک:** Dark، Mysterious، Tense، Scary، Eerie
- **آرام:** Calm، Peaceful، Serene، Contemplative، Dreamy

---

## نکته‌ی مهم معماری

**این کتابخانه مستقل از هر مدل AI خاص است.** توکن‌های فنی مخصوص یک پلتفرم (مثل `--ar 16:9` یا `--cref` در Midjourney) در این‌جا نگهداری **نمی‌شوند** — چون طبق معماری جداسازی PromptBlueprint خنثی از Rendering، هیچ نحو مخصوص پلتفرمی نباید در منطق اصلی (Prompt Engineering Core یا این کتابخانه‌ی مرجع) به‌کار رود.

محتوای مخصوص هر مدل (پارامترها، محدودیت‌ها، نحو دستوری) در **Model Profile Library** (بخشی از بلوپرینت Output Delivery System، به‌صورت JSON قابل‌ویرایش) نگهداری می‌شود — نه اینجا.

---

## نحوه‌ی استفاده در UI

طبق سند اصلی این کتابخانه، این مقادیر باید در UI به‌صورت **«تگ‌های آماده‌ی قابل‌کلیک» (Tag/Chip)** ارائه شوند — نه یک لیست متنی ساده یا Dropdown طولانی. هر انتخاب کاربر از این تگ‌ها مستقیماً یک Keyword به `structured_parts` در `PromptBlueprint` تزریق می‌کند.

**پیامد برای طراحی UI (Jetpack Compose):** برای هر دسته (سبک، نور، دوربین، آب‌وهوا، احساس) باید از یک کامپوننت `FilterChip` یا `AssistChip` (بخشی از Material 3، سازگار با Compose) در یک `FlowRow` استفاده شود تا کاربر بتواند به‌سرعت با لمس (نه تایپ یا اسکرول در یک لیست بلند) گزینه‌ی موردنظرش را انتخاب کند — مثلاً هنگام تعریف Lighting یک Shot، تگ‌های «Golden Hour»، «Chiaroscuro»، «Rembrandt Lighting» به‌صورت Chip های قابل‌کلیک کنار هم نمایش داده شوند، نه یک منوی کشویی طولانی.

```kotlin
// نمونه‌ی مفهومی: نمایش دسته‌ی Lighting به‌عنوان Chip های قابل‌انتخاب
@Composable
fun LightingTagSelector(
    options: List<String>,          // مثلاً از دسته‌ی "Dramatic" در بخش ۲
    selected: String?,
    onSelect: (String) -> Unit
) {
    FlowRow {
        options.forEach { tag ->
            FilterChip(
                selected = tag == selected,
                onClick = { onSelect(tag) },
                label = { Text(tag) }
            )
        }
    }
}
```

این مقادیر همچنین می‌توانند در تعریف DNA پروژه (سبک بصری، پالت رنگی) به همین شکل استفاده شوند.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این سند مرجع است (نه بلوپرینت مستقیم)، اما اکنون منبع مشترک واقعی برای enum های `VisualStyle`/`LightingStyle`/`Mood` در `02-dna-manager-v2.md` است:

- این سند خودش کدی برای پیاده‌سازی ندارد؛ فقط اطمینان حاصل کن که هنگام Migration بلوپرینت ۰۲، مقادیر enum دقیقاً با فهرست این سند یکی باشند (نه یک زیرمجموعه یا ابرمجموعه‌ی متفاوت) — اگر تفاوتی دیدی، این را به‌عنوان یک ناسازگاری واقعی گزارش بده، حدس نزن کدام درست است.
- بخش ۷ (Mood) کاملاً جدید است در این نسخه — قبلاً اصلاً وجود نداشت.
- الگوی UI پیشنهادی (Chip های قابل‌کلیک در `FlowRow`) که در انتهای این سند توضیح داده شده، برای هر ۷ بخش (شامل بخش جدید Mood) به‌طور یکسان قابل‌اعمال است.
- 🆕v3 این سند خودش هیچ enum ندارد (فقط توضیح متنی مقادیر enum های `domain.dna`) — پس هیچ Migration کدی برای این فایل لازم نیست. تنها اقدام لازم: مطمئن شو هیچ سند دیگری (بلوپرینت‌های ۰۳، ۰۸، ۰۹) عدد قدیمی/نادرست این enum ها را تکرار نکرده باشد؛ در صورت مشاهده، به‌عنوان یافته‌ی جدید گزارش بده.
