package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlyProfitPoint
import com.example.ui.theme.FinancialAmber
import com.example.ui.theme.FinancialBlue
import com.example.ui.theme.FinancialGreen
import com.example.ui.theme.FinancialRed
import com.example.util.Formatters
import kotlin.math.max

@Composable
fun MonthlyProfitBarChart(
    points: List<MonthlyProfitPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxVal = max(
        1000.0,
        points.maxOfOrNull { max(it.interestIncome, max(it.interestExpense, it.collections)) } ?: 1000.0
    )

    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(800))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Profit & Interest Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(color = FinancialGreen, label = "Income")
                LegendItem(color = FinancialRed, label = "Expense")
                LegendItem(color = FinancialBlue, label = "Net Profit")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barGroupWidth = w / points.size
                    val singleBarWidth = (barGroupWidth * 0.22f)

                    // Draw baseline
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(0f, h - 24.dp.toPx()),
                        end = Offset(w, h - 24.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    val availableHeight = h - 36.dp.toPx()

                    points.forEachIndexed { index, point ->
                        val groupX = index * barGroupWidth + (barGroupWidth * 0.15f)

                        // Bar 1: Income (Green)
                        val incomeHeight = ((point.interestIncome / maxVal) * availableHeight * progress.value).toFloat()
                        val incomeY = (h - 24.dp.toPx()) - incomeHeight
                        drawRoundRect(
                            color = FinancialGreen,
                            topLeft = Offset(groupX, incomeY),
                            size = Size(singleBarWidth, max(2f, incomeHeight)),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Bar 2: Expense (Red)
                        val expenseHeight = ((point.interestExpense / maxVal) * availableHeight * progress.value).toFloat()
                        val expenseY = (h - 24.dp.toPx()) - expenseHeight
                        drawRoundRect(
                            color = FinancialRed,
                            topLeft = Offset(groupX + singleBarWidth + 4f, expenseY),
                            size = Size(singleBarWidth, max(2f, expenseHeight)),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Bar 3: Net Profit (Blue)
                        val profit = max(0.0, point.netProfit)
                        val profitHeight = ((profit / maxVal) * availableHeight * progress.value).toFloat()
                        val profitY = (h - 24.dp.toPx()) - profitHeight
                        drawRoundRect(
                            color = FinancialBlue,
                            topLeft = Offset(groupX + (singleBarWidth * 2) + 8f, profitY),
                            size = Size(singleBarWidth, max(2f, profitHeight)),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Month Labels below chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { point ->
                    Text(
                        text = point.monthYear,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun LendingVsBorrowingChart(
    totalLent: Double,
    totalBorrowed: Double,
    modifier: Modifier = Modifier
) {
    val total = totalLent + totalBorrowed
    val lentRatio = if (total > 0) (totalLent / total).toFloat() else 0.5f
    val borrowedRatio = if (total > 0) (totalBorrowed / total).toFloat() else 0.5f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Lending vs Borrowing Ratio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (lentRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(lentRatio)
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(FinancialGreen, Color(0xFF34D399))),
                                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                )
                        )
                    }
                    if (borrowedRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(borrowedRatio)
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(FinancialAmber, Color(0xFFFBBF24))),
                                    RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    LegendItem(color = FinancialGreen, label = "Lent to Customers")
                    Text(
                        text = Formatters.formatCurrency(totalLent),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = FinancialGreen,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    )
                    Text(
                        text = "${(lentRatio * 100).toInt()}% of total portfolio",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    LegendItem(color = FinancialAmber, label = "Borrowed from Others")
                    Text(
                        text = Formatters.formatCurrency(totalBorrowed),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = FinancialAmber,
                        modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                    )
                    Text(
                        text = "${(borrowedRatio * 100).toInt()}% of total liability",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionTrendLineChart(
    points: List<MonthlyProfitPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxVal = max(1000.0, points.maxOfOrNull { it.collections } ?: 1000.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Collection Cashflow Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height - 20.dp.toPx()
                    val stepX = w / (points.size - 1).coerceAtLeast(1)

                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { index, pt ->
                        val x = index * stepX
                        val y = h - ((pt.collections / maxVal) * h).toFloat()

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        // Draw point dot
                        drawCircle(
                            color = FinancialGreen,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    fillPath.lineTo((points.size - 1) * stepX, h)
                    fillPath.close()

                    // Gradient fill under line
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(FinancialGreen.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )

                    // Stroke line
                    drawPath(
                        path = path,
                        color = FinancialGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { point ->
                    Text(
                        text = point.monthYear,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
