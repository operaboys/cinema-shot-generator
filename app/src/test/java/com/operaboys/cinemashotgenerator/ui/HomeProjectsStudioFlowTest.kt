package com.operaboys.cinemashotgenerator.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_HOME_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// واحد ۱۶ فاز ۱ — تست end-to-end واقعی Home→Projects→Studio طبق دستور کار
// (docs/adr/044-unit16-phase1-app-shell.md): ایجاد پروژه → ظاهر در Home/Projects؛
// ورود به Studio از کارت پروژه → نمایش ۴ Tab و جابه‌جایی؛ منوی Overflow → حذف واقعی.
//
// یافته‌ی این فاز: QuickCreateRow روی Home بعد از ایجاد موفق، خودکار وارد Studio
// همان پروژه می‌شود (`HomeScreen.kt`: `onCreated = { onOpenProject(it.projectId) }`)
// — یک تصمیم UX عمدی («ایجاد سریع» یعنی مستقیم به کار مشغول شو)، نه یک باگ. پس هر
// تستی که بعد از ایجاد نیاز به دیدن Home/Projects یا کارت پروژه دارد، ابتدا با
// BOTTOM_NAV_HOME_TAG به Home برمی‌گردد (نوار پایین همیشه در دسترس است، حتی داخل
// Studio — لایه‌ی مکمل طبق ADR-042).
//
// یافته‌ی دوم (رفع Flaky واقعی، نه فرضی): `ProjectListViewModel.projectSummaries`
// از `ProjectDao.getAllProjectsWithCounts()` می‌آید — یک Flow ای که Room آن را روی
// Executor داخلی خودش (نه Dispatcher تزریق‌شده‌ی ViewModel) دوباره Query و Emit
// می‌کند؛ یعنی بعد از «ذخیره»ی ایجاد پروژه، این Flow با یک تأخیر Thread واقعی (نه
// صفر) به‌روز می‌شود که `composeRule`'s idle خودکار (متکی به Looper اصلی) تضمینی
// برایش ندارد. راه‌حل: `waitUntilExactlyOneExists` (API واقعی و رسمی
// `ComposeTestRule` برای دقیقاً همین سناریو) بلافاصله بعد از «ذخیره»، قبل از هر
// تعامل بعدی که به دیدن پروژه در فهرست نیاز دارد.
//
// یافته‌ی سوم: Viewport پیش‌فرض `createComposeRule()` زیر Robolectric بسیار کوچک
// است (۴۷۰px ارتفاع) — روی این اندازه، کارت پروژه (و دکمه‌ی Overflow‌اش) داخل
// LazyColumn معمولاً بیرون از ناحیه‌ی دیده‌شونده‌ی فعلی اسکرول قرار می‌گیرد.
// `performClick()` مختصات را از Layout واقعی محاسبه می‌کند و بدون خطا «موفق»
// گزارش می‌شود، اما چون آن مختصات بیرون از ناحیه‌ی Clip شده‌ی LazyColumn است، لمس
// واقعی هرگز به View نمی‌رسد و onClick صدا زده نمی‌شود — نتیجه: هیچ خطایی روی خودِ
// کلیک نیست، اما تعامل بعدی (باز شدن DropdownMenu) هرگز رخ نمی‌دهد. راه‌حل واقعی و
// رسمی Compose Testing برای این کلاس مشکل: `performScrollTo()` قبل از هر
// `performClick()` روی عنصر داخل یک لیست/Column اسکرول‌شونده که ممکن است خارج از
// Viewport فعلی باشد. `ProjectListSection` (در Home/Projects) همچنین
// `Modifier.weight(1f)` گرفت تا صریحاً فضای باقی‌مانده‌ی Column میزبان را بگیرد —
// یک بهبود درست و Idiomatic مستقل، هرچند علت اصلی شکست تست نبود.
//
// یافته‌ی چهارم (واحد ۱۶ فاز ۲، قدم ۱): از وقتی Tab «داستان» محتوای واقعی گرفت،
// فیلد عنوان آن (StoryTabContent) هم نام پروژه را نشان می‌دهد — یعنی بلافاصله بعد
// از ایجاد سریع (وقتی هنوز داخل Studio با Tab داستان پیش‌فرض هستیم)، هم Header
// استودیو و هم این فیلد عنوان نام یکسانی دارند؛ `waitUntilExactlyOneExists` در این
// لحظه هرگز به «دقیقاً یک» نمی‌رسد. رفع شد با `waitUntilAtLeastOneExists` (همان
// راه‌حلی که در StoryTabFlowTest.kt هم استفاده شده) — اینجا فقط «داده بارگذاری شده»
// مهم است، نه شمارش دقیق گره‌ها.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeProjectsStudioFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = ProjectRepository(database.projectDao(), idProvider = { "proj_flow_test" })
        )

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    storyRepository = StoryRepository(database.storyDao(), database.storyBreakdownSessionDao()),
                    projectDnaRepository = ProjectDnaRepository(database.projectDnaDao()),
                    sceneRepository = SceneRepository(database.sceneDao())
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده.
    }

    @Test
    fun `creating a new project from Home makes it appear in both Home and Projects`() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("My First Movie")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("My First Movie"), timeoutMillis = 5_000)

        // ایجاد سریع خودکار وارد Studio همان پروژه می‌شود — برای بازدید Home/Projects
        // اول باید با نوار پایین برگردیم.
        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
        composeRule.onNodeWithText("My First Movie").assertIsDisplayed()

        composeRule.onNodeWithText(uiString("nav.projects", Language.FA)).performClick()
        composeRule.onNodeWithText("My First Movie").assertIsDisplayed()
    }

    @Test
    fun `opening a project card navigates into Studio and shows all 4 tabs, switchable`() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("Studio Test Project")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Studio Test Project"), timeoutMillis = 5_000)

        // برای تست واقعی «ورود به Studio از یک کارت پروژه» (نه از خودِ ایجاد سریع)،
        // اول به Home برمی‌گردیم و بعد صریحاً روی کارت کلیک می‌کنیم.
        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
        composeRule.onNodeWithText("Studio Test Project").performClick()

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.STORY)).assertIsDisplayed()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).assertIsDisplayed()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).assertIsDisplayed()
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).assertIsDisplayed()

        // واحد ۱۶ فاز ۲ قدم ۳: Tab «DNA» دیگر Placeholder نیست (محتوای واقعی
        // DnaTabContent دارد) — طبق docs/adr/047-unit16-phase2-step3-dna-tab.md.
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).performClick()
        composeRule.onNodeWithText(uiString("dna.group.coreIdentity", Language.FA)).assertIsDisplayed()

        // واحد ۱۶ فاز ۴ قدم ۱: Tab «صحنه‌ها» هم دیگر Placeholder نیست (محتوای واقعی
        // ScenesListScreen دارد) — طبق docs/adr/050-unit16-phase4-step1-scene-detail.md؛
        // این تست حالا Tab «خروجی» (تنها Tab باقی‌مانده‌ی واقعاً Placeholder) را برای
        // اثبات «هنوز Placeholder» بررسی می‌کند.
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.SCENES)).performClick()
        composeRule.onNodeWithText(uiString("scenesList.emptyState", Language.FA)).assertIsDisplayed()

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.OUTPUT)).performClick()
        composeRule.onNodeWithText(uiString("studio.tabPlaceholder", Language.FA)).assertIsDisplayed()
    }

    @Test
    fun `deleting a project via the overflow menu actually removes it from the list`() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("To Be Deleted")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("To Be Deleted"), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
        composeRule.onNodeWithText("To Be Deleted").assertIsDisplayed()

        // Viewport تست بسیار کوچک است (۴۷۰px) — کارت پروژه (و دکمه‌ی Overflow آن)
        // ممکن است بیرون از ناحیه‌ی دیده‌شونده‌ی LazyColumn باشد؛ performScrollTo
        // تضمین می‌کند قبل از کلیک واقعاً درون Viewport اسکرول شده باشد (بدون آن،
        // performClick روی مختصات صرفاً Layout-محاسبه‌شده کلیک را «موفق» گزارش
        // می‌کند اما لمس واقعی هرگز به View می‌رسد چون بیرون از ناحیه‌ی Clip شده
        // است — onClick واقعاً صدا زده نمی‌شود).
        composeRule.onNodeWithContentDescription(uiString("project.overflowMenu", Language.FA)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("project.overflow.delete", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("project.overflow.delete", Language.FA)).performClick()
        // AlertDialog تأیید حذف
        composeRule.waitUntilExactlyOneExists(hasText(uiString("project.delete.confirm", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("project.delete.confirm", Language.FA)).performClick()

        composeRule.waitUntilDoesNotExist(hasText("To Be Deleted"), timeoutMillis = 5_000)
    }

    /**
     * رفع یافته‌ی 🔴 G21 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md):
     * آرشیو قبلاً بدون هیچ دیالوگ تأیید مستقیماً اجرا می‌شد. این تست هر دو مسیر
     * دیالوگ را واقعاً می‌سنجد: (۱) Cancel → هیچ فراخوان واقعی archiveProject رخ
     * نمی‌دهد (lastActionMessage دست‌نخورده می‌ماند، پروژه هنوز در فهرست است)؛
     * (۲) Confirm → archiveProject واقعاً صدا زده می‌شود. چون پروژه‌ی تازه‌ساخته‌شده
     * در وضعیت DRAFT است (نه FINAL)، طبق ALLOWED_TRANSITIONS واقعی StateMachine.kt،
     * این فراخوان با شکست State Machine مواجه می‌شود — که خودش یک اثبات غیرمستقیم
     * اما واقعی است که واقعاً اجرا شد (نه فقط دیالوگ بسته شد): lastActionMessage
     * واقعی ViewModel با پیام خطای واقعی State Machine پر می‌شود.
     */
    @Test
    fun `archiving a project requires confirmation — cancel does nothing, confirm actually calls archiveProject`() {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput("To Be Archived")
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("To Be Archived"), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
        composeRule.onNodeWithText("To Be Archived").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(uiString("project.overflowMenu", Language.FA)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("project.overflow.archive", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("project.overflow.archive", Language.FA)).performClick()

        // (۱) لغو — دیالوگ تأیید ظاهر می‌شود؛ Cancel هیچ تغییری اعمال نمی‌کند.
        composeRule.waitUntilExactlyOneExists(hasText(uiString("project.archive.confirm", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("project.archive.cancel", Language.FA)).performClick()
        composeRule.waitUntilDoesNotExist(hasText(uiString("project.archive.confirm", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText("To Be Archived").assertIsDisplayed()
        assert(projectListViewModel.lastActionMessage.value == null) {
            "لغو دیالوگ نباید هیچ فراخوان واقعی archiveProject ای ایجاد کند"
        }

        // (۲) تأیید — اکنون archiveProject واقعاً صدا زده می‌شود.
        composeRule.onNodeWithContentDescription(uiString("project.overflowMenu", Language.FA)).performScrollTo().performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("project.overflow.archive", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("project.overflow.archive", Language.FA)).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("project.archive.confirm", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("project.archive.confirm", Language.FA)).performClick()

        composeRule.waitUntilDoesNotExist(hasText(uiString("project.archive.confirm", Language.FA)), timeoutMillis = 5_000)
        // یافته‌ی واقعی دیباگ این تست: ProjectListViewModel.lastActionMessage خودش
        // یک StateFlow خام است، بدون UI متناظر برای Poll مستقیم قابل‌اتکا (بدون
        // تغییری قابل‌مشاهده در درخت Compose، هماهنگی خودکار ComposeTestRule تضمینی
        // برای رسیدن Coroutine واقعی به پایان ندارد). این ViewModel از قبل به یک
        // Snackbar واقعی وصل است (HomeScreen.kt: LaunchedEffect(lastActionMessage))
        // — پس انتظار روی همان متن واقعی Snackbar (نه Poll مستقیم StateFlow) هم
        // قابل‌اتکاتر است و هم واقعاً اثبات می‌کند archiveProject به کاربر گزارش شد.
        composeRule.waitUntilAtLeastOneExists(hasText("DRAFT", substring = true), timeoutMillis = 5_000)
    }
}
