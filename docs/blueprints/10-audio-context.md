# واحد ۱۰: تولید صوتی (Audio Context Generator)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال
**وابستگی:** Scene Conditions System (واحد ۰۸)، Shot Engine (واحد ۰۵)

**نسخه:** ۲ — یادداشت هماهنگی (بدون تغییر ساختاری در این فایل): در بازبینی معماری مستقل، هم‌نامی `AmbientSound` بین این واحد (۴ فیلد شامل `source`) و بلوپرینت ۰۵ (که قبلاً فقط ۳ فیلد بدون `source` داشت) به‌عنوان یک تناقض بحرانی شناسایی شد. بلوپرینت ۰۵ (نسخه ۳) اصلاح شد تا فیلد `source: String = "auto_generated"` را هم داشته باشد — اکنون این دو `AmbientSound` کاملاً هم‌ساختارند (فقط پیش‌فرض متفاوت دارند که مشکلی ایجاد نمی‌کند). `AmbientSoundSuggestion` (بلوپرینت ۰۸) یک نوع کاملاً مجزا و عمداً هم‌نام نیست — نقش متفاوتی دارد (پیشنهاد اولیه‌ی Pipeline، نه نوع ذخیره‌شده‌ی نهایی). **تغییرات با «🆕v2» علامت‌گذاری شده‌اند.**

---

## تعریف

تولید توصیفات صوتی برای هر Shot — چون خروجی این ابزار ویدیو است، صدا (محیطی، رویدادی، کاراکتر) جزو جدایی‌ناپذیر توصیف کامل صحنه است، نه یک افزودنی اختیاری.

## اصل بنیادی: تفکیک Ambient/Action خودکار از Character دستی

```
Ambient Sounds  → خودکار (از آب‌وهوا و مکان استنباط می‌شود)  → قابل Override
Action Sounds   → خودکار (از اکشن فیزیکی Shot استنباط می‌شود) → قابل Override
Character Sounds → همیشه دستی → هرگز خودکار تولید نمی‌شود
```

**دلیل این تفکیک:** صدای محیط (باران، ترافیک) یک واقعیت فیزیکی قابل‌استنباط از Context است — دقیقاً مثل انتخاب خودکار Outfit بر اساس آب‌وهوا. اما صدای کاراکتر (نوع نفس، لحن، دیالوگ) یک **تصمیم خلاقانه** است که نباید توسط سیستم حدس زده شود.

---

## ساختار داده

```json
{
  "audio_context_id": "audio_001",
  "shot_id": "shot_001",
  "ambient_generation_mode": "auto",
  "ambient_sounds": [
    { "type": "rain", "intensity": "heavy", "description": "heavy rain on surfaces", "source": "weather", "continuous": true }
  ],
  "action_sounds": [
    { "timestamp": 2.5, "type": "footstep", "description": "footsteps on wet pavement", "source": "character_action" }
  ],
  "character_sounds": [
    { "character_id": "char_001", "type": "breathing", "description": "heavy breathing (تعریف‌شده توسط کاربر)", "source": "user_defined" }
  ],
  "sound_mixing": { "total_sound_layers": 3, "dominant_sound": "rain" }
}
```

**نکته:** `character_sounds[].source` همیشه `"user_defined"` است — این فیلد هرگز به‌صورت خودکار توسط سیستم پر نمی‌شود.

---

## پیاده‌سازی مفهومی (Kotlin)

```kotlin
data class AmbientSound(val type: String, val intensity: String, val description: String, val source: String)
data class ActionSound(val timestampSeconds: Float, val type: String, val description: String)
data class CharacterSound(val characterId: String, val type: String, val description: String)  // همیشه source=user_defined

data class AudioContext(
    val audioContextId: String,
    val shotId: String,
    val ambientSounds: List<AmbientSound> = emptyList(),
    val actionSounds: List<ActionSound> = emptyList(),
    val characterSounds: List<CharacterSound> = emptyList()  // خالی می‌ماند مگر کاربر پر کند
)

// AudioContext رکورد ذخیره‌شده و مستقل این واحد است؛ Shot.soundProfile (واحد ۰۵) کپی/نمای موازی همان داده در سطح Shot است.
// PromptGenerationInput.audioContext مستقیماً از این جدول (نه از Shot.soundProfile) خوانده می‌شود.

/** تولید خودکار صدای محیطی از آب‌وهوا — منطق کامل در واحد ۰۸ (Scene Conditions). */
fun generateWeatherSounds(weatherType: String, intensity: String): List<AmbientSound> {
    return when (weatherType) {
        "rain" -> listOf(AmbientSound("rain", intensity, "$intensity rain on surfaces", "weather"))
        "storm" -> listOf(
            AmbientSound("rain", "heavy", "torrential rain and wind", "weather"),
            AmbientSound("thunder", "high", "loud thunder cracks", "weather")
        )
        "fog" -> listOf(AmbientSound("ambient", "low", "eerie silence with muffled sounds", "weather"))
        else -> emptyList()
    }
}

/** تولید خودکار صدای اکشن از توضیح فیزیکی Shot (نه از Character Sound). */
fun generateActionSounds(shotDescription: String, groundState: String): List<ActionSound> {
    val sounds = mutableListOf<ActionSound>()
    if (shotDescription.contains("walk", ignoreCase = true) || shotDescription.contains("run", ignoreCase = true)) {
        sounds += ActionSound(0f, "footstep", "footsteps on $groundState surface")
    }
    return sounds
}

/**
 * پیشنهادهای صدای کاراکتر — این توابع هرگز مستقیماً audioContext را
 * پر نمی‌کنند. فقط یک لیست پیشنهاد برای نمایش در UI برمی‌گردانند؛
 * کاربر باید صراحتاً یکی را انتخاب یا خودش description بنویسد.
 */
data class BreathingSuggestion(val intensity: String, val description: String)

fun suggestBreathingSounds(activity: String, emotion: String?): BreathingSuggestion {
    var intensity = "low"
    var description = "calm breathing"
    if (activity.contains("running") || activity.contains("fighting")) {
        intensity = "heavy"; description = "heavy breathing from exertion"
    }
    if (emotion == "terrified" || emotion == "panicked") {
        intensity = "high"; description = "rapid panicked breathing"
    }
    return BreathingSuggestion(intensity, description)
    // نکته: این فقط پیشنهاد است؛ افزودن واقعی به audioContext.characterSounds
    // فقط از طریق اقدام صریح کاربر (مثل addCharacterSound) اتفاق می‌افتد.
}

/** تولید کامل Audio Context برای یک Shot — بدون پر کردن character_sounds. */
fun generateAudioContext(
    shotId: String,
    shotDescription: String,
    weatherType: String,
    weatherIntensity: String,
    groundState: String
): AudioContext {
    return AudioContext(
        audioContextId = generateAudioId(),
        shotId = shotId,
        ambientSounds = generateWeatherSounds(weatherType, weatherIntensity),
        actionSounds = generateActionSounds(shotDescription, groundState),
        characterSounds = emptyList()  // عمداً خالی — طبق اصل بنیادی این واحد
    )
}
```

---

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| تعداد کل لایه‌های صوتی بیش از ۸ | پیچیدگی بیش‌ازحد صدا | **Warning** |
| Action Sound با timestamp خارج از Duration شات | ناسازگاری زمانی | **Blocking** |
| `character_sounds` با `source` غیر از `user_defined` | نقض اصل بنیادی این واحد (خطای پیاده‌سازی، نه ورودی کاربر) | ساختاری |

---

## معیارهای موفقیت

- Ambient و Action Sounds با آب‌وهوا/مکان/اکشن سازگار و خودکار تولید می‌شوند.
- Character Sounds هرگز به‌طور خودکار درج نمی‌شوند؛ همیشه انتخاب صریح کاربر است.
- حداکثر ۸ لایه‌ی صوتی برای جلوگیری از پیچیدگی بیش‌ازحد پرامپت.
- پیشنهادهای Breathing/Vocal فقط در UI نمایش داده می‌شوند، نه در خروجی نهایی (مگر کاربر بپذیرد).
