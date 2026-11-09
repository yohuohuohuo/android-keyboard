package com.atharok.btremote

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import android.bluetooth.BluetoothHidDevice
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.atharok.btremote.domain.entities.DeviceHidConnectionState
import com.atharok.btremote.domain.entities.remoteInput.keyboard.advancedKeyboard.EnglishUSAdvancedKeyboardLayout
import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.EnglishUSVirtualKeyboardLayout
import com.atharok.btremote.ui.screens.RemoteScreen
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "zh-rCN-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ControlPagesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: RemoteFixture
    private lateinit var restoration: StateRestorationTester

    @Before fun setup() {
        fixture = RemoteFixture()
        startKoin { modules(module {
            single { EnglishUSAdvancedKeyboardLayout(ApplicationProvider.getApplicationContext()) }
            single { EnglishUSVirtualKeyboardLayout() }
            single { java.util.Locale.SIMPLIFIED_CHINESE }
        }) }
        restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme {
            RemoteScreen(DeviceHidConnectionState(BluetoothHidDevice.STATE_CONNECTED, "测试 Mac"), {}, {}, Modifier.fillMaxSize(), fixture.viewModel)
        } }
    }
    @After fun cleanup() { stopKoin() }

    @Test fun fourTabsStartAtTrackpadAndSwitchWithoutReconnecting() {
        val tabs = listOf("触控板", "键盘", "快捷输入", "遥控")
        tabs.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("触控板").assertIsSelected()
        val x = tabs.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.center.x }
        assertTrue(x.zipWithNext().all { (a, b) -> a < b })
        tabs.forEach { compose.onNodeWithText(it).performClick().assertIsSelected() }
    }

    @Test fun quickInputDraftSurvivesTabSwitch() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").performTextInput("nihao")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("快捷输入").assertIsSelected()
        compose.onNodeWithTag("quick-input").assertTextContains("nihao")
        compose.onNodeWithText("触控板").performClick()
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").assertTextContains("nihao")
    }
    private fun screenshot(name: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output = File("build/screenshots/$name.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun portraitKeyboardHasFixedHeightIsCenteredAndReleasesWhenLeaving() {
        screenshot("trackpad-360x800")
        compose.onNodeWithText("键盘").performClick()
        compose.onNodeWithText("A").assertIsDisplayed()
        compose.onNodeWithText("⌘ Cmd").assertIsDisplayed()
        val area = compose.onNodeWithTag("keyboard-area").fetchSemanticsNode().boundsInRoot
        val grid = compose.onNodeWithTag("keyboard-grid").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(320f, grid.height, 1f)
        org.junit.Assert.assertEquals(area.center.y, grid.center.y, 1f)
        assertTrue(grid.top > area.top && grid.bottom < area.bottom)
        screenshot("keyboard-360x800")
        fixture.reports.clear()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("A").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("触控板").performTouchInput { down(1, center); up(1) }
        compose.mainClock.advanceTimeByFrame()
        compose.onRoot().performTouchInput { up(0) }
        assertTrue(fixture.reports.contains(1 to listOf<Byte>(0,0)))
        assertTrue(fixture.reports.contains(2 to listOf<Byte>(0,0,0,0)))
        assertTrue(fixture.reports.contains(3 to listOf<Byte>(0,0)))
    }

    @Test @Config(qualifiers = "zh-rCN-w800dp-h360dp-land-mdpi")
    fun landscapeKeyboardAndFunctionLayerRemainReachable() {
        compose.onNodeWithText("键盘").performClick()
        compose.onNodeWithText("A").assertIsDisplayed()
        compose.onNodeWithText("⌘ Cmd").assertIsDisplayed()
        val area = compose.onNodeWithTag("keyboard-area").fetchSemanticsNode().boundsInRoot
        val grid = compose.onNodeWithTag("keyboard-grid").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(area.height, grid.height, 1f)
        screenshot("keyboard-800x360")
        compose.onNodeWithText("功能").performClick()
        listOf("F1", "F7", "Esc").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        screenshot("functions-800x360")
    }

    @Test fun leavingKeyboardRestoresRequestedOrientation() {
        val original = compose.activity.requestedOrientation
        compose.onNodeWithText("键盘").performClick()
        compose.onNodeWithText("横屏").performClick()
        compose.waitForIdle()
        org.junit.Assert.assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, compose.activity.requestedOrientation)
        compose.onNodeWithText("快捷输入").performClick()
        compose.waitForIdle()
        org.junit.Assert.assertEquals(original, compose.activity.requestedOrientation)
    }

    @Test fun imeInsetsKeepQuickInputAndTabsAboveKeyboard() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").performTextInput("nihao")
        compose.runOnIdle {
            ViewCompat.dispatchApplyWindowInsets(compose.activity.window.decorView,
                WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0,0,0,300))
                    .setVisible(WindowInsetsCompat.Type.ime(), true).build())
        }
        compose.waitForIdle()
        compose.onNodeWithTag("quick-input").assertIsDisplayed()
        compose.onNodeWithText("发送").assertIsDisplayed()
        listOf("触控板", "键盘", "快捷输入", "遥控").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        assertTrue(compose.onNodeWithText("触控板").fetchSemanticsNode().boundsInRoot.bottom <= 500f)
        screenshot("quick-input-ime-360x800")
        compose.onNodeWithContentDescription("下").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("发送").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("quick-input").assertTextContains("nihao")
    }

    @Test fun quickInputStacksFullWidthSendModeAndOtherKeys() {
        compose.onNodeWithText("快捷输入").performClick()
        val input = compose.onNodeWithTag("quick-input").fetchSemanticsNode().boundsInRoot
        val send = compose.onNodeWithText("发送").assertIsNotEnabled().fetchSemanticsNode().boundsInRoot
        val toggle = compose.onNode(isToggleable()).fetchSemanticsNode().boundsInRoot
        val mode = compose.onNodeWithText("边输入边发（辅助模式）").fetchSemanticsNode().boundsInRoot
        val explanation = compose.onNodeWithText("普通发送不附加回车；候选显示在 Mac。").fetchSemanticsNode().boundsInRoot
        val space = compose.onNodeWithContentDescription("空格键").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(input.left, send.left, 1f)
        org.junit.Assert.assertEquals(input.right, send.right, 1f)
        assertTrue(input.height in 120f..260f && send.height >= 48f)
        assertTrue(send.top >= input.bottom && toggle.top >= send.bottom)
        assertTrue(mode.left >= toggle.right && explanation.left >= toggle.right)
        assertTrue(explanation.top >= mode.bottom && space.top >= explanation.bottom)
        screenshot("quick-input-stacked-360x800")
        compose.onNodeWithTag("quick-input").performTextInput("abc")
        fixture.reports.clear()
        compose.onNodeWithText("发送").assertIsEnabled().performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        compose.mainClock.advanceTimeBy(1000)
        compose.waitUntil(3000) { fixture.viewModel.textStatus.value == com.atharok.btremote.presentation.viewmodel.TextSendStatus.SUBMITTED }
        org.junit.Assert.assertEquals("", fixture.viewModel.draft.value)
        org.junit.Assert.assertEquals(listOf<Byte>(4,5,6), fixture.reports.filter { it.first == 1 && it.second[1] != 0.toByte() }.map { it.second[1] })
        compose.onNodeWithText("蓝牙报告已提交，请在 Mac 核对输入。").assertDoesNotExist()
        screenshot("quick-input-manual-completed-360x800")
    }

    @Test @Config(qualifiers = "zh-rCN-w800dp-h360dp-land-mdpi")
    fun landscapeQuickInputKeepsSendAndOtherKeysReachableByScrolling() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").performTextInput("nihao")
        compose.onNodeWithText("发送").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("下").performScrollTo().assertIsDisplayed()
        listOf("触控板", "键盘", "快捷输入", "遥控").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithTag("quick-input").assertTextContains("nihao")
        screenshot("quick-input-scrolled-800x360")
    }

    @Test fun mouseClickAndMovementStillGenerateOriginalReports() {
        fixture.reports.clear()
        compose.onNodeWithContentDescription("单击左键").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertTrue(fixture.reports.contains(2 to listOf<Byte>(1,0,0,0)))
        assertTrue(fixture.reports.contains(2 to listOf<Byte>(0,0,0,0)))
        fixture.reports.clear()
        compose.onNodeWithTag("trackpad").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertTrue(fixture.reports.any { it.first == 2 && it.second[1] != 0.toByte() })
    }

    @Test fun imeSendDoesNotAppendEnterAndClearsOnlyAfterSubmission() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").performTextInput("abc123")
        fixture.reports.clear()
        compose.onNodeWithTag("quick-input").performImeAction()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        compose.mainClock.advanceTimeBy(1000)
        compose.waitUntil(3000) { fixture.viewModel.textStatus.value == com.atharok.btremote.presentation.viewmodel.TextSendStatus.SUBMITTED }
        org.junit.Assert.assertEquals("", fixture.viewModel.draft.value)
        assertTrue(fixture.reports.none { it.first == 1 && it.second[1] == 0x28.toByte() })
        org.junit.Assert.assertEquals(6, fixture.reports.count { it.first == 1 && it.second[1] != 0.toByte() })
    }

    @Test fun directInputSubmitsEveryCharacterInOneImeEdit() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNode(isToggleable()).performClick()
        fixture.reports.clear()
        compose.onNodeWithTag("quick-input").performTextInput("ab")
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        compose.mainClock.advanceTimeBy(1000)
        compose.waitUntil(3000) { fixture.viewModel.textStatus.value == com.atharok.btremote.presentation.viewmodel.TextSendStatus.SUBMITTED }
        org.junit.Assert.assertEquals(listOf(listOf<Byte>(0,4), listOf<Byte>(0,0), listOf<Byte>(0,5), listOf<Byte>(0,0)), fixture.reports.map { it.second })
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun directInputKeepsModeExplanationStableWithoutProgressOrStop() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNode(isToggleable()).performClick()
        fixture.reports.clear()
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(scheduler))
        try {
            for (character in listOf("a", "b")) {
                compose.onNodeWithTag("quick-input").performTextInput(character)
                scheduler.runCurrent()
                org.junit.Assert.assertEquals(com.atharok.btremote.presentation.viewmodel.TextSendStatus.SENDING, fixture.viewModel.textStatus.value)
                compose.onNodeWithText("发送中，请勿重复提交。").assertDoesNotExist()
                compose.onNodeWithText("停止发送").assertDoesNotExist()
                val busyHint = compose.onNodeWithText("输入后自动发送；候选显示在 Mac。").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                screenshot("quick-input-direct-sending-360x800")
                scheduler.advanceUntilIdle()
                org.junit.Assert.assertEquals(com.atharok.btremote.presentation.viewmodel.TextSendStatus.SUBMITTED, fixture.viewModel.textStatus.value)
                compose.onNodeWithText("蓝牙报告已提交，请在 Mac 核对输入。").assertDoesNotExist()
                compose.onNodeWithText("停止发送").assertDoesNotExist()
                org.junit.Assert.assertEquals(busyHint, compose.onNodeWithText("输入后自动发送；候选显示在 Mac。").fetchSemanticsNode().boundsInRoot)
                compose.onNode(isToggleable()).assertIsOn()
            }
            org.junit.Assert.assertEquals(listOf(listOf<Byte>(0,4), listOf<Byte>(0,0), listOf<Byte>(0,5), listOf<Byte>(0,0)), fixture.reports.filter { it.first == 1 }.map { it.second })
            screenshot("quick-input-direct-ready-360x800")
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun manualLongSendHasNoStopButtonAndLeavingStillPreservesDraft() {
        compose.onNodeWithText("快捷输入").performClick()
        val hint = compose.onNodeWithText("普通发送不附加回车；候选显示在 Mac。").fetchSemanticsNode().boundsInRoot
        val controls = compose.onNodeWithContentDescription("空格键").fetchSemanticsNode().boundsInRoot
        val draft = "abc".repeat(20)
        compose.onNodeWithTag("quick-input").performTextInput(draft)
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(scheduler))
        try {
            compose.onNodeWithText("发送").performClick()
            scheduler.runCurrent()
            compose.onNodeWithText("发送中，请勿重复提交。").assertDoesNotExist()
            compose.onNodeWithText("停止发送").assertDoesNotExist()
            compose.onNodeWithText("发送").assertIsNotEnabled()
            org.junit.Assert.assertEquals(hint, compose.onNodeWithText("普通发送不附加回车；候选显示在 Mac。").fetchSemanticsNode().boundsInRoot)
            org.junit.Assert.assertEquals(controls, compose.onNodeWithContentDescription("空格键").fetchSemanticsNode().boundsInRoot)
            screenshot("quick-input-manual-sending-360x800")
            compose.onNodeWithText("触控板").performClick()
            compose.onNodeWithText("快捷输入").performClick()
            compose.waitForIdle()
            org.junit.Assert.assertEquals(com.atharok.btremote.presentation.viewmodel.TextSendStatus.CANCELLED, fixture.viewModel.textStatus.value)
            compose.onNodeWithText("发送已中止，Mac 可能收到部分字符；请核对原文，不会自动重发。").assertIsDisplayed()
            compose.onNodeWithTag("quick-input").assertTextContains(draft)
            compose.onNodeWithText("停止发送").assertDoesNotExist()
            assertTrue(fixture.reports.contains(1 to listOf<Byte>(0,0)))
            val stoppedReports = fixture.reports.toList()
            scheduler.advanceUntilIdle()
            org.junit.Assert.assertEquals(stoppedReports, fixture.reports)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun directInputFailuresStayVisibleAndKeepDraft() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNode(isToggleable()).performClick()
        fixture.reject = true
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(scheduler))
        try {
            compose.onNodeWithTag("quick-input").performTextInput("abc")
            scheduler.advanceUntilIdle()
            compose.onNodeWithText("发送失败，Mac 可能收到部分字符；原文已保留，请核对后手动处理。").assertIsDisplayed()
            compose.onNodeWithTag("quick-input").assertTextContains("abc")
            compose.onNode(isToggleable()).assertIsOff()
            compose.onNodeWithText("普通发送不附加回车；候选显示在 Mac。").assertIsDisplayed()
            screenshot("quick-input-failed-360x800")
            fixture.reject = false
            compose.onNodeWithTag("quick-input").performTextClearance()
            compose.onNode(isToggleable()).performClick()
            compose.onNodeWithTag("quick-input").performTextInput("你好")
            compose.onNodeWithText("包含当前键盘无法映射的字符。请使用英文或拼音，原文已保留。").assertIsDisplayed()
            compose.onNodeWithTag("quick-input").assertTextContains("你好")
            compose.onNode(isToggleable()).assertIsOff()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun directBusyEditIsPreservedAndLeavesAuxiliaryMode() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNode(isToggleable()).performClick()
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(scheduler))
        try {
            compose.onNodeWithTag("quick-input").performTextInput("ab")
            scheduler.runCurrent()
            org.junit.Assert.assertEquals(com.atharok.btremote.presentation.viewmodel.TextSendStatus.SENDING, fixture.viewModel.textStatus.value)
            compose.onNodeWithTag("quick-input").performTextInput("c")
            org.junit.Assert.assertEquals("abc", fixture.viewModel.draft.value)
            compose.waitForIdle()
            compose.onNode(isToggleable()).assertIsOff()
            compose.onNodeWithText("发送中，请勿重复提交。").assertDoesNotExist()
            compose.onNodeWithText("停止发送").assertDoesNotExist()
            scheduler.advanceUntilIdle()
            compose.onNodeWithText("蓝牙报告已提交，请在 Mac 核对输入。").assertDoesNotExist()
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    @Test fun restorationKeepsFunctionLayerAndClearsHeldInputs() {
        compose.onNodeWithText("键盘").performClick()
        compose.onNodeWithText("功能").performClick()
        fixture.reports.clear()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("键盘").assertIsSelected()
        compose.onNodeWithText("F7").assertIsDisplayed()
        assertTrue(fixture.reports.contains(1 to listOf<Byte>(0,0)))
        assertTrue(fixture.reports.contains(2 to listOf<Byte>(0,0,0,0)))
        assertTrue(fixture.reports.contains(3 to listOf<Byte>(0,0)))
    }

    @Test fun rejectedManualReportKeepsFailureSnackbarVisible() {
        fixture.reject = true
        compose.onNodeWithContentDescription("单击左键").performTouchInput { down(center); up() }
        compose.waitForIdle()
        compose.onNodeWithText("蓝牙未受理按键报告，请检查连接；不会自动重发。").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "zh-rCN-w360dp-h360dp-port-mdpi")
    fun compactPortraitKeepsKeyboardScrollableAndTabsReachable() {
        compose.onNodeWithText("键盘").performClick()
        compose.onNodeWithText("⌘ Cmd").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("下").performScrollTo().assertIsDisplayed()
        listOf("触控板", "键盘", "快捷输入", "遥控").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test fun portraitDirectionsFormCrossBelowGridAndSpaceFillsWidth() {
        compose.onNodeWithText("键盘").performClick()
        assertPortraitDirectionCross()
        val grid = compose.onNodeWithTag("keyboard-grid").fetchSemanticsNode().boundsInRoot
        val space = compose.onNodeWithContentDescription("空格键").fetchSemanticsNode().boundsInRoot
        assertTrue(space.width > grid.width * 0.6f)
        org.junit.Assert.assertEquals(grid.right, space.right, 1f)
        screenshot("keyboard-portrait-cross-360x800")
        compose.onNodeWithText("功能").performClick()
        assertPortraitDirectionCross()
        screenshot("functions-portrait-cross-360x800")
    }

    private fun assertPortraitDirectionCross() {
        listOf("上", "下", "左", "右").forEach { compose.onAllNodesWithContentDescription(it).assertCountEquals(1) }
        val grid = compose.onNodeWithTag("keyboard-grid").fetchSemanticsNode().boundsInRoot
        val area = compose.onNodeWithTag("keyboard-area").fetchSemanticsNode().boundsInRoot
        val up = compose.onNodeWithContentDescription("上").fetchSemanticsNode().boundsInRoot
        val down = compose.onNodeWithContentDescription("下").fetchSemanticsNode().boundsInRoot
        val left = compose.onNodeWithContentDescription("左").fetchSemanticsNode().boundsInRoot
        val right = compose.onNodeWithContentDescription("右").fetchSemanticsNode().boundsInRoot
        assertTrue(up.top > grid.bottom)
        assertTrue(down.bottom < area.bottom)
        listOf(up, down, left, right).forEach { assertTrue(it.width >= 52f && it.height >= 52f) }
        org.junit.Assert.assertEquals(up.center.x, down.center.x, 1f)
        org.junit.Assert.assertEquals(grid.center.x, up.center.x, 1f)
        org.junit.Assert.assertEquals(left.center.y, right.center.y, 1f)
        assertTrue(left.center.x < up.center.x && right.center.x > up.center.x)
        assertTrue(up.bottom < left.top && down.top > left.bottom)
        fixture.reports.clear()
        compose.onRoot().performTouchInput { click(androidx.compose.ui.geometry.Offset(up.center.x, left.center.y)) }
        compose.waitForIdle()
        assertTrue(fixture.reports.isEmpty())
    }

    @Test fun relocatedDirectionSharesCmdStateAndReleasesNormally() {
        compose.onNodeWithText("键盘").performClick()
        compose.onNodeWithText("⌘ Cmd").assertIsDisplayed()
        fixture.reports.clear()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("⌘ Cmd").performTouchInput { down(0, center) }
        org.robolectric.shadows.ShadowLooper.idleMainLooper(200, java.util.concurrent.TimeUnit.MILLISECONDS)
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("左").performTouchInput { down(1, center); up(1) }
        compose.onNodeWithText("⌘ Cmd").performTouchInput { up(0) }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertTrue(fixture.reports.toString(), fixture.reports.contains(1 to listOf<Byte>(8,0x50)))
        org.junit.Assert.assertEquals(1 to listOf<Byte>(0,0), fixture.reports.last())
    }

}
