package com.operaboys.cinemashotgenerator.ui.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import com.operaboys.cinemashotgenerator.ui.theme.CinemaTheme
import java.util.UUID

// واحد ۱۶ فاز ۳ — قدم ۲ (آخرین قدم فاز ۳): اجزای مشترک سه فرم ساخت Asset
// (Character/Location/Object) — تا الگوی Dropdown/Header/Validation/Chip قابل
// حذف‌شدن سه‌گانه نباشد. طبق همان الگوی EnumDropdownField/ValidationIssueRow
// موجود در ui/dna/DnaTabContent.kt (که private است و قابل بازاستفاده‌ی مستقیم
// نبود)، اینجا نسخه‌ی مشترک سطح‌ماژول نوشته شده. جزئیات کامل تصمیمات در
// docs/adr/049-unit16-phase3-step2-asset-forms.md.

internal fun generateAssetFormId(prefix: String): String = "${prefix}_" + UUID.randomUUID().toString().replace("-", "").take(12)

@Composable
internal fun AssetFormHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = CinemaTheme.extendedColors.fg3)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssetFormEnumDropdownField(
    label: String,
    selectedLabel: String,
    testTag: String,
    menuContent: @Composable (onDismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(testTag)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menuContent { expanded = false }
        }
    }
}

@Composable
internal fun <T> AssetFormFlatEntries(entries: List<T>, itemLabel: (T) -> String, onSelected: (T) -> Unit) {
    entries.forEach { entry ->
        DropdownMenuItem(text = { Text(itemLabel(entry)) }, onClick = { onSelected(entry) })
    }
}

@Composable
internal fun AssetFormValidationIssueRow(issue: ValidationIssue, testTag: String? = null) {
    val color = if (issue.severity == Severity.BLOCKING) MaterialTheme.colorScheme.error else CinemaTheme.extendedColors.warning
    val icon = if (issue.severity == Severity.BLOCKING) Icons.Filled.Error else Icons.Filled.Warning
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text = issue.message, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * ورودی برچسب‌های آزاد (timeCompatibility/weatherCompatibility/keyElements روی
 * LocationAsset) — این سه فیلد `List<String>` هستند، نه `List<Enum>` (تأییدشده با
 * grep مستقیم `domain/asset/AssetModels.kt`؛ برخلاف فرض دستور کار این قدم که آن‌ها
 * را از نوع `List<Enum>` تصور کرده بود). چون واژگان این سه فیلد باز است (نه یک
 * enum بسته با مقادیر از‌پیش‌معلوم)، به‌جای چندانتخابی روی مقادیر ثابت، یک ورودی
 * «افزودن برچسب آزاد» ساخته شد — کاربر متن دلخواه تایپ می‌کند و آن به فهرست Chip
 * زیر اضافه می‌شود؛ لمس یک Chip آن را حذف می‌کند. از همان الگوی `horizontalScroll`
 * Row موجود (نه FlowRow جدید) برای یکدستی با سه ردیف زیرفیلتر موجود صفحه‌ی Asset
 * Library استفاده شده. Chip ها از `OpaqueChip` (اکنون `internal`، نه `private`،
 * تا این فایل هم بتواند بازاستفاده کند) — طبق محدودیت بصری عمومی «Apply this
 * standard to ANY segmented control, chip, or alert card» در Implementation Notes
 * سند طراحی، نه فقط صفحه‌ی Asset Library.
 */
@Composable
internal fun AssetFormTagListField(
    label: String,
    addButtonLabel: String,
    tags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    testTag: String
) {
    var input by remember { mutableStateOf("") }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag(testTag)
            )
            IconButton(onClick = {
                if (input.isNotBlank()) {
                    onAdd(input.trim())
                    input = ""
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = addButtonLabel)
            }
        }
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    OpaqueChip(label = "$tag ×", selected = false, onClick = { onRemove(tag) })
                }
            }
        }
    }
}
