package com.example.dormcheck.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dormcheck.data.CheckReport
import com.example.dormcheck.data.MockDormData
import com.example.dormcheck.data.ReportRoom
import com.example.dormcheck.ui.DormUiState
import com.example.dormcheck.ui.components.CtaButton
import com.example.dormcheck.ui.components.EmptyState
import com.example.dormcheck.ui.components.GlassCard
import com.example.dormcheck.ui.components.GlassProgressBar
import com.example.dormcheck.ui.components.GlassTone
import com.example.dormcheck.ui.components.Pill
import com.example.dormcheck.ui.components.ReportStudentTile
import com.example.dormcheck.ui.components.glassSurface
import com.example.dormcheck.ui.theme.DormCheckTheme
import com.example.dormcheck.ui.theme.LocalGlassPalette
import kotlin.math.roundToInt

/**
 * Tab2：查寝报告
 * 总览 → 未点名宿舍提示 → 分宿舍明细 → 不在寝名单；底部固定"一键生成报告"。
 */
@Composable
fun ReportScreen(
    state: DormUiState,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report = state.report
    val palette = LocalGlassPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        if (report == null) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GlassCard {
                        EmptyState(
                            title = "还没有生成报告",
                            description = "点击下方「一键生成报告」，把已点名宿舍的\n在寝 / 未到情况汇总成一页，方便快速核对。",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { SummaryCard(report = report) }

                if (report.uncheckedRoomNumbers.isNotEmpty()) {
                    item { UncheckedRoomsCard(roomNumbers = report.uncheckedRoomNumbers) }
                }

                item {
                    Text(
                        text = "分宿舍明细",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textStrong,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                items(items = report.rooms, key = { it.roomNumber }) { room ->
                    ReportRoomCard(room = room)
                }

                if (report.absentList.isNotEmpty()) {
                    item { AbsentListCard(report = report) }
                }

                item {
                    Text(
                        text = "报告是生成时的数据快照，之后修改点名结果不会自动更新，重新生成即可。",
                        fontSize = 12.sp,
                        color = palette.textMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        ReportBottomBar(state = state, hasReport = report != null, onGenerate = onGenerate)
    }
}

/* ---------------------------- 总览 ---------------------------- */

@Composable
private fun SummaryCard(report: CheckReport) {
    val palette = LocalGlassPalette.current
    val ratio = if (report.totalStudents == 0) 0f else report.presentStudents.toFloat() / report.totalStudents

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "查寝总览",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textStrong
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "生成时间 ${report.generatedAt}",
                    fontSize = 12.5.sp,
                    color = palette.textMuted
                )
            }
            Pill(
                text = "在寝率 ${(ratio * 100).roundToInt()}%",
                tone = if (ratio >= 1f) GlassTone.Ok else GlassTone.Info
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock2(
                value = "${report.checkedRoomCount} 间",
                label = "已点名宿舍",
                color = palette.accent,
                modifier = Modifier.weight(1f)
            )
            StatBlock2(
                value = "${report.totalStudents} 人",
                label = "总人数",
                color = palette.textStrong,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock2(
                value = "${report.presentStudents} 人",
                label = "在寝人数",
                color = palette.ok,
                modifier = Modifier.weight(1f)
            )
            StatBlock2(
                value = "${report.absentStudents} 人",
                label = "不在寝人数",
                color = palette.danger,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        GlassProgressBar(progress = ratio, color = palette.ok)
    }
}

@Composable
private fun StatBlock2(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 12.sp, color = palette.textMuted, maxLines = 1)
    }
}

/* ---------------------------- 未点名宿舍 ---------------------------- */

@Composable
private fun UncheckedRoomsCard(roomNumbers: List<String>) {
    val palette = LocalGlassPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(tint = palette.warn, shape = RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "未点名宿舍（${roomNumbers.size} 间）",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = palette.warn
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = roomNumbers.joinToString("、"),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.warn
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "本次尚未查寝，不计入在寝 / 不在寝统计，请勿与「未到」混淆。",
            fontSize = 12.sp,
            color = palette.warn.copy(alpha = 0.85f)
        )
    }
}

/* ---------------------------- 单间明细 ---------------------------- */

@Composable
private fun ReportRoomCard(room: ReportRoom) {
    val palette = LocalGlassPalette.current
    val allPresent = room.presentCount == room.totalCount

    GlassCard(shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${room.roomNumber} 宿舍",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textStrong
            )
            Spacer(Modifier.width(8.dp))
            Pill(
                text = "在寝 ${room.presentCount}/${room.totalCount}",
                tone = if (allPresent) GlassTone.Ok else GlassTone.Info
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (allPresent) "全部到齐" else "缺 ${room.totalCount - room.presentCount} 人",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (allPresent) palette.ok else palette.danger
            )
        }

        Spacer(Modifier.height(12.dp))

        val chunked = room.students.chunked(2)
        chunked.forEachIndexed { index, rowStudents ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowStudents.forEach { student ->
                    ReportStudentTile(student = student, modifier = Modifier.weight(1f))
                }
                if (rowStudents.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            if (index != chunked.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/* ---------------------------- 不在寝名单 ---------------------------- */

@Composable
private fun AbsentListCard(report: CheckReport) {
    val palette = LocalGlassPalette.current

    GlassCard(shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "不在寝名单",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textStrong
            )
            Spacer(Modifier.width(8.dp))
            Pill(text = "${report.absentStudents} 人", tone = GlassTone.Danger)
            Spacer(Modifier.weight(1f))
            Text(
                text = "可直接上报",
                fontSize = 11.5.sp,
                color = palette.textMuted
            )
        }

        Spacer(Modifier.height(12.dp))

        report.absentList.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.first} 宿舍",
                    fontSize = 13.sp,
                    color = palette.textSub
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${item.second} 号床",
                    fontSize = 13.sp,
                    color = palette.textSub
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.third,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textStrong
                )
            }
            if (index != report.absentList.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.softLine)
                )
            }
        }
    }
}

/* ---------------------------- 底部：生成报告 ---------------------------- */

@Composable
private fun ReportBottomBar(
    state: DormUiState,
    hasReport: Boolean,
    onGenerate: () -> Unit
) {
    val palette = LocalGlassPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                elevation = 14.dp
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = if (state.checkedRoomCount == 0) {
                    "还没有已点名的宿舍，请先到「查寝点名」登记"
                } else {
                    "已点名 ${state.checkedRoomCount}/${state.rooms.size} 间，报告只统计已点名宿舍"
                },
                fontSize = 12.sp,
                color = if (state.checkedRoomCount == 0) palette.danger else palette.textMuted
            )
            Spacer(Modifier.height(8.dp))
            CtaButton(
                text = if (hasReport) "重新生成报告" else "一键生成报告",
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 860)
@Composable
private fun ReportScreenPreview() {
    val demoRooms = MockDormData.rooms.take(3).mapIndexed { roomIndex, room ->
        ReportRoom(
            roomNumber = room.roomNumber,
            students = room.students.mapIndexed { index, student ->
                student.copy(isPresent = (index + roomIndex) % 3 != 0)
            }
        )
    }
    val demoReport = CheckReport(
        generatedAt = "2026-10-28 22:15",
        rooms = demoRooms,
        uncheckedRoomNumbers = MockDormData.rooms.drop(3).map { it.roomNumber }
    )
    DormCheckTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFFE9E4FB))
        ) {
            ReportScreen(
                state = DormUiState(report = demoReport, selectedTab = DormUiState.TAB_REPORT),
                onGenerate = {}
            )
        }
    }
}
