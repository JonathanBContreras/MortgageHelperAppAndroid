package com.example.mortgagehelperapp

// Source dates and methodology: docs/ESTIMATES.md.
data class StateEstimate(val name: String, val taxPercent: Double, val annualInsurance: Double) {
    override fun toString() = name
}

object StateEstimates {
    val all = listOf(
        StateEstimate("Alabama", 0.37, 3716.0),
        StateEstimate("Alaska", 0.94, 1492.0),
        StateEstimate("Arizona", 0.48, 2397.0),
        StateEstimate("Arkansas", 0.56, 3195.0),
        StateEstimate("California", 0.7, 1653.0),
        StateEstimate("Colorado", 0.5, 5511.0),
        StateEstimate("Connecticut", 1.54, 2132.0),
        StateEstimate("Delaware", 0.54, 1461.0),
        StateEstimate("District of Columbia", 0.6, 1558.0),
        StateEstimate("Florida", 0.78, 8471.0),
        StateEstimate("Georgia", 0.79, 2301.0),
        StateEstimate("Hawaii", 0.29, 738.0),
        StateEstimate("Idaho", 0.5, 2412.0),
        StateEstimate("Illinois", 1.88, 2802.0),
        StateEstimate("Indiana", 0.76, 2869.0),
        StateEstimate("Iowa", 1.33, 3148.0),
        StateEstimate("Kansas", 1.21, 5289.0),
        StateEstimate("Kentucky", 0.74, 4471.0),
        StateEstimate("Louisiana", 0.55, 5185.0),
        StateEstimate("Maine", 0.98, 1299.0),
        StateEstimate("Maryland", 0.92, 2242.0),
        StateEstimate("Massachusetts", 1.0, 2112.0),
        StateEstimate("Michigan", 1.19, 3071.0),
        StateEstimate("Minnesota", 1.0, 3333.0),
        StateEstimate("Mississippi", 0.58, 2602.0),
        StateEstimate("Missouri", 0.89, 3783.0),
        StateEstimate("Montana", 0.61, 3221.0),
        StateEstimate("Nebraska", 1.44, 5513.0),
        StateEstimate("Nevada", 0.5, 1876.0),
        StateEstimate("New Hampshire", 1.5, 1324.0),
        StateEstimate("New Jersey", 1.88, 1449.0),
        StateEstimate("New Mexico", 0.63, 3497.0),
        StateEstimate("New York", 1.3, 1844.0),
        StateEstimate("North Carolina", 0.66, 3799.0),
        StateEstimate("North Dakota", 0.92, 2846.0),
        StateEstimate("Ohio", 1.36, 2109.0),
        StateEstimate("Oklahoma", 0.79, 5378.0),
        StateEstimate("Oregon", 0.81, 1647.0),
        StateEstimate("Pennsylvania", 1.26, 1434.0),
        StateEstimate("Rhode Island", 1.12, 2379.0),
        StateEstimate("South Carolina", 0.49, 2870.0),
        StateEstimate("South Dakota", 1.0, 3740.0),
        StateEstimate("Tennessee", 0.52, 3198.0),
        StateEstimate("Texas", 1.4, 4582.0),
        StateEstimate("Utah", 0.48, 1771.0),
        StateEstimate("Vermont", 1.51, 1017.0),
        StateEstimate("Virginia", 0.78, 1939.0),
        StateEstimate("Washington", 0.75, 1766.0),
        StateEstimate("West Virginia", 0.51, 1961.0),
        StateEstimate("Wisconsin", 1.32, 1836.0),
        StateEstimate("Wyoming", 0.53, 2075.0)
    )
}
