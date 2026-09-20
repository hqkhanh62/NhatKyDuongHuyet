package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Result extracted from a glucose meter display (Pro AI multi-layer OCR).
 *
 * @param value glucose in mmol/L, already auto-cleaned to the 2.0–30.0 range.
 * @param date normalized `yyyy-MM-dd` when the meter screen shows a date, else null.
 * @param time normalized `HH:mm` when the meter screen shows a clock, else null.
 * @param source which engine produced the value (`ML_KIT` or `PIXEL`).
 * @param rawText full OCR text of the meter screen (debugging + fallback parsing).
 * @param confidence heuristic 0f..1f confidence of the numeric reading.
 */
data class ScannedGlucoseResult(
    val value: Float,
    val date: String? = null,
    val time: String? = null,
    val source: String = "ML_KIT",
    val rawText: String = "",
    val confidence: Float = 0.8f
) {
    /** True when the date was read from the meter (vs. system fallback). */
    val dateFromMeter: Boolean get() = date != null

    /** True when the time was read from the meter (vs. system fallback). */
    val timeFromMeter: Boolean get() = time != null
}

/**
 * Multi-layer OCR parse of a meter screen: glucose value + clock + calendar.
 * Used by the Auto Import Pipeline to decide meter-vs-system date/time.
 */
data class MeterOcrParse(
    val glucose: Float?,
    val date: String?,
    val time: String?,
    val rawText: String,
    val confidence: Float
)

@Singleton
class GlucoseScanner @Inject constructor() {

    // Initialize ML Kit only when a real camera frame is processed.
    // This keeps the pure OCR parser usable from JVM unit tests.
    private val pixelReader = PixelGlucoseReader()
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun processImage(
        image: InputImage,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                // Use spatial OCR lines first: the main seven-segment reading is
                // much larger than DAY/AVG/date/time labels. Fall back to the
                // text-only parser when ML Kit does not expose line geometry.
                val value = if (visionText.textBlocks.isEmpty()) {
                    extractGlucose(rawText)
                } else {
                    // Do not fall back to the flattened full text when layout
                    // exists: that path mixes display digits with DAY/AVG/date
                    // labels and can turn 5.7 into another plausible number.
                    extractGlucose(visionText)
                }
                if (value != null) {
                    onResult(
                        ScannedGlucoseResult(
                            value = value,
                            date = extractDate(rawText),
                            time = extractTime(rawText),
                            source = "ML_KIT",
                            rawText = rawText,
                            confidence = estimateConfidence(rawText, value)
                        )
                    )
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun processHybrid(
        fullBitmap: Bitmap,
        rotationDegrees: Int,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val rotated = ImageUtils.rotateBitmap(fullBitmap, rotationDegrees)
        val displayRoi = ImageUtils.cropNormalized(rotated, ImageUtils.DISPLAY_ROI)
        
        // 1. Run Pixel Reader
        val pixelResult = pixelReader.processDisplay(displayRoi)
        
        // 2. Run ML Kit
        val inputImage = InputImage.fromBitmap(rotated, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                // Prefer the spatial (large-display) reading, same policy as processImage.
                val mlKitValue = if (visionText.textBlocks.isEmpty()) {
                    extractGlucose(rawText)
                } else {
                    extractGlucose(visionText)
                }

                val finalResult = combineHybrid(pixelResult, mlKitValue, rawText)
                onResult(finalResult)
            }
            .addOnFailureListener { error ->
                // If ML Kit fails, we might still have pixel result
                if (pixelResult != null && pixelResult.confidence >= PIXEL_AUTHORITATIVE_CONFIDENCE) {
                    onResult(
                        ScannedGlucoseResult(
                            value = pixelResult.value,
                            source = "PIXEL",
                            rawText = "",
                            confidence = pixelResult.confidence
                        )
                    )
                } else {
                    onError(error)
                }
            }
    }

    private fun combineHybrid(
        pixel: PixelDisplayReading?,
        mlKitValue: Float?,
        rawText: String
    ): ScannedGlucoseResult? {
        // As per instructions: if pixel reader is confident and matches ML Kit (or ML Kit is null)
        if (pixel != null && pixel.confidence >= PIXEL_AUTHORITATIVE_CONFIDENCE) {
            if (mlKitValue == null || abs(pixel.value - mlKitValue) <= HYBRID_TOLERANCE) {
                return ScannedGlucoseResult(
                    value = pixel.value,
                    date = extractDate(rawText),
                    time = extractTime(rawText),
                    source = "PIXEL",
                    rawText = rawText,
                    confidence = pixel.confidence
                )
            }
        }

        // If they differ significantly, return null to prompt manual confirmation (or scanning again)
        if (pixel != null && mlKitValue != null && abs(pixel.value - mlKitValue) > HYBRID_TOLERANCE) {
            return null
        }

        // Fallback to ML Kit if available
        return mlKitValue?.let {
            ScannedGlucoseResult(
                value = it,
                date = extractDate(rawText),
                time = extractTime(rawText),
                source = "ML_KIT",
                rawText = rawText,
                confidence = estimateConfidence(rawText, it)
            )
        }
    }

    private data class GlucoseCandidate(
        val value: Float,
        val score: Int,
        val position: Int
    )

    private data class SpatialGlucoseCandidate(
        val value: Float,
        val score: Int,
        val position: Int
    )

    /**
     * Selects the largest plausible numeric line from ML Kit's layout tree.
     * This prevents small `DAY`, `AVG`, date and time digits from winning over
     * the large central display value.
     */
    private fun extractGlucose(visionText: Text): Float? {
        val candidates = visionText.textBlocks
            .flatMap { it.lines }
            .mapIndexedNotNull { index, line ->
                // ML Kit may split a seven-segment reading into separate
                // elements: ["5", ".", "7"]. Rebuild the line with spaces so
                // the normalizer can recover both the decimal point and digit.
                val elementText = line.elements.joinToString(" ") { it.text }
                val lineText = elementText.ifBlank { line.text }
                val value = extractGlucose(lineText) ?: return@mapIndexedNotNull null
                val context = lineText.lowercase()
                val boxHeight = line.boundingBox?.height() ?: 0
                var score = boxHeight.coerceAtMost(1_000)
                if (line.text.contains('.') || line.text.contains(',')) score += 180
                if (context.contains("mmol") || context.contains("mg")) score += 300
                if (context.contains("day") || context.contains("avg") ||
                    context.contains("date") || context.contains("time") ||
                    context.contains("mem")) score -= 500
                SpatialGlucoseCandidate(value, score, index)
            }

        return candidates
            .sortedWith(compareByDescending<SpatialGlucoseCandidate> { it.score }.thenBy { it.position })
            .firstOrNull()
            ?.value
    }

    /**
     * Extracts a plausible glucose value from common meter output formats:
     * Extracts a plausible glucose value from common meter output formats:
     * 6.1, 6,1, 6 1, 110 mg/dL and 110 mg/dl.
     *
     * A number greater than 30 is not converted without an explicit mg/dL
     * context. This avoids turning an OCR error such as "81" into 4.5 mmol/L.
     */
    private fun extractGlucose(text: String): Float? {
        if (text.isBlank()) return null

val numberRegex = Regex(
            "(?<![0-9A-Za-z])([0-9OoQqIiLl|]{1,3}(?:\\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])"
        )
        val dateOrTimeRegex = Regex(
            "\\b[0-9]{1,4}[/\\-][0-9]{1,2}(?:[/\\-][0-9]{1,4})?\\b|" +
                "\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b"
        )
        val candidates = mutableListOf<GlucoseCandidate>()
        var absolutePosition = 0

        normalizeOcrText(text).lineSequence().forEach { line ->
            val lineContext = line.lowercase()
            val hasMmolUnit = lineContext.contains("mmol")
            val hasMgUnit = lineContext.contains("mg")
            val hasGlucoseLabel = lineContext.contains("glucose") ||
                lineContext.contains("sugar") ||
                lineContext.contains("result") ||
                lineContext.contains("value")

            // Date/time rows are noise unless the same row explicitly identifies
            // a glucose value or unit.
            val isDateOrTimeRow = dateOrTimeRegex.containsMatchIn(line)
            if (isDateOrTimeRow && !hasGlucoseLabel && !hasMmolUnit && !hasMgUnit) {
                absolutePosition += line.length + 1
                return@forEach
            }

            numberRegex.findAll(line).forEach { match ->
                val numericToken = normalizeNumericToken(match.groupValues[1])
                val rawValue = numericToken.toFloatOrNull() ?: return@forEach
                val convertedValue = when {
                    hasMgUnit -> rawValue / MG_DL_PER_MMOL
                    rawValue > 20f && !hasMmolUnit -> return@forEach
                    rawValue in MIN_GLUCOSE..MAX_GLUCOSE -> rawValue
                    else -> return@forEach
                }

                if (convertedValue !in MIN_GLUCOSE..MAX_GLUCOSE) return@forEach

                var score = 0
                if (hasMmolUnit) score += 100
                if (hasMgUnit) score += 90
                if (rawValue % 1f != 0f) score += 25
                if (convertedValue in 3f..20f) score += 10
                if (hasGlucoseLabel) score += 20

                candidates += GlucoseCandidate(
                    value = convertedValue,
                    score = score,
                    position = absolutePosition + match.range.first
                )
            }
            absolutePosition += line.length + 1
        }

        return candidates
            .sortedWith(compareByDescending<GlucoseCandidate> { it.score }.thenBy { it.position })
            .firstOrNull()
            ?.value
    }

    /**
     * Makes common OCR errors deterministic before numeric parsing.
     * Delimiter normalization is intentionally conservative: a blank is
     * treated as a decimal separator only when followed by a unit or EOL.
     */
    private fun normalizeOcrText(text: String): String {
        var normalized = text
            .replace('\u00A0', ' ')
            .replace('٫', '.')
            .replace('，', '.')
            .replace(',', '.')

        // 6 , 1 / 6 . 1 -> 6.1, including spaces around the delimiter.
        normalized = Regex("(?<=\\d)\\s*[.]\\s*(?=\\d)")
            .replace(normalized, ".")

        // Some seven-segment displays produce "6 1 mmol/L".
        normalized = Regex(
            "(?<!\\d)(\\d{1,2})\\s+(\\d)(?=\\s*(?:mmol|mg(?:/\\s*dl)?|$))",
            RegexOption.IGNORE_CASE
        ).replace(normalized, "$1.$2")

        return normalized
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

private fun normalizeNumericToken(token: String): String = token
        .replace('O', '0', ignoreCase = true)
        .replace('Q', '0', ignoreCase = true)
        .replace('I', '1', ignoreCase = true)
        .replace('L', '1', ignoreCase = true)
        .replace('|', '1')

    /**
     * Multi-layer OCR: clock recognition.
     *
     * Accepts `HH:mm` plus common meter/OCR variants (`HH.mm`, `HHhmm`, `HH;mm`)
     * and always normalizes to zero-padded `HH:mm`. Hours must be 0–23 and
     * minutes 0–59, otherwise the match is treated as noise.
     */
    private fun extractTime(text: String): String? {
        // Seven-segment colons are often misread as ';' or '.'.
        val normalized = text.replace(';', ':')
        val timeRegex = Regex("\\b([01]?\\d|2[0-3])\\s*[:.hH]\\s*([0-5]\\d)\\b")
        val match = timeRegex.find(normalized) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return "%02d:%02d".format(hour, minute)
    }

    /**
     * Multi-layer OCR: calendar recognition.
     *
     * Supported meter formats (all normalized to `yyyy-MM-dd`):
     * - `YYYY-MM-DD` / `YYYY/MM/DD`
     * - `DD/MM/YYYY`, `MM/DD/YYYY` (+ `-` and `.` separators)
     * - `DD/MM`, `MM/DD` (+ `-` separator, current year is assumed)
     *
     * Ambiguous `11/12`-style pairs default to the Vietnamese `DD/MM`
     * convention; pairs where one side exceeds 12 are disambiguated
     * (`25/12` → 25 Dec, `12/25` → 25 Dec). Out-of-range months/days
     * are rejected so OCR noise never becomes a diary date.
     */
    private fun extractDate(text: String): String? {
        // Year-first: 2026-08-20, 2026/08/20.
        Regex("\\b(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})\\b").find(text)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return@let
            var month = match.groupValues[2].toIntOrNull() ?: return@let
            var day = match.groupValues[3].toIntOrNull() ?: return@let
            if (month > 12 && day <= 12) {
                // Meter shows yyyy-dd-MM: swap back to yyyy-MM-dd.
                val tmp = month
                month = day
                day = tmp
            }
            return formatIsoDate(year, month, day)
        }

        // Day-first with 4-digit year: 20/08/2026, 20-08-2026, 20.08.2026.
        Regex("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})\\b").find(text)?.let { match ->
            val p1 = match.groupValues[1].toIntOrNull() ?: return@let
            val p2 = match.groupValues[2].toIntOrNull() ?: return@let
            val year = match.groupValues[3].toIntOrNull() ?: return@let
            val (day, month) = disambiguateDayMonth(p1, p2)
            return formatIsoDate(year, month, day)
        }

        // Short date without year: 20/08, 08-20. Current year is assumed.
        // NOTE: '.' is intentionally not a short-date separator: it would
        // match glucose decimals such as "5.7".
        Regex("\\b(\\d{1,2})[-/](\\d{1,2})\\b").find(text)?.let { match ->
            val p1 = match.groupValues[1].toIntOrNull() ?: return@let
            val p2 = match.groupValues[2].toIntOrNull() ?: return@let
            val (day, month) = disambiguateDayMonth(p1, p2)
            val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            return formatIsoDate(year, month, day)
        }

        return null
    }

    /**
     * Resolves an ambiguous day/month pair. Vietnamese meters use `DD/MM`,
     * so ties default to day-first; a side above 12 must be the day.
     */
    private fun disambiguateDayMonth(first: Int, second: Int): Pair<Int, Int> =
        when {
            first > 12 && second <= 12 -> first to second // DD/MM proven
            second > 12 && first <= 12 -> second to first // MM/DD proven
            else -> first to second // Ambiguous: Vietnamese DD/MM default
        }

    private fun formatIsoDate(year: Int, month: Int, day: Int): String? {
        if (year !in 1990..2100 || month !in 1..12 || day !in 1..31) return null
        return try {
            val calendar = java.util.Calendar.getInstance().apply {
                isLenient = false
                set(year, month - 1, day, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            // getTime() throws when the date does not exist (e.g. 31/02).
            calendar.time
            "%04d-%02d-%02d".format(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Heuristic confidence for the numeric reading: explicit units and a
     * decimal fraction are the strongest signals on meter displays.
     */
    private fun estimateConfidence(rawText: String, value: Float): Float {
        val context = rawText.lowercase()
        val hasDecimal = value % 1f != 0f || rawText.contains('.') || rawText.contains(',')
        return when {
            context.contains("mmol") && hasDecimal -> 0.95f
            context.contains("mmol") -> 0.9f
            context.contains("mg") -> 0.9f
            hasDecimal -> 0.85f
            else -> 0.7f
        }
    }

    /** Visible to JVM tests without exposing parsing internals to production callers. */
    internal fun extractGlucoseForTesting(text: String): Float? = extractGlucose(text)

    /** Visible to JVM tests without exposing parsing internals to production callers. */
    internal fun extractDateForTesting(text: String): String? = extractDate(text)

    /** Visible to JVM tests without exposing parsing internals to production callers. */
    internal fun extractTimeForTesting(text: String): String? = extractTime(text)

    /** Full multi-layer parse (glucose + date + time) for JVM tests. */
    internal fun parseMeterTextForTesting(text: String): MeterOcrParse {
        val glucose = extractGlucose(text)
        return MeterOcrParse(
            glucose = glucose,
            date = extractDate(text),
            time = extractTime(text),
            rawText = text,
            confidence = glucose?.let { estimateConfidence(text, it) } ?: 0f
        )
    }

    private companion object {
        const val MG_DL_PER_MMOL = 18.0f
    }
}