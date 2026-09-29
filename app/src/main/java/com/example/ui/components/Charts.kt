package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategorySpend
import com.example.model.DailySpending
import com.example.ui.theme.*
import com.example.util.Formatters
import kotlin.math.atan2

@Composable
fun DonutPieChart(
    items: List<CategorySpend>,
    totalAmount: Double,
    modifier: Modifier = Modifier,
    selectedCategory: CategorySpend? = null,
    onCategoryClick: (CategorySpend?) -> Unit = {}
) {
    if (items.isEmpty() || totalAmount <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Belum ada pengeluaran di periode ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    // Interactive Donut Chart + Center Label
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("donut_chart_canvas")
                    .pointerInput(items) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            val outerRadius = size.width / 2f
                            val innerRadius = outerRadius * 0.65f

                            if (distance in innerRadius..outerRadius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                // In Canvas, start is -90
                                var normalizedAngle = (angle + 90f) % 360f

                                var accumulated = 0f
                                var found: CategorySpend? = null
                                for (item in items) {
                                    val sweep = (item.percentage / 100f) * 360f
                                    if (normalizedAngle >= accumulated && normalizedAngle <= accumulated + sweep) {
                                        found = item
                                        break
                                    }
                                    accumulated += sweep
                                }
                                onCategoryClick(if (selectedCategory == found) null else found)
                            } else if (distance < innerRadius) {
                                onCategoryClick(null)
                            }
                        }
                    }
            ) {
                val strokeWidth = size.width * 0.18f
                val chartRadius = (size.width - strokeWidth) / 2f
                val chartTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val chartSize = Size(chartRadius * 2, chartRadius * 2)

                var startAngle = -90f
                val progress = animationProgress.value

                items.forEach { item ->
                    val sweepAngle = (item.percentage / 100f) * 360f * progress
                    val isSelected = selectedCategory?.category?.id == item.category.id
                    val effectiveStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                    drawArc(
                        color = item.category.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle.coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = chartTopLeft,
                        size = chartSize,
                        style = Stroke(width = effectiveStroke, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onCategoryClick(null) }
                    .padding(8.dp)
            ) {
                Text(
                    text = selectedCategory?.category?.title ?: "Total Beban",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = Formatters.formatCompactRupiah(selectedCategory?.totalAmount ?: totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selectedCategory != null) selectedCategory.category.color else MaterialTheme.colorScheme.onSurface
                )
                if (selectedCategory != null) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", selectedCategory.percentage)}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "${items.size} Produk",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend Breakdown Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val isSelected = selectedCategory?.category?.id == item.category.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) item.category.color.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .clickable {
                            onCategoryClick(if (isSelected) null else item)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(item.category.color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.category.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", item.percentage)}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = Formatters.formatRupiah(item.totalAmount),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DailySpendingBarChart(
    dailyData: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    if (dailyData.isEmpty() || dailyData.all { it.amount <= 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada transaksi harian untuk ditampilkan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = remember(dailyData) {
        (dailyData.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
    }

    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tren Pengeluaran Harian",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Puncak: ${Formatters.formatCompactRupiah(maxAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExpenseRed
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dailyData.forEach { day ->
                    val ratio = (day.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                    val barHeight = (ratio * 80).dp
                    val isPeak = day.isPeakDay && day.amount > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.width(22.dp)
                    ) {
                        if (isPeak) {
                            Text(
                                text = "🔥",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    when {
                                        isPeak -> ExpenseRed
                                        day.amount > 0 -> Emerald600
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isPeak) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowComparisonCard(
    totalIncome: Double,
    totalExpense: Double,
    netSavings: Double,
    savingsRate: Float,
    modifier: Modifier = Modifier
) {
    val total = (totalIncome + totalExpense).coerceAtLeast(1.0)
    val incomeRatio = (totalIncome / total).toFloat().coerceIn(0f, 1f)
    val expenseRatio = (totalExpense / total).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Arus Kas (Pemasukan vs Pengeluaran)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Two-part ratio bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                if (incomeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(incomeRatio.coerceAtLeast(0.01f))
                            .background(IncomeGreen)
                    )
                }
                if (expenseRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(expenseRatio.coerceAtLeast(0.01f))
                            .background(ExpenseRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Masuk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCompactRupiah(totalIncome),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = IncomeGreen
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Keluar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCompactRupiah(totalExpense),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ExpenseRed
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Tabungan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${String.format(java.util.Locale.US, "%.0f", savingsRate)}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (netSavings >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetProgressBar(
    budget: Double,
    spent: Double,
    onEditBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usedPercentage = if (budget > 0) ((spent / budget) * 100f).toFloat() else 0f
    val remaining = budget - spent
    val isOver = remaining < 0

    val barProgress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f

    val statusColor = when {
        isOver -> ExpenseRed
        usedPercentage > 85f -> WarningAmber
        else -> IncomeGreen
    }

    val statusLabel = when {
        budget <= 0 -> "Belum Ditentukan"
        isOver -> "Over Budget! (-${Formatters.formatCompactRupiah(kotlin.math.abs(remaining))})"
        usedPercentage > 85f -> "Hati-hati (Sisa ${Formatters.formatCompactRupiah(remaining)})"
        else -> "Aman (Sisa ${Formatters.formatCompactRupiah(remaining)})"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOver) ExpenseRedBg.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Anggaran Bulanan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isOver) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan",
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                TextButton(
                    onClick = onEditBudgetClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (budget > 0) "Atur" else "+ Pasang",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (budget > 0) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(barProgress)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(statusColor.copy(alpha = 0.8f), statusColor)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${Formatters.formatCompactRupiah(spent)} dari ${Formatters.formatCompactRupiah(budget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            } else {
                Text(
                    text = "Pasang target anggaran untuk mengontrol pengeluaran bulan ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
