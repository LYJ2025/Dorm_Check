package com.example.dormcheck.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dormcheck.data.Student
import com.example.dormcheck.ui.DormUiState
import com.example.dormcheck.ui.DormViewModel
import com.example.dormcheck.ui.RosterActions
import com.example.dormcheck.ui.components.GlassButton
import com.example.dormcheck.ui.components.GlassDialog
import com.example.dormcheck.ui.components.GlassTextField
import com.example.dormcheck.ui.theme.LocalGlassPalette
import kotlinx.coroutines.delay

/**
 * 宿舍门号编辑器：新增与改名共用。
 */
@Composable
fun RoomEditorDialog(
    title: String,
    initialNumber: String = "",
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> DormViewModel.RosterResult
) {
    val palette = LocalGlassPalette.current
    var number by remember { mutableStateOf(initialNumber) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassDialog(onDismiss = onDismiss) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = palette.textStrong)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "门号随意填，例如 1#A630、8B-607；同一门号不能重复。",
            fontSize = 12.5.sp,
            lineHeight = 20.sp,
            color = palette.textSub
        )
        Spacer(Modifier.height(14.dp))
        GlassTextField(
            value = number,
            onValueChange = {
                number = it.take(12)
                error = null
            },
            placeholder = "宿舍门号"
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = error!!, fontSize = 12.sp, color = palette.danger)
        }
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f))
            GlassButton(
                text = confirmText,
                onClick = {
                    when (val result = onConfirm(number)) {
                        is DormViewModel.RosterResult.Ok -> onDismiss()
                        is DormViewModel.RosterResult.Failed -> error = result.message
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 学生编辑器：新增与修改床号/姓名共用。
 */
@Composable
fun StudentEditorDialog(
    title: String,
    initialName: String = "",
    initialBed: Int? = null,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, bed: String) -> DormViewModel.RosterResult
) {
    val palette = LocalGlassPalette.current
    var name by remember { mutableStateOf(initialName) }
    var bed by remember { mutableStateOf(initialBed?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassDialog(onDismiss = onDismiss) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = palette.textStrong)
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (initialName.isEmpty()) "床号留空会自动顺延到下一个空床。" else "可以改姓名或床号，床号不能与同宿舍其他人重复。",
            fontSize = 12.5.sp,
            lineHeight = 20.sp,
            color = palette.textSub
        )
        Spacer(Modifier.height(14.dp))
        GlassTextField(
            value = name,
            onValueChange = {
                name = it.take(12)
                error = null
            },
            placeholder = "学生姓名"
        )
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(120.dp)) {
                GlassTextField(
                    value = bed,
                    onValueChange = {
                        bed = it.filter(Char::isDigit).take(2)
                        error = null
                    },
                    placeholder = "床号",
                    keyboardType = KeyboardType.Number
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "床号可留空",
                fontSize = 11.5.sp,
                color = palette.textMuted,
                modifier = Modifier.weight(1f)
            )
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = error!!, fontSize = 12.sp, color = palette.danger)
        }
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(text = "取消", onClick = onDismiss, modifier = Modifier.weight(1f))
            GlassButton(
                text = confirmText,
                onClick = {
                    when (val result = onConfirm(name, bed)) {
                        is DormViewModel.RosterResult.Ok -> onDismiss()
                        is DormViewModel.RosterResult.Failed -> error = result.message
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 单间宿舍的名单管理：内置与自建的宿舍、学生都在这里改 / 删 / 加。
 * 删除走两步确认（第一次点变成"再点一次确认"，3 秒后自动复位）。
 */
@Composable
fun RoomRosterDialog(
    state: DormUiState,
    roomId: String,
    actions: RosterActions,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val room = state.rooms.firstOrNull { it.id == roomId }

    var showAddStudent by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var renamingRoom by remember { mutableStateOf(false) }
    var roomDeleteArmed by remember { mutableStateOf(false) }
    var studentDeleteArmedId by remember { mutableStateOf<String?>(null) }

    if (room == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    LaunchedEffect(roomDeleteArmed) {
        if (roomDeleteArmed) {
            delay(3000)
            roomDeleteArmed = false
        }
    }
    LaunchedEffect(studentDeleteArmedId) {
        if (studentDeleteArmedId != null) {
            delay(3000)
            studentDeleteArmedId = null
        }
    }

    GlassDialog(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${room.roomNumber} 名单",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textStrong
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${if (room.isUserAdded) "自建" else "内置"}宿舍 · ${room.totalCount} 人",
                    fontSize = 12.sp,
                    color = palette.textMuted
                )
            }
            GlassButton(
                text = "改门号",
                onClick = { renamingRoom = true },
                compact = true
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.38f).dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (room.students.isEmpty()) {
                Text(
                    text = "这间宿舍还没有人，点下面「添加学生」开始登记。",
                    fontSize = 12.5.sp,
                    color = palette.textMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                room.students.forEach { student ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${student.bedNumber} 号床",
                            fontSize = 13.sp,
                            color = palette.textSub,
                            modifier = Modifier.width(62.dp)
                        )
                        Text(
                            text = student.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textStrong,
                            modifier = Modifier.weight(1f)
                        )
                        GlassButton(
                            text = "改",
                            onClick = { editingStudent = student },
                            compact = true
                        )
                        Spacer(Modifier.width(6.dp))
                        GlassButton(
                            text = if (studentDeleteArmedId == student.id) "确认?" else "删",
                            onClick = {
                                if (studentDeleteArmedId == student.id) {
                                    actions.deleteStudent(roomId, student.id)
                                    studentDeleteArmedId = null
                                } else {
                                    studentDeleteArmedId = student.id
                                }
                            },
                            compact = true,
                            danger = true,
                            armed = studentDeleteArmedId == student.id
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        GlassButton(
            text = "＋ 添加学生",
            onClick = { showAddStudent = true },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        GlassButton(
            text = if (roomDeleteArmed) "再点一次，删除整间宿舍" else "删除宿舍",
            onClick = {
                if (roomDeleteArmed) {
                    actions.deleteRoom(room.id)
                    roomDeleteArmed = false
                    onDismiss()
                } else {
                    roomDeleteArmed = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            danger = true,
            armed = roomDeleteArmed
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "内置与自建宿舍都可以改名、删人、删整间；删除后可用「名单管理 → 重新导入内置名单」补回。",
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
            color = palette.textMuted
        )
    }

    if (showAddStudent) {
        StudentEditorDialog(
            title = "添加学生",
            initialBed = (room.students.maxOfOrNull { it.bedNumber } ?: 0) + 1,
            confirmText = "添加",
            onDismiss = { showAddStudent = false },
            onConfirm = { name, bed -> actions.addStudent(room.id, name, bed) }
        )
    }

    editingStudent?.let { student ->
        StudentEditorDialog(
            title = "修改 ${student.name}",
            initialName = student.name,
            initialBed = student.bedNumber,
            confirmText = "保存",
            onDismiss = { editingStudent = null },
            onConfirm = { name, bed -> actions.updateStudent(room.id, student.id, name, bed) }
        )
    }

    if (renamingRoom) {
        RoomEditorDialog(
            title = "修改门号",
            initialNumber = room.roomNumber,
            confirmText = "保存",
            onDismiss = { renamingRoom = false },
            onConfirm = { number -> actions.updateRoom(room.id, number) }
        )
    }
}

/**
 * 名单管理入口：统计 + 重新合并内置名单。
 */
@Composable
fun RosterToolsDialog(
    state: DormUiState,
    actions: RosterActions,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    GlassDialog(onDismiss = onDismiss) {
        Text(
            text = "名单管理",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textStrong
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "内置 ${state.builtInRoomCount} 间 · 自建 ${state.rooms.size - state.builtInRoomCount} 间 · 共 ${state.totalStudents} 人",
            fontSize = 13.sp,
            color = palette.textSub
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "内置名单在首次启动时导入本机，之后和自建名单一样可以改名、加人、删人、删整间。重新导入只会补回缺失的宿舍与学生，不会覆盖你已经做过的修改。",
            fontSize = 12.5.sp,
            lineHeight = 20.sp,
            color = palette.textMuted
        )
        if (restoreMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(text = restoreMessage!!, fontSize = 12.5.sp, color = palette.ok)
        }
        Spacer(Modifier.height(16.dp))
        GlassButton(
            text = "重新导入内置名单",
            onClick = {
                val added = actions.restoreBuiltIn()
                restoreMessage = if (added > 0) "已补回 $added 项" else "没有需要补回的项"
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        GlassButton(
            text = "关闭",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
