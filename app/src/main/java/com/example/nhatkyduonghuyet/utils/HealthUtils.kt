package com.example.nhatkyduonghuyet.utils

fun estimateHbA1c(avgGlucose: Double): Double {
    return (avgGlucose + 46.7) / 28.7
}

fun checkWarning(values: List<Int>): String? {
    val highCount = values.count { it > 250 }
    val lowCount = values.count { it < 70 }

    return when {
        highCount >= 3 -> "⚠️ Đường huyết cao nguy hiểm"
        lowCount >= 3 -> "⚠️ Hạ đường huyết nguy hiểm"
        else -> null
    }
}
