package com.example.lae_watcher.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lae_watcher.MainActivity
import com.example.lae_watcher.R
import com.example.lae_watcher.receivers.ScreenStateReceiver
import com.example.lae_watcher.receivers.ScreenStateListener
import com.example.lae_watcher.utils.ScreenTimeTracker

/**
 * 行为监控前台服务
 *
 * 功能:
 * - 在监控时段内运行（如 21:30 - 08:00）
 * - 动态注册 ScreenStateReceiver 监听屏幕开关
 * - 使用 ScreenTimeTracker 统计亮屏时长
 * - 定时检查（每秒 tick）
 * - 显示前台通知（符合 Android 要求）
 *
 * 生命周期:
 * - 由 AlarmManager 在监控时段开始时启动
 * - 由 AlarmManager 在监控时段结束时停止
 */
class BehaviorMonitorService : Service(), ScreenStateListener {

    companion object {
        private const val TAG = "BehaviorMonitorService"
        private const val NOTIFICATION_CHANNEL_ID = "behavior_monitor_channel"
        private const val NOTIFICATION_ID = 2001

        // Intent Extra 键名
        const val EXTRA_THRESHOLD_SECONDS = "threshold_seconds"

        // Service 控制 Action
        const val ACTION_STOP_MONITORING = "com.example.lae_watcher.STOP_MONITORING"
        const val ACTION_RESET_TRACKER = "com.example.lae_watcher.RESET_TRACKER"
    }

    // 屏幕状态接收器
    private var screenStateReceiver: ScreenStateReceiver? = null

    // 时长统计器
    private var screenTimeTracker: ScreenTimeTracker? = null

    // 定时器 Handler（每秒 tick）
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // 定时任务
    private val tickRunnable = object : Runnable {
        override fun run() {
            screenTimeTracker?.tick()
            if (isRunning) {
                handler.postDelayed(this, 1000) // 每 1 秒执行一次
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "📱 BehaviorMonitorService onCreate")

        // 创建通知渠道
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "📱 BehaviorMonitorService onStartCommand")

        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                Log.i(TAG, "收到停止监控指令")
                stopMonitoring()
                return START_NOT_STICKY
            }
            ACTION_RESET_TRACKER -> {
                Log.i(TAG, "收到重置计时器指令")
                screenTimeTracker?.reset()
                return START_STICKY
            }
            else -> {
                // 正常启动：开始监控
                val thresholdSeconds = intent?.getIntExtra(EXTRA_THRESHOLD_SECONDS, 30) ?: 30
                startMonitoring(thresholdSeconds)
                return START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "📱 BehaviorMonitorService onDestroy")
        stopMonitoring()
    }

    /**
     * 开始监控
     */
    private fun startMonitoring(thresholdSeconds: Int) {
        if (isRunning) {
            Log.w(TAG, "监控已在运行中")
            return
        }

        Log.i(TAG, "🚀 开始监控，阈值: ${thresholdSeconds}秒")

        // 1. 启动前台服务（显示通知）
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 2. 创建统计器
        screenTimeTracker = ScreenTimeTracker(this, thresholdSeconds)

        // 3. 注册屏幕状态接收器
        registerScreenStateReceiver()

        // 4. 检查当前屏幕状态（如果已经亮屏，通知 tracker）
        checkInitialScreenState()

        // 5. 启动定时器
        isRunning = true
        handler.post(tickRunnable)

        Log.i(TAG, "✓ 监控已启动")
    }

    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        if (!isRunning) {
            Log.w(TAG, "监控未在运行")
            return
        }

        Log.i(TAG, "🛑 停止监控")

        // 1. 停止定时器
        isRunning = false
        handler.removeCallbacks(tickRunnable)

        // 2. 注销屏幕状态接收器
        unregisterScreenStateReceiver()

        // 3. 清理统计器
        screenTimeTracker = null

        // 4. 停止前台服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.i(TAG, "✓ 监控已停止")
    }

    /**
     * 注册屏幕状态接收器
     */
    private fun registerScreenStateReceiver() {
        if (screenStateReceiver != null) {
            Log.w(TAG, "ScreenStateReceiver 已注册")
            return
        }

        screenStateReceiver = ScreenStateReceiver().apply {
            listener = this@BehaviorMonitorService
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        registerReceiver(screenStateReceiver, filter)
        Log.i(TAG, "✓ ScreenStateReceiver 已注册")
    }

    /**
     * 注销屏幕状态接收器
     */
    private fun unregisterScreenStateReceiver() {
        screenStateReceiver?.let {
            try {
                unregisterReceiver(it)
                screenStateReceiver = null
                Log.i(TAG, "✓ ScreenStateReceiver 已注销")
            } catch (e: Exception) {
                Log.e(TAG, "注销 ScreenStateReceiver 失败", e)
            }
        }
    }

    /**
     * 检查初始屏幕状态
     * 如果服务启动时屏幕已经亮着，通知 ScreenTimeTracker
     */
    private fun checkInitialScreenState() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            // isInteractive 在 API 20+ 可用
            val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }

            Log.d(TAG, "→ 检查初始屏幕状态: ${if (isScreenOn) "亮屏" else "息屏"}")

            if (isScreenOn) {
                // 屏幕已经亮着，通知 tracker 开始计时
                screenTimeTracker?.onScreenOn()
                Log.i(TAG, "✓ 初始状态为亮屏，已开始计时")
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查初始屏幕状态失败", e)
        }
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "行为监控服务",
                NotificationManager.IMPORTANCE_LOW // 低优先级，不打扰用户
            ).apply {
                description = "监控亮屏时长，超过阈值时提醒"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.i(TAG, "✓ 通知渠道已创建")
        }
    }

    /**
     * 构建前台服务通知
     */
    private fun buildNotification(): Notification {
        // 点击通知打开主界面
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("行为监控运行中")
            .setContentText("正在监控亮屏时长...")
            .setSmallIcon(android.R.drawable.ic_menu_view) // 使用系统图标
            .setOngoing(true) // 不可清除
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级
            .build()
    }

    // ========== ScreenStateListener 接口实现 ==========

    override fun onScreenTurnedOn() {
        Log.d(TAG, "→ 回调: 屏幕亮起")
        screenTimeTracker?.onScreenOn()
    }

    override fun onScreenTurnedOff() {
        Log.d(TAG, "→ 回调: 屏幕熄灭")
        screenTimeTracker?.onScreenOff()
    }
}
