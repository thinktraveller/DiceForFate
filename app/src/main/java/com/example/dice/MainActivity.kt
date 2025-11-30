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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.current.observe(this) { r ->
            if (r != null) {
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
            }
        }

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lotteryEnabled = prefs.getBoolean("lottery_mode_enabled", true)
        binding.switchLottery.isChecked = lotteryEnabled
        binding.switchLottery.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("lottery_mode_enabled", isChecked).apply()
        }

        fun handleLottery(sides: Int) {
            if (!binding.switchLottery.isChecked) { vm.roll(1, sides); return }
            val container = android.widget.LinearLayout(this)
            container.orientation = android.widget.LinearLayout.VERTICAL
            container.setPadding(40, 20, 40, 0)
            val inputs = ArrayList<com.google.android.material.textfield.TextInputEditText>()
            repeat(sides) { idx ->
                val layout = com.google.android.material.textfield.TextInputLayout(this)
                layout.hint = "点数 ${idx + 1} 对应事件"
                val edit = com.google.android.material.textfield.TextInputEditText(layout.context)
                edit.inputType = android.text.InputType.TYPE_CLASS_TEXT
                layout.addView(edit)
                container.addView(layout)
                inputs.add(edit)
            }
            val dialog = AlertDialog.Builder(this)
                .setTitle("填写事件")
                .setView(container)
                .setPositiveButton("开始抽签", null)
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
            dialog.setOnShowListener {
                val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                fun validate(): Boolean {
                    return inputs.all { !it.text.isNullOrBlank() }
                }
                val watcher = object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) { ok.isEnabled = validate() }
                }
                inputs.forEach { it.addTextChangedListener(watcher) }
                ok.isEnabled = validate()
                ok.setOnClickListener {
                    if (validate()) {
                        vibrate()
                        val labels = inputs.map { it.text.toString() }
                        val result = DiceRoller().roll(com.example.dice.model.Dice(1, sides))
                        val rolled = result.rolls.first()
                        val eventText = labels[rolled - 1]
                        val withEvent = result.copy(event = eventText)
                        HistoryStore.add(withEvent)
                        vm.setCurrent(withEvent)
                        dialog.dismiss()
                    }
                }
            }
            dialog.show()
        }

        binding.btn1d2.setOnClickListener { vibrate(); handleLottery(2) }
        binding.btn1d3.setOnClickListener { vibrate(); handleLottery(3) }
        binding.btn1d4.setOnClickListener { vibrate(); handleLottery(4) }
        binding.btn1d6.setOnClickListener { vibrate(); handleLottery(6) }
        binding.btn1d10.setOnClickListener { vibrate(); vm.roll(1, 10) }
        binding.btn1d20.setOnClickListener { vibrate(); vm.roll(1, 20) }
        binding.btn1d100.setOnClickListener { vibrate(); vm.roll(1, 100) }
        binding.btn2d2.setOnClickListener { vibrate(); vm.roll(2, 2) }
        binding.btn2d3.setOnClickListener { vibrate(); vm.roll(2, 3) }
        binding.btn2d4.setOnClickListener { vibrate(); vm.roll(2, 4) }
        binding.btn2d6.setOnClickListener { vibrate(); vm.roll(2, 6) }
        binding.btnMdn.setOnClickListener { showMdnDialog() }
        binding.btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }

        val base = 18
        val resultSize = prefs.getInt("result_text_size_sp", 30)
        val sumSize = prefs.getInt("sum_text_size_sp", 24)
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

    private fun copyToClipboard(result: RollResult) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("dice", ResultFormatter.format(result)))
    }

    private fun vibrate() {
        val vb = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val effect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
        vb.vibrate(effect)
    }

    private fun showMdnDialog() {
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(40, 20, 40, 0)

        val mLayout = com.google.android.material.textfield.TextInputLayout(this)
        val mEdit = com.google.android.material.textfield.TextInputEditText(mLayout.context)
        mLayout.hint = getString(R.string.hint_m_range)
        mEdit.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        mLayout.addView(mEdit)

        val nLayout = com.google.android.material.textfield.TextInputLayout(this)
        val nEdit = com.google.android.material.textfield.TextInputEditText(nLayout.context)
        nLayout.hint = getString(R.string.hint_n_range)
        nEdit.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        nLayout.addView(nEdit)

        container.addView(mLayout)
        container.addView(nLayout)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.mdn))
            .setView(container)
            .setPositiveButton(getString(R.string.confirm), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ok.isEnabled = false

            fun validate(): Boolean {
                val m = mEdit.text?.toString()?.toIntOrNull()
                val n = nEdit.text?.toString()?.toIntOrNull()
                var valid = true
                if (m == null || m !in 2..10) {
                    mLayout.error = getString(R.string.invalid_input)
                    valid = false
                } else {
                    mLayout.error = null
                }
                if (n == null || n !in 2..100) {
                    nLayout.error = getString(R.string.invalid_input)
                    valid = false
                } else {
                    nLayout.error = null
                }
                ok.isEnabled = valid
                return valid
            }

            val watcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) { validate() }
            }

            mEdit.addTextChangedListener(watcher)
            nEdit.addTextChangedListener(watcher)

            ok.setOnClickListener {
                if (validate()) {
                    val mVal = mEdit.text?.toString()?.toInt() ?: return@setOnClickListener
                    val nVal = nEdit.text?.toString()?.toInt() ?: return@setOnClickListener
                    vibrate(); vm.roll(mVal, nVal)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }
}
