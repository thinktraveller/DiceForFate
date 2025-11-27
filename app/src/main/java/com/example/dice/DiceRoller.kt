package com.example.dice

import com.example.dice.model.Dice
import com.example.dice.model.RollResult
import java.security.SecureRandom

class DiceRoller(private val rng: SecureRandom = SecureRandom()) {
    fun roll(dice: Dice): RollResult {
        require(dice.count > 0)
        require(dice.sides > 1)
        val rolls = MutableList(dice.count) { 1 + rng.nextInt(dice.sides) }
        val sum = rolls.sum()
        return RollResult(dice, rolls, sum, System.currentTimeMillis())
    }
}
