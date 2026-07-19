package com.operaboys.cinemashotgenerator.domain.sceneconditions

// واحد ۰۸ — Environment-to-Sound Mapping
// منبع حقیقت: docs/blueprints/08-scene-conditions.md
// دقیقاً طبق کد مفهومی بلوپرینت.

/**
 * تولید خودکار صدای محیطی از شرایط آب‌وهوا — این خروجی مستقیماً به
 * ambient_sounds در Sound Profile (واحد ۰۵) می‌رود، چون Ambient
 * همیشه خودکار است (برخلاف Character Sound که همیشه دستی می‌ماند).
 */
fun mapEnvironmentToSound(
    weatherType: String,
    weatherIntensity: String,
    windStrength: String
): List<AmbientSoundSuggestion> {
    val sounds = mutableListOf<AmbientSoundSuggestion>()
    when (weatherType) {
        "rain" -> sounds += AmbientSoundSuggestion("rain", weatherIntensity, "$weatherIntensity rain on surfaces")
        "storm" -> {
            sounds += AmbientSoundSuggestion("rain", "heavy", "torrential rain and wind")
            sounds += AmbientSoundSuggestion("thunder", "high", "loud thunder cracks")
        }
        "snow" -> sounds += AmbientSoundSuggestion("wind", "low", "gentle wind through snow")
        "fog" -> sounds += AmbientSoundSuggestion("ambient", "low", "eerie silence with muffled sounds")
    }
    if (windStrength != "none") {
        val intensity = if (windStrength == "strong") "high" else "medium"
        sounds += AmbientSoundSuggestion("wind", intensity, "$windStrength wind blowing")
    }
    return sounds
}
