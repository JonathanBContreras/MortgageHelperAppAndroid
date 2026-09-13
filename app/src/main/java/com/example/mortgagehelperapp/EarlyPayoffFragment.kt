package com.example.mortgagehelperapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.mortgagehelperapp.databinding.FragmentEarlyPayoffBinding
import java.text.NumberFormat
import java.util.Locale

class EarlyPayoffFragment : Fragment() {
    private var _binding: FragmentEarlyPayoffBinding? = null
    private val binding get() = _binding!!
    private val shared: SharedMortgageViewModel by activityViewModels()
    private val money = NumberFormat.getCurrencyInstance(Locale.US)
    private var base: MortgageCalculation? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEarlyPayoffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("early_payoff", 0)
        fields().forEach {
            AmountFormatting.attach(it)
            prefs.getString(resources.getResourceEntryName(it.id), null)?.let { value -> it.setText(value) }
            it.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.payoffResults.text = ""
                    binding.payoffError.text = ""
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        binding.calculatePayoffButton.setOnClickListener { compare() }
        shared.calculation.observe(viewLifecycleOwner) {
            base = it
            binding.calculatePayoffButton.isEnabled = it != null
            binding.payoffResults.text = ""
            binding.payoffLoanSummary.text = if (it == null) "Calculate a mortgage in the Calculator tab first." else
                "${money.format(it.totalPrincipal)} loan · ${it.monthlyBreakdown.loanTermYears} years · " +
                    (it.variableRate?.scenario?.label ?: "Fixed rate") +
                    "\nBaseline interest: ${money.format(it.totalInterest)}"
        }
    }

    private fun fields() = listOf(binding.monthlyExtraInput, binding.extraStartingMonthInput,
        binding.lumpSumInput, binding.lumpSumMonthInput)

    private fun amount(field: android.widget.EditText, label: String): Double {
        return field.text.toString().replace(",", "").toDoubleOrNull()?.takeIf { it.isFinite() }
            ?: throw IllegalArgumentException("Enter a valid $label")
    }

    private fun month(field: android.widget.EditText, label: String): Int {
        val value = amount(field, label)
        require(value in 1.0..600.0 && value % 1.0 == 0.0) { "$label must be a whole month within the loan term" }
        return value.toInt()
    }

    private fun duration(months: Int) = "${months / 12} years, ${months % 12} months"

    private fun compare() {
        val loan = base ?: return
        try {
            val result = PaymentScenarios.earlyPayoff(loan, ExtraPaymentPlan(
                amount(binding.monthlyExtraInput, "monthly extra"),
                month(binding.extraStartingMonthInput, "Starting month"),
                amount(binding.lumpSumInput, "lump sum"), month(binding.lumpSumMonthInput, "Lump-sum month")
            ))
            binding.payoffError.text = ""
            binding.payoffResults.text = if (loan.totalPrincipal == 0.0) "No loan balance to pay off." else
                "Interest saved: ${money.format(result.interestSaved)}\n" +
                "Paid off in: ${duration(result.monthsToPayoff)} (month ${result.monthsToPayoff})\n" +
                "Time saved: ${duration(result.monthsSaved)}\n\n" +
                "Baseline interest: ${money.format(loan.totalInterest)}\n" +
                "Custom interest: ${money.format(result.interest)}\n" +
                "Baseline principal + interest: ${money.format(loan.totalPrincipal + loan.totalInterest)}\n" +
                "Custom principal + interest: ${money.format(result.loanCost)}\n" +
                "Final loan payment: ${money.format(result.payments.lastOrNull()?.totalPayment ?: 0.0)}"
        } catch (e: IllegalArgumentException) {
            binding.payoffResults.text = ""
            binding.payoffError.text = e.message
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = requireContext().getSharedPreferences("early_payoff", 0).edit()
        fields().forEach { prefs.putString(resources.getResourceEntryName(it.id), it.text.toString()) }
        prefs.apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
