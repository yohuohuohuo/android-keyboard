package com.atharok.btremote.ui.views.keyboard

// HID descriptor supports one ordinary key plus independently held modifier bits.
internal class KeyboardReportState(private val send: (ByteArray) -> Unit) {
    private var modifiers = 0
    private var key: Byte = 0

    fun press(value: Byte, isModifier: Boolean) {
        if (isModifier) modifiers = modifiers or (value.toInt() and 0xff) else key = value
        send(byteArrayOf(modifiers.toByte(), key))
    }

    fun release(value: Byte, isModifier: Boolean) {
        if (isModifier) modifiers = modifiers and (value.toInt() and 0xff).inv()
        else if (key == value) key = 0
        send(byteArrayOf(modifiers.toByte(), key))
    }

    fun releaseAll() {
        modifiers = 0
        key = 0
        send(byteArrayOf(0, 0))
    }
}
