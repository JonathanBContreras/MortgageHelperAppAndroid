package com.example.mortgagehelperapp

import androidx.lifecycle.ViewModel
import kotlin.math.expm1
import kotlin.math.ln1p

class MortgageViewModel : ViewModel() {
    fun calculateMortgage(
        homePrice: Double, squareFootage: Double?, downPayment: Double,
        isDownPaymentPercentage: Boolean, interestRate: Double, loanTermYears: Int,
        hoaFees: Double?, propertyTaxPercent: Double = 1.0,
        annualHomeInsurance: Double = homePrice * 0.005,
        variableRate: VariableRateOptions? = null
    ): MortgageCalculation {
        require(homePrice.isFinite() && homePrice > 0) { "Home price must be greater than zero" }
        require(downPayment.isFinite() && downPayment >= 0 &&
            downPayment <= if (isDownPaymentPercentage) 100.0 else homePrice) { "Down payment must be between zero and the home price (or 100%)" }
        require(squareFootage == null || squareFootage.isFinite() && squareFootage > 0) { "Square footage must be greater than zero" }
        require(hoaFees == null || hoaFees.isFinite() && hoaFees >= 0) { "HOA fees cannot be negative" }
        require(propertyTaxPercent.isFinite() && propertyTaxPercent in 0.0..20.0) { "Property tax must be 0–20%" }
        require(annualHomeInsurance.isFinite() && annualHomeInsurance >= 0) { "Annual insurance cannot be negative" }
        val rates = RateProjection.yearlyRates(interestRate, loanTermYears, variableRate)
        val loan = homePrice - if (isDownPaymentPercentage) homePrice * downPayment / 100 else downPayment
        val months = loanTermYears * 12
        val tax = homePrice * propertyTaxPercent / 100 / 12
        val insurance = annualHomeInsurance / 12
        val fees = tax + insurance + (hoaFees ?: 0.0)
        var balance = loan
        var payment = 0.0
        val schedule = List(months) { index ->
            val rate = rates[index / 12]
            val monthlyRate = rate / 1200
            if (index % 12 == 0) {
                payment = if (monthlyRate == 0.0) balance / (months - index)
                else balance * monthlyRate / -expm1(-(months - index) * ln1p(monthlyRate))
            }
            val interest = balance * monthlyRate
            val principal = if (index == months - 1) balance else (payment - interest).coerceIn(0.0, balance)
            balance = (balance - principal).coerceAtLeast(0.0)
            MortgagePayment(index + 1, rate, principal, interest, balance, principal + interest + fees)
        }
        return MortgageCalculation(
            monthlyPayment = schedule.first().totalPayment,
            totalCost = schedule.sumOf { it.totalPayment }, totalPrincipal = loan,
            totalInterest = schedule.sumOf { it.interest }, costPerSqFt = squareFootage?.let { homePrice / it },
            monthlyBreakdown = MonthlyBreakdown(schedule.first().principal + schedule.first().interest,
                tax, insurance, hoaFees ?: 0.0, interestRate, loanTermYears, loan),
            schedule = schedule, variableRate = variableRate
        )
    }

    fun compareLoans(
        homePrice: Double, squareFootage: Double?, downPayment: Double,
        isDownPaymentPercentage: Boolean, interestRate: Double, hoaFees: Double?,
        propertyTaxPercent: Double = 1.0, annualHomeInsurance: Double = homePrice * 0.005,
        variableRate: VariableRateOptions? = null
    ) = LoanComparison(
        calculateMortgage(homePrice, squareFootage, downPayment, isDownPaymentPercentage, interestRate, 15,
            hoaFees, propertyTaxPercent, annualHomeInsurance, variableRate),
        calculateMortgage(homePrice, squareFootage, downPayment, isDownPaymentPercentage, interestRate, 30,
            hoaFees, propertyTaxPercent, annualHomeInsurance, variableRate)
    )
}
