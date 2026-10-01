package com.example.dice

import com.example.dice.model.RollResult

/** Display values shared by the screen and isolated acceptance fixtures. */
object MainPresentation {
    data class Result(
        val diceLabel: String,
        val faces: List<Int>,
        val footerLabel: String?,
        val footerValue: String?,
        val excludedLabel: String?
    )

    data class Round(
        val diceLabel: String,
        val state: String,
        val drawn: Int,
        val remaining: Int,
        val total: Int
    ) {
        val percent: Int get() = drawn * 100 / total
    }

    fun result(value: RollResult): Result = Result(
        "${value.dice.count}d${value.dice.sides}",
        value.rolls.toList(),
        if (value.event != null) "事件" else if (value.dice.count > 1) "总和" else null,
        value.event ?: value.sum.toString().takeIf { value.dice.count > 1 },
        value.excludedFaces.takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { "本次排除面值：$it" }
    )

    fun round(value: DrawRound?, enabled: Boolean): Round? = value?.let {
        Round("${it.dice.count}d${it.dice.sides}",
            if (it.complete) "本轮已抽完；下次开启新一轮" else if (enabled) "进行中" else "已暂停",
            it.drawn.size, it.remaining, it.dice.sides)
    }

    fun columns(availableWidth: Int, cellWidth: Int, maximum: Int): Int =
        (availableWidth.coerceAtLeast(1) / cellWidth.coerceAtLeast(1)).coerceIn(1, maximum)
}
