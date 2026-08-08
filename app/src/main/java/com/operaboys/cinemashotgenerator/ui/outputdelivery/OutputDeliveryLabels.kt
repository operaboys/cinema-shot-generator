package com.operaboys.cinemashotgenerator.ui.outputdelivery

// واحد ۱۶ فاز ۵ — قدم ۳: نام نمایشی هر مدل — طبق فهرست دقیق «Model List» سند طراحی.
// این نام‌ها اسم خاص/تجاری‌اند (یکسان در فارسی و انگلیسی، دقیقاً هم‌الگو با نگه‌داشتن
// اصطلاحات فنی به لاتین در جای‌جای این اپ)، پس عمداً از سیستم ترجمه‌ی uiString عبور
// نمی‌کنند — یک تابع Kotlin ساده، نه کلید UiStrings تکراری برای یک متن یکسان.
fun modelProfileDisplayName(profileId: String): String = when (profileId) {
    "universal_default" -> "Universal Default"
    "veo_3_1" -> "Veo 3.1"
    "kling_3_0" -> "Kling 3.0"
    "seedance_2_5" -> "Seedance 2.5"
    "happyhorse_1_0" -> "HappyHorse 1.0"
    "runway_gen_4_5" -> "Runway Gen-4.5"
    "luma_ray3_14" -> "Luma Ray3"
    "hailuo_2_3" -> "Hailuo 2.3"
    "wan_2_2" -> "Wan 2.2"
    "hunyuanvideo_1_5" -> "HunyuanVideo 1.5"
    "ltx_2_3" -> "LTX 2.3"
    "vidu_q3" -> "Vidu Q3"
    "midjourney_v7" -> "Midjourney v7"
    "stable_diffusion_sd3" -> "Stable Diffusion SD3"
    else -> profileId
}
