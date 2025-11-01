package com.example.lae_watcher.utils

import android.content.Context
import android.util.Log
import com.example.lae_watcher.data.BehaviorAlarmConfig

/**
 * 行为监控提醒配置管理器
 * 负责配置的持久化存储和读取
 */
object BehaviorAlarmConfigManager {
    private const val TAG = "BehaviorAlarmConfigManager"
    private const val PREFS_NAME = "behavior_alarm_prefs"
    private const val KEY_CONFIG = "behavior_alarm_config"

    /**
     * 保存配置到 SharedPreferences
     */
    fun saveConfig(context: Context, config: BehaviorAlarmConfig) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = config.toJsonString()

            prefs.edit().apply {
                putString(KEY_CONFIG, jsonString)
                apply()
            }

            Log.i(TAG, "配置已保存: enabled=${config.enabled}, threshold=${config.screenTimeThreshold}秒, 时段数=${config.timeRanges.size}")
            config.timeRanges.forEachIndexed { index, range ->
                Log.d(TAG, "  时段${index + 1}: $range")
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存配置失败: ${e.message}", e)
        }
    }

    /**
     * 从 SharedPreferences 读取配置
     * @return 配置对象，如果不存在则返回默认配置
     */
    fun loadConfig(context: Context): BehaviorAlarmConfig {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_CONFIG, null)

            if (jsonString != null) {
                val config = BehaviorAlarmConfig.fromJsonString(jsonString)
                Log.i(TAG, "配置已加载: enabled=${config.enabled}, threshold=${config.screenTimeThreshold}秒, 时段数=${config.timeRanges.size}")
                config
            } else {
                Log.i(TAG, "配置不存在，使用默认配置")
                BehaviorAlarmConfig.default()
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败，使用默认配置: ${e.message}", e)
            BehaviorAlarmConfig.default()
        }
    }

    /**
     * 更新配置的启用状态
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val config = loadConfig(context)
        val newConfig = config.copy(enabled = enabled)
        saveConfig(context, newConfig)
    }

    /**
     * 更新配置的阈值
     */
    fun setThreshold(context: Context, threshold: Int) {
        val config = loadConfig(context)
        val newConfig = config.copy(screenTimeThreshold = threshold.coerceIn(10, 600))
        saveConfig(context, newConfig)
    }

    /**
     * 清除所有配置（恢复默认）
     */
    fun clearConfig(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.i(TAG, "配置已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除配置失败: ${e.message}", e)
        }
    }
}
