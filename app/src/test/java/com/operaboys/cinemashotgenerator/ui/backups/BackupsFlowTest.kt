package com.operaboys.cinemashotgenerator.ui.backups

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.FakeBackupFileStorage
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.home.HOME_OPEN_DRAWER_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.studio.STUDIO_BACK_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// واحد ۱۶ فاز ۶ — قدم ۲ (آخرین قدم کل واحد ۱۶): تست End-to-End واقعی صفحه‌ی
// Backups + Scroll واقعی Nav Drawer روی یک صفحه‌ی کوچک شبیه‌سازی‌شده. الگوی
// راه‌اندازی عیناً از SettingsFlowTest.kt گرفته شده. جزئیات کامل تصمیمات در
// docs/adr/059-unit16-phase6-step2-backups-final-review.md.
//
// FakeBackupFileStorage (از BackupManagerTest.kt، همان الگوی in-memory تست‌پذیر
// ADR-023) اینجا بازاستفاده شد — بدون I/O فایل واقعی دستگاه.
//
// یافته‌ی واقعی این تست (نه صرفاً یک محدودیت تست): اولین اجرا با
// AppDatabase.getInstance(application) داخلی BackupsViewModel/StudioShell شکست
// می‌خورد، چون پروژه‌ی واقعاً ساخته‌شده در `database` تزریقی این فایل (زیر) در آن
// Singleton سراسری هرگز وجود نداشت. رفع شد: database اکنون یک پارامتر تزریقی
// واقعی است (BackupsViewModel/BackupsScreen/StudioShell/AppNavHost/MainScaffold،
// هم‌الگو دقیق با autoSaveManager/backupFileStorage) — همین‌جا هم صریحاً پاس داده
// می‌شود. جزئیات کامل در docs/adr/059-unit16-phase6-step2-backups-final-review.md.

private const val PROJECT_ID = "proj_backups_flow_test"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupsFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var fakeBackupStorage: FakeBackupFileStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "backups_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
            repository = ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID })
        )
        fakeBackupStorage = FakeBackupFileStorage()
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده. waitForIdle() بالا همچنان برای
        // Idling خودِ Compose مفید است، فقط دیگر ایمنی close() را تضمین نمی‌کند.
    }

    private fun setContent() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    backupFileStorage = fakeBackupStorage,
                    database = database
                )
            }
        }
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    /** پروژه‌ی واقعی می‌سازد و وارد Studio می‌شود (تنها راه واقعی startWorkflowSession را فعال کند). */
    private fun createProjectAndEnterStudio(name: String = "Backups Flow Test") {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(name)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 15_000)
        composeRule.waitUntilExactlyOneExists(hasTestTag(STUDIO_BACK_BUTTON_TAG), timeoutMillis = 15_000)
    }

    /**
     * تنها راه واقعی رسیدن به Backups: برگشت از Studio به Home (StudioShell خودش
     * دکمه‌ی همبرگری ندارد)، سپس باز کردن Drawer و کلیک روی «بکاپ‌ها» — طبق یافته‌ی
     * این قدم، بدون performScrollTo/clickViaSemantics روی گره‌ی خارج از Viewport
     * (آخرین آیتم گروه SYSTEM) بی‌اثر است.
     */
    private fun navigateToBackupsFromStudio() {
        composeRule.onNodeWithTag(STUDIO_BACK_BUTTON_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag(HOME_OPEN_DRAWER_BUTTON_TAG), timeoutMillis = 15_000)
        composeRule.onNodeWithTag(HOME_OPEN_DRAWER_BUTTON_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("drawer.backups", Language.FA)), timeoutMillis = 10_000)
        composeRule.onNodeWithText(uiString("drawer.backups", Language.FA)).performScrollTo().clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(BACKUPS_BACK_BUTTON_TAG), timeoutMillis = 10_000)
    }

    /**
     * یافته‌ی ۲ appendix ADR-081 (ADR-082): بنر «بدون Cloud Sync» طبق mockup
     * (`x.k49`) که قبلاً کاملاً غایب بود.
     */
    @Test
    fun `Backups screen shows the on-device no-cloud-sync banner with the exact mockup text`() {
        setContent()
        createProjectAndEnterStudio()
        navigateToBackupsFromStudio()

        composeRule.onNodeWithTag(BACKUPS_CLOUD_SYNC_BANNER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("backups.cloudSyncBanner", Language.FA)).assertIsDisplayed()
    }

    @Test
    fun `Nav Drawer scroll makes Backups (last item of the last group) reachable on a small simulated screen`() {
        setContent()
        createProjectAndEnterStudio()

        // خودِ navigateToBackupsFromStudio زیر، Scroll واقعی + رسیدن موفق به
        // BACKUPS_BACK_BUTTON_TAG را Assert می‌کند؛ این تست فقط تأیید می‌کند این
        // مسیر حتی روی یک صفحه‌ی کوچک (که در آن گروه SYSTEM قطعاً از ابتدا در
        // Viewport نیست) هم واقعاً کار می‌کند.
        navigateToBackupsFromStudio()

        composeRule.onNodeWithTag(BACKUPS_BACK_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `creating a manual backup shows it in the list with a Manual badge`() {
        setContent()
        createProjectAndEnterStudio()
        navigateToBackupsFromStudio()

        composeRule.onNodeWithTag(BACKUPS_CREATE_BUTTON_TAG).performScrollTo().clickViaSemantics()

        composeRule.waitUntilAtLeastOneExists(hasText(uiString("backups.kindManual", Language.FA)), timeoutMillis = 10_000)
        composeRule.onNodeWithText(uiString("backups.restoreButton", Language.FA)).assertIsDisplayed()
        composeRule.onNodeWithText(uiString("backups.deleteButton", Language.FA)).assertIsDisplayed()
    }

    @Test
    fun `restoring from a backup actually replaces the project's current data`() {
        setContent()
        createProjectAndEnterStudio()
        navigateToBackupsFromStudio()

        composeRule.onNodeWithTag(BACKUPS_CREATE_BUTTON_TAG).performScrollTo().clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("backups.kindManual", Language.FA)), timeoutMillis = 10_000)

        // شبیه‌سازی یک ویرایش واقعی بعد از بکاپ (تغییرنام مستقیم پروژه در دیتابیس،
        // هم‌ارز آنچه صفحه‌ی Rename واقعی انجام می‌دهد) — منطق داخلی restore خودش
        // قبلاً کامل در BackupManagerTest.kt پوشش داده شده؛ این تست فقط اثبات
        // می‌کند دکمه‌ی Restore این صفحه واقعاً همان مسیر را صدا می‌زند.
        val beforeRestore = runBlocking { database.projectDao().loadProject(PROJECT_ID) }!!
        runBlocking {
            database.projectDao().saveProject(beforeRestore.copy(projectName = "Edited After Backup"))
        }

        // رفع یافته‌ی 🔴 G19 ممیزی post-Unit16: Restore اکنون یک دیالوگ تأیید دارد —
        // کلیک اول دیالوگ را باز می‌کند، تأیید واقعی عملیات را اجرا می‌کند.
        composeRule.onNodeWithText(uiString("backups.restoreButton", Language.FA)).performScrollTo().clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("backups.restoreConfirmButton", Language.FA)), timeoutMillis = 10_000)
        composeRule.onNodeWithText(uiString("backups.restoreConfirmButton", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("backups.restoredMessage", Language.FA)), timeoutMillis = 10_000)

        val afterRestore = runBlocking { database.projectDao().loadProject(PROJECT_ID) }
        assertEquals("Backups Flow Test", afterRestore?.projectName)
    }

    /** رفع یافته‌ی 🔴 G19 ممیزی post-Unit16: Cancel دیالوگ Restore نباید هیچ اثری داشته باشد. */
    @Test
    fun `cancelling the restore confirmation dialog does not change the project's data`() {
        setContent()
        createProjectAndEnterStudio()
        navigateToBackupsFromStudio()

        composeRule.onNodeWithTag(BACKUPS_CREATE_BUTTON_TAG).performScrollTo().clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("backups.kindManual", Language.FA)), timeoutMillis = 10_000)

        val beforeRestore = runBlocking { database.projectDao().loadProject(PROJECT_ID) }!!
        runBlocking {
            database.projectDao().saveProject(beforeRestore.copy(projectName = "Edited After Backup"))
        }

        composeRule.onNodeWithText(uiString("backups.restoreButton", Language.FA)).performScrollTo().clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("backups.restoreConfirmButton", Language.FA)), timeoutMillis = 10_000)
        composeRule.onNodeWithText(uiString("backups.restoreConfirmCancel", Language.FA)).performClick()
        composeRule.waitUntilDoesNotExist(hasText(uiString("backups.restoreConfirmButton", Language.FA)), timeoutMillis = 10_000)

        val afterCancel = runBlocking { database.projectDao().loadProject(PROJECT_ID) }
        assertEquals("Edited After Backup", afterCancel?.projectName)
    }

    @Test
    fun `deleting a backup removes it from the list`() {
        setContent()
        createProjectAndEnterStudio()
        navigateToBackupsFromStudio()

        composeRule.onNodeWithTag(BACKUPS_CREATE_BUTTON_TAG).performScrollTo().clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("backups.kindManual", Language.FA)), timeoutMillis = 10_000)

        // رفع یافته‌ی 🔴 G20 ممیزی post-Unit16: Delete اکنون یک دیالوگ تأیید دارد.
        composeRule.onNodeWithText(uiString("backups.deleteButton", Language.FA)).performScrollTo().clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("backups.deleteConfirmButton", Language.FA)), timeoutMillis = 10_000)
        composeRule.onNodeWithText(uiString("backups.deleteConfirmButton", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("backups.emptyState", Language.FA)), timeoutMillis = 10_000)

        assertTrue(runBlocking { fakeBackupStorage.listFiles("backup_${PROJECT_ID.length}_${PROJECT_ID}_") }.isEmpty())
    }

    /**
     * یافته‌ی #۱۶ appendix ADR-081 (ADR-087): دکمه‌های سریع زبان/تم که قبلاً فقط
     * روی HomeHeader بودند اکنون به هر صفحه‌ی داخلی هم تزریق شده‌اند. این تست
     * اثبات می‌کند کلیک این دو دکمه‌ی تازه‌ی Backups واقعاً State سراسری
     * WorkflowViewModel را عوض می‌کند — نه فقط این‌که دکمه‌ها رندر می‌شوند.
     */
    @Test
    fun `the header's language and theme toggle buttons actually change global WorkflowViewModel state`() {
        setContent()
        createProjectAndEnterStudio()
        navigateToBackupsFromStudio()

        val initialLanguage = workflowViewModel.language.value
        val initialTheme = workflowViewModel.theme.value

        composeRule.onNodeWithTag(BACKUPS_TOGGLE_LANGUAGE_BUTTON_TAG).clickViaSemantics()
        composeRule.waitForIdle()
        assertTrue(workflowViewModel.language.value != initialLanguage)

        composeRule.onNodeWithTag(BACKUPS_TOGGLE_THEME_BUTTON_TAG).clickViaSemantics()
        composeRule.waitForIdle()
        assertTrue(workflowViewModel.theme.value != initialTheme)
    }

    /** رفع یافته‌ی 🔴 G20 ممیزی post-Unit16: Cancel دیالوگ Delete نباید بکاپ را حذف کند. */
    @Test
    fun `cancelling the delete confirmation dialog leaves the backup in the list`() {
        setContent()
        createProjectAndEnterStudio()
        navigateToBackupsFromStudio()

        composeRule.onNodeWithTag(BACKUPS_CREATE_BUTTON_TAG).performScrollTo().clickViaSemantics()
        composeRule.waitUntilAtLeastOneExists(hasText(uiString("backups.kindManual", Language.FA)), timeoutMillis = 10_000)

        composeRule.onNodeWithText(uiString("backups.deleteButton", Language.FA)).performScrollTo().clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("backups.deleteConfirmButton", Language.FA)), timeoutMillis = 10_000)
        composeRule.onNodeWithText(uiString("backups.deleteConfirmCancel", Language.FA)).performClick()
        composeRule.waitUntilDoesNotExist(hasText(uiString("backups.deleteConfirmButton", Language.FA)), timeoutMillis = 10_000)

        composeRule.onNodeWithText(uiString("backups.kindManual", Language.FA)).assertIsDisplayed()
        assertTrue(runBlocking { fakeBackupStorage.listFiles("backup_${PROJECT_ID.length}_${PROJECT_ID}_") }.isNotEmpty())
    }
}
