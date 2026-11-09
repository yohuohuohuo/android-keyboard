package com.atharok.btremote

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.atharok.btremote.domain.usecases.BluetoothHidServiceUseCase
import com.atharok.btremote.presentation.services.BluetoothHidService
import com.atharok.btremote.presentation.services.NotificationBroadcastReceiver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class HidShutdownTest {
    @After fun cleanup() { stopKoin() }
    @Test fun notificationDisconnectStopsServiceSoHidCanUnregister() {
        val f = RemoteFixture()
        startKoin { modules(module { single { BluetoothHidServiceUseCase(f.bluetooth, f.settings) } }) }
        val context = ApplicationProvider.getApplicationContext<Application>()
        NotificationBroadcastReceiver().onReceive(context, Intent(NotificationBroadcastReceiver.ACTION_DISCONNECT))
        assertEquals(BluetoothHidService::class.java.name, shadowOf(context).nextStoppedService?.component?.className)
    }
}
