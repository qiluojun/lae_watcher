import 'package:flutter/material.dart';
import '../services/native_bridge.dart';

/// SettingsScreen - 提醒时间设置界面
///
/// 功能:
/// - 显示当前设置的提醒时间
/// - TimePicker 修改提醒时间
/// - 实时更新 Android 原生定时
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  // 定时提醒配置
  TimeOfDay _selectedTime = const TimeOfDay(hour: 15, minute: 15);
  bool _isEnabled = true;
  bool _isLoading = true;

  // 行为监控配置
  BehaviorAlarmConfig _behaviorConfig = BehaviorAlarmConfig.defaultConfig();
  bool _isBehaviorLoading = true;

  @override
  void initState() {
    super.initState();
    _loadCurrentSettings();
    _loadBehaviorSettings();
  }

  /// 从原生加载当前设置
  Future<void> _loadCurrentSettings() async {
    final settings = await NativeBridge.getAlarmSettings();
    if (settings != null) {
      setState(() {
        _selectedTime = TimeOfDay(
          hour: settings['hour'] ?? 15,
          minute: settings['minute'] ?? 15,
        );
        _isEnabled = settings['enabled'] ?? true;
        _isLoading = false;
      });
    } else {
      setState(() {
        _isLoading = false;
      });
    }
  }

  /// 加载行为监控配置
  Future<void> _loadBehaviorSettings() async {
    final config = await NativeBridge.getBehaviorAlarmConfig();
    if (config != null) {
      setState(() {
        _behaviorConfig = config;
        _isBehaviorLoading = false;
      });
    } else {
      setState(() {
        _isBehaviorLoading = false;
      });
    }
  }

  /// 选择时间
  Future<void> _pickTime() async {
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: _selectedTime,
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: false),
          child: child!,
        );
      },
    );

    if (picked != null && picked != _selectedTime) {
      setState(() {
        _selectedTime = picked;
      });

      // 立即保存到原生
      final success = await NativeBridge.setAlarm(picked.hour, picked.minute);
      if (success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('提醒时间已设置为 ${picked.format(context)}'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    }
  }

  /// 切换启用状态
  Future<void> _toggleEnabled(bool value) async {
    if (value) {
      // 启用定时提醒
      final success = await NativeBridge.setAlarm(
        _selectedTime.hour,
        _selectedTime.minute,
      );
      if (success) {
        setState(() {
          _isEnabled = true;
        });
      }
    } else {
      // 取消定时提醒
      final success = await NativeBridge.cancelAlarm();
      if (success) {
        setState(() {
          _isEnabled = false;
        });
      }
    }
  }

  // ========== 行为监控相关方法 ==========

  /// 切换行为监控启用状态
  Future<void> _toggleBehaviorEnabled(bool value) async {
    final success = await NativeBridge.setBehaviorAlarmEnabled(value);
    if (success) {
      setState(() {
        _behaviorConfig = _behaviorConfig.copyWith(enabled: value);
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(value ? '行为监控已启用' : '行为监控已停止'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    }
  }

  /// 修改亮屏阈值
  Future<void> _updateThreshold(int threshold) async {
    final success = await NativeBridge.setBehaviorAlarmThreshold(threshold);
    if (success) {
      setState(() {
        _behaviorConfig = _behaviorConfig.copyWith(screenTimeThreshold: threshold);
      });
    }
  }

  /// 选择时间段的开始时间
  Future<void> _pickTimeRangeStart(int index) async {
    if (index >= _behaviorConfig.timeRanges.length) return;

    final currentRange = _behaviorConfig.timeRanges[index];
    final initialTime = TimeOfDay(
      hour: currentRange.startHour,
      minute: currentRange.startMinute,
    );

    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: initialTime,
    );

    if (picked != null) {
      final newRange = TimeRange(
        startHour: picked.hour,
        startMinute: picked.minute,
        endHour: currentRange.endHour,
        endMinute: currentRange.endMinute,
      );

      final newTimeRanges = List<TimeRange>.from(_behaviorConfig.timeRanges);
      newTimeRanges[index] = newRange;

      final newConfig = _behaviorConfig.copyWith(timeRanges: newTimeRanges);
      final success = await NativeBridge.setBehaviorAlarmConfig(newConfig);

      if (success) {
        setState(() {
          _behaviorConfig = newConfig;
        });

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('开始时间已设置为 ${picked.format(context)}'),
              duration: const Duration(seconds: 2),
            ),
          );
        }
      }
    }
  }

  /// 选择时间段的结束时间
  Future<void> _pickTimeRangeEnd(int index) async {
    if (index >= _behaviorConfig.timeRanges.length) return;

    final currentRange = _behaviorConfig.timeRanges[index];
    final initialTime = TimeOfDay(
      hour: currentRange.endHour,
      minute: currentRange.endMinute,
    );

    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: initialTime,
    );

    if (picked != null) {
      final newRange = TimeRange(
        startHour: currentRange.startHour,
        startMinute: currentRange.startMinute,
        endHour: picked.hour,
        endMinute: picked.minute,
      );

      final newTimeRanges = List<TimeRange>.from(_behaviorConfig.timeRanges);
      newTimeRanges[index] = newRange;

      final newConfig = _behaviorConfig.copyWith(timeRanges: newTimeRanges);
      final success = await NativeBridge.setBehaviorAlarmConfig(newConfig);

      if (success) {
        setState(() {
          _behaviorConfig = newConfig;
        });

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('结束时间已设置为 ${picked.format(context)}'),
              duration: const Duration(seconds: 2),
            ),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('提醒设置'),
        ),
        body: const Center(
          child: CircularProgressIndicator(),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('LAE-Watcher 设置'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 提醒时间设置卡片
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '每日提醒时间',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('当前设置时间'),
                          const SizedBox(height: 4),
                          Text(
                            _selectedTime.format(context),
                            style: const TextStyle(
                              fontSize: 32,
                              fontWeight: FontWeight.bold,
                              color: Colors.blue,
                            ),
                          ),
                        ],
                      ),
                      ElevatedButton.icon(
                        onPressed: _pickTime,
                        icon: const Icon(Icons.access_time),
                        label: const Text('修改时间'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SwitchListTile(
                    title: const Text('启用提醒'),
                    subtitle: Text(_isEnabled ? '提醒已启用' : '提醒已禁用'),
                    value: _isEnabled,
                    onChanged: _toggleEnabled,
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // ========== 行为监控提醒设置卡片 ==========
          Card(
            color: Colors.purple.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.visibility, color: Colors.purple.shade700),
                      const SizedBox(width: 8),
                      const Text(
                        '行为监控提醒',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '在指定时段内，监控亮屏时长，超过阈值后提醒',
                    style: TextStyle(fontSize: 14, color: Colors.grey),
                  ),
                  const SizedBox(height: 16),

                  // 启用开关
                  SwitchListTile(
                    title: const Text('启用监控'),
                    subtitle: Text(_behaviorConfig.enabled ? '监控运行中' : '监控已停止'),
                    value: _behaviorConfig.enabled,
                    onChanged: _isBehaviorLoading ? null : _toggleBehaviorEnabled,
                  ),

                  const Divider(),

                  // 监控时段设置
                  const Text(
                    '监控时段',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  if (_behaviorConfig.timeRanges.isNotEmpty)
                    ...List.generate(_behaviorConfig.timeRanges.length, (index) {
                      final range = _behaviorConfig.timeRanges[index];
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 4),
                        child: Row(
                          children: [
                            Expanded(
                              child: OutlinedButton(
                                onPressed: () => _pickTimeRangeStart(index),
                                child: Text(
                                  '${range.startHour.toString().padLeft(2, '0')}:${range.startMinute.toString().padLeft(2, '0')}',
                                  style: const TextStyle(fontSize: 16),
                                ),
                              ),
                            ),
                            const Padding(
                              padding: EdgeInsets.symmetric(horizontal: 8),
                              child: Text('至', style: TextStyle(fontSize: 16)),
                            ),
                            Expanded(
                              child: OutlinedButton(
                                onPressed: () => _pickTimeRangeEnd(index),
                                child: Text(
                                  '${range.endHour.toString().padLeft(2, '0')}:${range.endMinute.toString().padLeft(2, '0')}',
                                  style: const TextStyle(fontSize: 16),
                                ),
                              ),
                            ),
                          ],
                        ),
                      );
                    }),

                  const SizedBox(height: 8),
                  const Divider(),

                  // 亮屏阈值设置
                  const Text(
                    '亮屏时长阈值',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: Slider(
                          value: _behaviorConfig.screenTimeThreshold.toDouble(),
                          min: 10,
                          max: 600,
                          divisions: 59,
                          label: '${_behaviorConfig.screenTimeThreshold} 秒',
                          onChanged: _isBehaviorLoading
                              ? null
                              : (value) {
                                  setState(() {
                                    _behaviorConfig = _behaviorConfig.copyWith(
                                      screenTimeThreshold: value.toInt(),
                                    );
                                  });
                                },
                          onChangeEnd: (value) {
                            _updateThreshold(value.toInt());
                          },
                        ),
                      ),
                      SizedBox(
                        width: 80,
                        child: Text(
                          '${_behaviorConfig.screenTimeThreshold} 秒',
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '超过 ${_behaviorConfig.screenTimeThreshold} 秒后将触发提醒',
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),

                  const SizedBox(height: 16),

                  // 快速测试按钮
                  OutlinedButton.icon(
                    onPressed: () async {
                      // 快速测试：设置10秒阈值，立即启动监控
                      final testConfig = _behaviorConfig.copyWith(
                        enabled: true,
                        screenTimeThreshold: 10,
                      );
                      final success = await NativeBridge.setBehaviorAlarmConfig(testConfig);
                      if (success) {
                        setState(() {
                          _behaviorConfig = testConfig;
                        });
                        if (mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('测试模式已启动：10秒阈值，请亮屏测试'),
                              duration: Duration(seconds: 3),
                              backgroundColor: Colors.purple,
                            ),
                          );
                        }
                      }
                    },
                    icon: const Icon(Icons.science),
                    label: const Text('快速测试（10秒阈值）'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.purple.shade700,
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // 测试说明卡片
          Card(
            color: Colors.green.shade50,
            child: const Padding(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.check_circle_outline, color: Colors.green),
                      SizedBox(width: 8),
                      Text(
                        '功能状态',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 8),
                  Text(
                    '✅ Step 3 已完成：全屏提醒功能\n'
                    '• 定时触发时会直接弹出提醒界面\n'
                    '• 支持息屏唤醒（自动亮屏并显示）\n'
                    '• 可点击"我知道了"关闭或"再等1min"延迟',
                    style: TextStyle(fontSize: 14),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // 快捷测试按钮（秒级测试）
          Card(
            color: Colors.green.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.flash_on, color: Colors.green),
                      SizedBox(width: 8),
                      Text(
                        '快速测试（秒级）',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '用于测试息屏唤醒功能，请先锁屏再等待触发',
                    style: TextStyle(fontSize: 14, color: Colors.grey),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      ElevatedButton(
                        onPressed: () async {
                          final success = await NativeBridge.setTestAlarm(5);
                          if (success && mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text('测试提醒已设置：5秒后触发'),
                                duration: Duration(seconds: 2),
                                backgroundColor: Colors.green,
                              ),
                            );
                          }
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.green,
                          foregroundColor: Colors.white,
                        ),
                        child: const Text('5 秒'),
                      ),
                      ElevatedButton(
                        onPressed: () async {
                          final success = await NativeBridge.setTestAlarm(10);
                          if (success && mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text('测试提醒已设置：10秒后触发'),
                                duration: Duration(seconds: 2),
                                backgroundColor: Colors.green,
                              ),
                            );
                          }
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.green,
                          foregroundColor: Colors.white,
                        ),
                        child: const Text('10 秒'),
                      ),
                      ElevatedButton(
                        onPressed: () async {
                          final success = await NativeBridge.setTestAlarm(15);
                          if (success && mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text('测试提醒已设置：15秒后触发'),
                                duration: Duration(seconds: 2),
                                backgroundColor: Colors.green,
                              ),
                            );
                          }
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.green,
                          foregroundColor: Colors.white,
                        ),
                        child: const Text('15 秒'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // 保留原有的1分钟测试按钮
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '快捷测试（分钟级）',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '点击按钮快速设置为当前时间+1分钟，方便测试',
                    style: TextStyle(fontSize: 14, color: Colors.grey),
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton.icon(
                    onPressed: () async {
                      // 使用秒级测试方法（60秒 = 1分钟）
                      final success = await NativeBridge.setTestAlarm(60);

                      if (success && mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('测试提醒已设置：60秒（1分钟）后触发'),
                            duration: Duration(seconds: 3),
                            backgroundColor: Colors.orange,
                          ),
                        );
                      }
                    },
                    icon: const Icon(Icons.bug_report),
                    label: const Text('设置为 1 分钟后'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.orange,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // ========== 多提醒测试卡片 ==========
          const SizedBox(height: 16),
          Card(
            color: Colors.green.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.science, color: Colors.green.shade700),
                      const SizedBox(width: 8),
                      Text(
                        '测试多提醒功能 (Task 2)',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.green.shade700,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    '点击按钮将添加 3 个测试提醒：',
                    style: TextStyle(fontSize: 14),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '• 提醒1：1分钟后触发\n'
                    '• 提醒2：2分钟后触发\n'
                    '• 提醒3：禁用状态（不会触发）',
                    style: TextStyle(fontSize: 12, color: Colors.black87),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: () async {
                        final success = await NativeBridge.testMultipleAlarms();

                        if (success && mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text(
                                '测试提醒已创建！\n'
                                '请查看 logcat 日志，并等待 1-2 分钟观察提醒',
                              ),
                              duration: Duration(seconds: 5),
                              backgroundColor: Colors.green,
                            ),
                          );
                        }
                      },
                      icon: const Icon(Icons.play_arrow),
                      label: const Text('开始测试多提醒'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '💡 提示：点击后请用 adb logcat 查看日志',
                    style: TextStyle(fontSize: 11, color: Colors.black54),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
