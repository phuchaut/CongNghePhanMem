package com.example.donghodemgio

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import com.example.donghodemgio.ui.theme.DongHoDemGioTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DongHoDemGioTheme {
                TimerApp()
            }
        }
    }
}

// Global variables
var getHour by mutableStateOf(0L)
var getMinute by mutableStateOf(0L)
var getSecond by mutableStateOf(0L)

// Enums
enum class ColorTheme {
    TEAL,    // Màu mặc định (xanh ngọc)
    BLUE,    // Xanh dương
    PURPLE,  // Tím
    RED,     // Đỏ
    GREEN,    // Xanh lá
}

enum class SortOrder {
    NEWEST,
    OLDEST
}

// Data Classes
data class LapData(
    val lapNumber: Int,
    val lapTime: Long,
    val totalTime: Long,
    val timestamp: Long,
    val lapHour: Long,
    val lapMinute: Long,
    val lapSecond: Long
)

// Helper Functions
private fun formatTimestamp(hour: Long, minute: Long, second: Long): String {
    return String.format("%02d:%02d:%02d", hour, minute, second)
}

private fun saveLapsToPreferences(context: Context, laps: List<LapData>) {
    val prefs = context.getSharedPreferences("StopwatchPrefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = gson.toJson(laps)
    prefs.edit().putString("saved_laps", json).apply()
}

private fun loadLapsFromPreferences(context: Context): List<LapData> {
    val prefs = context.getSharedPreferences("StopwatchPrefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = prefs.getString("saved_laps", null)
    val type = object : TypeToken<List<LapData>>() {}.type
    return try {
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun getThemeColors(theme: ColorTheme, isDarkMode: Boolean): Triple<Color, Color, Color> {
    return when (theme) {
        ColorTheme.TEAL -> Triple(
            if (isDarkMode) Color(0xFF80CBC4) else Color(0xFF00796B),
            if (isDarkMode) Color(0xFF80CBC4).copy(alpha = 0.2f) else Color(0xFF00796B).copy(alpha = 0.2f),
            if (isDarkMode) Color(0xFF80CBC4) else Color(0xFF00796B)
        )
        ColorTheme.BLUE -> Triple(
            if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF1976D2),
            if (isDarkMode) Color(0xFF90CAF9).copy(alpha = 0.2f) else Color(0xFF1976D2).copy(alpha = 0.2f),
            if (isDarkMode) Color(0xFF90CAF9) else Color(0xFF1976D2)
        )
        ColorTheme.PURPLE -> Triple(
            if (isDarkMode) Color(0xFFCE93D8) else Color(0xFF7B1FA2),
            if (isDarkMode) Color(0xFFCE93D8).copy(alpha = 0.2f) else Color(0xFF7B1FA2).copy(alpha = 0.2f),
            if (isDarkMode) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
        )
        ColorTheme.RED -> Triple(
            if (isDarkMode) Color(0xFFEF9A9A) else Color(0xFFD32F2F),
            if (isDarkMode) Color(0xFFEF9A9A).copy(alpha = 0.2f) else Color(0xFFD32F2F).copy(alpha = 0.2f),
            if (isDarkMode) Color(0xFFEF9A9A) else Color(0xFFD32F2F)
        )
        ColorTheme.GREEN -> Triple(
            if (isDarkMode) Color(0xFFA5D6A7) else Color(0xFF388E3C),
            if (isDarkMode) Color(0xFFA5D6A7).copy(alpha = 0.2f) else Color(0xFF388E3C).copy(alpha = 0.2f),
            if (isDarkMode) Color(0xFFA5D6A7) else Color(0xFF388E3C)
        )
    }
}
@Composable
fun ColorThemeSelector(
    currentTheme: ColorTheme,
    onThemeSelected: (ColorTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Change theme color",
                tint = getThemeColors(currentTheme, false).first
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ColorTheme.values().forEach { theme ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        getThemeColors(theme, false).first,
                                        shape = CircleShape
                                    )
                            )
                            Text(theme.name.lowercase().capitalize())
                        }
                    },
                    onClick = {
                        onThemeSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StopwatchScreen() {
    val context = LocalContext.current
    var milliseconds by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var lastLapTime by remember { mutableStateOf(0L) }
    var isDarkMode by remember { mutableStateOf(false) }
    var currentTheme by remember { mutableStateOf(ColorTheme.TEAL) }

    // Get colors from current theme
    val (primaryColor, secondaryColor, textColor) = getThemeColors(currentTheme, isDarkMode)
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)

    var laps by remember { mutableStateOf(loadLapsFromPreferences(context).toMutableList()) }

    val animatedProgress by animateFloatAsState(
        targetValue = (milliseconds % 60000) / 60000f,
        animationSpec = tween(durationMillis = 10, easing = LinearEasing),
        label = "Progress Animation"
    )

    val hours = milliseconds / 3600000
    val minutes = (milliseconds % 3600000) / 60000
    val seconds = (milliseconds % 60000) / 1000
    val millis = milliseconds % 1000

    getHour = hours
    getMinute = minutes
    getSecond = seconds

    LaunchedEffect(isRunning) {
        val startTime = System.currentTimeMillis() - milliseconds
        while(isRunning) {
            milliseconds = System.currentTimeMillis() - startTime
            delay(10)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorThemeSelector(
                currentTheme = currentTheme,
                onThemeSelected = { currentTheme = it }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDarkMode) "Dark Mode" else "Light Mode",
                    color = textColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryColor,
                        checkedTrackColor = primaryColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = primaryColor,
                        uncheckedTrackColor = primaryColor.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp)
        ) {
            Canvas(modifier = Modifier.size(300.dp)) {
                drawCircle(
                    color = secondaryColor,
                    style = Stroke(width = 12f)
                )

                drawCircle(
                    color = backgroundColor,
                    radius = size.width / 2 - 25f
                )

                // Draw hour marks and numbers
                for (i in 1..12) {
                    val angle = (i * 30 - 90) * (PI / 180f)
                    val radius = size.width / 2 - 50f

                    val startRadius = size.width / 2 - 40f
                    val endRadius = size.width / 2 - 60f
                    drawLine(
                        color = primaryColor,
                        start = Offset(
                            x = (size.width / 2) + cos(angle).toFloat() * startRadius,
                            y = (size.height / 2) + sin(angle).toFloat() * startRadius
                        ),
                        end = Offset(
                            x = (size.width / 2) + cos(angle).toFloat() * endRadius,
                            y = (size.height / 2) + sin(angle).toFloat() * endRadius
                        ),
                        strokeWidth = 3f
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = textColor.toArgb()
                            textSize = 24.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }

                        val x = (size.width / 2) + cos(angle).toFloat() * radius
                        val y = (size.height / 2) + sin(angle).toFloat() * radius + 8.dp.toPx()
                        drawText(i.toString(), x, y, paint)
                    }
                }

                // Draw minute marks
                for (i in 0..59) {
                    if (i % 5 != 0) {
                        val angle = i * 6 * (PI / 180f)
                        val startRadius = size.width / 2 - 40f
                        val endRadius = size.width / 2 - 50f
                        drawLine(
                            color = primaryColor.copy(alpha = 0.5f),
                            start = Offset(
                                x = (size.width / 2) + cos(angle).toFloat() * startRadius,
                                y = (size.height / 2) + sin(angle).toFloat() * startRadius
                            ),
                            end = Offset(
                                x = (size.width / 2) + cos(angle).toFloat() * endRadius,
                                y = (size.height / 2) + sin(angle).toFloat() * endRadius
                            ),
                            strokeWidth = 1f
                        )
                    }
                }

                // Progress circle
                val sweepAngle = animatedProgress * 360f
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )

                // Second hand
                val secondHandLength = size.width / 2 - 70
                val angleInRadians = (animatedProgress * 360f + 270f) * (PI / 180f)
                drawLine(
                    color = primaryColor,
                    start = Offset(size.width / 2, size.height / 2),
                    end = Offset(
                        x = (size.width / 2) + cos(angleInRadians).toFloat() * secondHandLength,
                        y = (size.height / 2) + sin(angleInRadians).toFloat() * secondHandLength
                    ),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Center dot
                drawCircle(
                    color = primaryColor,
                    radius = 8f,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }

        // Digital display
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = String.format("%02d:", hours.toInt()),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = String.format("%02d:", minutes.toInt()),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = String.format("%02d.", seconds.toInt()),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = String.format("%02d", millis/10),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Button(
                onClick = {
                    if (isRunning) {
                        val currentLapTime = milliseconds - lastLapTime
                        laps.add(
                            LapData(
                                lapNumber = laps.size + 1,
                                lapTime = currentLapTime,
                                totalTime = milliseconds,
                                timestamp = System.currentTimeMillis(),
                                lapHour = hours,
                                lapMinute = minutes,
                                lapSecond = seconds
                            )
                        )
                        lastLapTime = milliseconds
                        saveLapsToPreferences(context, laps)
                    }
                },
                enabled = isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Lap")
            }

            Button(
                onClick = {
                    if (!isRunning && !isPaused) {
                        lastLapTime = milliseconds
                    }
                    isRunning = !isRunning
                    isPaused = !isRunning
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isRunning) primaryColor else Color(0xFFE57373)
                )
            ) {
                Text(if (!isRunning) "Start" else "Pause")
            }

            Button(
                onClick = {
                    milliseconds = 0
                    isRunning = false
                    isPaused = false
                    laps.clear()
                    lastLapTime = 0
                    saveLapsToPreferences(context, laps)
                },
                enabled = !isRunning || isPaused,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Reset")
            }
        }

        // Lap times list
        if (laps.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = surfaceColor
                )
            ) {
                val sortedLaps = laps.asReversed()
                val fastestLap = laps.minByOrNull { it.lapTime }
                val slowestLap = laps.maxByOrNull { it.lapTime }

                LazyColumn(
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(sortedLaps.size) { index ->
                        val lap = sortedLaps[index]
                        val backgroundColor = when (lap) {
                            fastestLap -> Color(0xFF81C784).copy(alpha = 0.2f)
                            slowestLap -> Color(0xFFE57373).copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Lap ${lap.lapNumber}",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatTimestamp(lap.lapHour, lap.lapMinute, lap.lapSecond),
                                    color = textColor
                                )
                            }
                        }

                        if (index < sortedLaps.size - 1) {
                            Divider(color = primaryColor.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            StopwatchScreen()
        }
    }
}