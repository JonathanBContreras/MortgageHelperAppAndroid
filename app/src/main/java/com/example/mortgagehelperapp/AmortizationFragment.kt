package com.example.mortgagehelperapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.mortgagehelperapp.databinding.FragmentAmortizationBinding
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.NumberFormat
import java.util.*

class AmortizationFragment : Fragment() {
    private var _binding: FragmentAmortizationBinding? = null
    private val binding get() = _binding!!
    private val viewModel = MortgageViewModel()
    private val sharedViewModel: SharedMortgageViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmortizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        sharedViewModel.calculation.observe(viewLifecycleOwner, Observer { calc ->
            if (calc != null) {
                updateAmortizationSchedule(calc)
            }
        })
    }

    private fun setupChart() {
        binding.amortizationChart.apply {
            if (this is CombinedChart) {
                description.isEnabled = false
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                isHighlightFullBarEnabled = false
                setDrawOrder(arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE))
                axisRight.isEnabled = true
                axisLeft.axisMinimum = 0f
                axisRight.axisMinimum = 0f
                legend.apply {
                    isEnabled = true
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    setDrawInside(false)
                    textSize = 12f
                }
            }
        }
    }

    fun updateAmortizationSchedule(calculation: MortgageCalculation) {
        val years = calculation.monthlyBreakdown.loanTermYears
        val xLabels = (1..years).map { (Calendar.getInstance().get(Calendar.YEAR) + it - 1).toString() }

        val principalEntries = mutableListOf<BarEntry>()
        val balanceEntries = mutableListOf<Entry>()
        var totalPrincipal = 0.0
        var totalInterest = 0.0
        var totalTaxes = 0.0
        val monthlyFees = calculation.monthlyBreakdown.propertyTax +
            calculation.monthlyBreakdown.hoaFees + calculation.monthlyBreakdown.homeInsurance
        calculation.schedule.chunked(12).forEachIndexed { year, payments ->
            val principal = payments.sumOf { it.principal }
            val interest = payments.sumOf { it.interest }
            val taxes = monthlyFees * payments.size
            totalPrincipal += principal
            totalInterest += interest
            totalTaxes += taxes
            principalEntries.add(BarEntry(year.toFloat(), floatArrayOf(principal.toFloat(), interest.toFloat(), taxes.toFloat())))
            balanceEntries.add(Entry(year.toFloat(), payments.last().balance.toFloat()))
        }

        val barDataSet = BarDataSet(principalEntries, "").apply {
            setDrawIcons(false)
            colors = listOf(Color.rgb(33, 150, 243), Color.rgb(13, 71, 161), Color.rgb(144, 202, 249))
            stackLabels = arrayOf("Principal", "Interest", "Taxes & Fees")
            setDrawValues(false)
        }
        val barData = BarData(barDataSet)
        barData.barWidth = 0.8f

        val lineDataSet = LineDataSet(balanceEntries, "Balance").apply {
            color = Color.BLACK
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(Color.BLACK)
            axisDependency = YAxis.AxisDependency.RIGHT
            setDrawValues(false)
        }
        val lineData = LineData(lineDataSet)

        val combinedData = CombinedData()
        combinedData.setData(barData)
        combinedData.setData(lineData)

        (binding.amortizationChart as? CombinedChart)?.apply {
            data = combinedData
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = -45f
            axisLeft.axisMinimum = 0f
            axisRight.axisMinimum = 0f
            // Set axis and legend text color for dark mode compatibility
            val typedArray = requireContext().theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
            val color = typedArray.getColor(0, 0xFF000000.toInt())
            typedArray.recycle()
            xAxis.textColor = color
            axisLeft.textColor = color
            axisRight.textColor = color
            legend.textColor = color
            invalidate()
        }

        binding.amortizationSummary.text = "Total Principal: ${currencyFormat.format(totalPrincipal)}\nTotal Interest: ${currencyFormat.format(totalInterest)}\nTotal Taxes & Fees: ${currencyFormat.format(totalTaxes)}\nTotal Cost: ${currencyFormat.format(totalPrincipal + totalInterest + totalTaxes)}"
        // Set summary text color to theme's primary text color
        val typedArray = requireContext().theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val color = typedArray.getColor(0, 0xFF000000.toInt())
        typedArray.recycle()
        binding.amortizationSummary.setTextColor(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 