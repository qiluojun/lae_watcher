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
import com.example.lae_watcher.utils.TimeAlarmManager
import com.example.lae_watcher.utils.BehaviorAlarmManager
import com.example.lae_watcher.data.TimeAlarmItem
import com.example.lae_watcher.data.BehaviorAlarmItem
import com.example.lae_watcher.utils.RecordConfigManager
import com.example.lae_watcher.utils.RecordAnswerManager
import com.example.lae_watcher.data.RecordConfig
import com.example.lae_watcher.data.RecordAnswer

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
                "setBehaviorAlarmMessage" -> {
                    val message = call.argument<String>("message") ?: "亮屏时间过长！"
                    BehaviorAlarmConfigManager.setMessage(this, message)
                    Log.i(TAG, "Flutter 调用: setBehaviorAlarmMessage($message)")

                    // 如果监控已启用，重启服务以应用新提示语
                    val config = BehaviorAlarmConfigManager.loadConfig(this)
                    if (config.enabled) {
                        BehaviorAlarmScheduler.start(this, config)
                    }

                    result.success(true)
                }
                // ========== Type A 多提醒管理接口 ==========
                "getTimeAlarms" -> {
                    val manager = TimeAlarmManager(this)
                    val alarms = manager.getAll()
                    Log.i(TAG, "Flutter 调用: getTimeAlarms -> ${alarms.size} 个提醒")
                    result.success(alarms.map { it.toMap() })
                }
                "addTimeAlarm" -> {
                    @Suppress("UNCHECKED_CAST")
                    val alarmMap = call.arguments as? Map<String, Any?>
                    if (alarmMap != null) {
                        val item = TimeAlarmItem.fromMap(alarmMap)
                        val manager = TimeAlarmManager(this)
                        val success = manager.add(item)
                        Log.i(TAG, "Flutter 调用: addTimeAlarm -> ${item.toString()}, success=$success")

                        // 重新调度所有提醒（先取消全部，再设置启用的）
                        if (success) {
                            val allAlarms = manager.getAll()
                            AlarmScheduler.rescheduleAll(this, allAlarms)
                        }

                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "提醒参数无效", null)
                    }
                }
                "updateTimeAlarm" -> {
                    @Suppress("UNCHECKED_CAST")
                    val alarmMap = call.arguments as? Map<String, Any?>
                    if (alarmMap != null) {
                        val item = TimeAlarmItem.fromMap(alarmMap)
                        val manager = TimeAlarmManager(this)
                        val success = manager.update(item)
                        Log.i(TAG, "Flutter 调用: updateTimeAlarm -> ${item.toString()}, success=$success")

                        // 重新调度所有提醒（先取消全部，再设置启用的）
                        if (success) {
                            val allAlarms = manager.getAll()
                            AlarmScheduler.rescheduleAll(this, allAlarms)
                        }

                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "提醒参数无效", null)
                    }
                }
                "deleteTimeAlarm" -> {
                    val id = call.argument<String>("id")
                    if (id != null) {
                        val manager = TimeAlarmManager(this)
                        val success = manager.delete(id)
                        Log.i(TAG, "Flutter 调用: deleteTimeAlarm -> id=$id, success=$success")

                        // 取消该提醒的闹钟
                        if (success) {
                            AlarmScheduler.cancel(this, id)
                            Log.i(TAG, "已取消提醒 $id 的闹钟")
                        }

                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "ID 参数无效", null)
                    }
                }
                "toggleTimeAlarm" -> {
                    val id = call.argument<String>("id")
                    if (id != null) {
                        val manager = TimeAlarmManager(this)
                        val newState = manager.toggle(id)
                        Log.i(TAG, "Flutter 调用: toggleTimeAlarm -> id=$id, enabled=$newState")

                        // 重新调度所有提醒（先取消全部，再设置启用的）
                        if (newState != null) {
                            val allAlarms = manager.getAll()
                            AlarmScheduler.rescheduleAll(this, allAlarms)
                        }

                        result.success(newState)
                    } else {
                        result.error("INVALID_ARGUMENTS", "ID 参数无效", null)
                    }
                }
                // ========== Type B 多提醒管理接口 ==========
                "getBehaviorAlarms" -> {
                    val manager = BehaviorAlarmManager(this)
                    val alarms = manager.getAll()
                    Log.i(TAG, "Flutter 调用: getBehaviorAlarms -> ${alarms.size} 个配置")
                    result.success(alarms.map { it.toMap() })
                }
                "addBehaviorAlarm" -> {
                    @Suppress("UNCHECKED_CAST")
                    val alarmMap = call.arguments as? Map<String, Any>
                    if (alarmMap != null) {
                        val item = BehaviorAlarmItem.fromMap(alarmMap)
                        val manager = BehaviorAlarmManager(this)
                        val success = manager.add(item)
                        Log.i(TAG, "Flutter 调用: addBehaviorAlarm -> ${item.toString()}, success=$success")
                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "监控配置参数无效", null)
                    }
                }
                "updateBehaviorAlarm" -> {
                    @Suppress("UNCHECKED_CAST")
                    val alarmMap = call.arguments as? Map<String, Any>
                    if (alarmMap != null) {
                        val item = BehaviorAlarmItem.fromMap(alarmMap)
                        val manager = BehaviorAlarmManager(this)
                        val success = manager.update(item)
                        Log.i(TAG, "Flutter 调用: updateBehaviorAlarm -> ${item.toString()}, success=$success")
                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "监控配置参数无效", null)
                    }
                }
                "deleteBehaviorAlarm" -> {
                    val id = call.argument<String>("id")
                    if (id != null) {
                        val manager = BehaviorAlarmManager(this)
                        val success = manager.delete(id)
                        Log.i(TAG, "Flutter 调用: deleteBehaviorAlarm -> id=$id, success=$success")
                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "ID 参数无效", null)
                    }
                }
                "toggleBehaviorAlarm" -> {
                    val id = call.argument<String>("id")
                    if (id != null) {
                        val manager = BehaviorAlarmManager(this)
                        val newState = manager.toggle(id)
                        Log.i(TAG, "Flutter 调用: toggleBehaviorAlarm -> id=$id, enabled=$newState")
                        result.success(newState)
                    } else {
                        result.error("INVALID_ARGUMENTS", "ID 参数无效", null)
                    }
                }
                // ========== Type C 记录系统接口 ==========
                "getRecordConfigs" -> {
                    val manager = RecordConfigManager(this)
                    val configs = manager.getAll()
                    Log.i(TAG, "Flutter 调用: getRecordConfigs -> ${configs.size} 个记录配置")
                    result.success(configs.map { it.toMap() })
                }
                "saveRecordConfig" -> {
                    @Suppress("UNCHECKED_CAST")
                    val configMap = call.arguments as? Map<String, Any?>
                    if (configMap != null) {
                        val config = RecordConfig.fromMap(configMap)
                        val manager = RecordConfigManager(this)

                        // 检查是否已存在 (通过 UUID 判断)
                        val existing = manager.getByUuid(config.uuid)
                        val success = if (existing != null) {
                            manager.update(config)
                        } else {
                            manager.add(config)
                        }

                        Log.i(TAG, "Flutter 调用: saveRecordConfig -> ${config.toString()}, success=$success")
                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "记录配置参数无效", null)
                    }
                }
                "deleteRecordConfig" -> {
                    val uuid = call.argument<String>("uuid")
                    if (uuid != null) {
                        val configManager = RecordConfigManager(this)
                        val answerManager = RecordAnswerManager(this)

                        // 级联删除：先删除关联的答案记录
                        val deletedAnswers = answerManager.deleteByRecordId(uuid)
                        Log.i(TAG, "级联删除答案记录: $deletedAnswers 条")

                        // 再删除配置本身
                        val success = configManager.delete(uuid)
                        Log.i(TAG, "Flutter 调用: deleteRecordConfig -> uuid=$uuid, success=$success")
                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "UUID 参数无效", null)
                    }
                }
                "getRecordAnswers" -> {
                    val manager = RecordAnswerManager(this)
                    val recordId = call.argument<String>("recordId")

                    val answers = if (recordId != null) {
                        // 获取指定记录的答案
                        Log.i(TAG, "Flutter 调用: getRecordAnswers -> recordId=$recordId")
                        manager.getByRecordId(recordId)
                    } else {
                        // 获取所有答案
                        Log.i(TAG, "Flutter 调用: getRecordAnswers -> 获取所有答案")
                        manager.getAll()
                    }

                    result.success(answers.map { it.toMap() })
                }
                "getRecordAnswersByTimeRange" -> {
                    val startTimestamp = call.argument<Long>("startTimestamp")
                    val endTimestamp = call.argument<Long>("endTimestamp")

                    if (startTimestamp != null && endTimestamp != null) {
                        val manager = RecordAnswerManager(this)
                        val answers = manager.getByTimeRange(startTimestamp, endTimestamp)
                        Log.i(TAG, "Flutter 调用: getRecordAnswersByTimeRange -> ${answers.size} 条记录")
                        result.success(answers.map { it.toMap() })
                    } else {
                        result.error("INVALID_ARGUMENTS", "时间范围参数无效", null)
                    }
                }
                "exportRecordAnswers" -> {
                    val manager = RecordAnswerManager(this)
                    val jsonString = manager.exportToJson()
                    Log.i(TAG, "Flutter 调用: exportRecordAnswers -> 导出完成")
                    result.success(jsonString)
                }
                "deleteRecordAnswer" -> {
                    val timestamp = call.argument<Long>("timestamp")
                    if (timestamp != null) {
                        val manager = RecordAnswerManager(this)
                        val success = manager.deleteByTimestamp(timestamp)
                        Log.i(TAG, "Flutter 调用: deleteRecordAnswer -> timestamp=$timestamp, success=$success")
                        result.success(success)
                    } else {
                        result.error("INVALID_ARGUMENTS", "时间戳参数无效", null)
                    }
                }
                // ========== 测试接口 ==========
                "testMultipleAlarms" -> {
                    Log.i(TAG, "Flutter 调用: testMultipleAlarms")
                    testMultipleAlarms()
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

    /**
     * 测试多提醒功能
     */
    private fun testMultipleAlarms() {
        Log.i(TAG, "========== 开始测试多提醒功能 ==========")

        val manager = TimeAlarmManager(this)

        // 清空旧数据
        manager.clear()
        Log.i(TAG, "已清空旧数据")

        // 添加 3 个测试提醒
        val now = java.util.Calendar.getInstance()
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)

        // 提醒1：当前时间 + 1 分钟
        val item1 = TimeAlarmItem.create(
            hour = currentHour,
            minute = (currentMinute + 1) % 60,
            message = "测试提醒1（1分钟后）",
            enabled = true
        )

        // 提醒2：当前时间 + 2 分钟
        val item2 = TimeAlarmItem.create(
            hour = currentHour,
            minute = (currentMinute + 2) % 60,
            message = "测试提醒2（2分钟后）",
            enabled = true
        )

        // 提醒3：禁用状态
        val item3 = TimeAlarmItem.create(
            hour = 21,
            minute = 30,
            message = "测试提醒3（禁用）",
            enabled = false
        )

        manager.add(item1)
        manager.add(item2)
        manager.add(item3)

        Log.i(TAG, "已添加 3 个测试提醒")

        // 获取所有提醒
        val alarms = manager.getAll()
        Log.i(TAG, "当前提醒数量: ${alarms.size}")
        alarms.forEach { alarm ->
            Log.i(TAG, "  - ${alarm.hour}:${alarm.minute.toString().padStart(2, '0')} \"${alarm.message}\" [${if (alarm.enabled) "启用" else "禁用"}]")
        }

        // 调度所有启用的提醒
        AlarmScheduler.scheduleAll(this, alarms)

        Log.i(TAG, "========== 测试多提醒功能完成 ==========")
    }
}
