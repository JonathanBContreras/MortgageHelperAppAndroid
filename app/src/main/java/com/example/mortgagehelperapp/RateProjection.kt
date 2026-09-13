package com.example.mortgagehelperapp

import kotlin.math.sqrt

enum class RateScenario(val label: String, val direction: Double) {
    CENTRAL("Historical trend", 0.0), LOWER("Lower-rate scenario", -1.0), HIGHER("Higher-rate scenario", 1.0);
    override fun toString() = label
}

data class VariableRateOptions(
    val fixedYears: Int = 5,
    val annualCap: Double = 2.0,
    val lifetimeCap: Double = 5.0,
    val scenario: RateScenario = RateScenario.CENTRAL
) {
    init {
        require(fixedYears in 1..10) { "Initial fixed period must be 1–10 years" }
        require(annualCap.isFinite() && annualCap in 0.0..10.0) { "Annual cap must be 0–10 percentage points" }
        require(lifetimeCap.isFinite() && lifetimeCap in 0.0..20.0) { "Lifetime cap must be 0–20 percentage points" }
    }
}

object RateProjection {
    // Freddie Mac PMMS weekly 30-year FRM averages, calendar years 2015–2024.
    // A broad market proxy, not an ARM index or a lender forecast.
    val historicalAnnualRates = listOf(3.8506, 3.654, 3.9898, 4.5446, 3.9358, 3.1117, 2.9577, 5.344, 6.8067, 6.7212)
    val historicalMean = historicalAnnualRates.average()
    private val changes = historicalAnnualRates.zipWithNext { a, b -> b - a }
    val annualVolatility = sqrt(changes.map { (it - changes.average()) * (it - changes.average()) }.average())

    fun yearlyRates(initial: Double, years: Int, options: VariableRateOptions?): List<Double> {
        require(initial.isFinite() && initial in 0.0..30.0) { "Interest rate must be 0–30%" }
        require(years in 1..50) { "Loan term must be 1–50 years" }
        var rate = initial
        return List(years) { year ->
            if (options != null && year >= options.fixedYears) {
                // Close 25% of the gap to the historical mean at each annual reset.
                val change = (0.25 * (historicalMean - rate) + options.scenario.direction * annualVolatility)
                    .coerceIn(-options.annualCap, options.annualCap)
                rate = (rate + change).coerceIn(maxOf(0.0, initial - options.lifetimeCap), initial + options.lifetimeCap)
            }
            rate
        }
    }
}
