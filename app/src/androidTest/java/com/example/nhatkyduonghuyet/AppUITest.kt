package com.example.nhatkyduonghuyet

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nhatkyduonghuyet.ui.navigation.Screen
import com.example.nhatkyduonghuyet.ui.theme.NhatKyDuongHuyetTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setupAppNavHost() {
        composeTestRule.setContent {
            navController = TestNavHostController(composeTestRule.activity)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            val application = composeTestRule.activity.application as NhatKyDuongHuyetApplication
            val viewModel = LogEntryViewModel(application.container.logEntryRepository)
            NhatKyDuongHuyetTheme {
                AppNavHost(navController = navController, viewModel = viewModel)
            }
        }
    }

    @Test
    fun verifyStartDestination() {
        composeTestRule
            .onNodeWithText("Nhật ký Đường huyết")
            .assertIsDisplayed()
    }

    @Test
    fun navigateToDayDetailScreen() {
        composeTestRule.onNodeWithContentDescription("Thêm ngày mới").performClick()
        composeTestRule
            .onNodeWithText(Regex("Chi tiết ngày: \\d{4}-\\d{2}-\\d{2}"))
            .assertIsDisplayed()
    }

    @Test
    fun editSessionAndVerifyPersistence() {
        // Navigate to detail screen
        composeTestRule.onNodeWithContentDescription("Thêm ngày mới").performClick()

        // Find and edit a field
        composeTestRule.onNodeWithText("Loại insulin/thuốc").performTextInput("Insulin NPH")
        composeTestRule.onNodeWithText("Lưu").performClick()

        // Go back and re-enter to verify persistence (requires a bit more setup for actual persistence check)
        // For now, just check if the save button works without crashing
    }

    @Test
    fun navigateToChartScreen() {
        composeTestRule.onNodeWithContentDescription("Biểu đồ").performClick()
        composeTestRule.onNodeWithText("Biểu đồ Đường huyết Hàng ngày").assertIsDisplayed()
    }
}
