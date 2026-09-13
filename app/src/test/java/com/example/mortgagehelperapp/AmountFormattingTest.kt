package com.example.mortgagehelperapp

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormattingTest {
    @Test fun groupsAmountsWithoutRoundingOrDroppingDecimalInput() {
        val cases = mapOf("490000" to "490,000", "2500" to "2,500", "1234567.80" to "1,234,567.80",
            "490000." to "490,000.", "0.05" to "0.05", "." to ".", "" to "",
            "-1000" to "-1,000", "490,000.00" to "490,000.00", "1.2.3" to "1.2.3")
        cases.forEach { (raw, expected) -> assertEquals(expected, AmountFormatting.group(raw)) }
    }
}
