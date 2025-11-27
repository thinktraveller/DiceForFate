package com.example.dice

import com.example.dice.model.Dice
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollerTest {
    @Test
    fun rollWithinRange() {
        val roller = DiceRoller()
        val result = roller.roll(Dice(2, 6))
        assertTrue(result.rolls.size == 2)
        assertTrue(result.rolls.all { it in 1..6 })
    }

    @Test
    fun distributionBasicCheck() {
        val roller = DiceRoller()
        val counts = IntArray(6)
        repeat(6000) {
            val r = roller.roll(Dice(1, 6))
            counts[r.rolls.first() - 1]++
        }
        counts.forEach { c ->
            assertTrue(c in 800..1200)
        }
    }
}
