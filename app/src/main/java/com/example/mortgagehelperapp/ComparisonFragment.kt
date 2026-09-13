package com.example.mortgagehelperapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.mortgagehelperapp.databinding.FragmentComparisonBinding
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.NumberFormat
import java.util.*

class ComparisonFragment : Fragment() {
    private var _binding: FragmentComparisonBinding? = null
    private val binding get() = _binding!!
    private val viewModel = MortgageViewModel()
    private val sharedViewModel: SharedMortgageViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComparisonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AmountFormatting.attach(binding.fixedQuoteInput)
        binding.compareRateTypesButton.setOnClickListener { compareRateTypes() }
        sharedViewModel.calculation.observe(viewLifecycleOwner) { calc ->
            binding.compareRateTypesButton.isEnabled = calc != null
            binding.rateComparisonResults.text = ""
            if (calc != null) {
                binding.fixedQuoteInput.setText(calc.monthlyBreakdown.interestRate.toString())
                val options = calc.variableRate ?: VariableRateOptions()
                binding.rateComparisonSummary.text = "Same ${calc.monthlyBreakdown.loanTermYears}-year term, principal, taxes, insurance and HOA. " +
                    "Variable starts at ${calc.monthlyBreakdown.interestRate}%; ${options.fixedYears} years fixed, then annual resets. " +
                    "${options.scenario.label}; annual cap ${options.annualCap} points, lifetime cap ${options.lifetimeCap} points. " +
                    "Enter a fixed quote below. Variable costs are estimates."
            }
        }
        binding.fixedQuoteInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { binding.rateComparisonResults.text = "" }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        sharedViewModel.comparison.observe(viewLifecycleOwner, Observer { comp ->
            if (comp != null) {
                updateComparisonChart(comp)
            }
        })
    }

    private fun compareRateTypes() {
        val base = sharedViewModel.calculation.value ?: return
        try {
            val rate = binding.fixedQuoteInput.text.toString().replace(",", "").toDoubleOrNull()
                ?: throw IllegalArgumentException("Enter a fixed-rate quote")
            val comparison = PaymentScenarios.compareRates(base, rate)
            val fixed = comparison.fixed
            val variable = comparison.variable
            val difference = fixed.totalCost - variable.totalCost
            fun money(value: Double) = currencyFormat.format(value)
            binding.rateComparisonResults.text =
                "Fixed / Variable\n" +
                "Initial monthly: ${money(fixed.monthlyPayment)} / ${money(variable.monthlyPayment)}\n" +
                "Peak monthly: ${money(fixed.schedule.maxOf { it.totalPayment })} / ${money(variable.schedule.maxOf { it.totalPayment })}\n" +
                "Total interest: ${money(fixed.totalInterest)} / ${money(variable.totalInterest)}\n" +
                "Total cost: ${money(fixed.totalCost)} / ${money(variable.totalCost)}\n\n" +
                if (difference >= 0) "Estimated variable savings: ${money(difference)}" else "Estimated extra variable cost: ${money(-difference)}"
        } catch (e: IllegalArgumentException) {
            binding.rateComparisonResults.text = e.message
        }
    }

    fun updateComparisonChart(comparison: LoanComparison) {
        val calc15 = comparison.loan15Year
        val calc30 = comparison.loan30Year
        val table = binding.comparisonTable
        table.removeAllViews()
        val context = requireContext()
        val headerRow = TableRow(context)
        headerRow.addView(makeCell("Metric", true))
        headerRow.addView(makeCell("15 Years", true))
        headerRow.addView(makeCell("30 Years", true))
        table.addView(headerRow)
        fun addRow(label: String, value15: String, value30: String) {
            val row = TableRow(context)
            row.addView(makeCell(label))
            row.addView(makeCell(value15))
            row.addView(makeCell(value30))
            table.addView(row)
        }
        val currency = { v: Double -> currencyFormat.format(v) }
        val monthlyTax15 = calc15.monthlyBreakdown.propertyTax
        val monthlyFees15 = calc15.monthlyBreakdown.hoaFees + calc15.monthlyBreakdown.homeInsurance
        val monthlyTax30 = calc30.monthlyBreakdown.propertyTax
        val monthlyFees30 = calc30.monthlyBreakdown.hoaFees + calc30.monthlyBreakdown.homeInsurance
        addRow("Initial Monthly", currency(calc15.monthlyPayment), currency(calc30.monthlyPayment))
        if (calc15.variableRate != null) {
            addRow("Peak Monthly", currency(calc15.schedule.maxOf { it.totalPayment }), currency(calc30.schedule.maxOf { it.totalPayment }))
        }
        addRow("Total Cost", currency(calc15.totalCost), currency(calc30.totalCost))
        addRow("Principal", currency(calc15.totalPrincipal), currency(calc30.totalPrincipal))
        addRow("Interest", currency(calc15.totalInterest), currency(calc30.totalInterest))
        addRow("Taxes & Fees", currency((monthlyTax15 + monthlyFees15) * 12 * 15), currency((monthlyTax30 + monthlyFees30) * 12 * 30))
    }

    private fun makeCell(text: String, bold: Boolean = false): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.setPadding(8, 8, 8, 8)
        tv.textSize = 16f
        if (bold) tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
        val typedArray = requireContext().theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val color = typedArray.getColor(0, 0xFF000000.toInt())
        typedArray.recycle()
        tv.setTextColor(color)
        return tv
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 