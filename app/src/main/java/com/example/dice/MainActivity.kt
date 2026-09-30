package com.example.dice

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.example.dice.databinding.ActivityMainBinding
import com.example.dice.databinding.SheetSettingsBinding
import com.example.dice.model.RollResult
import android.app.AlertDialog
import android.content.Intent

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsBinding: SheetSettingsBinding
    private lateinit var settingsDialog: com.google.android.material.bottomsheet.BottomSheetDialog
    private val vm: MainViewModel by viewModels()

    private var selectedCount = 1
    private var selectedSides = 6
    private var resultTextSize = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        settingsDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        settingsBinding = SheetSettingsBinding.inflate(settingsDialog.layoutInflater)
        settingsDialog.setContentView(settingsBinding.root)
        settingsDialog.setOnDismissListener {
            binding.btnSettings.requestFocus()
            androidx.core.view.ViewCompat.performAccessibilityAction(binding.btnSettings,
                androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null)
        }
        binding.btnSettings.setOnClickListener {
            settingsDialog.behavior.maxHeight = (binding.root.height * 0.9f).toInt()
            settingsDialog.show()
            settingsDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        settingsBinding.btnClose.setOnClickListener { settingsDialog.dismiss() }
        binding.btnRoundDetails.setOnClickListener { showRoundDetails() }
        binding.resultGrid.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) renderResult(vm.current.value)
        }

        vm.current.observe(this) { r ->
            renderResult(r)
            if (r != null) binding.tvResultDice.announceForAccessibility(ResultFormatter.format(r))
        }

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lotteryEnabled = prefs.getBoolean("lottery_mode_enabled", true)
        settingsBinding.switchLottery.isChecked = lotteryEnabled
        settingsBinding.switchLottery.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("lottery_mode_enabled", isChecked).apply()
            renderModes()
        }
        settingsBinding.switchExclusion.setOnCheckedChangeListener { _, _ -> renderModes() }

        val savedRound = runCatching {
            val count = prefs.getInt("round_count", 0)
            if (count == 0) null else DrawRound(
                com.example.dice.model.Dice(count, prefs.getInt("round_sides", 0)),
                prefs.getString("round_drawn", "")!!.split(',').filter { it.isNotBlank() }.map { it.toInt() }.toSet()
            )
        }.getOrNull()
        vm.restoreRound(savedRound)
        settingsBinding.switchNoRepeat.isChecked = prefs.getBoolean("no_repeat", false)
        settingsBinding.switchNoRepeat.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("no_repeat", checked).apply()
            renderRound()
            renderModes()
        }
        settingsBinding.btnClearRound.setOnClickListener { vm.clearRound() }
        vm.round.observe(this) { round ->
            prefs.edit().putInt("round_count", round?.dice?.count ?: 0)
                .putInt("round_sides", round?.dice?.sides ?: 0)
                .putString("round_drawn", round?.drawn?.sorted()?.joinToString(",") ?: "").apply()
            renderRound()
            renderModes()
        }

        selectedCount = prefs.getInt("selected_count", 1).takeIf { it in 1..10 } ?: 1
        selectedSides = prefs.getInt("selected_sides", 6).takeIf { it in 2..100 } ?: 6
        setupOptions(binding.countOptions, listOf(1, 2), true)
        setupSideOptions()
        binding.btnCustomCount.setOnClickListener { showNumberDialog(true) }
        binding.btnCustomSides.setOnClickListener { showNumberDialog(false) }
        binding.btnRoll.setOnClickListener { vibrate(); startRoll(selectedCount, selectedSides, selectedCount == 1) }
        renderSelection()
        binding.btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }

        val base = 18
        val resultSize = prefs.getInt("result_text_size_sp", 30).coerceIn(18, 40)
        val sumSize = prefs.getInt("sum_text_size_sp", 24).coerceIn(18, 40)
        resultTextSize = resultSize
        settingsBinding.sbResultSize.contentDescription = "结果字号"
        settingsBinding.sbSumSize.contentDescription = "总和或事件字号"
        settingsBinding.sbResultSize.progress = resultSize - base
        settingsBinding.sbSumSize.progress = sumSize - base
        settingsBinding.tvResultSizeLabel.text = "结果字号：${resultSize}sp"
        settingsBinding.tvSumSizeLabel.text = "总和 / 事件字号：${sumSize}sp"
        renderResult(vm.current.value)
        binding.tvSumLine.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sumSize.toFloat())
        settingsBinding.tvFontPreview.textSize = sumSize.toFloat()
        settingsBinding.tvResultFontPreview.textSize = resultSize.toFloat()

        settingsBinding.sbResultSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val size = base + progress
                resultTextSize = size
                settingsBinding.tvResultSizeLabel.text = "结果字号：${size}sp"
                renderResult(vm.current.value)
                settingsBinding.tvResultFontPreview.textSize = size.toFloat()
                prefs.edit().putInt("result_text_size_sp", size).apply()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        settingsBinding.sbSumSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val size = base + progress
                settingsBinding.tvSumSizeLabel.text = "总和 / 事件字号：${size}sp"
                binding.tvSumLine.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                settingsBinding.tvFontPreview.textSize = size.toFloat()
                prefs.edit().putInt("sum_text_size_sp", size).apply()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun startRoll(count: Int, sides: Int, eventCapable: Boolean = false) {
        if (!guard { vm.available(count, sides, emptySet(), settingsBinding.switchNoRepeat.isChecked) }) return
        if (!settingsBinding.switchExclusion.isChecked) {
            finishRoll(count, sides, eventCapable, emptySet())
            return
        }
        val selected = sortedSetOf<Int>()
        val labels = Array(sides) { (it + 1).toString() }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.exclusion_dialog_title)
            .setMultiChoiceItems(labels, null) { dialogInterface, index, checked ->
                val face = index + 1
                if (checked && selected.size == sides - 1) {
                    (dialogInterface as AlertDialog).listView.setItemChecked(index, false)
                    android.widget.Toast.makeText(this, R.string.exclusion_limit, android.widget.Toast.LENGTH_SHORT).show()
                } else if (checked) {
                    selected.add(face)
                } else {
                    selected.remove(face)
                }
            }
            .setPositiveButton(R.string.continue_roll, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val excludedFaces = selected.toSet()
                if (guard { vm.available(count, sides, excludedFaces, settingsBinding.switchNoRepeat.isChecked) }) {
                    dialog.dismiss()
                    finishRoll(count, sides, eventCapable, excludedFaces)
                }
            }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun finishRoll(count: Int, sides: Int, eventCapable: Boolean, excludedFaces: Set<Int>) {
        if (eventCapable && settingsBinding.switchLottery.isChecked) {
            showEventDialog(count, sides, excludedFaces)
        } else {
            commitRoll(count, sides, excludedFaces)
        }
    }

    private fun showEventDialog(count: Int, sides: Int, excludedFaces: Set<Int>) {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val inputs = linkedMapOf<Int, com.google.android.material.textfield.TextInputEditText>()
        vm.available(count, sides, excludedFaces, settingsBinding.switchNoRepeat.isChecked).forEach { face ->
            val layout = com.google.android.material.textfield.TextInputLayout(this)
            layout.hint = getString(R.string.event_for_face, face)
            val edit = com.google.android.material.textfield.TextInputEditText(layout.context)
            edit.inputType = android.text.InputType.TYPE_CLASS_TEXT
            layout.addView(edit)
            container.addView(layout)
            inputs[face] = edit
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.event_dialog_title)
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton(R.string.draw_event, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            fun valid() = inputs.values.all { !it.text.isNullOrBlank() }
            val watcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) { ok.isEnabled = valid() }
            }
            inputs.values.forEach { it.addTextChangedListener(watcher) }
            ok.isEnabled = valid()
            ok.setOnClickListener {
                if (valid()) {
                    val events = inputs.mapValues { it.value.text.toString() }
                    commitRoll(count, sides, excludedFaces, events)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun renderRound() {
        val round = vm.round.value
        settingsBinding.btnClearRound.isEnabled = round?.drawn?.isNotEmpty() == true
        binding.btnRoundDetails.isEnabled = round?.drawn?.isNotEmpty() == true
        binding.tvRound.text = if (round == null) "本轮尚无已掷记录" else {
            val state = if (round.complete) "本轮已抽完；下次开启新一轮" else if (!settingsBinding.switchNoRepeat.isChecked) "已暂停" else "进行中"
            "${round.dice.count}d${round.dice.sides} · $state\n已抽 ${round.drawn.size} · 剩余 ${round.remaining}"
        }
    }

    private fun renderModes() {
        val event = if (settingsBinding.switchLottery.isChecked) {
            if (selectedCount == 1) "事件开" else "事件开（仅单骰）"
        } else "事件关"
        val exclusion = if (settingsBinding.switchExclusion.isChecked) "当次排除开" else "当次排除关"
        val repeat = if (settingsBinding.switchNoRepeat.isChecked) "不重复开" else if (vm.round.value != null) "不重复暂停" else "不重复关"
        binding.tvModes.text = "$event · $exclusion · $repeat"
    }

    private fun renderResult(result: RollResult?) {
        val visible = android.view.View.VISIBLE
        val gone = android.view.View.GONE
        binding.resultGrid.removeAllViews()
        binding.tvEmptyResult.visibility = if (result == null) visible else gone
        if (result == null) {
            binding.tvResultDice.text = "上次结果"
            binding.tvSumLine.visibility = gone
            binding.tvExcludedLine.visibility = gone
            return
        }
        binding.tvResultDice.text = "上次结果 · ${result.dice.count}d${result.dice.sides}"
        val paint = android.text.TextPaint().apply {
            textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, resultTextSize.toFloat(), resources.displayMetrics)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val longestFace = result.rolls.maxOf { paint.measureText(it.toString()).toInt() }
        val cellWidth = maxOf(dp(56), longestFace + dp(20))
        val availableWidth = binding.resultGrid.width.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels - dp(48))
        val columns = (availableWidth / cellWidth).coerceIn(1, minOf(5, result.rolls.size))
        binding.resultGrid.columnCount = columns
        // Preserve generation order, including the final row when there are ten dice.
        result.rolls.forEachIndexed { index, face ->
            val cell = android.widget.TextView(this).apply {
                text = face.toString()
                textSize = resultTextSize.toFloat()
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                minHeight = dp(48)
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setBackgroundResource(R.drawable.result_face)
                contentDescription = "第 ${index + 1} 颗骰子，点数 $face"
            }
            binding.resultGrid.addView(cell, android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                rowSpec = android.widget.GridLayout.spec(index / columns)
                columnSpec = android.widget.GridLayout.spec(index % columns, 1f)
                setMargins(dp(2), dp(4), dp(2), dp(2))
            })
        }
        binding.tvSumLine.text = result.event ?: "总和：${result.sum}"
        binding.tvSumLine.visibility = if (result.event != null || result.dice.count > 1) visible else gone
        binding.tvExcludedLine.text = "本次排除面值：${result.excludedFaces.joinToString(", ")}"
        binding.tvExcludedLine.visibility = if (result.excludedFaces.isEmpty()) gone else visible
    }

    private fun showRoundDetails() {
        val round = vm.round.value ?: return
        AlertDialog.Builder(this)
            .setTitle("${round.dice.count}d${round.dice.sides} · 已掷面值")
            .setMessage("按面值排序（非投掷顺序）\n${round.drawn.sorted().joinToString(", ")}\n\n已抽 ${round.drawn.size} · 剩余 ${round.remaining}")
            .setPositiveButton("关闭", null).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun guard(action: () -> Unit): Boolean = try {
        action(); true
    } catch (error: IllegalArgumentException) {
        android.widget.Toast.makeText(this, error.message, android.widget.Toast.LENGTH_LONG).show()
        false
    }

    private fun commitRoll(count: Int, sides: Int, excluded: Set<Int>, events: Map<Int, String> = emptyMap()) {
        val previous = vm.round.value
        val enabled = settingsBinding.switchNoRepeat.isChecked
        if (guard { vm.roll(count, sides, excluded, events, enabled) }) {
            if (enabled && (previous == null || previous.complete || previous.dice != com.example.dice.model.Dice(count, sides))) {
                android.widget.Toast.makeText(this, "已开始新一轮", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyToClipboard(result: RollResult) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("dice", ResultFormatter.format(result)))
    }

    private fun vibrate() {
        val vb = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            vb.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vb.vibrate(20)
        }
    }

    private fun setupOptions(group: android.widget.RadioGroup, values: List<Int>, count: Boolean) {
        values.forEach { value ->
            val option = android.widget.RadioButton(this).apply {
                id = android.view.View.generateViewId()
                tag = value
                text = value.toString()
                contentDescription = if (count) "$value 颗骰子" else "$value 个面"
                minHeight = (48 * resources.displayMetrics.density).toInt()
                layoutParams = android.widget.RadioGroup.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    if (count) selectedCount = value else selectedSides = value
                    renderSelection()
                }
            }
            group.addView(option)
        }
    }

    private fun setupSideOptions() {
        listOf(2, 3, 4, 6, 10, 12, 20, 100).forEachIndexed { index, value ->
            val option = android.widget.RadioButton(this).apply {
                id = android.view.View.generateViewId()
                tag = value
                text = value.toString()
                contentDescription = "$value 个面"
                minHeight = dp(48)
                setOnClickListener { selectedSides = value; renderSelection() }
            }
            binding.sideOptions.addView(option, android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                rowSpec = android.widget.GridLayout.spec(index / 4)
                columnSpec = android.widget.GridLayout.spec(index % 4, 1f)
            })
        }
    }

    private fun renderSelection() {
        fun check(group: android.widget.RadioGroup, value: Int) {
            group.clearCheck()
            for (index in 0 until group.childCount) {
                val option = group.getChildAt(index) as android.widget.RadioButton
                if (option.tag == value) group.check(option.id)
            }
        }
        check(binding.countOptions, selectedCount)
        for (index in 0 until binding.sideOptions.childCount) {
            val option = binding.sideOptions.getChildAt(index) as android.widget.RadioButton
            option.isChecked = option.tag == selectedSides
        }
        binding.btnCustomCount.text = if (selectedCount in listOf(1,2)) "自定义" else "已选 $selectedCount 颗"
        binding.btnCustomCount.contentDescription = "自定义骰子个数，当前 $selectedCount 颗"
        binding.btnCustomSides.text = if (selectedSides in listOf(2,3,4,6,10,12,20,100)) "自定义面数" else "自定义 · $selectedSides 面"
        binding.btnCustomSides.contentDescription = "自定义骰子面数，当前 $selectedSides 面"
        binding.tvSelection.text = "待投掷：${selectedCount}d$selectedSides"
        binding.btnRoll.text = "投掷 ${selectedCount}d$selectedSides"
        settingsBinding.tvEventHint.text = if (selectedCount == 1) "随机事件支持所有单骰；仅填写本次可抽到的面值。" else "随机事件仅支持单骰；当前多骰按点数投掷。"
        renderModes()
        getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putInt("selected_count", selectedCount).putInt("selected_sides", selectedSides).apply()
    }

    private fun showNumberDialog(count: Boolean) {
        val layout = com.google.android.material.textfield.TextInputLayout(this)
        layout.setPadding(40, 20, 40, 0)
        layout.hint = if (count) "骰子个数（1–10）" else "骰子面数（2–100）"
        val edit = com.google.android.material.textfield.TextInputEditText(layout.context)
        edit.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(edit)
        val range = if (count) 1..10 else 2..100
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (count) "自定义骰子个数" else "自定义骰子面数")
            .setView(layout).setPositiveButton(R.string.confirm, null)
            .setNegativeButton(R.string.cancel, null).create()
        dialog.setOnShowListener {
            val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            fun validate(): Int? {
                val value = edit.text?.toString()?.toIntOrNull()?.takeIf { it in range }
                layout.error = if (value == null) "请输入 ${range.first}–${range.last} 的整数" else null
                ok.isEnabled = value != null
                return value
            }
            edit.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) { validate() }
            })
            ok.isEnabled = false
            ok.setOnClickListener {
                val value = validate() ?: return@setOnClickListener
                if (count) selectedCount = value else selectedSides = value
                renderSelection()
                dialog.dismiss()
            }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDestroy() {
        settingsDialog.setOnDismissListener(null)
        settingsDialog.dismiss()
        super.onDestroy()
    }
}
