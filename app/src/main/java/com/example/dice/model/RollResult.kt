package com.example.dice.model

data class RollResult(
    val dice: Dice,
    val rolls: List<Int>,
    val sum: Int,
    val timestamp: Long,
    val event: String? = null
)
