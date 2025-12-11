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

/// 问题类型枚举 (Type C)
enum QuestionType {
  choice,  // 单选/多选题
  text,    // 自由文本输入
  slider;  // 滑动打分

  String toNative() {
    switch (this) {
      case QuestionType.choice:
        return 'CHOICE';
      case QuestionType.text:
        return 'TEXT';
      case QuestionType.slider:
        return 'SLIDER';
    }
  }

  static QuestionType fromNative(String value) {
    switch (value.toUpperCase()) {
      case 'CHOICE':
        return QuestionType.choice;
      case 'TEXT':
        return QuestionType.text;
      case 'SLIDER':
        return QuestionType.slider;
      default:
        return QuestionType.text;
    }
  }
}

/// 触发类型枚举 (Type C)
enum TriggerType {
  time,      // 定时提醒触发
  behavior;  // 行为监控触发

  String toNative() {
    switch (this) {
      case TriggerType.time:
        return 'TIME';
      case TriggerType.behavior:
        return 'BEHAVIOR';
    }
  }

  static TriggerType fromNative(String value) {
    switch (value.toUpperCase()) {
      case 'TIME':
        return TriggerType.time;
      case 'BEHAVIOR':
        return TriggerType.behavior;
      default:
        return TriggerType.time;
    }
  }
}

/// 问题数据类 (Type C)
class Question {
  final String id;
  final QuestionType type;
  final String title;
  final List<String>? options;          // CHOICE 类型使用
  final bool isMultipleChoice;          // CHOICE 类型使用
  final int? min;                       // SLIDER 类型使用
  final int? max;                       // SLIDER 类型使用
  final int? step;                      // SLIDER 类型使用

  Question({
    required this.id,
    required this.type,
    required this.title,
    this.options,
    this.isMultipleChoice = false,
    this.min,
    this.max,
    this.step,
  });

  /// 从 Map 创建
  factory Question.fromMap(Map<String, dynamic> map) {
    return Question(
      id: map['id'] as String,
      type: QuestionType.fromNative(map['type'] as String),
      title: map['title'] as String,
      options: map['options'] != null ? List<String>.from(map['options']) : null,
      isMultipleChoice: map['isMultipleChoice'] as bool? ?? false,
      min: map['min'] as int?,
      max: map['max'] as int?,
      step: map['step'] as int?,
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'type': type.toNative(),
      'title': title,
      'options': options,
      'isMultipleChoice': isMultipleChoice,
      'min': min,
      'max': max,
      'step': step,
    };
  }

  /// 创建单选/多选题
  factory Question.choice({
    required String id,
    required String title,
    required List<String> options,
    bool isMultipleChoice = false,
  }) {
    return Question(
      id: id,
      type: QuestionType.choice,
      title: title,
      options: options,
      isMultipleChoice: isMultipleChoice,
    );
  }

  /// 创建文本题
  factory Question.text({
    required String id,
    required String title,
  }) {
    return Question(
      id: id,
      type: QuestionType.text,
      title: title,
    );
  }

  /// 创建滑动打分题
  factory Question.slider({
    required String id,
    required String title,
    int min = 0,
    int max = 10,
    int step = 1,
  }) {
    return Question(
      id: id,
      type: QuestionType.slider,
      title: title,
      min: min,
      max: max,
      step: step,
    );
  }
}

/// Type C - 记录配置数据类
class RecordConfig {
  final String uuid;
  final TriggerType triggerType;
  final String triggerRefId;
  final String title;
  final List<Question> questions;

  RecordConfig({
    required this.uuid,
    required this.triggerType,
    required this.triggerRefId,
    required this.title,
    required this.questions,
  });

  /// 从 Map 创建
  factory RecordConfig.fromMap(Map<String, dynamic> map) {
    final questionsList = map['questions'] as List<dynamic>;
    final questions = questionsList
        .map((q) => Question.fromMap(Map<String, dynamic>.from(q)))
        .toList();

    return RecordConfig(
      uuid: map['uuid'] as String,
      triggerType: TriggerType.fromNative(map['triggerType'] as String),
      triggerRefId: map['triggerRefId'] as String,
      title: map['title'] as String,
      questions: questions,
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'uuid': uuid,
      'triggerType': triggerType.toNative(),
      'triggerRefId': triggerRefId,
      'title': title,
      'questions': questions.map((q) => q.toMap()).toList(),
    };
  }

  /// 复制并修改部分字段
  RecordConfig copyWith({
    String? uuid,
    TriggerType? triggerType,
    String? triggerRefId,
    String? title,
    List<Question>? questions,
  }) {
    return RecordConfig(
      uuid: uuid ?? this.uuid,
      triggerType: triggerType ?? this.triggerType,
      triggerRefId: triggerRefId ?? this.triggerRefId,
      title: title ?? this.title,
      questions: questions ?? this.questions,
    );
  }

  @override
  String toString() {
    final triggerTypeStr = triggerType == TriggerType.time ? '定时' : '行为';
    return '$title ($triggerTypeStr, ${questions.length} 题)';
  }
}

/// 记录答案数据类 (Type C)
class RecordAnswer {
  final int timestamp;
  final String recordId;
  final Map<String, dynamic> answers;

  RecordAnswer({
    required this.timestamp,
    required this.recordId,
    required this.answers,
  });

  /// 从 Map 创建
  factory RecordAnswer.fromMap(Map<String, dynamic> map) {
    return RecordAnswer(
      timestamp: map['timestamp'] as int,
      recordId: map['recordId'] as String,
      answers: Map<String, dynamic>.from(map['answers']),
    );
  }

  /// 转换为 Map
  Map<String, dynamic> toMap() {
    return {
      'timestamp': timestamp,
      'recordId': recordId,
      'answers': answers,
    };
  }

  /// 格式化时间戳为日期时间字符串
  String get formattedDateTime {
    final dateTime = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} '
           '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
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

  // ========== Type C 记录系统接口 ==========

  /// 获取所有记录配置
  static Future<List<RecordConfig>> getRecordConfigs() async {
    try {
      final result = await _channel.invokeMethod('getRecordConfigs');
      final list = result as List<dynamic>;
      return list.map((item) => RecordConfig.fromMap(Map<String, dynamic>.from(item))).toList();
    } catch (e) {
      print('获取记录配置列表失败: $e');
      return [];
    }
  }

  /// 保存记录配置（自动判断新增/更新）
  static Future<bool> saveRecordConfig(RecordConfig config) async {
    try {
      final result = await _channel.invokeMethod('saveRecordConfig', config.toMap());
      return result == true;
    } catch (e) {
      print('保存记录配置失败: $e');
      return false;
    }
  }

  /// 删除记录配置（级联删除关联答案）
  static Future<bool> deleteRecordConfig(String uuid) async {
    try {
      final result = await _channel.invokeMethod('deleteRecordConfig', {'uuid': uuid});
      return result == true;
    } catch (e) {
      print('删除记录配置失败: $e');
      return false;
    }
  }

  /// 获取答案记录
  /// [recordId] 可选，指定则获取该记录的所有答案，否则获取全部答案
  static Future<List<RecordAnswer>> getRecordAnswers({String? recordId}) async {
    try {
      final result = await _channel.invokeMethod('getRecordAnswers',
        recordId != null ? {'recordId': recordId} : null);
      final list = result as List<dynamic>;
      return list.map((item) => RecordAnswer.fromMap(Map<String, dynamic>.from(item))).toList();
    } catch (e) {
      print('获取答案记录失败: $e');
      return [];
    }
  }

  /// 按时间范围获取答案记录
  /// [startTimestamp] 起始时间戳（Unix 毫秒）
  /// [endTimestamp] 结束时间戳（Unix 毫秒）
  static Future<List<RecordAnswer>> getRecordAnswersByTimeRange({
    required int startTimestamp,
    required int endTimestamp,
  }) async {
    try {
      final result = await _channel.invokeMethod('getRecordAnswersByTimeRange', {
        'startTimestamp': startTimestamp,
        'endTimestamp': endTimestamp,
      });
      final list = result as List<dynamic>;
      return list.map((item) => RecordAnswer.fromMap(Map<String, dynamic>.from(item))).toList();
    } catch (e) {
      print('按时间范围获取答案记录失败: $e');
      return [];
    }
  }

  /// 导出所有答案为 JSON 字符串
  static Future<String> exportRecordAnswers() async {
    try {
      final result = await _channel.invokeMethod('exportRecordAnswers');
      return result as String;
    } catch (e) {
      print('导出答案记录失败: $e');
      return '[]';
    }
  }

  /// 删除指定时间戳的答案记录
  static Future<bool> deleteRecordAnswer(int timestamp) async {
    try {
      final result = await _channel.invokeMethod('deleteRecordAnswer', {'timestamp': timestamp});
      return result == true;
    } catch (e) {
      print('删除答案记录失败: $e');
      return false;
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
