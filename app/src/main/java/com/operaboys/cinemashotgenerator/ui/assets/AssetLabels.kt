package com.operaboys.cinemashotgenerator.ui.assets

import com.operaboys.cinemashotgenerator.domain.asset.AssetType
import com.operaboys.cinemashotgenerator.domain.asset.CharacterContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.LocationContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.PropContinuityLevel
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

/** یافته‌ی #۱۱ appendix ADR-081 (ADR-085) — Tab «دارایی‌ها»ی Scene Detail اولین مصرف‌کننده‌ی این تابع است. */
fun assetTypeLabel(type: AssetType, language: Language): String = uiString(
    when (type) {
        AssetType.CHARACTER -> "assetKind.character"
        AssetType.LOCATION -> "assetKind.location"
        AssetType.OBJECT -> "assetKind.object"
    },
    language
)

// واحد ۱۶ فاز ۳ — قدم ۱: نگاشت enum های واحد ۰۶ (Asset & Continuity) به کلید
// ترجمه، هم‌الگو با ui/dna/DnaLabels.kt.

fun characterTierLabel(tier: CharacterTier, language: Language): String = uiString(
    when (tier) {
        CharacterTier.MAIN -> "characterTier.main"
        CharacterTier.SECONDARY -> "characterTier.secondary"
        CharacterTier.BACKGROUND -> "characterTier.background"
    },
    language
)

fun locationTypeLabel(type: LocationType, language: Language): String = uiString(
    when (type) {
        LocationType.INDOOR -> "locationType.indoor"
        LocationType.OUTDOOR -> "locationType.outdoor"
        LocationType.MIXED -> "locationType.mixed"
        LocationType.CUSTOM -> "locationType.custom"
    },
    language
)

fun objectSubtypeLabel(subtype: ObjectSubtype, language: Language): String = uiString(
    when (subtype) {
        ObjectSubtype.PERSONAL_PROP -> "objectSubtype.personalProp"
        ObjectSubtype.GENERAL_PROP -> "objectSubtype.generalProp"
        ObjectSubtype.COSTUME -> "objectSubtype.costume"
    },
    language
)

fun characterContinuityLevelLabel(level: CharacterContinuityLevel, language: Language): String = uiString(
    when (level) {
        CharacterContinuityLevel.FULL -> "characterContinuityLevel.full"
        CharacterContinuityLevel.MEDIUM -> "characterContinuityLevel.medium"
        CharacterContinuityLevel.NONE -> "characterContinuityLevel.none"
    },
    language
)

/** [LocationContinuityLevel] فقط یک مقدار دارد (`STYLE`) — تابع فقط برای یکدستی امضا با بقیه‌ی سطوح، نه به این معنا که Dropdown ای پشت آن لازم است. */
fun locationContinuityLevelLabel(level: LocationContinuityLevel, language: Language): String = uiString(
    when (level) {
        LocationContinuityLevel.STYLE -> "locationContinuityLevel.style"
    },
    language
)

/** [PropContinuityLevel] فقط یک مقدار دارد (`FORM`) — همان دلیل بالا. */
fun propContinuityLevelLabel(level: PropContinuityLevel, language: Language): String = uiString(
    when (level) {
        PropContinuityLevel.FORM -> "propContinuityLevel.form"
    },
    language
)

/** واحد ۱۶ فاز ۳ — قدم ۲: فرم Character اولین مصرف‌کننده‌ی [Gender] در UI است. */
fun genderLabel(gender: Gender, language: Language): String = uiString(
    when (gender) {
        Gender.FEMALE -> "gender.female"
        Gender.MALE -> "gender.male"
        Gender.OTHER -> "gender.other"
    },
    language
)
