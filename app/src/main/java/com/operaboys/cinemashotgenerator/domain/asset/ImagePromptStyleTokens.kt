package com.operaboys.cinemashotgenerator.domain.asset

import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.visualidentity.combineStyles
import com.operaboys.cinemashotgenerator.domain.visualidentity.toStyleReference

// فیچر مستقل «پرامپت ساخت عکس مرجع» — زیرقدم ۵ از ۵ (ADR-135): DRY کوچک —
// `styleTokensOf` در ImagePromptEngine.kt (ADR-132) private است و آن فایل طبق
// قانون این فیچر نباید تغییر کند؛ زیرقدم ۴ (ADR-134) به‌ناچار یک نسخه‌ی محلی
// در CharacterAssetFormViewModel.kt ساخت. این زیرقدم، پیش از افزودن نسخه‌ی
// سوم برای Location/Object، همان منطق را اینجا (public، در دسترس هر سه
// ViewModel) متمرکز کرد؛ نسخه‌ی محلی CharacterAssetFormViewModel.kt حذف و به
// این تابع مشترک تغییر داده شد.
fun styleTokensForImagePrompt(projectDna: ProjectDna): String {
    val coreIdentity = projectDna.coreIdentity
    return combineStyles(
        primary = coreIdentity.dominantVisualStyle.toStyleReference(),
        secondary = coreIdentity.secondaryStyle?.toStyleReference(),
        influence = coreIdentity.influence
    )
}
