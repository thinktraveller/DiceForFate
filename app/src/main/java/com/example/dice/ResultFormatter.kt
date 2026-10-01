package com.example.dice

import com.example.dice.model.RollResult

object ResultFormatter {
    fun format(result: RollResult): String {
        val body = if (result.dice.count == 1) {
            result.rolls.first().toString()
        } else {
            val rollsText = "结果: ${result.rolls.joinToString(", ")}"
            val sumText = "总和: ${result.sum}"
            "$rollsText\n$sumText"
        }
        return if (result.excludedFaces.isEmpty()) body else "$body\n排除面值: ${result.excludedFaces.joinToString(", ")}"
    }
}
