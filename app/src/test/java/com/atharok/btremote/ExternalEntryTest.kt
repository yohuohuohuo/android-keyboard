package com.atharok.btremote

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.service.quicksettings.TileService
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.atharok.btremote.common.injections.appModules
import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.EnglishUSVirtualKeyboardLayout
import com.atharok.btremote.domain.repositories.BluetoothRepository
import com.atharok.btremote.domain.repositories.DataStoreRepository
import com.atharok.btremote.presentation.activities.MainActivity
import com.atharok.btremote.presentation.glance.MediaControlWidget
import com.atharok.btremote.presentation.services.BluetoothHidService
import com.atharok.btremote.presentation.services.QuickSettingsTileService
import com.atharok.btremote.presentation.viewmodel.RemoteViewModel
import com.atharok.btremote.presentation.viewmodel.TextSendStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowTileService
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

// Robolectric lacks the API 34+ tile overload. Capture the PendingIntent passed by production onClick.
@Implements(TileService::class)
class PendingTileShadow : ShadowTileService() {
    var launch: PendingIntent? = null
    @Implementation(minSdk = 34)
    fun startActivityAndCollapse(pendingIntent: PendingIntent) { launch = pendingIntent }
}

internal enum class ExternalEntry {
    NOTIFICATION, TILE, WIDGET;

    fun intent(context: Application): Intent {
        val pending = when (this) {
            NOTIFICATION -> {
                val service = Robolectric.buildService(BluetoothHidService::class.java).get()
                val notification = ReflectionHelpers.callInstanceMethod<Notification>(service, "createNotification")
                notification.actions.first().actionIntent
            }
            TILE -> {
                val service = Robolectric.buildService(QuickSettingsTileService::class.java).get()
                service.onClick()
                Shadow.extract<PendingTileShadow>(service).launch!!
            }
            WIDGET -> return ReflectionHelpers.callInstanceMethod(
                MediaControlWidget(), "getStartActivityIntent", ClassParameter.from(Context::class.java, context)
            )
        }
        assertTrue(shadowOf(pending).isActivity)
        assertTrue(shadowOf(pending).isImmutable)
        val saved = shadowOf(pending).savedIntent
        pending.send()
        val dispatched = shadowOf(context).nextStartedActivity
        assertTrue(saved.filterEquals(dispatched))
        assertEquals(saved.flags, dispatched.flags)
        return dispatched
    }
}

internal fun assertResumesMainActivity(context: Context, intent: Intent) {
    assertEquals(context.packageName, intent.component?.packageName)
    assertEquals(MainActivity::class.java.name, intent.component?.className)
    assertEquals(Intent.ACTION_MAIN, intent.action)
    // CLEAR_TOP alone recreates a standard Activity. Both flags are required for this app.
    assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
    assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    assertEquals(ActivityInfo.LAUNCH_MULTIPLE, context.packageManager.getActivityInfo(intent.component!!, 0).launchMode)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "zh-rCN-w360dp-h800dp-mdpi", shadows = [PendingTileShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalCoroutinesApi::class)
class ExternalEntryTest {
    private lateinit var fixture: RemoteFixture
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule(order = 0) val dependencies = object : ExternalResource() {
        override fun before() {
            fixture = RemoteFixture()
            shadowOf(context).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)
            startKoin {
                androidContext(context)
                modules(appModules + module {
                    single<BluetoothRepository> { fixture.bluetooth }
                    single<DataStoreRepository> { fixture.settings }
                })
            }
        }
        override fun after() { stopKoin() }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun notificationPreservesDraft() = resume(ExternalEntry.NOTIFICATION, false)
    @Test fun notificationPreservesInterruptedTextAndNewDraft() = resume(ExternalEntry.NOTIFICATION, true)
    @Test fun tilePreservesDraft() = resume(ExternalEntry.TILE, false)
    @Test fun tilePreservesInterruptedTextAndNewDraft() = resume(ExternalEntry.TILE, true)
    @Test fun widgetPreservesDraft() = resume(ExternalEntry.WIDGET, false)
    @Test fun widgetPreservesInterruptedTextAndNewDraft() = resume(ExternalEntry.WIDGET, true)

    @Test fun retainedOldNotificationTokenCannotRestoreClearTaskFlags() {
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }, PendingIntent.FLAG_IMMUTABLE)
        assertResumesMainActivity(context, ExternalEntry.NOTIFICATION.intent(context))
    }

    private fun resume(entry: ExternalEntry, interrupted: Boolean) {
        compose.onNodeWithText("快捷输入").performClick()
        compose.onNodeWithTag("quick-input").performTextInput(if (interrupted) "abcdef" else "nihao")
        val originalActivity = compose.activity
        val originalVm = ViewModelProvider(originalActivity)[RemoteViewModel::class.java]
        val scheduler = TestCoroutineScheduler()
        if (interrupted) {
            Dispatchers.setMain(StandardTestDispatcher(scheduler))
            try {
                compose.runOnIdle { assertTrue(originalVm.sendDraft(EnglishUSVirtualKeyboardLayout(), true)) }
                scheduler.runCurrent()
                assertEquals(1, fixture.reports.count { it.first == 1 && it.second[1] != 0.toByte() })
                compose.runOnIdle { originalVm.updateDraft("new draft") }
            } finally { Dispatchers.resetMain() }
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        scheduler.runCurrent()
        if (interrupted) assertEquals(TextSendStatus.CANCELLED, originalVm.textStatus.value)
        val reportsBeforeReturn = fixture.reports.size
        val intent = entry.intent(context)
        assertResumesMainActivity(context, intent)
        // startActivity/PendingIntent.send are only recorded by Robolectric, not routed by Android's task manager.
        // After checking the actual launch contract, exercise real MainActivity's onNewIntent path explicitly.
        compose.activityRule.scenario.onActivity {
            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, intent)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        assertSame(originalActivity, compose.activity)
        assertSame(originalVm, ViewModelProvider(compose.activity)[RemoteViewModel::class.java])
        compose.onNodeWithText("快捷输入").assertIsSelected()
        compose.onNodeWithTag("quick-input").assertTextContains(if (interrupted) "new draft" else "nihao")
        if (interrupted) compose.onNodeWithTag("last-attempt").assertTextContains("abcdef")
        scheduler.advanceUntilIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(reportsBeforeReturn, fixture.reports.size)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LegacyTileEntryTest {
    @Test fun preAndroid14TileUsesTheSameReuseContract() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val service = Robolectric.buildService(QuickSettingsTileService::class.java).get()
        service.onClick()
        assertResumesMainActivity(context, shadowOf(context).nextStartedActivity)
    }
}
