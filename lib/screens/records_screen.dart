import 'package:flutter/material.dart';
import '../services/native_bridge.dart';
import 'edit_record_screen.dart';
import 'record_answers_screen.dart';

class RecordsScreen extends StatefulWidget {
  const RecordsScreen({Key? key}) : super(key: key);

  @override
  State<RecordsScreen> createState() => _RecordsScreenState();
}

class _RecordsScreenState extends State<RecordsScreen> {
  List<RecordConfig> _recordConfigs = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    try {
      final configs = await NativeBridge.getRecordConfigs();
      setState(() {
        _recordConfigs = configs;
        _loading = false;
      });
    } catch (e) {
      print('加载记录配置失败: $e');
      setState(() => _loading = false);
    }
  }

  Future<void> _deleteRecordConfig(RecordConfig config) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除记录配置 "${config.title}" 吗？\n关联的答案数据也将被删除。'),
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
      final success = await NativeBridge.deleteRecordConfig(config.uuid);
      if (success) {
        await _loadData();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('删除成功')),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('删除失败')),
          );
        }
      }
    }
  }

  Future<void> _editRecordConfig([RecordConfig? config]) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => EditRecordScreen(config: config),
      ),
    );

    if (result == true) {
      await _loadData();
    }
  }

  Future<void> _viewAnswers(RecordConfig config) async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => RecordAnswersScreen(recordConfig: config),
      ),
    );
  }

  String _getTriggerDescription(RecordConfig config) {
    switch (config.triggerType) {
      case TriggerType.time:
        return '定时提醒触发';
      case TriggerType.behavior:
        return '行为监控触发';
    }
  }

  IconData _getTriggerIcon(RecordConfig config) {
    switch (config.triggerType) {
      case TriggerType.time:
        return Icons.access_time;
      case TriggerType.behavior:
        return Icons.visibility;
    }
  }

  Color _getTriggerColor(RecordConfig config) {
    switch (config.triggerType) {
      case TriggerType.time:
        return Colors.blue;
      case TriggerType.behavior:
        return Colors.purple;
    }
  }

  @override
  Widget build(BuildContext context) {
    // 当嵌套在 TabView 中时，不显示 AppBar（由父页面控制）
    return _buildBody();
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_recordConfigs.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.assignment_outlined, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              '暂无记录配置',
              style: TextStyle(fontSize: 18, color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              '点击下方按钮添加记录任务',
              style: TextStyle(fontSize: 14, color: Colors.grey[500]),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () => _editRecordConfig(),
              icon: const Icon(Icons.add),
              label: const Text('添加记录配置'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _recordConfigs.length + 1, // +1 for header button
        itemBuilder: (context, index) {
          // 第一项：添加按钮
          if (index == 0) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Card(
                color: Colors.blue.shade50,
                child: InkWell(
                  onTap: () => _editRecordConfig(),
                  borderRadius: BorderRadius.circular(12),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.add_circle, color: Colors.blue.shade700),
                        const SizedBox(width: 8),
                        Text(
                          '添加新的记录配置',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Colors.blue.shade700,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            );
          }

          // 其他项：记录配置卡片
          final config = _recordConfigs[index - 1];
          return _buildRecordCard(config);
        },
      ),
    );
  }

  Widget _buildRecordCard(RecordConfig config) {
    final triggerColor = _getTriggerColor(config);
    final triggerIcon = _getTriggerIcon(config);
    final triggerDesc = _getTriggerDescription(config);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: triggerColor.withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(
            triggerIcon,
            color: triggerColor,
            size: 32,
          ),
        ),
        title: Text(
          config.title,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.black87,
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.label, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    triggerDesc,
                    style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                  ),
                ],
              ),
              const SizedBox(height: 2),
              Row(
                children: [
                  Icon(Icons.help_outline, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    '${config.questions.length} 个问题',
                    style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                  ),
                ],
              ),
            ],
          ),
        ),
        trailing: PopupMenuButton<String>(
          onSelected: (value) {
            if (value == 'edit') {
              _editRecordConfig(config);
            } else if (value == 'delete') {
              _deleteRecordConfig(config);
            } else if (value == 'answers') {
              _viewAnswers(config);
            }
          },
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'answers',
              child: Row(
                children: [
                  Icon(Icons.assessment, size: 20, color: Colors.green),
                  SizedBox(width: 8),
                  Text('查看答案'),
                ],
              ),
            ),
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
        onTap: () => _editRecordConfig(config),
      ),
    );
  }
}
