package com.example.lae_watcher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.lae_watcher.services.BehaviorMonitorService

/**
 * 行为监控闹钟接收器
 *
 * 功能:
 * - 接收 AlarmManager 发送的监控时段开始/结束事件
 * - 启动/停止 BehaviorMonitorService
 */
class BehaviorAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BehaviorAlarmReceiver"

        // Action 定义
        const val ACTION_START_MONITORING = "com.example.lae_watcher.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.lae_watcher.STOP_MONITORING"

        // Intent Extra 键名
        const val EXTRA_THRESHOLD_SECONDS = "threshold_seconds"
        const val EXTRA_TIME_RANGE_INDEX = "time_range_index"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        val timeRangeIndex = intent.getIntExtra(EXTRA_TIME_RANGE_INDEX, 0)

        when (intent.action) {
            ACTION_START_MONITORING -> {
                val thresholdSeconds = intent.getIntExtra(EXTRA_THRESHOLD_SECONDS, 30)
                Log.i(TAG, "⏰ 监控时段 ${timeRangeIndex + 1} 开始，阈值: ${thresholdSeconds}秒")
                startMonitoringService(context, thresholdSeconds)
            }
            ACTION_STOP_MONITORING -> {
                Log.i(TAG, "⏰ 监控时段 ${timeRangeIndex + 1} 结束")
                stopMonitoringService(context)
            }
            else -> {
                Log.w(TAG, "收到未知 Action: ${intent.action}")
            }
        }
    }

    /**
     * 启动监控服务
     */
    private fun startMonitoringService(context: Context, thresholdSeconds: Int) {
        val serviceIntent = Intent(context, BehaviorMonitorService::class.java).apply {
            putExtra(BehaviorMonitorService.EXTRA_THRESHOLD_SECONDS, thresholdSeconds)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.i(TAG, "✓ BehaviorMonitorService 启动指令已发送")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 启动 BehaviorMonitorService 失败", e)
        }
    }

    /**
     * 停止监控服务
     */
    private fun stopMonitoringService(context: Context) {
        val serviceIntent = Intent(context, BehaviorMonitorService::class.java).apply {
            action = BehaviorMonitorService.ACTION_STOP_MONITORING
        }

        try {
            context.startService(serviceIntent)
            Log.i(TAG, "✓ BehaviorMonitorService 停止指令已发送")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 停止 BehaviorMonitorService 失败", e)
        }
    }
}
