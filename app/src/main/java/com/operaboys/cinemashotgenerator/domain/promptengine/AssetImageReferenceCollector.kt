package com.operaboys.cinemashotgenerator.domain.promptengine

import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.shot.ImageReference

// فیچر مستقل «آپلود عکس مرجع واقعی Asset» — زیرقدم ۳ از ۳، پایانی (ADR-139):
// وصل خودکار عکس‌های مرجع آپلودشده‌ی زیرقدم‌های ۱/۲ (ADR-137/138) به
// imageReferences نهایی پرامپت هر Shot، کنار رفرنس‌های دستی موجود Shot
// Composer (assemblePromptBlueprint در PromptAssembly.kt، بدون تغییر آن
// مسیر). `characters`/`objects`/`locations` ورودی این تابع (از
// PromptGenerationInput) از قبل دقیقاً به همان Asset هایی فیلترشده‌اند که
// همین Shot استفاده می‌کند (PromptGenerationRepository.kt:
// loadCharacterAssets(shot.characterIds)/loadObjectAssets(shot.objectIds)/
// loadLocationAssets(resolveSceneLocation(shot, scene))) — پس هیچ فیلتر
// اضافه‌ای اینجا لازم نیست.
//
// انتخاب type: طبق راستی‌آزمایی مستقیم (grep در سراسر پروژه)،
// ImageReference.type امروز در هیچ‌جای مسیر Render واقعی خوانده نمی‌شود —
// فقط `imageReferences.size`/`isEmpty()` در buildReferenceImageInstruction
// (Renderer.kt) اثر دارند؛ خودِ type فقط برای مستندسازی/مصرف احتمالی
// آینده نگه داشته می‌شود. بین ۴ مقدار موجود ("character" | "style" |
// "composition" | "lighting")، Location به "composition" نگاشت شد (نزدیک‌ترین
// معادل معنایی — محیط/چیدمان صحنه). برای Object هیچ‌کدام از ۴ مقدار واقعاً
// مناسب «شیء فیزیکی» نیست؛ طبق دستور صریح («افزودن مقدار پنجم تصمیم
// طراحی بزرگ‌تری است، خودسرانه تصمیم نگیر») یک مقدار پنجم اضافه نشد —
// همان "composition" (کم‌غلط‌ترین گزینه‌ی موجود، چون شیء هم بخشی از
// چیدمان بصری صحنه است) به‌عنوان راه‌حل موقت انتخاب شد؛ این یک شکاف
// معنایی شناخته‌شده است، نه یک تصمیم قطعی — چون امروز بدون اثر عملکردی
// واقعی است.
fun collectAssetImageReferences(
    characters: List<CharacterAsset>,
    objects: List<ObjectAsset>,
    locations: List<LocationAsset>
): List<ImageReference> {
    val characterRefs = characters.flatMap { character ->
        character.referenceImages.map { ImageReference(type = "character", localFilePath = it.localFilePath, description = it.description) }
    }
    val locationRefs = locations.flatMap { location ->
        location.referenceImages.map { ImageReference(type = "composition", localFilePath = it.localFilePath, description = it.description) }
    }
    val objectRefs = objects.flatMap { objectAsset ->
        objectAsset.referenceImages.map { ImageReference(type = "composition", localFilePath = it.localFilePath, description = it.description) }
    }
    return characterRefs + locationRefs + objectRefs
}
