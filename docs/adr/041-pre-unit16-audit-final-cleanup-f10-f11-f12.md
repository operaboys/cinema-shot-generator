# ADR-041: پاک‌سازی نهایی ممیزی pre-Unit 16 — F10، F11، F12

**تاریخ:** 2026-07-28
**وضعیت:** کامل شد. تغییرات فقط کامنت/مستندات هستند — هیچ منطق Kotlin عوض نشد.

## Context

آخرین سه یافته‌ی باز `docs/audit/pre-unit16-audit.md` (هر سه ⚪، صرفاً
مستندسازی) در این قدم رفع شدند.

## F10 — ۱۲ کامنت هدر بدون `-v2`

۱۲ فایل فهرست‌شده در دستور کار grep و اصلاح شدند — هرکدام ارجاع «منبع حقیقت» را
از نام بلوپرینت بدون `-v2` به نسخه‌ی `-v2` موجود در `docs/blueprints/` تغییر داد:
`ProjectDnaEntity.kt`، `ShotSettingsResolution.kt`، `ShotOutfitSelection.kt`،
`ShotValidation.kt`، `AssetSelection.kt`، و ۷ فایل `domain/promptengine/`
(`PromptAssembly.kt`، `PriorityResolution.kt`، `WeightedEmphasis.kt`،
`SeedManagement.kt`، `ConflictResolution.kt`، `PromptEngineModels.kt`،
`CharacterContinuity.kt`).

**تصمیم مستقل (طبق تصریح دستور کار):** برخی از این فایل‌ها به بلوپرینت‌های دیگری
هم ارجاع می‌دهند که نسخه‌ی `-v2` ندارند (مثلاً `ProjectDnaEntity.kt` هم به
`15-project-storage.md` ارجاع می‌دهد؛ `ShotSettingsResolution.kt` هم به
`04-scene-engine.md`) — با grep در `docs/blueprints/` تأیید شد این دو فایل واقعاً
نسخه‌ی `-v2` ندارند، پس فقط بخش ارجاع‌شونده‌ی واقعاً `-v2`‌دار در هر خط اصلاح شد،
نه هر دو. ADR های تاریخی (۰۰۱، ۰۰۲، ۰۰۳، ۰۰۶، ۰۱۵، ۰۲۱، ۰۲۵، ۰۳۰ و مشابه) طبق
دستور صریح دست‌نخورده ماندند.

## F11 — `type-registry.md`، ردیف `Shot`: nullable اشتباه

سه فیلد `camera`/`lighting`/`environment` اشتباهاً با `?` (nullable) نوشته شده
بودند. با grep در `ShotModels.kt` تأیید شد هر سه non-nullable هستند (پیش‌فرض
`SourcedSettings()`، نه `null`) — `?` از هر سه حذف شد.

## F12 — `docs/blueprints/16-user-workflow-v2.md`: مقدار پنجم `EntityState`

خط ۵۷ فقط ۴ مقدار (`Draft/Review/Locked/Final`) فهرست کرده بود؛ `EntityState`
واقعی (`domain/stateversioning/StateMachine.kt:9`) ۵ مقدار دارد
(`DRAFT, REVIEW, LOCKED, FINAL, ARCHIVED`). متن به «Draft/Review/Locked/Final/
Archived» اصلاح شد، با ارجاع صریح به فایل واقعی enum.

با grep در کل بلوپرینت ۱۶ تأیید شد **هیچ‌جای دیگری** نگاشت رنگ/آیکون مشخصی برای
تک‌تک مقادیر `EntityState` تعریف نکرده بود (فقط یک توصیف کلی «آیکون قفل/دایره‌ی
رنگی» بدون جزئیات per-value) — پس مسئله‌ی «مطمئن شو ARCHIVED هم نمایش بصری
دارد» در واقع برای هیچ‌کدام از ۵ مقدار از قبل مشخص نبود، نه فقط ARCHIVED. طبق
اجازه‌ی صریح دستور کار («حتی اگر فقط یک پیشنهاد ساده باشد»)، یک پیشنهاد اولیه‌ی
ساده برای هر ۵ حالت اضافه شد (نه فقط ARCHIVED، تا سازگاری بصری بین ۵ حالت حفظ
شود): Draft (خاکستری روشن)، Review (زرد/چشم)، Locked (نارنجی/قفل بسته)، Final
(سبز/تیک)، Archived (خاکستری تیره/جعبه‌ی آرشیو) — صریحاً به‌عنوان «راهنمای اولیه‌ی
پیاده‌سازی»، نه یک الزام طراحی دقیق و نهایی، علامت‌گذاری شد تا طراح UI واقعی
واحد ۱۶ (زمان ساخت `EntityCard`/`SceneCard`/... طبق نکته‌ی خط ۳۶۱ همان بلوپرینت)
آزاد بماند این پیشنهاد را دقیق‌تر کند.

## تست

بدون تغییر منطقی — هیچ تست جدیدی لازم نبود. `gradle :app:assembleDebug
:app:testDebugUnitTest` برای تأیید این‌که هیچ کامنتی به‌اشتباه بلوک کد را نشکسته،
اجرا شد (نتیجه در README/گزارش نهایی).

## Consequences

- **آسان می‌شود:** با این قدم، **هر ۱۲ یافته‌ی** `pre-unit16-audit.md` رفع/مستند
  شدند — F1 تا F4 و F7 تا F12 کاملاً بسته‌اند؛ F5/F6 عمداً برای زمان طراحی واقعی
  فرم‌های واحد ۱۶ نگه داشته شده‌اند (نه یافته‌ی باز فراموش‌شده، بلکه تصمیم آگاهانه‌ی
  ممیزی اصلی). ممیزی pre-Unit 16 در کل بسته است.
- **بدون بدهی جدید:** این قدم فقط مستندات را با کد واقعی هم‌راستا کرد.
