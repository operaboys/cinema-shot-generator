package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.sceneconditions.WeatherType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع G5 (ADR-067 بخش ه، ADR-095): تست‌های واحد addOutfit/removeOutfit/
// setOutfitAsDefault/شرط Outfit — اولین تست ViewModel-level این کلاس (تا این
// قدم فقط AssetFormFlowTest.kt در سطح Compose E2E پوشش می‌داد). هم‌الگو با
// AiStoryBreakdownViewModelTest.kt (Room in-memory واقعی، بدون Compose).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CharacterAssetFormViewModelTest {

    private lateinit var injectedDatabase: AppDatabase
    private lateinit var viewModel: CharacterAssetFormViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        viewModel = CharacterAssetFormViewModel(
            application = application,
            projectId = "proj_character_outfit_test",
            repository = AssetRepository(injectedDatabase.assetDao())
        )
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    @Test
    fun `a new character form starts with exactly one default outfit, so Rule 5 holds without any user interaction`() {
        assertEquals(1, viewModel.outfits.value.size)
        assertTrue(viewModel.outfits.value.single().isDefault)
    }

    @Test
    fun `addOutfit adds a second outfit with its own condition, without touching the first`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        assertEquals(2, viewModel.outfits.value.size)

        viewModel.setOutfitConditionWeather(1, WeatherType.SNOW)

        val outfits = viewModel.outfits.value
        assertEquals("Winter coat", outfits[1].name)
        assertEquals("snow", outfits[1].condition?.weather)
        assertEquals(null, outfits[0].condition)
    }

    @Test
    fun `a newly added outfit is not default while an existing default remains`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        assertFalse(viewModel.outfits.value[1].isDefault)
        assertTrue(viewModel.outfits.value[0].isDefault)
    }

    @Test
    fun `removing a non-default outfit removes it and leaves the default untouched`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        val defaultId = viewModel.outfits.value[0].id

        viewModel.removeOutfit(1)

        assertEquals(1, viewModel.outfits.value.size)
        assertEquals(defaultId, viewModel.outfits.value[0].id)
        assertTrue(viewModel.outfits.value[0].isDefault)
    }

    @Test
    fun `removing the default outfit promotes the first remaining outfit to default`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")
        val winterCoatId = viewModel.outfits.value[1].id

        viewModel.removeOutfit(0)

        assertEquals(1, viewModel.outfits.value.size)
        assertEquals(winterCoatId, viewModel.outfits.value[0].id)
        assertTrue(
            "طبق تصمیم مستند در ViewModel، حذف Outfit پیش‌فرض باید اولین عضو باقی‌مانده را خودکار پیش‌فرض کند",
            viewModel.outfits.value[0].isDefault
        )
    }

    @Test
    fun `removing the only remaining outfit is blocked, keeping Rule 5 structurally true`() {
        val onlyOutfitId = viewModel.outfits.value[0].id

        viewModel.removeOutfit(0)

        assertEquals(
            "حذف تنها Outfit باقی‌مانده باید مسدود شود تا لیست هرگز خالی نشود (وگرنه selectOutfitForScene در زمان اجرا Crash می‌کند)",
            1,
            viewModel.outfits.value.size
        )
        assertEquals(onlyOutfitId, viewModel.outfits.value[0].id)
    }

    @Test
    fun `setOutfitAsDefault on a new outfit automatically un-defaults the previous default`() {
        viewModel.addOutfit("Winter coat", "a heavy wool coat")

        viewModel.setOutfitAsDefault(1)

        assertFalse(viewModel.outfits.value[0].isDefault)
        assertTrue(viewModel.outfits.value[1].isDefault)
    }

    @Test
    fun `setting all three outfit condition fields to non-null and back to null round-trips through OutfitCondition`() {
        viewModel.setOutfitConditionWeather(0, WeatherType.RAIN)
        viewModel.setOutfitConditionTimeOfDay(0, TimeOfDay.NIGHT)
        viewModel.setOutfitConditionLocationType(0, LocationType.OUTDOOR)

        val condition = viewModel.outfits.value[0].condition
        assertEquals("rain", condition?.weather)
        assertEquals("night", condition?.timeOfDay)
        assertEquals("outdoor", condition?.locationType)

        viewModel.setOutfitConditionWeather(0, null)
        viewModel.setOutfitConditionTimeOfDay(0, null)
        viewModel.setOutfitConditionLocationType(0, null)

        assertEquals(
            "وقتی هر سه فیلد شرط null می‌شوند، کل condition باید null شود (نه یک OutfitCondition با سه فیلد null)",
            null,
            viewModel.outfits.value[0].condition
        )
    }

    // یادداشت: تستی برای validationIssues (Rule ۵ روی لیست خالی) عمداً اینجا
    // نوشته نشد — validationIssues از combine(...).stateIn(WhileSubscribed) ساخته
    // می‌شود؛ بدون یک Collector واقعی (یا ioScopeOverride = Dispatchers.Unconfined،
    // که این فایل هم‌الگو با AiStoryBreakdownViewModelTest.kt عمداً ندارد)، `.value`
    // فقط مقدار اولیه (emptyList) را برمی‌گرداند، نه محاسبه‌ی واقعی — یک تست اینجا
    // چیزی را واقعاً اثبات نمی‌کرد. Rule ۵ خودش در AssetValidationTest.kt (سطح
    // دامنه) و از طریق آزمون‌های بالا (لیست هرگز خالی نمی‌شود) پوشش داده شده.
}
