package com.example.dormcheck.data

/**
 * 本地模拟数据：男生宿舍 101~104、201~204，共 8 间 44 人。
 * 真实项目中这里替换为后端接口或 Room 查询结果即可，UI 与 ViewModel 无需改动。
 */
object MockDormData {

    private fun room(roomNumber: String, names: List<String>): DormRoom = DormRoom(
        id = "room_$roomNumber",
        roomNumber = roomNumber,
        students = names.mapIndexed { index, name ->
            Student(
                id = "student_${roomNumber}_${index + 1}",
                bedNumber = index + 1,
                name = name
            )
        }
    )

    val rooms: List<DormRoom> = listOf(
        room("101", listOf("张伟", "李浩然", "王梓涵", "刘一鸣", "陈嘉豪", "杨子轩")),
        room("102", listOf("赵天成", "黄俊杰", "周子墨", "吴宇航", "徐博文")),
        room("103", listOf("孙志强", "马晓东", "朱家乐", "胡明轩", "郭子豪", "何俊熙")),
        room("104", listOf("高文博", "林嘉铭", "罗世杰", "郑浩宇")),
        room("201", listOf("梁思远", "谢承志", "唐子航", "韩泽宇", "曹雨晨", "邓皓文")),
        room("202", listOf("冯亦凡", "曾令辉", "彭子健", "蔡明宇", "潘博涵", "袁一诺")),
        room("203", listOf("于海涛", "董思睿", "余子豪", "苏梓航", "卢嘉宇")),
        room("204", listOf("蒋沐辰", "傅嘉诚", "沈泽楷", "钟启明", "姜文轩", "崔宇泽"))
    )
}
