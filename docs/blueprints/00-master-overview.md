# نمای کلی معماری (Master Overview)

**نقش:** Blueprint — نقطه‌ی ورود اصلی به معماری پروژه
**وضعیت:** فعال — این سند جایگزین کامل تمام اسناد Master قدیمی است

**اگر فقط یک سند از این پروژه را بخوانید، همین سند باشد.**

---

## معرفی پروژه

**Cinema Shot Generator** یک اپ اندروید **تک‌کاربره، On-Device، بدون سرور** است که پرامپت متنی برای مدل‌های AI ویدیوساز (Veo، Sora، Kling، Runway، Seedance، Luma، Hailuo، Pika، Midjourney، Stable Diffusion و مشابه) تولید می‌کند.

**نکته‌ی حیاتی:** این اپ **فقط متن تولید می‌کند — نه تصویر، نه ویدیو.** به همین دلیل هیچ محدودیت پردازشی رسانه‌ای وجود ندارد و تمام واحدهای معماری می‌توانند با عمق کامل طراحی شوند.

### Stack فنی
- **زبان:** Kotlin
- **UI:** Jetpack Compose
- **معماری:** MVVM ساده (ViewModel + StateFlow، بدون فریمورک DI در فاز اول)
- **ذخیره‌سازی:** Room (روی SQLite)، یک دیتابیس واحد برای چند پروژه هم‌زمان
- **اتصال AI (اختیاری):** Ktor Client — فقط وقتی کاربر کلید API شخصی وارد کند

---

## نقشه‌ی ۱۸ واحد معماری

معماری این پروژه از ۳۰ ماژول اولیه به **۱۸ واحد منطقی** بازطراحی شد — منطق حفظ شد، فقط بسته‌بندی تغییر کرد. هر واحد یک بلوپرینت مستقل در همین پوشه (`docs/blueprints/`) دارد.

| # | واحد | فایل | نقش خلاصه |
|---|---|---|---|
| ۰۱ | Story & Override | `01-story-and-override.md` | ورودی اولیه‌ی کاربر + مکانیزم Override انسانی |
| ۰۲ | DNA Manager | `02-dna-manager.md` | قوانین سراسری پروژه (Soft Lock) |
| ۰۳ | Visual Identity | `03-visual-identity.md` | سبک بصری + ریتم روایت |
| ۰۴ | Scene Engine | `04-scene-engine.md` | کانتینر صحنه، ارث‌بری به Shot |
| ۰۵ | Shot Engine | `05-shot-engine.md` | کوچک‌ترین واحد اجرایی |
| ۰۶ | Asset & Continuity | `06-asset-and-continuity.md` | کاراکتر/مکان/شیء + **Hard Lock** تداوم |
| ۰۷ | Validation & Consistency | `07-validation-and-consistency.md` | اعتبارسنجی + تشخیص تضاد + وابستگی |
| ۰۸ | Scene Conditions | `08-scene-conditions.md` | نور + آب‌وهوا + زمان‌بندی |
| ۰۹ | Camera & Motion | `09-camera-and-motion.md` | دوربین + شدت حرکت |
| ۱۰ | Audio Context | `10-audio-context.md` | صدا (Ambient خودکار / Character دستی) |
| ۱۱ | **Prompt Engineering Core** ⭐ | `11-prompt-engineering-core.md` | قلب سیستم — تولید PromptBlueprint خنثی |
| ۱۲ | State & Versioning | `12-state-and-versioning.md` | چرخه‌ی حیات + نسخه‌بندی |
| ۱۳ | Prompt Finalization | `13-prompt-finalization.md` | تمیزکاری + تخمین توکن |
| ۱۴ | **Output Delivery** ⭐ | `14-output-delivery.md` | Model Profile Library + Renderer + دوزبانگی |
| ۱۵ | Project Storage | `15-project-storage.md` | Room Database، چند پروژه هم‌زمان |
| ۱۶ | User Workflow | `16-user-workflow.md` | نقشه‌راه ۸ مرحله‌ای تعامل کاربر |
| ۱۷ | Universal Technical Variables | `../reference/universal-technical-variables.md` | کتابخانه‌ی مقادیر سینمایی مرجع |

---

## دو اصل معماری بنیادی (باید همیشه رعایت شوند)

### ۱. جداسازی PromptBlueprint از Renderer

```
Prompt Engineering Core (واحد ۱۱)
  → فقط جمع‌آوری داده + حل تضاد + اولویت (Human Override > Shot > Scene > DNA)
  → خروجی: PromptBlueprint خنثی (بدون هیچ فرض خاص مدلی)
        ↓
Output Delivery System (واحد ۱۴)
  → Renderer که Model Profile را می‌خواند
  → فرمت نهایی مخصوص یک مدل خاص
```

هرگز این دو مسئولیت را در یک جا مخلوط نکنید — این دقیقاً همان اشتباهی بود که در بازبینی معماری کشف و رفع شد (نگاه کنید به `governance/risk-register.md`، ریسک R1).

### ۲. دو نوع Lock کاملاً متفاوت

| | DNA Manager (واحد ۰۲) | Character Continuity (واحد ۰۶) |
|---|---|---|
| نوع | **Soft Lock** | **Hard Lock** |
| رفتار | قابل تغییر، فقط هشدار در ناسازگاری | **غیرقابل Override، همیشه Blocking** |
| دلیل | چارچوب سبکی قابل‌تجدیدنظر | تضمین کیفی بنیادی (ثبات ظاهری) |

این دو هرگز نباید با هم اشتباه گرفته شوند.

---

## جریان کامل داده (End-to-End)

```
کاربر (Story Wizard) → StoryContext
        ↓
DNA Manager → ProjectDna (Soft Lock)
        ↓
Asset & Continuity → Character/Location/Object (Hard Lock برای Character)
        ↓
Scene Engine → Scene (با Visual Identity + Scene Conditions)
        ↓
Shot Engine → Shot (با Camera & Motion + Audio Context + Beat Sheet)
        ↓
Validation & Consistency → گزارش (Blocking/Warning)
        ↓
Prompt Engineering Core → PromptBlueprint خنثی  ⭐
        ↓
Output Delivery System → انتخاب Model Profile → Render  ⭐
        ↓
Prompt Finalization → تمیزکاری + بررسی توکن
        ↓
خروجی نهایی به کاربر (هر دو زبان + هر مدل انتخابی)
        ↓
[در پس‌زمینه، همیشه فعال] State & Versioning + Project Storage
```

---

## قوانین رفتاری (Governance) — پیش از هر پیاده‌سازی بخوانید

مستندات کامل در `docs/governance/`:

| سند | موضوع |
|---|---|
| `override-policy.md` | Human-in-the-Loop، مدل Blocking/Warning، قوانین Override |
| `anti-patterns.md` | الگوهایی که باید هرگز رخ ندهند |
| `red-flags.md` | علائم هشدار حین توسعه |
| `module-boundaries.md` | مرز مسئولیت واحدها |
| `non-goals.md` | آنچه عمداً پیاده‌سازی نمی‌شود |
| `concept-ownership-map.md` | مالکیت مفهومی هر تصمیم |
| `architecture-philosophy.md` | اصول بنیادین معماری |
| `change-management.md` | فرآیند رسمی تغییرات (با قالب ثبت) |
| `risk-register.md` | ریسک‌های شناخته‌شده و راه مهار |
| `ai-coding-guidelines.md` | قوانین رفتاری Claude Code |
| `ai-governance.md` | ۱۰ قانون مطلق حاکمیت AI |

مراجع تکمیلی در `docs/reference/`: `glossary.md`، `naming-conventions.md`، `universal-technical-variables.md`.

---

## اصول تکرارشونده در سرتاسر معماری

- **پیش‌فرض هوشمند + Override دستی همیشه در دسترس** (مثل انتخاب Outfit بر اساس آب‌وهوا، Mood-to-Lighting Mapping) — نه حدس بی‌پایه، بلکه استنباط شفاف و قابل بازگشت.
- **کیفیت مهم‌تر از سرعت** — هیچ KPI عددی سرعت‌محور (مثل «۲۰ دقیقه اولین پرامپت») در این معماری وجود ندارد.
- **دو سطح Severity، نه بیشتر:** Blocking (واقعاً جلوی ادامه را می‌گیرد) و Warning (اطلاع می‌دهد، کاربر تصمیم می‌گیرد).
- **On-Device مطلق:** هیچ واحدی به سرور، Cloud، یا حساب کاربری وابسته نیست.

---

## برای شروع پیاده‌سازی هر واحد

1. بلوپرینت همان واحد را در `docs/blueprints/` کامل بخوانید.
2. اسناد `docs/governance/` مرتبط را چک کنید (خصوصاً `ai-coding-guidelines.md`).
3. اگر واحد به واحد دیگری وابسته است (ستون «وابستگی» در هر بلوپرینت)، آن واحد باید زودتر پیاده‌سازی شده باشد.
4. هر تصمیم مستقل یا انحراف از بلوپرینت باید گزارش و تأیید شود، طبق `governance/ai-coding-guidelines.md`.
