package com.example.lae_watcher.utils

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.example.lae_watcher.activities.AlarmActivity

/**
 * 亮屏时长统计器
 *
 * 功能:
 * - 统计累计亮屏时长
 * - 超过阈值时触发提醒
 * - 支持重置计时器
 */
class ScreenTimeTracker(
    private val context: Context,
    private var thresholdSeconds: Int,
    private var alarmMessage: String = "亮屏时间过长！" // 自定义提示语
) {
    companion object {
        private const val TAG = "ScreenTimeTracker"
    }

    // 累计亮屏时长（毫秒）
    private var accumulatedTime: Long = 0

    // 当前亮屏开始时间（SystemClock.elapsedRealtime）
    private var screenOnStartTime: Long = 0

    // 是否正在亮屏
    private var isScreenOn = false

    // 是否已触发提醒（防止重复触发）
    private var hasTriggered = false

    /**
     * 屏幕亮起事件
     */
    fun onScreenOn() {
        if (isScreenOn) {
            Log.w(TAG, "屏幕已经是亮屏状态，忽略重复事件")
            return
        }

        isScreenOn = true
        screenOnStartTime = SystemClock.elapsedRealtime()
        Log.i(TAG, "📱 屏幕亮起，开始计时")
        Log.d(TAG, "  → 当前累计时长: ${accumulatedTime / 1000}秒 / ${thresholdSeconds}秒")
    }

    /**
     * 屏幕熄灭事件
     */
    fun onScreenOff() {
        if (!isScreenOn) {
            Log.w(TAG, "屏幕已经是息屏状态，忽略重复事件")
            return
        }

        isScreenOn = false

        // 计算本次亮屏时长
        val currentSessionTime = SystemClock.elapsedRealtime() - screenOnStartTime
        accumulatedTime += currentSessionTime

        val totalSeconds = accumulatedTime / 1000
        Log.i(TAG, "📱 屏幕熄灭，本次亮屏: ${currentSessionTime / 1000}秒")
        Log.i(TAG, "  → 累计亮屏时长: ${totalSeconds}秒 / ${thresholdSeconds}秒")

        // 检查是否超过阈值
        checkThreshold()
    }

    /**
     * 定期检查（亮屏状态下每秒调用）
     */
    fun tick() {
        if (!isScreenOn) {
            return
        }

        // 计算当前总时长
        val currentSessionTime = SystemClock.elapsedRealtime() - screenOnStartTime
        val totalTime = accumulatedTime + currentSessionTime
        val totalSeconds = totalTime / 1000

        // 每 5 秒打印一次日志（避免日志过多）
        if (totalSeconds % 5 == 0L) {
            Log.d(TAG, "⏱️ 亮屏计时中: ${totalSeconds}秒 / ${thresholdSeconds}秒")
        }

        // 检查是否超过阈值
        if (totalSeconds >= thresholdSeconds && !hasTriggered) {
            Log.i(TAG, "⚠️ 亮屏时长超过阈值！触发提醒")
            triggerAlarm()
        }
    }

    /**
     * 检查是否超过阈值（息屏时调用）
     */
    private fun checkThreshold() {
        val totalSeconds = accumulatedTime / 1000
        if (totalSeconds >= thresholdSeconds && !hasTriggered) {
            Log.i(TAG, "⚠️ 累计亮屏时长超过阈值！触发提醒")
            triggerAlarm()
        }
    }

    /**
     * 触发提醒
     */
    private fun triggerAlarm() {
        hasTriggered = true

        Log.i(TAG, "🚨 启动 AlarmActivity...")
        Log.i(TAG, "  → 提示语: $alarmMessage")

        // 启动 AlarmActivity
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_MESSAGE, alarmMessage) // 使用自定义提示语
            putExtra(AlarmActivity.EXTRA_ALARM_TYPE, "behavior") // 标记为行为监控提醒
        }

        try {
            context.startActivity(intent)
            Log.i(TAG, "✓ AlarmActivity 已启动")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 启动 AlarmActivity 失败", e)
        }
    }

    /**
     * 重置计时器
     * 点击"我知道了"后调用
     */
    fun reset() {
        val oldTotal = (accumulatedTime + if (isScreenOn) {
            SystemClock.elapsedRealtime() - screenOnStartTime
        } else {
            0
        }) / 1000

        accumulatedTime = 0
        hasTriggered = false

        // 如果当前是亮屏状态，重新开始计时
        if (isScreenOn) {
            screenOnStartTime = SystemClock.elapsedRealtime()
        }

        Log.i(TAG, "🔄 计时器已重置")
        Log.d(TAG, "  → 之前累计: ${oldTotal}秒")
        Log.d(TAG, "  → 当前状态: ${if (isScreenOn) "亮屏，重新开始计时" else "息屏"}")
    }

    /**
     * 更新阈值
     */
    fun updateThreshold(newThresholdSeconds: Int) {
        val oldThreshold = thresholdSeconds
        thresholdSeconds = newThresholdSeconds
        Log.i(TAG, "⚙️ 阈值已更新: ${oldThreshold}秒 → ${newThresholdSeconds}秒")
    }

    /**
     * 获取当前统计信息（调试用）
     */
    fun getStats(): String {
        val currentTotal = if (isScreenOn) {
            accumulatedTime + (SystemClock.elapsedRealtime() - screenOnStartTime)
        } else {
            accumulatedTime
        }
        val totalSeconds = currentTotal / 1000

        return "亮屏时长: ${totalSeconds}秒 / ${thresholdSeconds}秒, " +
               "状态: ${if (isScreenOn) "亮屏" else "息屏"}, " +
               "已触发: $hasTriggered"
    }
}
