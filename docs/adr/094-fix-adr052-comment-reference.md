# ADR-094: اصلاح ارجاع اشتباه ADR در کامنت AddAssetButton

## زمینه

کامنت بالای `AddAssetButton` در `SceneDetailScreen.kt` (تصمیم «Dialog به‌جای
ModalBottomSheet») به‌اشتباه ADR-052 را منبع این تصمیم معرفی کرده بود.
ADR-052 موضوع کاملاً متفاوتی دارد (Tab دوربین Shot Composer، فاز ۴ قدم ۳
واحد ۱۶) و هیچ اشاره‌ای به Dialog/ModalBottomSheet برای Tab Assets صحنه
ندارد. تصمیم واقعی «Dialog به‌جای ModalBottomSheet» در
`docs/adr/085-scene-asset-linking.md` بخش «۳. تصمیم — دکمه‌ی افزودن یک
Dialog باز می‌کند، نه ModalBottomSheet» مستند است.

## بررسی مستقل

با خواندن کامل ADR-052 و ADR-085 (نه فقط اعتماد به گزارش اولیه) تأیید شد:
ADR-085 بخش ۳ دقیقاً همان تصمیم را با همان استدلال (appendix ADR-081
«عناصر مشترک») توضیح می‌دهد؛ ADR-052 هیچ بخشی درباره‌ی Dialog/BottomSheet
Tab Assets ندارد.

با `grep -rn "ADR-052"` روی کل مخزن، شش ارجاع دیگر پیدا شد
(`ShotComposerViewModel.kt`, `README.md`, دو مورد در
`docs/adr/053-...md`, `docs/adr/070-...md`, و `docs/adr/081-...md`) — همگی
بررسی و تأیید شدند که واقعاً درباره‌ی موضوعات خودِ ADR-052 هستند (تصمیم ۵
«Attached References» یا یافته‌ی تست ۲ «waitForIdle»)، نه اشتباه مشابه.
هیچ ارجاع اشتباه سومی پیدا نشد.

**یافته‌ی جداشده از فرض اولیه:** مورد دومی که قرار بود اصلاح شود
(`docs/blueprints/16-user-workflow-v2.md`، بخش «۶. وضعیت پیاده‌سازی
نهایی»، ردیف ModalBottomSheet) در فایل واقعی وجود ندارد — این فایل اصلاً
چنین بخشی ندارد و هیچ رشته‌ی «ADR-052» یا «۰۵۲» در کل فایل (۳۸۰ خط) یافت
نشد (`grep -c ""` و `grep -n "052\|۰۵۲"` هر دو تأیید کردند). تنها ارجاع
مرتبط با ModalBottomSheet در این فایل (خط ۳۷۰) یک دستورالعمل پیاده‌سازی
آینده است، بدون هیچ ارجاع ADR. این فایل دست‌نخورده ماند.

## تصمیم

فقط `SceneDetailScreen.kt` اصلاح شد: `ADR-052` → `ADR-085` در کامنت
`AddAssetButton`.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `grep -n "ADR-052\|ADR-085"` روی `SceneDetailScreen.kt` | فقط `ADR-085` باقی مانده (۴ مورد، همگی درست) |
| `grep -n "052\|۰۵۲"` روی `docs/blueprints/16-user-workflow-v2.md` | صفر مورد (فایل اصلاً موضوع را نداشت) |
| `gradle :app:compileDebugKotlin` | موفق |
