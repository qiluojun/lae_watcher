package com.example.lae_watcher.data

import org.json.JSONObject
import java.util.UUID

/**
 * Type B - 行为监控提醒数据类
 * @param id 唯一标识符 (UUID)
 * @param name 配置名称 (如 "夜间监控", "午休监控")
 * @param timeRange 监控时段
 * @param thresholdSeconds 亮屏时长阈值 (秒, 范围: 10-600)
 * @param enabled 是否启用
 * @param message 自定义提示语 (最多 50 字)
 */
data class BehaviorAlarmItem(
    val id: String,
    val name: String,
    val timeRange: TimeRange,
    val thresholdSeconds: Int,
    val enabled: Boolean,
    val message: String
) {
    /**
     * 判断当前时间是否在监控时段内
     */
    fun isTimeInRange(hour: Int, minute: Int): Boolean {
        return timeRange.isTimeInRange(hour, minute)
    }

    /**
     * 格式化为显示字符串 (如 "夜间监控 (21:30-08:00, 30秒)")
     */
    override fun toString(): String {
        return "$name (${timeRange}, ${thresholdSeconds}秒)"
    }

    /**
     * 转换为 Map (用于 MethodChannel 传递给 Flutter)
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "timeRange" to mapOf(
                "startHour" to timeRange.startHour,
                "startMinute" to timeRange.startMinute,
                "endHour" to timeRange.endHour,
                "endMinute" to timeRange.endMinute
            ),
            "thresholdSeconds" to thresholdSeconds,
            "enabled" to enabled,
            "message" to message
        )
    }

    /**
     * 转换为 JSON 对象 (用于 SharedPreferences 存储)
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("timeRange", timeRange.toJson())
            put("thresholdSeconds", thresholdSeconds)
            put("enabled", enabled)
            put("message", message)
        }
    }

    companion object {
        /**
         * 创建新的行为监控项 (自动生成 UUID)
         */
        fun create(
            name: String,
            timeRange: TimeRange,
            thresholdSeconds: Int,
            message: String,
            enabled: Boolean = true
        ): BehaviorAlarmItem {
            return BehaviorAlarmItem(
                id = UUID.randomUUID().toString(),
                name = name.take(20),  // 限制名称长度
                timeRange = timeRange,
                thresholdSeconds = thresholdSeconds.coerceIn(10, 600),  // 限制范围 10-600 秒
                enabled = enabled,
                message = message.take(50)  // 限制提示语长度
            )
        }

        /**
         * 从 JSON 对象创建 BehaviorAlarmItem
         */
        fun fromJson(json: JSONObject): BehaviorAlarmItem {
            return BehaviorAlarmItem(
                id = json.getString("id"),
                name = json.getString("name"),
                timeRange = TimeRange.fromJson(json.getJSONObject("timeRange")),
                thresholdSeconds = json.getInt("thresholdSeconds"),
                enabled = json.getBoolean("enabled"),
                message = json.getString("message")
            )
        }

        /**
         * 从 Map 创建 BehaviorAlarmItem (用于 Flutter 传递数据)
         */
        fun fromMap(map: Map<String, Any>): BehaviorAlarmItem {
            @Suppress("UNCHECKED_CAST")
            val timeRangeMap = map["timeRange"] as? Map<String, Any> ?: mapOf()
            val timeRange = TimeRange(
                startHour = timeRangeMap["startHour"] as? Int ?: 21,
                startMinute = timeRangeMap["startMinute"] as? Int ?: 30,
                endHour = timeRangeMap["endHour"] as? Int ?: 8,
                endMinute = timeRangeMap["endMinute"] as? Int ?: 0
            )

            return BehaviorAlarmItem(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                name = (map["name"] as? String ?: "监控配置").take(20),
                timeRange = timeRange,
                thresholdSeconds = (map["thresholdSeconds"] as? Int ?: 30).coerceIn(10, 600),
                enabled = map["enabled"] as? Boolean ?: true,
                message = (map["message"] as? String ?: "亮屏时间过长").take(50)
            )
        }
    }
}
