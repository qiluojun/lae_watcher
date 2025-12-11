import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/native_bridge.dart';

class RecordAnswersScreen extends StatefulWidget {
  final RecordConfig? recordConfig;

  const RecordAnswersScreen({super.key, this.recordConfig});

  @override
  State<RecordAnswersScreen> createState() => _RecordAnswersScreenState();
}

class _RecordAnswersScreenState extends State<RecordAnswersScreen> {
  List<RecordAnswer> _answers = [];
  Map<String, RecordConfig> _configsMap = {};
  bool _loading = true;

  bool get _isFilteredByConfig => widget.recordConfig != null;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    try {
      final configs = await NativeBridge.getRecordConfigs();
      final configsMap = <String, RecordConfig>{};
      for (var config in configs) {
        configsMap[config.uuid] = config;
      }

      List<RecordAnswer> answers;
      if (_isFilteredByConfig) {
        answers = await NativeBridge.getRecordAnswers(recordId: widget.recordConfig!.uuid);
      } else {
        answers = await NativeBridge.getRecordAnswers();
      }

      answers.sort((a, b) => b.timestamp.compareTo(a.timestamp));

      setState(() {
        _configsMap = configsMap;
        _answers = answers;
        _loading = false;
      });
    } catch (e) {
      print('加载答案数据失败: $e');
      setState(() => _loading = false);
    }
  }

  Future<void> _deleteAnswer(RecordAnswer answer) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除 ${answer.formattedDateTime} 的答案吗？'),
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
      final success = await NativeBridge.deleteRecordAnswer(answer.timestamp);
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

  Future<void> _exportAnswers() async {
    try {
      final jsonString = await NativeBridge.exportRecordAnswers();

      await Clipboard.setData(ClipboardData(text: jsonString));

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('答案数据已复制到剪贴板（JSON格式）'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      print('导出答案失败: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('导出失败: $e')),
        );
      }
    }
  }

  void _viewAnswerDetail(RecordAnswer answer) {
    final config = _configsMap[answer.recordId];

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(config?.title ?? '答案详情'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  const Icon(Icons.access_time, size: 16, color: Colors.grey),
                  const SizedBox(width: 4),
                  Text(
                    answer.formattedDateTime,
                    style: const TextStyle(color: Colors.grey),
                  ),
                ],
              ),
              const Divider(height: 24),

              if (config != null) ...[
                ...config.questions.map((question) {
                  final answerValue = answer.answers[question.id];
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          question.title,
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          _formatAnswerValue(question, answerValue),
                          style: const TextStyle(fontSize: 14),
                        ),
                      ],
                    ),
                  );
                }),
              ] else ...[
                const Text('配置已删除，无法显示问题详情'),
                const SizedBox(height: 8),
                Text('原始答案数据: ${answer.answers}'),
              ],
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  String _formatAnswerValue(Question question, dynamic value) {
    if (value == null) return '未回答';

    switch (question.type) {
      case QuestionType.choice:
        if (question.isMultipleChoice && value is List) {
          return value.join(', ');
        }
        return value.toString();
      case QuestionType.text:
        return value.toString();
      case QuestionType.slider:
        return value.toString();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isFilteredByConfig ? '${widget.recordConfig!.title} - 答案' : '所有答案记录'),
        actions: [
          IconButton(
            icon: const Icon(Icons.file_download),
            tooltip: '导出数据',
            onPressed: _answers.isNotEmpty ? _exportAnswers : null,
          ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_answers.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.assessment_outlined, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              '暂无答案记录',
              style: TextStyle(fontSize: 18, color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              _isFilteredByConfig ? '此记录配置尚未收集到任何答案' : '所有记录配置均无答案',
              style: TextStyle(fontSize: 14, color: Colors.grey[500]),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _answers.length,
        itemBuilder: (context, index) {
          final answer = _answers[index];
          final config = _configsMap[answer.recordId];
          return _buildAnswerCard(answer, config);
        },
      ),
    );
  }

  Widget _buildAnswerCard(RecordAnswer answer, RecordConfig? config) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: Colors.green.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Icon(
            Icons.assessment,
            color: Colors.green,
            size: 32,
          ),
        ),
        title: Text(
          config?.title ?? '已删除的配置',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: config != null ? Colors.black87 : Colors.grey,
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(Icons.access_time, size: 14, color: Colors.grey),
                  const SizedBox(width: 4),
                  Text(
                    answer.formattedDateTime,
                    style: const TextStyle(fontSize: 13, color: Colors.grey),
                  ),
                ],
              ),
              const SizedBox(height: 2),
              Row(
                children: [
                  const Icon(Icons.question_answer, size: 14, color: Colors.grey),
                  const SizedBox(width: 4),
                  Text(
                    '${answer.answers.length} 个回答',
                    style: const TextStyle(fontSize: 13, color: Colors.grey),
                  ),
                ],
              ),
            ],
          ),
        ),
        trailing: IconButton(
          icon: const Icon(Icons.delete, color: Colors.red),
          onPressed: () => _deleteAnswer(answer),
        ),
        onTap: () => _viewAnswerDetail(answer),
      ),
    );
  }
}
