package com.example.nhatkyduonghuyet.ml

/** Non-numeric states a meter can display instead of a result. */
enum class MeterStatus { HIGH, LOW, ERROR }

/**
 * What a single OCR line says about the glucose value, together with the evidence that
 * supports it. The caller adds layout information (text height) before picking a winner.
 */
data class GlucoseTextEvidence(
    /** Value already converted to mmol/L. */
    val value: Float,
    val hasUnit: Boolean,
    val hasDecimal: Boolean,
    val hasLabel: Boolean,
    val convertedFromMgDl: Boolean
) {
    /** Evidence strong enough to trust the line on its own. */
    val isStrong: Boolean get() = hasUnit || hasDecimal
}

/**
 * Parses glucose values out of OCR text.
 *
 * Pure JVM code (no Android, no ML Kit) so every rule below is unit-tested. The rules are
 * deliberately conservative: for a medical value, refusing to read is much cheaper than
 * reading the wrong number.
 */
object GlucoseTextParser {

    private const val MG_DL_PER_MMOL = 18.0f

    /** Status words. `Lo` must never become the number 10 through OCR digit substitution. */
    private val statusRegex = Regex(
        """(^|[^a-z0-9])(hi{1,2}|high|lo{1,2}|low|err?|e-?\d|-{2,})([^a-z0-9]|$)""",
        RegexOption.IGNORE_CASE
    )

    /** Screens that do not show a fresh measurement. */
    private val summaryRegex = Regex(
        """\b(avg|average|aver|mean|ctrl|control|hct|ket|ketone|hba1c|a1c|\d{1,2}\s*(?:day|days|d)\b)""",
        RegexOption.IGNORE_CASE
    )

    // A dot is NOT treated as a date separator: "5.7" is a reading, not the 5th of July.
    private val dateTimeRegex = Regex(
        """\b\d{1,4}[/\-]\d{1,2}(?:[/\-]\d{1,4})?\b""" +
            """|\b\d{1,2}\.\d{1,2}\.\d{2,4}\b""" +
            """|\b(?:[01]?\d|2[0-3]):[0-5]\d\b"""
    )

    private val numberRegex = Regex(
        """(?<![0-9A-Za-z.])([0-9OoQqIiLl|]{1,3}(?:\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])"""
    )

    private val labelRegex = Regex(
        """glucose|sugar|result|value|blood|bg\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Detects a meter status such as `HI`, `Lo` or `E-3`.
     * Returns null when the text also contains a plausible numeric reading.
     */
    fun status(text: String): MeterStatus? {
        val match = statusRegex.find(normalise(text)) ?: return null
        val token = match.groupValues[2].lowercase()
        return when {
            token.startsWith("hi") || token == "high" -> MeterStatus.HIGH
            token.startsWith("lo") || token == "low" -> MeterStatus.LOW
            else -> MeterStatus.ERROR
        }
    }

    /** Best evidence over all lines of [text], or null when nothing plausible was found. */
    fun parse(text: String): GlucoseTextEvidence? =
        normalise(text).lineSequence()
            .mapNotNull { parseLine(it) }
            .maxByOrNull { score(it) }

    /**
     * Parses one OCR line.
     *
     * @param allowBareInteger set by the caller when the line is known to be the dominant
     * text on the display *and* the app is configured for a mg/dL meter. Seven-segment OCR
     * regularly loses the decimal point, so `57` must not silently become 5.7 or 57.
     */
    fun parseLine(line: String, allowBareInteger: Boolean = false): GlucoseTextEvidence? {
        val normalised = normalise(line)
        if (normalised.isBlank()) return null
        val lower = normalised.lowercase()

        // Averages, control solution, ketone and HbA1c screens are not a measurement.
        if (summaryRegex.containsMatchIn(lower)) return null
        if (statusRegex.containsMatchIn(lower) && !normalised.any { it.isDigit() }) return null

        val hasMmol = lower.contains("mmol")
        val hasMgDl = !hasMmol && MG_DL_REGEX.containsMatchIn(lower)
        val hasLabel = labelRegex.containsMatchIn(lower)

        // Remove the date/time substrings instead of discarding the whole line: meters often
        // print "08:32   5.7 mmol/L" on one line and the value must survive.
        val stripped = dateTimeRegex.replace(normalised, " ")

        val tokens = numberRegex.findAll(stripped).mapNotNull { match ->
            val raw = match.groupValues[1]
            val token = normaliseDigits(raw) ?: return@mapNotNull null
            val number = token.toFloatOrNull() ?: return@mapNotNull null
            Token(number, token.contains('.'))
        }.toList()
        if (tokens.isEmpty()) return null

        val decimals = tokens.filter { it.hasDecimal }
        val candidates = when {
            decimals.isNotEmpty() -> decimals
            // Several bare integers on one line is the signature of a date or a clock whose
            // separator the OCR dropped ("10 24"), never of a glucose reading.
            tokens.size > 1 -> return null
            hasMmol || hasMgDl || allowBareInteger -> tokens
            else -> return null
        }

        var best: GlucoseTextEvidence? = null
        for (token in candidates) {
            val mmol = when {
                hasMgDl -> token.value / MG_DL_PER_MMOL
                hasMmol -> token.value
                token.hasDecimal -> token.value
                allowBareInteger -> token.value / MG_DL_PER_MMOL
                else -> continue
            }
            if (mmol < MIN_GLUCOSE || mmol > MAX_GLUCOSE) continue
            val evidence = GlucoseTextEvidence(
                value = mmol,
                hasUnit = hasMmol || hasMgDl,
                hasDecimal = token.hasDecimal,
                hasLabel = hasLabel,
                convertedFromMgDl = hasMgDl || (allowBareInteger && !hasMmol && !token.hasDecimal)
            )
            if (best == null || score(evidence) > score(best!!)) best = evidence
        }
        return best
    }

    /** Ranking used when several lines carry a plausible value. */
    fun score(evidence: GlucoseTextEvidence): Int {
        var score = 0
        if (evidence.hasUnit) score += 100
        if (evidence.hasDecimal) score += 40
        if (evidence.hasLabel) score += 20
        if (evidence.value in 3f..20f) score += 10
        return score
    }

    private data class Token(val value: Float, val hasDecimal: Boolean)

    private fun normalise(text: String): String {
        var normalised = text
            .replace('\u00A0', ' ')
            .replace('\u066B', '.')
            .replace('\uFF0C', '.')
            .replace(',', '.')
            .replace('·', '.')
        // "6 . 1" and "6. 1" are the same reading as "6.1".
        normalised = Regex("""(?<=\d)\s*\.\s*(?=\d)""").replace(normalised, ".")
        // Seven-segment OCR often splits "5.7" into "5 7"; only join when a unit or the end
        // of the line follows, so "10 24" (a clock) is left alone.
        normalised = Regex(
            """(?<![\d.])(\d{1,2})\s+(\d)(?=\s*(?:mmol|mg\s*/?\s*dl|$))""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        ).replace(normalised) { "${it.groupValues[1]}.${it.groupValues[2]}" }
        return normalised.replace(Regex("""[ \t]+"""), " ").trim()
    }

    /**
     * Fixes the letters an OCR engine confuses with digits — but only inside a token that
     * already contains at least one digit. Without that guard `Lo` (a hypoglycaemia warning)
     * turns into the number 10.
     */
    private fun normaliseDigits(token: String): String? {
        if (token.none { it.isDigit() }) return null
        return token
            .replace('O', '0').replace('o', '0')
            .replace('Q', '0').replace('q', '0')
            .replace('I', '1').replace('i', '1')
            .replace('L', '1').replace('l', '1')
            .replace('|', '1')
    }

    private val MG_DL_REGEX = Regex("""mg\s*/?\s*d?l?""", RegexOption.IGNORE_CASE)
}
