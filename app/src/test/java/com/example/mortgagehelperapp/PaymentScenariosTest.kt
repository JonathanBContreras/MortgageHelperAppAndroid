package com.example.mortgagehelperapp

import org.junit.Assert.*
import org.junit.Test

class PaymentScenariosTest {
    private fun loan(rate: Double = 6.0, variable: VariableRateOptions? = null) =
        MortgageViewModel().calculateMortgage(300000.0, null, 0.0, true, rate, 30, 100.0, 1.0, 1200.0, variable)

    @Test fun noExtraMatchesOriginalForFixedAndVariable() {
        for (base in listOf(loan(), loan(variable = VariableRateOptions()), loan(0.0))) {
            val result = PaymentScenarios.earlyPayoff(base, ExtraPaymentPlan())
            assertEquals(360, result.monthsToPayoff)
            assertEquals(0, result.monthsSaved)
            assertEquals(0.0, result.interestSaved, 0.000001)
            assertEquals(base.totalPrincipal + base.totalInterest, result.loanCost, 0.000001)
        }
    }

    @Test fun extraMonthlyMatchesIndependentClosedForm() {
        val base = loan()
        val result = PaymentScenarios.earlyPayoff(base, ExtraPaymentPlan(monthlyExtra = 500.0))
        val r = 0.06 / 12
        val payment = base.monthlyBreakdown.principalAndInterest + 500
        val expectedMonths = kotlin.math.ceil(-kotlin.math.ln(1 - r * 300000 / payment) / kotlin.math.ln(1 + r)).toInt()
        assertEquals(expectedMonths, result.monthsToPayoff)
        assertEquals(300000.0, result.payments.sumOf { it.principal }, 0.00001)
        assertEquals(0.0, result.payments.last().balance, 0.0)
        assertTrue(result.interestSaved > 0)
        assertTrue(result.payments.last().totalPayment <= payment + 0.00001)
        // No property tax, insurance or HOA is counted as avoided cost.
        assertEquals(base.totalPrincipal + base.totalInterest - result.loanCost, result.interestSaved, 0.00001)
    }

    @Test fun delayedExtraAndLumpSumApplyInSpecifiedMonths() {
        val base = loan(0.0)
        val result = PaymentScenarios.earlyPayoff(base, ExtraPaymentPlan(100.0, 13, 5000.0, 6))
        assertEquals(base.schedule[4].balance, result.payments[4].balance, 0.00001)
        assertEquals(base.schedule[5].balance - 5000, result.payments[5].balance, 0.00001)
        assertEquals(base.schedule[12].balance - 5100, result.payments[12].balance, 0.00001)
        assertEquals(0.0, result.interestSaved, 0.0)
        assertTrue(result.monthsSaved > 0)
    }

    @Test fun oversizedLumpSumClampsToBalanceAndStopsFurtherExtras() {
        val result = PaymentScenarios.earlyPayoff(loan(), ExtraPaymentPlan(lumpSum = 1000000.0))
        assertEquals(1, result.monthsToPayoff)
        assertEquals(301500.0, result.loanCost, 0.00001)
        assertEquals(1500.0, result.interest, 0.00001)
    }

    @Test fun rateComparisonUsesQuotedRateAndPreservesLoanCosts() {
        val base = loan(variable = VariableRateOptions(3, 1.0, 4.0, RateScenario.HIGHER))
        val comparison = PaymentScenarios.compareRates(base, 5.0)
        assertTrue(comparison.fixed.schedule.all { it.annualRate == 5.0 })
        assertEquals(base.totalCost, comparison.variable.totalCost, 0.000001)
        assertEquals(base.monthlyBreakdown.propertyTax, comparison.fixed.monthlyBreakdown.propertyTax, 0.0)
        assertEquals(360, comparison.fixed.schedule.size)
        assertEquals(base.totalPrincipal, comparison.fixed.totalPrincipal, 0.0)
        assertEquals(loan(5.0).totalCost, comparison.fixed.totalCost, 0.000001)
        assertNotNull(PaymentScenarios.compareRates(loan(), 6.0).variable.variableRate)
    }

    @Test fun invalidPlansAndRatesAreRejected() {
        for (plan in listOf(ExtraPaymentPlan(-1.0), ExtraPaymentPlan(Double.NaN),
            ExtraPaymentPlan(startingMonth = 361), ExtraPaymentPlan(lumpSum = -1.0), ExtraPaymentPlan(lumpSumMonth = 0))) {
            assertThrows(IllegalArgumentException::class.java) { PaymentScenarios.earlyPayoff(loan(), plan) }
        }
        assertThrows(IllegalArgumentException::class.java) { PaymentScenarios.compareRates(loan(), Double.NaN) }
    }

    @Test fun fullDownPaymentHasNoPayoffOrSavings() {
        val base = MortgageViewModel().calculateMortgage(300000.0, null, 100.0, true, 6.0, 30, null)
        val result = PaymentScenarios.earlyPayoff(base, ExtraPaymentPlan(500.0))
        assertEquals(0, result.monthsToPayoff)
        assertEquals(0, result.monthsSaved)
        assertEquals(0.0, result.interestSaved, 0.0)
    }
}
