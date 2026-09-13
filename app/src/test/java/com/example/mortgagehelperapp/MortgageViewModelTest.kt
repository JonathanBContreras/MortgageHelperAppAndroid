package com.example.mortgagehelperapp

import org.junit.Assert.*
import org.junit.Test

class MortgageViewModelTest {
    private val model = MortgageViewModel()
    private fun calculate(rate: Double = 4.5, variable: VariableRateOptions? = null) =
        model.calculateMortgage(300000.0, 2000.0, 20.0, true, rate, 30, 100.0, 1.0, 1500.0, variable)

    @Test fun fixedPaymentMatchesIndependentAmortizationExample() {
        val result = calculate()
        assertEquals(1216.0447, result.monthlyBreakdown.principalAndInterest, 0.0001)
        assertEquals(1691.0447, result.monthlyPayment, 0.0001)
        assertEquals(150.0, result.costPerSqFt!!, 0.0)
        assertEquals(240000.0, result.schedule.sumOf { it.principal }, 0.0001)
        assertEquals(0.0, result.schedule.last().balance, 0.0)
        assertEquals(result.monthlyPayment * 360, result.totalCost, 0.001)
    }

    @Test fun zeroInterestAndFullDownPaymentAreFinite() {
        val zero = calculate(0.0)
        assertEquals(240000.0 / 360, zero.monthlyBreakdown.principalAndInterest, 0.00001)
        assertEquals(0.0, zero.totalInterest, 0.0)
        val paid = model.calculateMortgage(300000.0, null, 100.0, true, 6.0, 15, null, 1.0, 1200.0)
        assertEquals(350.0, paid.monthlyPayment, 0.0)
        assertEquals(0.0, paid.totalInterest, 0.0)
    }

    @Test fun variableResetReamortizesRemainingBalanceAndHonorsCaps() {
        val options = VariableRateOptions(5, 0.5, 1.0, RateScenario.HIGHER)
        val result = calculate(4.5, options)
        assertTrue(result.schedule.take(60).all { it.annualRate == 4.5 })
        assertEquals(5.0, result.schedule[60].annualRate, 0.00001)
        val remaining = result.schedule[59].balance
        val r = 5.0 / 1200
        val expected = remaining * r / (1 - Math.pow(1 + r, -300.0))
        assertEquals(expected, result.schedule[60].principal + result.schedule[60].interest, 0.00001)
        assertTrue(result.schedule.all { it.annualRate in 3.5..5.5 && it.balance >= 0 })
        assertEquals(result.totalPrincipal, result.schedule.sumOf { it.principal }, 0.00001)
        assertEquals(result.totalCost, result.totalPrincipal + result.totalInterest + 475 * 360, 0.0001)
    }

    @Test fun lowerAndHigherScenariosBracketCentralAndHaveZeroFloor() {
        val lower = calculate(6.0, VariableRateOptions(scenario = RateScenario.LOWER))
        val central = calculate(6.0, VariableRateOptions())
        val higher = calculate(6.0, VariableRateOptions(scenario = RateScenario.HIGHER))
        assertTrue(lower.totalInterest < central.totalInterest)
        assertTrue(central.totalInterest < higher.totalInterest)
        assertTrue(RateProjection.yearlyRates(0.1, 30, VariableRateOptions(1, 2.0, 5.0, RateScenario.LOWER)).all { it >= 0 })
        assertEquals(calculate().totalCost, calculate(4.5, VariableRateOptions(5, 0.0)).totalCost, 0.001)
    }

    @Test fun stateDefaultsAndOverridesPropagateIntoBothComparisons() {
        assertEquals(51, StateEstimates.all.size)
        assertEquals(51, StateEstimates.all.map { it.name }.toSet().size)
        val texas = StateEstimates.all.single { it.name == "Texas" }
        assertEquals(1.40, texas.taxPercent, 0.0)
        assertEquals(4582.0, texas.annualInsurance, 0.0)
        val comparison = model.compareLoans(300000.0, null, 20.0, true, 6.0, 0.0, texas.taxPercent, texas.annualInsurance, VariableRateOptions())
        for (result in listOf(comparison.loan15Year, comparison.loan30Year)) {
            assertEquals(350.0, result.monthlyBreakdown.propertyTax, 0.00001)
            assertEquals(4582.0 / 12, result.monthlyBreakdown.homeInsurance, 0.00001)
            assertNotNull(result.variableRate)
            assertEquals(result.monthlyBreakdown.loanTermYears * 12, result.schedule.size)
        }
        val override = model.calculateMortgage(300000.0, null, 0.0, true, 6.0, 30, null, 0.0, 900.0)
        assertEquals(0.0, override.monthlyBreakdown.propertyTax, 0.0)
        assertEquals(75.0, override.monthlyBreakdown.homeInsurance, 0.0)
    }

    @Test fun rejectsInvalidInputs() {
        val cases = listOf<() -> Any>(
            { calculate(Double.NaN) }, { calculate(-1.0) },
            { model.calculateMortgage(-1.0, null, 0.0, true, 6.0, 30, null) },
            { model.calculateMortgage(300000.0, 0.0, 0.0, true, 6.0, 30, null) },
            { model.calculateMortgage(300000.0, null, 101.0, true, 6.0, 30, null) },
            { model.calculateMortgage(300000.0, null, 0.0, true, 6.0, 0, null) },
            { VariableRateOptions(0) }, { VariableRateOptions(annualCap = Double.NaN) }
        )
        cases.forEach { action -> assertThrows(IllegalArgumentException::class.java) { action() } }
    }
}
