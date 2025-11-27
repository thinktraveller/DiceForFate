package com.example.dice

import com.example.dice.model.RollResult

object ResultFormatter {
    fun format(result: RollResult): String {
        return if (result.dice.count == 1) {
            result.rolls.first().toString()
        } else {
            val rollsText = "结果: ${result.rolls.joinToString(", ")}"
            val sumText = "总和: ${result.sum}"
            "$rollsText\n$sumText"
        }
    }
}
