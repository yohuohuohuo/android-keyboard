package com.atharok.btremote.ui.components.customButtons

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import com.atharok.btremote.ui.components.DefaultElevatedCard
import com.atharok.btremote.ui.theme.surfaceElevationMedium
import com.atharok.btremote.ui.theme.surfaceElevationShadow

@Composable
private fun StatefulCustomButton(
    touchDown: () -> Unit,
    touchUp: () -> Unit,
    content: @Composable (interactionSource: MutableInteractionSource) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val currentDown by rememberUpdatedState(touchDown)
    val currentUp by rememberUpdatedState(touchUp)
    val pressed = remember { booleanArrayOf(false) }
    DisposableEffect(interactionSource) {
        onDispose { if (pressed[0]) currentUp() }
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    pressed[0] = true
                    currentDown()
                }
                is PressInteraction.Release -> { pressed[0] = false; currentUp() }
                is PressInteraction.Cancel -> { pressed[0] = false; currentUp() }
            }
        }
    }

    content(interactionSource)
}

@Composable
private fun CustomButton(
    touchDown: () -> Unit,
    touchUp: () -> Unit,
    shape: Shape,
    content: @Composable () -> Unit
) {
    StatefulCustomButton(
        touchDown = touchDown,
        touchUp = touchUp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .clipToBounds()
                .clickable(
                    interactionSource = it,
                    indication = LocalIndication.current,
                    onClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// ---- Surface Button ----

@Composable
fun SurfaceButton(
    touchDown: () -> Unit,
    touchUp: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    elevation: Dp = surfaceElevationMedium(),
    shadowElevation: Dp = surfaceElevationShadow(),
    content: @Composable () -> Unit
) {
    DefaultElevatedCard(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        shadowElevation = shadowElevation
    ) {
        CustomButton(
            touchDown = touchDown,
            touchUp = touchUp,
            shape = shape,
            content = content
        )
    }
}

// ---- Raw Button ----

@Composable
fun RawButton(
    touchDown: () -> Unit,
    touchUp: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        CustomButton(
            touchDown = touchDown,
            touchUp = touchUp,
            shape = shape,
            content = content
        )
    }
}