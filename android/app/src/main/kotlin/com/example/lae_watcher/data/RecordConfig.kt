package com.example.lae_watcher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 触发类型枚举
 */
enum class TriggerType {
    TIME,      // 定时提醒触发
    BEHAVIOR;  // 行为监控触发

    companion object {
        fun fromString(value: String): TriggerType {
            return when (value.uppercase()) {
                "TIME" -> TIME
                "BEHAVIOR" -> BEHAVIOR
                else -> TIME // 默认类型
            }
        }
    }
}

/**
 * Type C - 记录配置数据类 (Phase 3)
 * @param uuid 唯一标识符 (UUID)
 * @param triggerType 触发类型 (TIME/BEHAVIOR)
 * @param triggerRefId 关联的触发源 ID (TimeAlarmItem.id 或 固定值 "behavior")
 * @param title 记录配置标题
 * @param questions 问题列表
 */
data class RecordConfig(
    val uuid: String,
    val triggerType: TriggerType,
    val triggerRefId: String,
    val title: String,
    val questions: List<Question>
) {
    /**
     * 格式化为显示字符串
     */
    override fun toString(): String {
        val triggerTypeStr = when (triggerType) {
            TriggerType.TIME -> "定时"
            TriggerType.BEHAVIOR -> "行为"
        }
        return "$title ($triggerTypeStr, ${questions.size} 题)"
    }

    /**
     * 转换为 Map (用于 MethodChannel 传递给 Flutter)
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uuid" to uuid,
            "triggerType" to triggerType.name,
            "triggerRefId" to triggerRefId,
            "title" to title,
            "questions" to questions.map { it.toMap() }
        )
    }

    /**
     * 转换为 JSON 对象 (用于 SharedPreferences 存储)
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("uuid", uuid)
            put("triggerType", triggerType.name)
            put("triggerRefId", triggerRefId)
            put("title", title)

            val questionsArray = JSONArray()
            questions.forEach { questionsArray.put(it.toJson()) }
            put("questions", questionsArray)
        }
    }

    companion object {
        /**
         * 创建新的记录配置 (自动生成 UUID)
         */
        fun create(
            triggerType: TriggerType,
            triggerRefId: String,
            title: String,
            questions: List<Question>
        ): RecordConfig {
            return RecordConfig(
                uuid = UUID.randomUUID().toString(),
                triggerType = triggerType,
                triggerRefId = triggerRefId,
                title = title.take(50), // 限制标题长度
                questions = questions
            )
        }

        /**
         * 从 JSON 对象创建 RecordConfig
         */
        fun fromJson(json: JSONObject): RecordConfig {
            val questionsArray = json.getJSONArray("questions")
            val questions = mutableListOf<Question>()
            for (i in 0 until questionsArray.length()) {
                questions.add(Question.fromJson(questionsArray.getJSONObject(i)))
            }

            return RecordConfig(
                uuid = json.getString("uuid"),
                triggerType = TriggerType.fromString(json.getString("triggerType")),
                triggerRefId = json.getString("triggerRefId"),
                title = json.getString("title"),
                questions = questions
            )
        }

        /**
         * 从 Map 创建 RecordConfig (用于 Flutter 传递数据)
         */
        fun fromMap(map: Map<String, Any?>): RecordConfig {
            val triggerTypeStr = map["triggerType"] as? String ?: "TIME"
            val triggerType = TriggerType.fromString(triggerTypeStr)

            @Suppress("UNCHECKED_CAST")
            val questionsList = map["questions"] as? List<Map<String, Any?>> ?: emptyList()
            val questions = questionsList.map { Question.fromMap(it) }

            return RecordConfig(
                uuid = map["uuid"] as? String ?: UUID.randomUUID().toString(),
                triggerType = triggerType,
                triggerRefId = map["triggerRefId"] as? String ?: "",
                title = map["title"] as? String ?: "未命名记录",
                questions = questions
            )
        }
    }
}

/**
 * 记录答案数据类
 * @param timestamp 答案提交时间戳 (Unix 时间戳，毫秒)
 * @param recordId 关联的 RecordConfig UUID
 * @param answers 答案列表 (questionId -> value 映射)
 */
data class RecordAnswer(
    val timestamp: Long,
    val recordId: String,
    val answers: Map<String, Any>
) {
    /**
     * 转换为 Map (用于 MethodChannel 传递给 Flutter)
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "timestamp" to timestamp,
            "recordId" to recordId,
            "answers" to answers
        )
    }

    /**
     * 转换为 JSON 对象 (用于 SharedPreferences 存储)
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("timestamp", timestamp)
            put("recordId", recordId)

            val answersObj = JSONObject()
            answers.forEach { (questionId, value) ->
                when (value) {
                    is List<*> -> {
                        // 多选题答案 (List<String>)
                        val arr = JSONArray()
                        value.forEach { arr.put(it) }
                        answersObj.put(questionId, arr)
                    }
                    else -> {
                        // 单选题/文本/滑动打分 (String/Int)
                        answersObj.put(questionId, value)
                    }
                }
            }
            put("answers", answersObj)
        }
    }

    companion object {
        /**
         * 创建新的答案记录
         */
        fun create(
            recordId: String,
            answers: Map<String, Any>
        ): RecordAnswer {
            return RecordAnswer(
                timestamp = System.currentTimeMillis(),
                recordId = recordId,
                answers = answers
            )
        }

        /**
         * 从 JSON 对象创建 RecordAnswer
         */
        fun fromJson(json: JSONObject): RecordAnswer {
            val answersObj = json.getJSONObject("answers")
            val answers = mutableMapOf<String, Any>()

            answersObj.keys().forEach { questionId ->
                val value = answersObj.get(questionId)
                when (value) {
                    is JSONArray -> {
                        // 多选题答案
                        val list = mutableListOf<String>()
                        for (i in 0 until value.length()) {
                            list.add(value.getString(i))
                        }
                        answers[questionId] = list
                    }
                    else -> {
                        // 单选题/文本/滑动打分
                        answers[questionId] = value
                    }
                }
            }

            return RecordAnswer(
                timestamp = json.getLong("timestamp"),
                recordId = json.getString("recordId"),
                answers = answers
            )
        }

        /**
         * 从 Map 创建 RecordAnswer (用于 Flutter 传递数据)
         */
        fun fromMap(map: Map<String, Any?>): RecordAnswer {
            @Suppress("UNCHECKED_CAST")
            val answersMap = map["answers"] as? Map<String, Any> ?: emptyMap()

            return RecordAnswer(
                timestamp = map["timestamp"] as? Long ?: System.currentTimeMillis(),
                recordId = map["recordId"] as? String ?: "",
                answers = answersMap
            )
        }
    }
}
