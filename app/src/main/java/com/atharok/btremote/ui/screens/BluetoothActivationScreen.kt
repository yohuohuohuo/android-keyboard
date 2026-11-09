package com.atharok.btremote.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atharok.btremote.R
import com.atharok.btremote.common.extensions.getActivity
import com.atharok.btremote.common.utils.AppIcons
import com.atharok.btremote.common.utils.checkBluetoothConnectPermission
import com.atharok.btremote.presentation.viewmodel.BluetoothActivationViewModel
import com.atharok.btremote.ui.views.ActivationView
import org.koin.androidx.compose.koinViewModel

@Composable
fun BluetoothActivationScreen(
    isBluetoothEnabled: Boolean,
    navigateToBluetoothDeviceSelectionScreen: () -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    bluetoothActivationViewModel: BluetoothActivationViewModel = koinViewModel(),
    context: Context = LocalContext.current
) {
    val hideBluetoothActivationButton by bluetoothActivationViewModel.hideBluetoothActivationButton
        .collectAsStateWithLifecycle(false)

    LaunchedEffect(isBluetoothEnabled) {
        if(isBluetoothEnabled) {
            navigateToBluetoothDeviceSelectionScreen()
        }
    }

    ActivationView(
        topBarTitle = stringResource(id = R.string.activation),
        image = AppIcons.BluetoothDisabled,
        title = stringResource(id = R.string.bluetooth_disabled_info),
        message = stringResource(id = R.string.bluetooth_disabled_message),
        buttonIcon = AppIcons.Bluetooth,
        buttonText = stringResource(id = R.string.bluetooth_enabled_button),
        buttonOnClick = {
            if (checkBluetoothConnectPermission(context)) {
                context.getActivity()?.let { activity ->
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activity.startActivity(enableBtIntent)
                }
            }
        },
        hideButton = hideBluetoothActivationButton,
        navigateToSettings = navigateToSettings,
        modifier = modifier
    )
}