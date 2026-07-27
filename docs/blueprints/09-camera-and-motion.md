# واحد ۰۹: دوربین و حرکت (Camera & Motion System)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Camera System + Motion Intensity Controller
**وضعیت:** فعال
**وابستگی:** Shot Engine

Motion Intensity عملاً یک پارامتر اضافه روی همان تنظیمات دوربین/سوژه است، نه یک سیستم مجزا با state خودش. چون خروجی این پروژه فقط پرامپت متنی است، حرکات پیشرفته‌ی دوربین (Orbit، Drone Path، Dolly Zoom) هیچ هزینه‌ی پردازشی اضافه ندارند — پیچیدگی اجرای واقعی این حرکات کاملاً بر عهده‌ی مدل ویدیوساز است، نه این اپ.

---

## بخش الف — Camera System

### پارامترهای پایه

| پارامتر | گزینه‌ها | پیش‌فرض |
|---|---|---|
| `angle` | Eye-level, Low, High, Overhead, Dutch, POV | Eye-level |
| `distance` | Extreme Wide, Wide, Medium, Close-up, Extreme Close-up | Medium |
| `lens_type` | Ultra-wide (14-24mm), Wide (24-35mm), Standard (35-50mm), Portrait (85-135mm), Telephoto (135mm+) | Standard |
| `depth_of_field` | Shallow, Medium, Deep | Medium |
| `focus_mode` | Auto, Manual, Subject Tracking, Rack Focus | Subject Tracking |
| `stabilization` | Tripod, Gimbal, Handheld | Gimbal |
| `framing` | Rule of Thirds, Centered, Symmetrical, Asymmetrical | Rule of Thirds |

### حرکات دوربین — پایه + پیشرفته (بدون کاهش عمق)

**پایه:** Static, Pan Left/Right, Tilt Up/Down, Dolly In/Out, Tracking, Crane, Handheld

**پیشرفته (v1.1، حفظ‌شده کامل):**

| حرکت | پارامترها | احساس |
|---|---|---|
| **Orbit** | `orbit_degrees` (90/180/360)، `orbit_speed`، `maintain_eye_level` | نمایش همه‌جانبه، معرفی کاراکتر |
| **Drone Path** | `altitude_change` (ascending/descending/level)، `path_type` (straight/curved/spiral) | آزادی، دید هوایی |
| **Dolly Zoom** | `focal_start`, `focal_end`, `dolly_direction` | گیجی، شوک دراماتیک (Vertigo Effect) |
| **Handheld Shake** | `shake_intensity` (۰-۱۰), `shake_frequency` | اضطراب، فوریت، واقع‌گرایی |
| **Compound** | `primary_movement`, `secondary_movement`, `sync` | ترکیب دو حرکت (مثل Dolly In + Orbit) |

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class CameraAngle { EYE_LEVEL, LOW, HIGH, OVERHEAD, DUTCH, POV }
enum class CameraDistance { EXTREME_WIDE, WIDE, MEDIUM, CLOSE_UP, EXTREME_CLOSE_UP }
enum class LensType { ULTRA_WIDE, WIDE, STANDARD, PORTRAIT, TELEPHOTO }
enum class DepthOfField { SHALLOW, MEDIUM, DEEP }
enum class FocusMode { AUTO, MANUAL, SUBJECT_TRACKING, RACK_FOCUS }
enum class Stabilization { TRIPOD, GIMBAL, HANDHELD }
enum class Framing { RULE_OF_THIRDS, CENTERED, SYMMETRICAL, ASYMMETRICAL }

enum class BasicMovementType { STATIC, PAN_LEFT, PAN_RIGHT, TILT_UP, TILT_DOWN, DOLLY_IN, DOLLY_OUT, TRACKING, CRANE, HANDHELD }
enum class AdvancedMovementType { ORBIT, DRONE_PATH, DOLLY_ZOOM, HANDHELD_SHAKE, COMPOUND }

sealed class CameraMovement {
    data class Basic(val type: BasicMovementType, val speed: String = "medium") : CameraMovement()
    data class Orbit(val degrees: Int, val speed: String, val maintainEyeLevel: Boolean) : CameraMovement()
    data class DronePath(val altitudeChange: String, val pathType: String, val speed: String) : CameraMovement()
    data class DollyZoom(val focalStart: Int, val focalEnd: Int, val direction: String) : CameraMovement()
    data class HandheldShake(val intensity: Int, val frequency: String) : CameraMovement()
    data class Compound(val primary: String, val secondary: String, val sync: String) : CameraMovement()
}

data class CameraSettings(
    val angle: CameraAngle,
    val distance: CameraDistance,
    val movement: CameraMovement,
    val lensType: LensType,
    val depthOfField: DepthOfField,
    val focusMode: FocusMode,
    val stabilization: Stabilization,
    val framing: Framing
)
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| لنز Ultra-wide + Close-up/Extreme Close-up | اعوجاج شدید چهره | **Blocking** |
| Static Movement + Handheld Stabilization | ناسازگاری منطقی | **Warning** |
| Rack Focus با کمتر از ۲ Subject | Rack Focus نیاز به حداقل دو سوژه دارد | **Blocking** |
| Extreme Wide + Very Shallow DoF | فیزیکاً دشوار | **Warning** |
| مدت حرکت دوربین بیشتر از Duration شات | ناسازگاری زمانی | **Blocking** |

---

## بخش ب — Motion Intensity (شدت حرکت)

### پارامترهای Subject Motion

| پارامتر | گزینه‌ها |
|---|---|
| `speed` | Slow, Normal, Fast, Hyperkinetic |
| `intensity` | ۰ تا ۱۰ |
| `motion_type` | Continuous, Intermittent, Sudden, Oscillating |

### پارامترهای Camera Motion Speed

| گزینه | احساس | کاربرد |
|---|---|---|
| **Smooth** | سینمایی، آرام | صحنه‌های احساسی |
| **Natural** | واقع‌گرایانه | اکثر صحنه‌ها |
| **Quick** | پرانرژی، فوری | اکشن، تعقیب |
| **Whip** | شوک، جهش زمانی | انتقال زمان/مکان |

### Motion Blur

| گزینه | Shutter Angle | کاربرد |
|---|---|---|
| None | 0° | گرافیک، فریز فریم |
| Subtle | 90–120° | صحنه‌ی آرام، دیالوگ |
| Cinematic | 180° (استاندارد سینما) | اکثر صحنه‌ها |
| Extreme | 270–360° | اکشن شدید |

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class SubjectSpeed { SLOW, NORMAL, FAST, HYPERKINETIC }
enum class MotionType { CONTINUOUS, INTERMITTENT, SUDDEN, OSCILLATING }
enum class CameraMotionSpeed { SMOOTH, NATURAL, QUICK, WHIP }
enum class MotionBlurAmount { NONE, SUBTLE, CINEMATIC, EXTREME }
enum class SubjectCameraSync { MATCHED, INDEPENDENT, CONTRASTING }

data class SubjectMotion(val speed: SubjectSpeed, val intensity: Int, val motionType: MotionType)
data class CameraMotion(val speed: CameraMotionSpeed, val movementStyle: String)
data class MotionBlur(val amount: MotionBlurAmount)

data class MotionIntensitySettings(
    val subjectMotion: SubjectMotion,
    val cameraMotion: CameraMotion,
    val motionBlur: MotionBlur,
    val subjectCameraSync: SubjectCameraSync = SubjectCameraSync.MATCHED
)

/** بررسی سازگاری سرعت و شدت — تناقض‌ها همیشه Warning هستند. */
fun validateSpeedIntensity(motion: SubjectMotion): ValidationIssue? {
    return when {
        motion.speed == SubjectSpeed.SLOW && motion.intensity > 6 ->
            ValidationIssue(Severity.WARNING, message = "حرکت آرام با Intensity بالا تناقض دارد")
        motion.speed == SubjectSpeed.HYPERKINETIC && motion.intensity < 7 ->
            ValidationIssue(Severity.WARNING, message = "حرکت Hyperkinetic باید Intensity بالا داشته باشد (حداقل ۷)")
        else -> null
    }
}

fun validateMotionBlur(speed: SubjectSpeed, blur: MotionBlurAmount): ValidationIssue? {
    return when {
        speed == SubjectSpeed.SLOW && blur == MotionBlurAmount.EXTREME ->
            ValidationIssue(Severity.WARNING, message = "Blur شدید برای حرکت آرام غیرطبیعی است")
        speed == SubjectSpeed.HYPERKINETIC && blur == MotionBlurAmount.NONE ->
            ValidationIssue(Severity.WARNING, message = "حرکت بسیار سریع بدون Blur غیرواقعی است")
        else -> null
    }
}
```

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| Speed=Slow + Intensity>6 | تناقض سرعت/شدت | **Warning** |
| Speed=Hyperkinetic + Intensity<7 | تناقض سرعت/شدت | **Warning** |
| Speed=Slow + Blur=Extreme | Blur نامتناسب | **Warning** |
| Speed=Hyperkinetic + Blur=None | فقدان Blur لازم | **Warning** |

---

## معیارهای موفقیت

- تمام حرکات دوربین (پایه + پیشرفته) بدون کاهش عمق پیاده‌سازی شده‌اند.
- ترکیبات فنی ناسازگار (لنز/فاصله، Rack Focus بدون سوژه‌ی کافی) به‌درستی Blocking هستند.
- Motion Speed/Intensity/Blur به‌طور منطقی با هم سازگارند (Warning در صورت ناهماهنگی).
- Camera Motion Speed می‌تواند با Subject Speed هماهنگ (Matched)، مستقل، یا متضاد باشد.
