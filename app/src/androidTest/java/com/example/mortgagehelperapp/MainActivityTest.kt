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

    @Test fun variableCalculationStateOverridesAndTabs() {
        fill(R.id.homePriceInput, "300000")
        fill(R.id.downPaymentInput, "60000")
        onView(withId(R.id.downPaymentAmount)).perform(scrollTo(), click())
        fill(R.id.interestRateInput, "6.76")
        selectState("Texas")
        onView(withId(R.id.propertyTaxInput)).check(matches(withText("1.4")))
        onView(withId(R.id.insuranceInput)).check(matches(withText("4582")))
        fill(R.id.insuranceInput, "2400")
        onView(withId(R.id.variableRateSwitch)).perform(scrollTo())
        activityRule.scenario.onActivity { it.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.variableRateSwitch).isChecked = true }
        fill(R.id.fixedYearsInput, "5")
        onView(withId(R.id.calculateButton)).perform(scrollTo(), click())
        onView(withId(R.id.projectionResult)).check(matches(withText(containsString("Year 6:"))))
        onView(withId(R.id.breakdownResult)).check(matches(withText(containsString("Home Insurance: $200.00"))))
        onView(allOf(withText("Amortization"), isDescendantOfA(withId(R.id.tabLayout)))).perform(click())
        onView(withId(R.id.amortizationSummary)).check(matches(withText(containsString("Total Principal: $240,000.00"))))
        onView(allOf(withText("Comparison"), isDescendantOfA(withId(R.id.tabLayout)))).perform(click())
        onView(withText("Peak Monthly")).check(matches(isDisplayed()))
        onView(allOf(withText("Calculator"), isDescendantOfA(withId(R.id.tabLayout)))).perform(click())
        activityRule.scenario.recreate()
        onView(withId(R.id.insuranceInput)).check(matches(withText("2400")))
        onView(withId(R.id.variableRateSwitch)).check(matches(isChecked()))
    }

    @Test fun changingStateReplacesBothEstimates() {
        selectState("Florida")
        onView(withId(R.id.insuranceInput)).check(matches(withText("8471")))
        fill(R.id.propertyTaxInput, "2.5")
        selectState("California")
        onView(withId(R.id.propertyTaxInput)).check(matches(withText("0.7")))
        onView(withId(R.id.insuranceInput)).check(matches(withText("1653")))
    }
}
