package com.atharok.btremote

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothHidDevice
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.atharok.btremote.common.injections.appModules
import com.atharok.btremote.domain.entities.DeviceHidConnectionState
import com.atharok.btremote.domain.repositories.BluetoothRepository
import com.atharok.btremote.domain.repositories.DataStoreRepository
import com.atharok.btremote.ui.app.BtRemoteApp
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "zh-rCN-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DraftNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @After fun cleanup() { stopKoin() }

    private lateinit var fixture: RemoteFixture
    @Before fun setup() {
        fixture = RemoteFixture()
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)
        startKoin {
            androidContext(context)
            modules(appModules + module {
                single<BluetoothRepository> { fixture.bluetooth }
                single<DataStoreRepository> { fixture.settings }
            })
        }
        compose.setContent { BtRemoteApp() }
    }

    @Test fun disconnectAndReconnectRetainsDraftAcrossActualNavigationPop() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").performTextInput("nihao")
        compose.runOnIdle { fixture.connection.value = DeviceHidConnectionState(BluetoothHidDevice.STATE_DISCONNECTED, "") }
        compose.waitForIdle()
        compose.onNodeWithText("快捷输入").assertDoesNotExist()
        compose.runOnIdle { fixture.connection.value = DeviceHidConnectionState(BluetoothHidDevice.STATE_CONNECTED, "测试 Mac") }
        compose.waitForIdle()
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").assertTextContains("nihao")
    }
    @Test fun partialSendDisconnectPreservesOriginalAndNewDraftWithoutReplay() {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").performTextInput("nihao")
        compose.onNodeWithTag("quick-input").performImeAction()
        compose.onNodeWithTag("quick-input").performTextReplacement("new draft")
        compose.runOnIdle { fixture.connection.value = DeviceHidConnectionState(BluetoothHidDevice.STATE_DISCONNECTED, "") }
        compose.waitForIdle()
        val pressed = fixture.reports.count { it.first == 1 && it.second[1] != 0.toByte() }
        compose.runOnIdle { fixture.connection.value = DeviceHidConnectionState(BluetoothHidDevice.STATE_CONNECTED, "测试 Mac") }
        compose.waitForIdle()
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").assertTextContains("new draft")
        compose.onNodeWithTag("last-attempt").assertTextContains("nihao")
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        org.junit.Assert.assertEquals(pressed, fixture.reports.count { it.first == 1 && it.second[1] != 0.toByte() })
    }

}
