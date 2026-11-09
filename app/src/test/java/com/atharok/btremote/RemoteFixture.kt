package com.atharok.btremote

import com.atharok.btremote.domain.entities.settings.RemoteSettings
import com.atharok.btremote.domain.repositories.BluetoothRepository
import com.atharok.btremote.domain.repositories.DataStoreRepository
import com.atharok.btremote.domain.usecases.RemoteUseCase
import com.atharok.btremote.presentation.viewmodel.RemoteViewModel
import kotlinx.coroutines.flow.flowOf
import java.lang.reflect.Proxy

internal class RemoteFixture {
    val connection = kotlinx.coroutines.flow.MutableStateFlow(com.atharok.btremote.domain.entities.DeviceHidConnectionState(android.bluetooth.BluetoothHidDevice.STATE_CONNECTED, "测试 Mac"))
    val reports = mutableListOf<Pair<Int, List<Byte>>>()
    var reject = false
    val bluetooth = Proxy.newProxyInstance(BluetoothRepository::class.java.classLoader, arrayOf(BluetoothRepository::class.java)) { _, method, args ->
        when (method.name) {
            "sendReport" -> { reports += (args!![0] as Int) to (args[1] as ByteArray).toList(); !reject }
            "disconnectDevice" -> true
            "isBluetoothSupported", "isBluetoothEnabled" -> true
            "isBluetoothServiceRunning", "isBluetoothHidProfileRegistered" -> kotlinx.coroutines.flow.MutableStateFlow(true)
            "getDeviceHidConnectionState" -> connection
            "getBondedDevices" -> emptyList<com.atharok.btremote.domain.entities.DeviceEntity>()
            "getLocalDeviceName" -> "测试手机"
            else -> error("Unexpected Bluetooth call: ${method.name}")
        }
    } as BluetoothRepository
    val settings = Proxy.newProxyInstance(DataStoreRepository::class.java.classLoader, arrayOf(DataStoreRepository::class.java)) { _, method, _ ->
        when (method.name) {
            "getRemoteSettingsFlow" -> flowOf(RemoteSettings())
            "getAppearanceSettingsFlow" -> flowOf(com.atharok.btremote.domain.entities.settings.AppearanceSettings(useDynamicColors = false))
            "hideBluetoothActivationButton" -> flowOf(false)
            "getAutoConnectDeviceAddressFlow" -> flowOf("")
            "getFavoriteDevices" -> flowOf(emptyList<String>())
            else -> error("Unexpected settings call: ${method.name}")
        }
    } as DataStoreRepository
    val viewModel = RemoteViewModel(RemoteUseCase(bluetooth, settings))
}
