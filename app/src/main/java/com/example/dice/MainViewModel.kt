package com.example.dice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dice.model.Dice
import com.example.dice.model.RollResult

class MainViewModel : ViewModel() {
    private val engine = RoundRoller()
    private var restored = false
    private val _current = MutableLiveData<RollResult?>(null)
    val current: LiveData<RollResult?> = _current
    private val _round = MutableLiveData<DrawRound?>(null)
    val round: LiveData<DrawRound?> = _round

    fun restoreRound(value: DrawRound?) {
        if (restored) return
        restored = true
        engine.restore(value)
        _round.value = value
    }

    fun clearRound() {
        engine.clear()
        _round.value = null
    }

    fun available(count: Int, sides: Int, excluded: Set<Int>, enabled: Boolean): List<Int> =
        engine.available(Dice(count, sides), excluded, enabled)

    fun roll(count: Int, sides: Int, excludedFaces: Set<Int> = emptySet(), events: Map<Int, String> = emptyMap(), noRepeat: Boolean = false) {
        val result = engine.roll(Dice(count, sides), excludedFaces, events, noRepeat)
        HistoryStore.add(result)
        _round.value = engine.round
        _current.value = result
    }

    fun setCurrent(result: RollResult) { _current.value = result }
}
