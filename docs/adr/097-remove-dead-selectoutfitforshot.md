# ADR-097: حذف کد یتیم selectOutfitForShot (واحد ۰۵)

## زمینه

`domain/shot/ShotOutfitSelection.kt` (تابع `selectOutfitForShot`) در commit
`ea1df8b` (پیاده‌سازی اولیه‌ی واحد ۰۵، ۱۹ جولای، ثبت‌شده در ADR-006 §۵) ساخته
شد. مسیر واقعی انتخاب خودکار Outfit که در تولید پرامپت نهایی استفاده می‌شود
از جای دیگری می‌گذرد: `enforceCharacterContinuity` در
`domain/promptengine/CharacterContinuity.kt` (واحد ۱۱)، که به‌تازگی در
ADR-096 کامل شد (تطبیق weather+timeOfDay+locationType). این دو تابع منطق
مشابهی داشتند اما هرگز به هم وصل نشدند.

علاوه‌بر این، `Shot` (`domain/shot/ShotModels.kt`) هیچ فیلدی برای «Outfit
Override دستی این شات خاص» ندارد — یعنی حتی اگر می‌خواستیم
`selectOutfitForShot` را وصل کنیم، جایی برای ذخیره‌ی چنین override ای در حال
حاضر وجود ندارد.

**تصمیم کاربر پروژه:** قطعی — حذف.

## راستی‌آزمایی مستقل (پیش از حذف، نه بعد از آن)

با چند grep متفاوت روی کل `app/src` (نه فقط `app/src/main/java`، طبق دستور
این قدم — `app/src/test/java` و مسیر فرضی `app/src/androidTest/java` هم
بررسی شدند؛ این پوشه اصلاً وجود ندارد در این ریپو):

```
$ grep -rn "selectOutfitForShot" . --include="*.kt"
./app/src/test/java/.../ShotOutfitSelectionTest.kt:42,48,54   (فقط تست خودش)
./app/src/main/java/.../ui/assets/CharacterAssetFormViewModel.kt:205   (کامنت، نه فراخوانی)
./app/src/main/java/.../domain/shot/ShotOutfitSelection.kt:15   (تعریف خودش)

$ grep -rn "ShotOutfitSelection" . --include="*.kt"
./app/src/test/java/.../ShotOutfitSelectionTest.kt:14   (نام کلاس تست خودش)
```

یک grep سوم بدون فیلتر پسوند هم زده شد (`grep -rln` روی کل مخزن به‌جز
`.git/`) — تنها نتایج اضافه، ارجاع‌های تاریخی در چند ADR قدیمی (۰۰۶، ۰۲۹،
۰۳۱، ۰۴۱)، بلوپرینت ۰۵، `docs/audit/pre-unit16-audit.md`، و یک خط در
`README.md` (بخش تاریخی «ممیزی F10» — توصیف یک اصلاح کامنت گذشته، نه ادعایی
درباره‌ی وضعیت فعلی) بودند — همگی مستندسازی/تاریخ، نه کد. **صفر فراخوان‌کننده‌ی
واقعی تأیید شد — پیش‌بررسی معمار دقیق بود.**

## تصمیم

- `domain/shot/ShotOutfitSelection.kt` کامل حذف شد.
- `domain/shot/ShotOutfitSelectionTest.kt` کامل حذف شد (۱۰۰٪ اختصاصی به این
  تابع، بدون هیچ تست دیگری در همان فایل — برخلاف مورد مشابه `OutputComposer.kt`
  در ADR-093 که فقط بخشی از فایل مرده بود).
- کامنت ارجاع‌دهنده در `CharacterAssetFormViewModel.kt` (خط ۲۰۵، درون مستندات
  `removeOutfit`) اصلاح شد تا فقط به `selectOutfitForScene` (مصرف‌کننده‌ی
  واقعی: `enforceCharacterContinuity`) اشاره کند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` | موفق، بدون هیچ خطای کامپایل (طبق پیش‌بینی — صفر فراخوان واقعی) |
| تعداد کل تست پیش از حذف | ۷۷۷ |
| تعداد کل تست پس از حذف | ۷۷۴ (دقیقاً ۳ کمتر — ۳ تست حذف‌شده‌ی `ShotOutfitSelectionTest.kt`) |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۳ شکست نامرتبط (`AssetFormFlowTest`، `BackupsFlowTest`، `AppNavigationTest`) — flaky شناخته‌شده‌ی محیط Sandbox؛ هر سه در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
