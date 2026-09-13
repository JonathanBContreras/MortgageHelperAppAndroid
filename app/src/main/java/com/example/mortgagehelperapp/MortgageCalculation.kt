package com.example.mortgagehelperapp

data class MortgageCalculation(
    val monthlyPayment: Double,
    val totalCost: Double,
    val totalPrincipal: Double,
    val totalInterest: Double,
    val costPerSqFt: Double?,
    val monthlyBreakdown: MonthlyBreakdown,
    val schedule: List<MortgagePayment> = emptyList(),
    val variableRate: VariableRateOptions? = null
)

data class MonthlyBreakdown(
    val principalAndInterest: Double,
    val propertyTax: Double,
    val homeInsurance: Double,
    val hoaFees: Double,
    val interestRate: Double,
    val loanTermYears: Int,
    val loanAmount: Double
)

data class LoanComparison(
    val loan15Year: MortgageCalculation,
    val loan30Year: MortgageCalculation
)
data class MortgagePayment(val month: Int, val annualRate: Double, val principal: Double,
    val interest: Double, val balance: Double, val totalPayment: Double)
