package com.operaboys.cinemashotgenerator.ui.navigation

import com.operaboys.cinemashotgenerator.domain.project.ProjectSummary
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowState

// رفع G6 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/062-...):
// PLACEHOLDER_ACTIVE_PROJECT_ID (رشته‌ی جعلی ثابت که قبلاً در BottomNavBar.kt/
// AssetsScreen.kt/هر سه فرم Asset استفاده می‌شد) کاملاً حذف شد — این تابع مفهوم
// واقعی «آخرین/فعال پروژه» را پیاده می‌کند: اگر یک Session واقعی Studio باز است
// (WorkflowState.projectId)، همان برنده است؛ در غیر این صورت، پروژه‌ای که آخرین‌بار
// تغییر کرده (projectSummaries از قبل توسط ProjectDao.getAllProjectsWithCounts
// روی lastModified DESC مرتب می‌شود — همان لیستی که «پروژه‌های اخیر» Home از آن
// استفاده می‌کند)؛ اگر اصلاً هیچ پروژه‌ای وجود ندارد، null (فراخوان‌کننده باید طبق
// همان الگوی Fallback «هدایت به Projects» قدم G1 عمل کند).
fun resolveActiveOrRecentProjectId(workflowState: WorkflowState?, projectSummaries: List<ProjectSummary>): String? =
    workflowState?.projectId ?: projectSummaries.firstOrNull()?.project?.projectId
