package com.example.lae_watcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lae_watcher.data.BehaviorAlarmItem
import org.json.JSONArray

/**
 * Type B - 行为监控提醒管理器
 * 负责监控配置列表的增删改查和持久化存储
 */
class BehaviorAlarmManager(context: Context) {
    companion object {
        private const val TAG = "BehaviorAlarmManager"
        private const val PREFS_NAME = "behavior_alarms_prefs"
        private const val KEY_ALARMS = "alarms_list"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取所有行为监控配置
     */
    fun getAll(): List<BehaviorAlarmItem> {
        return try {
            val jsonString = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonString)
            val alarms = mutableListOf<BehaviorAlarmItem>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                alarms.add(BehaviorAlarmItem.fromJson(json))
            }

            Log.d(TAG, "加载监控配置列表: ${alarms.size} 个配置")
            alarms
        } catch (e: Exception) {
            Log.e(TAG, "加载监控配置列表失败", e)
            emptyList()
        }
    }

    /**
     * 添加新监控配置
     * @return 是否添加成功
     */
    fun add(item: BehaviorAlarmItem): Boolean {
        return try {
            val alarms = getAll().toMutableList()
            alarms.add(item)
            save(alarms)
            Log.d(TAG, "添加监控配置成功: ${item.toString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "添加监控配置失败", e)
            false
        }
    }

    /**
     * 更新监控配置
     * @return 是否更新成功
     */
    fun update(item: BehaviorAlarmItem): Boolean {
        return try {
            val alarms = getAll().toMutableList()
            val index = alarms.indexOfFirst { it.id == item.id }

            if (index == -1) {
                Log.w(TAG, "更新失败: 未找到 ID=${item.id} 的监控配置")
                return false
            }

            alarms[index] = item
            save(alarms)
            Log.d(TAG, "更新监控配置成功: ${item.toString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "更新监控配置失败", e)
            false
        }
    }

    /**
     * 删除监控配置
     * @return 是否删除成功
     */
    fun delete(id: String): Boolean {
        return try {
            val alarms = getAll().toMutableList()
            val removed = alarms.removeIf { it.id == id }

            if (!removed) {
                Log.w(TAG, "删除失败: 未找到 ID=$id 的监控配置")
                return false
            }

            save(alarms)
            Log.d(TAG, "删除监控配置成功: ID=$id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除监控配置失败", e)
            false
        }
    }

    /**
     * 切换监控配置的启用状态
     * @return 切换后的状态，失败返回 null
     */
    fun toggle(id: String): Boolean? {
        return try {
            val alarms = getAll().toMutableList()
            val index = alarms.indexOfFirst { it.id == id }

            if (index == -1) {
                Log.w(TAG, "切换状态失败: 未找到 ID=$id 的监控配置")
                return null
            }

            val oldItem = alarms[index]
            val newItem = oldItem.copy(enabled = !oldItem.enabled)
            alarms[index] = newItem
            save(alarms)

            Log.d(TAG, "切换监控配置状态: ID=$id, enabled=${newItem.enabled}")
            newItem.enabled
        } catch (e: Exception) {
            Log.e(TAG, "切换监控配置状态失败", e)
            null
        }
    }

    /**
     * 根据 ID 获取监控配置
     */
    fun getById(id: String): BehaviorAlarmItem? {
        return getAll().find { it.id == id }
    }

    /**
     * 获取所有已启用的监控配置
     */
    fun getEnabled(): List<BehaviorAlarmItem> {
        return getAll().filter { it.enabled }
    }

    /**
     * 保存监控配置列表
     */
    private fun save(alarms: List<BehaviorAlarmItem>): Boolean {
        return try {
            val jsonArray = JSONArray()
            alarms.forEach { jsonArray.put(it.toJson()) }

            prefs.edit()
                .putString(KEY_ALARMS, jsonArray.toString())
                .apply()

            true
        } catch (e: Exception) {
            Log.e(TAG, "保存监控配置列表失败", e)
            false
        }
    }

    /**
     * 清空所有监控配置
     */
    fun clear(): Boolean {
        return try {
            prefs.edit().remove(KEY_ALARMS).apply()
            Log.d(TAG, "清空所有监控配置")
            true
        } catch (e: Exception) {
            Log.e(TAG, "清空监控配置失败", e)
            false
        }
    }
}
