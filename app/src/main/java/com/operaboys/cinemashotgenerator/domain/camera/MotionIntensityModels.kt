package com.operaboys.cinemashotgenerator.domain.camera

// واحد ۰۹ — Motion Intensity (بخش ب، ساختار داده)
// منبع حقیقت: docs/blueprints/09-camera-and-motion.md
//
// NOTE: SubjectSpeed (چهارمقداره: SLOW/NORMAL/FAST/HYPERKINETIC) مفهوماً شبیه
// MotionLevel واحد ۰۵ (پنج‌مقداره: STATIC/SUBTLE/MODERATE/DYNAMIC/EXTREME) است، اما
// بلوپرینت این دو را با enum های کاملاً جداگانه تعریف کرده. طبق دستور کار صریح،
// این دو enum یکی/نگاشت نشدند — این یک هم‌پوشانی مفهومی یادداشت‌شده است، نه سؤال باز
// جدی (جزئیات در docs/adr/008-unit09-camera-motion-deviations.md).

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
