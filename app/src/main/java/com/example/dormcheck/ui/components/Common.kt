package com.example.dormcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dormcheck.data.DormRoom
import com.example.dormcheck.data.Student
import com.example.dormcheck.ui.theme.LocalGlassPalette

/**
 * 宿舍门号按钮：三态
 * 未点名 → 中性玻璃；已点名 → 绿色调玻璃 + 对勾；当前选中 → 渐变实心
 * 按钮内部自带一个「全部到齐」小按钮，作用范围**仅限这一间宿舍**。
 */
@Composable
fun RoomChip(
    room: DormRoom,
    selected: Boolean,
    onClick: () -> Unit,
    onMarkAllPresent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(14.dp)
    val brush = Brush.linearGradient(listOf(palette.accent2, palette.accent3))

    val contentColor = when {
        selected -> Color.White
        room.isChecked -> palette.ok
        else -> palette.textSub
    }

    Box(
        modifier = modifier
            .height(92.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.drawBehind { drawRect(brush = brush) }
                } else {
                    Modifier
                        .background(if (room.isChecked) palette.okTint else palette.softFill)
                        .border(
                            width = if (room.isChecked) 1.5.dp else 1.dp,
                            color = if (room.isChecked) palette.okLine else palette.softLine,
                            shape = shape
                        )
                }
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = room.roomNumber,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (room.isChecked) "已到 ${room.presentCount}/${room.totalCount}" else "未点名",
                fontSize = 11.sp,
                color = if (selected) Color.White.copy(alpha = 0.9f) else contentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
            if (room.totalCount > 0) {
                Spacer(Modifier.height(6.dp))
                AllPresentMiniButton(
                    done = room.isAllPresent,
                    onDark = selected,
                    onClick = onMarkAllPresent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (room.isChecked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else palette.ok),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) palette.accent else Color.White
                )
            }
        }
    }
}

/**
 * 门号卡片里的迷你「全部到齐」：
 * 只处理自己这一间宿舍；整间都到了就变灰不可再点，避免重复点击。
 */
@Composable
private fun AllPresentMiniButton(
    done: Boolean,
    onDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(7.dp)
    val fill = when {
        onDark -> Color.White.copy(alpha = if (done) 0.14f else 0.24f)
        done -> palette.okTint
        else -> Color.White.copy(alpha = 0.62f)
    }
    val line = when {
        onDark -> Color.White.copy(alpha = if (done) 0.22f else 0.55f)
        done -> palette.okLine
        else -> palette.accent.copy(alpha = 0.32f)
    }
    val textColor = when {
        onDark -> Color.White.copy(alpha = if (done) 0.70f else 1f)
        done -> palette.ok
        else -> palette.accent
    }

    Box(
        modifier = modifier
            .height(23.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, line, shape)
            .clickable(enabled = !done, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (done) "已全到" else "全部到齐",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1
        )
    }
}


/**
 * 学生按钮（点名页）：绿色=已到，红色=未到，点击双向切换。
 * 用"降色调玻璃 + 彩色描边 + 彩色文字"表达状态，比实心红绿块更贴合玻璃气质，
 * 同时保留「已到/未到」文字，不单靠颜色传达信息。
 */
@Composable
fun StudentTile(
    student: Student,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(18.dp)
    val tint = if (student.isPresent) palette.ok else palette.danger

    Box(
        modifier = modifier
            .height(84.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = if (palette.isDark) 0.24f else 0.18f), tint.copy(alpha = 0.10f))
                )
            )
            .border(
                width = if (student.isPresent) 2.dp else 1.5.dp,
                color = tint.copy(alpha = if (student.isPresent) 0.62f else 0.42f),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${student.bedNumber} 号床",
                fontSize = 11.5.sp,
                color = tint.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = student.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (student.isPresent) "已到" else "未到",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = tint.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * 学生方块（报告页，紧凑版）
 */
@Composable
fun ReportStudentTile(
    student: Student,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val tint = if (student.isPresent) palette.ok else palette.danger
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(tint.copy(alpha = if (palette.isDark) 0.20f else 0.13f))
            .border(1.dp, tint.copy(alpha = 0.32f), shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${student.bedNumber} 号床",
                fontSize = 11.sp,
                color = tint.copy(alpha = 0.8f)
            )
            Text(
                text = student.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (student.isPresent) "在寝" else "未到",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

/** 红绿图例 */
@Composable
fun StatusLegend(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        LegendDot(palette.ok, "已到")
        Spacer(Modifier.width(14.dp))
        LegendDot(palette.danger, "未到")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val palette = LocalGlassPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(text = label, fontSize = 12.sp, color = palette.textMuted)
    }
}

/** 空状态：说明"为什么空" + "下一步去哪" */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textSub
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = palette.textMuted,
            lineHeight = 21.sp
        )
    }
}
