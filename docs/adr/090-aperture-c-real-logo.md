# ADR-090: اتصال واقعی نشان «Aperture C» به کد (یافته‌ی #۸ appendix، آخرین یافته‌ی UI باقی‌مانده)

## زمینه

طبق ADR-083 (بخش ۳)، نشان سفارشی Aperture C عمداً موکول شده بود چون هیچ
دارایی گرافیکی واقعی وجود نداشت — فقط یک آیکون Material موقت
(`Icons.Filled.Movie`/`MovieFilter`) جایگزینش بود. حالا که فایل‌های اصلی
لوگو (`docs/design/logo/aperture-c-mark.svg` + PNG، اورجینال، بدون محدودیت
کپی‌رایت) در پروژه موجودند، این بدهی رفع شد.

## پیاده‌سازی — تبدیل SVG به Vector Drawable

`aperture-c-mark.svg` دقیقاً ۸ `<path>` دارد، هرکدام با یک `<linearGradient>`
مجزا (`gradientUnits` پیش‌فرض SVG یعنی `objectBoundingBox`: x1=0,y1=0 تا
x2=1,y2=1 — یعنی گرادیان از گوشه‌ی بالا-چپ تا پایین-راست کادر مرزی همان
Path، نه کل Canvas). این تبدیل با یک اسکریپت Python (با کتابخانه‌ی
`svgpathtools`، نصب‌شده در این محیط) انجام شد، نه دستی:

1. **جابه‌جایی مختصات**: viewBox اصلی SVG (`-128 -128 256 256`) با اندروید
   VectorDrawable (که مفهوم Origin منفی/viewBox ندارد) سازگار نیست — هر
   مختصات پایانی (M/L/A) با +۱۲۸ جابه‌جا شد. پارامترهای هر Arc (شعاع، زاویه‌ی
   چرخش، پرچم‌های large-arc/sweep) دست‌نخورده ماندند — فقط نقطه‌ی پایانی هر
   Arc شیفت شد.
2. **گرادیان هر Path**: چون اندروید `objectBoundingBox` را ندارد (`<gradient
   android:startX/Y-endX/Y>` همیشه در فضای مطلق viewport است)، bbox واقعی
   هر Path (نه bbox کل Canvas) با `svgpathtools.parse_path(d).bbox()` محاسبه
   شد — این تابع Arc ها را واقعاً حساب می‌کند (نه فقط min/max نقاط پایانی)،
   پس نتیجه معادل دقیق `objectBoundingBox 0,0 → 1,1` اصلی SVG است.

نتیجه: `app/src/main/res/drawable/ic_aperture_c_logo.xml` — ۸ `<path>`، هرکدام
با `<aapt:attr name="android:fillColor"><gradient .../></aapt:attr>` (نیازمند
`xmlns:aapt`، از API 24+ پشتیبانی می‌شود؛ minSdk این پروژه ۲۶ است).

## تصمیم مستقل — Icon() با tint=Unspecified، نه Image()

دستور کار `Icon(painter = painterResource(...))` را پیشنهاد داده بود. مسئله‌ی
واقعی: `Icon()` با هر مقدار `tint` غیر از `Color.Unspecified` رنگ چندگانه‌ی
خودِ Vector را با یک رنگ یکدست جایگزین می‌کند — اگر بی‌توجه با همان الگوی
موجود (`tint = MaterialTheme.colorScheme.primary`/`onPrimary`) جایگزین
می‌شد، کل گرادیان بنفش-نارنجی نامرئی می‌شد (فقط یک شکل تک‌رنگ). رفع: همان
`Icon()` پیشنهادی دستور کار حفظ شد، اما با `tint = Color.Unspecified` صریح —
API استاندارد و مستندشده‌ی Compose برای نگه‌داشتن رنگ واقعی یک Vector چندرنگ
از داخل `Icon()`.

## محل‌های تعویض‌شده

- **`HomeScreen.kt` خط ۶۴۰** (Pill هدر «Cinema Studio»، Root screens) —
  یکی از سه محل رسمی مستندشده‌ی `docs/design/README.md` بخش Assets («Used in
  header (root screens)...»). `tint = Color.Unspecified`.
- **`SettingsScreen.kt` — کارت «درباره»** — قبلاً هیچ آیکونی نداشت (تأیید
  دستور کار). طبق mockup (`is.settings`، ردیف «درباره»: کادر ۶۴dp با
  `border-radius:22px`، پس‌زمینه‌ی `inset`، حاشیه‌ی `hairlineStrong`، نشان
  ۴۴dp داخلش، کنار ستون متن) اضافه شد — دقیقاً همان مقادیر mockup، با توکن‌های
  رنگ موجود پروژه (`CinemaTheme.extendedColors.inset`/`hairlineStrong`)، نه
  رنگ خام. ساختار کارت بیرونی (`SettingsCard` با عنوان بخش مستقل) دست‌نخورده
  ماند — این تفاوت جزئی و از پیش پذیرفته‌شده‌ی الگوی این صفحه با mockup است
  (هر بخش Settings، از جمله About، از یک عنوان بخش مشترک استفاده می‌کند؛
  mockup فقط برای About یک کارت مستقل بدون عنوان جدا دارد)، تغییرش خارج از
  دامنه‌ی این قدم بود.
- **`HomeScreen.kt` خط ۴۸۷** (کاشی «خروجی» در Home Resume Layout) — طبق
  دستور صریح این قدم تعویض شد. یافته‌ی مستقل (برای شفافیت، نه یک انحراف):
  ۳ کاشی دیگر همین ردیف (AI Breakdown/DNA/Validation) آیکون‌های Material
  تک‌رنگ دارند و tint یکسان می‌گیرند؛ این یکی حالا تنها کاشی چندرنگ این ردیف
  است — چون این محل جزو ۳ محل رسمی مستند mockup برای نشان برند نبود (فقط
  header/drawer-header/Settings-About)، اما دستور کار صریحاً همین خط را نام
  برده بود، دنبال شد.

## یافته‌ی #۴ دستور کار — جاهای دیگر با آیکون موقت مشابه

با grep، ۴ محل دیگر پیدا شد که از `Icons.Filled.Movie`/`MovieFilter` استفاده
می‌کنند: `ShotComposerScreen.kt` (دکمه‌ی ورود Output Delivery + آیکون گروه
Accordion «Main»)، `ValidationScreen.kt` (دکمه‌ی ورود Output Delivery)، و
`BottomNavBar.kt` (Tab «استودیو»). همه‌شان آیکون‌های عملکردی عمومی‌اند (نه
نشان برند) — هیچ‌کدام جزو ۳ محل رسمی mockup نیستند، و در یک نوار پایین/دکمه
با آیکون تک‌رنگ ثابت، یک نشان چندرنگ ناهماهنگ می‌بود. دست‌نخورده ماندند.

**Splash Screen**: هیچ فایلی در این کدبیس وجود ندارد (تأیید‌شده با grep).
**آیکون اپ Launcher**: `res/drawable/ic_launcher_foreground.xml` هم یک
Placeholder موقت است («دایره‌ی ساده به‌عنوان آیکون موقت (لنز دوربین)»، کامنت
صریح خودِ فایل) — طبق دستور صریح این قدم («بدون تأیید من تغییرش نده»)
دست‌نخورده ماند؛ فقط همین‌جا گزارش شد.

**یافته‌ی مستقل — سومین محل مستند mockup اصلاً پیاده نشده**: `docs/design/
README.md` بخش Assets سه محل رسمی نشان را نام می‌برد: «header (root
screens)»، «drawer header»، و «Settings About card». دو محل اول/سوم در این
قدم رفع شدند؛ اما با grep در `NavDrawer.kt` تأیید شد **هیچ هدر/بخش برندینگ
ای اصلاً در این فایل وجود ندارد** (نه یک آیکون موقت که باید تعویض شود — یک
بخش UI کاملاً غایب که باید از صفر ساخته شود، کاری بزرگ‌تر از «تعویض آیکون
موقت»). این خارج از دامنه‌ی این قدم ماند و دست‌نخورده باقی ماند؛ فقط گزارش
شد. یعنی یافته‌ی appendix #۸ («آیکون موقت») در همه‌ی محل‌های شناخته‌شده‌اش
بسته شد، اما «drawer header» به‌عنوان یک قابلیت کاملاً غایب همچنان یک قدم
جداگانه (طراحی UI تازه، نه رفع آیکون) می‌طلبد.

## تست

بدون تست تازه (طبق دستور کار — تغییر بصری خالص). تست‌های موجود
`HomeResumeLayoutFlowTest.kt`/`HomeScreenBackgroundImageTest.kt`/
`HomeProjectsStudioFlowTest.kt`/`SettingsFlowTest.kt` بدون تغییر سبز ماندند
(تأیید می‌کند Vector Drawable معتبر است و کامپایل/رندر واقعی صفحات می‌شکند).

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (پوشه‌های home/settings + HomeProjectsStudioFlowTest) | ۱۹ تست، ۰ شکست |
| `gradle :app:testDebugUnitTest` (کل Suite) | موفق |
| `gradle :app:assembleDebug` | موفق |
