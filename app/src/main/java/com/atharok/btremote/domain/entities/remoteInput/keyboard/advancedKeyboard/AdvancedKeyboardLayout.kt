package com.atharok.btremote.domain.entities.remoteInput.keyboard.advancedKeyboard

import android.content.Context
import androidx.compose.ui.text.style.TextAlign
import com.atharok.btremote.R
import com.atharok.btremote.domain.entities.remoteInput.RemoteButtonProperties
import com.atharok.btremote.domain.entities.remoteInput.keyboard.KeyboardKey

abstract class AdvancedKeyboardLayout(protected val context: Context) {

    abstract val line1: Array<AdvancedKeyboardKey>
    abstract val line2: Array<AdvancedKeyboardKey>
    abstract val line3: Array<AdvancedKeyboardKey>
    abstract val line4: Array<AdvancedKeyboardKey>

    val layout by lazy {

        arrayOf<Array<AdvancedKeyboardKey>>(

            // F1 to F12
            arrayOf(
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F1.byte,
                    weight = 1f,
                    text = "F1"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F2.byte,
                    weight = 1f,
                    text = "F2"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F3.byte,
                    weight = 1f,
                    text = "F3"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F4.byte,
                    weight = 1f,
                    text = "F4"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F5.byte,
                    weight = 1f,
                    text = "F5"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F6.byte,
                    weight = 1f,
                    text = "F6"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F7.byte,
                    weight = 1f,
                    text = "F7"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F8.byte,
                    weight = 1f,
                    text = "F8"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F9.byte,
                    weight = 1f,
                    text = "F9"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F10.byte,
                    weight = 1f,
                    text = "F10"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F11.byte,
                    weight = 1f,
                    text = "F11"
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_F12.byte,
                    weight = 1f,
                    text = "F12"
                )
            ),

            line1, line2, line3, line4,

            // Esc, Tab, Print Screen, Arrow Up, Backspace, Enter
            arrayOf(
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_ESCAPE.byte,
                    weight = 1f,
                    text = context.getString(R.string.escape)
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardTabButton,
                    weight = 1f
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardPrintScreenButton,
                    weight = 1f
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardUpButton,
                    weight = 1f
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardBackspaceButton,
                    weight = 1.5f
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardEnterButton,
                    weight = 1.5f
                )
            ),

            // LShift, LMeta, Arrow Left, Arrow Down, Arrow Right, RMeta, RShift
            arrayOf(
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_SHIFT_LEFT.byte,
                    weight = 1f,
                    text = context.getString(R.string.shift),
                    isModifier = true,
                    textAlign = TextAlign.Start
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_META_LEFT.byte,
                    weight = 1f,
                    text = "⌘ Cmd",
                    isModifier = true,
                    textAlign = TextAlign.Start
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardLeftButton,
                    weight = 1f
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardDownButton,
                    weight = 1f
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardRightButton,
                    weight = 1f
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_META_RIGHT.byte,
                    weight = 1f,
                    text = "⌘ Cmd",
                    isModifier = true,
                    textAlign = TextAlign.End
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_SHIFT_RIGHT.byte,
                    weight = 1f,
                    text = context.getString(R.string.shift),
                    isModifier = true,
                    textAlign = TextAlign.End
                )
            ),

            // LCtrl, Alt, Space bar, Alt Gr, RCtrl
            arrayOf(
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_CTRL_LEFT.byte,
                    weight = 1f,
                    text = context.getString(R.string.ctrl),
                    isModifier = true,
                    textAlign = TextAlign.Start
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_ALT.byte,
                    weight = 1f,
                    text = "⌥ Option",
                    isModifier = true,
                    textAlign = TextAlign.Center
                ),
                IconAdvancedKeyboardKey(
                    properties = RemoteButtonProperties.KeyboardSpaceBarButton,
                    weight = 3f
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_ALT_GR.byte,
                    weight = 1f,
                    text = "⌥ Option",
                    isModifier = true,
                    textAlign = TextAlign.Center
                ),
                TextAdvancedKeyboardKey(
                    byte = KeyboardKey.KEY_CTRL_RIGHT.byte,
                    weight = 1f,
                    text = context.getString(R.string.ctrl),
                    isModifier = true,
                    textAlign = TextAlign.End
                )
            )
        )
    }
    val macLayout by lazy {
        val edit = layout[5]
        val modifiers = layout[6]
        val bottom = layout[7]
        arrayOf(
            line1 + edit[4],
            arrayOf(edit[1]) + line2,
            line3 + edit[5],
            arrayOf(modifiers[0]) + line4 + modifiers[6],
            arrayOf(bottom[0], bottom[1], modifiers[1], bottom[2], modifiers[2], modifiers[3], edit[3], modifiers[4])
        )
    }

    val functionLayout by lazy {
        arrayOf(layout[0].take(6).toTypedArray(), layout[0].drop(6).toTypedArray(), layout[5], layout[6], layout[7])
    }

}