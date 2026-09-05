package com.example.nhatkyduonghuyet.ml

import com.example.nhatkyduonghuyet.domain.GlucosePolicy

/** Plausible glucose value range in mmol/L accepted from a meter display. */
const val MIN_GLUCOSE = GlucosePolicy.MIN_GLUCOSE_MMOL
const val MAX_GLUCOSE = GlucosePolicy.MAX_GLUCOSE_MMOL

/** How many frames must agree before a value is offered to the user. */
const val STABILITY_REQUIRED_MATCHES = 3

/** Size of the sliding window of recent frame results. */
const val STABILITY_WINDOW_SIZE = 6
