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

    fun processImage(
        image: InputImage,
        onResult: (ScannedGlucoseResult?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Khi có layout thì chỉ tin vào cây layout: gộp toàn bộ văn bản
                // thành một chuỗi sẽ trộn lẫn số lớn ở giữa màn hình với chữ số
                // nhỏ của nhãn DAY/AVG/ngày/giờ (5.7 có thể thành số khác).
                val fields = toFields(visionText, allowTextFallback = false)
                onResult(resultOf(fields))
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
        val rotated = ImageUtils.rotateBitmap(fullBitmap, rotationDegrees)
        val safeRoi = roi.sanitized()
        val displayRoi = ImageUtils.enhanceForOcr(ImageUtils.cropNormalized(rotated, safeRoi))

        // 1. Pixel reader đọc đúng vùng màn hình người dùng căn khung.
        val pixelResult = pixelReader.processDisplay(displayRoi)

        // 2. ML Kit đọc vùng đã nới nhẹ + tăng tương phản, đủ để thấy đơn vị
        // và nhãn ngày/giờ mà vẫn loại bỏ được nền xung quanh.
        val ocrBitmap = ImageUtils.prepareOcrBitmap(rotated, safeRoi.expand(OCR_ROI_PADDING))
        val inputImage = InputImage.fromBitmap(ocrBitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fields = toFields(visionText, allowTextFallback = true)
                onOcrFields(fields)
                onResult(combineHybrid(pixelResult, fields))
            }
            .addOnFailureListener { error ->
                // ML Kit hỏng thì vẫn còn kết quả đọc điểm ảnh.
                if (pixelResult != null && pixelResult.confidence >= PIXEL_AUTHORITATIVE_CONFIDENCE) {
                    onResult(
                        ScannedGlucoseResult(
                            value = pixelResult.value,
                            source = "PIXEL",
                            confidence = pixelResult.confidence
                        )
                    )
                } else {
                    onError(error)
                }
            }
    }

    /** Chuyển cây văn bản ML Kit thành dữ liệu 3 tầng của [MeterTextParser]. */
    private fun toFields(visionText: Text, allowTextFallback: Boolean): MeterDisplayFields {
        val lines = visionText.textBlocks
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
        // Không có block nào thì buộc phải dùng chuỗi văn bản gộp, kể cả khi
        // người gọi không muốn fallback (đó là dữ liệu duy nhất ML Kit trả về).
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
