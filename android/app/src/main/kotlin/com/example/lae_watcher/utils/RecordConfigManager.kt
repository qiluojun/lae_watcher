package com.example.lae_watcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lae_watcher.data.RecordConfig
import com.example.lae_watcher.data.TriggerType
import org.json.JSONArray

/**
 * Type C - 记录配置管理器 (Phase 3)
 * 负责记录配置的增删改查和持久化存储
 */
class RecordConfigManager(context: Context) {
    companion object {
        private const val TAG = "RecordConfigManager"
        private const val PREFS_NAME = "record_configs_prefs"
        private const val KEY_CONFIGS = "configs_list"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取所有记录配置
     */
    fun getAll(): List<RecordConfig> {
        return try {
            val jsonString = prefs.getString(KEY_CONFIGS, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonString)
            val configs = mutableListOf<RecordConfig>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                configs.add(RecordConfig.fromJson(json))
            }

            Log.d(TAG, "加载记录配置列表: ${configs.size} 个配置")
            configs
        } catch (e: Exception) {
            Log.e(TAG, "加载记录配置列表失败", e)
            emptyList()
        }
    }

    /**
     * 添加新记录配置
     * @return 是否添加成功
     */
    fun add(config: RecordConfig): Boolean {
        return try {
            val configs = getAll().toMutableList()
            configs.add(config)
            save(configs)
            Log.d(TAG, "添加记录配置成功: ${config.toString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "添加记录配置失败", e)
            false
        }
    }

    /**
     * 更新记录配置
     * @return 是否更新成功
     */
    fun update(config: RecordConfig): Boolean {
        return try {
            val configs = getAll().toMutableList()
            val index = configs.indexOfFirst { it.uuid == config.uuid }

            if (index == -1) {
                Log.w(TAG, "更新失败: 未找到 UUID=${config.uuid} 的记录配置")
                return false
            }

            configs[index] = config
            save(configs)
            Log.d(TAG, "更新记录配置成功: ${config.toString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "更新记录配置失败", e)
            false
        }
    }

    /**
     * 删除记录配置
     * @return 是否删除成功
     */
    fun delete(uuid: String): Boolean {
        return try {
            val configs = getAll().toMutableList()
            val removed = configs.removeIf { it.uuid == uuid }

            if (!removed) {
                Log.w(TAG, "删除失败: 未找到 UUID=$uuid 的记录配置")
                return false
            }

            save(configs)
            Log.d(TAG, "删除记录配置成功: UUID=$uuid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除记录配置失败", e)
            false
        }
    }

    /**
     * 根据 UUID 获取记录配置
     */
    fun getByUuid(uuid: String): RecordConfig? {
        return getAll().find { it.uuid == uuid }
    }

    /**
     * 根据触发源 ID 获取关联的记录配置
     * @param triggerRefId 触发源 ID (TimeAlarmItem.id 或 "behavior")
     * @return 关联的记录配置列表
     */
    fun getByTriggerRefId(triggerRefId: String): List<RecordConfig> {
        return getAll().filter { it.triggerRefId == triggerRefId }
    }

    /**
     * 根据触发类型获取记录配置
     * @param triggerType 触发类型 (TIME/BEHAVIOR)
     * @return 该类型的所有记录配置
     */
    fun getByTriggerType(triggerType: TriggerType): List<RecordConfig> {
        return getAll().filter { it.triggerType == triggerType }
    }

    /**
     * 删除与指定触发源关联的所有记录配置
     * 用于级联删除 (当 TimeAlarmItem 被删除时，同时删除关联的记录配置)
     * @param triggerRefId 触发源 ID
     * @return 删除的记录配置数量
     */
    fun deleteByTriggerRefId(triggerRefId: String): Int {
        return try {
            val configs = getAll().toMutableList()
            val originalSize = configs.size
            configs.removeIf { it.triggerRefId == triggerRefId }
            val deletedCount = originalSize - configs.size

            if (deletedCount > 0) {
                save(configs)
                Log.d(TAG, "级联删除记录配置: triggerRefId=$triggerRefId, 删除数量=$deletedCount")
            }

            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "级联删除记录配置失败", e)
            0
        }
    }

    /**
     * 保存记录配置列表
     */
    private fun save(configs: List<RecordConfig>): Boolean {
        return try {
            val jsonArray = JSONArray()
            configs.forEach { jsonArray.put(it.toJson()) }

            prefs.edit()
                .putString(KEY_CONFIGS, jsonArray.toString())
                .apply()

            true
        } catch (e: Exception) {
            Log.e(TAG, "保存记录配置列表失败", e)
            false
        }
    }

    /**
     * 清空所有记录配置
     */
    fun clear(): Boolean {
        return try {
            prefs.edit().remove(KEY_CONFIGS).apply()
            Log.d(TAG, "清空所有记录配置")
            true
        } catch (e: Exception) {
            Log.e(TAG, "清空记录配置失败", e)
            false
        }
    }
}
