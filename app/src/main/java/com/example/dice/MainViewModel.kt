package com.example.dice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dice.model.Dice
import com.example.dice.model.RollResult

class MainViewModel : ViewModel() {
    private val diceRoller = DiceRoller()
    private val _current = MutableLiveData<RollResult?>(null)
    val current: LiveData<RollResult?> = _current

    fun roll(count: Int, sides: Int, excludedFaces: Set<Int> = emptySet(), events: Map<Int, String> = emptyMap()) {
        if (events.isNotEmpty()) {
            require(count == 1) { "事件模式仅支持单颗骰子" }
            require(events.keys == (1..sides).filterNot { it in excludedFaces }.toSet()) { "事件必须覆盖所有可出现的面值" }
            require(events.values.all { it.isNotBlank() }) { "事件内容不能为空" }
        }
        val baseResult = diceRoller.roll(Dice(count, sides), excludedFaces)
        val result = if (events.isEmpty()) baseResult else baseResult.copy(event = events.getValue(baseResult.rolls.first()))
        HistoryStore.add(result)
        _current.value = result
    }

    fun setCurrent(result: RollResult) {
        _current.value = result
    }
}
