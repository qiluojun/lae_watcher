import 'package:flutter/material.dart';
import '../services/native_bridge.dart';

/// Type B 行为监控配置编辑界面
///
/// 功能：
/// - 调整亮屏阈值（滑块，10秒 - 600秒）
/// - 输入自定义提示语（最多50字）
/// - 管理多个监控时段（添加/删除）
/// - 保存配置
class EditBehaviorConfigScreen extends StatefulWidget {
  final BehaviorAlarmConfig config;

  const EditBehaviorConfigScreen({super.key, required this.config});

  @override
  State<EditBehaviorConfigScreen> createState() => _EditBehaviorConfigScreenState();
}

class _EditBehaviorConfigScreenState extends State<EditBehaviorConfigScreen> {
  late int _threshold;
  late TextEditingController _messageController;
  late List<TimeRange> _timeRanges;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _threshold = widget.config.screenTimeThreshold;
    _messageController = TextEditingController(text: widget.config.message);
    _timeRanges = List.from(widget.config.timeRanges); // 创建副本
  }

  @override
  void dispose() {
    _messageController.dispose();
    super.dispose();
  }

  /// 添加时段
  Future<void> _addTimeRange() async {
    final result = await _showTimeRangePicker(context);
    if (result != null) {
      setState(() {
        _timeRanges.add(result);
      });
    }
  }

  /// 删除时段
  void _deleteTimeRange(int index) {
    if (_timeRanges.length <= 1) {
      _showSnackBar('至少需要保留一个监控时段');
      return;
    }

    setState(() {
      _timeRanges.removeAt(index);
    });
  }

  /// 编辑时段
  Future<void> _editTimeRange(int index) async {
    final oldRange = _timeRanges[index];
    final result = await _showTimeRangePicker(
      context,
      initialStartTime: TimeOfDay(hour: oldRange.startHour, minute: oldRange.startMinute),
      initialEndTime: TimeOfDay(hour: oldRange.endHour, minute: oldRange.endMinute),
    );

    if (result != null) {
      setState(() {
        _timeRanges[index] = result;
      });
    }
  }

  /// 保存配置
  Future<void> _save() async {
    // 验证提示语
    final message = _messageController.text.trim();
    if (message.isEmpty) {
      _showSnackBar('提示语不能为空');
      return;
    }

    if (message.length > 50) {
      _showSnackBar('提示语最多50个字符');
      return;
    }

    if (_timeRanges.isEmpty) {
      _showSnackBar('至少需要添加一个监控时段');
      return;
    }

    setState(() => _saving = true);

    try {
      final newConfig = BehaviorAlarmConfig(
        enabled: widget.config.enabled, // 保持原有启用状态
        timeRanges: _timeRanges,
        screenTimeThreshold: _threshold,
        message: message,
      );

      final success = await NativeBridge.setBehaviorAlarmConfig(newConfig);

      if (success) {
        if (mounted) {
          Navigator.pop(context, true); // 返回 true 表示保存成功
        }
      } else {
        _showSnackBar('保存失败');
        setState(() => _saving = false);
      }
    } catch (e) {
      _showSnackBar('保存失败: $e');
      setState(() => _saving = false);
    }
  }

  void _showSnackBar(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('编辑行为监控'),
        actions: [
          // 保存按钮
          TextButton(
            onPressed: _saving ? null : _save,
            child: _saving
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Text('保存', style: TextStyle(color: Colors.white, fontSize: 16)),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 阈值滑块
          Card(
            elevation: 2,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '亮屏时长阈值',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '当前阈值：$_threshold 秒',
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.purple),
                  ),
                  const SizedBox(height: 16),
                  Slider(
                    value: _threshold.toDouble(),
                    min: 10,
                    max: 600,
                    divisions: 59, // (600-10)/10
                    label: '$_threshold 秒',
                    onChanged: (value) {
                      setState(() {
                        _threshold = value.toInt();
                      });
                    },
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('10秒', style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                      Text('600秒 (10分钟)', style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                    ],
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // 提示语输入
          Card(
            elevation: 2,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '提示语',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _messageController,
                    maxLength: 50,
                    maxLines: 2,
                    decoration: const InputDecoration(
                      hintText: '输入触发提醒时显示的文字（最多50字）',
                      border: OutlineInputBorder(),
                      counterText: '',
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text(
                        '${_messageController.text.length}/50',
                        style: TextStyle(
                          fontSize: 12,
                          color: _messageController.text.length > 50 ? Colors.red : Colors.grey,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // 监控时段列表
          Card(
            elevation: 2,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        '监控时段',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                      ),
                      TextButton.icon(
                        onPressed: _addTimeRange,
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('添加时段'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  if (_timeRanges.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 16),
                      child: Center(
                        child: Text(
                          '暂无监控时段',
                          style: TextStyle(color: Colors.grey),
                        ),
                      ),
                    )
                  else
                    ..._timeRanges.asMap().entries.map((entry) {
                      final index = entry.key;
                      final range = entry.value;
                      return _buildTimeRangeItem(range, index);
                    }),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // 说明文字
          Card(
            color: Colors.purple.shade50,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline, size: 20, color: Colors.purple.shade700),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      '在设定的监控时段内，如果连续亮屏时间超过阈值，将弹出提醒。\n'
                      '可以设置多个监控时段（例如：21:30-08:00 + 13:00-14:00）。',
                      style: TextStyle(fontSize: 13, color: Colors.purple.shade700),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 时段列表项
  Widget _buildTimeRangeItem(TimeRange range, int index) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: Colors.purple.shade50,
      child: ListTile(
        leading: const Icon(Icons.access_time, color: Colors.purple),
        title: Text(
          range.toString(),
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 编辑按钮
            IconButton(
              icon: const Icon(Icons.edit, size: 20),
              onPressed: () => _editTimeRange(index),
              tooltip: '编辑',
            ),
            // 删除按钮
            IconButton(
              icon: const Icon(Icons.delete, size: 20, color: Colors.red),
              onPressed: () => _deleteTimeRange(index),
              tooltip: '删除',
            ),
          ],
        ),
        onTap: () => _editTimeRange(index),
      ),
    );
  }

  /// 时段选择器对话框
  static Future<TimeRange?> _showTimeRangePicker(
    BuildContext context, {
    TimeOfDay? initialStartTime,
    TimeOfDay? initialEndTime,
  }) async {
    TimeOfDay startTime = initialStartTime ?? const TimeOfDay(hour: 21, minute: 30);
    TimeOfDay endTime = initialEndTime ?? const TimeOfDay(hour: 8, minute: 0);

    return showDialog<TimeRange>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('选择监控时段'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // 开始时间
              ListTile(
                leading: const Icon(Icons.play_arrow),
                title: const Text('开始时间'),
                trailing: Text(
                  startTime.format(context),
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                onTap: () async {
                  final time = await showTimePicker(
                    context: context,
                    initialTime: startTime,
                    builder: (context, child) {
                      return MediaQuery(
                        data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
                        child: child!,
                      );
                    },
                  );
                  if (time != null) {
                    setState(() => startTime = time);
                  }
                },
              ),
              const Divider(),
              // 结束时间
              ListTile(
                leading: const Icon(Icons.stop),
                title: const Text('结束时间'),
                trailing: Text(
                  endTime.format(context),
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                onTap: () async {
                  final time = await showTimePicker(
                    context: context,
                    initialTime: endTime,
                    builder: (context, child) {
                      return MediaQuery(
                        data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
                        child: child!,
                      );
                    },
                  );
                  if (time != null) {
                    setState(() => endTime = time);
                  }
                },
              ),
              const SizedBox(height: 8),
              Text(
                '提示：支持跨午夜时段（如 21:30 - 08:00）',
                style: TextStyle(fontSize: 12, color: Colors.grey[600]),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () {
                final range = TimeRange(
                  startHour: startTime.hour,
                  startMinute: startTime.minute,
                  endHour: endTime.hour,
                  endMinute: endTime.minute,
                );
                Navigator.pop(context, range);
              },
              child: const Text('确定'),
            ),
          ],
        ),
      ),
    );
  }
}
