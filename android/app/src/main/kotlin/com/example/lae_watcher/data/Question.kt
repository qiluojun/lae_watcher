package com.example.lae_watcher.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 问题类型枚举
 */
enum class QuestionType {
    CHOICE,  // 单选/多选题
    TEXT,    // 自由文本输入
    SLIDER;  // 滑动打分

    companion object {
        fun fromString(value: String): QuestionType {
            return when (value.uppercase()) {
                "CHOICE" -> CHOICE
                "TEXT" -> TEXT
                "SLIDER" -> SLIDER
                else -> TEXT // 默认类型
            }
        }
    }
}

/**
 * 问题数据类 (Phase 3)
 * @param id 问题唯一标识符
 * @param type 问题类型 (CHOICE/TEXT/SLIDER)
 * @param title 问题标题/描述
 * @param options 选项列表 (仅 CHOICE 类型使用)
 * @param isMultipleChoice 是否多选 (仅 CHOICE 类型使用, 默认 false)
 * @param min 最小值 (仅 SLIDER 类型使用)
 * @param max 最大值 (仅 SLIDER 类型使用)
 * @param step 步进值 (仅 SLIDER 类型使用)
 */
data class Question(
    val id: String,
    val type: QuestionType,
    val title: String,
    val options: List<String>? = null,
    val isMultipleChoice: Boolean = false,
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null
) {
    /**
     * 转换为 JSON 对象 (用于 SharedPreferences 存储)
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("type", type.name)
            put("title", title)

            // CHOICE 类型字段
            if (options != null) {
                val optionsArray = JSONArray()
                options.forEach { optionsArray.put(it) }
                put("options", optionsArray)
                put("isMultipleChoice", isMultipleChoice)
            } else {
                put("options", JSONObject.NULL)
                put("isMultipleChoice", false)
            }

            // SLIDER 类型字段
            put("min", min ?: JSONObject.NULL)
            put("max", max ?: JSONObject.NULL)
            put("step", step ?: JSONObject.NULL)
        }
    }

    /**
     * 转换为 Map (用于 MethodChannel 传递给 Flutter)
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "type" to type.name,
            "title" to title,
            "options" to options,
            "isMultipleChoice" to isMultipleChoice,
            "min" to min,
            "max" to max,
            "step" to step
        )
    }

    companion object {
        /**
         * 从 JSON 对象创建 Question
         */
        fun fromJson(json: JSONObject): Question {
            val type = QuestionType.fromString(json.getString("type"))

            // 解析选项列表
            val options = if (!json.isNull("options")) {
                val optionsArray = json.getJSONArray("options")
                val list = mutableListOf<String>()
                for (i in 0 until optionsArray.length()) {
                    list.add(optionsArray.getString(i))
                }
                list
            } else null

            return Question(
                id = json.getString("id"),
                type = type,
                title = json.getString("title"),
                options = options,
                isMultipleChoice = json.optBoolean("isMultipleChoice", false),
                min = if (json.isNull("min")) null else json.getInt("min"),
                max = if (json.isNull("max")) null else json.getInt("max"),
                step = if (json.isNull("step")) null else json.getInt("step")
            )
        }

        /**
         * 从 Map 创建 Question (用于 Flutter 传递数据)
         */
        fun fromMap(map: Map<String, Any?>): Question {
            val typeStr = map["type"] as? String ?: "TEXT"
            val type = QuestionType.fromString(typeStr)

            @Suppress("UNCHECKED_CAST")
            val options = map["options"] as? List<String>

            return Question(
                id = map["id"] as? String ?: "",
                type = type,
                title = map["title"] as? String ?: "",
                options = options,
                isMultipleChoice = map["isMultipleChoice"] as? Boolean ?: false,
                min = map["min"] as? Int,
                max = map["max"] as? Int,
                step = map["step"] as? Int
            )
        }

        /**
         * 创建单选题
         */
        fun createChoiceQuestion(
            id: String,
            title: String,
            options: List<String>,
            isMultipleChoice: Boolean = false
        ): Question {
            return Question(
                id = id,
                type = QuestionType.CHOICE,
                title = title,
                options = options,
                isMultipleChoice = isMultipleChoice
            )
        }

        /**
         * 创建文本题
         */
        fun createTextQuestion(
            id: String,
            title: String
        ): Question {
            return Question(
                id = id,
                type = QuestionType.TEXT,
                title = title
            )
        }

        /**
         * 创建滑动打分题
         */
        fun createSliderQuestion(
            id: String,
            title: String,
            min: Int = 0,
            max: Int = 10,
            step: Int = 1
        ): Question {
            return Question(
                id = id,
                type = QuestionType.SLIDER,
                title = title,
                min = min,
                max = max,
                step = step
            )
        }
    }
}
