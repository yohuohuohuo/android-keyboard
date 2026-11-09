package com.atharok.btremote.ui.navigation

sealed class AppNavDestination(val route: String) {
    data object BluetoothPermissionsDestination: AppNavDestination(route = "bluetooth_permissions_route")
    data object SettingsDestination: AppNavDestination(route = "settings_route")
    data object ThirdLibrariesDestination: AppNavDestination(route = "third_libraries_route")
    data object BluetoothActivationDestination: AppNavDestination(route = "bluetooth_activation_route")
    data object DeviceSelectionDestination: AppNavDestination(route = "bluetooth_device_selection_route")
    data object DeviceDiscoveryDestination: AppNavDestination(route = "bluetooth_pairing_from_a_scanned_device_route")
    data object DistantDevicePairDestination: AppNavDestination(route = "bluetooth_pairing_from_a_remote_device_route")
    data object RemoteDestination: AppNavDestination(route = "bluetooth_remote_route")
}