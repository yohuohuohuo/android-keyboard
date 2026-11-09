package com.atharok.btremote.ui.views.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.getAdvancedKeyboardLayout
import com.atharok.btremote.domain.entities.remoteInput.RemoteButtonProperties
import com.atharok.btremote.domain.entities.remoteInput.keyboard.KeyboardLanguage
import com.atharok.btremote.domain.entities.remoteInput.keyboard.advancedKeyboard.AdvancedKeyboardKey
import com.atharok.btremote.domain.entities.remoteInput.keyboard.advancedKeyboard.IconAdvancedKeyboardKey
import com.atharok.btremote.ui.components.customButtons.IconSurfaceButton
import com.atharok.btremote.ui.theme.surfaceElevationHigh

private val directionProperties = listOf(
    RemoteButtonProperties.KeyboardUpButton, RemoteButtonProperties.KeyboardLeftButton,
    RemoteButtonProperties.KeyboardRightButton, RemoteButtonProperties.KeyboardDownButton
)
private val directionPadSize = 172.dp

private fun isDirectionKey(key: AdvancedKeyboardKey) =
    key is IconAdvancedKeyboardKey && key.properties in directionProperties

@Composable
fun AdvancedKeyboard(
    keyboardLanguage: KeyboardLanguage,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(dimensionResource(R.dimen.keyboard_key_corner_radius)),
    keyElevation: Dp = surfaceElevationHigh(),
    functionLayer: Boolean = false
) {
    val layout = remember(keyboardLanguage) { getAdvancedKeyboardLayout(keyboardLanguage) }
    val currentSend by rememberUpdatedState(sendKeyboardKeyReport)
    val reports = remember(keyboardLanguage, functionLayer) { KeyboardReportState { currentSend(it) } }
    DisposableEffect(reports) { onDispose { reports.releaseAll() } }
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val sourceRows = if (functionLayer) layout.functionLayout else layout.macLayout
    val rows = if (isPortrait) sourceRows.map { row ->
        val movedWeight = row.filter(::isDirectionKey).fold(0f) { sum, key -> sum + key.weight }
        row.filterNot(::isDirectionKey).map { key ->
            if (key is IconAdvancedKeyboardKey && key.properties == RemoteButtonProperties.KeyboardSpaceBarButton)
                key.copy(weight = key.weight + movedWeight) else key
        }.toTypedArray()
    }.toTypedArray() else sourceRows

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.testTag("keyboard-area"), contentAlignment = Alignment.Center) {
            val gridHeight = if (isPortrait) 320.dp else maxHeight.coerceAtLeast(216.dp)
            val contentHeight = maxHeight.coerceAtLeast(gridHeight + 16.dp + directionPadSize)
            val keepGridCentered = maxHeight >= gridHeight + directionPadSize * 2
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (isPortrait) {
                    Column(Modifier.fillMaxWidth().height(contentHeight), verticalArrangement = Arrangement.Center) {
                        if (keepGridCentered) Spacer(Modifier.weight(1f))
                        KeyboardGrid(rows, reports, shape, keyElevation, Modifier.height(gridHeight))
                        if (!keepGridCentered) Spacer(Modifier.height(16.dp))
                        val padModifier = if (keepGridCentered) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth()
                        Box(padModifier, contentAlignment = Alignment.Center) {
                            KeyboardDirectionPad(reports, shape, keyElevation)
                        }
                    }
                } else {
                    KeyboardGrid(rows, reports, shape, keyElevation, Modifier.height(gridHeight))
                }
            }
        }
    }
}

@Composable
private fun KeyboardGrid(
    rows: Array<Array<AdvancedKeyboardKey>>,
    reports: KeyboardReportState,
    shape: Shape,
    keyElevation: Dp,
    modifier: Modifier
) {
    Column(
        modifier.fillMaxWidth().testTag("keyboard-grid"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    key(reports, key) {
                        key.KeyboardKeyView(
                            touchDown = { reports.press(it, key.isModifier) },
                            touchUp = { reports.release(it, key.isModifier) },
                            modifier = Modifier.weight(key.weight), shape = shape, elevation = keyElevation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardDirectionPad(reports: KeyboardReportState, shape: Shape, elevation: Dp) {
    Box(Modifier.size(directionPadSize)) {
        directionProperties.forEach { properties ->
            val alignment = when (properties) {
                RemoteButtonProperties.KeyboardUpButton -> Alignment.TopCenter
                RemoteButtonProperties.KeyboardDownButton -> Alignment.BottomCenter
                RemoteButtonProperties.KeyboardLeftButton -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            IconSurfaceButton(
                image = properties.icon,
                contentDescription = stringResource(properties.stringRes),
                touchDown = { reports.press(properties.input[1], false) },
                touchUp = { reports.release(properties.input[1], false) },
                modifier = Modifier.size(52.dp).align(alignment),
                shape = shape, elevation = elevation, iconFillFraction = 0.5f
            )
        }
    }
}
