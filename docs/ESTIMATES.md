# Beta estimates and assumptions

## Interest rates

The in-app **snapshot is dated September 10, 2026**, retrieved September 12, 2026:
30-year fixed 6.76%; 15-year fixed 6.09%; prior-week 30-year 6.71%; year-earlier
30-year 6.35%. Source: [Freddie Mac PMMS](https://www.freddiemac.com/pmms).
This is an offline snapshot, not a live feed. The “View latest” button opens the
publisher's current page. The app never silently substitutes this average for the
user's interest rate. Future beta releases should refresh the snapshot and date
together. National conforming fixed-rate averages are not ARM index values or
personal loan offers.

Historical inputs in `RateProjection.kt` are calendar-year means of weekly
30-year fixed PMMS observations for 2015–2024, rounded to four decimals. Source:
[Freddie Mac historical workbook](https://www.freddiemac.com/pmms/docs/historicalweeklydata.xlsx),
retrieved September 12, 2026. Only complete years in that explicit window are used.

Variable mode models an initial fixed period (default five years), then annual
resets. At each reset the central scenario closes 25% of the gap between the prior
rate and the mean of those ten annual observations. Lower/higher scenarios subtract
or add one population standard deviation of the nine annual changes. The 25%
reversion coefficient is a modeling assumption, not a statistically fitted forecast.
Scenarios illustrate possible outcomes, not probabilities or confidence intervals.
The fixed mortgage history is a broad market proxy; actual ARMs use their contract's
index, margin, reset frequency, and caps. This beta does not model a specific lender's ARM.

Each change is limited by the entered annual cap (default 2 percentage points),
then by the initial rate ± lifetime cap (default 5 points), with a zero floor.
The same annual cap applies to the first and subsequent resets. Payments are
reamortized on the outstanding balance and remaining term at each annual reset.
The final payment clears residual floating-point balance. Zero-rate and fully
paid homes are supported. All calculator, comparison, and amortization totals use
this schedule. Comparison holds the entered initial rate and other assumptions
identical between 15- and 30-year terms.

## Purchase state

All 50 states and D.C. are covered in `StateEstimates.kt`. No state is assumed on
first launch. Changing state replaces both editable defaults.

* Property tax: **2024** effective rates, Table 2, from
  [Tax Foundation / Census ACS](https://taxfoundation.org/data/all/state/property-taxes-by-state-county/),
  retrieved September 12, 2026. Annual estimate = purchase price × effective rate.
  Existing-owner averages may understate a new purchase after reassessment. Local
  jurisdictions, exemptions, assessment ratios, and special assessments matter;
  replace the rate with the effective rate for the intended home when available.
* Home insurance: **2026** annual state averages for **$300,000 dwelling coverage**,
  from [Insurance.com](https://www.insurance.com/home-and-renters-insurance/home-insurance-basics/average-homeowners-insurance-rates-by-state),
  retrieved September 12, 2026. These are annual dollar premiums, not percentages
  of market value. The app does not scale them with the purchase price, because
  dwelling replacement cost differs from market value. Enter a property-specific
  annual quote for the appropriate coverage, deductible, and risk exposure.

Tax, insurance, and HOA stay constant throughout all scenarios. Totals exclude
down payment, closing costs, maintenance, and PMI. No future property appreciation,
insurance inflation, or tax law changes are assumed. Inputs and state selection
are retained locally between app sessions.
