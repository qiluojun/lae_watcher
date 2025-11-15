package com.example.lae_watcher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Type A - 定时提醒数据类
 * @param id 唯一标识符 (UUID)
 * @param hour 小时 (0-23)
 * @param minute 分钟 (0-59)
 * @param enabled 是否启用
 * @param message 自定义提示语 (最多 50 字)
 * @param repeatDays 周重复 (可选, 1=周一, 7=周日, null=每日重复)
 */
data class TimeAlarmItem(
    val id: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val message: String,
    val repeatDays: List<Int>? = null  // V1 保留字段，暂不使用
) {
    /**
     * 格式化为显示字符串 (如 "09:00 - 早安提醒")
     */
    override fun toString(): String {
        return String.format("%02d:%02d - %s", hour, minute, message)
    }

    /**
     * 转换为 Map (用于 MethodChannel 传递给 Flutter)
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "hour" to hour,
            "minute" to minute,
            "enabled" to enabled,
            "message" to message,
            "repeatDays" to repeatDays
        )
    }

    /**
     * 转换为 JSON 对象 (用于 SharedPreferences 存储)
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("hour", hour)
            put("minute", minute)
            put("enabled", enabled)
            put("message", message)
            if (repeatDays != null) {
                val repeatArray = JSONArray()
                repeatDays.forEach { repeatArray.put(it) }
                put("repeatDays", repeatArray)
            } else {
                put("repeatDays", JSONObject.NULL)
            }
        }
    }

    companion object {
        /**
         * 创建新的提醒项 (自动生成 UUID)
         */
        fun create(
            hour: Int,
            minute: Int,
            message: String,
            enabled: Boolean = true,
            repeatDays: List<Int>? = null
        ): TimeAlarmItem {
            return TimeAlarmItem(
                id = UUID.randomUUID().toString(),
                hour = hour,
                minute = minute,
                enabled = enabled,
                message = message.take(50),  // 限制最多 50 字
                repeatDays = repeatDays
            )
        }

        /**
         * 从 JSON 对象创建 TimeAlarmItem
         */
        fun fromJson(json: JSONObject): TimeAlarmItem {
            val repeatDays = if (json.isNull("repeatDays")) {
                null
            } else {
                val repeatArray = json.getJSONArray("repeatDays")
                val list = mutableListOf<Int>()
                for (i in 0 until repeatArray.length()) {
                    list.add(repeatArray.getInt(i))
                }
                list
            }

            return TimeAlarmItem(
                id = json.getString("id"),
                hour = json.getInt("hour"),
                minute = json.getInt("minute"),
                enabled = json.getBoolean("enabled"),
                message = json.getString("message"),
                repeatDays = repeatDays
            )
        }

        /**
         * 从 Map 创建 TimeAlarmItem (用于 Flutter 传递数据)
         */
        fun fromMap(map: Map<String, Any?>): TimeAlarmItem {
            @Suppress("UNCHECKED_CAST")
            val repeatDays = map["repeatDays"] as? List<Int>

            val rawMessage = (map["message"] as? String)?.trim() ?: ""
            val safeMessage = if (rawMessage.isEmpty()) "提醒" else rawMessage

            return TimeAlarmItem(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                hour = map["hour"] as? Int ?: 0,
                minute = map["minute"] as? Int ?: 0,
                enabled = map["enabled"] as? Boolean ?: true,
                message = safeMessage.take(50),
                repeatDays = repeatDays
            )
        }
    }
}
