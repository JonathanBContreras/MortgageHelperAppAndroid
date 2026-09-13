package com.example.mortgagehelperapp

import kotlin.math.expm1
import kotlin.math.ln1p

data class ExtraPaymentPlan(
    val monthlyExtra: Double = 0.0,
    val startingMonth: Int = 1,
    val lumpSum: Double = 0.0,
    val lumpSumMonth: Int = 1
)

data class EarlyPayoffResult(val baseline: MortgageCalculation, val payments: List<MortgagePayment>) {
    val monthsToPayoff get() = payments.size
    val monthsSaved get() = if (baseline.totalPrincipal == 0.0) 0 else baseline.schedule.size - monthsToPayoff
    val interest get() = payments.sumOf { it.interest }
    val interestSaved get() = baseline.totalInterest - interest
    val loanCost get() = payments.sumOf { it.principal + it.interest }
}

data class RateTypeComparison(val fixed: MortgageCalculation, val variable: MortgageCalculation)

object PaymentScenarios {
    fun compareRates(base: MortgageCalculation, fixedRate: Double): RateTypeComparison = RateTypeComparison(
        reprice(base, fixedRate, null),
        reprice(base, base.monthlyBreakdown.interestRate, base.variableRate ?: VariableRateOptions())
    )

    private fun reprice(base: MortgageCalculation, initialRate: Double, options: VariableRateOptions?): MortgageCalculation {
        val rates = RateProjection.yearlyRates(initialRate, base.monthlyBreakdown.loanTermYears, options)
        val fees = base.monthlyBreakdown.let { it.propertyTax + it.homeInsurance + it.hoaFees }
        var balance = base.totalPrincipal
        var payment = 0.0
        val months = base.schedule.size
        require(months > 0) { "Calculate a mortgage first" }
        val schedule = List(months) { index ->
            val rate = rates[index / 12] / 1200
            if (index % 12 == 0) payment = if (rate == 0.0) balance / (months - index)
                else balance * rate / -expm1(-(months - index) * ln1p(rate))
            val interest = balance * rate
            val principal = if (index == months - 1) balance else (payment - interest).coerceIn(0.0, balance)
            balance = (balance - principal).coerceAtLeast(0.0)
            MortgagePayment(index + 1, rates[index / 12], principal, interest, balance, principal + interest + fees)
        }
        return base.copy(monthlyPayment = schedule.first().totalPayment,
            totalCost = schedule.sumOf { it.totalPayment }, totalInterest = schedule.sumOf { it.interest },
            monthlyBreakdown = base.monthlyBreakdown.copy(interestRate = initialRate,
                principalAndInterest = schedule.first().principal + schedule.first().interest),
            schedule = schedule, variableRate = options)
    }

    fun earlyPayoff(base: MortgageCalculation, plan: ExtraPaymentPlan): EarlyPayoffResult {
        require(base.schedule.isNotEmpty()) { "Calculate a mortgage first" }
        require(plan.monthlyExtra.isFinite() && plan.monthlyExtra >= 0) { "Monthly extra must be zero or more" }
        require(plan.lumpSum.isFinite() && plan.lumpSum >= 0) { "Lump sum must be zero or more" }
        require(plan.startingMonth in 1..base.schedule.size) { "Starting month must be within the loan term" }
        require(plan.lumpSumMonth in 1..base.schedule.size) { "Lump-sum month must be within the loan term" }
        var balance = base.totalPrincipal
        val payments = mutableListOf<MortgagePayment>()
        for (scheduled in base.schedule) {
            if (balance <= 0.0) break
            val interest = balance * scheduled.annualRate / 1200
            val regular = scheduled.principal + scheduled.interest
            val extra = (if (scheduled.month >= plan.startingMonth) plan.monthlyExtra else 0.0) +
                (if (scheduled.month == plan.lumpSumMonth) plan.lumpSum else 0.0)
            val principal = if (scheduled.month == base.schedule.size) balance
                else (regular - interest + extra).coerceIn(0.0, balance)
            balance = (balance - principal).coerceAtLeast(0.0)
            // Loan-only payments: ownership costs continue after payoff and are not savings.
            payments.add(MortgagePayment(scheduled.month, scheduled.annualRate, principal, interest, balance, principal + interest))
        }
        return EarlyPayoffResult(base, payments)
    }
}
