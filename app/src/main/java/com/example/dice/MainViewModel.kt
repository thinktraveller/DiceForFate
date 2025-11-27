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

    fun roll(count: Int, sides: Int) {
        val result = diceRoller.roll(Dice(count, sides))
        HistoryStore.add(result)
        _current.value = result
    }
}
