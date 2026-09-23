package com.example.nhatkyduonghuyet.ml

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * OCR đa tầng cho màn hình máy đo đường huyết (tầng "Pro AI").
 *
 * Tầng 1 – chỉ số: số có dạng X.X mmol/L, kèm chuẩn hoá lỗi đọc seven-segment.
 * Tầng 2 – giờ:    HH:mm, HH:mm:ss và dạng 12 giờ có AM/PM.
 * Tầng 3 – ngày:   DD/MM, MM/DD, DD/MM/YYYY và YYYY-MM-DD (đã kiểm tra hợp lệ).
 * Ngoài ra: mã lỗi máy đo (E-05, HI, LO…) để không bao giờ lưu dữ liệu nhiễu.
 *
 * File này cố tình không import Android/ML Kit: toàn bộ logic parse chạy được
 * trên JVM unit test. [tools/prototype_meter_text_parser.py] là bản
 * transliterate Python của cùng logic, dùng để đối chiếu regex.
 */
object MeterTextParser {

    // ------------------------------------------------------------------ tầng 0
    private val COLON_DECIMAL = Regex("(?<![0-9])([0-9])\\s*:\\s*([0-9])(?![0-9])")
    private val SPACED_DOT = Regex("(?<=\\d)\\s*[.]\\s*(?=\\d)")
    private val SPACE_DECIMAL = Regex(
        "(?<!\\d)(\\d{1,2})\\s+(\\d)(?=\\s*(?:mmol|mg(?:/\\s*dl)?|$))",
        RegexOption.IGNORE_CASE
    )
    private val MULTI_SPACE = Regex("[ \\t]+")
    private val BLANK_RUN = Regex("[\\s-]")

    // ------------------------------------------------------------------ tầng 1
    private val NUMBER =
        Regex("(?<![0-9A-Za-z])([0-9OoQqIiLl|]{1,3}(?:\\.[0-9OoQqIiLl|]{1,2})?)(?![0-9A-Za-z])")
    private val DATE_OR_TIME_ROW = Regex(
        "\\b[0-9]{1,4}[/\\-][0-9]{1,2}(?:[/\\-][0-9]{1,4})?\\b|" +
            "\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b"
    )
    private val UNIT_HINTS = listOf("mmol", "mg")
    private val LABEL_HINTS = listOf("glucose", "sugar", "result", "value")
    private val NOISE_HINTS = listOf("day", "avg", "date", "time", "mem", "max", "min")

    // ------------------------------------------------------------------ tầng 2
    private val TIME_LABEL = Regex("(time|giờ|gio|clock)", RegexOption.IGNORE_CASE)
    private val COLON_TIME =
        Regex("(?<![\\d:.])((?:[01]?\\d|2[0-3])):([0-5]\\d)(?::([0-5]\\d))?(?![\\d:])")
    private val LABELLED_TIME =
        Regex("(?<![\\d:.])((?:[01]?\\d|2[0-3]))[.:]([0-5]\\d)(?::([0-5]\\d))?(?![\\d:])")
    private val MERIDIEM_TIME = Regex(
        "(?<!\\d)(1[0-2]|[1-9])[:.]([0-5]\\d)\\s*([AP])\\.?\\s*M?(?![0-9A-Za-z])",
        RegexOption.IGNORE_CASE
    )

    // ------------------------------------------------------------------ tầng 3
    private val DATE_LABEL = Regex("(date|ngày|ngay|dd/mm|yyyy)", RegexOption.IGNORE_CASE)
    private val ISO_DATE = Regex("(?<!\\d)(\\d{4})([-/.])(\\d{1,2})\\2(\\d{1,2})(?!\\d)")
    private val FULL_DATE = Regex("(?<!\\d)(\\d{1,2})([-/.])(\\d{1,2})\\2(\\d{2,4})(?!\\d)")
    private val SHORT_DATE = Regex("(?<!\\d)(\\d{1,2})([-/])(\\d{1,2})(?!\\d)")

    // ------------------------------------------------- chế độ đọc chữ nhỏ
    /**
     * Cặp số cạnh nhau mà ML Kit hay đọc lẫn chữ cái thành số (dùng cho mm-dd, hh:mm).
     *
     * Dấu chấm cố tình bị loại: chỉ số thập phân cũng dùng nó, nên "5.O" mà được
     * "sửa" thành "5.0" là AI đổi luôn đường huyết của người bệnh. Ngày/giờ viết
     * bằng dấu chấm chỉ được sửa khi cả hai vế đủ 2 chữ số ("09.23", "14.35").
     */
    private val NUMERIC_PAIR = Regex(
        "[0-9OoQlIi|]{1,2}\\s*[:;\\-]\\s*[0-9OoSsZzGbBlIi|]{1,2}(?:\\s*[:;.\\-]\\s*[0-9OoSsZz]{1,2})?"
    )
    private val NUMERIC_PAIR_DOT = Regex("[0-9OoQDlIi]{2}\\.\\s*[0-9OoSsZzGbB]{2}")
    /** "09-2314:35" - hai trường dính liền vì ML Kit không thấy khoảng trắng. */
    private val GLUED_DATE_TIME = Regex("(\\d{1,2}[-/.]\\d{1,2})(\\d{1,2}[:;]\\d{2})")
    private val GLUED_TIME_DATE = Regex("(\\d{1,2}[:;]\\d{2})(\\d{1,2}[-/.]\\d{1,2})")
    /** Cặp số dính liền: xoá giờ trước khi tìm ngày, chỉ dùng : và ; để dấu "."
     *  không ăn mất ngày dạng "09.23". */
    private val LOOSE_TIME_STRIP =
        Regex("(?<![\\d:;])([0-9OoQlIi|]{1,2})\\s*[:;]\\s*([0-5][0-9OoSsZz])(?:\\s*[:;]\\s*\\d{1,2})?(?![\\d:])")
    /** Giờ ở dòng chữ nhỏ: chấp nhận ";" hoặc "." thay cho ":" vì LCD mờ dễ nhầm. */
    private val LOOSE_TIME =
        Regex("(?<![\\d:])([0-9OoQlIi|]{1,2})\\s*[:;.]\\s*([0-5][0-9OoSsZz]){1}(?:\\s*[:;.]\\s*\\d{1,2})?(?![\\d:])")
    private val DIGIT_MAP = mapOf(
        'O' to '0', 'o' to '0', 'Q' to '0', 'q' to '0', 'D' to '0',
        'l' to '1', 'I' to '1', 'i' to '1', '|' to '1', '!' to '1',
        'Z' to '2', 'z' to '2',
        'S' to '5', 's' to '5',
        'G' to '6', 'g' to '6',
        'B' to '8', 'b' to '8'
    )

    private val SHORT_DOT_DATE = Regex("(?<!\\d)(\\d{2})[.](\\d{2})(?!\\d)")
    /** Dong chu nho: LCD mo khien "-" thuong bi doc thanh ".", nen cho phep ca 3. */
    private val LOOSE_SHORT_DATE = Regex("(?<!\\d)(\\d{1,2})([-/.])(\\d{1,2})(?!\\d)")
    private val ERROR_CODE = Regex(
        "(?<![0-9A-Za-z])(E[\\s-]?\\d{1,3}|ERR(?:OR)?|HI|LO)(?![0-9A-Za-z])",
        RegexOption.IGNORE_CASE
    )

    /** Ngày máy đo vượt quá hôm nay số ngày này bị coi là đồng hồ máy sai (Auto Clean). */
    private const val MAX_FUTURE_DAYS = 1
    private const val MAX_PAST_YEARS = 5
    private const val MIN_SUPPORTED_YEAR = 2000
    private const val MAX_SUPPORTED_YEAR = 2100

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val RECENCY_TIE_DAYS = 3L
    /**
     * Lon hon khoang cach 0.05 giu cach hieu chinh (0.60) va cach hieu phu (0.55),
     * de "khop ngay hom nay" du suc dao ket luan - nhung chi khi that su khop.
     */
    private const val RECENCY_BONUS = 0.10f

    /** Nguồn phụ phải tin cậy hơn nguồn chính khoản này mới được thay. */
    private const val MERGE_TIE_MARGIN = 0.05f

    /** Dòng thắng cuộc phải cao hơn median cỡ này lần mới được coi là số lớn giữa màn hình. */
    private const val DOMINANT_LINE_RATIO = 1.6f

    private const val MG_DL_PER_MMOL = 18.0f

    private data class Candidate(
        val value: Float,
        val score: Int,
        val position: Int,
        val hasUnit: Boolean,
        val hasDecimal: Boolean
    )

    private data class LinePick(val reading: GlucoseReading, val score: Int, val index: Int)

    private data class TimeCandidate(
        val hour: Int,
        val minute: Int,
        val confidence: Float,
        val penalty: Int,
        val lineIndex: Int
    )

    private data class DateCandidate(
        val year: Int,
        val month: Int,
        val day: Int,
        val confidence: Float,
        val ambiguous: Boolean,
        val lineIndex: Int
    )

    private data class DayMonth(val day: Int, val month: Int, val ambiguous: Boolean, val swapped: Boolean)

    private data class ShortPair(
        val first: Int,
        val second: Int,
        val confidence: Float,
        /** "-" = máy đo để kiểu MM/DD; "/" hoặc "." = kiểu DD/MM. */
        val dashSeparated: Boolean = false
    )

    // Kotlin khong cung cap toan tu so sanh cho Triple/Pair o day, nen thu tu
    // uu tien duoc viet ro rang: tin cay cao -> it moi hon -> dong som hon.
    private fun isBetterTime(candidate: TimeCandidate, current: TimeCandidate?): Boolean {
        if (current == null) return true
        if (candidate.confidence != current.confidence) return candidate.confidence > current.confidence
        if (candidate.penalty != current.penalty) return candidate.penalty < current.penalty
        return candidate.lineIndex < current.lineIndex
    }

    private fun isBetterDate(candidate: DateCandidate, current: DateCandidate?): Boolean {
        if (current == null) return true
        if (candidate.confidence != current.confidence) return candidate.confidence > current.confidence
        return candidate.lineIndex < current.lineIndex
    }

    /**
     * Chuẩn hoá ký tự dễ nhầm trước khi parse. Dấu hai chấm giữa đúng một chữ số
     * mỗi bên thành dấu thập phân ("5:7" -> "5.7"), còn giờ thật (08:32, 8:30)
     * giữ nguyên vì phút luôn có hai chữ số.
     */
    fun normalizeDisplayText(text: String): String {
        var normalized = text
            .replace('\u00A0', ' ')
            .replace('٫', '.')
            .replace('，', '.')
            .replace(',', '.')
        normalized = COLON_DECIMAL.replace(normalized, "$1.$2")
        return MULTI_SPACE.replace(normalized, " ").trim()
    }

    /** Tầng 1 trên một dòng: mọi giá trị hợp lệ kèm điểm ưu tiên. */
    private fun lineCandidates(line: String, offset: Int = 0): List<Candidate> {
        val context = line.lowercase(Locale.US)
        val hasMmolUnit = context.contains("mmol")
        val hasMgUnit = context.contains("mg")
        val hasGlucoseLabel = LABEL_HINTS.any { context.contains(it) }

        // Dòng ngày/giờ là nhiễu, trừ khi chính nó ghi đơn vị hoặc nhãn chỉ số.
        if (DATE_OR_TIME_ROW.containsMatchIn(line) && !hasMmolUnit && !hasMgUnit && !hasGlucoseLabel) {
            return emptyList()
        }
        val spaced = SPACE_DECIMAL.replace(line, "$1.$2")
        val normalized = SPACED_DOT.replace(spaced, ".")

        val output = mutableListOf<Candidate>()
        for (match in NUMBER.findAll(normalized)) {
            val token = match.groupValues[1]
            // Token chỉ toàn chữ ("Lo") không bao giờ được thành 10.
            val hasRealDigit = token.any { it in '0'..'9' }
            if (!hasRealDigit && !hasMmolUnit && !hasMgUnit) continue
            val rawValue = normalizeNumericToken(token).toFloatOrNull() ?: continue

            val convertedValue: Float? = when {
                hasMgUnit -> rawValue / MG_DL_PER_MMOL
                // Mất dấu thập phân: máy mmol/L luôn có 1 chữ số sau dấu phẩy.
                hasMmolUnit && rawValue > MAX_GLUCOSE && rawValue <= 350f -> rawValue / 10f
                rawValue > 20f && !hasMmolUnit -> null
                rawValue in MIN_GLUCOSE..MAX_GLUCOSE -> rawValue
                else -> null
            }
            val glucoseValue = convertedValue ?: continue
            if (glucoseValue !in MIN_GLUCOSE..MAX_GLUCOSE) continue

            var score = 0
            if (hasMmolUnit) score += 100
            if (hasMgUnit) score += 90
            if (rawValue % 1f != 0f) score += 25
            if (glucoseValue in 3f..20f) score += 10
            if (hasGlucoseLabel) score += 20
            output += Candidate(
                value = glucoseValue,
                score = score,
                position = offset + match.range.first,
                hasUnit = hasMmolUnit || hasMgUnit,
                hasDecimal = rawValue % 1f != 0f
            )
        }
        return output
    }

    /** Tầng 1 trên văn bản thô (khi ML Kit không trả về layout). */
    fun extractGlucose(text: String): Float? = extractReading(text)?.value

    /** Tầng 1 trên văn bản thô, kèm độ tin cậy. */
    fun extractReading(text: String): GlucoseReading? {
        if (text.isBlank()) return null
        val lines = normalizeDisplayText(text).lines()
        val candidates = mutableListOf<Candidate>()
        var absolutePosition = 0
        for (line in lines) {
            candidates += lineCandidates(line, absolutePosition)
            absolutePosition += line.length + 1
        }
        val best = candidates
            .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.position })
            .firstOrNull()
            ?: return null

        var confidence = 0.45f
        if (best.hasUnit) confidence += 0.20f
        if (best.hasDecimal) confidence += 0.15f
        return GlucoseReading(
            value = best.value,
            confidence = confidence.coerceIn(0f, 1f),
            fromSpatialLine = false,
            hasUnit = best.hasUnit,
            hasDecimal = best.hasDecimal
        )
    }

    /**
     * Tầng 1 dùng thông tin layout của ML Kit: chọn dòng có chữ to nhất trong
     * những dòng hợp lý. Đây là cách phân biệt số lớn ở giữa màn hình với các
     * chữ số nhỏ của nhãn DAY/AVG/ngày/giờ.
     */
    fun extractReadingFromLines(lines: List<OcrLine>): GlucoseReading? {
        if (lines.isEmpty()) return null
        val medianHeight = medianLineHeight(lines)
        val picks = mutableListOf<LinePick>()

        for (index in lines.indices) {
            val line = lines[index]
            val normalized = normalizeDisplayText(line.text)
            val candidates = lineCandidates(normalized)
            if (candidates.isEmpty()) continue
            val best = candidates.maxByOrNull { it.score } ?: continue
            val lower = normalized.lowercase(Locale.US)
            val hasUnit = UNIT_HINTS.any { lower.contains(it) }
            val hasDecimal = normalized.contains('.') || normalized.contains(',')
            val dominantThreshold = (medianHeight * DOMINANT_LINE_RATIO).roundToInt()
            val isDominant = medianHeight > 0 && line.heightPx >= dominantThreshold

            var score = best.score + line.heightPx.coerceAtMost(1_000)
            if (hasDecimal) score += 180
            if (hasUnit) score += 300
            if (NOISE_HINTS.any { lower.contains(it) }) score -= 500

            var confidence = 0.35f
            if (hasUnit) confidence += 0.25f
            if (hasDecimal) confidence += 0.20f
            if (isDominant) confidence += 0.15f

            picks += LinePick(
                reading = GlucoseReading(
                    value = best.value,
                    confidence = confidence.coerceIn(0f, 1f),
                    fromSpatialLine = true,
                    hasUnit = hasUnit,
                    hasDecimal = hasDecimal
                ),
                score = score,
                index = index
            )
        }

        return picks
            .sortedWith(compareByDescending<LinePick> { it.score }.thenBy { it.index })
            .firstOrNull()
            ?.reading
    }

    private fun medianLineHeight(lines: List<OcrLine>): Int {
        val heights = lines.map { it.heightPx }.filter { it > 0 }.sorted()
        if (heights.isEmpty()) return 0
        return heights[heights.size / 2]
    }

    /**
     * Tầng 2: giờ trên màn hình máy đo. Hỗ trợ 08:32, 8:32, 09:15:30,
     * 7:30 AM và 7.45 PM. Dạng dùng dấu chấm ("8.30") chỉ được chấp nhận khi
     * dòng có nhãn Time/giờ hoặc kèm AM/PM, để "5.7" không bao giờ thành giờ.
     */
    fun extractTime(text: String): MeterTime? = extractTime(text, loose = false)

    private fun extractTime(text: String, loose: Boolean): MeterTime? {
        if (text.isBlank()) return null
        var best: TimeCandidate? = null
        val lines = normalizeDisplayText(text).lines()

        for (index in lines.indices) {
            val line = normalizeDisplayText(lines[index])
            if (line.isBlank()) continue
            val labelled = TIME_LABEL.containsMatchIn(line)

            if (!labelled) {
                val meridiem = MERIDIEM_TIME.find(line)
                if (meridiem != null) {
                    val hourValue = meridiem.groupValues[1].toIntOrNull()
                    val minuteValue = meridiem.groupValues[2].toIntOrNull()
                    if (hourValue != null && minuteValue != null && hourValue in 1..12 && minuteValue <= 59) {
                        val isPm = meridiem.groupValues[3].equals("p", ignoreCase = true)
                        val candidate = TimeCandidate(
                            hour = (hourValue % 12) + if (isPm) 12 else 0,
                            minute = minuteValue,
                            confidence = 0.85f,
                            penalty = 0,
                            lineIndex = index
                        )
                        if (isBetterTime(candidate, best)) best = candidate
                    }
                }
            }

            val pattern = if (labelled) LABELLED_TIME else COLON_TIME
            for (match in pattern.findAll(line)) {
                val hour = match.groupValues[1].toIntOrNull() ?: continue
                val minute = match.groupValues[2].toIntOrNull() ?: continue
                if (hour > 23 || minute > 59) continue
                val candidate = TimeCandidate(
                    hour = hour,
                    minute = minute,
                    confidence = if (labelled) 1.0f else 0.8f,
                    penalty = if (match.groupValues[3].isNotEmpty()) 30 else 0,
                    lineIndex = index
                )
                if (isBetterTime(candidate, best)) best = candidate
            }
        }

        // Che do doc chu nho: ": " bi doc thanh ";" hoac "." tren LCD mo.
        if (loose) {
            for (index in lines.indices) {
                val line = normalizeDisplayText(repairOcrDigits(lines[index]))
                if (line.isBlank()) continue
                val labelled = TIME_LABEL.containsMatchIn(line)
                for (match in LOOSE_TIME.findAll(line)) {
                    val hour = normalizeNumericToken(match.groupValues[1]).toIntOrNull() ?: continue
                    val minute = normalizeNumericToken(match.groupValues[2]).toIntOrNull() ?: continue
                    if (hour > 23 || minute > 59) continue
                    val candidate = TimeCandidate(
                        hour = hour,
                        minute = minute,
                        confidence = if (labelled) 0.85f else 0.66f,
                        penalty = if (labelled) 10 else 45,
                        lineIndex = index
                    )
                    if (isBetterTime(candidate, best)) best = candidate
                }
            }
        }

        return best?.let { MeterTime(it.hour, it.minute, it.confidence) }
    }

    /**
     * Tầng 3: ngày trên màn hình máy đo, chuẩn hoá về yyyy-MM-dd.
     *
     * Thứ tự ưu tiên: YYYY-MM-DD -> D/M/YYYY (hoặc D.M.YYYY) -> DD/MM không năm.
     * Nếu hai vế đều <= 12 thì hiểu theo kiểu Việt Nam (ngày trước) và đánh dấu
     * [MeterDate.ambiguous] để người dùng kiểm lại. Ngày lệch hơn 1 ngày so với
     * máy hoặc quá cũ 5 năm bị loại.
     */
    fun extractDate(
        text: String,
        fallbackYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        todayIso: String? = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ): MeterDate? = extractDate(text, fallbackYear, todayIso, loose = false)

    /**
     * @param loose chế độ đọc dòng chữ nhỏ: số đã được sửa lỗi nhận dạng, dấu
     *   chấm/phẩy_chấm cũng được coi là dấu phân cách giờ, và cặp `MM-DD` phân biệt
     *   với `DD/MM` theo đúng kiểu máy đo Việt Nam (gạch ngang = tháng trước).
     */
    private fun extractDate(
        text: String,
        fallbackYear: Int,
        todayIso: String?,
        loose: Boolean
    ): MeterDate? {
        if (text.isBlank()) return null
        var best: DateCandidate? = null
        val lines = normalizeDisplayText(text).lines()

        for (index in lines.indices) {
            // Giờ bị xoá trước khi tìm ngày, nếu không "08.30" dễ thành 30/08.
            val stripped = COLON_TIME.replace(normalizeDisplayText(lines[index]), " ")
            val line = if (loose) LOOSE_TIME_STRIP.replace(stripped, " ") else stripped
            if (line.isBlank()) continue
            val labelled = DATE_LABEL.containsMatchIn(line)
            val found = mutableListOf<DateCandidate>()

            for (match in ISO_DATE.findAll(line)) {
                val year = match.groupValues[1].toIntOrNull() ?: continue
                val second = match.groupValues[3].toIntOrNull() ?: continue
                val third = match.groupValues[4].toIntOrNull() ?: continue
                when {
                    isRealDate(year, second, third) ->
                        found += DateCandidate(
                            year, second, third, if (labelled) 0.95f else 0.9f, false, index
                        )
                    // "2026-13-05": tháng và ngày bị OCR đổi chỗ.
                    isRealDate(year, third, second) ->
                        found += DateCandidate(year, third, second, 0.7f, true, index)
                }
            }

            for (match in FULL_DATE.findAll(line)) {
                val year = expandYear(match.groupValues[4]) ?: continue
                val first = match.groupValues[1].toIntOrNull() ?: continue
                val second = match.groupValues[3].toIntOrNull() ?: continue
                // Gach ngang la kieu MM-DD cua may do; gach cheo la DD/MM kieu Viet Nam.
                val monthFirst = match.groupValues[2] == "-"
                for (option in orientationCandidates(first, second, monthFirst)) {
                    if (!isRealDate(year, option.month, option.day)) continue
                    found += DateCandidate(
                        year = year,
                        month = option.month,
                        day = option.day,
                        confidence = (if (option.swapped) 0.7f else if (labelled) 0.9f else 0.85f) +
                            recencyBonus(year, option.month, option.day, todayIso, option.ambiguous),
                        ambiguous = option.ambiguous,
                        lineIndex = index
                    )
                }
            }

            if (found.isEmpty()) {
                for (short in shortDateCandidates(line, labelled, loose)) {
                    for (option in orientationCandidates(short.first, short.second, short.dashSeparated)) {
                        if (!isRealDate(fallbackYear, option.month, option.day)) continue
                        found += DateCandidate(
                            year = fallbackYear,
                            month = option.month,
                            day = option.day,
                            // 0.55 < 0.60 (muc chua co nhan): cach hieu phu khong
                            // bao gio duoc thang cach hieu chinh khi ca hai deu hop le.
                            confidence = (if (option.swapped) 0.55f else short.confidence) +
                                recencyBonus(
                                    fallbackYear, option.month, option.day, todayIso, option.ambiguous
                                ),
                            ambiguous = option.ambiguous,
                            lineIndex = index
                        )
                    }
                }
            }

            for (candidate in found) {
                if (isOutsidePlausibleWindow(candidate.year, candidate.month, candidate.day, todayIso)) {
                    continue
                }
                if (isBetterDate(candidate, best)) best = candidate
            }
        }

        return best?.let { MeterDate(it.year, it.month, it.day, it.confidence, it.ambiguous) }
    }

    /**
     * Các cách hiểu (ngày, tháng): vế > 12 bắt buộc là ngày. Khi cả hai vế đều <= 12
     * thì thứ tự ưu tiên theo dấu phân cách - gạch chéo "/" là DD/MM (Việt Nam), gạch
     * ngang "-" là MM/DD (đa số máy đo đặt sẵn kiểu Mỹ) - và đánh dấu ambiguous để
     * người dùng kiểm lại.
     */
    private fun orientationCandidates(
        first: Int,
        second: Int,
        preferMonthFirst: Boolean = false
    ): List<DayMonth> = when {
        first > 12 && second <= 12 -> listOf(DayMonth(first, second, false, false))
        second > 12 && first <= 12 -> listOf(DayMonth(second, first, false, false))
        preferMonthFirst -> listOf(
            DayMonth(second, first, true, false),
            DayMonth(first, second, true, true)
        )
        else -> listOf(DayMonth(first, second, true, false), DayMonth(second, first, true, true))
    }

    /**
     * Hai cách hiểu đều hợp lý: cái rơi đúng vào [RECENCY_TIE_DAYS] ngày quanh hôm nay
     * được thưởng [RECENCY_BONUS] - đủ lớn để vượt hình phạt đảo chiều 0.05, nên chỉ
     * sự khớp ngày thật sự mới đảo được kết luận. Đồng hồ máy đo thường lệch vài
     * phút chứ không lệch vài tháng, nên đây là cách phân biệt MM-DD và DD/MM mà
     * không cần đoán trường.
     */
    private fun recencyBonus(
        year: Int,
        month: Int,
        day: Int,
        todayIso: String?,
        ambiguous: Boolean
    ): Float {
        if (!ambiguous || todayIso.isNullOrBlank()) return 0f
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(todayIso) ?: return 0f
        val parsed = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day)
        }
        val days = kotlin.math.abs(parsed.timeInMillis - today.time) / MILLIS_PER_DAY
        return if (days <= RECENCY_TIE_DAYS) RECENCY_BONUS else 0f
    }

    /** Cặp ngày/tháng không có năm; bắt buộc một vế đủ 2 chữ số ("6-1" không thành ngày). */
    private fun shortDateCandidates(
        line: String,
        labelled: Boolean,
        loose: Boolean = false
    ): List<ShortPair> {
        val output = mutableListOf<ShortPair>()
        val pattern = if (loose) LOOSE_SHORT_DATE else SHORT_DATE
        for (match in pattern.findAll(line)) {
            val first = match.groupValues[1]
            val second = match.groupValues[3]
            if (first.length < 2 && second.length < 2) continue
            val firstValue = first.toIntOrNull() ?: continue
            val secondValue = second.toIntOrNull() ?: continue
            output += ShortPair(
                first = firstValue,
                second = secondValue,
                confidence = if (labelled) 0.9f else 0.6f,
                dashSeparated = match.groupValues[2] == "-"
            )
        }
        if (output.isEmpty() && (labelled || loose)) {
            for (match in SHORT_DOT_DATE.findAll(line)) {
                val firstValue = match.groupValues[1].toIntOrNull()
                val secondValue = match.groupValues[2].toIntOrNull()
                if (firstValue != null && secondValue != null) {
                    output += ShortPair(firstValue, secondValue, 0.45f)
                }
            }
        }
        return output
    }

    private fun expandYear(token: String): Int? {
        val value = token.toIntOrNull() ?: return null
        return when {
            token.length >= 4 -> value
            value <= 79 -> 2000 + value
            else -> 1900 + value
        }
    }

    private fun isRealDate(year: Int, month: Int, day: Int): Boolean {
        if (year < MIN_SUPPORTED_YEAR || year > MAX_SUPPORTED_YEAR) return false
        if (month < 1 || month > 12) return false
        if (day < 1) return false
        return day <= daysInMonth(year, month)
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(year, month - 1, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun isOutsidePlausibleWindow(year: Int, month: Int, day: Int, todayIso: String?): Boolean {
        if (todayIso.isNullOrBlank()) return false
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(todayIso) ?: return false
        val parsed = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day)
        }
        val max = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, MAX_FUTURE_DAYS)
        }
        val min = Calendar.getInstance().apply {
            time = today
            add(Calendar.YEAR, -MAX_PAST_YEARS)
        }
        return parsed.time.after(max.time) || parsed.time.before(min.time)
    }

    /** Mã lỗi máy đo (E-05, HI, LO…) – chỉ tin khi dòng đó không chứa chỉ số hợp lệ. */
    fun detectMeterError(text: String): String? {
        if (text.isBlank()) return null
        for (rawLine in normalizeDisplayText(text).lines()) {
            val line = normalizeDisplayText(rawLine)
            if (line.isBlank()) continue
            val lower = line.lowercase(Locale.US)
            val hasUnitOrLabel = UNIT_HINTS.any { lower.contains(it) } ||
                LABEL_HINTS.any { lower.contains(it) }
            if (hasUnitOrLabel && lineCandidates(line).isNotEmpty()) continue
            val match = ERROR_CODE.find(line) ?: continue
            val code = BLANK_RUN.replace(match.value, "").uppercase(Locale.US)
            return when {
                code.startsWith("ERR") -> "ERR"
                code.startsWith("E") -> "E-" + code.substring(1)
                else -> code
            }
        }
        return null
    }

    /**
     * Sửa các ký tự ML Kit hay nhầm lẫn trong dãy số nhỏ của dòng trạng thái (
     * `O`/`Q`/`D` -> 0, `l`/`I`/`|` -> 1, `Z` -> 2, `S` -> 5, `G` -> 6, `B` -> 8.
     *
     * Chỉ áp dụng cho chuỗi số đứng cạnh dấu phân cách giờ/ngày, nên nhãn chữ
     * ("Date", "AM", "PM") không bị viết lại. Vì vậy hàm chỉ dùng cho tầng ngày/giờ,
     * tuyệt đối không dùng cho dòng chứa chỉ số: "5.O" sửa thành "5.0" là đổi luôn
     * đường huyết của người bệnh.
     */
    fun repairOcrDigits(text: String): String {
        fun repair(input: String): String =
            input.map { c -> DIGIT_MAP[c] ?: c }.joinToString("")
        return repair(NUMERIC_PAIR.replace(text) { match -> repair(match.value) })
            .let { NUMERIC_PAIR_DOT.replace(it) { match -> repair(match.value) } }
    }

    /** "09-2314:35" -> "09-23 14:35" (ML Kit không thấy khoảng trắng giữa 2 trường). */
    fun splitGluedRow(text: String): String =
        GLUED_TIME_DATE.replace(
            GLUED_DATE_TIME.replace(text, "${'$'}1 ${'$'}2"),
            "${'$'}1 ${'$'}2"
        )

    /**
     * Tầng ngày/giờ đọc từ dải quét toàn màn hình (trên cùng -> dưới cùng).
     *
     * Ảnh được phóng to mạnh nên chữ nhỏ đọc được, đổi lại nó cũng thấy cả nhãn
     * và chữ số nền, vì đây chỉ là nguồn bổ sung: [includeGlucose] mặc định tắt để
     * không bao giờ để dòng mm-dd quyết định chỉ số đường huyết.
     */
    fun parseSmallText(
        rawText: String,
        lines: List<OcrLine> = emptyList(),
        includeGlucose: Boolean = false,
        fallbackYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        todayIso: String? = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ): MeterDisplayFields {
        val repairedLines = lines.map { line ->
            OcrLine(splitGluedRow(repairOcrDigits(line.text)), line.heightPx)
        }
        val text = repairedLines.joinToString("\n") { it.text }
            .ifBlank { splitGluedRow(repairOcrDigits(rawText)) }
        val reading = if (includeGlucose) {
            extractReadingFromLines(repairedLines) ?: extractReading(text)
        } else {
            null
        }
        return MeterDisplayFields(
            glucose = reading,
            time = extractTime(text, loose = true),
            date = extractDate(text, fallbackYear, todayIso, loose = true),
            errorCode = null,
            rawText = rawText,
            lines = lines,
            smallTextScanned = true
        )
    }

    /**
     * Ghép kết quả dải quét chữ nhỏ vào kết quả chính. Kết quả chính luôn thắng khi
     * hai nguồn trùng trường, trừ khi dải nhỏ tin cậy hơn rõ rệt (no upscale 8x nên
     * đọc dòng chữ bé chuẩn hơn).
     */
    fun merge(base: MeterDisplayFields, extra: MeterDisplayFields): MeterDisplayFields = base.copy(
        glucose = base.glucose ?: extra.glucose,
        time = pickStrongerTime(base.time, extra.time),
        date = pickStrongerDate(base.date, extra.date),
        errorCode = base.errorCode ?: extra.errorCode,
        lines = if (base.lines.isEmpty()) extra.lines else base.lines,
        rawText = base.rawText.ifBlank { extra.rawText },
        smallTextScanned = base.smallTextScanned || extra.smallTextScanned
    )

    private fun pickStrongerTime(a: MeterTime?, b: MeterTime?): MeterTime? = when {
        a == null -> b
        b == null -> a
        b.confidence > a.confidence + MERGE_TIE_MARGIN -> b
        else -> a
    }

    private fun pickStrongerDate(a: MeterDate?, b: MeterDate?): MeterDate? = when {
        a == null -> b
        b == null -> a
        b.confidence > a.confidence + MERGE_TIE_MARGIN -> b
        else -> a
    }

    /** Chạy cả ba tầng trên một frame và trả về mọi trường đọc được. */
    fun parse(
        rawText: String,
        lines: List<OcrLine> = emptyList(),
        allowTextFallback: Boolean = true
    ): MeterDisplayFields {
        val spatial = extractReadingFromLines(lines)
        val reading = spatial ?: if (allowTextFallback) extractReading(rawText) else null
        return MeterDisplayFields(
            glucose = reading,
            time = extractTime(rawText),
            date = extractDate(rawText),
            errorCode = if (reading == null) detectMeterError(rawText) else null,
            rawText = rawText,
            lines = lines
        )
    }

    /** Làm sạch chữ số OCR: O/Q -> 0, I/L/| -> 1. */
    private fun normalizeNumericToken(token: String): String = token
        .replace('O', '0', ignoreCase = true)
        .replace('Q', '0', ignoreCase = true)
        .replace('I', '1', ignoreCase = true)
        .replace('L', '1', ignoreCase = true)
        .replace('|', '1')
}
