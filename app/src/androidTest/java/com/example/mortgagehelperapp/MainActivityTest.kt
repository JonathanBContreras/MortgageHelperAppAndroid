package com.example.mortgagehelperapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private fun fill(id: Int, value: String) {
        onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
    }
    private fun selectState(name: String) {
        onView(withId(R.id.stateSpinner)).perform(scrollTo(), click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`(name))).perform(click())
    }

    private fun tab(index: Int) {
        activityRule.scenario.onActivity {
            it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(index, false)
        }
    }

    @Test fun formattedTypingComparisonAndEarlyPayoff() {
        fill(R.id.homePriceInput, "")
        onView(withId(R.id.homePriceInput)).perform(typeText("490000.25"), closeSoftKeyboard())
        onView(withId(R.id.homePriceInput)).check(matches(withText("490,000.25")))
        fill(R.id.squareFootageInput, "2500")
        onView(withId(R.id.squareFootageInput)).check(matches(withText("2,500")))
        fill(R.id.downPaymentInput, "90000")
        onView(withId(R.id.downPaymentAmount)).perform(scrollTo(), click())
        fill(R.id.interestRateInput, "6.5")
        selectState("California")
        onView(withId(R.id.variableRateSwitch)).perform(scrollTo())
        activityRule.scenario.onActivity { it.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.variableRateSwitch).isChecked = false }
        onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
        tab(2)
        fill(R.id.fixedQuoteInput, "6.25")
        onView(withId(R.id.compareRateTypesButton)).perform(scrollTo(), click())
        onView(withId(R.id.rateComparisonResults)).check(matches(withText(containsString("Fixed / Variable"))))
        onView(withId(R.id.rateComparisonResults)).check(matches(withText(containsString("Total interest:"))))
        tab(3)
        fill(R.id.monthlyExtraInput, "500")
        fill(R.id.extraStartingMonthInput, "1")
        fill(R.id.lumpSumInput, "10000")
        fill(R.id.lumpSumMonthInput, "12")
        onView(withId(R.id.lumpSumInput)).check(matches(withText("10,000")))
        onView(withId(R.id.calculatePayoffButton)).perform(scrollTo(), click())
        onView(withId(R.id.payoffResults)).check(matches(withText(containsString("Interest saved:"))))
        onView(withId(R.id.payoffResults)).check(matches(withText(containsString("Time saved:"))))
        fill(R.id.extraStartingMonthInput, "0")
        onView(withId(R.id.calculatePayoffButton)).perform(scrollTo(), click())
        onView(withId(R.id.payoffError)).check(matches(withText(containsString("whole month"))))
        onView(withId(R.id.payoffResults)).check(matches(withText("")))
    }

    @Test fun variableCalculationStateOverridesAndTabs() {
        fill(R.id.homePriceInput, "300000")
        fill(R.id.downPaymentInput, "60000")
        onView(withId(R.id.downPaymentAmount)).perform(scrollTo(), click())
        fill(R.id.interestRateInput, "6.76")
        selectState("Texas")
        onView(withId(R.id.propertyTaxInput)).check(matches(withText("1.4")))
        onView(withId(R.id.insuranceInput)).check(matches(withText("4,582")))
        fill(R.id.insuranceInput, "2400")
        onView(withId(R.id.variableRateSwitch)).perform(scrollTo())
        activityRule.scenario.onActivity { it.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.variableRateSwitch).isChecked = true }
        fill(R.id.fixedYearsInput, "5")
        onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
        onView(withId(R.id.projectionResult)).check(matches(withText(containsString("Year 6:"))))
        onView(withId(R.id.breakdownResult)).check(matches(withText(containsString("Home Insurance: $200.00"))))
        tab(1)
        onView(withId(R.id.amortizationSummary)).check(matches(withText(containsString("Total Principal: $240,000.00"))))
        tab(2)
        onView(withText("Peak Monthly")).perform(scrollTo()).check(matches(isDisplayed()))
        tab(0)
        activityRule.scenario.recreate()
        onView(withId(R.id.insuranceInput)).check(matches(withText("2,400")))
        onView(withId(R.id.variableRateSwitch)).check(matches(isChecked()))
    }

    @Test fun changingStateReplacesBothEstimates() {
        selectState("Florida")
        onView(withId(R.id.insuranceInput)).check(matches(withText("8,471")))
        fill(R.id.propertyTaxInput, "2.5")
        selectState("California")
        onView(withId(R.id.propertyTaxInput)).check(matches(withText("0.7")))
        onView(withId(R.id.insuranceInput)).check(matches(withText("1,653")))
    }
}
