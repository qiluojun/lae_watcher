package com.example.lae_watcher

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.lae_watcher.utils.AlarmScheduler
import com.example.lae_watcher.utils.BehaviorAlarmConfigManager
import com.example.lae_watcher.utils.BehaviorAlarmScheduler
import com.example.lae_watcher.data.BehaviorAlarmConfig

/**
 * MainActivity - Flutter 宿主 Activity
 *
 * 功能:
 * - 启动时自动设置定时提醒 (15:15)
 * - MethodChannel 通信桥梁
 */
class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.example.lae_watcher/alarm"
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用启动时自动设置定时提醒
        initializeAlarm()

        // 请求必要的权限
        requestRequiredPermissions()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 配置 MethodChannel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "setAlarm" -> {
                    val hour = call.argument<Int>("hour") ?: 15
                    val minute = call.argument<Int>("minute") ?: 15
                    Log.i(TAG, "Flutter 调用: setAlarm($hour, $minute)")

                    AlarmScheduler.scheduleDaily(this, hour, minute)
                    result.success(true)
                }
                "cancelAlarm" -> {
                    Log.i(TAG, "Flutter 调用: cancelAlarm")
                    AlarmScheduler.cancelAlarm(this)
                    result.success(true)
                }
                "getAlarmSettings" -> {
                    val (hour, minute, enabled) = AlarmScheduler.getAlarmSettings(this)
                    Log.i(TAG, "Flutter 调用: getAlarmSettings -> $hour:$minute, enabled=$enabled")

                    result.success(mapOf(
                        "hour" to hour,
                        "minute" to minute,
                        "enabled" to enabled
                    ))
                }
                "setTestAlarm" -> {
                    val seconds = call.argument<Int>("seconds") ?: 10
                    Log.i(TAG, "Flutter 调用: setTestAlarm($seconds 秒)")

                    AlarmScheduler.scheduleTest(this, seconds)
                    result.success(true)
                }
                // ========== 行为监控相关接口 ==========
                "getBehaviorAlarmConfig" -> {
                    val config = BehaviorAlarmConfigManager.loadConfig(this)
                    Log.i(TAG, "Flutter 调用: getBehaviorAlarmConfig -> enabled=${config.enabled}, threshold=${config.screenTimeThreshold}秒")
                    result.success(config.toMap())
                }
                "setBehaviorAlarmConfig" -> {
                    @Suppress("UNCHECKED_CAST")
                    val configMap = call.arguments as? Map<String, Any>
                    if (configMap != null) {
                        val config = BehaviorAlarmConfig.fromMap(configMap)
                        BehaviorAlarmConfigManager.saveConfig(this, config)
                        Log.i(TAG, "Flutter 调用: setBehaviorAlarmConfig -> enabled=${config.enabled}, threshold=${config.screenTimeThreshold}秒")

                        // 根据配置启动/停止监控服务
                        if (config.enabled) {
                            BehaviorAlarmScheduler.start(this, config)
                        } else {
                            BehaviorAlarmScheduler.stop(this)
                        }

                        result.success(true)
                    } else {
                        result.error("INVALID_ARGUMENTS", "配置参数无效", null)
                    }
                }
                "setBehaviorAlarmEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    BehaviorAlarmConfigManager.setEnabled(this, enabled)
                    Log.i(TAG, "Flutter 调用: setBehaviorAlarmEnabled($enabled)")

                    // 启动/停止监控服务
                    if (enabled) {
                        val config = BehaviorAlarmConfigManager.loadConfig(this)
                        BehaviorAlarmScheduler.start(this, config)
                    } else {
                        BehaviorAlarmScheduler.stop(this)
                    }

                    result.success(true)
                }
                "setBehaviorAlarmThreshold" -> {
                    val threshold = call.argument<Int>("threshold") ?: 30
                    BehaviorAlarmConfigManager.setThreshold(this, threshold)
                    Log.i(TAG, "Flutter 调用: setBehaviorAlarmThreshold($threshold 秒)")

                    // 如果监控已启用，重启服务以应用新阈值
                    val config = BehaviorAlarmConfigManager.loadConfig(this)
                    if (config.enabled) {
                        BehaviorAlarmScheduler.start(this, config)
                    }

                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    /**
     * 初始化定时提醒
     * 从 SharedPreferences 读取设置，如果启用则设置定时
     */
    private fun initializeAlarm() {
        val (hour, minute, enabled) = AlarmScheduler.getAlarmSettings(this)
        Log.i(TAG, "应用启动: 初始化定时提醒 -> $hour:$minute, enabled=$enabled")

        if (enabled) {
            AlarmScheduler.scheduleDaily(this, hour, minute)
        }
    }

    /**
     * 请求必要的权限
     */
    private fun requestRequiredPermissions() {
        // 1. 请求通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                Log.i(TAG, "请求通知权限...")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            } else {
                Log.i(TAG, "通知权限已授权")
            }
        }

        // 2. 检查全屏通知权限 (Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val canUseFullScreenIntent = notificationManager.canUseFullScreenIntent()

            if (!canUseFullScreenIntent) {
                Log.w(TAG, "全屏通知权限未授权，需要引导用户手动开启")
                // 引导用户到设置页面
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    Log.i(TAG, "正在打开全屏通知权限设置页面...")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "无法打开全屏通知权限设置页面", e)
                }
            } else {
                Log.i(TAG, "全屏通知权限已授权")
            }
        }
    }
}
