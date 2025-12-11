package com.example.lae_watcher.activities

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.lae_watcher.data.Question
import com.example.lae_watcher.data.QuestionType
import com.example.lae_watcher.data.RecordAnswer
import com.example.lae_watcher.utils.RecordAnswerManager
import com.example.lae_watcher.utils.RecordConfigManager

/**
 * RecordActivity - 问卷记录界面 (Phase 3)
 *
 * 功能:
 * - 根据 RecordConfig 动态渲染问题列表
 * - 支持三种题型: CHOICE (单选/多选), TEXT (文本输入), SLIDER (滑动打分)
 * - 收集用户答案并提交到 RecordAnswerManager
 *
 * 触发方式:
 * - AlarmActivity 检测到 recordId 后跳转
 */
class RecordActivity : Activity() {

    companion object {
        private const val TAG = "RecordActivity"
        const val EXTRA_RECORD_ID = "record_id"
    }

    private lateinit var recordId: String
    private lateinit var questions: List<Question>
    private val answers = mutableMapOf<String, Any>()  // questionId -> value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "========================================")
        Log.i(TAG, "📝 RecordActivity onCreate 被调用")

        // 读取 recordId
        recordId = intent.getStringExtra(EXTRA_RECORD_ID) ?: run {
            Log.e(TAG, "✗ 缺少 record_id 参数")
            finish()
            return
        }

        Log.i(TAG, "记录配置 UUID: $recordId")

        // 加载记录配置
        val configManager = RecordConfigManager(this)
        val config = configManager.getByUuid(recordId)

        if (config == null) {
            Log.e(TAG, "✗ 未找到记录配置: $recordId")
            Toast.makeText(this, "记录配置不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        questions = config.questions
        Log.i(TAG, "加载问题列表: ${questions.size} 个问题")

        // 动态构建 UI
        buildUI(config.title)

        Log.i(TAG, "✓ RecordActivity 初始化完成")
        Log.i(TAG, "========================================")
    }

    /**
     * 动态构建 UI
     */
    private fun buildUI(title: String) {
        // 创建主容器（垂直布局）
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            setBackgroundColor(Color.WHITE)
        }

        // 标题
        val titleText = TextView(this).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, dpToPx(16))
        }
        mainLayout.addView(titleText)

        // 创建可滚动容器
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f  // weight = 1
            )
        }

        val questionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 动态渲染问题
        questions.forEach { question ->
            val questionView = createQuestionView(question)
            questionsContainer.addView(questionView)

            // 添加分隔线
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(1)
                ).apply {
                    topMargin = dpToPx(16)
                    bottomMargin = dpToPx(16)
                }
                setBackgroundColor(Color.LTGRAY)
            }
            questionsContainer.addView(divider)
        }

        scrollView.addView(questionsContainer)
        mainLayout.addView(scrollView)

        // 提交按钮
        val submitButton = Button(this).apply {
            text = "提交"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(50)
            ).apply {
                topMargin = dpToPx(16)
            }
            setOnClickListener { submitAnswers() }
        }
        mainLayout.addView(submitButton)

        setContentView(mainLayout)
    }

    /**
     * 根据问题类型创建对应的 UI 组件
     */
    private fun createQuestionView(question: Question): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 问题标题
        val questionTitle = TextView(this).apply {
            text = question.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, dpToPx(8))
        }
        container.addView(questionTitle)

        // 根据类型添加输入控件
        when (question.type) {
            QuestionType.CHOICE -> addChoiceInput(container, question)
            QuestionType.TEXT -> addTextInput(container, question)
            QuestionType.SLIDER -> addSliderInput(container, question)
        }

        return container
    }

    /**
     * 添加选择题输入（单选/多选）
     */
    private fun addChoiceInput(container: LinearLayout, question: Question) {
        val options = question.options ?: return

        if (question.isMultipleChoice) {
            // 多选题
            val selectedOptions = mutableListOf<String>()

            options.forEach { option ->
                val checkBox = CheckBox(this).apply {
                    text = option
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedOptions.add(option)
                        } else {
                            selectedOptions.remove(option)
                        }
                        answers[question.id] = selectedOptions.toList()
                    }
                }
                container.addView(checkBox)
            }

            // 初始化空答案
            answers[question.id] = emptyList<String>()
        } else {
            // 单选题
            val radioGroup = RadioGroup(this).apply {
                orientation = RadioGroup.VERTICAL
            }

            options.forEach { option ->
                val radioButton = RadioButton(this).apply {
                    text = option
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                }
                radioGroup.addView(radioButton)
            }

            radioGroup.setOnCheckedChangeListener { group, checkedId ->
                val radioButton = group.findViewById<RadioButton>(checkedId)
                answers[question.id] = radioButton.text.toString()
            }

            container.addView(radioGroup)
        }
    }

    /**
     * 添加文本输入
     */
    private fun addTextInput(container: LinearLayout, question: Question) {
        val editText = EditText(this).apply {
            hint = "请输入答案..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 5
            setBackgroundColor(Color.parseColor("#F0F0F0"))

            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    answers[question.id] = s.toString()
                }
            })
        }

        container.addView(editText)

        // 初始化空答案
        answers[question.id] = ""
    }

    /**
     * 添加滑动打分输入
     */
    private fun addSliderInput(container: LinearLayout, question: Question) {
        val minValue = question.min ?: 0
        val maxValue = question.max ?: 10
        val stepValue = question.step ?: 1

        // 当前值显示
        val valueText = TextView(this).apply {
            text = "当前值: $minValue"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        container.addView(valueText)

        // 滑动条
        val seekBar = SeekBar(this).apply {
            max = (maxValue - minValue) / stepValue
            progress = 0
            setPadding(dpToPx(8), 0, dpToPx(8), 0)

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = minValue + (progress * stepValue)
                    valueText.text = "当前值: $value"
                    answers[question.id] = value
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        container.addView(seekBar)

        // 范围提示
        val rangeText = TextView(this).apply {
            text = "范围: $minValue - $maxValue"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, 0)
        }
        container.addView(rangeText)

        // 初始化默认答案
        answers[question.id] = minValue
    }

    /**
     * 提交答案
     */
    private fun submitAnswers() {
        Log.d(TAG, "→ 开始提交答案...")

        // 验证所有问题都已回答
        val unansweredQuestions = questions.filter { question ->
            val answer = answers[question.id]
            when (question.type) {
                QuestionType.CHOICE -> {
                    if (question.isMultipleChoice) {
                        (answer as? List<*>)?.isEmpty() != false
                    } else {
                        answer == null
                    }
                }
                QuestionType.TEXT -> (answer as? String)?.isEmpty() != false
                QuestionType.SLIDER -> answer == null
            }
        }

        if (unansweredQuestions.isNotEmpty()) {
            val unansweredTitles = unansweredQuestions.joinToString(", ") { it.title }
            Toast.makeText(this, "请回答所有问题:\n$unansweredTitles", Toast.LENGTH_LONG).show()
            Log.w(TAG, "有未回答的问题: ${unansweredQuestions.size} 个")
            return
        }

        // 创建答案记录
        val recordAnswer = RecordAnswer.create(recordId, answers.toMap())

        // 保存答案
        val answerManager = RecordAnswerManager(this)
        val success = answerManager.add(recordAnswer)

        if (success) {
            Log.i(TAG, "✓ 答案提交成功")
            Log.d(TAG, "答案内容: ${answers.entries.joinToString { "${it.key}=${it.value}" }}")
            Toast.makeText(this, "提交成功！", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Log.e(TAG, "✗ 答案提交失败")
            Toast.makeText(this, "提交失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * dp 转 px
     */
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
