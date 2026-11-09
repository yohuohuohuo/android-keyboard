package com.atharok.btremote.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharok.btremote.common.utils.KEYBOARD_REPORT_ID
import com.atharok.btremote.common.utils.MOUSE_REPORT_ID
import com.atharok.btremote.common.utils.REMOTE_REPORT_ID
import com.atharok.btremote.domain.entities.remoteInput.MouseAction
import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.VirtualKeyboardLayout
import com.atharok.btremote.domain.entities.settings.RemoteSettings
import com.atharok.btremote.domain.usecases.RemoteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class TextSendStatus { IDLE, SENDING, SUBMITTED, FAILED, UNSUPPORTED, CANCELLED }

class RemoteViewModel(private val useCase: RemoteUseCase) : ViewModel() {
    val remoteSettingsFlow: Flow<RemoteSettings> = useCase.remoteSettingsFlow
    private val _draft = MutableStateFlow("")
    val draft = _draft.asStateFlow()
    private val _lastAttempt = MutableStateFlow("")
    val lastAttempt = _lastAttempt.asStateFlow()
    private val _textStatus = MutableStateFlow(TextSendStatus.IDLE)
    val textStatus = _textStatus.asStateFlow()
    private val _inputEpoch = MutableStateFlow(0)
    val inputEpoch = _inputEpoch.asStateFlow()
    private val _reportFailed = MutableStateFlow(false)
    val reportFailed = _reportFailed.asStateFlow()
    private var inputsActive = true
    private var sendTextJob: Job? = null

    fun updateDraft(value: String) {
        _draft.value = value
        if (_textStatus.value != TextSendStatus.SENDING) _textStatus.value = TextSendStatus.IDLE
    }
    fun dismissReportFailure() { _reportFailed.value = false }

    fun disconnectDevice(): Boolean {
        setInputsActive(false)
        return useCase.disconnectDevice()
    }

    private fun submitReport(id: Int, bytes: ByteArray): Boolean = try {
        useCase.sendReport(id, bytes)
    } catch (_: RuntimeException) {
        false
    }

    fun sendReport(id: Int, bytes: ByteArray): Boolean {
        if (!inputsActive || (id == KEYBOARD_REPORT_ID && _textStatus.value == TextSendStatus.SENDING)) return false
        return submitReport(id, bytes).also { if (!it) _reportFailed.value = true }
    }

    val sendRemoteReport: (ByteArray) -> Unit = { sendReport(REMOTE_REPORT_ID, it) }
    val sendKeyboardReport: (ByteArray) -> Unit = { sendReport(KEYBOARD_REPORT_ID, it) }
    val sendMouseReport: (MouseAction, Float, Float, Float) -> Unit = { input, x, y, wheel ->
        sendReport(MOUSE_REPORT_ID, byteArrayOf(input.byte, x.roundToInt().coerceIn(-127, 127).toByte(),
            y.roundToInt().coerceIn(-127, 127).toByte(), wheel.roundToInt().toByte()))
    }

    fun sendDraft(layout: VirtualKeyboardLayout, clearAfterSend: Boolean): Boolean {
        if (!inputsActive || _textStatus.value == TextSendStatus.SENDING) return false
        val text = _draft.value
        if (text.isEmpty()) return false
        if (text.any { !layout.supports(it) }) {
            _textStatus.value = TextSendStatus.UNSUPPORTED
            return false
        }
        val epoch = _inputEpoch.value
        _lastAttempt.value = text
        _textStatus.value = TextSendStatus.SENDING
        sendTextJob = viewModelScope.launch {
            try {
                val submitted = useCase.sendTextReport(text, layout, false) { inputsActive && epoch == _inputEpoch.value }
                if (epoch == _inputEpoch.value) {
                    _textStatus.value = if (submitted) TextSendStatus.SUBMITTED else TextSendStatus.FAILED
                    if (submitted && _draft.value == text) {
                        if (clearAfterSend) _draft.value = ""
                        _lastAttempt.value = ""
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (epoch == _inputEpoch.value) _textStatus.value = TextSendStatus.FAILED
            }
        }
        return true
    }

    fun releaseAllInputs() {
        _inputEpoch.value++ // Invalidates late releases from a cancelled text task before another page can press a key.
        sendTextJob?.cancel()
        sendTextJob = null
        if (_textStatus.value == TextSendStatus.SENDING) _textStatus.value = TextSendStatus.CANCELLED
        submitReport(KEYBOARD_REPORT_ID, byteArrayOf(0, 0))
        submitReport(MOUSE_REPORT_ID, byteArrayOf(0, 0, 0, 0))
        submitReport(REMOTE_REPORT_ID, byteArrayOf(0, 0))
    }

    fun setInputsActive(active: Boolean) {
        if (!active) releaseAllInputs()
        inputsActive = active
    }

    override fun onCleared() {
        setInputsActive(false)
        super.onCleared()
    }
}
