package com.example.nhatkyduonghuyet.ml

/** Plausible glucose value range in mmol/L accepted from a meter display. */
const val MIN_GLUCOSE = 2.0f
const val MAX_GLUCOSE = 30.0f

/** Darkness ratio that marks a seven-segment element as lit. */
const val SEGMENT_ON_THRESHOLD = 0.28f

/** Darkness ratio that marks a decimal point as present. */
const val DECIMAL_POINT_THRESHOLD = 0.25f

/** Minimum confidence to accept an individual seven-segment digit. */
const val PIXEL_DIGIT_CONFIDENCE = 0.70f

/** Minimum combined confidence to accept the whole pixel reading. */
const val PIXEL_READING_CONFIDENCE = 0.78f

/** Confidence at which the pixel reading wins without an ML Kit match. */
const val PIXEL_AUTHORITATIVE_CONFIDENCE = 0.85f

/** Maximum relative deviation tolerated between pixel and ML Kit readings. */
const val HYBRID_TOLERANCE = 0.15f