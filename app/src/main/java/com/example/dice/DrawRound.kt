package com.example.dice

import com.example.dice.model.Dice
import com.example.dice.model.RollResult

/** Immutable progress; preparing or cancelling a draw never mutates a round. */
data class DrawRound(val dice: Dice, val drawn: Set<Int> = emptySet()) {
    init {
        require(dice.count in 1..10 && dice.sides in 2..100 && dice.count < dice.sides)
        require(drawn.all { it in 1..dice.sides })
        require(drawn.size % dice.count == 0)
    }
    val remaining: Int get() = dice.sides - drawn.size
    val complete: Boolean get() = remaining < dice.count
}

class RoundRoller(private val roller: DiceRoller = DiceRoller()) {
    var round: DrawRound? = null
        private set

    fun restore(value: DrawRound?) { round = value }
    fun clear() { round = null }

    private fun base(dice: Dice): Set<Int> =
        round?.takeIf { it.dice == dice && !it.complete }?.drawn ?: emptySet()

    fun available(dice: Dice, excluded: Set<Int>, enabled: Boolean): List<Int> {
        require(dice.count in 1..10 && dice.sides in 2..100) { "骰型超出范围" }
        require(excluded.all { it in 1..dice.sides }) { "排除面值超出骰子范围" }
        require(!enabled || dice.count < dice.sides) { "不重复抽取要求骰子个数小于面数" }
        val used = if (enabled) base(dice) else emptySet()
        val faces = (1..dice.sides).filterNot { it in excluded || it in used }
        require(faces.size >= if (enabled) dice.count else 1) { "可选面值不足，请调整当次排除项" }
        return faces
    }

    fun roll(dice: Dice, excluded: Set<Int>, events: Map<Int, String>, enabled: Boolean): RollResult {
        val faces = available(dice, excluded, enabled)
        if (events.isNotEmpty()) {
            require(dice.count == 1) { "事件模式仅支持单颗骰子" }
            require(events.keys == faces.toSet()) { "事件必须覆盖所有可出现的面值" }
            require(events.values.all { it.isNotBlank() }) { "事件内容不能为空" }
        }
        val used = if (enabled) base(dice) else emptySet()
        val raw = roller.roll(dice, excluded + used, enabled)
        // Keep temporary exclusions distinct from round progress in history.
        val result = raw.copy(excludedFaces = excluded.sorted(), event = events[raw.rolls.first()])
        if (enabled) round = DrawRound(dice, used + result.rolls)
        else if (round?.dice != dice) round = null
        return result
    }
}
