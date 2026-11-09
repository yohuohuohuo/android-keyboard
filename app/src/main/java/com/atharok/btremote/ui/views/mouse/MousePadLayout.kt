package com.atharok.btremote.ui.views.mouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.common.utils.Ref
import com.atharok.btremote.domain.entities.remoteInput.MouseAction
import com.atharok.btremote.ui.components.customButtons.EmptySurfaceButton
import com.atharok.btremote.ui.components.customButtons.IconHoldButton

@Composable
fun MousePadLayout(
    mouseSpeed: Float,
    scrollSpeed: Float,
    shouldInvertMouseScrollingDirection: Boolean,
    useGyroscope: Boolean,
    sendMouseInput: (MouseAction, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val mouseAction: Ref<MouseAction> = remember { Ref(MouseAction.NONE) }

    if(useGyroscope) {
        MouseGyroscope(
            mouseSpeed = mouseSpeed,
            onMousePositionChange = { x: Float, y: Float ->
                sendMouseInput(mouseAction.value, x, y, 0f)
            }
        )
    }

    Column(modifier) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = dimensionResource(id = R.dimen.padding_min))
        ) {
            MousePad(
                mouseSpeed = mouseSpeed,
                scrollSpeed = scrollSpeed,
                sendMouseAction = {
                    mouseAction.value = it
                    sendMouseInput(mouseAction.value, 0f, 0f, 0f)
                },
                sendMousePosition = { x: Float, y: Float ->
                    sendMouseInput(mouseAction.value, x, y, 0f)
                },
                sendMouseScroll = { wheel: Float ->
                    val mouseWheel = wheel * if(shouldInvertMouseScrollingDirection) -1f else 1f
                    sendMouseInput(mouseAction.value, 0f, 0f, mouseWheel)
                },
                shape = RoundedCornerShape(
                    topStart = dimensionResource(id = R.dimen.card_corner_radius),
                    topEnd = 0.dp,
                    bottomEnd = 0.dp,
                    bottomStart = 0.dp
                ),
                modifier = Modifier.testTag("trackpad")
                    .weight(1f)
                    .padding(end = dimensionResource(id = R.dimen.padding_min))
            )

            ScrollMouseButtonsLayout(
                scrollSpeed = scrollSpeed,
                sendMouseScroll = {
                    sendMouseInput(mouseAction.value, 0f, 0f, it)
                },
                modifier = Modifier
                    .width(56.dp)
                    .padding(start = dimensionResource(id = R.dimen.padding_min))
            )
        }

        MouseButtonsLayout(
            sendMouseAction = {
                mouseAction.value = it
                sendMouseInput(mouseAction.value, 0f, 0f, 0f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = dimensionResource(id = R.dimen.padding_min))
        )
    }
}

// ---- Mouse buttons ----

@Composable
private fun MouseButtonsLayout(
    sendMouseAction: (MouseAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val leftLabel = stringResource(R.string.mouse_click_left)
    val middleLabel = stringResource(R.string.mouse_click_middle)
    val rightLabel = stringResource(R.string.mouse_click_right)
    Row(modifier = modifier, horizontalArrangement = Arrangement.Absolute.Left) {

        // Start
        EmptySurfaceButton(
            touchDown = { sendMouseAction(MouseAction.MOUSE_CLICK_LEFT) },
            touchUp = { sendMouseAction(MouseAction.NONE) },
            modifier = Modifier.semantics { contentDescription = leftLabel }
                .weight(0.38f)
                .fillMaxHeight()
                .absolutePadding(right = dimensionResource(id = R.dimen.padding_min)),
            shape = AbsoluteRoundedCornerShape(
                topLeft = 0.dp,
                topRight = 0.dp,
                bottomRight = 0.dp,
                bottomLeft = dimensionResource(id = R.dimen.card_corner_radius)
            )
        )

        // Center
        EmptySurfaceButton(
            touchDown = { sendMouseAction(MouseAction.MOUSE_CLICK_MIDDLE) },
            touchUp = { sendMouseAction(MouseAction.NONE) },
            modifier = Modifier.semantics { contentDescription = middleLabel }
                .weight(0.24f)
                .fillMaxHeight()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.padding_min)
                ),
            shape = RectangleShape
        )

        // End
        EmptySurfaceButton(
            touchDown = { sendMouseAction(MouseAction.MOUSE_CLICK_RIGHT) },
            touchUp = { sendMouseAction(MouseAction.NONE) },
            modifier = Modifier.semantics { contentDescription = rightLabel }
                .weight(0.38f)
                .fillMaxHeight()
                .absolutePadding(left = dimensionResource(id = R.dimen.padding_min)),
            shape = AbsoluteRoundedCornerShape(
                topLeft = 0.dp,
                topRight = 0.dp,
                bottomRight = dimensionResource(id = R.dimen.card_corner_radius),
                bottomLeft = 0.dp
            )
        )
    }
}

// ---- Scroll Up/Down buttons ----

@Composable
private fun ScrollMouseButtonsLayout(
    scrollSpeed: Float,
    sendMouseScroll: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        IconHoldButton(
            image = AppIcons.MouseScrollUp,
            contentDescription = stringResource(id = R.string.mouse_wheel_up),
            onHold = {
                sendMouseScroll(1f)
            },
            onRelease = {
                sendMouseScroll(0f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(bottom = dimensionResource(id = R.dimen.padding_min)),
            durationBetweenRepeatsInMillis = getScrollDurationBetweenRepeats(scrollSpeed),
            shape = RoundedCornerShape(topEnd = dimensionResource(id = R.dimen.card_corner_radius))
        )

        IconHoldButton(
            image = AppIcons.MouseScrollDown,
            contentDescription = stringResource(id = R.string.mouse_wheel_down),
            onHold = {
                sendMouseScroll(-1f)
            },
            onRelease = {
                sendMouseScroll(0f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(top = dimensionResource(id = R.dimen.padding_min)),
            durationBetweenRepeatsInMillis = getScrollDurationBetweenRepeats(scrollSpeed),
            shape = RectangleShape
        )
    }
}

private fun getScrollDurationBetweenRepeats(speed: Float): Long = speed.takeIf { it > 0f }?.let { (100f / it).toLong().coerceIn(16L, 200L) } ?: 100L