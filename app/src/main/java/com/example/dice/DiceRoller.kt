package com.example.dice

import com.example.dice.model.Dice
import com.example.dice.model.RollResult
import java.security.SecureRandom

class DiceRoller(private val rng: SecureRandom = SecureRandom()) {
    fun roll(dice: Dice, excludedFaces: Set<Int> = emptySet(), withoutReplacement: Boolean = false): RollResult {
        require(dice.count > 0)
        require(dice.sides > 1)
        require(excludedFaces.all { it in 1..dice.sides }) { "排除面值超出骰子范围" }
        require(excludedFaces.size < dice.sides) { "至少保留一个可掷出的面值" }
        val availableFaces = (1..dice.sides).filterNot { it in excludedFaces }.toMutableList()
        require(!withoutReplacement || availableFaces.size >= dice.count) { "可选面值不足，请调整当次排除项" }
        val rolls = MutableList(dice.count) {
            val index = rng.nextInt(availableFaces.size)
            if (withoutReplacement) availableFaces.removeAt(index) else availableFaces[index]
        }
        val sum = rolls.sum()
        return RollResult(dice, rolls, sum, System.currentTimeMillis(), excludedFaces = excludedFaces.sorted())
    }
}
