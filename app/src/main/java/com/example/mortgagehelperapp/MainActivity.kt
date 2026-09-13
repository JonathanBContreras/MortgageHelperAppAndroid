package com.example.mortgagehelperapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mortgagehelperapp.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var calculatorFragment: CalculatorFragment
    private lateinit var amortizationFragment: AmortizationFragment
    private lateinit var comparisonFragment: ComparisonFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupTabLayout()
    }

    private fun setupViewPager() {
        calculatorFragment = CalculatorFragment()
        amortizationFragment = AmortizationFragment()
        comparisonFragment = ComparisonFragment()

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> calculatorFragment
                    1 -> amortizationFragment
                    2 -> comparisonFragment
                    3 -> EarlyPayoffFragment()
                    else -> throw IllegalArgumentException("Invalid position $position")
                }
            }
        }
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.calculator)
                1 -> getString(R.string.amortization)
                2 -> getString(R.string.comparison)
                3 -> "Early payoff"
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }.attach()
    }

} 