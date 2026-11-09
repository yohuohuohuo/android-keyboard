package com.atharok.btremote.ui.screens

import android.app.Activity
import android.bluetooth.BluetoothHidDevice
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atharok.btremote.R
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.common.utils.getKeyboardLayout
import com.atharok.btremote.domain.entities.DeviceHidConnectionState
import com.atharok.btremote.domain.entities.RemoteNavigationEntity
import com.atharok.btremote.domain.entities.remoteInput.PhysicalVolumeButtonAction
import com.atharok.btremote.domain.entities.settings.RemoteSettings
import com.atharok.btremote.presentation.services.BluetoothHidService
import com.atharok.btremote.presentation.viewmodel.RemoteViewModel
import com.atharok.btremote.ui.components.*
import com.atharok.btremote.ui.views.RemoteScreenHelpModalBottomSheet
import com.atharok.btremote.ui.views.keyboard.AdvancedKeyboard
import com.atharok.btremote.ui.views.keyboard.QuickInputControlPage
import com.atharok.btremote.ui.views.mouse.MousePadLayout
import com.atharok.btremote.ui.views.remote.*
import com.atharok.btremote.ui.views.remoteNavigation.RemoteDirectionalPadNavigation
import com.atharok.btremote.ui.views.remoteNavigation.RemoteSwipeNavigation
import org.koin.androidx.compose.koinViewModel

internal enum class ControlPage(val label: Int) {
    TRACKPAD(R.string.control_trackpad), KEYBOARD(R.string.control_keyboard),
    QUICK_INPUT(R.string.control_quick_input), REMOTE(R.string.control_remote)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    bluetoothDeviceHidConnectionState: DeviceHidConnectionState,
    closeApp: () -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    remoteViewModel: RemoteViewModel = koinViewModel(),
    context: Context = LocalContext.current
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val settings by remoteViewModel.remoteSettingsFlow.collectAsStateWithLifecycle(RemoteSettings())
    val epoch by remoteViewModel.inputEpoch.collectAsStateWithLifecycle()
    val reportFailed by remoteViewModel.reportFailed.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(ControlPage.TRACKPAD) }
    var functionLayer by rememberSaveable { mutableStateOf(false) }
    var landscapeRequested by rememberSaveable { mutableStateOf(false) }
    var moreVisible by remember { mutableStateOf(false) }
    var helpVisible by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val connected = bluetoothDeviceHidConnectionState.state == BluetoothHidDevice.STATE_CONNECTED
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val snackbar = remember { SnackbarHostState() }
    val failureMessage = stringResource(R.string.control_report_failed)
    val activity = context.findActivity()
    val originalOrientation = rememberSaveable { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }

    LaunchedEffect(page, landscapeRequested) {
        activity?.requestedOrientation = if (page == ControlPage.KEYBOARD && landscapeRequested)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else originalOrientation
    }
    DisposableEffect(activity) {
        onDispose { if (activity?.isChangingConfigurations == false) activity.requestedOrientation = originalOrientation }
    }
    DisposableEffect(page, configuration.orientation, connected, lifecycle) {
        remoteViewModel.setInputsActive(connected && foreground)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { foreground = true; remoteViewModel.setInputsActive(connected) }
                Lifecycle.Event.ON_PAUSE -> { foreground = false; remoteViewModel.setInputsActive(false) }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            remoteViewModel.setInputsActive(false)
        }
    }
    LaunchedEffect(reportFailed) {
        if (reportFailed) {
            snackbar.showSnackbar(failureMessage)
            remoteViewModel.dismissReportFailure()
        }
    }
    BackHandler { remoteViewModel.releaseAllInputs(); closeApp() }
    PhysicalVolumeButtonsHandler(settings.physicalVolumeUpButtonAction, settings.physicalVolumeDownButtonAction, remoteViewModel)

    AppScaffold(
        title = bluetoothDeviceHidConnectionState.deviceName + " · " + stringResource(if (connected) R.string.control_connected else R.string.control_disconnected),
        modifier = modifier.imePadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        scrollBehavior = null,
        compactTopBar = landscape,
        snackbarHost = { SnackbarHost(snackbar) },
        topBarActions = {
            if (page == ControlPage.KEYBOARD) {
                TextButton(onClick = { remoteViewModel.releaseAllInputs(); functionLayer = !functionLayer }) {
                    Text(stringResource(if (functionLayer) R.string.control_letters else R.string.control_functions))
                }
                TextButton(onClick = { remoteViewModel.releaseAllInputs(); landscapeRequested = !landscapeRequested }) {
                    Text(stringResource(if (landscapeRequested) R.string.control_auto_orientation else R.string.control_landscape))
                }
            }
            MoreOverflowMenu { close ->
                BasicDropdownMenuItem(stringResource(R.string.more_buttons), AppIcons.MoreHoriz, onClick = { close(); moreVisible = true })
                BasicDropdownMenuItem(stringResource(R.string.disconnect), AppIcons.Disconnect, onClick = {
                    close(); keyboard?.hide(); remoteViewModel.disconnectDevice(); BluetoothHidService.stop(context)
                })
                BasicDropdownMenuItem(stringResource(R.string.help), AppIcons.Help, onClick = { close(); helpVisible = true })
                BasicDropdownMenuItem(stringResource(R.string.settings), AppIcons.Settings, onClick = {
                    close(); remoteViewModel.releaseAllInputs(); keyboard?.hide(); navigateToSettings()
                })
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().height(48.dp).selectableGroup()) {
                    ControlPage.entries.forEach { tab ->
                        val label = stringResource(tab.label)
                        val icon = when (tab) {
                            ControlPage.TRACKPAD -> AppIcons.Mouse
                            ControlPage.KEYBOARD -> AppIcons.Keyboard
                            ControlPage.QUICK_INPUT -> AppIcons.Input
                            ControlPage.REMOTE -> AppIcons.RemoteControl
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight().selectable(page == tab, role = Role.Tab, onClick = {
                                if (page != tab) {
                                    remoteViewModel.releaseAllInputs()
                                    focus.clearFocus(); keyboard?.hide(); page = tab
                                }
                            }),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val color = if (page == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(icon, null, Modifier.size(20.dp), tint = color)
                            Text(label, fontSize = 12.sp, maxLines = 1, color = color)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).padding(4.dp)) {
            if (connected && foreground) key(page, epoch, configuration.orientation) {
                when (page) {
                    ControlPage.TRACKPAD -> MousePadLayout(settings.mouseSpeed, settings.mouseScrollSpeed,
                        settings.shouldInvertMouseScrollingDirection, settings.useGyroscope, remoteViewModel.sendMouseReport, Modifier.fillMaxSize())
                    ControlPage.KEYBOARD -> AdvancedKeyboard(settings.keyboardLanguage, remoteViewModel.sendKeyboardReport,
                        Modifier.fillMaxSize(), functionLayer = functionLayer)
                    ControlPage.QUICK_INPUT -> QuickInputControlPage(remoteViewModel, remember(settings.keyboardLanguage) {
                        getKeyboardLayout(settings.keyboardLanguage)
                    }, settings.mustClearInputField, Modifier.fillMaxSize())
                    ControlPage.REMOTE -> RemoteControlPage(settings, remoteViewModel, { moreVisible = true })
                }
            }
        }
    }
    when {
        bluetoothDeviceHidConnectionState.state == BluetoothHidDevice.STATE_DISCONNECTING -> LoadingDialog(
            title = stringResource(R.string.connection),
            message = stringResource(R.string.bluetooth_device_disconnecting_message, bluetoothDeviceHidConnectionState.deviceName),
            buttonText = stringResource(R.string.disconnect),
            onButtonClick = { remoteViewModel.disconnectDevice(); BluetoothHidService.stop(context) }
        )
        helpVisible -> RemoteScreenHelpModalBottomSheet(onDismissRequest = { helpVisible = false })
        moreVisible -> MoreButtonsDialog(remoteViewModel.sendRemoteReport, remoteViewModel.sendKeyboardReport, { moreVisible = false })
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun RemoteControlPage(settings: RemoteSettings, vm: RemoteViewModel, showMore: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val remote: @Composable () -> Unit = {
            RemoteButtonsLayouts(settings.useMinimalistRemote, showMore, vm.sendRemoteReport, vm.sendKeyboardReport)
        }
        val direction: @Composable () -> Unit = {
            RemotePadLayout(settings.remoteNavigationEntity, vm.sendRemoteReport, vm.sendKeyboardReport, settings.useEnterForSelection)
        }
        if (maxWidth > maxHeight) Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(2f).fillMaxHeight()) { remote() }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { direction() }
        } else Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(3f).fillMaxWidth()) { remote() }
            Box(Modifier.weight(2f).fillMaxWidth(), contentAlignment = Alignment.Center) { direction() }
        }
    }
}

@Composable
private fun RemoteButtonsLayouts(
    useMinimalistRemote: Boolean,
    onMoreButtonsVisibleChanged: () -> Unit,
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit
) {
    if (useMinimalistRemote) {
        var isTVChannelButtonsVisible: Boolean by remember { mutableStateOf(false) }

        MinimalistRemoteView(
            sendRemoteKeyReport = sendRemoteKeyReport,
            onTVChannelButtonsChanged = {
                isTVChannelButtonsVisible = !isTVChannelButtonsVisible
            },
            onMoreButtonsVisibleChanged = onMoreButtonsVisibleChanged,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_normal))
        )

        if (isTVChannelButtonsVisible) {
            TVChannelDialog(
                sendRemoteKeyReport = sendRemoteKeyReport,
                sendKeyboardKeyReport = sendKeyboardKeyReport,
                onDismissRequest = {
                    isTVChannelButtonsVisible = false
                }
            )
        }
    } else {
        RemoteView(
            sendRemoteKeyReport = sendRemoteKeyReport,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_normal))
        )
    }
}

@Composable
private fun RemotePadLayout(
    remoteNavigationMode: RemoteNavigationEntity,
    sendRemoteKeyReport: (ByteArray) -> Unit,
    sendKeyboardKeyReport: (ByteArray) -> Unit,
    useEnterForSelection: Boolean
) {
    if(remoteNavigationMode == RemoteNavigationEntity.D_PAD) {
        RemoteDirectionalPadNavigation(
            sendRemoteKeyReport = sendRemoteKeyReport,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            useEnterForSelection = useEnterForSelection,
            modifier = Modifier.aspectRatio(1f)
        )
    } else {
        RemoteSwipeNavigation(
            sendRemoteKeyReport = sendRemoteKeyReport,
            sendKeyboardKeyReport = sendKeyboardKeyReport,
            useEnterForSelection = useEnterForSelection,
            modifier = Modifier
        )
    }
}

@Composable
private fun PhysicalVolumeButtonsHandler(
    upAction: PhysicalVolumeButtonAction,
    downAction: PhysicalVolumeButtonAction,
    remoteViewModel: RemoteViewModel
) {
    if(upAction != PhysicalVolumeButtonAction.DEFAULT || downAction != PhysicalVolumeButtonAction.DEFAULT) {
        PhysicalVolumeButtonsHandler(
            onVolumeUpPressed = {
                upAction.doPhysicalVolumeAction {
                    remoteViewModel.sendReport(it.hidReportId, it.pressedBytes)
                }
            },
            onVolumeUpReleased = {
                upAction.doPhysicalVolumeAction {
                    remoteViewModel.sendReport(it.hidReportId, it.releasedBytes)
                }
            },
            onVolumeDownPressed = {
                downAction.doPhysicalVolumeAction {
                    remoteViewModel.sendReport(it.hidReportId, it.pressedBytes)
                }
            },
            onVolumeDownReleased = {
                downAction.doPhysicalVolumeAction {
                    remoteViewModel.sendReport(it.hidReportId, it.releasedBytes)
                }
            }
        )
    }
}

private fun PhysicalVolumeButtonAction.doPhysicalVolumeAction(
    sendReport: (PhysicalVolumeButtonAction) -> Unit
): Boolean {
    return when (this) {
        PhysicalVolumeButtonAction.DEFAULT -> false
        else -> { sendReport(this); true }
    }
}
