package com.operaboys.cinemashotgenerator.ui.i18n

// واحد ۱۶ — فاز ۰: زیرساخت الزام سخت‌گیرانه‌ی RTL (docs/design/README.md بخش
// Bilingual/RTL Requirement): توکن‌های فنی لاتین/عددی (نام مدل‌ها، مقادیر enum مثل
// WIDE/LOCKED، شناسه‌های کد مثل shot_description، 03-SH.01) باید همیشه LTR داخل
// متن RTL رندر شوند. این تابع با Unicode Bidi Isolate (FSI/PDI) این تضمین را در سطح
// خودِ رشته اعمال می‌کند — مستقل از جهت پاراگراف اطرافش.
//
// هنوز هیچ Composable واقعی این تابع را مصرف نمی‌کند (فاز ۰ هیچ محتوای متنی فنی/کد
// ندارد) — این فقط زیرساخت آماده برای فازهای بعدی است که چنین توکن‌هایی را واقعاً
// رندر می‌کنند (مثلاً کد شات «03-SH.01» در Shots List، فاز ۵).

private const val FIRST_STRONG_ISOLATE = '⁨'
private const val POP_DIRECTIONAL_ISOLATE = '⁩'

/**
 * توکن فنی لاتین/عددی (نام مدل، مقدار enum، شناسه‌ی کد) را طوری می‌پیچد که همیشه
 * LTR رندر شود، حتی وقتی داخل یک جمله‌ی فارسی (RTL) قرار گرفته باشد.
 */
fun String.asLtrToken(): String = "$FIRST_STRONG_ISOLATE$this$POP_DIRECTIONAL_ISOLATE"
