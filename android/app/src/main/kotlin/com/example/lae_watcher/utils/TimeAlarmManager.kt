package com.example.lae_watcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lae_watcher.data.TimeAlarmItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Type A - 定时提醒管理器
 * 负责提醒列表的增删改查和持久化存储
 */
class TimeAlarmManager(context: Context) {
    companion object {
        private const val TAG = "TimeAlarmManager"
        private const val PREFS_NAME = "time_alarms_prefs"
        private const val KEY_ALARMS = "alarms_list"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取所有提醒
     */
    fun getAll(): List<TimeAlarmItem> {
        return try {
            val jsonString = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonString)
            val alarms = mutableListOf<TimeAlarmItem>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                alarms.add(TimeAlarmItem.fromJson(json))
            }

            Log.d(TAG, "加载提醒列表: ${alarms.size} 个提醒")
            alarms
        } catch (e: Exception) {
            Log.e(TAG, "加载提醒列表失败", e)
            emptyList()
        }
    }

    /**
     * 添加新提醒
     * @return 是否添加成功
     */
    fun add(item: TimeAlarmItem): Boolean {
        return try {
            val alarms = getAll().toMutableList()
            alarms.add(item)
            save(alarms)
            Log.d(TAG, "添加提醒成功: ${item.toString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "添加提醒失败", e)
            false
        }
    }

    /**
     * 更新提醒
     * @return 是否更新成功
     */
    fun update(item: TimeAlarmItem): Boolean {
        return try {
            val alarms = getAll().toMutableList()
            val index = alarms.indexOfFirst { it.id == item.id }

            if (index == -1) {
                Log.w(TAG, "更新失败: 未找到 ID=${item.id} 的提醒")
                return false
            }

            alarms[index] = item
            save(alarms)
            Log.d(TAG, "更新提醒成功: ${item.toString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "更新提醒失败", e)
            false
        }
    }

    /**
     * 删除提醒
     * @return 是否删除成功
     */
    fun delete(id: String): Boolean {
        return try {
            val alarms = getAll().toMutableList()
            val removed = alarms.removeIf { it.id == id }

            if (!removed) {
                Log.w(TAG, "删除失败: 未找到 ID=$id 的提醒")
                return false
            }

            save(alarms)
            Log.d(TAG, "删除提醒成功: ID=$id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除提醒失败", e)
            false
        }
    }

    /**
     * 切换提醒的启用状态
     * @return 切换后的状态，失败返回 null
     */
    fun toggle(id: String): Boolean? {
        return try {
            val alarms = getAll().toMutableList()
            val index = alarms.indexOfFirst { it.id == id }

            if (index == -1) {
                Log.w(TAG, "切换状态失败: 未找到 ID=$id 的提醒")
                return null
            }

            val oldItem = alarms[index]
            val newItem = oldItem.copy(enabled = !oldItem.enabled)
            alarms[index] = newItem
            save(alarms)

            Log.d(TAG, "切换提醒状态: ID=$id, enabled=${newItem.enabled}")
            newItem.enabled
        } catch (e: Exception) {
            Log.e(TAG, "切换提醒状态失败", e)
            null
        }
    }

    /**
     * 根据 ID 获取提醒
     */
    fun getById(id: String): TimeAlarmItem? {
        return getAll().find { it.id == id }
    }

    /**
     * 获取所有已启用的提醒
     */
    fun getEnabled(): List<TimeAlarmItem> {
        return getAll().filter { it.enabled }
    }

    /**
     * 保存提醒列表
     */
    private fun save(alarms: List<TimeAlarmItem>): Boolean {
        return try {
            val jsonArray = JSONArray()
            alarms.forEach { jsonArray.put(it.toJson()) }

            prefs.edit()
                .putString(KEY_ALARMS, jsonArray.toString())
                .apply()

            true
        } catch (e: Exception) {
            Log.e(TAG, "保存提醒列表失败", e)
            false
        }
    }

    /**
     * 清空所有提醒
     */
    fun clear(): Boolean {
        return try {
            prefs.edit().remove(KEY_ALARMS).apply()
            Log.d(TAG, "清空所有提醒")
            true
        } catch (e: Exception) {
            Log.e(TAG, "清空提醒失败", e)
            false
        }
    }
}
