package com.example.surymeter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surymeter.data.DailyUsage
import com.example.surymeter.data.DayKey

@Composable
fun UsageBarChart(
    days: List<DailyUsage>,
    barColor: Color,
    todayColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val data = days.takeLast(7)
    val maxVal = data.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
    val today = DayKey.today()

    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        if (data.isEmpty()) return@Canvas
        val n = data.size
        val slot = size.width / n
        val barMaxWidth = slot * 0.55f
        val labelArea = 28f
        val chartBottom = size.height - labelArea
        val chartTop = 6f

        data.forEachIndexed { i, d ->
            val barHeight = (d.total.toFloat() / maxVal) * (chartBottom - chartTop)
            val left = i * slot + (slot - barMaxWidth) / 2f
            val top = chartBottom - barHeight
            val color = if (d.date == today) todayColor else barColor
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barMaxWidth, barHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            val layout = textMeasurer.measure(
                text = Format.dateLabel(d.date),
                style = TextStyle(fontSize = 10.sp, color = Color.Gray)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = i * slot + (slot - layout.size.width) / 2f,
                    y = chartBottom + 6f
                )
            )
        }
    }
}
