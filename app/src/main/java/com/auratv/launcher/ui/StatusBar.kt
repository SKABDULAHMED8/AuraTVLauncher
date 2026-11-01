package com.auratv.launcher.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun hasNetwork(context: Context): Boolean = try {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    cm?.activeNetwork != null
} catch (_: Exception) {
    false
}

/** True while the TV is connected to any network (Wi-Fi, a phone hotspot or Ethernet). */
@Composable
fun rememberNetworkConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(hasNetwork(context)) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var current: Network? = null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                current = network
                connected = true
            }

            override fun onLost(network: Network) {
                if (network == current) {
                    current = null
                    connected = false
                }
            }
        }
        val registered = try {
            cm?.registerDefaultNetworkCallback(callback)
            cm != null
        } catch (_: Exception) {
            false
        }
        connected = hasNetwork(context)
        onDispose {
            if (registered) {
                try {
                    cm?.unregisterNetworkCallback(callback)
                } catch (_: Exception) {
                }
            }
        }
    }
    return connected
}

/** A Wi-Fi symbol: green when connected, red with a slash when not (shape and colour both say it). */
@Composable
fun WifiIcon(connected: Boolean, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val d = LocalDimens.current
    val color = if (connected) {
        if (c.dark) Color(0xFF4ADE80) else Color(0xFF1B7F3B)
    } else {
        if (c.dark) Color(0xFFFF6B6B) else Color(0xFFB3261E)
    }
    val description = if (connected) "Connected to a network" else "Not connected to a network"
    Canvas(modifier.size(d.dp(32f)).semantics { contentDescription = description }) {
        val w = size.width
        val cx = w / 2f
        val cy = size.height * 0.84f
        val stroke = w * 0.09f
        for (r in listOf(0.30f, 0.55f, 0.80f)) {
            val radius = r * w
            drawArc(
                color = color,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        drawCircle(color = color, radius = stroke * 0.95f, center = Offset(cx, cy))
        if (!connected) {
            drawLine(
                color = color,
                start = Offset(w * 0.10f, size.height * 0.10f),
                end = Offset(w * 0.90f, size.height * 0.90f),
                strokeWidth = stroke * 1.1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Network symbol, date and time. The clock moves on at each minute boundary. */
@Composable
fun StatusCluster() {
    val c = LocalColors.current
    val d = LocalDimens.current
    val context = LocalContext.current
    val connected = rememberNetworkConnected()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val t = System.currentTimeMillis()
            now = t
            delay(60_000L - t % 60_000L + 50L)
        }
    }
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val dateFormat = remember(context) {
        val locale = Locale.getDefault()
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        WifiIcon(connected)
        Spacer(Modifier.width(d.dp(16f)))
        Text(dateFormat.format(Date(now)), color = c.textDim, fontSize = d.sp(21f))
        Spacer(Modifier.width(d.dp(16f)))
        Text(timeFormat.format(Date(now)), color = c.text, fontSize = d.sp(26f), fontWeight = FontWeight.Medium)
    }
}

/** App name with its tag line. */
@Composable
fun AppTitle() {
    val c = LocalColors.current
    val d = LocalDimens.current
    Column {
        Text("Aura", color = c.text, fontFamily = BrandFont, fontWeight = FontWeight.Medium, fontSize = d.sp(40f))
        Text(
            "Android UI Redesigned Architecture",
            color = c.textDim,
            fontSize = d.sp(15f),
            letterSpacing = 0.5.sp,
        )
    }
}
