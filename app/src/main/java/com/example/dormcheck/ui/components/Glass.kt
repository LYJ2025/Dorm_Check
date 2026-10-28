package com.example.dormcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dormcheck.ui.theme.GlassBg1
import com.example.dormcheck.ui.theme.GlassBg1Dark
import com.example.dormcheck.ui.theme.GlassBg2
import com.example.dormcheck.ui.theme.GlassBg2Dark
import com.example.dormcheck.ui.theme.GlassBg3
import com.example.dormcheck.ui.theme.GlassBg3Dark
import com.example.dormcheck.ui.theme.LocalGlassPalette
import com.example.dormcheck.ui.theme.OrbBlue
import com.example.dormcheck.ui.theme.OrbCyan
import com.example.dormcheck.ui.theme.OrbPurple
import com.example.dormcheck.ui.theme.ShadowBlue
import com.example.dormcheck.ui.theme.ShadowBlueStrong

/* ============================================================
   L0 + L1：渐变底色 + 三颗柔光球
   Android 端按规范第 5 节做移动端降级：不使用 backdrop-filter
   （Compose 里相当于真实模糊），改用「径向渐变光晕 + 半透明玻璃」，
   既保留通透感，又避免低端机掉帧与渲染鬼影。
   ============================================================ */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    val bg1 = if (palette.isDark) GlassBg1Dark else GlassBg1
    val bg2 = if (palette.isDark) GlassBg2Dark else GlassBg2
    val bg3 = if (palette.isDark) GlassBg3Dark else GlassBg3
    val orbAlpha = if (palette.isDark) 0.26f else 0.55f

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 160° 线性渐变：浅蓝 → 浅紫 → 浅青
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(bg1, bg2, bg3),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.55f, size.height)
                    )
                )

                fun orb(color: Color, cx: Float, cy: Float, radius: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = orbAlpha), color.copy(alpha = 0f)),
                            center = Offset(cx, cy),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }

                orb(OrbBlue, -30.dp.toPx(), -20.dp.toPx(), 190.dp.toPx())
                orb(OrbPurple, size.width + 30.dp.toPx(), size.height + 20.dp.toPx(), 175.dp.toPx())
                orb(OrbCyan, size.width * 0.92f, size.height * 0.40f, 130.dp.toPx())
            }
    )
}

/**
 * L2：玻璃表面（半透明白 + 1px 高光边 + 深蓝灰投影）
 * 传 tint 得到带语义色的玻璃块（红/绿/黄），仍然是"降色调"而不是实心色块。
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    tint: Color? = null
): Modifier {
    val palette = LocalGlassPalette.current
    val top = tint?.copy(alpha = if (palette.isDark) 0.22f else 0.16f) ?: palette.glassTop
    val bottom = tint?.copy(alpha = if (palette.isDark) 0.12f else 0.08f) ?: palette.glassBottom
    val line = tint?.copy(alpha = if (palette.isDark) 0.45f else 0.32f) ?: palette.glassLine

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ShadowBlue,
            spotColor = ShadowBlue
        )
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, line, shape)
}

/** 卡片：一个模块一张卡，卡片之间只用间距分隔 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape)
            .padding(contentPadding),
        content = content
    )
}

/** 内嵌软块（不用玻璃，避免玻璃叠玻璃发灰） */
@Composable
fun Modifier.softBlock(
    shape: Shape = RoundedCornerShape(12.dp),
    fill: Color? = null,
    line: Color? = null
): Modifier {
    val palette = LocalGlassPalette.current
    return this
        .clip(shape)
        .background(fill ?: palette.softFill)
        .border(1.dp, line ?: palette.softLine, shape)
}

/* ---------------------------- 按钮 ---------------------------- */

/** 全屏唯一的主按钮：蓝紫渐变 + 白字，只给"这一屏最重要的动作" */
@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(14.dp)
    val brush = Brush.linearGradient(listOf(palette.accent2, palette.accent3))

    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = ShadowBlueStrong,
                spotColor = ShadowBlueStrong
            )
            .clip(shape)
            .drawBehind {
                if (enabled) {
                    drawRect(brush = brush)
                } else {
                    drawRect(color = Color(0xFFB9C3D6))
                }
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

/** 次按钮：玻璃质感 + 主色文字 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    armed: Boolean = false,
    compact: Boolean = false
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(12.dp)
    val fill = when {
        armed -> palette.danger
        danger -> palette.dangerTint
        else -> palette.softFill
    }
    val line = when {
        armed -> Color.Transparent
        danger -> palette.danger.copy(alpha = 0.35f)
        else -> palette.softLine
    }
    val contentColor = when {
        armed -> Color.White
        danger -> palette.danger
        else -> palette.accent
    }

    Box(
        modifier = modifier
            .height(if (compact) 36.dp else 44.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = if (compact) 12.5.sp else 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ---------------------------- TAB ---------------------------- */

/** 同级视图切换：等分横排，选中项用渐变实心 */
@Composable
fun GlassTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(14.dp)
    val brush = Brush.linearGradient(listOf(palette.accent2, palette.accent3))

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = shape,
                                    clip = false,
                                    ambientColor = ShadowBlueStrong,
                                    spotColor = ShadowBlueStrong
                                )
                                .drawBehind { drawRect(brush = brush) }
                        } else {
                            Modifier
                                .background(palette.softFill)
                                .border(1.dp, palette.softLine, shape)
                        }
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else palette.textSub,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ---------------------------- 标签 / 统计 / 进度 ---------------------------- */

enum class GlassTone { Neutral, Ok, Warn, Danger, Info }

/** 胶囊标签：计数与状态 */
@Composable
fun Pill(
    text: String,
    tone: GlassTone = GlassTone.Neutral,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val (fg, bg, line) = when (tone) {
        GlassTone.Neutral -> Triple(palette.accent, palette.softFill, palette.softLine)
        GlassTone.Ok -> Triple(palette.ok, palette.okTint, palette.okLine)
        GlassTone.Warn -> Triple(palette.warn, palette.warnTint, palette.warnLine)
        GlassTone.Danger -> Triple(palette.danger, palette.dangerTint, palette.dangerLine)
        GlassTone.Info -> Triple(palette.info, palette.info.copy(alpha = 0.12f), palette.info.copy(alpha = 0.32f))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .border(1.dp, line, RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 数字强调：26/800 */
@Composable
fun StatBlock(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = palette.textMuted,
            maxLines = 1
        )
    }
}

/** 进度条：8dp 圆角，填充走渐变 */
@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color? = null
) {
    val palette = LocalGlassPalette.current
    val safeProgress = progress.coerceIn(0f, 1f)
    val fillBrush = if (color != null) {
        Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color))
    } else {
        Brush.horizontalGradient(listOf(palette.accent2, palette.accent3))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0x2E788CB4))
            .drawBehind {
                if (safeProgress > 0f) {
                    drawRect(
                        brush = fillBrush,
                        size = size.copy(width = size.width * safeProgress)
                    )
                }
            }
    )
}

/** 区块标题：16/700 + 可选右侧说明 */
@Composable
fun SectionHeading(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textStrong
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = palette.textMuted
                )
            }
        }
        trailing?.invoke()
    }
}

/** 玻璃弹窗容器：圆角 20、内边距 20，遮罩由 Dialog 提供 */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(20.dp)

    // 弹窗用接近不透明的表面 + 全屏遮罩：
    // 玻璃半透明叠在长列表上会让背后的姓名和状态透上来，对话框里必须优先保证可读性。
    val surfaceTop = if (palette.isDark) Color(0xFF272631) else Color(0xFFF9F8FE)
    val surfaceBottom = if (palette.isDark) Color(0xFF1C1B24) else Color(0xFFF0EEFA)
    val scrim = if (palette.isDark) Color.Black.copy(alpha = 0.66f) else Color(0xFF231B44).copy(alpha = 0.38f)
    val swallowTap = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 20.dp, vertical = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = ShadowBlueStrong,
                        spotColor = ShadowBlueStrong
                    )
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(surfaceTop, surfaceBottom)))
                    .border(1.dp, palette.glassLine, shape)
                    // 卡片自身吞掉点击，点内容不会误触发遮罩的关闭
                    .clickable(
                        interactionSource = swallowTap,
                        indication = null,
                        onClick = {}
                    )
                    .padding(20.dp),
                content = content
            )
        }
    }
}

/** 主标题渐变字 */
@Composable
fun titleGradientStyle(): TextStyle {
    val palette = LocalGlassPalette.current
    return TextStyle(
        fontSize = 25.sp,
        fontWeight = FontWeight.ExtraBold,
        brush = Brush.linearGradient(
            listOf(palette.textStrong, palette.accent3, palette.info)
        )
    )
}
