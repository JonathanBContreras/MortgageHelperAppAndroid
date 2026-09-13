package com.example.mortgagehelperapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.mortgagehelperapp.databinding.FragmentCalculatorBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.Locale

class CalculatorFragment : Fragment() {
    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!
    private val viewModel = MortgageViewModel()
    private val sharedViewModel: SharedMortgageViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEstimates()
        setupChart()
        setupListeners()
        inputFields().forEach { AmountFormatting.attach(it) }
        restoreInputs()
        sharedViewModel.calculation.observe(viewLifecycleOwner) { result ->
            result?.let { displayResults(it) }
        }
    }

    private fun requiredNumber(input: android.widget.EditText, label: String): Double =
        input.text.toString().replace(",", "").toDoubleOrNull()
            ?.takeIf { it.isFinite() } ?: throw IllegalArgumentException("Enter a valid $label")

    private fun optionalNumber(input: android.widget.EditText, label: String): Double? =
        if (input.text.isNullOrBlank()) null else requiredNumber(input, label)

    private fun setupEstimates() {
        binding.stateSpinner.adapter = android.widget.ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, listOf("Select purchase state") + StateEstimates.all.map { it.name })
        binding.scenarioSpinner.adapter = android.widget.ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, RateScenario.entries.toList())
        binding.stateSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position != lastStatePosition) {
                    val state = StateEstimates.all[position - 1]
                    binding.propertyTaxInput.setText(state.taxPercent.toString())
                    binding.insuranceInput.setText(state.annualInsurance.toInt().toString())
                }
                lastStatePosition = position
            }
        }
        binding.variableRateSwitch.setOnCheckedChangeListener { _, checked ->
            binding.variableOptions.visibility = if (checked) View.VISIBLE else View.GONE
        }
        binding.marketInfo.text = "Rate snapshot · September 10, 2026\n30-year fixed: 6.76% · 15-year fixed: 6.09%\n30-year: +0.05 percentage points over the prior week; 6.35% a year earlier. National averages, not a personal quote or ARM index."
        binding.latestRatesButton.setOnClickListener { openSource("https://www.freddiemac.com/pmms") }
        binding.estimateSourcesButton.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Estimate sources")
                .setItems(arrayOf("Property tax: Tax Foundation / Census (2024)", "Insurance: Insurance.com (2026, $300,000 coverage)", "Rate history: Freddie Mac (2015–2024)")) { _, which ->
                    openSource(listOf("https://taxfoundation.org/data/all/state/property-taxes-by-state-county/",
                        "https://www.insurance.com/home-and-renters-insurance/home-insurance-basics/average-homeowners-insurance-rates-by-state",
                        "https://www.freddiemac.com/pmms/archive")[which])
                }.show()
        }
    }

    private var lastStatePosition = 0

    private fun openSource(url: String) {
        try { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: android.content.ActivityNotFoundException) { Toast.makeText(context, url, Toast.LENGTH_LONG).show() }
    }

    private fun inputFields() = listOf(binding.homePriceInput, binding.squareFootageInput,
        binding.downPaymentInput, binding.interestRateInput, binding.hoaFeesInput,
        binding.propertyTaxInput, binding.insuranceInput, binding.fixedYearsInput,
        binding.annualCapInput, binding.lifetimeCapInput)

    private fun restoreInputs() {
        val prefs = requireContext().getSharedPreferences("mortgage_inputs", 0)
        lastStatePosition = prefs.getInt("state", 0).coerceIn(0, StateEstimates.all.size)
        binding.stateSpinner.setSelection(lastStatePosition)
        inputFields().forEach { field ->
            prefs.getString(resources.getResourceEntryName(field.id), null)?.let { field.setText(it) }
        }
        binding.variableRateSwitch.isChecked = prefs.getBoolean("variable", false)
        binding.variableOptions.visibility = if (binding.variableRateSwitch.isChecked) View.VISIBLE else View.GONE
        binding.scenarioSpinner.setSelection(prefs.getInt("scenario", 0).coerceIn(0, 2))
        binding.downPaymentTypeGroup.check(if (prefs.getBoolean("percent", false)) R.id.downPaymentPercent else R.id.downPaymentAmount)
        binding.loanTermGroup.check(if (prefs.getBoolean("fifteen", false)) R.id.loanTerm15 else R.id.loanTerm30)
    }

    override fun onPause() {
        super.onPause()
        val prefs = requireContext().getSharedPreferences("mortgage_inputs", 0).edit()
        inputFields().forEach { prefs.putString(resources.getResourceEntryName(it.id), it.text.toString()) }
        prefs.putInt("state", binding.stateSpinner.selectedItemPosition)
            .putInt("scenario", binding.scenarioSpinner.selectedItemPosition)
            .putBoolean("variable", binding.variableRateSwitch.isChecked)
            .putBoolean("percent", binding.downPaymentPercent.isChecked)
            .putBoolean("fifteen", binding.loanTerm15.isChecked).apply()
    }

    private fun setupChart() {
        binding.paymentBreakdownChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 12f
                formSize = 12f
                formToTextSpace = 5f
                xEntrySpace = 10f
            }
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }
    }

    private fun setupListeners() {
        binding.calculateButton.setOnClickListener {
            try {
                val homePrice = binding.homePriceInput.text.toString().replace(",", "").toDoubleOrNull()
                val squareFootage = optionalNumber(binding.squareFootageInput, "Square footage")
                val downPayment = binding.downPaymentInput.text.toString().replace(",", "").toDoubleOrNull()
                val isDownPaymentPercentage = binding.downPaymentPercent.isChecked
                val interestRate = binding.interestRateInput.text.toString().replace(",", "").toDoubleOrNull()
                val hoaFees = optionalNumber(binding.hoaFeesInput, "HOA fees")
                val loanTermYears = if (binding.loanTerm15.isChecked) 15 else 30

                if (homePrice == null || downPayment == null || interestRate == null) {
                    Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                require(binding.stateSpinner.selectedItemPosition > 0) { "Select the purchase state" }
                val tax = requiredNumber(binding.propertyTaxInput, "Property tax")
                val insurance = requiredNumber(binding.insuranceInput, "Annual insurance")
                val variable = if (binding.variableRateSwitch.isChecked) VariableRateOptions(
                    fixedYears = requiredNumber(binding.fixedYearsInput, "Initial fixed years").let {
                        require(it % 1.0 == 0.0) { "Initial fixed years must be a whole number" }; it.toInt()
                    },
                    annualCap = requiredNumber(binding.annualCapInput, "Annual cap"),
                    lifetimeCap = requiredNumber(binding.lifetimeCapInput, "Lifetime cap"),
                    scenario = RateScenario.entries[binding.scenarioSpinner.selectedItemPosition]
                ) else null
                val result = viewModel.calculateMortgage(
                    homePrice = homePrice,
                    squareFootage = squareFootage,
                    downPayment = downPayment,
                    isDownPaymentPercentage = isDownPaymentPercentage,
                    interestRate = interestRate,
                    loanTermYears = loanTermYears,
                    hoaFees = hoaFees,
                    propertyTaxPercent = tax, annualHomeInsurance = insurance, variableRate = variable
                )
                sharedViewModel.setCalculation(result)
                // Also update comparison automatically
                val comparison = viewModel.compareLoans(
                    homePrice = homePrice,
                    squareFootage = squareFootage,
                    downPayment = downPayment,
                    isDownPaymentPercentage = isDownPaymentPercentage,
                    interestRate = interestRate,
                    hoaFees = hoaFees,
                    propertyTaxPercent = tax, annualHomeInsurance = insurance, variableRate = variable
                )
                sharedViewModel.setComparison(comparison)
            } catch (e: Exception) {
                val msg = e.message ?: "An unexpected error occurred."
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayResults(result: MortgageCalculation) {
        binding.monthlyPaymentResult.text = "Initial Monthly Payment: ${currencyFormat.format(result.monthlyPayment)}"
        binding.totalCostResult.text = "Total Cost: ${currencyFormat.format(result.totalCost)}"
        binding.principalInterestBreakdown.text = "Principal: ${currencyFormat.format(result.totalPrincipal)}\nInterest: ${currencyFormat.format(result.totalInterest)}"

        if (result.costPerSqFt != null) {
            binding.costPerSqFtResult.text = "Cost per Square Foot: ${currencyFormat.format(result.costPerSqFt)}"
        } else {
            binding.costPerSqFtResult.text = ""
        }

        binding.breakdownResult.text = """
            Initial Monthly Breakdown:
            Principal & Interest: ${currencyFormat.format(result.monthlyBreakdown.principalAndInterest)}
            Property Tax: ${currencyFormat.format(result.monthlyBreakdown.propertyTax)}
            Home Insurance: ${currencyFormat.format(result.monthlyBreakdown.homeInsurance)}
            HOA Fees: ${currencyFormat.format(result.monthlyBreakdown.hoaFees)}
        """.trimIndent()

        binding.projectionResult.text = if (result.variableRate != null) {
            val payments = result.schedule
            "Estimated monthly range: ${currencyFormat.format(payments.minOf { it.totalPayment })}–${currencyFormat.format(payments.maxOf { it.totalPayment })}\n" +
                "Annual rate / monthly payment (tax, insurance and HOA held constant):\n" +
                payments.filter { (it.month - 1) % 12 == 0 }.joinToString("\n") {
                    "Year ${(it.month - 1) / 12 + 1}: ${String.format(Locale.US, "%.2f%%", it.annualRate)} · ${currencyFormat.format(it.totalPayment)}"
                }
        } else "Fixed rate throughout the loan."
        updateChart(result.monthlyBreakdown)

    }

    private fun updateChart(breakdown: MonthlyBreakdown) {
        val sections = mutableListOf<Pair<String, Float>>()
        // Calculate principal and interest split
        val monthlyRate = breakdown.interestRate / 100 / 12
        val interest = breakdown.loanAmount * monthlyRate
        val principal = breakdown.principalAndInterest - interest
        if (principal > 0) sections.add(getString(R.string.principal) to principal.toFloat())
        if (interest > 0) sections.add(getString(R.string.interest) to interest.toFloat())
        if (breakdown.propertyTax > 0) sections.add(getString(R.string.property_tax) to breakdown.propertyTax.toFloat())
        if (breakdown.homeInsurance > 0) sections.add(getString(R.string.home_insurance) to breakdown.homeInsurance.toFloat())
        if (breakdown.hoaFees > 0) sections.add(getString(R.string.hoa_fees) to breakdown.hoaFees.toFloat())
        val entries = sections.map { PieEntry(it.second, it.first) }
        if (entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, getString(R.string.monthly_payment_breakdown)).apply {
                colors = List(entries.size) { ColorTemplate.MATERIAL_COLORS[it % ColorTemplate.MATERIAL_COLORS.size] }
                valueTextColor = Color.BLACK
                valueTextSize = 12f
                valueFormatter = DefaultValueFormatter(0)
            }
            binding.paymentBreakdownChart.apply {
                data = PieData(dataSet)
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.isWordWrapEnabled = true
                legend.setCustom(sections.mapIndexed { index, pair ->
                    LegendEntry(pair.first, Legend.LegendForm.CIRCLE, 12f, 12f, null, ColorTemplate.MATERIAL_COLORS[index % ColorTemplate.MATERIAL_COLORS.size])
                })
                invalidate()
            }
        } else {
            binding.paymentBreakdownChart.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 