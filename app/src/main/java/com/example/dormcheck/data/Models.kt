package com.example.dormcheck.data

/**
 * 学生：床号 + 姓名 + 点名状态（默认未到）
 * @param isUserAdded true 表示这条是用户在 App 内自行添加的，可删除；内置名单为 false。
 */
data class Student(
    val id: String,
    val bedNumber: Int,
    val name: String,
    val isPresent: Boolean = false,
    val isUserAdded: Boolean = false
)

/**
 * 宿舍：门号 + 学生列表 + 是否已完成点名
 * @param isUserAdded true 表示用户在 App 内自行新增的宿舍。
 */
data class DormRoom(
    val id: String,
    val roomNumber: String,
    val students: List<Student>,
    val isChecked: Boolean = false,
    val isUserAdded: Boolean = false
) {
    val totalCount: Int get() = students.size
    val presentCount: Int get() = students.count { it.isPresent }
    val absentCount: Int get() = totalCount - presentCount
    val isAllPresent: Boolean get() = totalCount > 0 && presentCount == totalCount
}

/**
 * 报告中的单个宿舍条目（生成报告时对数据做快照，之后点名变化不影响已生成的报告）
 */
data class ReportRoom(
    val roomNumber: String,
    val students: List<Student>
) {
    val presentCount: Int get() = students.count { it.isPresent }
    val totalCount: Int get() = students.size
}

/**
 * 查寝报告
 */
data class CheckReport(
    val generatedAt: String,
    val rooms: List<ReportRoom>,
    val uncheckedRoomNumbers: List<String>
) {
    val checkedRoomCount: Int get() = rooms.size
    val totalStudents: Int get() = rooms.sumOf { it.totalCount }
    val presentStudents: Int get() = rooms.sumOf { it.presentCount }
    val absentStudents: Int get() = totalStudents - presentStudents

    /** 不在寝学生清单：门号 + 床号 + 姓名 */
    val absentList: List<Triple<String, Int, String>>
        get() = rooms.flatMap { room ->
            room.students.filterNot { it.isPresent }
                .map { Triple(room.roomNumber, it.bedNumber, it.name) }
        }
}
