package com.example.dormcheck.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.dormDataStore by preferencesDataStore(name = "dorm_local")

/**
 * 本地持久化。存两类数据：
 * 1. 名单（首次启动时把 assets 里的内置名单"导入"到这里，之后**完全可增删改**）；
 * 2. 点名结果（已到学生、已点名宿舍）。
 *
 * 内置名单文件始终保持只读种子：修改只发生在本地副本上，因此
 * 不会污染 assets，也不会因为重新读取而覆盖用户的改动。
 */
data class SavedState(
    val seeded: Boolean = false,
    val title: String? = null,
    val rooms: List<DormRoom> = emptyList(),
    val presentKeys: Set<String> = emptySet(),
    val checkedRoomIds: Set<String> = emptySet()
)

class LocalStore(private val context: Context) {

    private object Keys {
        val SEEDED = booleanPreferencesKey("roster_seeded")
        val ROSTER = stringPreferencesKey("roster_json")
        val PRESENT = stringSetPreferencesKey("present_student_keys")
        val CHECKED_ROOMS = stringSetPreferencesKey("checked_room_ids")
    }

    private val flow: Flow<SavedState> = context.dormDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            SavedState(
                seeded = prefs[Keys.SEEDED] ?: false,
                title = prefs[Keys.ROSTER]?.let { decodeTitle(it) },
                rooms = decodeRooms(prefs[Keys.ROSTER]),
                presentKeys = prefs[Keys.PRESENT] ?: emptySet(),
                checkedRoomIds = prefs[Keys.CHECKED_ROOMS] ?: emptySet()
            )
        }

    suspend fun load(): SavedState = flow.first()

    suspend fun saveRoster(title: String?, rooms: List<DormRoom>) {
        context.dormDataStore.edit { prefs ->
            prefs[Keys.ROSTER] = encodeRoster(title, rooms)
            prefs[Keys.SEEDED] = true
        }
    }

    suspend fun saveAttendance(presentKeys: Set<String>, checkedRoomIds: Set<String>) {
        context.dormDataStore.edit { prefs ->
            prefs[Keys.PRESENT] = presentKeys
            prefs[Keys.CHECKED_ROOMS] = checkedRoomIds
        }
    }

    suspend fun clearAttendance() {
        context.dormDataStore.edit { prefs ->
            prefs.remove(Keys.PRESENT)
            prefs.remove(Keys.CHECKED_ROOMS)
        }
    }

    companion object {
        fun studentKey(roomId: String, studentId: String): String = "$roomId|$studentId"

        private fun encodeRoster(title: String?, rooms: List<DormRoom>): String {
            val root = JSONObject()
            if (title != null) root.put("title", title)
            root.put("seeded", true)

            val roomsJson = JSONArray()
            rooms.forEach { room ->
                val studentsJson = JSONArray()
                room.students.forEach { student ->
                    studentsJson.put(
                        JSONObject()
                            .put("id", student.id)
                            .put("bed", student.bedNumber)
                            .put("name", student.name)
                            .put("userAdded", student.isUserAdded)
                    )
                }
                roomsJson.put(
                    JSONObject()
                        .put("id", room.id)
                        .put("roomNumber", room.roomNumber)
                        .put("userAdded", room.isUserAdded)
                        .put("students", studentsJson)
                )
            }
            root.put("rooms", roomsJson)
            return root.toString()
        }

        private fun decodeTitle(raw: String): String? = runCatching {
            JSONObject(raw).optString("title").takeIf { it.isNotBlank() }
        }.getOrNull()

        private fun decodeRooms(raw: String?): List<DormRoom> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val roomsJson = JSONObject(raw).optJSONArray("rooms") ?: return emptyList()
                buildList {
                    for (i in 0 until roomsJson.length()) {
                        val item = roomsJson.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        val roomNumber = item.optString("roomNumber")
                        if (id.isEmpty() || roomNumber.isEmpty()) continue

                        val studentsJson = item.optJSONArray("students") ?: JSONArray()
                        val students = buildList {
                            for (j in 0 until studentsJson.length()) {
                                val s = studentsJson.optJSONObject(j) ?: continue
                                val sid = s.optString("id")
                                val name = s.optString("name").trim()
                                if (sid.isEmpty() || name.isEmpty()) continue
                                add(
                                    Student(
                                        id = sid,
                                        bedNumber = s.optInt("bed", j + 1),
                                        name = name,
                                        isUserAdded = s.optBoolean("userAdded", false)
                                    )
                                )
                            }
                        }

                        add(
                            DormRoom(
                                id = id,
                                roomNumber = roomNumber,
                                students = students,
                                isUserAdded = item.optBoolean("userAdded", false)
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
