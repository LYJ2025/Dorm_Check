package com.example.dormcheck.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 内置名单的加载入口。
 *
 * 设计要点：
 * - 真实名单放在 `app/src/main/assets/roster.json`，**不写进代码**；
 * - 该文件默认被 .gitignore 排除，公开仓库里只有 `roster.example.json`；
 * - 文件缺失 / 解析失败时返回空列表，App 仍可正常启动，用户自己新增宿舍与学生。
 *
 * 因为每次启动都从 assets 读取，内置数据是"只读种子"：
 * 用户在 App 里做的任何修改都写进 DataStore，不会回写、也不会覆盖这份 JSON。
 */
object BuiltInRosterLoader {

    const val ASSET_NAME = "roster.json"

    data class Result(
        val title: String? = null,
        val rooms: List<DormRoom> = emptyList()
    )

    fun load(context: Context): Result {
        val raw = runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return Result()

        return runCatching { parse(raw) }.getOrDefault(Result())
    }

    private fun parse(raw: String): Result {
        val root = JSONObject(raw)
        val title = root.optString("title").takeIf { it.isNotBlank() }
        val roomsArray = root.optJSONArray("rooms") ?: JSONArray()

        val rooms = buildList {
            for (i in 0 until roomsArray.length()) {
                val roomJson = roomsArray.optJSONObject(i) ?: continue
                val roomNumber = roomJson.optString("roomNumber").trim()
                if (roomNumber.isEmpty()) continue

                val studentsJson = roomJson.optJSONArray("students") ?: JSONArray()
                val students = buildList {
                    for (j in 0 until studentsJson.length()) {
                        val studentJson = studentsJson.optJSONObject(j) ?: continue
                        val name = studentJson.optString("name").trim()
                        if (name.isEmpty()) continue
                        val bed = studentJson.optInt("bed", j + 1)
                        add(
                            Student(
                                id = "builtin_${roomNumber}_$bed",
                                bedNumber = bed,
                                name = name,
                                isUserAdded = false
                            )
                        )
                    }
                }

                add(
                    DormRoom(
                        id = "builtin_$roomNumber",
                        roomNumber = roomNumber,
                        students = students,
                        isUserAdded = false
                    )
                )
            }
        }
        return Result(title = title, rooms = rooms)
    }
}
