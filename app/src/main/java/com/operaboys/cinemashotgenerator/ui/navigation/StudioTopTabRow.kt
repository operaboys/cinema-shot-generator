package com.operaboys.cinemashotgenerator.ui.navigation

import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowState
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowStep
import com.operaboys.cinemashotgenerator.domain.workflow.canJumpToStep
import com.operaboys.cinemashotgenerator.ui.i18n.uiString

// واحد ۱۶ — فاز ۰: Top Tab Row درون‌پروژه‌ای — طبق DDR-002/بلوپرینت ۱۶ نسخه ۷: فقط
// وقتی مسیر فعلی داخل Studio است نمایش داده می‌شود، اضافه بر نوار پایین (نه
// جایگزینش)؛ ۴ تب: داستان/DNA/صحنه‌ها/خروجی.
//
// StudioTab یک enum سطح UI محض است (نه domain) — تعداد Tab (۴) یک تصمیم Navigation
// bilayer است، جدا از دانه‌بندی دقیق‌تر ۹-مرحله‌ای WorkflowStep دامنه؛ به همین دلیل
// در ui/navigation نگه داشته شد نه در domain/workflow (برخلاف AppTheme/...، که واقعاً
// State سراسری Persist‌شونده‌اند، نه یک گروه‌بندی محض ناوبری). محل State انتخاب Tab
// فعلی هم rememberSaveable در Composable مصرف‌کننده (Studio Shell، فاز بعدی) است —
// طبق تفکیک صریح docs/design/README.md بخش State Management (پایین ADR-042).

enum class StudioTab { STORY, DNA, SCENES, OUTPUT }

/**
 * نگاشت هر Tab به یک WorkflowStep نماینده، فقط برای فراخوانی canJumpToStep (که
 * جابه‌جایی را هرگز Block نمی‌کند، فقط هشدار می‌دهد — طبق ADR-037). چون Tab «داستان»
 * دو مرحله (STORY_WIZARD+AI_STORY_BREAKDOWN) و «صحنه‌ها» دو مرحله
 * (SCENE_CREATION+SHOT_CREATION) و «خروجی» سه مرحله (VALIDATION+PROMPT_GENERATION+
 * OUTPUT_DELIVERY) را می‌پوشانند، اولین مرحله‌ی هرکدام به‌عنوان نماینده انتخاب شد —
 * چون canJumpToStep فقط به «آیا مراحل قبل از target کامل‌اند» نگاه می‌کند، اولین
 * مرحله‌ی گروه دقیقاً همان مرزی است که باید بررسی شود.
 */
private val studioTabRepresentativeStep: Map<StudioTab, WorkflowStep> = mapOf(
    StudioTab.STORY to WorkflowStep.STORY_WIZARD,
    StudioTab.DNA to WorkflowStep.DNA_CONFIG,
    StudioTab.SCENES to WorkflowStep.SCENE_CREATION,
    StudioTab.OUTPUT to WorkflowStep.VALIDATION
)

/**
 * آیا جابه‌جایی به این Tab باید هشدار نشان دهد؟ اگر هنوز هیچ WorkflowState واقعی‌ای
 * موجود نیست (Session شروع نشده)، هیچ‌چیزی برای قضاوت وجود ندارد — بدون هشدار.
 */
fun evaluateStudioTabJump(workflowState: WorkflowState?, tab: StudioTab): Pair<Boolean, String?> {
    val state = workflowState ?: return true to null
    val targetStep = studioTabRepresentativeStep.getValue(tab)
    return canJumpToStep(state, targetStep)
}

@Composable
fun StudioTopTabRow(
    selectedTab: StudioTab,
    workflowState: WorkflowState?,
    language: Language,
    onTabSelected: (StudioTab) -> Unit,
    onWarning: (String) -> Unit
) {
    SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
        StudioTab.entries.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = {
                    val (allowed, warning) = evaluateStudioTabJump(workflowState, tab)
                    if (allowed) {
                        warning?.let(onWarning)
                        onTabSelected(tab)
                    }
                },
                text = { Text(uiString(studioTabLabelKey(tab), language)) }
            )
        }
    }
}

private fun studioTabLabelKey(tab: StudioTab): String = when (tab) {
    StudioTab.STORY -> "studioTab.story"
    StudioTab.DNA -> "studioTab.dna"
    StudioTab.SCENES -> "studioTab.scenes"
    StudioTab.OUTPUT -> "studioTab.output"
}
