# Mortgage Helper App (Android)

Mortgage calculator with fixed and illustrative variable interest rates, monthly
payment breakdown, amortization charts, and 15/30-year loan comparison.

Features include purchase-state tax/insurance defaults with manual overrides, dated
mortgage market context, and capped annual rate scenarios informed by historical
mortgage rates. See [estimate sources and methodology](docs/ESTIMATES.md).

## Build and test

Use a full JDK 17 or 21, Android SDK platform 34, and the Gradle wrapper:

```sh
bash ./gradlew testDebugUnitTest assembleDebug lintDebug
bash ./gradlew connectedDebugAndroidTest
```

The second command requires a running Android emulator or connected device.
Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Select a purchase state, review its tax and annual insurance defaults, and enter
home price, down payment, and initial interest rate. Turn on variable interest
to choose the initial fixed period, annual/lifetime caps, and a scenario. Calculate
shows the initial payment, projected annual payments, and whole-term cost.

Amounts are grouped with commas as you type. The Comparison tab can compare a
fixed-rate quote with the selected variable scenario for the same loan term.
The Early payoff tab supports recurring extra principal and a lump-sum payment,
showing interest saved, time saved, and custom loan costs.

New features start on a new branch from current `master`. Feature branches are
committed and pushed for review; merging into `master` requires owner approval.
