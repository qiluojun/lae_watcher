package com.example.lae_watcher.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 时段范围数据类
 * @param startHour 开始小时 (0-23)
 * @param startMinute 开始分钟 (0-59)
 * @param endHour 结束小时 (0-23)
 * @param endMinute 结束分钟 (0-59)
 */
data class TimeRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    /**
     * 判断给定时间是否在当前时段内
     * 支持跨午夜时段 (如 21:30 - 08:00)
     */
    fun isTimeInRange(hour: Int, minute: Int): Boolean {
        val currentMinutes = hour * 60 + minute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes < endMinutes) {
            // 不跨午夜: 如 08:00 - 21:30
            currentMinutes in startMinutes..endMinutes
        } else {
            // 跨午夜: 如 21:30 - 08:00
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    /**
     * 格式化为字符串 (如 "21:30 - 08:00")
     */
    override fun toString(): String {
        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
    }

    /**
     * 转换为 JSON 对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("startHour", startHour)
            put("startMinute", startMinute)
            put("endHour", endHour)
            put("endMinute", endMinute)
        }
    }

    companion object {
        /**
         * 从 JSON 对象创建 TimeRange
         */
        fun fromJson(json: JSONObject): TimeRange {
            return TimeRange(
                startHour = json.getInt("startHour"),
                startMinute = json.getInt("startMinute"),
                endHour = json.getInt("endHour"),
                endMinute = json.getInt("endMinute")
            )
        }
    }
}

/**
 * 行为监控提醒配置
 * @param enabled 是否启用行为监控
 * @param timeRanges 监控时段列表 (支持多时段)
 * @param screenTimeThreshold 亮屏时长阈值 (秒, 范围: 10-600)
 * @param message 自定义提示语 (最多 50 字)
 */
data class BehaviorAlarmConfig(
    val enabled: Boolean = false,
    val timeRanges: List<TimeRange> = listOf(TimeRange(21, 30, 8, 0)), // 默认 21:30 - 08:00
    val screenTimeThreshold: Int = 30, // 默认 30 秒
    val message: String = "亮屏时间过长！" // 默认提示语
) {
    /**
     * 判断当前时间是否在任一监控时段内
     */
    fun isCurrentTimeInAnyRange(hour: Int, minute: Int): Boolean {
        return timeRanges.any { it.isTimeInRange(hour, minute) }
    }

    /**
     * 转换为 Map (用于 MethodChannel 传递给 Flutter)
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "enabled" to enabled,
            "timeRanges" to timeRanges.map { mapOf(
                "startHour" to it.startHour,
                "startMinute" to it.startMinute,
                "endHour" to it.endHour,
                "endMinute" to it.endMinute
            )},
            "screenTimeThreshold" to screenTimeThreshold,
            "message" to message
        )
    }

    /**
     * 转换为 JSON 字符串 (用于 SharedPreferences 存储)
     */
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("enabled", enabled)
        json.put("screenTimeThreshold", screenTimeThreshold)
        json.put("message", message)

        val rangesArray = JSONArray()
        timeRanges.forEach { rangesArray.put(it.toJson()) }
        json.put("timeRanges", rangesArray)

        return json.toString()
    }

    companion object {
        /**
         * 默认配置: 禁用, 21:30-08:00, 30秒阈值, 默认提示语
         */
        fun default(): BehaviorAlarmConfig {
            return BehaviorAlarmConfig(
                enabled = false,
                timeRanges = listOf(TimeRange(21, 30, 8, 0)),
                screenTimeThreshold = 30,
                message = "亮屏时间过长！"
            )
        }

        /**
         * 从 JSON 字符串创建配置
         */
        fun fromJsonString(jsonString: String): BehaviorAlarmConfig {
            return try {
                val json = JSONObject(jsonString)
                val enabled = json.getBoolean("enabled")
                val threshold = json.getInt("screenTimeThreshold")
                val message = json.optString("message", "亮屏时间过长！") // 兼容旧数据

                val rangesArray = json.getJSONArray("timeRanges")
                val timeRanges = mutableListOf<TimeRange>()
                for (i in 0 until rangesArray.length()) {
                    timeRanges.add(TimeRange.fromJson(rangesArray.getJSONObject(i)))
                }

                BehaviorAlarmConfig(enabled, timeRanges, threshold, message)
            } catch (e: Exception) {
                // 解析失败返回默认配置
                default()
            }
        }

        /**
         * 从 Map 创建配置 (用于 Flutter 传递数据)
         */
        fun fromMap(map: Map<String, Any>): BehaviorAlarmConfig {
            val enabled = map["enabled"] as? Boolean ?: false
            val threshold = map["screenTimeThreshold"] as? Int ?: 30
            val message = map["message"] as? String ?: "亮屏时间过长！"

            @Suppress("UNCHECKED_CAST")
            val rangesList = map["timeRanges"] as? List<Map<String, Any>> ?: emptyList()
            val timeRanges = rangesList.map { rangeMap ->
                TimeRange(
                    startHour = rangeMap["startHour"] as? Int ?: 0,
                    startMinute = rangeMap["startMinute"] as? Int ?: 0,
                    endHour = rangeMap["endHour"] as? Int ?: 0,
                    endMinute = rangeMap["endMinute"] as? Int ?: 0
                )
            }

            return BehaviorAlarmConfig(enabled, timeRanges.ifEmpty { listOf(TimeRange(21, 30, 8, 0)) }, threshold, message)
        }
    }
}
