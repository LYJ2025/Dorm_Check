package com.example.dormcheck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dormcheck.data.DormRoom
import com.example.dormcheck.data.MockDormData
import com.example.dormcheck.ui.DormUiState
import com.example.dormcheck.ui.RosterActions
import com.example.dormcheck.ui.components.CtaButton
import com.example.dormcheck.ui.components.GlassButton
import com.example.dormcheck.ui.components.GlassCard
import com.example.dormcheck.ui.components.GlassProgressBar
import com.example.dormcheck.ui.components.GlassTone
import com.example.dormcheck.ui.components.Pill
import com.example.dormcheck.ui.components.RoomChip
import com.example.dormcheck.ui.components.SectionHeading
import com.example.dormcheck.ui.components.StatBlock
import com.example.dormcheck.ui.components.StatusLegend
import com.example.dormcheck.ui.components.StudentTile
import com.example.dormcheck.ui.components.glassSurface
import com.example.dormcheck.ui.theme.DormCheckTheme
import com.example.dormcheck.ui.theme.LocalGlassPalette
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Tab1：查寝点名
 * 模块化三张玻璃卡片：今晚进度 → 宿舍门号 → 当前宿舍点名；底部固定"下一间宿舍"。
 * 除内置名单外，支持在这里新增宿舍与宿舍内的学生。
 */
@Composable
fun CheckInScreen(
    state: DormUiState,
    onSelectRoom: (String) -> Unit,
    onToggleStudent: (String) -> Unit,
    onMarkAllPresent: (String) -> Unit,
    onNextRoom: () -> Unit,
    actions: RosterActions,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val room = state.selectedRoom
    var showAddRoom by remember { mutableStateOf(false) }
    var editingRosterRoomId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ProgressCard(state = state) }

            if (state.rooms.isEmpty()) {
                item { EmptyRosterCard(loaded = state.loaded, onAddRoom = { showAddRoom = true }) }
            } else {
                item {
                    RoomGridCard(
                        state = state,
                        onSelectRoom = onSelectRoom,
                        onMarkAllPresent = onMarkAllPresent,
                        onAddRoomClick = { showAddRoom = true }
                    )
                }
                if (room != null) {
                    item {
                        CurrentRoomCard(
                            state = state,
                            room = room,
                            onToggleStudent = onToggleStudent,
                            onEditRoster = { editingRosterRoomId = room.id }
                        )
                    }
                }
                item {
                    Text(
                        text = "误点可再点一次切回「未到」；门号上的「全部到齐」只作用于那一间宿舍。" +
                            "宿舍与学生的增删改在顶部「名单」里。",
                        fontSize = 12.sp,
                        color = palette.textMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        if (state.rooms.isNotEmpty()) {
            CheckInBottomBar(state = state, onNextRoom = onNextRoom)
        }
    }

    if (showAddRoom) {
        RoomEditorDialog(
            title = "新增宿舍",
            confirmText = "添加宿舍",
            onDismiss = { showAddRoom = false },
            onConfirm = actions.addRoom
        )
    }

    val editingRoomId = editingRosterRoomId
    if (editingRoomId != null) {
        RoomRosterDialog(
            state = state,
            roomId = editingRoomId,
            actions = actions,
            onDismiss = { editingRosterRoomId = null }
        )
    }
}

/* ---------------------------- 卡片 1：今晚总进度 ---------------------------- */

@Composable
private fun ProgressCard(state: DormUiState) {
    val palette = LocalGlassPalette.current

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今晚查寝进度",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textStrong
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "已点名 ${state.checkedRoomCount}/${state.rooms.size} 间 · 共 ${state.totalStudents} 人",
                    fontSize = 12.5.sp,
                    color = palette.textMuted
                )
            }
            Text(
                text = "${(state.checkedRoomPercent * 100).roundToInt()}%",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.accent
            )
        }

        Spacer(Modifier.height(12.dp))
        GlassProgressBar(progress = state.checkedRoomPercent)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                value = "${state.checkedRoomCount} 间",
                label = "已点名",
                color = palette.accent,
                modifier = Modifier.weight(1f)
            )
            StatBlock(
                value = "${state.presentStudents} 人",
                label = "在寝",
                color = palette.ok,
                modifier = Modifier.weight(1f)
            )
            StatBlock(
                value = "${state.absentInCheckedRooms} 人",
                label = "未到",
                color = palette.danger,
                modifier = Modifier.weight(1f)
            )
        }

        if (state.allRoomsChecked) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.okTint)
                    .border(1.dp, palette.okLine, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "✓", color = palette.ok, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "全部 ${state.rooms.size} 间宿舍已完成点名，去「查寝报告」生成报告",
                    fontSize = 12.5.sp,
                    color = palette.ok
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "点名与自建名单都保存在本机，重启后自动恢复。",
            fontSize = 12.sp,
            color = palette.textMuted
        )
    }
}

/* ---------------------------- 空名单引导 ---------------------------- */

@Composable
private fun EmptyRosterCard(loaded: Boolean, onAddRoom: () -> Unit) {
    val palette = LocalGlassPalette.current

    GlassCard {
        Text(
            text = if (loaded) "还没有宿舍名单" else "正在读取名单…",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textStrong
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (loaded) {
                "当前版本没有内置名单（公开版默认不含），可以手动添加宿舍和学生，数据只保存在这台手机上。"
            } else {
                "稍等一下，正在从 assets/roster.json 读取内置名单。"
            },
            fontSize = 13.sp,
            lineHeight = 21.sp,
            color = palette.textSub
        )
        if (loaded) {
            Spacer(Modifier.height(16.dp))
            CtaButton(text = "新增宿舍", onClick = onAddRoom, modifier = Modifier.fillMaxWidth())
        }
    }
}

/* ---------------------------- 卡片 2：宿舍门号网格 ---------------------------- */

@Composable
private fun RoomGridCard(
    state: DormUiState,
    onSelectRoom: (String) -> Unit,
    onMarkAllPresent: (String) -> Unit,
    onAddRoomClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val columns = 3
    val rows = ceil(state.rooms.size / columns.toFloat()).toInt().coerceAtLeast(1)
    val gridHeight = (rows * 92 + (rows - 1) * 8).dp

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "宿舍门号",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textStrong
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "共 ${state.rooms.size} 间，点击门号开始点名",
                    fontSize = 12.5.sp,
                    color = palette.textMuted
                )
            }
            Pill(
                text = "已点名 ${state.checkedRoomCount}/${state.rooms.size}",
                tone = if (state.checkedRoomCount == 0) GlassTone.Neutral else GlassTone.Ok
            )
            Spacer(Modifier.width(8.dp))
            GlassButton(
                text = "＋ 宿舍",
                onClick = onAddRoomClick,
                compact = true
            )
        }
        Spacer(Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = state.rooms, key = { it.id }) { room ->
                RoomChip(
                    room = room,
                    selected = room.id == state.selectedRoomId,
                    onClick = { onSelectRoom(room.id) },
                    onMarkAllPresent = { onMarkAllPresent(room.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* ---------------------------- 卡片 3：当前宿舍点名 ---------------------------- */

@Composable
private fun CurrentRoomCard(
    state: DormUiState,
    room: DormRoom,
    onToggleStudent: (String) -> Unit,
    onEditRoster: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val index = state.rooms.indexOfFirst { it.id == room.id } + 1
    val ratio = if (room.totalCount == 0) 0f else room.presentCount.toFloat() / room.totalCount
    val rows = ceil(room.students.size / 2f).toInt().coerceAtLeast(1)
    val gridHeight = (rows * 84 + (rows - 1) * 10).dp

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${room.roomNumber} 宿舍",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textStrong
                    )
                    Spacer(Modifier.width(8.dp))
                    if (room.isUserAdded) {
                        Pill(text = "自建", tone = GlassTone.Info)
                    } else if (room.isChecked) {
                        Pill(text = "已点名", tone = GlassTone.Ok)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "第 $index/${state.rooms.size} 间 · 共 ${room.totalCount} 床",
                    fontSize = 12.5.sp,
                    color = palette.textMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "已到 ${room.presentCount}/${room.totalCount}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (room.isAllPresent) palette.ok else palette.accent
                )
                Text(text = "人", fontSize = 11.5.sp, color = palette.textMuted)
            }
        }

        Spacer(Modifier.height(12.dp))
        GlassProgressBar(
            progress = ratio,
            color = if (room.isAllPresent) palette.ok else null
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusLegend()
            Spacer(Modifier.weight(1f))
            GlassButton(text = "编辑名单", onClick = onEditRoster, compact = true)
        }

        Spacer(Modifier.height(14.dp))

        if (room.students.isEmpty()) {
            Text(
                text = "这间宿舍还没有学生，点「编辑名单 → 添加学生」。",
                fontSize = 13.sp,
                color = palette.textMuted,
                modifier = Modifier.padding(vertical = 18.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = room.students, key = { it.id }) { student ->
                    StudentTile(
                        student = student,
                        onClick = { onToggleStudent(student.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/* ---------------------------- 底部：下一间宿舍 ---------------------------- */

@Composable
private fun CheckInBottomBar(
    state: DormUiState,
    onNextRoom: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val index = state.rooms.indexOfFirst { it.id == state.selectedRoomId } + 1
    val unchecked = state.rooms.count { !it.isChecked }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                elevation = 14.dp
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "第 ${index.coerceAtLeast(1)}/${state.rooms.size} 间",
                    fontSize = 12.sp,
                    color = palette.textMuted
                )
                Text(
                    text = if (state.allRoomsChecked) "全部点名完成" else "还有 $unchecked 间未点名",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textStrong
                )
            }
            CtaButton(
                text = "下一间宿舍",
                onClick = onNextRoom,
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 830)
@Composable
private fun CheckInScreenPreview() {
    DormCheckTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE9E4FB))
        ) {
            CheckInScreen(
                state = DormUiState(rooms = MockDormData.rooms, selectedRoomId = MockDormData.rooms.first().id),
                onSelectRoom = {},
                onToggleStudent = {},
                onMarkAllPresent = {},
                onNextRoom = {},
                actions = RosterActions()
            )
        }
    }
}
