package com.example.lae_watcher.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lae_watcher.data.BehaviorAlarmConfig
import com.example.lae_watcher.data.TimeRange
import com.example.lae_watcher.receivers.BehaviorAlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

/**
 * 行为监控提醒调度器
 *
 * 功能:
 * - 使用 AlarmManager 在监控时段开始时启动 BehaviorMonitorService
 * - 使用 AlarmManager 在监控时段结束时停止 BehaviorMonitorService
 * - 支持多时段配置
 */
object BehaviorAlarmScheduler {
    private const val TAG = "BehaviorAlarmScheduler"

    // REQUEST_CODE 基数（每个时段使用 baseCode 和 baseCode+1）
    private const val REQUEST_CODE_BASE = 3000

    /**
     * 启动监控调度
     * @param config 行为监控配置
     */
    fun start(context: Context, config: BehaviorAlarmConfig) {
        if (!config.enabled) {
            Log.w(TAG, "配置未启用，忽略启动请求")
            return
        }

        Log.i(TAG, "🚀 启动行为监控调度")
        Log.i(TAG, "  → 阈值: ${config.screenTimeThreshold}秒")
        Log.i(TAG, "  → 提示语: ${config.message}")
        Log.i(TAG, "  → 时段数: ${config.timeRanges.size}")

        // 取消旧的调度
        cancelAll(context)

        // 为每个时段设置调度
        config.timeRanges.forEachIndexed { index, timeRange ->
            scheduleTimeRange(context, index, timeRange, config.screenTimeThreshold, config.message)
        }

        Log.i(TAG, "✓ 行为监控调度已启动")
    }

    /**
     * 停止监控调度
     */
    fun stop(context: Context) {
        Log.i(TAG, "🛑 停止行为监控调度")
        cancelAll(context)

        // 同时停止正在运行的服务
        stopMonitoringService(context)

        Log.i(TAG, "✓ 行为监控调度已停止")
    }

    /**
     * 为单个时段设置调度
     */
    private fun scheduleTimeRange(
        context: Context,
        index: Int,
        timeRange: TimeRange,
        thresholdSeconds: Int,
        alarmMessage: String
    ) {
        Log.i(TAG, "⏰ 设置时段 ${index + 1}: $timeRange")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. 设置开始时间的闹钟
        val startRequestCode = REQUEST_CODE_BASE + (index * 2)
        val startTriggerTime = calculateNextTriggerTime(
            timeRange.startHour,
            timeRange.startMinute
        )

        val startIntent = Intent(context, BehaviorAlarmReceiver::class.java).apply {
            action = BehaviorAlarmReceiver.ACTION_START_MONITORING
            putExtra(BehaviorAlarmReceiver.EXTRA_THRESHOLD_SECONDS, thresholdSeconds)
            putExtra(BehaviorAlarmReceiver.EXTRA_ALARM_MESSAGE, alarmMessage)
            putExtra(BehaviorAlarmReceiver.EXTRA_TIME_RANGE_INDEX, index)
        }

        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            startRequestCode,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            startTriggerTime,
            AlarmManager.INTERVAL_DAY, // 每天重复
            startPendingIntent
        )

        val startTimeStr = formatTime(startTriggerTime)
        Log.i(TAG, "  → 开始时间: ${timeRange.startHour}:${String.format("%02d", timeRange.startMinute)}")
        Log.d(TAG, "     下次触发: $startTimeStr")

        // 2. 设置结束时间的闹钟
        val endRequestCode = REQUEST_CODE_BASE + (index * 2) + 1
        val endTriggerTime = calculateNextTriggerTime(
            timeRange.endHour,
            timeRange.endMinute
        )

        val endIntent = Intent(context, BehaviorAlarmReceiver::class.java).apply {
            action = BehaviorAlarmReceiver.ACTION_STOP_MONITORING
            putExtra(BehaviorAlarmReceiver.EXTRA_TIME_RANGE_INDEX, index)
        }

        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            endRequestCode,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            endTriggerTime,
            AlarmManager.INTERVAL_DAY,
            endPendingIntent
        )

        val endTimeStr = formatTime(endTriggerTime)
        Log.i(TAG, "  → 结束时间: ${timeRange.endHour}:${String.format("%02d", timeRange.endMinute)}")
        Log.d(TAG, "     下次触发: $endTimeStr")

        // 3. 检查当前是否在监控时段内，如果是则立即启动服务
        checkAndStartIfInRange(context, timeRange, thresholdSeconds, alarmMessage)
    }

    /**
     * 检查当前是否在监控时段内，如果是则立即启动服务
     */
    private fun checkAndStartIfInRange(
        context: Context,
        timeRange: TimeRange,
        thresholdSeconds: Int,
        alarmMessage: String
    ) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        Log.d(TAG, "  → 检查当前时间: $currentHour:${String.format("%02d", currentMinute)}")
        Log.d(TAG, "     监控时段: ${timeRange.startHour}:${String.format("%02d", timeRange.startMinute)} - ${timeRange.endHour}:${String.format("%02d", timeRange.endMinute)}")

        if (timeRange.isTimeInRange(currentHour, currentMinute)) {
            Log.i(TAG, "  ✓ 当前时间在监控时段内，立即启动服务")
            startMonitoringService(context, thresholdSeconds, alarmMessage)
        } else {
            Log.i(TAG, "  ✗ 当前时间不在监控时段内，等待闹钟触发")
        }
    }

    /**
     * 计算下次触发时间
     */
    private fun calculateNextTriggerTime(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            // 先设置为今天的目标时间
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayTriggerTime = calendar.timeInMillis

        // 如果今天的触发时间已经过去（包括正好等于当前时间），延后到明天
        if (todayTriggerTime <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "     (今天的 $hour:${String.format("%02d", minute)} 已过，推迟到明天)")
        }

        return calendar.timeInMillis
    }

    /**
     * 格式化时间
     */
    private fun formatTime(timeInMillis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(Date(timeInMillis))
    }

    /**
     * 取消所有调度（最多支持 10 个时段）
     */
    private fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 取消最多 10 个时段的调度（每个时段 2 个闹钟）
        for (i in 0 until 10) {
            val startRequestCode = REQUEST_CODE_BASE + (i * 2)
            val endRequestCode = REQUEST_CODE_BASE + (i * 2) + 1

            val startIntent = Intent(context, BehaviorAlarmReceiver::class.java)
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                startRequestCode,
                startIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            startPendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            val endIntent = Intent(context, BehaviorAlarmReceiver::class.java)
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                endRequestCode,
                endIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            endPendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        Log.d(TAG, "✓ 已取消所有调度")
    }

    /**
     * 启动监控服务
     */
    private fun startMonitoringService(context: Context, thresholdSeconds: Int, alarmMessage: String) {
        val serviceIntent = Intent(context, com.example.lae_watcher.services.BehaviorMonitorService::class.java).apply {
            putExtra(com.example.lae_watcher.services.BehaviorMonitorService.EXTRA_THRESHOLD_SECONDS, thresholdSeconds)
            putExtra(com.example.lae_watcher.services.BehaviorMonitorService.EXTRA_ALARM_MESSAGE, alarmMessage)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
        val serviceIntent = Intent(context, com.example.lae_watcher.services.BehaviorMonitorService::class.java).apply {
            action = com.example.lae_watcher.services.BehaviorMonitorService.ACTION_STOP_MONITORING
        }

        try {
            context.startService(serviceIntent)
            Log.i(TAG, "✓ BehaviorMonitorService 停止指令已发送")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 停止 BehaviorMonitorService 失败", e)
        }
    }
}
