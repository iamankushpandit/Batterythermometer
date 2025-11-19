package com.goodtime.thermometer.presentation

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.wear.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------

// Live graph: 1 sample / second, keep last 60 seconds
private const val LIVE_SAMPLE_INTERVAL_MS = 1_000L
private const val LIVE_WINDOW_SECONDS = 60L

// Snapshot collection: every 1 minute for long-term storage
private const val SNAPSHOT_SAMPLE_INTERVAL_MS = 60_000L

// Max number of snapshot points stored (3 days worth)
private const val MAX_SNAPSHOT_POINTS = 4320

// Battery considered "full" at / above this %
private const val BATTERY_FULL_THRESHOLD = 99.9f

// For snapshot axis: default horizon (in minutes) to show even if empty
private const val SNAPSHOT_DEFAULT_HORIZON_MIN = 20

// ---------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------

/**
 * Temperature display unit.
 */
enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT, KELVIN
}

/**
 * Graph modes:
 * - LIVE: last 60 seconds, updating every second.
 * - SNAPSHOT: frozen long-term view (5-minute data), not updating while shown.
 */
enum class GraphMode {
    LIVE,
    SNAPSHOT
}

/**
 * High-level screens: main vs about.
 */
enum class Screen {
    MAIN,
    ABOUT
}

/**
 * A snapshot point collected every 1 minute.
 *
 * @param secondsSinceStart Seconds elapsed since app started.
 * @param tempCelsius       Battery temperature in °C at that time.
 * @param batteryPercent    Battery level in percent at that time.
 */
data class SnapshotPoint(
    val secondsSinceStart: Float,
    val tempCelsius: Float,
    val batteryPercent: Float
)

// ---------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------

@Composable
fun ThermometerApp() {
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }
    
    // Persistent data state at app level
    var batteryTempC by remember { mutableStateOf<Float?>(null) }
    var batteryPercent by remember { mutableStateOf<Float?>(null) }
    var isCharging by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(TemperatureUnit.FAHRENHEIT) }
    var sessionSeconds by remember { mutableStateOf(0L) }
    val liveEntries = remember { mutableStateListOf<BarEntry>() }
    val snapshotPoints = remember { mutableStateListOf<SnapshotPoint>() }
    var snapshotViewEntries by remember { mutableStateOf<List<BarEntry>>(emptyList()) }
    var graphMode by remember { mutableStateOf(GraphMode.LIVE) }

    // Live sampling at app level
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            batteryTempC?.let { temp ->
                sessionSeconds++
                liveEntries.add(BarEntry(sessionSeconds.toFloat(), temp))
                
                // Keep only last 60 seconds
                val cutoff = (sessionSeconds - 60L).coerceAtLeast(0L)
                while (liveEntries.isNotEmpty() && liveEntries.first().x < cutoff.toFloat()) {
                    liveEntries.removeAt(0)
                }
            }
        }
    }

    // Snapshot sampling at app level
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            val temp = batteryTempC
            val level = batteryPercent
            if (temp != null && level != null) {
                snapshotPoints.add(SnapshotPoint(sessionSeconds.toFloat(), temp, level))
                if (snapshotPoints.size > 4320) {
                    snapshotPoints.removeAt(0)
                }
            }
        }
    }

    when (currentScreen) {
        Screen.MAIN -> TemperatureScreen(
            onShowAbout = { currentScreen = Screen.ABOUT },
            batteryTempC = batteryTempC,
            setBatteryTempC = { batteryTempC = it },
            batteryPercent = batteryPercent,
            setBatteryPercent = { batteryPercent = it },
            isCharging = isCharging,
            setIsCharging = { isCharging = it },
            unit = unit,
            setUnit = { unit = it },
            sessionSeconds = sessionSeconds,
            liveEntries = liveEntries,
            snapshotPoints = snapshotPoints,
            snapshotViewEntries = snapshotViewEntries,
            setSnapshotViewEntries = { snapshotViewEntries = it },
            graphMode = graphMode,
            setGraphMode = { graphMode = it }
        )
        Screen.ABOUT -> AboutScreen(onClose = { currentScreen = Screen.MAIN })
    }
}

// ---------------------------------------------------------------------
// Main screen: Battery Thermometer
// ---------------------------------------------------------------------

@Composable
fun TemperatureScreen(
    onShowAbout: () -> Unit,
    batteryTempC: Float?,
    setBatteryTempC: (Float?) -> Unit,
    batteryPercent: Float?,
    setBatteryPercent: (Float?) -> Unit,
    isCharging: Boolean,
    setIsCharging: (Boolean) -> Unit,
    unit: TemperatureUnit,
    setUnit: (TemperatureUnit) -> Unit,
    sessionSeconds: Long,
    liveEntries: MutableList<BarEntry>,
    snapshotPoints: MutableList<SnapshotPoint>,
    snapshotViewEntries: List<BarEntry>,
    setSnapshotViewEntries: (List<BarEntry>) -> Unit,
    graphMode: GraphMode,
    setGraphMode: (GraphMode) -> Unit
) {
    val context = LocalContext.current



    // -----------------------------------------------------------------
    // Battery broadcast receiver
    // -----------------------------------------------------------------
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return

                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                setBatteryPercent(if (level >= 0 && scale > 0) {
                    level * 100f / scale
                } else null)

                val tempTenthC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (tempTenthC != -1) {
                    setBatteryTempC(tempTenthC / 10f)
                    // Add initial point at time 0 if this is the first reading
                    if (liveEntries.isEmpty()) {
                        liveEntries.add(BarEntry(0f, tempTenthC / 10f))
                    }
                }

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                setIsCharging(status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL)
            }
        }

        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
                // ignore if already unregistered
            }
        }
    }



    // -----------------------------------------------------------------
    // Toggle between LIVE and SNAPSHOT modes
    // -----------------------------------------------------------------
    fun toggleGraphMode() {
        setGraphMode(when (graphMode) {
            GraphMode.LIVE -> {
                // When entering SNAPSHOT, take a frozen copy of all snapshot points so far
                setSnapshotViewEntries(snapshotPoints.map { point ->
                    // Store batteryPercent in data for possible future labels
                    BarEntry(point.secondsSinceStart, point.tempCelsius, point.batteryPercent)
                })
                GraphMode.SNAPSHOT
            }

            GraphMode.SNAPSHOT -> {
                // Back to live view
                GraphMode.LIVE
            }
        })
    }

    // -----------------------------------------------------------------
    // UI Layout
    // -----------------------------------------------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ---------------- Temperature text + unit + icon (only in LIVE mode) ----------------
        if (graphMode == GraphMode.LIVE) {
            if (batteryTempC != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val tempValue = when (unit) {
                        TemperatureUnit.CELSIUS -> batteryTempC!!
                        TemperatureUnit.FAHRENHEIT -> batteryTempC!! * 9 / 5 + 32
                        TemperatureUnit.KELVIN -> batteryTempC!! + 273.15f
                    }
                    val valueText = String.format("%.0f", tempValue)
                    val unitText = when (unit) {
                        TemperatureUnit.CELSIUS -> "°C"
                        TemperatureUnit.FAHRENHEIT -> "°F"
                        TemperatureUnit.KELVIN -> "K"
                    }

                    val cycleUnit: () -> Unit = {
                        setUnit(when (unit) {
                            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.CELSIUS
                            TemperatureUnit.CELSIUS -> TemperatureUnit.KELVIN
                            TemperatureUnit.KELVIN -> TemperatureUnit.FAHRENHEIT
                        })
                    }

                    // Temperature value with unit (tap to change unit)
                    Text(
                        text = "$valueText$unitText",
                        style = MaterialTheme.typography.display1,
                        modifier = Modifier.clickable { cycleUnit() }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Small, flat unit-toggle icon (swap)
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "Change unit",
                        tint = Color.White,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { cycleUnit() }
                    )
                }
            } else {
                Text("--", style = MaterialTheme.typography.display1)
                Text("Waiting for battery data...", style = MaterialTheme.typography.caption1)
            }

            // ---------------- Battery bar (only in LIVE mode) ----------------
            if (batteryPercent != null) {
                BatteryBar(
                    level = batteryPercent!!,
                    isCharging = isCharging,
                    onShowAbout = onShowAbout
                )
            } else {
                Text("Battery info unavailable", style = MaterialTheme.typography.caption1)
            }
        }

        // ---------------- Graph (live or snapshot) ----------------
        TemperatureGraph(
            mode = graphMode,
            unit = unit,
            liveEntries = liveEntries,
            snapshotEntries = snapshotViewEntries,
            sessionSeconds = sessionSeconds.toFloat(),
            onToggleMode = { toggleGraphMode() }
        )

        // ---------------- Mode toggle icon under the graph ----------------
        val pinVector = when (graphMode) {
            GraphMode.LIVE -> Icons.Filled.PushPin      // live = "pinned" to now
            GraphMode.SNAPSHOT -> Icons.Outlined.PushPin // snapshot = "unpinned", frozen
        }

        Icon(
            imageVector = pinVector,
            contentDescription = "Toggle live/snapshot",
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .clickable { toggleGraphMode() }
        )
    }
}

// ---------------------------------------------------------------------
// About screen
// ---------------------------------------------------------------------

@Composable
fun AboutScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Battery Thermometer",
            style = MaterialTheme.typography.title1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Live mode updates every second with a 60-second rolling window. " +
                    "Snapshot mode collects data every minute for long-term analysis.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Double-tap the graph to switch between live and snapshot modes. " +
                    "Tap temperature to cycle units (°F → °C → K).",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "NOTICE: This measures BATTERY temperature only.\n" +
                    "Do NOT use for room, weather, or body temperature.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.caption2
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Battery Thermometer is a proprietary application developed by GoodTime Micro® for the Samsung Galaxy Watch 5.\n\n" +
                    "The design, user experience, features, and source code of this application are protected by copyright law and may not be copied, redistributed, reverse-engineered, or modified without written permission from GoodTime Micro®.\n\n" +
                    "All trademarks, including GoodTime Micro®, are the property of their respective owners.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "© GoodTime Micro. All rights reserved.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption2
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { (context as? Activity)?.finish() }) {
            Text("Exit")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onClose) {
            Text("Back")
        }
    }
}

// ---------------------------------------------------------------------
// Battery bar
// ---------------------------------------------------------------------

/**
 * Draws the battery bar with:
 * - fill starting from LEFT, so depletion appears on the RIGHT
 * - dynamic color (green → red) based on level
 * - text & info icon always readable on top
 */
@Composable
fun BatteryBar(level: Float, isCharging: Boolean, onShowAbout: () -> Unit) {
    val clampedLevel = level.coerceIn(0f, 100f)

    // Background behind the bar
    val barBackground = Color(0xFF303030)

    // Interpolate color from green (100%) to red (0%)
    val green = Color(0xFF4CAF50)
    val red = Color(0xFFF44336)

    fun lerpColor(a: Color, b: Color, t: Float): Color {
        val r = a.red + (b.red - a.red) * t
        val g = a.green + (b.green - a.green) * t
        val bComp = a.blue + (b.blue - a.blue) * t
        return Color(r, g, bComp)
    }

    val t = 1f - (clampedLevel / 100f) // 0 at 100%, 1 at 0%
    val fillColor = lerpColor(green, red, t)

    // Compute brightness to decide text color (simple luma heuristic)
    val brightness = 0.299f * fillColor.red + 0.587f * fillColor.green + 0.114f * fillColor.blue
    val textColor = if (brightness < 0.5f) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(18.dp)
            .background(barBackground),
        contentAlignment = Alignment.Center
    ) {
        // Filled portion anchored on RIGHT so depletion appears from LEFT
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = (clampedLevel / 100f))
                .height(18.dp)
                .align(Alignment.CenterEnd)
                .background(fillColor)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Filled.BatteryChargingFull,
                        contentDescription = "Charging",
                        tint = textColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${String.format("%.1f", clampedLevel)}%",
                    style = MaterialTheme.typography.body1,
                    color = textColor
                )
            }

            // Info icon on the right (vector, flat)
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, textColor, CircleShape)
                    .clickable { onShowAbout() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "About Battery Thermometer",
                    tint = textColor,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Helper: format temperature with unit
// ---------------------------------------------------------------------

fun formatTemperature(tempC: Float, unit: TemperatureUnit): String {
    val converted = when (unit) {
        TemperatureUnit.CELSIUS -> tempC
        TemperatureUnit.FAHRENHEIT -> tempC * 9 / 5 + 32
        TemperatureUnit.KELVIN -> tempC + 273.15f
    }
    val suffix = when (unit) {
        TemperatureUnit.CELSIUS -> "°C"
        TemperatureUnit.FAHRENHEIT -> "°F"
        TemperatureUnit.KELVIN -> "K"
    }
    return "${String.format("%.0f", converted)}$suffix"
}

// ---------------------------------------------------------------------
// Helper: get temperature color based on unit
// ---------------------------------------------------------------------

fun getTemperatureColor(tempC: Float, unit: TemperatureUnit): Int {
    val tempInUnit = when (unit) {
        TemperatureUnit.CELSIUS -> tempC
        TemperatureUnit.FAHRENHEIT -> tempC * 9 / 5 + 32
        TemperatureUnit.KELVIN -> tempC + 273.15f
    }
    
    return when (unit) {
        TemperatureUnit.FAHRENHEIT -> when {
            tempInUnit < 50f -> android.graphics.Color.BLUE
            tempInUnit < 70f -> android.graphics.Color.CYAN
            tempInUnit < 80f -> android.graphics.Color.GREEN
            tempInUnit < 90f -> android.graphics.Color.YELLOW
            tempInUnit < 100f -> android.graphics.Color.parseColor("#FFA500")
            tempInUnit < 110f -> android.graphics.Color.parseColor("#FF6B35")
            else -> android.graphics.Color.RED
        }
        TemperatureUnit.CELSIUS -> when {
            tempInUnit < 10f -> android.graphics.Color.BLUE
            tempInUnit < 21f -> android.graphics.Color.CYAN
            tempInUnit < 27f -> android.graphics.Color.GREEN
            tempInUnit < 32f -> android.graphics.Color.YELLOW
            tempInUnit < 38f -> android.graphics.Color.parseColor("#FFA500")
            tempInUnit < 43f -> android.graphics.Color.parseColor("#FF6B35")
            else -> android.graphics.Color.RED
        }
        TemperatureUnit.KELVIN -> when {
            tempInUnit < 283f -> android.graphics.Color.BLUE
            tempInUnit < 294f -> android.graphics.Color.CYAN
            tempInUnit < 300f -> android.graphics.Color.GREEN
            tempInUnit < 305f -> android.graphics.Color.YELLOW
            tempInUnit < 311f -> android.graphics.Color.parseColor("#FFA500")
            tempInUnit < 316f -> android.graphics.Color.parseColor("#FF6B35")
            else -> android.graphics.Color.RED
        }
    }
}

fun createTemperatureGradient(tempC: Float, unit: TemperatureUnit): android.graphics.drawable.GradientDrawable {
    val topColor = getTemperatureColor(tempC, unit)
    val bottomColor = android.graphics.Color.BLUE
    
    return android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
        intArrayOf(bottomColor, topColor)
    )
}

// ---------------------------------------------------------------------
// Temperature graph (MPAndroidChart)
// ---------------------------------------------------------------------

@Composable
fun TemperatureGraph(
    mode: GraphMode,
    unit: TemperatureUnit,
    liveEntries: List<BarEntry>,
    snapshotEntries: List<BarEntry>,
    sessionSeconds: Float,
    onToggleMode: () -> Unit
) {
    val graphHeight = if (mode == GraphMode.SNAPSHOT) 140.dp else 100.dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(graphHeight)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onToggleMode() }
                )
            }
    ) {
        when (mode) {
            GraphMode.LIVE -> {
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(false)
                            setScaleEnabled(false)
                            isDragEnabled = false

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                textColor = android.graphics.Color.WHITE
                                setDrawGridLines(true)
                                gridColor = android.graphics.Color.argb(60, 255, 255, 255)
                                labelRotationAngle = -90f
                                granularity = 15f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val totalMinutes = value / 60f
                                        val seconds = (value % 60f).toInt()
                                        return when (seconds) {
                                            0 -> if (value >= 60f) "${totalMinutes.toInt()}m" else ""
                                            15 -> ".25m"
                                            30 -> ".50m"
                                            45 -> ".75m"
                                            else -> ""
                                        }
                                    }
                                }
                            }

                            axisLeft.apply {
                                textColor = android.graphics.Color.WHITE
                                setDrawGridLines(true)
                                gridColor = android.graphics.Color.argb(60, 255, 255, 255)
                                granularity = 1f
                            }

                            axisRight.isEnabled = false
                            data = BarData()
                        }
                    },
                    update = { chart ->
                        val data = chart.data ?: BarData().also { chart.data = it }
                        data.clearValues()
                        
                        val tempDataSet = BarDataSet(liveEntries, "Temperature").apply {
                            colors = if (liveEntries.isNotEmpty()) {
                                liveEntries.map { entry ->
                                    getTemperatureColor(entry.y, unit)
                                }
                            } else {
                                listOf(android.graphics.Color.WHITE)
                            }
                            valueTextColor = android.graphics.Color.WHITE
                            setDrawValues(false)
                        }
                        data.addDataSet(tempDataSet)

                        if (liveEntries.isNotEmpty()) {
                            val lastX = liveEntries.last().x
                            val minX = max(0f, lastX - LIVE_WINDOW_SECONDS.toFloat())
                            chart.xAxis.axisMinimum = minX
                            chart.xAxis.axisMaximum = lastX
                        }

                        chart.axisLeft.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String =
                                formatTemperature(value, unit)
                        }

                        data.notifyDataChanged()
                        chart.notifyDataSetChanged()
                        chart.invalidate()
                    }
                )
            }
            GraphMode.SNAPSHOT -> {
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(false)
                            setScaleEnabled(false)
                            isDragEnabled = false

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                textColor = android.graphics.Color.WHITE
                                setDrawGridLines(true)
                                gridColor = android.graphics.Color.argb(60, 255, 255, 255)
                                labelRotationAngle = -90f
                                granularity = 300f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val minutes = (value / 60f).toInt()
                                        return if (minutes > 0 && minutes % 5 == 0) "${minutes}m" else ""
                                    }
                                }
                            }

                            axisLeft.apply {
                                textColor = android.graphics.Color.WHITE
                                setDrawGridLines(true)
                                gridColor = android.graphics.Color.argb(60, 255, 255, 255)
                            }

                            axisRight.apply {
                                isEnabled = true
                                textColor = android.graphics.Color.WHITE
                                setDrawGridLines(false)
                                axisMinimum = 0f
                                axisMaximum = 100f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
                                }
                            }
                            data = LineData()
                        }
                    },
                    update = { chart ->
                        val data = chart.data ?: LineData().also { chart.data = it }
                        data.clearValues()
                        
                        if (snapshotEntries.isNotEmpty()) {
                            val lineEntries = snapshotEntries.map { Entry(it.x, it.y) }
                            val tempDataSet = LineDataSet(lineEntries, "Temperature").apply {
                                color = android.graphics.Color.parseColor("#FF6B35")
                                lineWidth = 2f
                                setDrawCircles(false)
                                setDrawValues(false)
                                setDrawFilled(true)
                                fillDrawable = android.graphics.drawable.GradientDrawable(
                                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                    intArrayOf(
                                        android.graphics.Color.parseColor("#80FF6B35"),
                                        android.graphics.Color.TRANSPARENT
                                    )
                                )
                                axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
                            }
                            data.addDataSet(tempDataSet)
                            
                            val batteryEntries = snapshotEntries.map { entry ->
                                Entry(entry.x, entry.data as? Float ?: 50f)
                            }
                            val batteryDataSet = LineDataSet(batteryEntries, "Battery %").apply {
                                color = android.graphics.Color.YELLOW
                                lineWidth = 1f
                                setDrawCircles(false)
                                setDrawValues(false)
                                axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.RIGHT
                            }
                            data.addDataSet(batteryDataSet)

                            val lastX = snapshotEntries.last().x
                            val lastOrDefault = max(lastX, SNAPSHOT_DEFAULT_HORIZON_MIN * 60f)
                            chart.xAxis.axisMinimum = 0f
                            chart.xAxis.axisMaximum = lastOrDefault
                        }

                        chart.axisLeft.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String =
                                formatTemperature(value, unit)
                        }

                        data.notifyDataChanged()
                        chart.notifyDataSetChanged()
                        chart.invalidate()
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun TemperatureScreenPreview() {
    ThermometerApp()
}
