package com.atharok.btremote

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.atharok.btremote.domain.entities.remoteInput.keyboard.KeyboardLanguage
import com.atharok.btremote.domain.entities.remoteInput.keyboard.advancedKeyboard.EnglishUSAdvancedKeyboardLayout
import com.atharok.btremote.ui.views.keyboard.AdvancedKeyboard
import org.junit.After
import org.junit.Assert.assertEquals
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
@Config(sdk = [36], application = Application::class, qualifiers = "en-rUS-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardEventTest {
    @get:Rule val compose = createComposeRule()
    private var functions by mutableStateOf(false)
    private val reports = mutableListOf<List<Byte>>()

    @Before fun setup() {
        startKoin { modules(module {
            single { EnglishUSAdvancedKeyboardLayout(ApplicationProvider.getApplicationContext()) }
        }) }
        compose.setContent {
            MaterialTheme {
                AdvancedKeyboard(KeyboardLanguage.ENGLISH_US, { reports += it.toList() }, Modifier.fillMaxSize(), functionLayer = functions)
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { reports.clear() }
    }

    @After fun cleanup() { stopKoin() }

    @Test fun sameFramePressAndReleaseMustBothReachReportCallback() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("A").performTouchInput { down(center); up() }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(listOf(listOf<Byte>(0, 4), listOf<Byte>(0, 0)), reports)
    }

    @Test fun repeatedKeyMustNotBeDeduplicatedByComposition() {
        compose.mainClock.autoAdvance = false
        repeat(100) {
            compose.onNodeWithText("A").performTouchInput { down(center); up() }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(200, reports.size)
        assertEquals(List(100) { listOf(listOf<Byte>(0, 4), listOf<Byte>(0, 0)) }.flatten(), reports)
    }

    @Test fun functionKeyIsNotAModifier() {
        compose.runOnIdle { functions = true }
        compose.waitForIdle()
        compose.runOnIdle { reports.clear() }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("F1").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        assertEquals(listOf<Byte>(0, 0x3a), reports.last())
        compose.onNodeWithText("F1").performTouchInput { up() }
    }
    @Test fun alternatingKeysPreserveBothEdges() {
        compose.mainClock.autoAdvance = false
        repeat(10) {
            compose.onNodeWithText(if (it % 2 == 0) "A" else "B").performTouchInput { down(center); up() }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(List(10) { listOf(listOf<Byte>(0, if (it % 2 == 0) 4 else 5), listOf<Byte>(0,0)) }.flatten(), reports)
    }

    @Test fun functionKeysAndEscapeUseOrdinaryKeyByteEvenWhenModifierValuesOverlap() {
        compose.runOnIdle { functions = true }; compose.waitForIdle()
        compose.runOnIdle { reports.clear() }
        compose.mainClock.autoAdvance = false
        listOf("F1", "F7", "Esc").forEach { compose.onNodeWithText(it).performTouchInput { down(center); up() } }
        compose.mainClock.advanceTimeByFrame(); compose.waitForIdle()
        assertEquals(listOf(listOf<Byte>(0,0x3a), listOf<Byte>(0,0), listOf<Byte>(0,0x40), listOf<Byte>(0,0), listOf<Byte>(0,0x29), listOf<Byte>(0,0)), reports)
    }

    @Test fun cancellingTouchReleasesTheHeldKey() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("A").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("A").performTouchInput { cancel() }
        compose.mainClock.advanceTimeByFrame(); compose.waitForIdle()
        assertEquals(listOf<Byte>(0,0), reports.last())
    }

}
