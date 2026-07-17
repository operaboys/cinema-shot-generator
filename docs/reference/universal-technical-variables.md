# کتابخانه‌ی متغیرهای فنی و سینمایی

**نقش:** Reference — کتابخانه‌ی مرجع مستقل از هر مدل خاص
**وضعیت:** فعال

مجموعه‌ای از مقادیر و اصطلاحات سینمایی که سایر واحدها (Camera & Motion، Scene Conditions، Character & Subject) از آن استفاده می‌کنند. این مقادیر واقعاً جهانی‌اند — مستقل از این‌که خروجی نهایی برای کدام مدل AI رندر می‌شود.

---

## ۱. سبک‌های بصری (Visual Styles)

هسته‌ی اصلی DNA پروژه از این متغیرها تشکیل می‌شود:

- **سینمایی:** Studio Ghibli، Disney 1950s، Pixar Modern، Anime Makoto Shinkai، Wes Anderson Style
- **هنری:** Oil Painting، Watercolor، Charcoal Sketch، Ink Wash، Ukiyo-e
- **فنی:** Photorealistic، 8K Resolution، Unreal Engine 5 Render، IMAX 70mm، Vintage Film Stock
- **سبک‌شده:** Low Poly، Claymation، Cyberpunk Neon، Steampunk Copper

## ۲. نورپردازی (Lighting Matrix)

- **طبیعی:** Golden Hour، Blue Hour، High Noon، Overcast، Twilight
- **مصنوعی:** Neon Lights، Fluorescent، Candlelight، Street Lamp، Studio Softbox
- **دراماتیک:** Chiaroscuro (کنتراست بالا)، Rim Lighting، Backlit (سایه‌نما)، Volumetric God Rays
- **استودیویی:** Three-point Lighting، Butterfly Lighting، Rembrandt Lighting

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

---

## نکته‌ی مهم معماری

**این کتابخانه مستقل از هر مدل AI خاص است.** توکن‌های فنی مخصوص یک پلتفرم (مثل `--ar 16:9` یا `--cref` در Midjourney) در این‌جا نگهداری **نمی‌شوند** — چون طبق معماری جداسازی PromptBlueprint خنثی از Rendering، هیچ نحو مخصوص پلتفرمی نباید در منطق اصلی (Prompt Engineering Core یا این کتابخانه‌ی مرجع) به‌کار رود.

محتوای مخصوص هر مدل (پارامترها، محدودیت‌ها، نحو دستوری) در **Model Profile Library** (بخشی از بلوپرینت Output Delivery System، به‌صورت JSON قابل‌ویرایش) نگهداری می‌شود — نه اینجا.

---

## نحوه‌ی استفاده

این مقادیر به‌عنوان گزینه‌های انتخابی در UI (مثلاً هنگام تعریف Shot یا DNA پروژه) ارائه می‌شوند. انتخاب کاربر از این فهرست، مستقیماً به‌عنوان بخشی از `structured_parts` در `PromptBlueprint` قرار می‌گیرد.
