package com.example.donghodemgio

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.tooling.preview.Preview
import com.example.donghodemgio.ui.theme.DongHoDemGioTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.Locale


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
var getHour by mutableStateOf(0L)
var getMinute by mutableStateOf(0L)
var getSecond by mutableStateOf(0L)

@Composable
fun StopwatchScreen() {
    val context = LocalContext.current
    var milliseconds by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var lastLapTime by remember { mutableStateOf(0L) }

    // Sử dụng remember với mutableStateOf để lưu danh sách lap
    var laps by remember {
        mutableStateOf(
            loadLapsFromPreferences(context).toMutableList()
        )
    }

    // Thêm state cho việc sắp xếp và lọc
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST) }
    var showFastestSlowest by remember { mutableStateOf(false) }

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
    LaunchedEffect(laps) {
        saveLapsToPreferences(context, laps)
    }

    LaunchedEffect(isRunning) {
        while(isRunning) {
            delay(10)
            milliseconds += 10
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp)
        ) {
            // Background circle
            Canvas(modifier = Modifier.size(300.dp)) {
                drawCircle(
                    color = Color(0xFF00796B).copy(alpha = 0.2f),
                    style = Stroke(width = 12f)
                )
            }

            // Progress circle
            Canvas(modifier = Modifier.size(300.dp)) {
                val sweepAngle = animatedProgress * 360f
                drawArc(
                    color = Color(0xFF00796B),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )

                // Tick marks
                for (i in 0..59) {
                    val length = if (i % 5 == 0) 25f else 15f
                    drawLine(
                        color = Color(0xFF00796B).copy(alpha = 0.7f),
                        start = Offset(
                            x = (size.width / 2) + cos(i * 6 * (Math.PI / 180)).toFloat() * (size.width / 2 - 50),
                            y = (size.height / 2) + sin(i * 6 * (Math.PI / 180)).toFloat() * (size.height / 2 - 50)
                        ),
                        end = Offset(
                            x = (size.width / 2) + cos(i * 6 * (Math.PI / 180)).toFloat() * (size.width / 2 - 50 - length),
                            y = (size.height / 2) + sin(i * 6 * (Math.PI / 180)).toFloat() * (size.height / 2 - 50 - length)
                        ),
                        strokeWidth = if (i % 5 == 0) 3f else 2f
                    )
                }

                // Second hand
                val secondHandLength = size.width / 2 - 70
                val angleInRadians = (animatedProgress * 360f + 270f) * (PI / 180f)
                drawLine(
                    color = Color(0xFF00796B),
                    start = Offset(size.width / 2, size.height / 2),
                    end = Offset(
                        x = (size.width / 2) + cos(angleInRadians).toFloat() * secondHandLength,
                        y = (size.height / 2) + sin(angleInRadians).toFloat() * secondHandLength
                    ),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
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
                color = Color(0xFF00796B)
            )
            Text(
                text = String.format("%02d:", minutes.toInt()),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00796B)
            )
            Text(
                text = String.format("%02d.", seconds.toInt()),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00796B)
            )
            Text(
                text = String.format("%02d", millis),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00796B)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            // Lap button
            Button(
                onClick = {
                    var getHour:Long=hours
                    var getMinute:Long=minutes
                    var getSecond:Long=seconds
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
                                lapSecond = seconds,
                            )
                        )
                        lastLapTime = milliseconds
                        saveLapsToPreferences(context, laps)
                    }
                },
                enabled = isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
            ) {
                Text("Lap")
            }

            // Start/Pause button giữ nguyên
            Button(
                onClick = {
                    if (!isRunning && !isPaused) {
                        lastLapTime = milliseconds
                    }
                    isRunning = !isRunning
                    isPaused = !isRunning
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isRunning) Color(0xFF00796B) else Color(0xFFE57373)
                )
            ) {
                Text(if (!isRunning) "Start" else "Pause")
            }

            // Reset button với xóa laps
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
            ) {
                Text("Reset")
            }
        }

        // Sorting options

        // Show fastest/slowest switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        }

        // Lap times list with enhanced display
        // Không cần `showFastestSlowest` hoặc `Switch` nữa

// Lap times list with enhanced display
        if (laps.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                val sortedLaps = when (sortOrder) {
                    SortOrder.NEWEST -> laps.asReversed()
                    SortOrder.OLDEST -> laps
                }

                // Luôn luôn highlight fastest và slowest lap
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
                                    color = Color(0xFF00796B),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatTimestamp(lap.lapHour,lap.lapMinute,lap.lapSecond),
                                    color = Color(0xFF00796B)
                                )
                            }

                        }

                        if (index < sortedLaps.size - 1) {
                            Divider(color = Color(0xFF00796B).copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

    }
}

// Data class để lưu thông tin chi tiết về mỗi lap
data class LapData(
    val lapNumber: Int,
    val lapTime: Long,
    val totalTime: Long,
    val timestamp: Long,
    val lapHour: Long,    // Thêm thuộc tính cho giờ, phút, giây của lap
    val lapMinute: Long,
    val lapSecond: Long
)

// Enum class cho việc sắp xếp
enum class SortOrder {
    NEWEST,
    OLDEST
}

// Helper functions
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