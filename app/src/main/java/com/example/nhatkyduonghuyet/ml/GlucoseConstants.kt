package com.example.nhatkyduonghuyet.ml

import com.example.nhatkyduonghuyet.domain.GlucosePolicy

/** Plausible glucose value range in mmol/L accepted from a meter display. */
const val MIN_GLUCOSE = GlucosePolicy.MIN_GLUCOSE_MMOL
const val MAX_GLUCOSE = GlucosePolicy.MAX_GLUCOSE_MMOL

/**
 * Confidence at which the pixel reading wins without an ML Kit match.
 * Segment-level thresholds (binarization, digit confidence, ...) live inside
 * [SevenSegmentDecoder] where they are tuned and tested together.
 */
const val PIXEL_AUTHORITATIVE_CONFIDENCE = 0.85f

/**
 * Confidence at which a pixel reading overrides a *disagreeing* ML Kit value.
 * ML Kit's Latin recognizer systematically confuses seven-segment digits, so a
 * crisp segment-level reading is trusted over a contradictory OCR guess.
 */
const val PIXEL_OVERRIDE_CONFIDENCE = 0.92f

/** Maximum absolute difference (mmol/L) tolerated between pixel and ML Kit readings. */
const val HYBRID_TOLERANCE = 0.15f
