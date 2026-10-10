package com.example.nhatkyduonghuyet.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Một khung hình đã được AI đọc.
 *
 * [date] và [time] là ngày/giờ *lấy trên chính màn hình máy đo* (đã chuẩn hoá
 * yyyy-MM-dd / HH:mm) - null khi máy đo không hiển thị, khi đó luồng Auto
 * Import sẽ dùng ngày/giờ hệ thống làm dự phòng.
 */
data class ScannedGlucoseResult(
    val value: Float,
    val date: String? = null,
    val time: String? = null,
    val source: String = "ML_KIT",
    val confidence: Float = 0f,
    val fields: MeterDisplayFields = MeterDisplayFields()
)

/**
 * Quét màn hình máy đo bằng ML Kit Text Recognition + bộ giải mã seven-segment.
 *
 * Kiến trúc 3 lớp:
 * 1. [PixelGlucoseReader]/[SevenSegmentDecoder] đọc trực tiếp từng thanh của
 *    màn hình LCD (chính xác nhất với chữ số bảy thanh).
 * 2. ML Kit đọc toàn bộ văn bản trên màn hình, [MeterTextParser] tách 3 tầng:
 *    chỉ số X.X mmol/L, giờ HH:mm và ngày DD/MM | MM/DD | YYYY-MM-DD.
 * 3. [combineHybrid] hợp nhất hai nguồn theo độ tin cậy.
 */
@Singleton
class GlucoseScanner @Inject constructor() {

    // Khởi tạo ML Kit chỉ khi có frame thật, để parser thuần chạy được trong JVM test.
    private val pixelReader = PixelGlucoseReader()
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Lượt quét bổ sung chạy không quá một lần mỗi khoảng này. Nó đắt gấp đôi một
     * frame thường (thêm một lần ML Kit trên ảnh phóng 8x) và chỉ có ý nghĩa khi
     * màn hình có dòng mm-dd / HH:mm, nên không cần chạy mỗi 250 ms.
     */
    private val lastSweepAt = AtomicLong(0L)

    /**
     * Số lượt quét toàn màn hình liên tiếp không tìm thêm được gì. Máy đo không in
     * giờ/ngày lên màn hình thì dù có quét hết chiều cao cũng vẫn trống, nên sau vài
     * lần như vậy AI ngừng quét lặp - tiết kiệm pin cho tới khi thấy lại chỉ số.
     */
    private val emptySweeps = AtomicInteger(0)

    fun processImage(
        image: InputImage,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onResult(initialOcrResult(visionText.text))
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    /**
     * @param roi region of the rotated frame the user framed in the green guide.
     *   Passing the real on-screen frame keeps the analysed pixels identical no
     *   matter how large the preview surface is (dialog vs. full screen).
     * @param onOcrFields được gọi cho *mọi* frame phân tích, kể cả frame chưa đọc
     *   ra chỉ số - giao diện dùng để hiển thị giờ/ngày/văn bản nhận dạng được.
     */
    fun processHybrid(
        fullBitmap: Bitmap,
        rotationDegrees: Int,
        roi: NormalizedRect = ImageUtils.DISPLAY_ROI,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit,
        onOcrFields: (MeterDisplayFields) -> Unit = {}
    ) {
        var rotated: Bitmap? = null
        var ocrBitmap: Bitmap? = null
        try {
            val rotatedBmp = ImageUtils.rotateBitmap(fullBitmap, rotationDegrees)
            rotated = rotatedBmp

            // 1. Cắt, tăng tương phản và nâng kích thước vùng khung căn chỉnh
            val croppedOcr = ImageUtils.prepareOcrBitmap(rotatedBmp, roi)
            ocrBitmap = croppedOcr

            // 2. Bộ giải mã 7-segment đọc trực tiếp từng thanh LCD
            val pixelResult = try {
                pixelReader.processDisplay(croppedOcr)
            } catch (e: Exception) {
                null
            }

            // 3. ML Kit nhận dạng văn bản trên vùng crop
            recognizer.process(InputImage.fromBitmap(croppedOcr, 0))
                .addOnSuccessListener { visionText ->
                    try {
                        var fields = fieldsFrom(visionText, allowTextFallback = true)

                        // Dự phòng quét toàn bộ khung hình nếu vùng crop chưa đọc ra chỉ số
                        if (fields.glucose == null && pixelResult == null) {
                            recognizer.process(InputImage.fromBitmap(rotatedBmp, 0))
                                .addOnSuccessListener { fullVisionText ->
                                    try {
                                        val fullFields = fieldsFrom(fullVisionText, allowTextFallback = true)
                                        val initialVal = initialExtractGlucose(fullVisionText.text)
                                        val fallbackGlucose = fullFields.glucose ?: initialVal?.let {
                                            GlucoseReading(
                                                value = it,
                                                confidence = INITIAL_OCR_CONFIDENCE,
                                                fromSpatialLine = false,
                                                hasUnit = false,
                                                hasDecimal = true
                                            )
                                        }
                                        fields = fullFields.copy(glucose = fallbackGlucose)

                                        if (shouldRunSweep(fields)) {
                                            runSmallTextSweep(rotatedBmp, roi, fields, pixelResult, onOcrFields) { res ->
                                                cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                                onResult(res)
                                            }
                                        } else {
                                            cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                            finishFrame(fields, pixelResult, onOcrFields, onResult)
                                        }
                                    } catch (e: Exception) {
                                        cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                        finishFrame(fields, pixelResult, onOcrFields, onResult)
                                    }
                                }
                                .addOnFailureListener {
                                    if (shouldRunSweep(fields)) {
                                        runSmallTextSweep(rotatedBmp, roi, fields, pixelResult, onOcrFields) { res ->
                                            cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                            onResult(res)
                                        }
                                    } else {
                                        cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                        finishFrame(fields, pixelResult, onOcrFields, onResult)
                                    }
                                }
                        } else {
                            if (shouldRunSweep(fields)) {
                                runSmallTextSweep(rotatedBmp, roi, fields, pixelResult, onOcrFields) { res ->
                                    cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                    onResult(res)
                                }
                            } else {
                                cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                finishFrame(fields, pixelResult, onOcrFields, onResult)
                            }
                        }
                    } catch (e: Exception) {
                        cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                        finishFrame(MeterDisplayFields(), pixelResult, onOcrFields, onResult)
                    }
                }
                .addOnFailureListener {
                    // Dự phòng quét full frame nếu ML Kit crop báo lỗi
                    try {
                        recognizer.process(InputImage.fromBitmap(rotatedBmp, 0))
                            .addOnSuccessListener { fullVisionText ->
                                try {
                                    val fields = fieldsFrom(fullVisionText, allowTextFallback = true)
                                    cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                    finishFrame(fields, pixelResult, onOcrFields, onResult)
                                } catch (e: Exception) {
                                    cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                    finishFrame(MeterDisplayFields(), pixelResult, onOcrFields, onResult)
                                }
                            }
                            .addOnFailureListener { error ->
                                cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                                onError(error)
                            }
                    } catch (e: Exception) {
                        cleanupBitmaps(rotatedBmp, fullBitmap, croppedOcr)
                        onError(e)
                    }
                }
        } catch (e: Exception) {
            cleanupBitmaps(rotated, fullBitmap, ocrBitmap)
            onError(e)
        }
    }

    private fun cleanupBitmaps(rotated: Bitmap?, fullBitmap: Bitmap, ocrBitmap: Bitmap?) {
        try {
            if (ocrBitmap != null && !ocrBitmap.isRecycled) {
                ocrBitmap.recycle()
            }
            if (rotated != null && rotated != fullBitmap && !rotated.isRecycled) {
                rotated.recycle()
            }
        } catch (_: Exception) {}
    }

    /**
     * OCR camera nguyên bản của commit 583e8da.
     *
     * Không đọc pixel/seven-segment, không sweep, không suy luận ngày/giờ:
     * chỉ lấy token thập phân mà ML Kit trả về từ frame camera.
     */
    private fun initialOcrResult(rawText: String): ScannedGlucoseResult? {
        val value = initialExtractGlucose(rawText) ?: return null
        return ScannedGlucoseResult(
            value = value,
            source = "ML_KIT",
            confidence = INITIAL_OCR_CONFIDENCE
        )
    }

    private fun initialExtractGlucose(text: String): Float? {
        val normalizedText = text.replace(',', '.')
        val match = INITIAL_GLUCOSE_REGEX.find(normalizedText) ?: return null
        val value = match.value.toFloatOrNull() ?: return null
        return value.takeIf { it in INITIAL_MIN_GLUCOSE..INITIAL_MAX_GLUCOSE }
    }

    /**
     * Lượt quét thứ hai, chạy từ trên xuống dưới toàn bộ khung hình.
     *
     * Đây là cách sửa đúng bệnh "chỉ quét phần trên màn hình": dòng trạng thái của
     * máy đo (mm-dd góc trái, HH:mm góc phải) cao chưa tới 1/8 chiều cao số lớn, nên
     * nó nằm ngoài crop căn giữa và hoàn toàn vô hình với OCR. Dải này phủ hết chiều
     * cao, tương phản mạnh và phóng to tới 8x để ML Kit đủ nét chữ mà đọc.
     */
    private fun runSmallTextSweep(
        rotated: Bitmap,
        displayRoi: NormalizedRect,
        base: MeterDisplayFields,
        pixelResult: PixelDisplayReading?,
        onOcrFields: (MeterDisplayFields) -> Unit,
        onResult: (ScannedGlucoseResult?) -> Unit
    ) {
        try {
            val sweepRoi = smallTextSweepRoi(displayRoi)
            val sweepBitmap = ImageUtils.prepareSmallTextBitmap(rotated, sweepRoi)
            recognizer.process(InputImage.fromBitmap(sweepBitmap, 0))
                .addOnSuccessListener { sweepText ->
                    try {
                        val extra = MeterTextParser.parseSmallText(
                            rawText = sweepText.text,
                            lines = toLines(sweepText),
                            includeGlucose = true
                        )
                        val merged = MeterTextParser.merge(base, extra)
                        emptySweeps.set(
                            if (merged.time == null && merged.date == null) {
                                emptySweeps.get() + 1
                            } else {
                                0
                            }
                        )
                        finishFrame(merged, pixelResult, onOcrFields, onResult)
                    } catch (e: Exception) {
                        finishFrame(base, pixelResult, onOcrFields, onResult)
                    }
                }
                .addOnFailureListener {
                    finishFrame(base, pixelResult, onOcrFields, onResult)
                }
        } catch (e: Exception) {
            finishFrame(base, pixelResult, onOcrFields, onResult)
        }
    }

    private fun finishFrame(
        fields: MeterDisplayFields,
        pixelResult: PixelDisplayReading?,
        onOcrFields: (MeterDisplayFields) -> Unit,
        onResult: (ScannedGlucoseResult?) -> Unit
    ) {
        onOcrFields(fields)
        onResult(combineHybrid(pixelResult, fields))
    }

    /** Quet bo sung khi con thieu truong, khong qua 2 lan/giay va bo cuoc sau vai lan cong. */
    private fun shouldRunSweep(fields: MeterDisplayFields): Boolean {
        if (fields.glucose != null && fields.time != null && fields.date != null) return false
        // Mat chi so = nguoi dung dua may ra/vo -> cho phep thu lai tu dau.
        if (fields.glucose == null) emptySweeps.set(0)
        if (emptySweeps.get() >= MAX_EMPTY_SWEEPS) return false
        val now = System.currentTimeMillis()
        val last = lastSweepAt.get()
        if (now - last < SWEEP_MIN_INTERVAL_MS) return false
        return lastSweepAt.compareAndSet(last, now)
    }

    /** Chuyển cây văn bản ML Kit thành danh sách dòng kèm cỡ chữ. */
    private fun toLines(visionText: Text): List<OcrLine> = visionText.textBlocks
        .flatMap { block -> block.lines }
        .map { line ->
            // ML Kit có thể tách một chỉ số thành 3 element: ["5", ".", "7"].
            // Nối lại có dấu cách để bộ chuẩn hoá khôi phục cả dấu phẩy thập
            // phân lẫn chữ số.
            val elementText = line.elements.joinToString(" ") { it.text }
            OcrLine(
                text = elementText.ifBlank { line.text },
                heightPx = line.boundingBox?.height() ?: 0
            )
        }

    /** Chuyển cây văn bản ML Kit thành dữ liệu 3 tầng của [MeterTextParser]. */
    private fun fieldsFrom(visionText: Text, allowTextFallback: Boolean): MeterDisplayFields {
        val lines = toLines(visionText)
        // Khong co block nao thi buoc phai dung chuoi van ban gop, ke ca khi
        // nguoi goi khong muon fallback (do la du lieu duy nhat ML Kit tra ve).
        return MeterTextParser.parse(visionText.text, lines, allowTextFallback || lines.isEmpty())
    }

    private fun resultOf(fields: MeterDisplayFields, source: String = "ML_KIT"): ScannedGlucoseResult? {
        val reading = fields.glucose ?: return null
        return ScannedGlucoseResult(
            value = reading.value,
            date = fields.date?.iso,
            time = fields.time?.formatted,
            source = source,
            confidence = reading.confidence,
            fields = fields
        )
    }

    /**
     * Hợp nhất pixel reader và ML Kit.
     *
     * Ảnh đọc từ điểm ảnh thắng thế khi đủ tin cậy, kể cả khi ML Kit phản đối:
     * mô hình Latin của ML Kit đọc chữ số bảy thanh rất hay nhầm (5.7 thành 5.1),
     * còn quy tắc cũ "bất đồng là trả null" khiến một lỗi đọc sai *lặp lại*
     * chặn vĩnh viễn kết quả đúng.
     */
    private fun combineHybrid(
        pixel: PixelDisplayReading?,
        fields: MeterDisplayFields
    ): ScannedGlucoseResult? {
        val mlKit = fields.glucose

        if (pixel != null && pixel.confidence >= PIXEL_OVERRIDE_CONFIDENCE) {
            return pixelResult(pixel, fields)
        }

        if (pixel != null && pixel.confidence >= PIXEL_AUTHORITATIVE_CONFIDENCE) {
            if (mlKit == null || abs(pixel.value - mlKit.value) <= HYBRID_TOLERANCE) {
                return pixelResult(pixel, fields)
            }
        }

        // Hai nguồn lệch nhau nhiều: thà để người dùng xác nhận tay còn hơn đoán.
        if (pixel != null && mlKit != null && abs(pixel.value - mlKit.value) > HYBRID_TOLERANCE) {
            return null
        }

        return resultOf(fields)
    }

    private fun pixelResult(pixel: PixelDisplayReading, fields: MeterDisplayFields): ScannedGlucoseResult =
        ScannedGlucoseResult(
            value = pixel.value,
            date = fields.date?.iso,
            time = fields.time?.formatted,
            source = "PIXEL",
            confidence = pixel.confidence,
            fields = fields
        )

    /** Visible to JVM tests without exposing parsing internals to production callers. */
    internal fun extractGlucoseForTesting(text: String): Float? = MeterTextParser.extractGlucose(text)

    /** Regression hook cho đường OCR camera nguyên bản từ commit 583e8da. */
    internal fun extractInitialGlucoseForTesting(text: String): Float? = initialExtractGlucose(text)

    /** Visible to JVM tests: hybrid combination decision for a frame. */
    internal fun combineHybridForTesting(
        pixel: PixelDisplayReading?,
        mlKitValue: Float?,
        rawText: String = ""
    ): ScannedGlucoseResult? = combineHybrid(pixel, fieldsWithOverride(MeterTextParser.parse(rawText), mlKitValue))

    private fun fieldsWithOverride(fields: MeterDisplayFields, mlKitValue: Float?): MeterDisplayFields {
        val reading = mlKitValue?.let { GlucoseReading(it, 0.5f, fromSpatialLine = false, hasUnit = false, hasDecimal = true) }
        return fields.copy(glucose = reading)
    }
}

/** Khoảng cách tối thiểu giữa hai lượt quét bổ sung, tính bằng ms. */
private const val SWEEP_MIN_INTERVAL_MS = 600L

/** Số lượt quét toàn màn hình liên tiếp trống ngày/giờ trước khi bỏ cuộc. */
private const val MAX_EMPTY_SWEEPS = 6

/** Quy tắc OCR camera nguyên bản từ commit 583e8da. */
private val INITIAL_GLUCOSE_REGEX = Regex("\\b(\\d{1,2}\\.\\d)\\b")
private const val INITIAL_MIN_GLUCOSE = 2.0f
private const val INITIAL_MAX_GLUCOSE = 30.0f
private const val INITIAL_OCR_CONFIDENCE = 0.5f
