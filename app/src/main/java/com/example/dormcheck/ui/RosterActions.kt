package com.example.dormcheck.ui

/**
 * 名单相关的用户操作集合。
 * 内置名单与用户自建名单在这里走同一套接口——两者都能改、能删、能加，
 * 区别只体现在 [com.example.dormcheck.data.DormRoom.isUserAdded] 标签上。
 */
data class RosterActions(
    val addRoom: (roomNumber: String) -> DormViewModel.RosterResult = {
        DormViewModel.RosterResult.Failed("不可用")
    },
    val updateRoom: (roomId: String, roomNumber: String) -> DormViewModel.RosterResult = { _, _ ->
        DormViewModel.RosterResult.Failed("不可用")
    },
    val deleteRoom: (roomId: String) -> Unit = {},
    val addStudent: (roomId: String, name: String, bed: String) -> DormViewModel.RosterResult = { _, _, _ ->
        DormViewModel.RosterResult.Failed("不可用")
    },
    val updateStudent: (roomId: String, studentId: String, name: String, bed: String) -> DormViewModel.RosterResult =
        { _, _, _, _ -> DormViewModel.RosterResult.Failed("不可用") },
    val deleteStudent: (roomId: String, studentId: String) -> Unit = { _, _ -> },
    /** 把 assets 里的内置名单重新合并进来（只补缺失项，不覆盖已有修改），返回新增条目数 */
    val restoreBuiltIn: () -> Int = { 0 }
)
