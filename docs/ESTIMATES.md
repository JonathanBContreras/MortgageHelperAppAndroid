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

## Fixed versus variable comparison

The Comparison tab's action holds principal, term, property tax, insurance, and
HOA constant and compares the entered fixed-rate quote against the calculator's
initial rate and variable settings. If the calculator is in fixed mode, variable
comparison uses the documented default five-year initial period, 2-point annual
cap, 5-point lifetime cap, and historical central scenario; these assumptions
appear above the comparison action. Initial and peak monthly costs include the
same ownership costs. The total-cost difference therefore equals the difference
in interest, not a difference in taxes or insurance. The existing 15/30-year
comparison remains available below this action.

## Early payoff

Extra payments are principal-only additions to the selected calculator schedule:
a recurring monthly amount beginning at a specified month, plus an optional
one-time lump sum at another specified month. Month 1 is the loan's first payment.
Interest accrues on the opening monthly balance before that month's scheduled
and extra principal payments. Payments are capped at the amount needed to clear
the balance; subsequent payments stop. A lump sum scheduled after payoff has no
effect. Amounts use full precision internally and are displayed to cents.

The borrower maintains the baseline principal-and-interest payment schedule,
including its projected changes for a variable loan, rather than recasting to a
lower required payment after prepayment. Actual lender servicing and reset rules
may differ. The tab identifies this assumption. This is a scenario from loan
origination, not a payoff quote for an existing loan at an unspecified date.

Savings are baseline interest minus custom-scenario interest. Principal is still
repaid; taxes, insurance, and HOA continue and are never counted as avoided costs.
The results also show payoff month, months saved, principal plus interest costs,
and the last loan payment. No prepayment penalties, fees, tax deductions, or
alternative investment returns are modeled. See the CFPB explanation of
[prepayment penalties](https://www.consumerfinance.gov/ask-cfpb/what-is-a-prepayment-penalty-en-1957/).
