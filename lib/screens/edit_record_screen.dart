import 'package:flutter/material.dart';
import '../services/native_bridge.dart';

class EditRecordScreen extends StatefulWidget {
  final RecordConfig? config;

  const EditRecordScreen({super.key, this.config});

  @override
  State<EditRecordScreen> createState() => _EditRecordScreenState();
}

class _EditRecordScreenState extends State<EditRecordScreen> {
  late TextEditingController _titleController;
  late TriggerType _selectedTriggerType;
  late String _selectedTriggerRefId;
  List<Question> _questions = [];
  bool _saving = false;

  List<TimeAlarmItem> _timeAlarms = [];
  bool _loadingTriggers = true;

  bool get _isEditMode => widget.config != null;

  @override
  void initState() {
    super.initState();

    if (_isEditMode) {
      final config = widget.config!;
      _titleController = TextEditingController(text: config.title);
      _selectedTriggerType = config.triggerType;
      _selectedTriggerRefId = config.triggerRefId;
      _questions = List.from(config.questions);
    } else {
      _titleController = TextEditingController(text: '日常记录');
      _selectedTriggerType = TriggerType.time;
      _selectedTriggerRefId = '';
    }

    _loadTriggers();
  }

  @override
  void dispose() {
    _titleController.dispose();
    super.dispose();
  }

  Future<void> _loadTriggers() async {
    setState(() => _loadingTriggers = true);
    try {
      final timeAlarms = await NativeBridge.getTimeAlarms();
      setState(() {
        _timeAlarms = timeAlarms;
        _loadingTriggers = false;

        if (!_isEditMode && _timeAlarms.isNotEmpty) {
          _selectedTriggerRefId = _timeAlarms.first.id;
        }
      });
    } catch (e) {
      print('加载触发源失败: $e');
      setState(() => _loadingTriggers = false);
    }
  }

  Future<void> _save() async {
    final title = _titleController.text.trim();
    if (title.isEmpty) {
      _showSnackBar('标题不能为空');
      return;
    }

    if (_selectedTriggerRefId.isEmpty) {
      _showSnackBar('请选择触发源');
      return;
    }

    if (_questions.isEmpty) {
      _showSnackBar('请至少添加一个问题');
      return;
    }

    setState(() => _saving = true);

    try {
      final config = RecordConfig(
        uuid: _isEditMode ? widget.config!.uuid : '',
        triggerType: _selectedTriggerType,
        triggerRefId: _selectedTriggerRefId,
        title: title,
        questions: _questions,
      );

      final success = await NativeBridge.saveRecordConfig(config);

      if (success) {
        if (mounted) {
          Navigator.pop(context, true);
        }
      } else {
        _showSnackBar('保存失败');
        setState(() => _saving = false);
      }
    } catch (e) {
      print('保存记录配置失败: $e');
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

  void _addQuestion() async {
    final result = await showDialog<Question>(
      context: context,
      builder: (context) => _QuestionEditorDialog(),
    );

    if (result != null) {
      setState(() {
        _questions.add(result);
      });
    }
  }

  void _editQuestion(int index) async {
    final result = await showDialog<Question>(
      context: context,
      builder: (context) => _QuestionEditorDialog(question: _questions[index]),
    );

    if (result != null) {
      setState(() {
        _questions[index] = result;
      });
    }
  }

  void _deleteQuestion(int index) {
    setState(() {
      _questions.removeAt(index);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditMode ? '编辑记录配置' : '新建记录配置'),
        actions: [
          if (_saving)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            )
          else
            IconButton(
              icon: const Icon(Icons.check),
              onPressed: _save,
            ),
        ],
      ),
      body: _loadingTriggers
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _buildTitleSection(),
                const SizedBox(height: 24),
                _buildTriggerSection(),
                const SizedBox(height: 24),
                _buildQuestionsSection(),
              ],
            ),
    );
  }

  Widget _buildTitleSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '记录标题',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _titleController,
              decoration: const InputDecoration(
                hintText: '例如：睡前状态记录',
                border: OutlineInputBorder(),
                contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 12),
              ),
              maxLength: 30,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTriggerSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '触发源设置',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),

            Row(
              children: [
                Expanded(
                  child: RadioListTile<TriggerType>(
                    title: const Text('定时提醒'),
                    value: TriggerType.time,
                    groupValue: _selectedTriggerType,
                    onChanged: (value) {
                      setState(() {
                        _selectedTriggerType = value!;
                        if (_timeAlarms.isNotEmpty) {
                          _selectedTriggerRefId = _timeAlarms.first.id;
                        }
                      });
                    },
                  ),
                ),
                Expanded(
                  child: RadioListTile<TriggerType>(
                    title: const Text('行为监控'),
                    value: TriggerType.behavior,
                    groupValue: _selectedTriggerType,
                    onChanged: (value) {
                      setState(() {
                        _selectedTriggerType = value!;
                        _selectedTriggerRefId = 'behavior';
                      });
                    },
                  ),
                ),
              ],
            ),

            const SizedBox(height: 12),

            if (_selectedTriggerType == TriggerType.time)
              _buildTimeAlarmSelector()
            else
              _buildBehaviorTriggerInfo(),
          ],
        ),
      ),
    );
  }

  Widget _buildTimeAlarmSelector() {
    if (_timeAlarms.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.orange.shade50,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(Icons.warning_amber, color: Colors.orange.shade700),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                '暂无定时提醒，请先在"定时提醒"页面添加',
                style: TextStyle(color: Colors.orange.shade700),
              ),
            ),
          ],
        ),
      );
    }

    return DropdownButtonFormField<String>(
      value: _selectedTriggerRefId.isEmpty ? null : _selectedTriggerRefId,
      decoration: const InputDecoration(
        labelText: '选择定时提醒',
        border: OutlineInputBorder(),
      ),
      items: _timeAlarms.map((alarm) {
        return DropdownMenuItem(
          value: alarm.id,
          child: Text('${alarm.formattedTime} - ${alarm.message}'),
        );
      }).toList(),
      onChanged: (value) {
        setState(() {
          _selectedTriggerRefId = value!;
        });
      },
    );
  }

  Widget _buildBehaviorTriggerInfo() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.blue.shade50,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Icon(Icons.info_outline, color: Colors.blue.shade700),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              '记录将在行为监控触发时弹出',
              style: TextStyle(color: Colors.blue.shade700),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuestionsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  '问题列表',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                TextButton.icon(
                  onPressed: _addQuestion,
                  icon: const Icon(Icons.add),
                  label: const Text('添加问题'),
                ),
              ],
            ),
            const SizedBox(height: 12),

            if (_questions.isEmpty)
              Container(
                padding: const EdgeInsets.all(24),
                alignment: Alignment.center,
                child: Column(
                  children: [
                    Icon(Icons.help_outline, size: 48, color: Colors.grey[400]),
                    const SizedBox(height: 8),
                    Text(
                      '暂无问题，点击右上角添加',
                      style: TextStyle(color: Colors.grey[600]),
                    ),
                  ],
                ),
              )
            else
              ..._questions.asMap().entries.map((entry) {
                final index = entry.key;
                final question = entry.value;
                return _buildQuestionItem(index, question);
              }),
          ],
        ),
      ),
    );
  }

  Widget _buildQuestionItem(int index, Question question) {
    String typeLabel;
    IconData typeIcon;

    switch (question.type) {
      case QuestionType.choice:
        typeLabel = question.isMultipleChoice ? '多选题' : '单选题';
        typeIcon = Icons.radio_button_checked;
        break;
      case QuestionType.text:
        typeLabel = '文本题';
        typeIcon = Icons.text_fields;
        break;
      case QuestionType.slider:
        typeLabel = '滑动打分';
        typeIcon = Icons.linear_scale;
        break;
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: Colors.grey.shade50,
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Colors.blue.shade100,
          child: Text(
            '${index + 1}',
            style: const TextStyle(color: Colors.blue, fontWeight: FontWeight.bold),
          ),
        ),
        title: Text(
          question.title,
          style: const TextStyle(fontWeight: FontWeight.w500),
        ),
        subtitle: Row(
          children: [
            Icon(typeIcon, size: 14, color: Colors.grey[600]),
            const SizedBox(width: 4),
            Text(typeLabel, style: TextStyle(color: Colors.grey[600])),
          ],
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(Icons.edit, size: 20),
              onPressed: () => _editQuestion(index),
            ),
            IconButton(
              icon: const Icon(Icons.delete, size: 20, color: Colors.red),
              onPressed: () => _deleteQuestion(index),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuestionEditorDialog extends StatefulWidget {
  final Question? question;

  const _QuestionEditorDialog({this.question});

  @override
  State<_QuestionEditorDialog> createState() => _QuestionEditorDialogState();
}

class _QuestionEditorDialogState extends State<_QuestionEditorDialog> {
  late TextEditingController _titleController;
  late QuestionType _selectedType;
  late bool _isMultipleChoice;
  late List<String> _options;
  late int _minValue;
  late int _maxValue;
  late int _stepValue;

  bool get _isEditMode => widget.question != null;

  @override
  void initState() {
    super.initState();

    if (_isEditMode) {
      final q = widget.question!;
      _titleController = TextEditingController(text: q.title);
      _selectedType = q.type;
      _isMultipleChoice = q.isMultipleChoice;
      _options = q.options != null ? List.from(q.options!) : ['选项1', '选项2'];
      _minValue = q.min ?? 0;
      _maxValue = q.max ?? 10;
      _stepValue = q.step ?? 1;
    } else {
      _titleController = TextEditingController();
      _selectedType = QuestionType.choice;
      _isMultipleChoice = false;
      _options = ['选项1', '选项2'];
      _minValue = 0;
      _maxValue = 10;
      _stepValue = 1;
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    super.dispose();
  }

  void _save() {
    final title = _titleController.text.trim();
    if (title.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('问题标题不能为空')),
      );
      return;
    }

    Question question;
    final id = _isEditMode ? widget.question!.id : DateTime.now().millisecondsSinceEpoch.toString();

    switch (_selectedType) {
      case QuestionType.choice:
        if (_options.isEmpty || _options.any((o) => o.trim().isEmpty)) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('选项不能为空')),
          );
          return;
        }
        question = Question.choice(
          id: id,
          title: title,
          options: _options,
          isMultipleChoice: _isMultipleChoice,
        );
        break;
      case QuestionType.text:
        question = Question.text(id: id, title: title);
        break;
      case QuestionType.slider:
        question = Question.slider(
          id: id,
          title: title,
          min: _minValue,
          max: _maxValue,
          step: _stepValue,
        );
        break;
    }

    Navigator.pop(context, question);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(_isEditMode ? '编辑问题' : '添加问题'),
      content: SingleChildScrollView(
        child: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: _titleController,
                decoration: const InputDecoration(
                  labelText: '问题标题',
                  border: OutlineInputBorder(),
                ),
                maxLength: 100,
              ),
              const SizedBox(height: 16),

              const Text('问题类型', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),

              SegmentedButton<QuestionType>(
                segments: const [
                  ButtonSegment(value: QuestionType.choice, label: Text('选择题'), icon: Icon(Icons.radio_button_checked)),
                  ButtonSegment(value: QuestionType.text, label: Text('文本'), icon: Icon(Icons.text_fields)),
                  ButtonSegment(value: QuestionType.slider, label: Text('打分'), icon: Icon(Icons.linear_scale)),
                ],
                selected: {_selectedType},
                onSelectionChanged: (Set<QuestionType> newSelection) {
                  setState(() {
                    _selectedType = newSelection.first;
                  });
                },
              ),

              const SizedBox(height: 16),

              if (_selectedType == QuestionType.choice) ...[
                SwitchListTile(
                  title: const Text('允许多选'),
                  value: _isMultipleChoice,
                  onChanged: (value) {
                    setState(() {
                      _isMultipleChoice = value;
                    });
                  },
                ),
                const SizedBox(height: 8),
                ..._options.asMap().entries.map((entry) {
                  final index = entry.key;
                  final option = entry.value;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      children: [
                        Expanded(
                          child: TextField(
                            decoration: InputDecoration(
                              labelText: '选项 ${index + 1}',
                              border: const OutlineInputBorder(),
                            ),
                            controller: TextEditingController(text: option)
                              ..selection = TextSelection.fromPosition(
                                TextPosition(offset: option.length),
                              ),
                            onChanged: (value) {
                              _options[index] = value;
                            },
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.delete, color: Colors.red),
                          onPressed: _options.length > 1
                              ? () {
                                  setState(() {
                                    _options.removeAt(index);
                                  });
                                }
                              : null,
                        ),
                      ],
                    ),
                  );
                }),
                TextButton.icon(
                  onPressed: () {
                    setState(() {
                      _options.add('选项${_options.length + 1}');
                    });
                  },
                  icon: const Icon(Icons.add),
                  label: const Text('添加选项'),
                ),
              ] else if (_selectedType == QuestionType.slider) ...[
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        decoration: const InputDecoration(
                          labelText: '最小值',
                          border: OutlineInputBorder(),
                        ),
                        keyboardType: TextInputType.number,
                        controller: TextEditingController(text: _minValue.toString())
                          ..selection = TextSelection.fromPosition(
                            TextPosition(offset: _minValue.toString().length),
                          ),
                        onChanged: (value) {
                          _minValue = int.tryParse(value) ?? 0;
                        },
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        decoration: const InputDecoration(
                          labelText: '最大值',
                          border: OutlineInputBorder(),
                        ),
                        keyboardType: TextInputType.number,
                        controller: TextEditingController(text: _maxValue.toString())
                          ..selection = TextSelection.fromPosition(
                            TextPosition(offset: _maxValue.toString().length),
                          ),
                        onChanged: (value) {
                          _maxValue = int.tryParse(value) ?? 10;
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                TextField(
                  decoration: const InputDecoration(
                    labelText: '步长',
                    border: OutlineInputBorder(),
                  ),
                  keyboardType: TextInputType.number,
                  controller: TextEditingController(text: _stepValue.toString())
                    ..selection = TextSelection.fromPosition(
                      TextPosition(offset: _stepValue.toString().length),
                    ),
                  onChanged: (value) {
                    _stepValue = int.tryParse(value) ?? 1;
                  },
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('取消'),
        ),
        ElevatedButton(
          onPressed: _save,
          child: const Text('保存'),
        ),
      ],
    );
  }
}
