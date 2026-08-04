package com.operaboys.cinemashotgenerator.ui.workflow

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// واحد ۱۶ — فاز ۰: مکانیزم Persistence زبان/تم/Layout A-B.
// تصمیم مستند در docs/adr/042-unit16-phase0-shared-foundation.md: DataStore
// Preferences (نه Room) — این‌ها Preference سبک UI هستند، نه یک Entity دامنه؛ افزودن
// یک جدول Room جدید فقط برای ۴ فلگ Scalar بی‌ربط به هیچ Aggregate موجود، برخلاف
// الگوی «هر Aggregate یک جدول» است که واحد ۱۵ در کل پروژه رعایت کرده.

val Context.workflowDataStore: DataStore<Preferences> by preferencesDataStore(name = "workflow_prefs")

internal object WorkflowPrefKeys {
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
    val HOME_LAYOUT_VARIANT = stringPreferencesKey("home_layout_variant")
    val COMPOSER_LAYOUT_VARIANT = stringPreferencesKey("composer_layout_variant")
}
