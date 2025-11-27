package com.example.dice

import com.example.dice.model.Dice
import com.example.dice.model.RollResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultFormatterTest {
    @Test
    fun format_m1_showsSingle() {
        val r = RollResult(Dice(1, 6), listOf(4), 4, 0)
        assertEquals("4", ResultFormatter.format(r))
    }

    @Test
    fun format_m2_showsListAndSum() {
        val r = RollResult(Dice(2, 6), listOf(3, 5), 8, 0)
        assertEquals("结果: 3, 5\n总和: 8", ResultFormatter.format(r))
    }
}
