package com.example.dice

import androidx.lifecycle.MutableLiveData
import com.example.dice.model.RollResult

object HistoryStore {
    val results = MutableLiveData<List<RollResult>>(emptyList())

    fun add(result: RollResult) {
        val current = results.value ?: emptyList()
        results.value = listOf(result) + current
    }

    fun clear() {
        results.value = emptyList()
    }
}
