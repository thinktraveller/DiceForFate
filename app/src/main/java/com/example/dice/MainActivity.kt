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
import com.example.dice.model.RollResult
import android.app.AlertDialog
import android.content.Intent

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    private var selectedCount = 1
    private var selectedSides = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.current.observe(this) { r ->
            if (r != null) {
                binding.tvResultDice.text = "本次结果：${r.dice.count}d${r.dice.sides}"
                if (r.event != null) {
                    binding.tvResultLine.text = "点数: ${r.rolls.first()}"
                    binding.tvSumLine.text = r.event
                    binding.tvSumLine.visibility = android.view.View.VISIBLE
                } else {
                    val m = r.dice.count
                    if (m == 1) {
                        binding.tvResultLine.text = r.rolls.first().toString()
                        binding.tvSumLine.visibility = android.view.View.GONE
                    } else {
                        binding.tvResultLine.text = "结果: ${r.rolls.joinToString(", ")}"
                        binding.tvSumLine.text = "总和: ${r.sum}"
                        binding.tvSumLine.visibility = android.view.View.VISIBLE
                    }
                }
                binding.tvExcludedLine.visibility = if (r.excludedFaces.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                binding.tvExcludedLine.text = "排除面值: ${r.excludedFaces.joinToString(", ")}"
            }
        }

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lotteryEnabled = prefs.getBoolean("lottery_mode_enabled", true)
        binding.switchLottery.isChecked = lotteryEnabled
        binding.switchLottery.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("lottery_mode_enabled", isChecked).apply()
        }

        val savedRound = runCatching {
            val count = prefs.getInt("round_count", 0)
            if (count == 0) null else DrawRound(
                com.example.dice.model.Dice(count, prefs.getInt("round_sides", 0)),
                prefs.getString("round_drawn", "")!!.split(',').filter { it.isNotBlank() }.map { it.toInt() }.toSet()
            )
        }.getOrNull()
        vm.restoreRound(savedRound)
        binding.switchNoRepeat.isChecked = prefs.getBoolean("no_repeat", false)
        binding.switchNoRepeat.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("no_repeat", checked).apply()
            renderRound()
        }
        binding.btnClearRound.setOnClickListener { vm.clearRound() }
        vm.round.observe(this) { round ->
            prefs.edit().putInt("round_count", round?.dice?.count ?: 0)
                .putInt("round_sides", round?.dice?.sides ?: 0)
                .putString("round_drawn", round?.drawn?.sorted()?.joinToString(",") ?: "").apply()
            renderRound()
        }

        selectedCount = prefs.getInt("selected_count", 1).takeIf { it in 1..10 } ?: 1
        selectedSides = prefs.getInt("selected_sides", 6).takeIf { it in 2..100 } ?: 6
        setupOptions(binding.countOptions, listOf(1, 2), true)
        setupOptions(binding.sideOptions, listOf(2, 3, 4, 6, 10, 12, 20, 100), false)
        binding.btnCustomCount.setOnClickListener { showNumberDialog(true) }
        binding.btnCustomSides.setOnClickListener { showNumberDialog(false) }
        binding.btnRoll.setOnClickListener { vibrate(); startRoll(selectedCount, selectedSides, selectedCount == 1) }
        renderSelection()
        binding.btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }

        val base = 18
        val resultSize = prefs.getInt("result_text_size_sp", 30)
        val sumSize = prefs.getInt("sum_text_size_sp", 24)
        binding.sbResultSize.contentDescription = "结果字号"
        binding.sbSumSize.contentDescription = "总和或事件字号"
        binding.sbResultSize.progress = resultSize - base
        binding.sbSumSize.progress = sumSize - base
        binding.tvResultSizeLabel.text = getString(R.string.font_size_label, resultSize)
        binding.tvSumSizeLabel.text = getString(R.string.font_size_label, sumSize)
        binding.tvResultLine.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, resultSize.toFloat())
        binding.tvSumLine.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sumSize.toFloat())

        binding.sbResultSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val size = base + progress
                binding.tvResultSizeLabel.text = getString(R.string.font_size_label, size)
                binding.tvResultLine.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                prefs.edit().putInt("result_text_size_sp", size).apply()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.sbSumSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val size = base + progress
                binding.tvSumSizeLabel.text = getString(R.string.font_size_label, size)
                binding.tvSumLine.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                prefs.edit().putInt("sum_text_size_sp", size).apply()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun startRoll(count: Int, sides: Int, eventCapable: Boolean = false) {
        if (!guard { vm.available(count, sides, emptySet(), binding.switchNoRepeat.isChecked) }) return
        if (!binding.switchExclusion.isChecked) {
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
                if (guard { vm.available(count, sides, excludedFaces, binding.switchNoRepeat.isChecked) }) {
                    dialog.dismiss()
                    finishRoll(count, sides, eventCapable, excludedFaces)
                }
            }
        }
        dialog.show()
    }

    private fun finishRoll(count: Int, sides: Int, eventCapable: Boolean, excludedFaces: Set<Int>) {
        if (eventCapable && binding.switchLottery.isChecked) {
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
        vm.available(count, sides, excludedFaces, binding.switchNoRepeat.isChecked).forEach { face ->
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
    }

    private fun renderRound() {
        val round = vm.round.value
        binding.btnClearRound.isEnabled = round?.drawn?.isNotEmpty() == true
        binding.tvRound.text = if (round == null) "本轮尚无已掷记录" else {
            val state = if (round.complete) "本轮已抽完；下次开启新一轮" else if (!binding.switchNoRepeat.isChecked) "已暂停" else "进行中"
            "${round.dice.count}d${round.dice.sides} · $state\n已掷：${round.drawn.sorted().joinToString(", ")}\n剩余 ${round.remaining} 个面值"
        }
    }

    private fun guard(action: () -> Unit): Boolean = try {
        action(); true
    } catch (error: IllegalArgumentException) {
        android.widget.Toast.makeText(this, error.message, android.widget.Toast.LENGTH_LONG).show()
        false
    }

    private fun commitRoll(count: Int, sides: Int, excluded: Set<Int>, events: Map<Int, String> = emptyMap()) {
        val previous = vm.round.value
        val enabled = binding.switchNoRepeat.isChecked
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
                setOnClickListener {
                    if (count) selectedCount = value else selectedSides = value
                    renderSelection()
                }
            }
            group.addView(option)
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
        check(binding.sideOptions, selectedSides)
        binding.btnCustomCount.text = if (selectedCount in listOf(1,2)) "M · 自定义个数" else "M · 已选 $selectedCount 颗"
        binding.btnCustomSides.text = if (selectedSides in listOf(2,3,4,6,10,12,20,100)) "M · 自定义面数" else "M · 已选 $selectedSides 面"
        binding.tvSelection.text = "当前骰型：${selectedCount}d$selectedSides"
        binding.btnRoll.text = "投掷 ${selectedCount}d$selectedSides"
        binding.tvEventHint.text = if (selectedCount == 1) "随机事件支持所有单骰；仅填写本次可抽到的面值。" else "随机事件仅支持单骰；当前多骰按点数投掷。"
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
    }
}
