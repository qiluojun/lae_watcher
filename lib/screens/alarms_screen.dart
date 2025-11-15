import 'package:flutter/material.dart';
import '../services/native_bridge.dart';
import 'edit_time_alarm_screen.dart';
import 'edit_behavior_config_screen.dart';

/// 提醒管理主界面
///
/// 功能：
/// - Tab 1: Type A 定时提醒列表（多提醒）
/// - Tab 2: Type B 行为监控配置（单配置 + 多时段）
class AlarmsScreen extends StatefulWidget {
  const AlarmsScreen({Key? key}) : super(key: key);

  @override
  State<AlarmsScreen> createState() => _AlarmsScreenState();
}

class _AlarmsScreenState extends State<AlarmsScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  // Type A 定时提醒列表
  List<TimeAlarmItem> _timeAlarms = [];
  bool _loadingTimeAlarms = true;

  // Type B 行为监控配置
  BehaviorAlarmConfig? _behaviorConfig;
  bool _loadingBehaviorConfig = true;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  /// 加载所有数据
  Future<void> _loadData() async {
    await Future.wait([
      _loadTimeAlarms(),
      _loadBehaviorConfig(),
    ]);
  }

  /// 加载定时提醒列表
  Future<void> _loadTimeAlarms() async {
    setState(() => _loadingTimeAlarms = true);
    try {
      final alarms = await NativeBridge.getTimeAlarms();
      setState(() {
        _timeAlarms = alarms;
        _loadingTimeAlarms = false;
      });
    } catch (e) {
      print('加载定时提醒失败: $e');
      setState(() => _loadingTimeAlarms = false);
    }
  }

  /// 加载行为监控配置
  Future<void> _loadBehaviorConfig() async {
    setState(() => _loadingBehaviorConfig = true);
    try {
      final config = await NativeBridge.getBehaviorAlarmConfig();
      setState(() {
        _behaviorConfig = config;
        _loadingBehaviorConfig = false;
      });
    } catch (e) {
      print('加载行为监控配置失败: $e');
      setState(() => _loadingBehaviorConfig = false);
    }
  }

  /// 切换定时提醒启用状态
  Future<void> _toggleTimeAlarm(String id, bool currentEnabled) async {
    final newEnabled = await NativeBridge.toggleTimeAlarm(id);
    if (newEnabled != null) {
      await _loadTimeAlarms(); // 刷新列表
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('已${newEnabled ? "启用" : "禁用"}提醒')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('切换失败')),
      );
    }
  }

  /// 删除定时提醒（带确认）
  Future<void> _deleteTimeAlarm(TimeAlarmItem item) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除提醒 "${item.message}" 吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirm == true) {
      final success = await NativeBridge.deleteTimeAlarm(item.id);
      if (success) {
        await _loadTimeAlarms();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('删除成功')),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('删除失败')),
        );
      }
    }
  }

  /// 添加/编辑定时提醒
  Future<void> _editTimeAlarm([TimeAlarmItem? item]) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => EditTimeAlarmScreen(alarm: item),
      ),
    );

    if (result == true) {
      await _loadTimeAlarms();
    }
  }

  /// 编辑行为监控配置
  Future<void> _editBehaviorConfig() async {
    if (_behaviorConfig == null) return;

    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => EditBehaviorConfigScreen(config: _behaviorConfig!),
      ),
    );

    if (result == true) {
      await _loadBehaviorConfig();
    }
  }

  /// 切换行为监控启用状态
  Future<void> _toggleBehaviorAlarm(bool currentEnabled) async {
    final success = await NativeBridge.setBehaviorAlarmEnabled(!currentEnabled);
    if (success) {
      await _loadBehaviorConfig();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('已${!currentEnabled ? "启用" : "禁用"}行为监控')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('切换失败')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('提醒管理'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.access_time), text: '定时提醒'),
            Tab(icon: Icon(Icons.visibility), text: '行为监控'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildTimeAlarmsTab(),
          _buildBehaviorConfigTab(),
        ],
      ),
      floatingActionButton: _buildFloatingActionButton(),
    );
  }

  /// Tab 1: 定时提醒列表
  Widget _buildTimeAlarmsTab() {
    if (_loadingTimeAlarms) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_timeAlarms.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.alarm_off, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              '暂无定时提醒',
              style: TextStyle(fontSize: 18, color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              '点击右下角 + 按钮添加提醒',
              style: TextStyle(fontSize: 14, color: Colors.grey[500]),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadTimeAlarms,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _timeAlarms.length,
        itemBuilder: (context, index) {
          final alarm = _timeAlarms[index];
          return _buildTimeAlarmCard(alarm);
        },
      ),
    );
  }

  /// 定时提醒卡片
  Widget _buildTimeAlarmCard(TimeAlarmItem alarm) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: alarm.enabled ? Colors.blue.withOpacity(0.1) : Colors.grey.withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(
            Icons.alarm,
            color: alarm.enabled ? Colors.blue : Colors.grey,
            size: 32,
          ),
        ),
        title: Text(
          alarm.formattedTime,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: alarm.enabled ? Colors.black87 : Colors.grey,
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(
            alarm.message,
            style: TextStyle(
              fontSize: 14,
              color: alarm.enabled ? Colors.black54 : Colors.grey,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 启用/禁用开关
            Switch(
              value: alarm.enabled,
              onChanged: (value) => _toggleTimeAlarm(alarm.id, alarm.enabled),
            ),
            const SizedBox(width: 8),
            // 更多操作按钮
            PopupMenuButton<String>(
              onSelected: (value) {
                if (value == 'edit') {
                  _editTimeAlarm(alarm);
                } else if (value == 'delete') {
                  _deleteTimeAlarm(alarm);
                }
              },
              itemBuilder: (context) => [
                const PopupMenuItem(
                  value: 'edit',
                  child: Row(
                    children: [
                      Icon(Icons.edit, size: 20),
                      SizedBox(width: 8),
                      Text('编辑'),
                    ],
                  ),
                ),
                const PopupMenuItem(
                  value: 'delete',
                  child: Row(
                    children: [
                      Icon(Icons.delete, size: 20, color: Colors.red),
                      SizedBox(width: 8),
                      Text('删除', style: TextStyle(color: Colors.red)),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
        onTap: () => _editTimeAlarm(alarm),
      ),
    );
  }

  /// Tab 2: 行为监控配置
  Widget _buildBehaviorConfigTab() {
    if (_loadingBehaviorConfig) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_behaviorConfig == null) {
      return const Center(child: Text('加载失败'));
    }

    final config = _behaviorConfig!;

    return RefreshIndicator(
      onRefresh: _loadBehaviorConfig,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 配置状态卡片
          Card(
            elevation: 2,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 标题栏
                  Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          color: config.enabled ? Colors.purple.withOpacity(0.1) : Colors.grey.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Icon(
                          Icons.visibility,
                          color: config.enabled ? Colors.purple : Colors.grey,
                          size: 28,
                        ),
                      ),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Text(
                          '行为监控',
                          style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                        ),
                      ),
                      Switch(
                        value: config.enabled,
                        onChanged: (value) => _toggleBehaviorAlarm(config.enabled),
                      ),
                    ],
                  ),
                  const Divider(height: 24),

                  // 阈值
                  Row(
                    children: [
                      const Icon(Icons.timer, size: 20, color: Colors.grey),
                      const SizedBox(width: 8),
                      const Text('亮屏阈值: ', style: TextStyle(fontSize: 16)),
                      Text(
                        '${config.screenTimeThreshold}秒',
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.purple),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // 提示语
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.message, size: 20, color: Colors.grey),
                      const SizedBox(width: 8),
                      const Text('提示语: ', style: TextStyle(fontSize: 16)),
                      Expanded(
                        child: Text(
                          config.message,
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.purple),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // 监控时段列表
                  const Row(
                    children: [
                      Icon(Icons.access_time, size: 20, color: Colors.grey),
                      SizedBox(width: 8),
                      Text('监控时段:', style: TextStyle(fontSize: 16)),
                    ],
                  ),
                  const SizedBox(height: 8),
                  ...config.timeRanges.map((range) => Padding(
                    padding: const EdgeInsets.only(left: 28, bottom: 4),
                    child: Text(
                      '• ${range.toString()}',
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  )),

                  const SizedBox(height: 16),
                  // 编辑按钮
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: _editBehaviorConfig,
                      icon: const Icon(Icons.edit),
                      label: const Text('编辑配置'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.purple,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

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
                      'Type B 行为监控使用单个配置支持多个监控时段。\n在设置的时段内，如果连续亮屏时间超过阈值，将弹出提醒。',
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

  /// 浮动操作按钮（根据当前 Tab 显示不同功能）
  Widget? _buildFloatingActionButton() {
    if (_tabController.index == 0) {
      // Tab 1: 添加定时提醒
      return FloatingActionButton(
        onPressed: () => _editTimeAlarm(),
        tooltip: '添加定时提醒',
        child: const Icon(Icons.add),
      );
    } else {
      // Tab 2: 行为监控没有 FAB（不支持添加多个配置）
      return null;
    }
  }
}
