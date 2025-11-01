package com.example.lae_watcher.receivers

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lae_watcher.R
import com.example.lae_watcher.activities.AlarmActivity
import com.example.lae_watcher.utils.AlarmScheduler

/**
 * TimeReceiver - 定时提醒广播接收器
 *
 * 功能: 接收 AlarmManager 触发的定时事件
 * 触发时机: 每日 15:15 (默认)
 *
 * 后续任务: Step 3 时将在此触发全屏提醒
 */
class TimeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimeReceiver"
        const val ACTION_DAILY_ALARM = "com.example.lae_watcher.ACTION_DAILY_ALARM"
        private const val NOTIFICATION_CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "收到定时提醒触发事件")

        when (intent.action) {
            ACTION_DAILY_ALARM -> {
                handleDailyAlarm(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // 开机自启：重新设置定时提醒
                Log.d(TAG, "设备重启，重新设置定时提醒")
                rescheduleAlarm(context)
            }
        }
    }

    /**
     * 处理每日定时提醒
     */
    private fun handleDailyAlarm(context: Context) {
        Log.i(TAG, "========================================")
        Log.i(TAG, "⏰ 定时提醒触发！")
        Log.i(TAG, "触发时间: ${System.currentTimeMillis()}")

        // 检查屏幕状态
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive
        Log.i(TAG, "屏幕状态: ${if (isScreenOn) "亮屏" else "息屏"}")

        // 唤醒屏幕
        wakeUpScreen(context)

        // 直接启动 AlarmActivity
        launchAlarmActivity(context)

        // 重新设置明天的定时提醒
        rescheduleAlarm(context)

        Log.i(TAG, "========================================")
    }

    /**
     * 直接启动 AlarmActivity
     */
    private fun launchAlarmActivity(context: Context) {
        Log.d(TAG, "→ 开始启动 AlarmActivity...")

        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }

        try {
            context.startActivity(intent)
            Log.i(TAG, "  ✓ AlarmActivity 已启动")
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 启动 AlarmActivity 失败", e)
        }
    }

    /**
     * 唤醒屏幕（支持息屏状态）
     */
    private fun wakeUpScreen(context: Context) {
        try {
            Log.d(TAG, "  → 尝试获取 WakeLock...")
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            // 检查当前屏幕状态
            val isScreenOnBefore = powerManager.isInteractive
            Log.d(TAG, "  → WakeLock获取前屏幕状态: ${if (isScreenOnBefore) "亮屏" else "息屏"}")

            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "LAEWatcher:AlarmWakeLock"
            )

            // 唤醒屏幕，持续 3 秒（足够 Activity 启动）
            wakeLock.acquire(3000L)
            Log.i(TAG, "  ✓ WakeLock 已获取 (持续3秒)")

            // 再次检查屏幕状态
            val isScreenOnAfter = powerManager.isInteractive
            Log.d(TAG, "  → WakeLock获取后屏幕状态: ${if (isScreenOnAfter) "亮屏" else "息屏"}")

            // WakeLock 会在 3 秒后自动释放，或由 Activity 接管
        } catch (e: SecurityException) {
            Log.e(TAG, "  ✗ WakeLock 权限不足", e)
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 唤醒屏幕失败", e)
        }
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "定时提醒",
                NotificationManager.IMPORTANCE_HIGH  // 高重要性，允许全屏通知
            ).apply {
                description = "LAE-Watcher 定时提醒通知"
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                // 允许绕过勿扰模式
                setBypassDnd(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "通知渠道已创建: 重要性=HIGH, 可绕过勿扰")
        }
    }

    /**
     * 显示全屏提醒（支持息屏唤醒）
     */
    private fun showFullScreenAlarm(context: Context) {
        Log.d(TAG, "→ 开始发送 FullScreenIntent 通知...")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "  → 通知权限状态: ${if (hasNotificationPermission) "已授权" else "未授权"}")

            if (!hasNotificationPermission) {
                Log.e(TAG, "  ✗ 缺少 POST_NOTIFICATIONS 权限，无法显示通知")
                return
            }
        }

        // 检查全屏通知权限 (Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val canUseFullScreenIntent = notificationManager.canUseFullScreenIntent()
            Log.d(TAG, "  → 全屏通知权限状态: ${if (canUseFullScreenIntent) "已授权" else "未授权"}")

            if (!canUseFullScreenIntent) {
                Log.w(TAG, "  ⚠ 需要用户手动授权全屏通知权限")
                // 即使没有权限也继续发送通知，至少能显示在通知栏
            }
        }

        // 创建启动 AlarmActivity 的 Intent
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "  → 构建通知...")

        // 构建通知（使用 FullScreenIntent 实现息屏唤醒）
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // 使用系统内置图标
            .setContentTitle("该休息了！")
            .setContentText("点击查看提醒")
            .setPriority(NotificationCompat.PRIORITY_MAX)  // 最高优先级
            .setCategory(NotificationCompat.CATEGORY_ALARM)  // 闹钟类别（重要！）
            .setAutoCancel(true)
            .setOngoing(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)  // 关键：全屏 Intent
            .setContentIntent(fullScreenPendingIntent)  // 点击通知也能打开
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // 使用默认声音、振动、灯光
            .setVibrate(longArrayOf(0, 500, 200, 500))  // 振动模式
            .build()

        // 显示通知
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "  ✓ FullScreenIntent 通知已发送 (ID: $NOTIFICATION_ID)")
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 发送通知失败", e)
        }
    }

    /**
     * 重新设置定时提醒
     */
    private fun rescheduleAlarm(context: Context) {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("alarm_enabled", true)

        if (enabled) {
            val hour = prefs.getInt("alarm_hour", 15)
            val minute = prefs.getInt("alarm_minute", 15)

            try {
                AlarmScheduler.scheduleDaily(context, hour, minute)
                Log.d(TAG, "重新设置定时提醒: $hour:$minute")
            } catch (e: Exception) {
                Log.e(TAG, "重新设置定时提醒失败", e)
            }
        }
    }
}
