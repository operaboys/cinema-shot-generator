package com.operaboys.cinemashotgenerator.domain.outputdelivery

// واحد ۱۴ — Output Delivery System (بخش الف: Model Profile Library — تکمیل)
// منبع حقیقت: docs/blueprints/14-output-delivery.md
//
// طبق تصمیم معمار: بلوپرینت ۱۴ دست‌نخورده ماند — خودش صراحتاً تأکید کرده جدول
// مدل‌هایش «نقطه‌ی شروع، نه فهرست نهایی» است و معماری باید Data-driven باشد. این
// فایل دقیقاً همان اصل را با افزودن داده (val های ثابت Kotlin، نه تغییر سند) دنبال
// می‌کند. مدل Sora عمداً از این فهرست حذف شده (تصمیم صریح معمار).
//
// ⚠️ یادداشت صداقت داده: برای هر ۱۳ پروفایل، هر مقداری که مستقیماً از اطلاعات
// تأییدشده‌ی معمار (نام مدل، سازنده، ویژگی خاص، نوع فرمت) گرفته نشده — به‌خصوص
// maxPromptLength/maxTokens دقیق API — یک **تخمین محافظه‌کارانه و مستندشده** است، نه
// عدد رسمی مدل. جایی که عدد واقعی از خودِ بلوپرینت (نمونه‌ی Veo) در دسترس بود، همان
// استفاده شد. الگوی maxTokens ≈ maxPromptLength/4 از همان heuristic خودِ پروژه
// (estimateTokensFromCharacters، واحد ۱۳) پیروی می‌کند، برای هماهنگی داخلی، نه چون
// این یک استاندارد رسمی صنعتی است.
//
// جزئیات کامل هر تصمیم/تخمین در docs/adr/021-unit14-model-profiles-deviations.md.

/**
 * Veo 3.1 (Google) — بهترین گزینه‌ی سینمایی غربی با صدای همزمان با لب.
 * maxPromptLength=2000 مستقیماً از نمونه‌ی کامل Veo در خودِ بلوپرینت ۱۴ گرفته شد
 * (نه تخمین). maxTokens تخمینی (طبق heuristic بالا). supportsWeightedTags=false و
 * ساختار paragraph هم دقیقاً طبق همان نمونه‌ی بلوپرینت.
 * supportsImage=false (خروجی فقط ویدیو)؛ supportsImagePrompt=true (Image-to-Video
 * شناخته‌شده)؛ supportsNegativePrompt=false (بدون پارامتر رسمی Negative Prompt در
 * Veo API — تخمین محافظه‌کارانه).
 */
val veoProfile = ModelProfile(
    profileId = "veo_3_1",
    platform = "veo",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = false
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true, "enforce_camera_description" to true)
)

/**
 * Kling 3.0/Turbo (Kuaishou) — صدر جدول کیفیت جولای ۲۰۲۶، ۴K/۶۰fps، Lip-sync تا ۷ زبان.
 * تمام اعداد (maxPromptLength=2500) تخمینی‌اند — کمی سخاوتمندانه‌تر از Veo، چون Kling
 * به قبول توصیف‌های تصویری بلند و جزئی معروف است؛ عدد رسمی API در دسترس نبود.
 * supportsNegativePrompt=true — Kling API رسماً پارامتر negative_prompt دارد.
 */
val klingProfile = ModelProfile(
    profileId = "kling_3_0",
    platform = "kling",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = true
    ),
    constraints = ModelConstraints(maxPromptLength = 2500, maxTokens = 625),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true)
)

/**
 * Seedance 2.0/2.5 (ByteDance) — رتبه‌ی بالای کیفیت خام و صدا؛ دسترسی از طریق
 * Doubao/fal.ai. profileId نسخه‌ی جدیدتر (۲.۵) را نشان می‌دهد. تمام اعداد تخمینی و
 * محافظه‌کارانه (هم‌تراز Veo) چون مستندات رسمی API در دسترس نبود.
 * supportsNegativePrompt=false — عدم‌قطعیت، تخمین محافظه‌کارانه.
 */
val seedanceProfile = ModelProfile(
    profileId = "seedance_2_5",
    platform = "seedance",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = false
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true)
)

/**
 * HappyHorse-1.0 (Alibaba ATH) — جدیدترین و صعودکننده، معماری Transformer، Lip-sync
 * ۷ زبانه. چون این مدل بسیار تازه است، هیچ عدد فنی مستندی در دسترس نیست — تمام
 * constraints عیناً هم‌تراز پروفایل universal_default گذاشته شد (کاملاً تخمینی،
 * محافظه‌کارترین حالت ممکن).
 */
val happyHorseProfile = ModelProfile(
    profileId = "happyhorse_1_0",
    platform = "happyhorse",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = false
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true)
)

/**
 * Runway Gen-4.5 — بهترین کنترل خلاقانه (Motion Brushes، Camera Controls)، فرمت
 * Structured Text (نه JSON خام) — type محلی جدید "structured_text" (نوع رشته‌ای آزاد
 * ModelFormat.type، بدون نیاز به enum). maxPromptLength=1000 تخمینی — Runway به
 * پرامپت‌های کوتاه‌تر و متمرکزتر (نه توصیف بلند سینمایی) معروف است.
 * supportsNegativePrompt=true — Runway از نسل Gen-2/3 پارامتر Negative Prompt داشته.
 */
val runwayProfile = ModelProfile(
    profileId = "runway_gen_4_5",
    platform = "runway",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = true
    ),
    constraints = ModelConstraints(maxPromptLength = 1000, maxTokens = 250),
    format = ModelFormat(type = "structured_text", structure = "paragraph"),
    optimizationRules = mapOf("prefer_concise_prompts" to true)
)

/**
 * Luma Ray3/Ray3.14 — اولین HDR ۱۶-بیتی واقعی، قوی در Image-to-Video.
 * maxPromptLength=1500 تخمینی. supportsNegativePrompt=false — عدم‌قطعیت، تخمین
 * محافظه‌کارانه (Luma Dream Machine API به‌طور رسمی چنین پارامتری منتشر نکرده).
 */
val lumaProfile = ModelProfile(
    profileId = "luma_ray3_14",
    platform = "luma",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = false
    ),
    constraints = ModelConstraints(maxPromptLength = 1500, maxTokens = 375),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true)
)

/**
 * Hailuo (MiniMax) 2.3 — بهترین ثبات ظاهری کاراکتر بین شات‌ها.
 * maxPromptLength=2000 تخمینی. supportsNegativePrompt=true — MiniMax API رسماً
 * negative_prompt دارد.
 */
val hailuoProfile = ModelProfile(
    profileId = "hailuo_2_3",
    platform = "hailuo",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = true
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_character_consistency_tags" to true)
)

/**
 * Midjourney v6/v7 — فقط تصویر (نه ویدیو)، فرمت Command String با پیشوند واقعی
 * دستور Discord/API («/imagine prompt:»). platform="midjourney" عمداً دقیقاً همین
 * رشته است چون applyWeightSyntax (Renderer.kt، از قبل موجود) صراحتاً
 * `profile.platform == "midjourney"` را چک می‌کند تا نحو وزن‌دهی («tag::weight»)
 * اعمال شود — supportsWeightedTags=true اینجا واقعاً به رفتار متفاوت در Renderer
 * منجر می‌شود (تنها پروفایل این فهرست که این‌طور است).
 * supportsNegativePrompt=true — پارامتر --no.
 * maxPromptLength=1000 تخمین محافظه‌کارانه (پرامپت‌های واقعی Midjourney معمولاً
 * کوتاه و به‌صورت لیست ویژگی/پارامتر هستند، نه پاراگراف بلند).
 */
val midjourneyProfile = ModelProfile(
    profileId = "midjourney_v7",
    platform = "midjourney",
    capabilities = ModelCapabilities(
        supportsVideo = false,
        supportsImage = true,
        supportsWeightedTags = true,
        supportsImagePrompt = true,
        supportsNegativePrompt = true
    ),
    constraints = ModelConstraints(maxPromptLength = 1000, maxTokens = 250),
    format = ModelFormat(type = "command_string", commandPrefix = "/imagine prompt:"),
    optimizationRules = mapOf("prefer_concise_prompts" to true)
)

/**
 * Wan 2.2 (Alibaba) — بهترین کیفیت Open-weight. maxPromptLength=2000 تخمینی (مدل‌های
 * Open-weight معمولاً از قراردادهای پرامپت مشابه مدل‌های بسته پیروی می‌کنند).
 * supportsNegativePrompt=true — مدل‌های Diffusion-based Open-weight معمولاً این
 * پارامتر را دارند (تخمین با اطمینان متوسط).
 */
val wanProfile = ModelProfile(
    profileId = "wan_2_2",
    platform = "wan",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = true
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true)
)

/**
 * HunyuanVideo 1.5 (Tencent) — حرکت سینمایی قوی. maxPromptLength=2000 تخمینی.
 * supportsNegativePrompt=true — مدل Diffusion-based با پارامتر Negative Prompt
 * شناخته‌شده (تخمین با اطمینان متوسط).
 */
val hunyuanVideoProfile = ModelProfile(
    profileId = "hunyuanvideo_1_5",
    platform = "hunyuan",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = true
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("enforce_camera_description" to true)
)

/**
 * LTX-2.3 (Lightricks) — صدای استریو، کیفیت لوکال ۴K. maxPromptLength=2000 تخمینی.
 * supportsNegativePrompt=false — عدم‌قطعیت، تخمین محافظه‌کارانه.
 */
val ltxProfile = ModelProfile(
    profileId = "ltx_2_3",
    platform = "ltx",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = false
    ),
    constraints = ModelConstraints(maxPromptLength = 2000, maxTokens = 500),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true)
)

/**
 * Vidu Q3 (Shengshu) — کلیپ تا ۱۶ ثانیه. maxPromptLength=1500 تخمینی — کلیپ کوتاه‌تر،
 * تخمین محافظه‌کارانه‌تر نسبت به مدل‌های کلیپ بلندتر مثل Kling.
 * supportsNegativePrompt=false — عدم‌قطعیت، تخمین محافظه‌کارانه.
 */
val viduProfile = ModelProfile(
    profileId = "vidu_q3",
    platform = "vidu",
    capabilities = ModelCapabilities(
        supportsVideo = true,
        supportsImage = false,
        supportsWeightedTags = false,
        supportsImagePrompt = true,
        supportsNegativePrompt = false
    ),
    constraints = ModelConstraints(maxPromptLength = 1500, maxTokens = 375),
    format = ModelFormat(type = "json", structure = "paragraph"),
    optimizationRules = mapOf("prefer_paragraph_style" to true)
)

/**
 * Stable Diffusion XL / SD3 (Stability AI) — فقط تصویر، ControlNet.
 * فرمت: نه JSON و نه Command-string با پیشوند — پرامپت‌های واقعی SD/A1111/ComfyUI
 * فهرستی از برچسب‌های جدا با کاما هستند («tag soup»)، نه پاراگراف و نه JSON. با نوع
 * موجود ModelFormat، نزدیک‌ترین بازنمایی: type="plain_text" با structure="tags"
 * (هر مقداری غیر از "paragraph") — چون renderBlueprintToText برای هر structure غیر
 * از "paragraph" segments را با ", " (کاما) به‌جای ". " می‌چسباند؛ این دقیقاً معادل
 * سبک واقعی SD است، بدون نیاز به تغییر Renderer.
 * supportsWeightedTags=true — نحو وزن‌دهی معروف SD («(tag:1.2)») واقعاً وجود دارد؛
 * ⚠️ محدودیت شناخته‌شده: applyWeightSyntax (از قبل، در Renderer.kt) فقط
 * platform=="midjourney" را ویژه می‌کند، پس این پرچم فعلاً هیچ نحو خاصی برای SD
 * اعمال نمی‌کند — مستند شده، تغییر داده نشده (خارج از Scope این قدم).
 * maxPromptLength=500 — کوتاه‌تر از مدل‌های ویدیویی، چون محدودیت شناخته‌شده‌ی
 * Encoder متنی CLIP (۷۷ توکن در نسخه‌ی پایه) پرامپت‌های SD را عملاً کوتاه‌تر نگه
 * می‌دارد؛ SDXL/SD3 با Encoder دوگانه کمی بیشتر تحمل می‌کنند اما همچنان کوتاه‌تر از
 * توصیف‌های سینمایی بلند مدل‌های ویدیویی است.
 */
val stableDiffusionProfile = ModelProfile(
    profileId = "stable_diffusion_sd3",
    platform = "stable_diffusion",
    capabilities = ModelCapabilities(
        supportsVideo = false,
        supportsImage = true,
        supportsWeightedTags = true,
        supportsImagePrompt = true,
        supportsNegativePrompt = true
    ),
    constraints = ModelConstraints(maxPromptLength = 500, maxTokens = 125),
    format = ModelFormat(type = "plain_text", structure = "tags"),
    optimizationRules = mapOf("prefer_tag_style" to true)
)

/** تمام پروفایل‌های موجود پروژه — universal_default + ۱۳ مدل واقعی بازار. */
val ALL_MODEL_PROFILES: List<ModelProfile> = listOf(
    universalDefaultProfile,
    veoProfile,
    klingProfile,
    seedanceProfile,
    happyHorseProfile,
    runwayProfile,
    lumaProfile,
    hailuoProfile,
    midjourneyProfile,
    wanProfile,
    hunyuanVideoProfile,
    ltxProfile,
    viduProfile,
    stableDiffusionProfile
)
