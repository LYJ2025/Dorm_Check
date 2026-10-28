package com.example.dormcheck.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dormcheck.ui.theme.LocalGlassPalette

/**
 * 玻璃风输入框：40~44px 高、12dp 圆角、1dp 边框，
 * 焦点态用主色描边（spec 3.8）。
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(12.dp)
    val hasText = value.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .softBlock(
                shape = shape,
                fill = if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.92f),
                line = palette.accent2.copy(alpha = if (hasText) 0.55f else 0.30f)
            )
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (!hasText) {
            Text(
                text = placeholder,
                fontSize = 14.sp,
                color = palette.textMuted
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = palette.text
            ),
            cursorBrush = SolidColor(palette.accent2),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
