package com.atharok.btremote

import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.EnglishUSVirtualKeyboardLayout
import com.atharok.btremote.domain.repositories.BluetoothRepository
import com.atharok.btremote.domain.repositories.DataStoreRepository
import com.atharok.btremote.domain.usecases.RemoteUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TextReportTest {
    private val reports = mutableListOf<List<Byte>>()
    private var reject = false
    private val bluetooth = Proxy.newProxyInstance(
        BluetoothRepository::class.java.classLoader, arrayOf(BluetoothRepository::class.java)
    ) { _, method, args ->
        when (method.name) {
            "sendReport" -> { reports += (args!![1] as ByteArray).toList(); !reject }
            "sendTextReport" -> false
            else -> error("Unexpected Bluetooth call: ${method.name}")
        }
    } as BluetoothRepository
    private val settings = Proxy.newProxyInstance(
        DataStoreRepository::class.java.classLoader, arrayOf(DataStoreRepository::class.java)
    ) { _, method, _ -> error("Unexpected settings call: ${method.name}") } as DataStoreRepository
    private val useCase = RemoteUseCase(bluetooth, settings)
    private val layout = EnglishUSVirtualKeyboardLayout()

    @Test fun normalTextHasPressReleasePairsAndNoImplicitEnter() = runTest {
        assertTrue(useCase.sendTextReport("ab", layout, false))
        assertEquals(listOf(listOf<Byte>(0, 4), listOf<Byte>(0, 0), listOf<Byte>(0, 5), listOf<Byte>(0, 0)), reports)
        assertEquals(100, testScheduler.currentTime)
    }

    @Test fun unsupportedTextIsRejectedBeforeAnyReport() = runTest {
        assertFalse(useCase.sendTextReport("a你好", layout, false))
        assertTrue(reports.isEmpty())
    }

    @Test fun reportFailureStopsRemainingCharacters() = runTest {
        reject = true
        assertFalse(useCase.sendTextReport("ab", layout, false))
        assertEquals(listOf(listOf<Byte>(0, 4), listOf<Byte>(0, 0)), reports)
    }
}
