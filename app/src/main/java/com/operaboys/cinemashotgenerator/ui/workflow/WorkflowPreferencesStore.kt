package com.operaboys.cinemashotgenerator.ui.workflow

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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

    // واحد ۱۶ فاز ۶ — قدم ۱: صفحه‌ی Settings — همان الگوی دقیق ۴ کلید بالا.
    val DYNAMIC_FONT_ENABLED = booleanPreferencesKey("dynamic_font_enabled")
    val MIN_TOUCH_TARGET_ENABLED = booleanPreferencesKey("min_touch_target_enabled")
    val REDUCED_MOTION_ENABLED = booleanPreferencesKey("reduced_motion_enabled")
    val AUTO_SAVE_CADENCE_SECONDS = longPreferencesKey("auto_save_cadence_seconds")
    val ALLOW_FREE_STEP_JUMP = booleanPreferencesKey("allow_free_step_jump")
    val HOME_SCREEN_IMAGE_URI = stringPreferencesKey("home_screen_image_uri")
}
