package com.example.lae_watcher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 屏幕状态监听回调接口
 */
interface ScreenStateListener {
    fun onScreenTurnedOn()
    fun onScreenTurnedOff()
}

/**
 * 屏幕状态广播接收器
 *
 * 功能:
 * - 监听 ACTION_SCREEN_ON 和 ACTION_SCREEN_OFF 事件
 * - 通知 BehaviorMonitorService 更新统计
 *
 * 注意:
 * - 此 Receiver 仅在监控时段内由 BehaviorMonitorService 动态注册
 * - 不在 AndroidManifest.xml 中静态注册
 */
class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }

    // 回调监听器（由 BehaviorMonitorService 设置）
    var listener: ScreenStateListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.i(TAG, "📱 接收到屏幕亮起广播")
                listener?.onScreenTurnedOn()
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.i(TAG, "📱 接收到屏幕熄灭广播")
                listener?.onScreenTurnedOff()
            }
            else -> {
                Log.w(TAG, "收到未知广播: ${intent.action}")
            }
        }
    }
}
