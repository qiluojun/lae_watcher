import 'package:flutter/material.dart';
import '../services/native_bridge.dart';

/// Type A 定时提醒添加/编辑界面
///
/// 功能：
/// - 选择时间（TimePicker）
/// - 输入自定义提示语（最多50字）
/// - 保存提醒
class EditTimeAlarmScreen extends StatefulWidget {
  /// 编辑模式时传入已有提醒，添加模式时为 null
  final TimeAlarmItem? alarm;

  const EditTimeAlarmScreen({super.key, this.alarm});

  @override
  State<EditTimeAlarmScreen> createState() => _EditTimeAlarmScreenState();
}

class _EditTimeAlarmScreenState extends State<EditTimeAlarmScreen> {
  late TimeOfDay _selectedTime;
  late TextEditingController _messageController;
  late bool _enabled;
  bool _saving = false;

  // 是否为编辑模式
  bool get _isEditMode => widget.alarm != null;

  @override
  void initState() {
    super.initState();

    if (_isEditMode) {
      // 编辑模式：初始化为已有数据
      final alarm = widget.alarm!;
      _selectedTime = TimeOfDay(hour: alarm.hour, minute: alarm.minute);
      _messageController = TextEditingController(text: alarm.message);
      _enabled = alarm.enabled;
    } else {
      // 添加模式：初始化为默认值
      _selectedTime = TimeOfDay.now();
      _messageController = TextEditingController(text: '该休息了！');
      _enabled = true;
    }
  }

  @override
  void dispose() {
    _messageController.dispose();
    super.dispose();
  }

  /// 选择时间
  Future<void> _selectTime() async {
    final time = await showTimePicker(
      context: context,
      initialTime: _selectedTime,
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
          child: child!,
        );
      },
    );

    if (time != null) {
      setState(() {
        _selectedTime = time;
      });
    }
  }

  /// 保存提醒
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

    setState(() => _saving = true);

    try {
      final alarm = TimeAlarmItem(
        id: _isEditMode ? widget.alarm!.id : '', // 添加模式时 id 为空，原生层会生成 UUID
        hour: _selectedTime.hour,
        minute: _selectedTime.minute,
        enabled: _enabled,
        message: message,
        repeatDays: null, // V1 暂不支持周重复
      );

      final success = _isEditMode
          ? await NativeBridge.updateTimeAlarm(alarm)
          : await NativeBridge.addTimeAlarm(alarm);

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
        title: Text(_isEditMode ? '编辑提醒' : '添加提醒'),
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
          // 时间选择卡片
          Card(
            elevation: 2,
            child: InkWell(
              onTap: _selectTime,
              borderRadius: BorderRadius.circular(8),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    const Text(
                      '提醒时间',
                      style: TextStyle(fontSize: 16, color: Colors.black54),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      _selectedTime.format(context),
                      style: const TextStyle(
                        fontSize: 56,
                        fontWeight: FontWeight.bold,
                        color: Colors.blue,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.edit, size: 16, color: Colors.grey[600]),
                        const SizedBox(width: 4),
                        Text(
                          '点击修改时间',
                          style: TextStyle(fontSize: 14, color: Colors.grey[600]),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),

          const SizedBox(height: 20),

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
                      hintText: '输入提醒时显示的文字（最多50字）',
                      border: OutlineInputBorder(),
                      counterText: '', // 隐藏默认计数器
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

          const SizedBox(height: 20),

          // 启用开关
          Card(
            elevation: 2,
            child: SwitchListTile(
              value: _enabled,
              onChanged: (value) => setState(() => _enabled = value),
              title: const Text('启用提醒', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              subtitle: Text(
                _enabled ? '提醒已启用' : '提醒已禁用',
                style: TextStyle(fontSize: 14, color: _enabled ? Colors.green : Colors.grey),
              ),
              secondary: Icon(
                _enabled ? Icons.alarm_on : Icons.alarm_off,
                color: _enabled ? Colors.blue : Colors.grey,
              ),
            ),
          ),

          const SizedBox(height: 20),

          // 说明文字
          Card(
            color: Colors.blue.shade50,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline, size: 20, color: Colors.blue.shade700),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      '提醒将在每天的设定时间触发。\n即使手机息屏或切换到其他应用，也会正常弹出提醒。',
                      style: TextStyle(fontSize: 13, color: Colors.blue.shade700),
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
}
