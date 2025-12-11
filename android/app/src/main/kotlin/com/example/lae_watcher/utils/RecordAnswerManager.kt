package com.example.lae_watcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lae_watcher.data.RecordAnswer
import org.json.JSONArray

/**
 * 记录答案管理器 (Phase 3)
 * 负责答案的追加存储、查询和导出
 */
class RecordAnswerManager(context: Context) {
    companion object {
        private const val TAG = "RecordAnswerManager"
        private const val PREFS_NAME = "record_answers_prefs"
        private const val KEY_ANSWERS = "answers_list"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取所有答案记录
     * @return 答案列表 (按时间戳倒序排列，最新的在前)
     */
    fun getAll(): List<RecordAnswer> {
        return try {
            val jsonString = prefs.getString(KEY_ANSWERS, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonString)
            val answers = mutableListOf<RecordAnswer>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                answers.add(RecordAnswer.fromJson(json))
            }

            Log.d(TAG, "加载答案记录列表: ${answers.size} 条记录")
            // 按时间戳倒序排列
            answers.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "加载答案记录列表失败", e)
            emptyList()
        }
    }

    /**
     * 追加新答案记录
     * @param answer 答案记录
     * @return 是否添加成功
     */
    fun add(answer: RecordAnswer): Boolean {
        return try {
            val answers = getAllRaw().toMutableList()
            answers.add(answer)
            save(answers)
            Log.d(TAG, "添加答案记录成功: recordId=${answer.recordId}, timestamp=${answer.timestamp}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "添加答案记录失败", e)
            false
        }
    }

    /**
     * 根据 recordId 获取答案记录
     * @param recordId 记录配置 UUID
     * @return 该记录的所有答案 (按时间戳倒序排列)
     */
    fun getByRecordId(recordId: String): List<RecordAnswer> {
        return getAll().filter { it.recordId == recordId }
    }

    /**
     * 获取指定时间范围内的答案记录
     * @param startTimestamp 起始时间戳 (Unix 毫秒)
     * @param endTimestamp 结束时间戳 (Unix 毫秒)
     * @return 时间范围内的答案列表 (按时间戳倒序排列)
     */
    fun getByTimeRange(startTimestamp: Long, endTimestamp: Long): List<RecordAnswer> {
        return getAll().filter { it.timestamp in startTimestamp..endTimestamp }
    }

    /**
     * 获取最近 N 条答案记录
     * @param limit 记录数量
     * @return 最近的答案列表
     */
    fun getRecent(limit: Int): List<RecordAnswer> {
        return getAll().take(limit)
    }

    /**
     * 删除指定时间戳的答案记录
     * @param timestamp 时间戳
     * @return 是否删除成功
     */
    fun deleteByTimestamp(timestamp: Long): Boolean {
        return try {
            val answers = getAllRaw().toMutableList()
            val removed = answers.removeIf { it.timestamp == timestamp }

            if (!removed) {
                Log.w(TAG, "删除失败: 未找到 timestamp=$timestamp 的答案记录")
                return false
            }

            save(answers)
            Log.d(TAG, "删除答案记录成功: timestamp=$timestamp")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除答案记录失败", e)
            false
        }
    }

    /**
     * 删除指定 recordId 的所有答案记录
     * 用于级联删除 (当 RecordConfig 被删除时，同时删除关联的答案)
     * @param recordId 记录配置 UUID
     * @return 删除的答案数量
     */
    fun deleteByRecordId(recordId: String): Int {
        return try {
            val answers = getAllRaw().toMutableList()
            val originalSize = answers.size
            answers.removeIf { it.recordId == recordId }
            val deletedCount = originalSize - answers.size

            if (deletedCount > 0) {
                save(answers)
                Log.d(TAG, "级联删除答案记录: recordId=$recordId, 删除数量=$deletedCount")
            }

            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "级联删除答案记录失败", e)
            0
        }
    }

    /**
     * 删除指定时间范围内的答案记录
     * @param startTimestamp 起始时间戳
     * @param endTimestamp 结束时间戳
     * @return 删除的答案数量
     */
    fun deleteByTimeRange(startTimestamp: Long, endTimestamp: Long): Int {
        return try {
            val answers = getAllRaw().toMutableList()
            val originalSize = answers.size
            answers.removeIf { it.timestamp in startTimestamp..endTimestamp }
            val deletedCount = originalSize - answers.size

            if (deletedCount > 0) {
                save(answers)
                Log.d(TAG, "删除时间范围答案记录: 删除数量=$deletedCount")
            }

            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "删除时间范围答案记录失败", e)
            0
        }
    }

    /**
     * 获取答案记录总数
     */
    fun getCount(): Int {
        return getAllRaw().size
    }

    /**
     * 获取指定 recordId 的答案记录数量
     */
    fun getCountByRecordId(recordId: String): Int {
        return getByRecordId(recordId).size
    }

    /**
     * 导出所有答案为 JSON 字符串
     * 用于数据备份或导出
     */
    fun exportToJson(): String {
        return try {
            val answers = getAllRaw()
            val jsonArray = JSONArray()
            answers.forEach { jsonArray.put(it.toJson()) }
            jsonArray.toString()
        } catch (e: Exception) {
            Log.e(TAG, "导出答案记录失败", e)
            "[]"
        }
    }

    /**
     * 从 JSON 字符串导入答案
     * 用于数据恢复
     * @param jsonString JSON 字符串
     * @return 是否导入成功
     */
    fun importFromJson(jsonString: String): Boolean {
        return try {
            val jsonArray = JSONArray(jsonString)
            val answers = mutableListOf<RecordAnswer>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                answers.add(RecordAnswer.fromJson(json))
            }

            save(answers)
            Log.d(TAG, "导入答案记录成功: ${answers.size} 条记录")
            true
        } catch (e: Exception) {
            Log.e(TAG, "导入答案记录失败", e)
            false
        }
    }

    /**
     * 获取原始答案列表 (不排序)
     */
    private fun getAllRaw(): List<RecordAnswer> {
        return try {
            val jsonString = prefs.getString(KEY_ANSWERS, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonString)
            val answers = mutableListOf<RecordAnswer>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                answers.add(RecordAnswer.fromJson(json))
            }

            answers
        } catch (e: Exception) {
            Log.e(TAG, "加载原始答案列表失败", e)
            emptyList()
        }
    }

    /**
     * 保存答案列表
     */
    private fun save(answers: List<RecordAnswer>): Boolean {
        return try {
            val jsonArray = JSONArray()
            answers.forEach { jsonArray.put(it.toJson()) }

            prefs.edit()
                .putString(KEY_ANSWERS, jsonArray.toString())
                .apply()

            true
        } catch (e: Exception) {
            Log.e(TAG, "保存答案列表失败", e)
            false
        }
    }

    /**
     * 清空所有答案记录
     */
    fun clear(): Boolean {
        return try {
            prefs.edit().remove(KEY_ANSWERS).apply()
            Log.d(TAG, "清空所有答案记录")
            true
        } catch (e: Exception) {
            Log.e(TAG, "清空答案记录失败", e)
            false
        }
    }
}
