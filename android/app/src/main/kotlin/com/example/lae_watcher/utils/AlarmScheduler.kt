package com.example.lae_watcher.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.lae_watcher.receivers.TimeReceiver
import com.example.lae_watcher.data.TimeAlarmItem
import java.util.Calendar

/**
 * AlarmScheduler - 定时提醒调度工具类（重构版 - 支持多提醒）
 *
 * 功能:
 * - 为多个 TimeAlarmItem 设置独立闹钟
 * - 每个提醒使用唯一的 REQUEST_CODE（基数 1000 + id.hashCode()）
 * - Intent Extra 传递 id 和 message
 * - 支持精确定时 (Android 12+ 需要 SCHEDULE_EXACT_ALARM 权限)
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE_BASE = 1000  // Type A 提醒的基数
    private const val REQUEST_CODE_TEST = 1999  // 测试提醒的固定 CODE

    /**
     * 为所有启用的提醒设置闹钟
     */
    fun scheduleAll(context: Context, items: List<TimeAlarmItem>) {
        Log.i(TAG, "========== 开始批量设置提醒 ==========")
        items.filter { it.enabled }.forEach { item ->
            schedule(context, item)
        }
        Log.i(TAG, "批量设置完成: 共 ${items.filter { it.enabled }.size} 个启用的提醒")
    }

    /**
     * 为单个提醒设置闹钟
     */
    fun schedule(context: Context, item: TimeAlarmItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 检查精确定时权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "缺少 SCHEDULE_EXACT_ALARM 权限，无法设置精确定时")
                return
            }
        }

        val intent = Intent(context, TimeReceiver::class.java).apply {
            action = TimeReceiver.ACTION_DAILY_ALARM
            putExtra("alarm_id", item.id)
            putExtra("alarm_message", item.message)
        }

        val requestCode = REQUEST_CODE_BASE + item.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算下次触发时间
        val triggerTime = calculateNextTriggerTime(item.hour, item.minute)

        // 使用 AlarmClockInfo 设置闹钟
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, com.example.lae_watcher.MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
                val idPrefix = if (item.id.length >= 8) item.id.substring(0, 8) else item.id
                val messagePreview = if (item.message.length > 10) item.message.substring(0, 10) + "..." else item.message
                Log.i(TAG, "⏰ 提醒已设置: [$idPrefix] ${item.hour}:${item.minute.toString().padStart(2, '0')} \"$messagePreview\" (REQUEST_CODE=$requestCode)")
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                val idPrefix = if (item.id.length >= 8) item.id.substring(0, 8) else item.id
                val messagePreview = if (item.message.length > 10) item.message.substring(0, 10) + "..." else item.message
                Log.i(TAG, "提醒已设置: [$idPrefix] ${item.hour}:${item.minute.toString().padStart(2, '0')} \"$messagePreview\"")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "设置提醒失败: 缺少权限", e)
        }
    }

    /**
     * 取消单个提醒
     */
    fun cancel(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimeReceiver::class.java).apply {
            action = TimeReceiver.ACTION_DAILY_ALARM
        }

        val requestCode = REQUEST_CODE_BASE + id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        val idPrefix = if (id.length >= 8) id.substring(0, 8) else id
        Log.i(TAG, "提醒已取消: ID=[$idPrefix] (REQUEST_CODE=$requestCode)")
    }

    /**
     * 取消所有提醒
     */
    fun cancelAll(context: Context, items: List<TimeAlarmItem>) {
        Log.i(TAG, "========== 开始批量取消提醒 ==========")
        items.forEach { item ->
            cancel(context, item.id)
        }
        Log.i(TAG, "批量取消完成: 共 ${items.size} 个提醒")
    }

    /**
     * 重新调度所有提醒（先取消全部，再设置启用的）
     * 这是最安全的方式，确保禁用的提醒不会残留在 AlarmManager 中
     */
    fun rescheduleAll(context: Context, allItems: List<TimeAlarmItem>) {
        Log.i(TAG, "========== 开始重新调度所有提醒 ==========")
        // 先取消所有提醒（包括启用和禁用的）
        cancelAll(context, allItems)
        // 只设置启用的提醒
        val enabledItems = allItems.filter { it.enabled }
        enabledItems.forEach { item ->
            schedule(context, item)
        }
        Log.i(TAG, "重新调度完成: 取消 ${allItems.size} 个，设置 ${enabledItems.size} 个启用的提醒")
    }

    // ========== 以下是旧版方法，保留用于兼容 ==========
    private const val REQUEST_CODE = 1001  // 旧版单个提醒的 REQUEST_CODE
    private const val REQUEST_CODE_ONCE = 1002  // 一次性提醒的 REQUEST_CODE

    /**
     * 设置每日定时提醒（旧版，保留用于兼容）
     *
     * @param context 上下文
     * @param hour 小时 (0-23)
     * @param minute 分钟 (0-59)
     */
    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 检查精确定时权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "缺少 SCHEDULE_EXACT_ALARM 权限，无法设置精确定时")
                // TODO: Step 5 Flutter UI 时引导用户授权
                return
            }
        }

        val intent = Intent(context, TimeReceiver::class.java).apply {
            action = TimeReceiver.ACTION_DAILY_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算下次触发时间
        val triggerTime = calculateNextTriggerTime(hour, minute)

        // 使用 AlarmClockInfo 设置闹钟（让系统识别为真正的闹钟，允许息屏唤醒）
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 创建一个用于显示的 PendingIntent（点击状态栏图标时打开的界面）
                val showIntent = Intent(context, com.example.lae_watcher.MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 使用 AlarmClockInfo 设置闹钟
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
                Log.i(TAG, "⏰ 定时提醒已设置（闹钟模式）: ${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} $hour:${minute.toString().padStart(2, '0')}")
            } else {
                // Android 5.0 以下使用普通方式
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )

                val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
                Log.i(TAG, "定时提醒已设置: ${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} $hour:${minute.toString().padStart(2, '0')}")
            }

            // 保存设置到 SharedPreferences
            saveAlarmSettings(context, hour, minute, true)

        } catch (e: SecurityException) {
            Log.e(TAG, "设置定时提醒失败: 缺少权限", e)
        }
    }

    /**
     * 取消定时提醒
     */
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TimeReceiver::class.java).apply {
            action = TimeReceiver.ACTION_DAILY_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "定时提醒已取消")

        // 更新设置
        saveAlarmSettings(context, 15, 15, false)
    }

    /**
     * 计算下次触发时间
     * 规则: 如果今天的设定时间已过，则设置为明天同一时间
     */
    private fun calculateNextTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 如果设定时间已过，延后到明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * 保存定时提醒设置到 SharedPreferences
     */
    private fun saveAlarmSettings(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("alarm_hour", hour)
            putInt("alarm_minute", minute)
            putBoolean("alarm_enabled", enabled)
            apply()
        }
    }

    /**
     * 读取定时提醒设置
     */
    fun getAlarmSettings(context: Context): Triple<Int, Int, Boolean> {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("alarm_hour", 15)
        val minute = prefs.getInt("alarm_minute", 15)
        val enabled = prefs.getBoolean("alarm_enabled", true)
        return Triple(hour, minute, enabled)
    }

    /**
     * 设置一次性延迟提醒
     *
     * @param context 上下文
     * @param delayMinutes 延迟时间（分钟）
     */
    fun scheduleOnce(context: Context, delayMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 检查精确定时权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "缺少 SCHEDULE_EXACT_ALARM 权限，无法设置精确定时")
                return
            }
        }

        val intent = Intent(context, TimeReceiver::class.java).apply {
            action = TimeReceiver.ACTION_DAILY_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ONCE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算触发时间：当前时间 + 延迟分钟数
        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        // 使用 AlarmClockInfo 设置闹钟（让系统识别为真正的闹钟）
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 创建一个用于显示的 PendingIntent
                val showIntent = Intent(context, com.example.lae_watcher.MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 使用 AlarmClockInfo 设置闹钟
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
                Log.i(TAG, "⏰ 一次性提醒已设置（闹钟模式）: ${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE).toString().padStart(2, '0')} (延迟 $delayMinutes 分钟)")
            } else {
                // Android 5.0 以下使用普通方式
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )

                val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
                Log.i(TAG, "一次性提醒已设置: ${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} ${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE).toString().padStart(2, '0')} (延迟 $delayMinutes 分钟)")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "设置一次性提醒失败: 缺少权限", e)
        }
    }

    /**
     * 设置快速测试提醒（秒级延迟）
     *
     * @param context 上下文
     * @param delaySeconds 延迟时间（秒）
     * @param message 自定义提示语（可选）
     */
    fun scheduleTest(context: Context, delaySeconds: Int, message: String = "测试提醒") {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 检查精确定时权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "缺少 SCHEDULE_EXACT_ALARM 权限，无法设置精确定时")
                return
            }
        }

        val intent = Intent(context, TimeReceiver::class.java).apply {
            action = TimeReceiver.ACTION_DAILY_ALARM
            putExtra("alarm_id", "test")
            putExtra("alarm_message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算触发时间：当前时间 + 延迟秒数
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

        // 使用 AlarmClockInfo 设置闹钟（让系统识别为真正的闹钟）
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 创建一个用于显示的 PendingIntent（点击状态栏图标时打开的界面）
                val showIntent = Intent(context, com.example.lae_watcher.MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 使用 AlarmClockInfo 设置闹钟
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                Log.i(TAG, "⏰ [测试提醒] 已设置为闹钟模式，${delaySeconds}秒后触发")
            } else {
                // Android 5.0 以下使用普通方式
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.i(TAG, "⏰ [测试提醒] 已设置 ${delaySeconds}秒 后触发")
            }

            val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
            Log.i(TAG, "触发时间: ${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE).toString().padStart(2, '0')}:${calendar.get(Calendar.SECOND).toString().padStart(2, '0')}")

        } catch (e: SecurityException) {
            Log.e(TAG, "设置测试提醒失败: 缺少权限", e)
        }
    }
}
