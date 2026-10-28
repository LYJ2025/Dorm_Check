package com.example.dormcheck.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dormcheck.data.BuiltInRosterLoader
import com.example.dormcheck.data.CheckReport
import com.example.dormcheck.data.DormRoom
import com.example.dormcheck.data.LocalStore
import com.example.dormcheck.data.ReportRoom
import com.example.dormcheck.data.SavedState
import com.example.dormcheck.data.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 单 Activity 架构下的唯一 UI 状态。
 * 放在 ViewModel 中，屏幕旋转、Tab 切换都不会丢失。
 */
data class DormUiState(
    val rooms: List<DormRoom> = emptyList(),
    val selectedRoomId: String? = null,
    val selectedTab: Int = TAB_CHECK_IN,
    val report: CheckReport? = null,
    val rosterTitle: String? = null,
    val loaded: Boolean = false
) {
    val selectedRoom: DormRoom?
        get() = rooms.firstOrNull { it.id == selectedRoomId } ?: rooms.firstOrNull()

    val checkedRoomCount: Int get() = rooms.count { it.isChecked }
    val allRoomsChecked: Boolean get() = rooms.isNotEmpty() && checkedRoomCount == rooms.size
    val totalStudents: Int get() = rooms.sumOf { it.students.size }
    val presentStudents: Int get() = rooms.sumOf { it.presentCount }
    val absentInCheckedRooms: Int get() = rooms.filter { it.isChecked }.sumOf { it.absentCount }
    val checkedRoomPercent: Float
        get() = if (rooms.isEmpty()) 0f else checkedRoomCount.toFloat() / rooms.size
    val builtInRoomCount: Int get() = rooms.count { !it.isUserAdded }

    companion object {
        const val TAB_CHECK_IN = 0
        const val TAB_REPORT = 1
    }
}

class DormViewModel(application: Application) : AndroidViewModel(application) {

    /** 「下一间宿舍」的结果，用于给 UI 决定要不要弹轻提示 */
    sealed interface NextRoomResult {
        data class Moved(val fromRoom: String, val toRoom: String, val missingCount: Int) : NextRoomResult
        data object AllRoomsChecked : NextRoomResult
    }

    /** 名单增删改的结果（就地显示在弹窗里） */
    sealed interface RosterResult {
        data class Ok(val message: String) : RosterResult
        data class Failed(val message: String) : RosterResult
    }

    /** 「全部到齐」的结果：只针对一间宿舍 */
    data class RoomMarkResult(
        val roomNumber: String,
        val total: Int,
        val newlyMarked: Int
    )

    private val store = LocalStore(application)
    private val saveMutex = Mutex()

    /** 启动后用户已手动改过数据，就不再让磁盘数据覆盖界面 */
    private var hasLocalEdits = false

    /** 名单本体（不含点名状态）：内置名单首次启动后会被复制到这里，之后与自建数据一视同仁 */
    private var rosterRooms: List<DormRoom> = emptyList()
    private var rosterTitle: String? = null

    /** 点名结果：只存 key，展示状态由 rebuild() 派生 */
    private var presentKeys: Set<String> = emptySet()
    private var checkedRoomIds: Set<String> = emptySet()

    var uiState by mutableStateOf(DormUiState())
        private set

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { store.load() }.getOrDefault(SavedState())
            }
            val builtIn = withContext(Dispatchers.IO) { BuiltInRosterLoader.load(getApplication()) }
            if (hasLocalEdits) return@launch

            if (!saved.seeded) {
                // 首次启动：把内置名单导入本地副本。之后它就是普通数据，可随意增删改。
                rosterRooms = builtIn.rooms
                rosterTitle = builtIn.title
                withContext(Dispatchers.IO) {
                    runCatching { store.saveRoster(rosterTitle, rosterRooms) }
                }
            } else {
                rosterRooms = saved.rooms
                rosterTitle = saved.title
                presentKeys = saved.presentKeys
                checkedRoomIds = saved.checkedRoomIds
            }

            rebuild()
            uiState = uiState.copy(loaded = true)
        }
    }

    /** 由名单 + 点名数据派生出要展示的房间列表（单一数据源，避免两份状态互相覆盖） */
    private fun rebuild() {
        val rooms = rosterRooms.map { room ->
            room.copy(
                isChecked = room.id in checkedRoomIds,
                students = room.students
                    .sortedBy { it.bedNumber }
                    .map { student ->
                        student.copy(
                            isPresent = LocalStore.studentKey(room.id, student.id) in presentKeys
                        )
                    }
            )
        }
        val currentId = uiState.selectedRoomId
        uiState = uiState.copy(
            rooms = rooms,
            rosterTitle = rosterTitle,
            selectedRoomId = rooms.firstOrNull { it.id == currentId }?.id ?: rooms.firstOrNull()?.id
        )
    }

    /* ---------------- Tab / 宿舍选择 ---------------- */

    fun selectTab(index: Int) {
        if (index != DormUiState.TAB_CHECK_IN && index != DormUiState.TAB_REPORT) return
        uiState = uiState.copy(selectedTab = index)
    }

    fun selectRoom(roomId: String) {
        if (uiState.rooms.none { it.id == roomId }) return
        uiState = uiState.copy(selectedRoomId = roomId)
    }

    /* ---------------- 点名 ---------------- */

    fun toggleStudent(studentId: String) {
        val roomId = uiState.selectedRoomId ?: return
        hasLocalEdits = true

        val key = LocalStore.studentKey(roomId, studentId)
        presentKeys = if (key in presentKeys) presentKeys - key else presentKeys + key
        checkedRoomIds = checkedRoomIds + roomId
        rebuild()
        persistAttendance()
    }

    /**
     * 「全部到齐」：只把**这一间**宿舍整间标记为已到，其他宿舍一概不动。
     * 同时把这间宿舍设为当前选中，方便顺手核对谁到了；点错了仍可逐个再点回「未到」。
     */
    fun markRoomAllPresent(roomId: String): RoomMarkResult? {
        val room = uiState.rooms.firstOrNull { it.id == roomId } ?: return null
        if (room.students.isEmpty()) return null

        hasLocalEdits = true
        val keys = room.students.map { LocalStore.studentKey(roomId, it.id) }.toSet()
        val newlyMarked = keys.count { it !in presentKeys }

        presentKeys = presentKeys + keys
        checkedRoomIds = checkedRoomIds + roomId
        uiState = uiState.copy(selectedRoomId = roomId)
        rebuild()
        persistAttendance()

        return RoomMarkResult(
            roomNumber = room.roomNumber,
            total = room.totalCount,
            newlyMarked = newlyMarked
        )
    }

    fun goToNextRoom(): NextRoomResult {
        val currentId = uiState.selectedRoomId ?: return NextRoomResult.AllRoomsChecked
        val currentRoom = uiState.rooms.firstOrNull { it.id == currentId }
            ?: return NextRoomResult.AllRoomsChecked
        val missingCount = currentRoom.absentCount

        hasLocalEdits = true
        checkedRoomIds = checkedRoomIds + currentId
        rebuild()

        val rooms = uiState.rooms
        val currentIndex = rooms.indexOfFirst { it.id == currentId }
        val nextRoom = (1 until rooms.size)
            .map { offset -> rooms[(currentIndex + offset) % rooms.size] }
            .firstOrNull { !it.isChecked }

        uiState = uiState.copy(selectedRoomId = nextRoom?.id ?: currentId)
        persistAttendance()

        return if (nextRoom == null) {
            NextRoomResult.AllRoomsChecked
        } else {
            NextRoomResult.Moved(
                fromRoom = currentRoom.roomNumber,
                toRoom = nextRoom.roomNumber,
                missingCount = missingCount
            )
        }
    }

    /* ---------------- 名单：增 ---------------- */

    fun addRoom(rawNumber: String): RosterResult {
        val roomNumber = rawNumber.trim()
        if (roomNumber.isEmpty()) return RosterResult.Failed("请输入宿舍门号")
        if (rosterRooms.any { it.roomNumber.equals(roomNumber, ignoreCase = true) }) {
            return RosterResult.Failed("门号 $roomNumber 已存在")
        }

        hasLocalEdits = true
        val room = DormRoom(
            id = "room_${UUID.randomUUID().toString().take(8)}",
            roomNumber = roomNumber,
            students = emptyList(),
            isUserAdded = true
        )
        rosterRooms = rosterRooms + room
        persistRoster()
        rebuild()
        uiState = uiState.copy(selectedRoomId = room.id)
        return RosterResult.Ok("已添加 $roomNumber")
    }

    fun addStudent(roomId: String, rawName: String, rawBed: String): RosterResult {
        val room = rosterRooms.firstOrNull { it.id == roomId }
            ?: return RosterResult.Failed("宿舍不存在，可能已被删除")
        val name = rawName.trim()
        if (name.isEmpty()) return RosterResult.Failed("请输入学生姓名")

        val bed = rawBed.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
            ?: (room.students.maxOfOrNull { it.bedNumber } ?: 0) + 1
        if (bed <= 0) return RosterResult.Failed("床号需为正整数")
        if (room.students.any { it.bedNumber == bed }) {
            return RosterResult.Failed("${bed} 号床已经有人了")
        }

        hasLocalEdits = true
        val student = Student(
            id = "student_${UUID.randomUUID().toString().take(8)}",
            bedNumber = bed,
            name = name,
            isUserAdded = true
        )
        rosterRooms = rosterRooms.map { if (it.id == roomId) it.copy(students = it.students + student) else it }
        persistRoster()
        rebuild()
        return RosterResult.Ok("已添加 $name（${bed} 号床）")
    }

    /* ---------------- 名单：改 ---------------- */

    fun updateRoom(roomId: String, rawNumber: String): RosterResult {
        val roomNumber = rawNumber.trim()
        if (roomNumber.isEmpty()) return RosterResult.Failed("请输入宿舍门号")
        if (rosterRooms.any { it.id != roomId && it.roomNumber.equals(roomNumber, ignoreCase = true) }) {
            return RosterResult.Failed("门号 $roomNumber 已存在")
        }

        hasLocalEdits = true
        rosterRooms = rosterRooms.map { if (it.id == roomId) it.copy(roomNumber = roomNumber) else it }
        persistRoster()
        rebuild()
        return RosterResult.Ok("门号已改为 $roomNumber")
    }

    fun updateStudent(
        roomId: String,
        studentId: String,
        rawName: String,
        rawBed: String
    ): RosterResult {
        val room = rosterRooms.firstOrNull { it.id == roomId }
            ?: return RosterResult.Failed("宿舍不存在")
        val student = room.students.firstOrNull { it.id == studentId }
            ?: return RosterResult.Failed("学生不存在")

        val name = rawName.trim()
        if (name.isEmpty()) return RosterResult.Failed("请输入学生姓名")

        val bed = rawBed.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
            ?: return RosterResult.Failed("床号请填数字")
        if (bed <= 0) return RosterResult.Failed("床号需为正整数")
        if (room.students.any { it.id != student.id && it.bedNumber == bed }) {
            return RosterResult.Failed("${bed} 号床已经有人了")
        }

        hasLocalEdits = true
        rosterRooms = rosterRooms.map { currentRoom ->
            if (currentRoom.id != roomId) {
                currentRoom
            } else {
                currentRoom.copy(
                    students = currentRoom.students.map { current ->
                        if (current.id == studentId) {
                            current.copy(name = name, bedNumber = bed)
                        } else {
                            current
                        }
                    }
                )
            }
        }
        persistRoster()
        rebuild()
        return RosterResult.Ok("已更新 $name")
    }

    /* ---------------- 名单：删 ---------------- */

    fun deleteRoom(roomId: String) {
        if (rosterRooms.none { it.id == roomId }) return
        hasLocalEdits = true
        rosterRooms = rosterRooms.filterNot { it.id == roomId }
        checkedRoomIds = checkedRoomIds - roomId
        presentKeys = presentKeys.filterNot { it.startsWith("$roomId|") }.toSet()
        persistRoster()
        persistAttendance()
        rebuild()
    }

    fun deleteStudent(roomId: String, studentId: String) {
        if (rosterRooms.none { room -> room.id == roomId && room.students.any { it.id == studentId } }) return
        hasLocalEdits = true
        rosterRooms = rosterRooms.map { room ->
            if (room.id == roomId) {
                room.copy(students = room.students.filterNot { it.id == studentId })
            } else {
                room
            }
        }
        presentKeys = presentKeys - LocalStore.studentKey(roomId, studentId)
        persistRoster()
        persistAttendance()
        rebuild()
    }

    /** 重新合并 assets 里的内置名单：只补缺失的宿舍/学生，不动已有修改。返回新增条目数 */
    fun restoreBuiltIn(): Int {
        val seeded = BuiltInRosterLoader.load(getApplication()).rooms
        if (seeded.isEmpty()) return 0

        var result = rosterRooms
        var added = 0

        seeded.forEach { seedRoom ->
            val existing = result.firstOrNull { it.roomNumber.equals(seedRoom.roomNumber, ignoreCase = true) }
            if (existing == null) {
                result = result + seedRoom
                added += 1 + seedRoom.students.size
            } else {
                val missing = seedRoom.students.filter { seedStudent ->
                    existing.students.none { it.name == seedStudent.name }
                }
                if (missing.isNotEmpty()) {
                    var nextBed = existing.students.maxOfOrNull { it.bedNumber } ?: 0
                    val appended = missing.map { seedStudent ->
                        nextBed += 1
                        seedStudent.copy(id = "seed_${existing.id}_$nextBed", bedNumber = nextBed)
                    }
                    result = result.map { room ->
                        if (room.id == existing.id) room.copy(students = room.students + appended) else room
                    }
                    added += appended.size
                }
            }
        }

        if (added > 0) {
            hasLocalEdits = true
            rosterRooms = result
            persistRoster()
            rebuild()
        }
        return added
    }

    /* ---------------- 报告 / 重置 ---------------- */

    fun generateReport() {
        val rooms = uiState.rooms
        val report = CheckReport(
            generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            rooms = rooms.filter { it.isChecked }
                .map { room -> ReportRoom(roomNumber = room.roomNumber, students = room.students) },
            uncheckedRoomNumbers = rooms.filterNot { it.isChecked }.map { it.roomNumber }
        )
        uiState = uiState.copy(report = report)
    }

    /** 只清点名结果，名单保留 */
    fun resetAll() {
        hasLocalEdits = true
        presentKeys = emptySet()
        checkedRoomIds = emptySet()
        rebuild()
        uiState = uiState.copy(report = null)
        viewModelScope.launch { runCatching { store.clearAttendance() } }
    }

    /* ---------------- 持久化 ---------------- */

    private fun persistAttendance() {
        val present = presentKeys
        val checked = checkedRoomIds
        viewModelScope.launch {
            saveMutex.withLock {
                runCatching { store.saveAttendance(present, checked) }
            }
        }
    }

    private fun persistRoster() {
        val rooms = rosterRooms
        val title = rosterTitle
        viewModelScope.launch {
            saveMutex.withLock {
                runCatching { store.saveRoster(title, rooms) }
            }
        }
    }

    /** 供根组件把 VM 的能力打包给 UI */
    fun rosterActions(): RosterActions = RosterActions(
        addRoom = ::addRoom,
        updateRoom = ::updateRoom,
        deleteRoom = ::deleteRoom,
        addStudent = ::addStudent,
        updateStudent = ::updateStudent,
        deleteStudent = ::deleteStudent,
        restoreBuiltIn = ::restoreBuiltIn
    )
}
