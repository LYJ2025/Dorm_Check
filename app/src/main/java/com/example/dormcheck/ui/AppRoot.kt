package com.example.dormcheck.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dormcheck.ui.components.GlassBackdrop
import com.example.dormcheck.ui.components.GlassButton
import com.example.dormcheck.ui.components.GlassDialog
import com.example.dormcheck.ui.components.GlassTabs
import com.example.dormcheck.ui.components.glassSurface
import com.example.dormcheck.ui.components.titleGradientStyle
import com.example.dormcheck.ui.screens.CheckInScreen
import com.example.dormcheck.ui.screens.RosterToolsDialog
import com.example.dormcheck.ui.screens.ReportScreen
import com.example.dormcheck.ui.theme.LocalGlassPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 单 Activity + ViewModel 的根组件：
 * 背景（渐变 + 光晕）→ 顶栏 → TAB → 页面 → 底部操作区。
 */
@Composable
fun DormCheckApp(viewModel: DormViewModel = viewModel()) {
    val state = viewModel.uiState
    val palette = LocalGlassPalette.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var showRosterTools by remember { mutableStateOf(false) }
    val actions = remember(viewModel) { viewModel.rosterActions() }

    fun toast(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassBackdrop()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(14.dp),
                        containerColor = palette.textStrong,
                        contentColor = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AppTopBar(
                    subtitle = listOfNotNull(state.rosterTitle, "本地数据").joinToString(" · "),
                    onRosterTools = { showRosterTools = true },
                    onReset = { showResetDialog = true }
                )

                GlassTabs(
                    items = listOf("查寝点名", "查寝报告"),
                    selectedIndex = state.selectedTab,
                    onSelect = viewModel::selectTab,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )

                when (state.selectedTab) {
                    DormUiState.TAB_REPORT -> ReportScreen(
                        state = state,
                        onGenerate = {
                            if (state.checkedRoomCount == 0) {
                                toast("还没有已点名的宿舍，请先点名")
                            } else {
                                viewModel.generateReport()
                                toast("报告已生成，共 ${state.checkedRoomCount} 间宿舍")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    else -> CheckInScreen(
                        state = state,
                        onSelectRoom = viewModel::selectRoom,
                        onToggleStudent = viewModel::toggleStudent,
                        onMarkAllPresent = { roomId ->
                            // 只对点中的这一间宿舍生效，其他宿舍保持原样
                            val result = viewModel.markRoomAllPresent(roomId)
                            toast(
                                when {
                                    result == null -> "这间宿舍还没有学生，先添加学生再点名"
                                    result.newlyMarked > 0 ->
                                        "${result.roomNumber} 全部到齐（${result.total} 人）"
                                    else -> "${result.roomNumber} 已经全部到齐"
                                }
                            )
                        },
                        actions = actions,
                        onNextRoom = {
                            when (val result = viewModel.goToNextRoom()) {
                                is DormViewModel.NextRoomResult.Moved -> toast(
                                    if (result.missingCount > 0) {
                                        "${result.fromRoom} 还有 ${result.missingCount} 人未到，已跳到 ${result.toRoom}"
                                    } else {
                                        "${result.fromRoom} 已全部到齐，已跳到 ${result.toRoom}"
                                    }
                                )

                                DormViewModel.NextRoomResult.AllRoomsChecked ->
                                    toast("全部宿舍都已完成点名")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showRosterTools) {
        RosterToolsDialog(
            state = state,
            actions = actions,
            onDismiss = { showRosterTools = false }
        )
    }

    if (showResetDialog) {
        ResetDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                viewModel.resetAll()
                showResetDialog = false
                toast("已重置本次点名，名单保留")
            }
        )
    }
}

@Composable
private fun AppTopBar(
    subtitle: String,
    onRosterTools: () -> Unit,
    onReset: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val today = remember {
        SimpleDateFormat("M 月 d 日", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 14.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "宿管晚查寝", style = titleGradientStyle())
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$today · $subtitle",
                fontSize = 12.sp,
                color = palette.textMuted,
                maxLines = 1
            )
        }
        GlassButton(
            text = "名单",
            onClick = onRosterTools,
            compact = true
        )
        Spacer(Modifier.width(8.dp))
        GlassButton(
            text = "重置",
            onClick = onReset,
            danger = true,
            compact = true
        )
    }
}

/**
 * 危险操作两步确认：第一次点只变红并换成"再点一次确认"（3 秒后自动复位）。
 */
@Composable
private fun ResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val palette = LocalGlassPalette.current
    var armed by remember { mutableStateOf(false) }

    LaunchedEffect(armed) {
        if (armed) {
            delay(3000)
            armed = false
        }
    }

    GlassDialog(onDismiss = onDismiss) {
        Text(
            text = "重置本次点名？",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.textStrong
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "所有「已到 / 未到」状态和已生成的报告都会清空，宿舍名单保留。",
            fontSize = 13.5.sp,
            lineHeight = 22.sp,
            color = palette.textSub
        )
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            GlassButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            GlassButton(
                text = if (armed) "再点一次确认" else "重置",
                onClick = {
                    if (armed) onConfirm() else armed = true
                },
                modifier = Modifier.weight(1f),
                danger = true,
                armed = armed
            )
        }
    }
}
