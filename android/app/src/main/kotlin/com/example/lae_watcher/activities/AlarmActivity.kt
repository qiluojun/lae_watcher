package com.example.lae_watcher.activities

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.lae_watcher.R
import com.example.lae_watcher.utils.AlarmScheduler

/**
 * AlarmActivity - 全屏提醒界面
 *
 * 功能:
 * - 支持息屏唤醒（通过 FullScreenIntent 启动）
 * - 显示提醒信息
 * - 提供"我知道了"和"再等1min"按钮
 * - 振动和声音提醒
 *
 * 触发方式:
 * - TimeReceiver 通过 Notification + FullScreenIntent 启动
 *
 * Android 14/15 完全黑屏唤醒技术方案:
 * 1. AlarmManager.setAlarmClock() - 注册为系统闹钟
 * 2. PARTIAL_WAKE_LOCK - 保持 CPU 唤醒
 * 3. WindowManager 标志组合 - 唤醒屏幕并显示在锁屏上方
 * 4. 振动 + 声音 - 多感官提醒
 */
class AlarmActivity : Activity() {

    companion object {
        private const val TAG = "AlarmActivity"

        // Intent Extra 键名
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_MESSAGE = "alarm_message"
        const val EXTRA_ALARM_TYPE = "alarm_type"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "========================================")
        Log.i(TAG, "📱 AlarmActivity onCreate 被调用")
        Log.i(TAG, "线程: ${Thread.currentThread().name}")

        // 1. 先获取 WakeLock（确保 CPU 不休眠）
        acquireWakeLock()

        // 2. 设置窗口属性（唤醒屏幕、显示在锁屏上方）
        setupWindowFlags()

        setContentView(R.layout.activity_alarm)

        // 3. 触发振动和声音提醒
        triggerAlertFeedback()

        // 4. 初始化UI组件
        initViews()

        Log.i(TAG, "✓ AlarmActivity 初始化完成")
        Log.i(TAG, "========================================")
    }

    /**
     * 获取 WakeLock（Android 14/15 完全黑屏唤醒的关键）
     */
    private fun acquireWakeLock() {
        try {
            Log.d(TAG, "→ 尝试获取 PARTIAL_WAKE_LOCK...")
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            // 使用 PARTIAL_WAKE_LOCK（FULL_WAKE_LOCK 已废弃）
            // 配合 WindowManager 标志实现完全唤醒
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "LAEWatcher:AlarmActivityWakeLock"
            )

            // 持续 30 秒（确保用户有充足时间处理提醒）
            wakeLock?.acquire(30000L)
            Log.i(TAG, "  ✓ WakeLock 已获取 (持续30秒)")
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 获取 WakeLock 失败", e)
        }
    }

    /**
     * 设置窗口属性：息屏唤醒 + 锁屏显示
     *
     * Android 14/15 完全黑屏唤醒关键配置：
     * - FLAG_TURN_SCREEN_ON: 唤醒屏幕（需配合 WakeLock）
     * - FLAG_SHOW_WHEN_LOCKED: 锁屏上方显示
     * - FLAG_KEEP_SCREEN_ON: 保持屏幕常亮
     * - FLAG_DISMISS_KEYGUARD: 尝试解锁（不一定成功）
     */
    private fun setupWindowFlags() {
        Log.d(TAG, "→ 设置窗口属性...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Android 8.1+ 使用新的 API
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            Log.d(TAG, "  → 已调用 setShowWhenLocked(true) 和 setTurnScreenOn(true)")
        } else {
            // Android 8.0 及以下使用 WindowManager.LayoutParams
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
            Log.d(TAG, "  → 已添加窗口标志（旧版API）")
        }

        // 通用窗口标志（所有版本）
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        Log.d(TAG, "  → 已添加 FLAG_KEEP_SCREEN_ON")

        // 尝试解锁屏幕（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                keyguardManager.requestDismissKeyguard(this, null)
                Log.d(TAG, "  → 已请求解锁屏幕")
            } catch (e: Exception) {
                Log.w(TAG, "  ⚠ 请求解锁失败（可能需要用户密码）", e)
            }
        }
    }

    /**
     * 触发振动和声音提醒
     */
    private fun triggerAlertFeedback() {
        Log.d(TAG, "→ 触发振动和声音提醒...")

        // 1. 振动提醒
        triggerVibration()

        // 2. 声音提醒（播放默认闹钟铃声）
        playAlarmSound()
    }

    /**
     * 触发振动
     */
    private fun triggerVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 使用新的 VibratorManager
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                // Android 11 及以下使用旧的 API
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ 使用 VibrationEffect
                    // 振动模式: 等待0ms -> 振动500ms -> 停止200ms -> 振动500ms -> 停止200ms -> 振动500ms
                    val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                    val effect = VibrationEffect.createWaveform(pattern, -1) // -1 表示不重复
                    vibrator?.vibrate(effect)
                } else {
                    // Android 7.1 及以下使用旧的 API
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                }
                Log.i(TAG, "  ✓ 振动已触发")
            } else {
                Log.w(TAG, "  ⚠ 设备不支持振动")
            }
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 振动触发失败", e)
        }
    }

    /**
     * 播放闹钟声音
     */
    private fun playAlarmSound() {
        try {
            // 获取系统默认闹钟铃声
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 5.0+ 设置音频属性为闹钟类型
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.audioAttributes = audioAttributes
            }

            // 播放铃声（自动播放短时间后停止）
            ringtone.play()
            Log.i(TAG, "  ✓ 闹钟声音已播放")

            // 2秒后停止铃声（避免过长打扰）
            window.decorView.postDelayed({
                if (ringtone.isPlaying) {
                    ringtone.stop()
                    Log.d(TAG, "  → 闹钟声音已停止")
                }
            }, 2000L)
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 播放声音失败", e)
        }
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        val titleText = findViewById<TextView>(R.id.alarmTitle)
        val dismissButton = findViewById<Button>(R.id.dismissButton)
        val snoozeButton = findViewById<Button>(R.id.snoozeButton)

        // 读取自定义消息（如果有）
        val message = intent.getStringExtra(EXTRA_ALARM_MESSAGE) ?: "该休息了！"
        val alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: "daily"

        // 设置标题
        titleText.text = message

        // "我知道了" 按钮 - 关闭提醒
        dismissButton.setOnClickListener {
            Log.d(TAG, "用户点击：我知道了 (类型: $alarmType)")

            // 如果是行为监控提醒，通知服务重置计时器
            if (alarmType == "behavior") {
                resetBehaviorTracker()
            }

            finish()
        }

        // "再等1min" 按钮 - 延迟1分钟
        snoozeButton.setOnClickListener {
            Log.d(TAG, "用户点击：再等1min (类型: $alarmType)")
            snoozeAlarm()
            finish()
        }
    }

    /**
     * 重置行为监控计时器
     */
    private fun resetBehaviorTracker() {
        try {
            val serviceIntent = android.content.Intent(this, com.example.lae_watcher.services.BehaviorMonitorService::class.java).apply {
                action = com.example.lae_watcher.services.BehaviorMonitorService.ACTION_RESET_TRACKER
            }
            startService(serviceIntent)
            Log.i(TAG, "✓ 已发送重置计时器指令")
        } catch (e: Exception) {
            Log.e(TAG, "✗ 重置计时器失败", e)
        }
    }

    /**
     * 延迟提醒：1分钟后再次提醒
     */
    private fun snoozeAlarm() {
        try {
            AlarmScheduler.scheduleOnce(this, 1)
            Log.i(TAG, "已设置延迟提醒：1分钟后")
        } catch (e: Exception) {
            Log.e(TAG, "设置延迟提醒失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "→ AlarmActivity onDestroy 被调用")

        // 释放 WakeLock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "  ✓ WakeLock 已释放")
            }
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 释放 WakeLock 失败", e)
        }

        // 停止振动
        try {
            vibrator?.cancel()
            Log.d(TAG, "  ✓ 振动已停止")
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ 停止振动失败", e)
        }
    }
}
