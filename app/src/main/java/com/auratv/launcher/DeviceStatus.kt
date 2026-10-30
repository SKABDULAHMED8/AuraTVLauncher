package com.auratv.launcher

import android.app.ActivityManager
import android.content.Context
import android.hardware.input.InputManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs
import android.view.InputDevice
import java.util.Locale

data class DeviceSnapshot(
    val wifi: String,
    val memory: String,
    val storage: String,
    val inputDevices: String,
)

object DeviceStatus {
    fun read(context: Context): DeviceSnapshot {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        val caps = network?.let(connectivity::getNetworkCapabilities)
        val wifi = when {
            caps == null -> "Network: Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi: Connected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Network: Ethernet"
            else -> "Network: Connected"
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memory)
        val usedMb = (memory.totalMem - memory.availMem) / (1024 * 1024)
        val totalMb = memory.totalMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val totalStorage = stat.totalBytes
        val available = stat.availableBytes
        val used = totalStorage - available

        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val devices = inputManager.inputDeviceIds.mapNotNull { id -> inputManager.getInputDevice(id) }
            .filterNot { it.isVirtual }
        val keyboards = devices.count { it.sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD }
        val mice = devices.count { it.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE }
        val inputs = "Input devices: $keyboards keyboard(s), $mice mouse(s)"

        return DeviceSnapshot(
            wifi = wifi,
            memory = "RAM: ${formatMb(usedMb)} used / ${formatMb(totalMb)}",
            storage = "Internal storage: ${formatBytes(used)} used / ${formatBytes(totalStorage)}",
            inputDevices = inputs,
        )
    }

    private fun formatMb(value: Long) = String.format(Locale.getDefault(), "%.0f MB", value.toFloat())
    private fun formatBytes(value: Long): String {
        val gb = value / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) String.format(Locale.getDefault(), "%.1f GB", gb)
        else String.format(Locale.getDefault(), "%.0f MB", value / (1024.0 * 1024.0).toFloat())
    }
}
