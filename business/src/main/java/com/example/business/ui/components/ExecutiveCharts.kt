package com.example.business.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.business.data.model.BusinessSummary
import com.example.business.ui.theme.*
import java.util.Locale

@Composable
fun ExecutiveHeroBanner(
    summary: BusinessSummary,
    onQuickReports: () -> Unit,
    onQuickDebtors: () -> Unit,
    onQuickWarehouse: () -> Unit,
    onQuickChat: () -> Unit
) {
    val totalLiquidity = summary.cashInHand + summary.bankBalance
    val netPosition = totalLiquidity + summary.totalReceivables - summary.totalPayables

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(EmeraldDark, EmeraldDeep, TealAccent)
                )
            )
    ) {
        // Subtle decorative circles
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.height * 0.65f,
                center = Offset(size.width * 0.92f, size.height * 0.15f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.height * 0.45f,
                center = Offset(size.width * 0.12f, size.height * 0.95f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Ümumi Likvidlik (Kassa & Bank)",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₼ ${String.format(Locale.US, "%,.2f", totalLiquidity)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Emerald80)
                        )
                        Text(
                            text = "Xalis: ₼ ${String.format(Locale.US, "%,.0f", netPosition)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroQuickActionPill(
                    icon = Icons.Default.Assessment,
                    label = "Hesabat",
                    onClick = onQuickReports,
                    modifier = Modifier.weight(1f)
                )
                HeroQuickActionPill(
                    icon = Icons.Default.People,
                    label = "Debitorlar",
                    onClick = onQuickDebtors,
                    modifier = Modifier.weight(1f)
                )
                HeroQuickActionPill(
                    icon = Icons.Default.Inventory2,
                    label = "Anbar",
                    onClick = onQuickWarehouse,
                    modifier = Modifier.weight(1f)
                )
                HeroQuickActionPill(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "Komanda",
                    onClick = onQuickChat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroQuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MonthlyTargetTrackerCard(
    currentSales: Double,
    monthlyTarget: Double,
    onUpdateTarget: (Double) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var targetInput by remember(monthlyTarget) { mutableStateOf(monthlyTarget.toInt().toString()) }

    val progress = (currentSales / monthlyTarget.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val percentInt = (progress * 100).toInt()
    val remaining = (monthlyTarget - currentSales).coerceAtLeast(0.0)

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Aylıq Satış Hədəfi", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bu ay üçün hədəf satış məbləğini (₼) təyin edin:", fontSize = 12.sp, color = Slate500)
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Hədəf Məbləğ (₼)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTarget = targetInput.toDoubleOrNull()
                        if (newTarget != null && newTarget >= 1000.0) {
                            onUpdateTarget(newTarget)
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Yadda Saxla") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Ləğv") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column {
                        Text("Aylıq Gəlir Hədəfi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Hədəf: ₼ ${String.format(Locale.US, "%,.0f", monthlyTarget)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "$percentInt%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Hədəfi dəyiş", tint = Slate500, modifier = Modifier.size(16.dp))
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = EmeraldPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "İcra: ₼ ${String.format(Locale.US, "%,.0f", currentSales)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )
                Text(
                    text = if (remaining > 0) "Qalıq: ₼ ${String.format(Locale.US, "%,.0f", remaining)}" else "Hədəf tamamlandı! 🎉",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendChartCard(summary: BusinessSummary) {
    val trend = summary.weeklyTrend
    val dayLabels = remember(trend) {
        if (trend.isNotEmpty()) trend.map { it.dayName }
        else listOf("B.e", "Ç.a", "Çər", "C.a", "Cüm", "Şən", "Baz")
    }
    val baseDaily = remember(trend) {
        if (trend.isNotEmpty()) trend.map { it.salesAmount }
        else List(7) { 0.0 }
    }

    var selectedIndex by remember(baseDaily.size) { mutableIntStateOf((baseDaily.size - 1).coerceAtLeast(0)) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(trend) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    Text("7-Günlük Satış Dinamikası", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${dayLabels[selectedIndex]}: ₼ ${String.format(Locale.US, "%,.0f", baseDaily[selectedIndex])}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Smooth Canvas Line + Area Chart
            val maxVal = (baseDaily.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
            val minVal = (baseDaily.minOrNull() ?: 0.0) * 0.7
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                val stepX = size.width / (baseDaily.size - 1).coerceAtLeast(1)
                val points = baseDaily.mapIndexed { index, v ->
                    val x = index * stepX
                    val normY = ((v - minVal) / range).toFloat().coerceIn(0f, 1f)
                    val y = size.height - (normY * size.height * animProgress.value)
                    Offset(x, y)
                }

                // Gradient fill path
                val fillPath = Path().apply {
                    moveTo(points.first().x, size.height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val midX = (points[i].x + points[i + 1].x) / 2f
                        cubicTo(midX, points[i].y, midX, points[i + 1].y, points[i + 1].x, points[i + 1].y)
                    }
                    lineTo(points.last().x, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(EmeraldPrimary.copy(alpha = 0.28f), Color.Transparent)
                    )
                )

                // Smooth curve line
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val midX = (points[i].x + points[i + 1].x) / 2f
                        cubicTo(midX, points[i].y, midX, points[i + 1].y, points[i + 1].x, points[i + 1].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = EmeraldPrimary,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                // Data nodes
                points.forEachIndexed { idx, pt ->
                    val isSel = idx == selectedIndex
                    drawCircle(
                        color = if (isSel) EmeraldPrimary else Color.White,
                        radius = if (isSel) 9f else 6f,
                        center = pt
                    )
                    drawCircle(
                        color = EmeraldPrimary,
                        radius = if (isSel) 9f else 6f,
                        center = pt,
                        style = Stroke(width = 3f)
                    )
                }
            }

            // Interactive Day Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val isSel = index == selectedIndex
                    Surface(
                        onClick = { selectedIndex = index },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) EmeraldPrimary else Color.Transparent
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
