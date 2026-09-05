# Review tính năng Camera OCR — đọc chỉ số từ máy đo đường huyết

Ngày review: 2026-09-05 · Nhánh: `arena/01a071f4-nhatkyduonghuyet` · Base commit: `689dc99`

Phạm vi review:

| File | Vai trò |
|---|---|
| `ml/GlucoseScanner.kt` | Điều phối hybrid (pixel + ML Kit), parse text → giá trị |
| `ml/PixelGlucoseReader.kt` | Bộ đọc LED/LCD 7 đoạn theo pixel |
| `ml/ImageUtils.kt` | Crop / xoay / tiền xử lý ảnh cho OCR |
| `ml/ScannerGeometry.kt` | Ánh xạ khung xanh trên màn hình → ROI trên ảnh camera |
| `ml/GlucoseConstants.kt` | Các ngưỡng tin cậy |
| `ui/camera/GlucoseCameraScanner.kt` | CameraX, khung ngắm, vote đa khung |
| `ui/scanner/ScannerScreen.kt`, `ui/detail/CameraScannerDialog.kt` | 2 điểm gọi |
| `ui/dashboard/DashboardViewModel.kt` | Ghi kết quả quét vào DB |

---

## 0. TL;DR — vì sao đang đọc sai

Nguyên nhân **không phải** ở chỗ "ML Kit yếu". Nguyên nhân chính là **nhánh `PixelGlucoseReader`
đang tạo ra các giá trị SAI nhưng lại tự tin (confidence 1.0), và được quyền ghi đè ML Kit**.
Cộng thêm 4 vấn đề cấu trúc nữa:

| # | Nguyên nhân | Mức | Bằng chứng |
|---|---|---|---|
| 1 | Ngưỡng nhị phân hoá tính **cục bộ trong từng ô segment** → một đoạn ĐANG SÁNG có độ xám 45–255 luôn bị coi là TẮT | 🔴 P0 | `PixelGlucoseReader.kt:148`, mục 2.3 |
| 2 | Lưới **3 ô chữ số cố định** (0.05–0.32 / 0.35–0.62 / 0.68–0.95) — thực tế số hiển thị 2 hoặc 3 chữ số, to nhỏ, lệch tuỳ cách người dùng đưa máy vào khung | 🔴 P0 | `PixelGlucoseReader.kt:56`, mục 2.1 |
| 3 | `confidence` chỉ đo **độ khớp với mẫu gần nhất**, không đo độ đúng → rác vẫn cho 1.0 và được `PIXEL_AUTHORITATIVE_CONFIDENCE` cho ghi đè ML Kit | 🔴 P0 | `PixelGlucoseReader.kt:163`, `GlucoseConstants.kt:22`, `GlucoseScanner.kt:141` |
| 4 | Chuẩn hoá ký tự vô điều kiện: máy báo **`Lo` (hạ đường huyết) → đọc thành 10.0 mmol/L**; `E-3` → 3.0 | 🔴 P0 | `GlucoseScanner.kt:332`, mục 2.4 |
| 5 | Kết quả quét được **tự động ghi thẳng vào DB**, không có bước người dùng xác nhận | 🔴 P0 | `DashboardViewModel.kt:134-210` |
| 6 | Ảnh đưa vào ML Kit **chưa nhị phân hoá / chưa nối khe giữa các đoạn** → model Latin đọc font 7 đoạn rất kém | 🟠 P1 | `ImageUtils.kt:55` |
| 7 | Vote đa khung không chặn được lỗi **hệ thống** (cảnh tĩnh → 4 khung cùng sai giống nhau) | 🟠 P1 | `GlucoseCameraScanner.kt:438` |

> **Kết luận nhanh:** nếu chỉ được làm 1 việc trong hôm nay → **tắt nhánh pixel** (đặt
> `PIXEL_AUTHORITATIVE_CONFIDENCE = 2f` để nó không bao giờ tự quyết) và **bắt buộc người dùng
> xác nhận** trước khi lưu. Riêng 2 thay đổi đó đã loại bỏ phần lớn ca "đọc sai mà vẫn lưu".

---

## 1. Pipeline hiện tại

```
ImageProxy (1920x1080 mong muốn)
  └─ toBitmap()                                  GlucoseCameraScanner.kt:242
     └─ rotateBitmap(full frame)                 GlucoseScanner.kt:68     ← xoay CẢ khung rồi mới crop
        ├─ crop ROI khung xanh ─────────────► PixelGlucoseReader (7 đoạn)
        └─ crop ROI + 35% chiều cao
           └─ enhanceForOcr (grayscale +
              contrast stretch + upscale) ────► ML Kit Latin OCR ─► extractGlucoseCandidate()
                                                                   └─ extractGlucose(text)
              combineHybrid(pixel, mlKit)        GlucoseScanner.kt:134
                 └─ findStableValue(4 khung)     GlucoseCameraScanner.kt:438
                    └─ onResult → DashboardViewModel.onGlucoseScanned → repo.insertLog()  ← LƯU LUÔN
```

Hai điểm kiến trúc đáng lưu ý ngay từ sơ đồ:

* **Cùng một ROI** được đưa cho hai bộ đọc có yêu cầu hoàn toàn khác nhau. `PixelGlucoseReader`
  cần ROI **bó sát chữ số** (comment ở `:53` cũng ghi vậy: *"expects a digit-centered ROI"*),
  nhưng ROI thực tế là khung xanh tỉ lệ 1.5 mà người dùng đưa **cả màn hình LCD** vào — bên trong
  còn có icon giọt máu, `mmol/L`, ngày giờ, ký hiệu bữa ăn. Giả định của bộ đọc pixel **không bao
  giờ đúng** trong sử dụng thật.
* Không có bước nào **định vị chữ số** (digit localization). Toàn bộ nhánh pixel dựa vào việc
  người dùng canh khung hoàn hảo — điều không thể yêu cầu ở người bệnh lớn tuổi, cầm tay run.

---

## 2. Bằng chứng đo đạc

Tôi đã dựng lại **nguyên văn** thuật toán `PixelGlucoseReader` và `extractGlucose` bằng Python
(`tools/ocr-review/`) rồi cho chạy trên ảnh 7 đoạn tổng hợp và trên các chuỗi OCR thực tế.
Cách chạy ở mục 8.

> Lưu ý trung thực: ảnh tổng hợp không thay thế được ảnh chụp thật, nên **con số % dưới đây là để
> chỉ ra lỗi cấu trúc của thuật toán**, không phải số đo hiện trường. Riêng mục 2.3 là chứng minh
> thuần số học, đúng với mọi ảnh.

### 2.1 Bộ đọc pixel trả về giá trị sai nhiều hơn là đúng

Quét các giá trị 3.0 → 19.9 mmol/L:

| Điều kiện | Đúng | **SAI mà vẫn trả về** | Không đọc được | Bị loại (ngoài dải) |
|---|---|---|---|---|
| Tương phản lý tưởng, chữ số lấp đầy khung | 5% | **63%** | 17% | 14% |
| Tương phản lý tưởng, chữ số chiếm 70% khung | 0% | 0% | 100% | 0% |
| Tương phản ảnh chụp thật (chữ 110 / nền 205) | 3% | **54%** | 42% | 0% |
| Màn hình đảo màu (chữ sáng / nền tối) | 0% | **71%** | 0% | 28% |

Ví dụ cụ thể từ log:

```
display=5.7   -> ('5', 5.0, conf 0.80)     # mất hẳn phần thập phân
display=9.4   -> ('5', 5.0, conf 0.80)     # 9.4 thành 5.0  (!)
display=12.3  -> ('12', 12.0, conf 1.00)
display=5.7 (đảo màu) -> ('9.8', 9.8, conf 1.00)   # sai hoàn toàn, confidence tuyệt đối
display=5.7 (khối 2 số lệch trái) -> '57' -> bị loại vì ngoài dải
```

Đây chính là mô tả của người dùng: *"thường đọc sai chỉ số"* — sai kiểu **rơi phần thập phân**
(`5.7 → 5.0`) hoặc **sai hẳn con số** (`9.4 → 5.0`, `5.7 → 9.8`), chứ không phải "không đọc được".

### 2.2 Vì sao lưới 3 ô cố định luôn hỏng

`PixelGlucoseReader.kt:56` giả định đúng 3 ô chữ số ở toạ độ chuẩn hoá cố định, và dấu chấm thập
phân **luôn** nằm trong dải x ∈ [0.62, 0.68] (`:74`). Trong thực tế:

* Giá trị 2 chữ số (`5.7`, `6.1` — chiếm đa số) trải đều trên khung → chữ số 1 rơi vào ô 0, chữ số
  2 rơi vào **giữa ô 1 và ô 2** → hoặc mất chữ số, hoặc đọc nhầm.
* Giá trị 3 chữ số (`12.3`) thì dấu chấm nằm ở x ≈ 0.62 chứ không phải 0.66 → mất dấu chấm.
* `combineDigits` (`:169`) chèn dấu chấm **trước chữ số cuối của danh sách đọc được**, không phải
  theo vị trí thật. Nếu 1 ô bị bỏ (không đọc được) thì dấu chấm nhảy chỗ: `12.3` có thể thành `1.2`.

### 2.3 Lỗi ngưỡng nhị phân hoá — chứng minh số học

```kotlin
// PixelGlucoseReader.kt:148
val threshold = (totalLuminance / sampleCount * 0.85).coerceIn(40.0, 180.0)
```

Ngưỡng được tính **từ chính vùng đang xét**. Với một ô segment **đang sáng hoàn toàn** (đồng nhất,
độ xám L), ngưỡng = 0.85·L < L ⇒ **không pixel nào dưới ngưỡng** ⇒ tỉ lệ tối = 0 ⇒ kết luận "TẮT".
Nó chỉ tình cờ đúng khi L < 40/0.85 ≈ 47 nhờ mệnh đề `coerceIn(40.0, …)`:

```
độ xám của đoạn đang sáng =  10 -> darkPixelRatio=1.00 -> ON
                             30 -> 1.00 -> ON
                             45 -> 0.00 -> OFF   ← bắt đầu sai
                             90 -> 0.00 -> OFF
                            120 -> 0.00 -> OFF
                            200 -> 0.00 -> OFF
```

Ảnh chụp màn LCD bằng điện thoại gần như luôn cho nét chữ ở mức xám 60–140 (chưa kể ánh sáng
phòng, phản quang). ⇒ **Ở điều kiện thật, hầu hết các đoạn đang sáng bị đọc là TẮT**, và chỉ những
ô có bóng/viền/loá (không đồng nhất) mới vượt ngưỡng → tập segment "bật" thu được là **nhiễu**, và
`readDigit` vẫn ép nó về chữ số gần nhất.

Ngưỡng phải được tính **một lần cho toàn ROI (Otsu)** hoặc theo từng chữ số, không bao giờ theo
từng ô segment.

### 2.4 Bộ parse text: các ca nguy hiểm

Chạy `tools/ocr-review/text_parser_sim.py`:

| Input OCR | Mong đợi | Thực tế | Ghi chú |
|---|---|---|---|
| `Lo` | không nhận | **10.0** | 🔴 máy báo **hạ đường huyết nặng** → ghi thành 10.0 mmol/L |
| `E-3` | không nhận | **3.0** | 🔴 mã lỗi → ghi thành hạ đường huyết |
| `10 24` (giờ 10:24 mất dấu `:`) | không nhận | **10.0** | 🟠 giờ thành chỉ số |
| `AVG 7.2` | không nhận | **7.2** | 🟠 màn hình trung bình 14 ngày bị ghi như 1 lần đo |
| `6.1 12:45` | 6.1 | **null** | 🟠 mất kết quả đúng: cả dòng bị vứt vì có giờ |
| `20-08 6.2` | 6.2 | **null** | 🟠 như trên |
| `126` (máy mg/dL, chữ `mg/dL` ngoài khung) | 7.0 | null | 🟡 xem P1-10 |
| `HbA1c 6.5 %` | không nhận | 6.5 | 🟡 nhầm HbA1c thành glucose |

Ca `Lo → 10.0` là nghiêm trọng nhất về mặt an toàn: nó **đảo ngược** ý nghĩa lâm sàng (hạ đường
huyết → đường huyết bình thường-cao) và hiện nay được **tự động lưu vào nhật ký**.

Nguyên nhân: `normalizeNumericToken` (`GlucoseScanner.kt:332`) đổi `L→1`, `o→0` cho **mọi** token,
kể cả token không chứa chữ số nào; regex `[0-9OoQqIiLl|]{1,3}` cho phép token toàn chữ cái.

---

## 3. Danh sách lỗi P0 (phải sửa)

### P0-1 · Ngưỡng segment tính cục bộ — `PixelGlucoseReader.kt:132-161`
Xem 2.3. **Sửa:** tính ngưỡng Otsu một lần trên toàn ROI (hoặc trên hộp bao của khối chữ số), lưu
lại mặt nạ nhị phân, rồi mỗi segment chỉ đếm tỉ lệ pixel "mực" trong mặt nạ đó.

### P0-2 · Lưới chữ số cố định — `PixelGlucoseReader.kt:56-79`
Xem 2.2. **Sửa:** thay bằng định vị chữ số thật (mục 6, Giai đoạn 2): nhị phân hoá → đóng hình thái
(morphological close) để nối các khe giữa đoạn → connected components → lọc theo chiều cao/tỉ lệ →
sắp theo x → mỗi component là một chữ số; dấu chấm là component nhỏ nằm ở đường chân chữ.

### P0-3 · `confidence` không đo độ đúng — `PixelGlucoseReader.kt:163-167` + `GlucoseConstants.kt:22`
`calculateConfidence` = Jaccard(mẫu, tập bật). Nếu ô nằm trên vùng tối đồng nhất → tất cả 7 đoạn
"bật" → khớp hoàn hảo với số **8** → confidence 1.0. Nếu chỉ có 2 đoạn nhiễu B,C → khớp hoàn hảo
với số **1** → confidence 1.0. Rồi `GlucoseScanner.kt:141` cho phép giá trị đó **ghi đè ML Kit**,
và `:103` cho nó tự quyết khi ML Kit lỗi.

**Sửa (ngay):**
```kotlin
// GlucoseConstants.kt — vô hiệu hoá quyền tự quyết của nhánh pixel
const val PIXEL_AUTHORITATIVE_CONFIDENCE = 2f   // > 1.0 ⇒ không bao giờ đạt
```
**Sửa (đúng bản chất):** confidence = **khoảng cách tới mẫu tốt thứ hai** (margin), kết hợp
"độ chắc" của từng đoạn (|tỉ lệ − ngưỡng|). Nếu mẫu tốt nhất và nhì cách nhau < 1 đoạn → từ chối.

### P0-4 · `Lo` → 10.0, `E-3` → 3.0 — `GlucoseScanner.kt:255-300, 332`
**Sửa:**
```kotlin
private val statusTokens = Regex("""\b(hi+|lo+|err?|e-?\d|-{2,})\b""", RegexOption.IGNORE_CASE)

private fun normalizeNumericToken(token: String): String {
    // Chỉ sửa nhầm ký tự khi token đã chứa ít nhất một chữ số thật.
    if (token.none { it.isDigit() }) return token
    return token.replace('O','0', true).replace('Q','0', true)
                .replace('I','1', true).replace('L','1', true).replace('|','1')
}
```
Và trong `processHybrid`/`extractGlucose`: nếu `statusTokens` khớp trên dòng lớn nhất → trả về
`ScannedGlucoseResult` dạng **trạng thái** (`HI`/`LO`/`ERROR`) để UI hiện cảnh báo
("Máy báo LO — đường huyết quá thấp, hãy nhập tay") thay vì một con số.

### P0-5 · Tự động lưu, không xác nhận — `DashboardViewModel.kt:134-210`
`onGlucoseScanned` ghi thẳng `repo.insertLog(...)` với `note = "Auto-scanned via AI Camera"`, thậm
chí **ghi đè** bản ghi có sẵn theo heuristic `hour % 2 == 0` (`:190`) để chọn `bgBefore`/`bgAfter`.
Một lần OCR sai ⇒ dữ liệu sức khoẻ sai, người dùng không hề biết.

**Sửa:** `ScannerScreen` phải hiện bottom sheet **"Xác nhận chỉ số"** gồm: giá trị (ô nhập **sửa
được**), ảnh crop vừa quét, nguồn (PIXEL/ML_KIT), nút *Lưu* / *Quét lại*. Chỉ `insertLog` sau khi
người dùng bấm Lưu. Bỏ heuristic `hour % 2`, hỏi rõ "trước ăn / sau ăn".

---

## 4. Danh sách lỗi P1 (ảnh hưởng lớn tới độ chính xác)

### P1-1 · Tiền xử lý chưa phù hợp cho font 7 đoạn — `ImageUtils.kt:55-120`
`enhanceForOcr` mới dừng ở grayscale + kéo giãn tương phản + upscale. Model Latin của ML Kit **không
được huấn luyện cho chữ số 7 đoạn**; điểm chết người là **các khe hở giữa 7 đoạn** khiến model thấy
nhiều nét rời rạc thay vì một glyph.

**Sửa — thứ tự đã được kiểm chứng trong các dự án SSOCR:**
1. Grayscale → **CLAHE / cân bằng cục bộ** (chống loá một góc).
2. **Nhị phân hoá Otsu** (hoặc Sauvola cho ảnh có gradient sáng).
3. **Tự nhận cực tính**: nếu tỉ lệ pixel tối > 50% ⇒ màn đảo màu ⇒ đảo ảnh, luôn đưa về **chữ đen
   nền trắng**.
4. **Morphological closing** kernel ~ (h/12 × h/12) để **nối khe giữa các đoạn** → chữ số trở thành
   nét liền.
5. Khử nghiêng nhẹ (deskew theo trục chính của các component).
6. Upscale ×3–4 với nội suy song tuyến + viền trắng 10% quanh ảnh (ML Kit đọc tốt hơn khi có lề).
7. Chạy OCR **2 biến thể** (bản nhị phân + bản grayscale gốc) và chỉ nhận khi 2 bên khớp.

### P1-2 · Vứt cả dòng khi dòng có ngày/giờ — `GlucoseScanner.kt:209` và `:259`
Rất nhiều máy in `08:32   5.7 mmol/L` **trên cùng một dòng OCR**. Hiện tại cả dòng bị bỏ ⇒ mất kết
quả đúng ⇒ hệ thống rơi về giá trị của nhánh pixel (sai). **Sửa:** xoá **đúng phần khớp** ngày/giờ
khỏi dòng rồi mới parse phần còn lại:
```kotlin
val cleaned = dateOrTimeRegex.replace(line, " ")
```

### P1-3 · Giờ mất dấu `:` thành chỉ số — mục 2.4
`10 24` → 10.0. **Sửa:** nếu trên dòng có ≥ 2 cụm số nguyên 1–2 chữ số cách nhau bởi khoảng trắng và
không có đơn vị ⇒ coi là ngày/giờ, bỏ qua. Bổ sung: chỉ chấp nhận **số nguyên không đơn vị** khi nó
là dòng cao nhất (chữ to nhất) trong ảnh.

### P1-4 · `AVG`/`MEM`/`DAY` chỉ bị phạt ở nhánh spatial — `GlucoseScanner.kt:215-218`
Nhánh text (`extractGlucose(text)`, dùng khi ML Kit không trả về layout) không có bộ lọc này.
**Sửa:** đưa danh sách từ khoá phạt vào một hàm dùng chung cho cả 2 nhánh, và **từ chối hẳn** khi
thấy `avg`/`average`/`ngày`/`14d`/`30d` (đó là màn hình thống kê, không phải một lần đo).

### P1-5 · Không xử lý màn hình đảo màu — `PixelGlucoseReader.kt`
Toàn bộ bộ đọc giả định "chữ tối trên nền sáng" (`darkPixelRatio`). Máy có nền đen/OLED sai 100%
(bảng 2.1: 71% trả về sai). **Sửa:** tự nhận cực tính như P1-1 bước 3.

### P1-6 · Vote đa khung không chặn lỗi hệ thống — `GlucoseCameraScanner.kt:427-446`
Cảnh tĩnh ⇒ 4 khung liên tiếp cho **cùng một kết quả sai** ⇒ vote xác nhận cái sai. Ngoài ra
`findStableValue` với `STABILITY_TOLERANCE = 0.15f` coi 5.7 và 5.8 là "khớp" rồi chọn mode → có thể
trả về giá trị **chưa từng được xác nhận đủ 3 lần**.

**Sửa:**
```kotlin
private const val STABILITY_WINDOW_SIZE = 6
private const val STABILITY_REQUIRED_MATCHES = 3   // khớp CHÍNH XÁC, không tolerance

private fun findStableValue(values: ArrayDeque<Float>): Float? {
    if (values.size < STABILITY_REQUIRED_MATCHES) return null
    val counts = values.groupingBy { it }.eachCount()
    val (best, n) = counts.maxByOrNull { it.value } ?: return null
    if (n < STABILITY_REQUIRED_MATCHES) return null
    // Có bất kỳ giá trị hợp lệ nào KHÁC trong cửa sổ ⇒ chưa ổn định, tiếp tục quét.
    if (counts.size > 1 && counts.filterKeys { it != best }.values.sum() > 1) return null
    return best
}
```
Và **xoá cửa sổ** khi có 2 khung liên tiếp không đọc được (máy đo đã bị đưa ra khỏi khung).

### P1-7 · Ánh xạ ROI giả định Preview và ImageAnalysis cùng FOV — `GlucoseCameraScanner.kt:210-224`, `ScannerGeometry.kt:78`
`Preview` đặt 1280×720, `ImageAnalysis` đặt 1920×1080; trên máy yếu CameraX có thể rơi về **640×480
(4:3)**. Khi đó preview (16:9) và ảnh phân tích (4:3) **khác FOV**, nên khung xanh người dùng thấy
**không phải** vùng được cắt ra phân tích → OCR đọc nhầm vùng (ví dụ trúng dòng ngày/giờ).

**Sửa — cách chuẩn của CameraX:**
```kotlin
val viewPort = previewView.viewPort!!            // hoặc ViewPort.Builder(Rational(w,h), rot).build()
val group = UseCaseGroup.Builder()
    .setViewPort(viewPort)
    .addUseCase(preview)
    .addUseCase(imageAnalysis)
    .build()
provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, group)
```
rồi trong analyzer dùng `imageProxy.cropRect` làm gốc toạ độ thay vì tự suy ra FILL_CENTER. Đồng
thời dùng `ResolutionSelector` với `AspectRatioStrategy.RATIO_16_9` cho **cả hai** use case.

### P1-8 · Lấy nét / phơi sáng — `GlucoseCameraScanner.kt:325-331`
Chỉ có 1 lần AF khi bind (`setAutoCancelDuration(3s)`) và tap-to-focus. Sau 3 giây không còn AF
liên tục ⇒ ảnh mờ ⇒ OCR sai. Màn LCD lại sáng hơn nền ⇒ AE trung bình làm chữ bị "cháy".

**Sửa:** bật AF liên tục + bù phơi sáng âm nhẹ khi đo vùng khung:
```kotlin
Camera2Interop.Extender(previewBuilder)
    .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
camera.cameraControl.setExposureCompensationIndex(-1)   // chống cháy chữ trên LCD nền sáng
```
Thêm nút **bật/tắt đèn flash** (hiện `enableTorch` chỉ dùng để nháy cảnh báo nguy hiểm ở
`ScannerScreen.kt:62-76`, không dùng để soi sáng khi quét).

### P1-9 · Không có cổng chất lượng khung
Khung mờ/loá vẫn được đưa vào OCR. **Sửa:** trước khi OCR, tính **phương sai Laplacian** trên ROI
(nhòe ⇒ phương sai thấp) và tỉ lệ pixel bão hoà (>250) để phát hiện loá; nếu không đạt → bỏ khung
và hiện gợi ý cho người dùng ("Ảnh bị mờ, giữ yên máy" / "Bị loá, nghiêng máy đo"). Chi phí ~1ms
trên ROI đã thu nhỏ, đổi lại loại bỏ phần lớn khung rác.

### P1-10 · Đơn vị mg/dL — `GlucoseScanner.kt:270`
`rawValue > 20f && !hasMmolUnit -> bỏ` ⇒ máy mg/dL (Mỹ/Nhật, hiển thị 70–300) **không bao giờ đọc
được** nếu chữ `mg/dL` nằm ngoài khung. Ngược lại chỉ cần thấy chuỗi `"mg"` là chia 18 — dễ dính
false positive. **Sửa:** thêm **Cài đặt → Đơn vị máy đo (mmol/L | mg/dL)** do người dùng chọn một
lần; OCR dùng đơn vị đó làm mặc định, chữ trên màn hình chỉ để xác nhận. Đây cũng là cách loại bỏ
nhập nhằng `5.7` vs `57`.

### P1-11 · Hiệu năng — `GlucoseScanner.kt:68`
`rotateBitmap(fullBitmap)` xoay **cả khung 1920×1080** (cấp phát ~8MB/khung, 4 khung/giây) rồi mới
crop ROI chỉ chiếm ~15% diện tích. **Sửa:** crop trước → xoay sau (hoặc gộp crop+rotate vào một
`Matrix` duy nhất trong `Bitmap.createBitmap`). Giảm ~85% chi phí, cho phép tăng tần suất phân tích
⇒ vote nhanh hơn.

---

## 5. P2 — chất lượng mã & khả năng kiểm thử (lý do bug lọt lưới)

| Vấn đề | Vị trí | Ghi chú |
|---|---|---|
| `PixelGlucoseReader` phụ thuộc `android.graphics.Bitmap`/`RectF` ⇒ **không thể unit-test trên JVM** | toàn file | Đây là lý do gốc khiến lỗi 2.3 tồn tại. Tách thành lớp thuần `IntArray` + rect riêng, giống cách `ScannerGeometry` đã làm rất tốt. |
| Không có **test cấp ảnh** nào | `app/src/test/...` | 12 test hiện có đều là test chuỗi; không test nào chạm vào `PixelGlucoseReader`. |
| `extractGlucose(visionText: Text)` là hàm chết | `GlucoseScanner.kt:178` | Xoá. |
| `HYBRID_TOLERANCE` chú thích "relative deviation" nhưng dùng như **tuyệt đối** | `GlucoseConstants.kt:25`, `GlucoseScanner.kt:142` | Sai lệch tài liệu ↔ hành vi. |
| `ui/camera/CameraScreen.kt` là placeholder chết (khung 250dp, không camera) | cả file | Gây nhầm lẫn khi đọc code; nên xoá. |
| Kết quả hiển thị `"${result.value} mmol/L"` ⇒ in ra `5.699999` với một số giá trị float | `ScannerScreen.kt:192` | Format `%.1f`. |
| `processImage()` (nhánh ML Kit thuần) không còn ai gọi | `GlucoseScanner.kt:40` | Xoá hoặc dùng cho luồng "chọn ảnh từ thư viện". |

---

## 6. Lộ trình sửa đề xuất

### Giai đoạn 1 — "cầm máu" (0.5 ngày, rủi ro thấp)
1. `PIXEL_AUTHORITATIVE_CONFIDENCE = 2f` → ML Kit là nguồn duy nhất được quyết. *(P0-3)*
2. Chặn token trạng thái `HI/LO/Er/E-x`; chỉ sửa nhầm ký tự khi token có chữ số. *(P0-4)*
3. Màn hình **xác nhận trước khi lưu** + cho sửa tay. *(P0-5)*
4. Siết vote: khớp chính xác, cửa sổ 6, có mâu thuẫn thì không trả kết quả. *(P1-6)*
5. Bỏ ngày/giờ **trong dòng** thay vì bỏ cả dòng. *(P1-2, P1-3)*

→ Kỳ vọng: tỉ lệ "ghi nhầm số vào nhật ký" giảm về gần 0; số ca "đọc được" giảm nhẹ (đánh đổi có
chủ ý — thà không đọc được còn hơn đọc sai chỉ số y tế).

### Giai đoạn 2 — làm lại bộ đọc 7 đoạn cho đúng (2–3 ngày)
Tạo `SevenSegmentReader` **thuần Kotlin** (không phụ thuộc Android) nhận `IntArray` luminance:

```kotlin
class SevenSegmentReader(private val cfg: Config = Config()) {

    fun read(gray: IntArray, w: Int, h: Int): SegmentedReading? {
        val thr = otsu(gray)                        // ngưỡng TOÀN CỤC, không cục bộ  (P0-1)
        var mask = binarize(gray, w, h, thr)
        if (inkRatio(mask) > 0.5f) mask = invert(mask)          // tự nhận cực tính  (P1-5)
        mask = closeMorph(mask, w, h, radius = h / 24)          // nối khe 7 đoạn    (P1-1)

        val comps = connectedComponents(mask, w, h)
            .filter { it.h > h * 0.35f && it.w < w * 0.5f }     // bỏ icon, đơn vị, ngày giờ
            .sortedBy { it.left }
        if (comps.isEmpty() || comps.size > 4) return null      // bố cục vô lý ⇒ từ chối

        val digits = comps.map { c -> classify(mask, w, c) }    // sample 7 vùng THEO hộp bao thật
        if (digits.any { it == null }) return null

        val dot = findDecimalDot(mask, w, h, comps)             // component nhỏ ở chân chữ
        val text = compose(digits, dot)
        val margin = digits.minOf { it!!.margin }               // confidence = margin  (P0-3)
        return SegmentedReading(text, text.toFloat(), margin)
    }
}
```

Điểm mấu chốt so với bản hiện tại:

| Hiện tại | Bản mới |
|---|---|
| ngưỡng theo từng ô segment | Otsu toàn ROI |
| 3 ô cố định | connected components → hộp bao thật của từng chữ số |
| chấm thập phân ở x cố định | component nhỏ ở đường chân chữ, biết nằm sau chữ số nào |
| confidence = Jaccard (≈ luôn cao) | margin giữa mẫu nhất và nhì; loại khi margin thấp |
| chỉ chữ tối/nền sáng | tự nhận cực tính |
| không kiểm tra bố cục | từ chối khi số component vô lý (>4, chiều cao lệch nhau) |
| không test được | thuần JVM ⇒ unit test bằng ảnh mẫu trong `src/test/resources` |

Song song: nâng `enhanceForOcr` theo P1-1 để **ML Kit cũng khá lên**, và chỉ trả kết quả khi
**hai nhánh đồng thuận** (hoặc một nhánh có margin rất cao). Đồng thuận giữa hai thuật toán **độc
lập** mới thực sự chống được lỗi hệ thống — điều mà vote đa khung không làm được.

### Giai đoạn 3 — bền vững (1–2 tuần, tuỳ dữ liệu)
* **Bộ phân loại chữ số 7 đoạn bằng TFLite** (~50KB, CNN nhỏ trên ảnh 28×28 nhị phân). Dự án đã có
  sẵn TFLite runtime (`app/build.gradle`), chi phí tích hợp gần bằng 0. Huấn luyện từ ảnh cắt của
  Giai đoạn 2 + tăng cường dữ liệu (nghiêng, mờ, loá, đảo màu). Đây là cách duy nhất đạt >99%.
* **Bộ ảnh vàng (golden set)** ≥ 100 ảnh chụp thật màn hình máy đo (nhiều model, nhiều điều kiện
  sáng), kèm nhãn, đặt trong `app/src/androidTest/assets/meters/`; test hồi quy chạy trong CI với
  ngưỡng: `accuracy ≥ 95%` **và** `wrong-delivery ≤ 0.5%`.
* **Chế độ gỡ lỗi tuỳ chọn**: khi người dùng bấm "Kết quả sai", lưu ảnh crop + text OCR thô + giá trị
  đã chọn vào bộ nhớ máy (có xin phép, không tự gửi đi) để bổ sung golden set.

---

## 7. Tiêu chí nghiệm thu đề xuất

| Chỉ số | Hiện tại (ước lượng từ mô phỏng) | Mục tiêu GĐ1 | Mục tiêu GĐ2 | Mục tiêu GĐ3 |
|---|---|---|---|---|
| **Wrong-delivery rate** (trả về số SAI) — chỉ số quan trọng nhất | rất cao (xem 2.1) | < 2% | < 1% | < 0.5% |
| Accuracy (đọc đúng trên golden set) | — | ≥ 60% | ≥ 85% | ≥ 95% |
| Thời gian tới kết quả (p50) | ~1–2s | ≤ 3s | ≤ 2s | ≤ 1.5s |
| Ghi vào DB mà không có xác nhận | có | **không** | không | không |

Nguyên tắc xuyên suốt: **với dữ liệu y tế, "không đọc được" là kết quả chấp nhận được; "đọc sai"
thì không.** Toàn bộ các ngưỡng nên được chỉnh theo hướng ưu tiên precision hơn recall.

---

## 8. Phụ lục — chạy lại bằng chứng

```bash
python3 tools/ocr-review/pixel_reader_sim.py   # mô phỏng PixelGlucoseReader trên ảnh 7 đoạn tổng hợp
python3 tools/ocr-review/text_parser_sim.py    # kiểm thử extractGlucose với các chuỗi OCR thực tế
```

Hai script là bản dựng lại **1:1** logic Kotlin (cùng hằng số, cùng công thức), dùng để (a) chứng
minh lỗi hiện tại và (b) làm bàn thử nhanh khi hiệu chỉnh thuật toán mới trước khi viết lại bằng
Kotlin. Chỉ cần `numpy`.

---

# PHẦN II — ĐÃ TRIỂN KHAI (commit tiếp theo)

Toàn bộ Giai đoạn 1 và Giai đoạn 2 ở mục 6 đã được thực hiện. Dưới đây là kết quả.

## II.1. Kết quả đo lại (cùng bộ mô phỏng, cùng điều kiện)

| Chỉ số | Bộ đọc **cũ** | Bộ đọc **mới** |
|---|---|---|
| Đọc đúng | 5% | **90–92%** |
| **Trả về giá trị SAI** | **63%** | **0%** (chỉ còn ở khung bị loá nặng, và khung đó bị cổng chất lượng loại bỏ) |
| Không đọc được | 17% | 6–8% |
| Màn hình đảo màu | 0% đúng / 71% sai | **100% đúng** |
| Màn hình nghiêng (italic) | không hỗ trợ | **72–80% đúng** |
| Ảnh chụp tương phản thấp | 3% đúng / 54% sai | **100% đúng** |

Đo bằng 625 ảnh 7 đoạn tổng hợp (25 giá trị × 5 kiểu khung hình × 5 kiểu nhiễu: nét,
mờ, loá, nhiễu hạt, nghiêng). Chạy lại bằng `SevenSegmentReaderTest`.

## II.2. Kiến trúc mới

```
ImageProxy
 └─ crop ROI khung xanh + xoay MỘT lần (ImageUtils.cropRotated)      ← trước: xoay cả khung 1080p
    ├─ FrameQuality.inspect  → mờ / loá / phẳng ⇒ BỎ khung, báo lý do cho người dùng
    ├─ SevenSegmentReader (thuần Kotlin, không phụ thuộc Android)
    │    Otsu toàn cục → tự nhận cực tính → closing theo bề rộng nét →
    │    định vị chữ số bằng projection nửa trên → khớp 7 đoạn (7 độ nghiêng) →
    │    kiểm tra hình học → margin
    └─ ImageUtils.enhanceForOcr (nhị phân hoá + nối khe 7 đoạn + lề trắng + upscale)
         └─ ML Kit → GlucoseTextParser (thuần Kotlin)
              └─ GlucoseScanner.combine  → Reading / Status(HI,LO,ERROR) / Rejected(lý do)
                   └─ StabilityVoter: 3 khung TRÙNG KHỚP TUYỆT ĐỐI, tối đa 1 khung mâu thuẫn
                        └─ Màn hình XÁC NHẬN (sửa được số, chọn trước/sau ăn)
                             └─ chỉ khi bấm Lưu mới ghi vào DB
```

## II.3. Đối chiếu từng lỗi đã nêu ở Phần I

| Lỗi | Trạng thái | Cách xử lý |
|---|---|---|
| P0-1 ngưỡng cục bộ theo từng ô segment | ✅ | `ImageOps.otsu` — một ngưỡng toàn cục cho cả ROI |
| P0-2 lưới 3 ô chữ số cố định | ✅ | Định vị chữ số thật bằng projection + tách theo bước ô (`locateDigits`) |
| P0-3 confidence vô nghĩa, pixel ghi đè ML Kit | ✅ | `margin` = khoảng cách tới mẫu nhì; chỉ tự quyết khi margin ≥ 1.0, còn lại phải đồng thuận với ML Kit |
| P0-4 `Lo` → 10.0, `E-3` → 3.0 | ✅ | `GlucoseTextParser.status()`; chỉ sửa nhầm ký tự trong token đã có chữ số. UI hiển thị cảnh báo hạ/tăng đường huyết thay vì con số |
| P0-5 tự động lưu vào DB | ✅ | `ScannerScreen` có thẻ **Xác nhận chỉ số**: sửa số, chọn *Trước ăn / Sau ăn*, *Lưu* hoặc *Quét lại*. Bỏ heuristic `hour % 2` |
| P1-1 tiền xử lý cho ML Kit | ✅ | `enhanceForOcr`: Otsu + tự nhận cực tính + **closing nối khe 7 đoạn** + lề trắng + upscale |
| P1-2 vứt cả dòng có ngày/giờ | ✅ | Chỉ xoá đúng chuỗi ngày/giờ trong dòng (`6.1 12:45` → 6.1) |
| P1-3 `10 24` thành 10.0 | ✅ | Hai số nguyên rời trên một dòng ⇒ từ chối |
| P1-4 AVG/MEM chỉ chặn ở nhánh spatial | ✅ | `summaryRegex` dùng chung, chặn cả avg/HbA1c/ketone/control |
| P1-5 màn hình đảo màu | ✅ | Tự nhận cực tính trong `ImageOps.binarize` |
| P1-6 vote đa khung yếu | ✅ | `StabilityVoter`: khớp tuyệt đối, cửa sổ 6, xoá vote khi khung không đọc được |
| P1-7 preview và analysis khác FOV | ✅ | `UseCaseGroup` + `ViewPort` của PreviewView + `ResolutionSelector` 16:9 cho cả hai use case |
| P1-8 lấy nét / phơi sáng | ✅ | Tự lấy nét lại mỗi 2.5s, bù phơi sáng −1, thêm nút bật/tắt đèn |
| P1-9 không có cổng chất lượng khung | ✅ | `FrameQuality`: Laplacian chuẩn hoá theo tương phản + tỉ lệ pixel cháy sáng + tương phản |
| P1-10 đơn vị mg/dL | ⚠️ một phần | Chuyển đổi khi thấy chữ `mg/dL`; **bắt buộc có dấu thập phân** với nhánh 7 đoạn nên không còn đọc nhầm số nguyên thành mmol. Vẫn nên thêm Cài đặt chọn đơn vị (xem II.5) |
| P1-11 xoay cả khung ảnh | ✅ | `cropRotated`: cắt trước, xoay sau; ROI 7 đoạn còn được thu về ≤360px |
| P2 không unit-test được | ✅ | Toàn bộ thuật toán nằm trong lớp thuần JVM, **60 unit test** |
| P2 hàm chết, `CameraScreen` placeholder | ✅ | Đã xoá |
| P2 workflow CI trỏ nhánh `master` không tồn tại | ✅ | Sửa thành `main` (+ nhánh agent), giữ nguyên logic đổi tên APK |

## II.4. Những quy tắc an toàn mới (quan trọng khi đọc code)

1. **Bắt buộc có dấu thập phân** (`SevenSegmentReader.Config.requireDecimalPoint`): máy mmol/L
   luôn hiển thị một chữ số thập phân, nên `57` không bao giờ được hiểu thành 5.7 hay 57.
2. **Vị trí dấu thập phân theo quy ước**, chỉ *sự tồn tại* của nó mới cần dò tìm — đây là điểm
   mong manh nhất khi màn hình nghiêng, và quy ước loại bỏ hoàn toàn rủi ro `13.5 → 1.35`.
3. **Từ chối khi hai bộ đọc mâu thuẫn** (`ScanRejection.DISAGREEMENT`) thay vì chọn bừa.
4. **Từ chối khi màn hình bị khung cắt** hoặc khi viền máy lọt vào ROI (`touchesRoiBorder`).
5. **Từ chối khi bố cục vô lý**: số chữ số > 4, bề rộng ô lệch nhau > 1.35 lần, bước ô không đều,
   có chữ số bị rơi ở hai đầu dãy.
6. Mọi từ chối đều có lý do và được hiển thị bằng tiếng Việt cho người dùng
   ("Ảnh bị mờ…", "Bị loá…", "Máy báo LO…").

## II.5. Còn lại (đề xuất tiếp theo)

* **Cài đặt đơn vị máy đo (mmol/L | mg/dL)** để hỗ trợ máy hiển thị số nguyên.
* **Bộ ảnh vàng**: cần ~100 ảnh chụp thật màn hình máy đo (nhiều model, nhiều điều kiện sáng)
  để đo chính xác trên hiện trường; hiện các con số đến từ ảnh tổng hợp.
* **TFLite phân loại chữ số 7 đoạn** (Giai đoạn 3) khi đã có bộ ảnh thật.
* Nhận dạng nghiêng còn ~20–28% khung không đọc được (an toàn, nhưng có thể cải thiện bằng
  ước lượng góc nghiêng từ chính nét chữ).

## II.6. Kiểm thử

```bash
./gradlew testDebugUnitTest      # 60 test thuần JVM, không cần thiết bị
```

| Bộ test | Nội dung |
|---|---|
| `SevenSegmentReaderTest` | 17 test: mọi chữ số, dải 3.0–19.9, đảo màu, nghiêng, mờ, tương phản thấp, khung cắt, nhiễu, và **quét toàn dải khẳng định không trả về giá trị sai nào** |
| `GlucoseTextParserTest` | 19 test: `Lo`/`HI`/`E-3`, giờ mất dấu `:`, AVG/HbA1c/ketone, mg/dL, số nguyên trần, dấu phẩy thập phân, giá trị ngoài dải |
| `FrameQualityTest` | 6 test: nét/mờ/loá/phẳng/đảo màu |
| `StabilityVoterTest` | 6 test: quy tắc bỏ phiếu đa khung |
| `ScannerGeometryTest` | 10 test (có sẵn): ánh xạ khung xanh → ROI |
