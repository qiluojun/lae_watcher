import 'package:flutter/services.dart';

/// 时段范围数据类
class TimeRange {
  final int startHour;
  final int startMinute;
  final int endHour;
  final int endMinute;

  TimeRange({
    required this.startHour,
    required this.startMinute,
    required this.endHour,
    required this.endMinute,
  });

  /// 从 Map 创建
  factory TimeRange.fromMap(Map<String, dynamic> map) {
    return TimeRange(
      startHour: map['startHour'] as int,
      startMinute: map['startMinute'] as int,
      endHour: map['endHour'] as int,
      endMinute: map['endMinute'] as int,
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'startHour': startHour,
      'startMinute': startMinute,
      'endHour': endHour,
      'endMinute': endMinute,
    };
  }

  /// 格式化为字符串 (如 "21:30 - 08:00")
  @override
  String toString() {
    return '${_formatTime(startHour, startMinute)} - ${_formatTime(endHour, endMinute)}';
  }

  String _formatTime(int hour, int minute) {
    return '${hour.toString().padLeft(2, '0')}:${minute.toString().padLeft(2, '0')}';
  }
}

/// 行为监控提醒配置
class BehaviorAlarmConfig {
  final bool enabled;
  final List<TimeRange> timeRanges;
  final int screenTimeThreshold; // 秒

  BehaviorAlarmConfig({
    required this.enabled,
    required this.timeRanges,
    required this.screenTimeThreshold,
  });

  /// 默认配置
  factory BehaviorAlarmConfig.defaultConfig() {
    return BehaviorAlarmConfig(
      enabled: false,
      timeRanges: [TimeRange(startHour: 21, startMinute: 30, endHour: 8, endMinute: 0)],
      screenTimeThreshold: 30,
    );
  }

  /// 从 Map 创建
  factory BehaviorAlarmConfig.fromMap(Map<String, dynamic> map) {
    final rangesList = map['timeRanges'] as List<dynamic>;
    final timeRanges = rangesList
        .map((range) => TimeRange.fromMap(Map<String, dynamic>.from(range)))
        .toList();

    return BehaviorAlarmConfig(
      enabled: map['enabled'] as bool,
      timeRanges: timeRanges,
      screenTimeThreshold: map['screenTimeThreshold'] as int,
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'enabled': enabled,
      'timeRanges': timeRanges.map((range) => range.toMap()).toList(),
      'screenTimeThreshold': screenTimeThreshold,
    };
  }

  /// 复制并修改部分字段
  BehaviorAlarmConfig copyWith({
    bool? enabled,
    List<TimeRange>? timeRanges,
    int? screenTimeThreshold,
  }) {
    return BehaviorAlarmConfig(
      enabled: enabled ?? this.enabled,
      timeRanges: timeRanges ?? this.timeRanges,
      screenTimeThreshold: screenTimeThreshold ?? this.screenTimeThreshold,
    );
  }
}

/// NativeBridge - Flutter 与 Android 原生通信桥梁
///
/// 功能:
/// - 设置定时提醒时间
/// - 获取当前设置
/// - 取消定时提醒
/// - 行为监控配置管理
class NativeBridge {
  static const MethodChannel _channel = MethodChannel('com.example.lae_watcher/alarm');

  /// 设置定时提醒
  ///
  /// [hour] 小时 (0-23)
  /// [minute] 分钟 (0-59)
  static Future<bool> setAlarm(int hour, int minute) async {
    try {
      final result = await _channel.invokeMethod('setAlarm', {
        'hour': hour,
        'minute': minute,
      });
      return result == true;
    } catch (e) {
      print('设置定时提醒失败: $e');
      return false;
    }
  }

  /// 取消定时提醒
  static Future<bool> cancelAlarm() async {
    try {
      final result = await _channel.invokeMethod('cancelAlarm');
      return result == true;
    } catch (e) {
      print('取消定时提醒失败: $e');
      return false;
    }
  }

  /// 获取当前定时设置
  ///
  /// 返回: {'hour': int, 'minute': int, 'enabled': bool}
  static Future<Map<String, dynamic>?> getAlarmSettings() async {
    try {
      final result = await _channel.invokeMethod('getAlarmSettings');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      print('获取定时设置失败: $e');
      return null;
    }
  }

  /// 设置快速测试提醒（秒级延迟）
  ///
  /// [seconds] 延迟秒数 (建议 5-15 秒)
  static Future<bool> setTestAlarm(int seconds) async {
    try {
      final result = await _channel.invokeMethod('setTestAlarm', {
        'seconds': seconds,
      });
      return result == true;
    } catch (e) {
      print('设置测试提醒失败: $e');
      return false;
    }
  }

  // ========== 行为监控相关接口 ==========

  /// 获取行为监控配置
  static Future<BehaviorAlarmConfig?> getBehaviorAlarmConfig() async {
    try {
      final result = await _channel.invokeMethod('getBehaviorAlarmConfig');
      final map = Map<String, dynamic>.from(result);
      return BehaviorAlarmConfig.fromMap(map);
    } catch (e) {
      print('获取行为监控配置失败: $e');
      return null;
    }
  }

  /// 设置行为监控配置
  static Future<bool> setBehaviorAlarmConfig(BehaviorAlarmConfig config) async {
    try {
      final result = await _channel.invokeMethod('setBehaviorAlarmConfig', config.toMap());
      return result == true;
    } catch (e) {
      print('设置行为监控配置失败: $e');
      return false;
    }
  }

  /// 设置行为监控启用状态
  static Future<bool> setBehaviorAlarmEnabled(bool enabled) async {
    try {
      final result = await _channel.invokeMethod('setBehaviorAlarmEnabled', {
        'enabled': enabled,
      });
      return result == true;
    } catch (e) {
      print('设置行为监控启用状态失败: $e');
      return false;
    }
  }

  /// 设置亮屏时长阈值
  ///
  /// [threshold] 阈值（秒），范围: 10-600
  static Future<bool> setBehaviorAlarmThreshold(int threshold) async {
    try {
      final result = await _channel.invokeMethod('setBehaviorAlarmThreshold', {
        'threshold': threshold,
      });
      return result == true;
    } catch (e) {
      print('设置亮屏时长阈值失败: $e');
      return false;
    }
  }
}
