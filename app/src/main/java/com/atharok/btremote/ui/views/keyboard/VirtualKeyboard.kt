package com.atharok.btremote.ui.views.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.common.utils.REMOTE_INPUT_NONE
import com.atharok.btremote.domain.entities.remoteInput.RemoteButtonProperties
import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.VirtualKeyboardLayout
import com.atharok.btremote.presentation.viewmodel.RemoteViewModel
import com.atharok.btremote.presentation.viewmodel.TextSendStatus
import com.atharok.btremote.ui.components.customButtons.RemoteIconSurfaceButton
import com.atharok.btremote.ui.theme.surfaceElevationHigh

@Composable
fun QuickInputControlPage(
    viewModel: RemoteViewModel,
    virtualKeyboardLayout: VirtualKeyboardLayout,
    mustClearInputField: Boolean,
    modifier: Modifier = Modifier
) {
    val text by viewModel.draft.collectAsStateWithLifecycle()
    val status by viewModel.textStatus.collectAsStateWithLifecycle()
    val lastAttempt by viewModel.lastAttempt.collectAsStateWithLifecycle()
    val showAttempt = lastAttempt.isNotEmpty() && lastAttempt != text
    val busy = status == TextSendStatus.SENDING
    val hintStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    var direct by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(status) {
        if (status in listOf(TextSendStatus.FAILED, TextSendStatus.UNSUPPORTED, TextSendStatus.CANCELLED)) direct = false
    }
    val submit = { viewModel.sendDraft(virtualKeyboardLayout, mustClearInputField); Unit }
    val message = when (status) {
        TextSendStatus.IDLE, TextSendStatus.SENDING, TextSendStatus.SUBMITTED -> null
        TextSendStatus.FAILED -> R.string.quick_failed
        TextSendStatus.UNSUPPORTED -> R.string.quick_unsupported
        TextSendStatus.CANCELLED -> R.string.quick_cancelled
    }
    BoxWithConstraints(modifier) {
        // Keep the send action visible when the IME reduces the viewport.
        val inputHeight = (maxHeight - 264.dp).coerceIn(120.dp, 240.dp)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.quick_hint), style = hintStyle, color = hintColor)
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    viewModel.updateDraft(value)
                    if (direct && value.isNotEmpty() && !viewModel.sendDraft(virtualKeyboardLayout, true)) direct = false
                },
                modifier = Modifier.fillMaxWidth().height(inputHeight).testTag("quick-input").onPreviewKeyEvent { event ->
                    if (direct && !busy && event.type == KeyEventType.KeyDown) {
                        val report = AndroidKeyMapper.getKeyboardReport(event)
                        if (report != null) {
                            viewModel.sendKeyboardReport(report)
                            viewModel.sendKeyboardReport(REMOTE_INPUT_NONE)
                            true
                        } else false
                    } else false
                },
                label = { Text(stringResource(R.string.quick_edit)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onSend = { if (!busy && !direct) submit() })
            )
            Button(
                onClick = submit,
                enabled = !busy && !direct && text.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(AppIcons.Send, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.send))
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(checked = direct, onCheckedChange = { direct = it }, enabled = !busy && text.isEmpty())
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.quick_direct), style = MaterialTheme.typography.bodySmall, color = hintColor)
                    Text(stringResource(if (direct) R.string.quick_direct_hint else R.string.quick_ready), style = hintStyle, color = hintColor)
                }
            }
            if (message != null) Text(stringResource(message), style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            if (showAttempt) OutlinedTextField(
                value = lastAttempt, onValueChange = {}, readOnly = true,
                label = { Text(stringResource(R.string.quick_attempt)) }, maxLines = 2,
                modifier = Modifier.fillMaxWidth().height(80.dp).testTag("last-attempt")
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AdditionalKeyboardKeysLayout(
                    sendKeyboardKeyReport = { if (!busy) viewModel.sendKeyboardReport(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AdditionalKeyboardKeysLayout(
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    spaceBetweenButtons: Dp = dimensionResource(id = R.dimen.padding_small)
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spaceBetweenButtons)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardSpaceBarButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(3f)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardUpButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f).padding(horizontal = spaceBetweenButtons)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardPrintScreenButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardBackspaceButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardEnterButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f).padding(horizontal = spaceBetweenButtons)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardLeftButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardDownButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f).padding(horizontal = spaceBetweenButtons)
            )

            VirtualKeyboardButton(
                properties = RemoteButtonProperties.KeyboardRightButton,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VirtualKeyboardButton(
    properties: RemoteButtonProperties,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    RemoteIconSurfaceButton(
        properties = properties,
        sendKeyReport = sendKeyboardKeyReport,
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.keyboard_key_corner_radius)),
        elevation = surfaceElevationHigh(),
        iconFillFraction = 1f,
        iconPadding = dimensionResource(id = R.dimen.padding_medium)
    )
}
