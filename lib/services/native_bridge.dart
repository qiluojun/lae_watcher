import 'package:flutter/services.dart';

/// Type A - 定时提醒数据类
class TimeAlarmItem {
  final String id;
  final int hour;
  final int minute;
  final bool enabled;
  final String message;
  final List<int>? repeatDays; // null = 每日重复

  TimeAlarmItem({
    required this.id,
    required this.hour,
    required this.minute,
    required this.enabled,
    required this.message,
    this.repeatDays,
  });

  /// 从 Map 创建
  factory TimeAlarmItem.fromMap(Map<String, dynamic> map) {
    return TimeAlarmItem(
      id: map['id'] as String,
      hour: map['hour'] as int,
      minute: map['minute'] as int,
      enabled: map['enabled'] as bool,
      message: map['message'] as String,
      repeatDays: map['repeatDays'] != null
          ? List<int>.from(map['repeatDays'])
          : null,
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'hour': hour,
      'minute': minute,
      'enabled': enabled,
      'message': message,
      'repeatDays': repeatDays,
    };
  }

  /// 格式化时间 (如 "09:00")
  String get formattedTime {
    return '${hour.toString().padLeft(2, '0')}:${minute.toString().padLeft(2, '0')}';
  }

  /// 复制并修改部分字段
  TimeAlarmItem copyWith({
    String? id,
    int? hour,
    int? minute,
    bool? enabled,
    String? message,
    List<int>? repeatDays,
  }) {
    return TimeAlarmItem(
      id: id ?? this.id,
      hour: hour ?? this.hour,
      minute: minute ?? this.minute,
      enabled: enabled ?? this.enabled,
      message: message ?? this.message,
      repeatDays: repeatDays ?? this.repeatDays,
    );
  }

  @override
  String toString() {
    return '$formattedTime - $message';
  }
}

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

/// Type B - 行为监控提醒数据类
class BehaviorAlarmItem {
  final String id;
  final String name;
  final TimeRange timeRange;
  final int thresholdSeconds;
  final bool enabled;
  final String message;

  BehaviorAlarmItem({
    required this.id,
    required this.name,
    required this.timeRange,
    required this.thresholdSeconds,
    required this.enabled,
    required this.message,
  });

  /// 从 Map 创建
  factory BehaviorAlarmItem.fromMap(Map<String, dynamic> map) {
    return BehaviorAlarmItem(
      id: map['id'] as String,
      name: map['name'] as String,
      timeRange: TimeRange.fromMap(Map<String, dynamic>.from(map['timeRange'])),
      thresholdSeconds: map['thresholdSeconds'] as int,
      enabled: map['enabled'] as bool,
      message: map['message'] as String,
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'timeRange': timeRange.toMap(),
      'thresholdSeconds': thresholdSeconds,
      'enabled': enabled,
      'message': message,
    };
  }

  /// 复制并修改部分字段
  BehaviorAlarmItem copyWith({
    String? id,
    String? name,
    TimeRange? timeRange,
    int? thresholdSeconds,
    bool? enabled,
    String? message,
  }) {
    return BehaviorAlarmItem(
      id: id ?? this.id,
      name: name ?? this.name,
      timeRange: timeRange ?? this.timeRange,
      thresholdSeconds: thresholdSeconds ?? this.thresholdSeconds,
      enabled: enabled ?? this.enabled,
      message: message ?? this.message,
    );
  }

  @override
  String toString() {
    return '$name ($timeRange, ${thresholdSeconds}秒)';
  }
}

/// 行为监控提醒配置（旧版，保留用于兼容）
class BehaviorAlarmConfig {
  final bool enabled;
  final List<TimeRange> timeRanges;
  final int screenTimeThreshold; // 秒
  final String message; // 自定义提示语

  BehaviorAlarmConfig({
    required this.enabled,
    required this.timeRanges,
    required this.screenTimeThreshold,
    this.message = "亮屏时间过长！",
  });

  /// 默认配置
  factory BehaviorAlarmConfig.defaultConfig() {
    return BehaviorAlarmConfig(
      enabled: false,
      timeRanges: [TimeRange(startHour: 21, startMinute: 30, endHour: 8, endMinute: 0)],
      screenTimeThreshold: 30,
      message: "亮屏时间过长！",
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
      message: map['message'] as String? ?? "亮屏时间过长！",
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'enabled': enabled,
      'timeRanges': timeRanges.map((range) => range.toMap()).toList(),
      'screenTimeThreshold': screenTimeThreshold,
      'message': message,
    };
  }

  /// 复制并修改部分字段
  BehaviorAlarmConfig copyWith({
    bool? enabled,
    List<TimeRange>? timeRanges,
    int? screenTimeThreshold,
    String? message,
  }) {
    return BehaviorAlarmConfig(
      enabled: enabled ?? this.enabled,
      timeRanges: timeRanges ?? this.timeRanges,
      screenTimeThreshold: screenTimeThreshold ?? this.screenTimeThreshold,
      message: message ?? this.message,
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

  /// 设置行为监控提示语
  ///
  /// [message] 自定义提示语（最多 50 字）
  static Future<bool> setBehaviorAlarmMessage(String message) async {
    try {
      final result = await _channel.invokeMethod('setBehaviorAlarmMessage', {
        'message': message,
      });
      return result == true;
    } catch (e) {
      print('设置行为监控提示语失败: $e');
      return false;
    }
  }

  // ========== Type A 多提醒管理接口 ==========

  /// 获取所有定时提醒
  static Future<List<TimeAlarmItem>> getTimeAlarms() async {
    try {
      final result = await _channel.invokeMethod('getTimeAlarms');
      final list = result as List<dynamic>;
      return list.map((item) => TimeAlarmItem.fromMap(Map<String, dynamic>.from(item))).toList();
    } catch (e) {
      print('获取定时提醒列表失败: $e');
      return [];
    }
  }

  /// 添加定时提醒
  static Future<bool> addTimeAlarm(TimeAlarmItem item) async {
    try {
      final result = await _channel.invokeMethod('addTimeAlarm', item.toMap());
      return result == true;
    } catch (e) {
      print('添加定时提醒失败: $e');
      return false;
    }
  }

  /// 更新定时提醒
  static Future<bool> updateTimeAlarm(TimeAlarmItem item) async {
    try {
      final result = await _channel.invokeMethod('updateTimeAlarm', item.toMap());
      return result == true;
    } catch (e) {
      print('更新定时提醒失败: $e');
      return false;
    }
  }

  /// 删除定时提醒
  static Future<bool> deleteTimeAlarm(String id) async {
    try {
      final result = await _channel.invokeMethod('deleteTimeAlarm', {'id': id});
      return result == true;
    } catch (e) {
      print('删除定时提醒失败: $e');
      return false;
    }
  }

  /// 切换定时提醒启用状态
  /// 返回新的启用状态，失败返回 null
  static Future<bool?> toggleTimeAlarm(String id) async {
    try {
      final result = await _channel.invokeMethod('toggleTimeAlarm', {'id': id});
      return result as bool?;
    } catch (e) {
      print('切换定时提醒状态失败: $e');
      return null;
    }
  }

  // ========== Type B 多提醒管理接口 ==========

  /// 获取所有行为监控配置
  static Future<List<BehaviorAlarmItem>> getBehaviorAlarms() async {
    try {
      final result = await _channel.invokeMethod('getBehaviorAlarms');
      final list = result as List<dynamic>;
      return list.map((item) => BehaviorAlarmItem.fromMap(Map<String, dynamic>.from(item))).toList();
    } catch (e) {
      print('获取行为监控配置列表失败: $e');
      return [];
    }
  }

  /// 添加行为监控配置
  static Future<bool> addBehaviorAlarm(BehaviorAlarmItem item) async {
    try {
      final result = await _channel.invokeMethod('addBehaviorAlarm', item.toMap());
      return result == true;
    } catch (e) {
      print('添加行为监控配置失败: $e');
      return false;
    }
  }

  /// 更新行为监控配置
  static Future<bool> updateBehaviorAlarm(BehaviorAlarmItem item) async {
    try {
      final result = await _channel.invokeMethod('updateBehaviorAlarm', item.toMap());
      return result == true;
    } catch (e) {
      print('更新行为监控配置失败: $e');
      return false;
    }
  }

  /// 删除行为监控配置
  static Future<bool> deleteBehaviorAlarm(String id) async {
    try {
      final result = await _channel.invokeMethod('deleteBehaviorAlarm', {'id': id});
      return result == true;
    } catch (e) {
      print('删除行为监控配置失败: $e');
      return false;
    }
  }

  /// 切换行为监控配置启用状态
  /// 返回新的启用状态，失败返回 null
  static Future<bool?> toggleBehaviorAlarm(String id) async {
    try {
      final result = await _channel.invokeMethod('toggleBehaviorAlarm', {'id': id});
      return result as bool?;
    } catch (e) {
      print('切换行为监控配置状态失败: $e');
      return null;
    }
  }

  // ========== 测试接口 ==========

  /// 测试多提醒功能（添加3个测试提醒：1分钟后、2分钟后、禁用的）
  static Future<bool> testMultipleAlarms() async {
    try {
      final result = await _channel.invokeMethod('testMultipleAlarms');
      return result == true;
    } catch (e) {
      print('测试多提醒功能失败: $e');
      return false;
    }
  }
}
