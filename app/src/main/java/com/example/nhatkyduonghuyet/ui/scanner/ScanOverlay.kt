package com.example.nhatkyduonghuyet.ui.scanner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hình học của khung căn chỉnh mà [GlucoseCameraPreview] đã đo được, tính bằng
 * dp và tính cả kích thước vùng xem. Overlay cần số này để vẽ lớp tối, khung
 * và dải quét đúng vị trí bất kể màn hình to hay nhỏ.
 */
data class ScanFrameSpec(
    val viewWidth: Dp,
    val viewHeight: Dp,
    val frameWidth: Dp,
    val frameHeight: Dp
)

/** Trạng thái hiển thị của lớp overlay. */
data class ScanOverlayState(
    val locked: Boolean = false,
    val hits: Int = 0,
    /** Khớp với StableReadingTracker: 4 frame liên tiếp phải trùng chỉ số. */
    val required: Int = 4,
    val liveValue: String? = null,
    val meterTime: String? = null,
    val meterDate: String? = null,
    val hint: String? = null,
    /** Da chay luot quet toan chieu cao man hinh de tim dong mm-dd / hh:mm. */
    val smallTextSweep: Boolean = false
)

private val GUIDE_GREEN = Color(0xFF4CAF50)
private val GUIDE_AMBER = Color(0xFFFFC107)

/** Khung viền đơn giản (đối thoại quét nhanh dùng bản này). */
@Composable
fun BoxScope.ScanGuideFrame(
    spec: ScanFrameSpec,
    locked: Boolean = false
) {
    val accent = if (locked) GUIDE_GREEN else GUIDE_AMBER.copy(alpha = 0.9f)
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .width(spec.frameWidth)
            .height(spec.frameHeight)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            color = Color.Transparent,
            border = BorderStroke(2.dp, if (locked) GUIDE_GREEN else Color.White.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(10.dp)
        ) {}
        CornerBracket(accent = accent, alignment = Alignment.TopStart, fromTop = true, fromLeft = true)
        CornerBracket(accent = accent, alignment = Alignment.TopEnd, fromTop = true, fromLeft = false)
        CornerBracket(accent = accent, alignment = Alignment.BottomStart, fromTop = false, fromLeft = true)
        CornerBracket(accent = accent, alignment = Alignment.BottomEnd, fromTop = false, fromLeft = false)
    }
}

/**
 * Lớp phủ "chuyên nghiệp" cho màn hình quét: làm tối ngoài khung, 4 góc neo,
 * dải quét chạy dọc, chấm tiến trình khoá chỉ số và chip hiển thị nhanh
 * giờ/ngày mà OCR tầng 2 - 3 vừa đọc được.
 */
@Composable
fun BoxScope.ScanAlignmentOverlay(
    spec: ScanFrameSpec,
    state: ScanOverlayState
) {
    // 1. Mặt nạ. Phần trên/dưới khung chỉ bị *tối nhẹ*: AI thật sự đọc cả cột này
    // (lượt quét chữ nhỏ phủ hết chiều cao), nên không được vẽ như vùng bị loại.
    // Hai cột trái/phải mới là vùng bị cắt khỏi phân tích.
    val maskColor = Color.Black.copy(alpha = 0.55f)
    val softMaskColor = Color.Black.copy(alpha = 0.20f)
    val sideHeight = ((spec.viewHeight - spec.frameHeight) / 2f).coerceAtLeast(0.dp)
    val sideWidth = ((spec.viewWidth - spec.frameWidth) / 2f).coerceAtLeast(0.dp)
    Box(modifier = Modifier.fillMaxWidth().height(sideHeight).align(Alignment.TopCenter).background(softMaskColor))
    Box(modifier = Modifier.fillMaxWidth().height(sideHeight).align(Alignment.BottomCenter).background(softMaskColor))
    Box(modifier = Modifier.width(sideWidth).fillMaxHeight().align(Alignment.CenterStart).background(maskColor))
    Box(modifier = Modifier.width(sideWidth).fillMaxHeight().align(Alignment.CenterEnd).background(maskColor))

    // 2. Khung neo + 4 góc.
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .width(spec.frameWidth)
            .height(spec.frameHeight)
    ) {
        val accent = if (state.locked) GUIDE_GREEN else Color.White.copy(alpha = 0.75f)
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            color = Color.Transparent,
            border = BorderStroke(if (state.locked) 3.dp else 2.dp, accent),
            shape = RoundedCornerShape(10.dp)
        ) {}
        CornerBracket(accent = accent, alignment = Alignment.TopStart, fromTop = true, fromLeft = true, length = 30.dp)
        CornerBracket(accent = accent, alignment = Alignment.TopEnd, fromTop = true, fromLeft = false, length = 30.dp)
        CornerBracket(accent = accent, alignment = Alignment.BottomStart, fromTop = false, fromLeft = true, length = 30.dp)
        CornerBracket(accent = accent, alignment = Alignment.BottomEnd, fromTop = false, fromLeft = false, length = 30.dp)

        // 3. Dải quét chạy hết chiều cao khung hình, từ trên xuống dưới, cho tới khi
        //    khoá được chỉ số - đúng vùng AI thực sự phân tích (số lớn ở giữa, dòng
        //    mm-dd / hh:mm ở mép trên, nhãn AVG/MAX ở mép dưới).
        if (!state.locked) {
            val transition = rememberInfiniteTransition(label = "scanline")
            val fraction by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
                label = "scanline"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    // y = 0 la mép trên của khung; bien tren/duoi la dem giua khung hinh
                    // va khung, nen dai quat di het (viewHeight - frameHeight) / 2 ve moi phia.
                    .offset(y = ((spec.viewHeight - spec.frameHeight) / 2f) * (2f * fraction - 1f))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, GUIDE_GREEN.copy(alpha = 0.95f), Color.Transparent)
                        )
                    )
            )
        }
    }

    // 4. Chip thông tin ngay dưới khung: chỉ số live + giờ/ngày đọc được.
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = spec.frameHeight / 2f + 14.dp)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.liveValue?.let { InfoChip(text = "$it mmol/L", emphasis = true) }
            state.meterTime?.let { InfoChip(text = "Giờ $it", emphasis = false) }
            state.meterDate?.let { InfoChip(text = "Ngày $it", emphasis = false) }
            if (state.meterTime == null && state.meterDate == null && state.smallTextSweep) {
                InfoChip(text = "Đã quét cả màn hình", emphasis = false)
            }
        }
        if (!state.locked && state.hits > 0) {
            StabilityDots(hits = state.hits, required = state.required)
        }
        state.hint?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BoxScope.CornerBracket(
    accent: Color,
    alignment: Alignment,
    fromTop: Boolean,
    fromLeft: Boolean,
    length: Dp = 22.dp,
    thickness: Dp = 3.dp
) {
    Box(modifier = Modifier.align(alignment).size(length)) {
        Box(
            modifier = Modifier
                .align(if (fromTop) Alignment.TopStart else Alignment.BottomStart)
                .fillMaxWidth()
                .height(thickness)
                .background(accent)
        )
        Box(
            modifier = Modifier
                .align(if (fromLeft) Alignment.TopStart else Alignment.TopEnd)
                .fillMaxHeight()
                .width(thickness)
                .background(accent)
        )
    }
}

@Composable
private fun InfoChip(text: String, emphasis: Boolean) {
    Surface(
        color = if (emphasis) GUIDE_GREEN.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.62f),
        contentColor = if (emphasis) Color(0xFF06240D) else Color.White,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/** Bao nhiêu khung hình liên tiếp đã đọc trùng một giá trị. */
@Composable
private fun StabilityDots(hits: Int, required: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val total = required.coerceAtLeast(1)
        for (index in 0 until total) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = if (index < hits) GUIDE_GREEN else Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}
