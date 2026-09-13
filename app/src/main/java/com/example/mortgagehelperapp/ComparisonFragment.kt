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
        sharedViewModel.comparison.observe(viewLifecycleOwner, Observer { comp ->
            if (comp != null) {
                updateComparisonChart(comp)
            }
        })
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