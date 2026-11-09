package com.atharok.btremote.domain.usecases

import com.atharok.btremote.common.utils.DELAY_BETWEEN_KEY_PRESSES_IN_MILLIS
import com.atharok.btremote.common.utils.KEYBOARD_REPORT_ID
import com.atharok.btremote.common.utils.REMOTE_INPUT_NONE
import com.atharok.btremote.domain.entities.remoteInput.RemoteButtonProperties
import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.VirtualKeyboardLayout
import kotlinx.coroutines.delay
import com.atharok.btremote.domain.entities.settings.RemoteSettings
import com.atharok.btremote.domain.repositories.BluetoothRepository
import com.atharok.btremote.domain.repositories.DataStoreRepository
import kotlinx.coroutines.flow.Flow

class RemoteUseCase(
    private val bluetoothRepository: BluetoothRepository,
    private val dataStoreRepository: DataStoreRepository
) {

    // ---- Settings ----

    val remoteSettingsFlow: Flow<RemoteSettings> get() = dataStoreRepository.remoteSettingsFlow

    // ---- Connection ----

    fun disconnectDevice(): Boolean {
        return bluetoothRepository.disconnectDevice()
    }

    // ---- Send ----

    fun sendReport(id: Int, bytes: ByteArray): Boolean {
        return bluetoothRepository.sendReport(id, bytes)
    }

    suspend fun sendTextReport(
        text: String,
        virtualKeyboardLayout: VirtualKeyboardLayout,
        shouldSendEnter: Boolean,
        canSend: () -> Boolean = { true }
    ): Boolean {
        if (text.isEmpty() || text.any { !virtualKeyboardLayout.supports(it) }) return false
        val keys = text.map { virtualKeyboardLayout.getKeyboardKey(it) } +
            if (shouldSendEnter) listOf(RemoteButtonProperties.KeyboardEnterButton.input) else emptyList()
        var needsRelease = false
        fun report(bytes: ByteArray) = canSend() && bluetoothRepository.sendReport(KEYBOARD_REPORT_ID, bytes)
        try {
            for (key in keys) {
                needsRelease = true
                if (!report(key)) return false
                delay(DELAY_BETWEEN_KEY_PRESSES_IN_MILLIS)
                if (!report(REMOTE_INPUT_NONE)) return false
                needsRelease = false
                delay(DELAY_BETWEEN_KEY_PRESSES_IN_MILLIS)
            }
            return true
        } finally {
            if (needsRelease) report(REMOTE_INPUT_NONE)
        }
    }
}
