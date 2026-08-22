package com.operaboys.cinemashotgenerator.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.storybreakdown.AiConnectorProfile
import com.operaboys.cinemashotgenerator.domain.storybreakdown.GEMINI_API_PROFILE
import com.operaboys.cinemashotgenerator.ui.assets.OpaqueChip
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme

// فیچر مستقل «ترجمه‌ی مجدد با AI» (ADR-124، جدا از برنامه‌ی سه‌قدمی Preview
// دوزبانه‌ی ADR-121 تا ۱۲۳) — اولین کامپوننت UI مشترک این پروژه (تأییدشده با
// grep: تا این قدم هیچ پوشه‌ی ui.common/کامپوننت مشترک بین چند پکیج UI وجود
// نداشت). چهار فرم (CharacterAssetFormScreen/LocationAssetFormScreen/
// ObjectAssetFormScreen/ShotComposerScreen، در دو پکیج جدای ui.assets/ui.shots)
// همگی این یک Composable عمومی (نه private/internal) را صدا می‌زنند — منطق
// ساخت پرامپت/فراخوان HTTP فقط یک‌بار در Domain Layer (translateToFarsi،
// domain/storybreakdown/AiConnector.kt) نوشته شده؛ این فایل فقط UI است.
//
// OpaqueChip (ui/assets/AssetsScreen.kt) بازاستفاده شد — internal در Kotlin
// یعنی «قابل‌مشاهده در کل ماژول»، نه فقط همان پکیج، پس از ui.common هم قابل‌
// Import است؛ نیازی به کپی/تعریف تازه نبود.
//
// دکمه شکل «آیکون + متن کوتاه» دارد (نه فقط آیکون تنها، نه کارت مستقل) — طبق
// تصمیم محصولی صریح. چیپ‌های انتخاب پروفایل فقط وقتی بیش از یک پروفایل وجود
// دارد نمایش داده می‌شوند (هم‌الگو دقیق با GeneratedPromptCard در
// AiStoryBreakdownScreen.kt). چیپ Gemini یک برچسب کوچک «رایگان»/"Free" کنارش
// دارد — طبق تصمیم محصولی: Gemini تنها پروفایل با یک لایه‌ی رایگان دائمی است
// (نه فقط یک اعتبار اولیه‌ی یک‌بارمصرف مثل رقبا)، جزئیات کامل تحقیق در ADR-124.
@Composable
fun RetranslateButton(
    profiles: List<AiConnectorProfile>,
    selectedProfileId: String,
    onSelectProfile: (String) -> Unit,
    apiKeySaved: Boolean,
    inProgress: Boolean,
    onRetranslate: () -> Unit,
    language: Language,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (profiles.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { profile ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OpaqueChip(
                            label = profile.displayName,
                            selected = profile.profileId == selectedProfileId,
                            onClick = { onSelectProfile(profile.profileId) },
                            testTag = retranslateProfileChipTag(profile.profileId)
                        )
                        if (profile.profileId == GEMINI_API_PROFILE.profileId) {
                            Text(
                                text = uiString("retranslate.freeLabel", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = CinemaTheme.extendedColors.fg3
                            )
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onRetranslate,
            enabled = apiKeySaved && !inProgress,
            modifier = Modifier.fillMaxWidth().testTag(RETRANSLATE_BUTTON_TAG)
        ) {
            if (inProgress) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(uiString("retranslate.buttonLabel", language))
        }
        if (!apiKeySaved) {
            Text(
                text = uiString("retranslate.noKeyHint", language),
                style = MaterialTheme.typography.labelSmall,
                color = CinemaTheme.extendedColors.fg3
            )
        }
    }
}

const val RETRANSLATE_BUTTON_TAG = "retranslate.button"
fun retranslateProfileChipTag(profileId: String): String = "retranslate.profileChip.$profileId"
