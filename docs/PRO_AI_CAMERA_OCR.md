# Pro AI Camera — Nâng cấp hệ thống quét camera thông minh

Tài liệu mô tả phiên bản "Pro AI" của luồng quét máy đo đường huyết
(commit tiếp theo trên nhánh `arena/01a0a44d-nhatkyduonghuyet`).

## 1. Bức tranh tổng thể

```
Camera (CameraX ImageAnalysis, 4 fps)
   │
   ├─ PixelGlucoseReader / SevenSegmentDecoder   (đọc từng thanh của màn LCD)
   └─ ML Kit Text Recognition 16.0.0             (đọc TOÀN BỘ văn bản màn hình)
                    │
             MeterTextParser  ← OCR ĐA TẦNG
        tầng 1: chỉ số X.X mmol/L
        tầng 2: giờ HH:mm | HH:mm:ss | 7:30 AM
        tầng 3: ngày DD/MM | MM/DD | DD/MM/YYYY | YYYY-MM-DD
        phụ:    mã lỗi máy đo (E-05, HI, LO)
                    │
        combineHybrid (pixel + ML Kit, theo độ tin cậy)
                    │
             FrameAccumulator  → Auto Clean (2.0 – 30.0, bỏ phiếu 4 frame)
                    │
             AutoImportPipeline → ngày/giờ (máy đo → fallback hệ thống)
                                → tự phân loại Buổi + ô Trước/Sau ăn
                                → ImportPlan (Insert / Update / Duplicate)
                    │
        Banner kiểm tra nhanh dưới camera ──▶ xác nhận ──▶ Room (LogEntry)
                    │
             REAL-TIME AI LOOP: GlucoseMetrics (HbA1c tính lại)
                              + RealtimePredictor (LSTM dự báo 6h/24h)
                              + ScanInsightEngine (Risk Alerts)
                              + AIRepository.autoCalibrate / checkRetrainStatus
```

## 2. Đối chiếu yêu cầu → mã nguồn

| Yêu cầu "Pro AI" | Triển khai | Nơi kiểm chứng |
|---|---|---|
| AI Camera OCR (ML Kit) quét trực tiếp màn hình máy đo | `ml/GlucoseScanner.kt` giữ nguyên pipeline ML Kit `text-recognition:16.0.0` + bộ giải mã seven-segment | `GlucoseScannerTest` |
| Tự nhận diện số X.X (mmol/L) theo thời gian thực | `MeterTextParser.extractReading` / `extractReadingFromLines` (chọn dòng có chữ to nhất, không nhầm với nhãn DAY/AVG) | `MeterTextParserTest` |
| OCR đa tầng — quét toàn bộ văn bản màn hình | `MeterTextParser.parse(rawText, lines)` trả `MeterDisplayFields` (chỉ số + giờ + ngày + văn bản + mã lỗi) | `MeterTextParserTest` |
| Nhận diện giờ HH:mm | `MeterTextParser.extractTime` (08:32, 9:15:30, 7:30 AM, `Time:` được ưu tiên) | `MeterTextParserTest` |
| Nhận diện ngày DD/MM, MM/DD, YYYY-MM-DD | `MeterTextParser.extractDate` (kiểm tra số ngày trong tháng, tự sửa khi tháng/ngày bị OCR đổi chỗ, quy ước ngày-trước khi mơ hồ và đánh dấu `ambiguous`) | `MeterTextParserTest` |
| Auto Import — ưu tiên ngày/giờ trên máy đo | `AutoImportPipeline.resolveDate` / `resolveTime` (nguồn `METER`) | `AutoImportPipelineTest` |
| Auto Import — fallback ngày/giờ hệ thống | như trên, nhánh `FieldSource.SYSTEM` khi máy đo không hiển thị | `AutoImportPipelineTest` |
| Tự động phân loại Buổi theo giờ AI vừa quét | `GlucoseSession.fromHour` (Sáng 5–10h, Trưa 11–13h, Chiều 14–17h, Tối 18–4h) + `AutoImportPipeline.draft` | `AutoImportPipelineTest` |
| Auto Clean — loại nhiễu ngoài 2.0 – 30.0 | `AutoImportPipeline.clean` (NaN/Inf, ngoài ngưỡng, mã lỗi máy đo, làm tròn 0.1) | `AutoImportPipelineTest`, `FrameAccumulatorTest` |
| Tự động lưu thẳng vào DB khi xác nhận | `ScanViewModel.confirm/persist` → `AutoImportPipeline.plan` → `LogRepository.insertLog` | `AutoImportPipelineTest` (Insert/Update/Duplicate) |
| Kích hoạt toàn bộ luồng dự báo AI | `ScanViewModel.saveEntry`: `RealtimePredictor.refresh` → `ScanInsightEngine` → `AIRepository.autoCalibrate` + `checkRetrainStatus` | `ScanInsightEngineTest`, `GlucoseMetricsTest` |
| Real-time AI Loop: Quét → Lưu → Dự báo → Cảnh báo | banner Review/Saved của `ScanViewModel` + `ScanInsightEngine` (HbA1c ước tính tính lại ngay khi quét, chưa cần lưu) | `ScanInsightEngineTest` |
| Cảnh báo rủi ro khi vượt ngưỡng | `ScanInsightEngine.analyze` → `RiskAlert(level, message)` với 3 mức INFO/WARNING/CRITICAL (hạ < 4.0, nặng ≤ 3.0, rất cao > 16.7, sau ăn 2h > 7.8, đường đói 5.6–6.9, lệch trung bình ≥ 2.0, HbA1c > 7%) | `ScanInsightEngineTest` |
| Giao diện quét hiện đại, khung căn chỉnh | `ui/scanner/ScanOverlay.kt`: mặt nạ tối ngoài khung, 4 góc neo, dải quét chạy, chấm tiến trình khoá chỉ số, chip giờ/ngày OCR, tap-to-focus | xem mục 4 |
| Banner kết quả tức thì dưới camera để kiểm tra trước khi xác nhận | `ui/scanner/ScanResultBanner.kt`: chỉ số lớn + nút ±0.1, chip Buổi, chip Trước/Sau ăn, ngày+giờ kèm nguồn ("từ máy đo" / "giờ hệ thống"), cảnh báo, HbA1c/TIR/dự báo, "Xem văn bản OCR", nút XÁC NHẬN & LƯU / QUÉT LẠI, và nút HOÀN TÁC sau khi lưu | xem mục 4 |

## 3. File thay đổi

**Mới — logic thuần Kotlin (test được trên JVM, không Android/ML Kit):**

```
ml/MeterOcr.kt                      OcrLine, GlucoseReading, MeterTime, MeterDate, MeterDisplayFields
ml/MeterTextParser.kt               OCR đa tầng (chỉ số / giờ / ngày / mã lỗi)
domain/scanner/GlucoseSession.kt    4 buổi + ReadingSlot + FieldSource
domain/scanner/AutoImportPipeline.kt Auto Clean, resolve ngày/giờ, decideSlot, ImportPlan
domain/scanner/FrameAccumulator.kt  Auto Clean + bỏ phiếu ổn định (StableReadingTracker)
domain/scanner/ScanInsightEngine.kt Risk alerts + HbA1c ước tính + TIR
domain/health/GlucoseMetrics.kt     Công thức dùng chung (HbA1c, trung bình ngày, TIR)
ui/scanner/ScanOverlay.kt           Khung căn chỉnh, mặt nạ, dải quét, chip OCR
ui/scanner/ScanResultBanner.kt      Banner kiểm tra + banner đã lưu + tóm tắt AI
ui/scanner/ScanUiState.kt           ScanPhase: Scanning / Review / Saved
ui/scanner/ScanViewModel.kt         Hilt ViewModel điều phối cả luồng
```

**Sửa:**

```
ml/GlucoseScanner.kt                dùng MeterTextParser, trả MeterDisplayFields + confidence,
                                    thêm callback onOcrFields cho mọi frame
ml/StableReadingTracker.kt          public requiredMatches + matchCount() cho chỉ báo tiến trình
ui/scanner/GlucoseCameraPreview.kt  slot overlay + tap-to-focus + onOcrFields
ui/scanner/ScannerScreen.kt         viết lại theo ScanViewModel (banner, auto-save, cảnh báo)
ui/detail/CameraScannerDialog.kt    bước "kiểm tra trước khi xác nhận" trong đối thoại
ui/detail/SessionEntryCard.kt       Auto Clean khi điền ô, tự điền Giờ từ máy đo, ghi chú nguồn
ui/navigation/AppNavHost.kt         route scanner dùng ScanViewModel
ui/dashboard/DashboardViewModel.kt  onGlucoseScanned đi qua AutoImportPipeline; HbA1c/trung bình
                                    ngày dùng GlucoseMetrics (một định nghĩa duy nhất)
```

**Test mới:** `MeterTextParserTest`, `AutoImportPipelineTest`, `FrameAccumulatorTest`,
`ScanInsightEngineTest`, `GlucoseMetricsTest` (chạy cùng bộ test có sẵn, không cần máy thật).

## 4. Hành vi người dùng nhìn thấy

1. Mở **Quét máy đo – AI OCR** từ Dashboard. Khung neo sáng ở giữa, phần còn lại
   của ảnh bị làm tối; dải quét chạy dọc cho đến khi khoá được chỉ số.
2. Chip dưới khung hiện ngay `6.2 mmol/L`, `Giờ 08:32`, `Ngày 20/08` vừa đọc
   được; 4 chấm nhỏ báo tiến trình "4 frame liên tiếp trùng chỉ số".
3. Máy đo báo lỗi (E-05/HI/LO) → app nói rõ "Máy đo đang báo lỗi E-05" và **không**
   lưu; chỉ số ngoài 2.0–30.0 bị loại kèm thông báo "Bỏ qua giá trị nhiễu …".
4. Khi chỉ số ổn định:
   * **Tự lưu = BẬT** (mặc định): dữ liệu vào thẳng Room, banner chuyển sang trạng
     thái *ĐÃ LƯU* với HbA1c vừa tính lại (kèm % thay đổi), TIR, dự báo 6 giờ và
     danh sách cảnh báo; có nút **HOÀN TÁC** để xoá bản ghi vừa lưu.
   * **Tự lưu = TẮT**: hiện banner *KIỂM TRA TRƯỚC KHI LƯU* — sửa chỉ số bằng
     ±0.1, đổi Buổi, đổi Trước/Sau ăn, đổi sang giờ điện thoại nếu máy đo không có
     giờ — rồi bấm **XÁC NHẬN & LƯU**.
5. Chạm vào màn hình để lấy nét đúng điểm cần quét; nút đèn flash vẫn giữ nguyên.
6. Cảnh báo CRITICAL (hạ đường huyết ≤ 3.0, rất cao > 16.7 …) kèm rung SOS và
   nhấp nháy đỏ ngay trên màn hình quét.

## 4b. Bản sửa lỗi: "chỉ quét phần trên màn hình" và dòng mm-dd / hh:mm

**Hiện tượng người dùng báo:** vùng quét chỉ phủ phần trên của màn hình máy đo, không quét từ trên
xuống dưới, nên chỉ số bị đọc sai; dòng trên cùng (mm-dd góc trái, hh:mm góc phải, chữ nhỏ) hầu như
không nhận dạng được hoặc nhận dạng sai.

**Bốn nguyên nhân gốc, mỗi cái một lớp:**

| Lớp | Nguyên nhân | Cách sửa |
|---|---|---|
| Hình học | Crop phân tích được căn giữa theo khung hướng dẫn rồi chỉ nới 12% đều 4 phía. Dòng trạng thái nằm ở **mép trên** màn hình nên rơi ra ngoài crop. Trùng hơn, `PreviewView` (16:9) và `ImageAnalysis` (YUV, nhiều máy về 4:3) là hai stream khác nhau, nên ROI ánh xạ từ khungPreview **hẹp hơn** cái người dùng nhìn thấy | `OCR_ROI_PADDING_X = 0.10f` / `OCR_ROI_PADDING_Y = 0.34f` (nới dọc mạnh hơn ngang), thêm `smallTextSweepRoi()` phủ từ `0.02` tới `0.98` chiều cao, `DEFAULT_DISPLAY_ROI` mở rộng, `MIN_ROI_FRACTION 0.10 → 0.16` |
| Tiền xử lý | `upscaleForOcr` chỉ nhìn **chiều ngang** (`width >= 720` → không phóng gì cả). Dòng mm-dd/hh:mm cao ~12 px, dưới nửa ngưỡng ~32 px mà ML Kit cần | `scaleToCover(minWidth, minHeight)` xét cả hai trục; `prepareSmallTextBitmap()` cho riêng dải chữ nhỏ: tương phản 2.1, mục tiêu 1400×220 px, phóng tới **8x** |
| Khung căn chỉnh | `SCAN_FRAME_ASPECT_RATIO = 1.6` (rộng-ngắn) buộc người dùng phải cắt bớt mép trên/dưới màn hình khi căn khung | Đổi sang **1.34**; dải quét trong overlay nay chạy hết `viewHeight` thay vì quẩn quanh trong khung, phần trên/dưới chỉ bị tối nhẹ (0.20) vì thật ra AI vẫn đọc |
| Luật đọc | Ngày không năm chỉ hiểu theo kiểu VN; `S`/`Z`/`l`/`O` bị OCR đọc thành chữ nên `14:35` thành `l4:3S`; `09-23` và `14:35` dính liền thành `09-2314:35` | Thêm `parseSmallText()` (chế độ `loose`): `repairOcrDigits()` + `splitGluedRow()` + `LOOSE_TIME`/`LOOSE_SHORT_DATE`; **dấu gạch ngang `-` = MM/DD**, gạch chéo `/` = DD/MM; nếu một cách hiểu rơi khỏi cửa sổ hợp lý thì lấy cách còn lại và đánh dấu `ambiguous` |

**Bất biến quan trọng nhất:** dòng chữ nhỏ **không bao giờ** được quyết định chỉ số.
`repairOcrDigits()` cố tình loại dấu chấm khỏi bảng sửa (chỉ sửa cặp `XX.YY` đủ 2+2 chữ số) để
`"5.O"` không bị "sửa" thành `"5.0"` — sửa như thế là đổi luôn đường huyết của người bệnh.
`parseSmallText(includeGlucose = false)` là mặc định; chỉ bật lên khi crop chính **không** đọc ra số
nào, và khi đó vẫn đi qua guard `DATE_OR_TIME_ROW` nên `09-23` không bao giờ thành 9.23.

**Chi phí có kiểm soát:** lượt quét thứ hai chỉ chạy khi còn thiếu trường, không quá 1 lần/600 ms,
và bỏ cuộc sau 6 lần liên tiếp không thấy gì (`MAX_EMPTY_SWEEPS`) — máy đo không in giờ/ngày lên
màn hình sẽ không bị quét lặp vô ích. Counter này tự reset khi frame mất chỉ số (người dùng đưa máy ra).

**Test khoá hành vi:** `MeterTextParserTest` (9 ca mới: sửa ký tự đúng phạm vi, tách token dính,
đọc mm-dd + hh:mm, ưu tiên theo dấu phân cách, tie-break theo ngày hôm nay, `merge`),
`ScanRoiGeometryTest` (6 ca mới: dải quét phủ 0.02→0.98, nới dọc > nới ngang, khung 1.34),
và `python3 tools/prototype_meter_text_parser.py` → **125 checks ALL PASS**.

## 5. Chạy kiểm thử

```bash
# Test logic OCR + luồng tự động nhập + cảnh báo (nhanh, không cần máy)
./gradlew :app:testDebugUnitTest --tests "*MeterTextParserTest" \
  --tests "*AutoImportPipelineTest" --tests "*FrameAccumulatorTest" \
  --tests "*ScanInsightEngineTest" --tests "*GlucoseMetricsTest" \
  --tests "*GlucoseScannerTest" --tests "*SevenSegmentDecoderTest"

# Đối chiếu regex/bằng chứng thiết kế bằng Python (không cần JDK)
python3 tools/prototype_meter_text_parser.py
```

`tools/prototype_meter_text_parser.py` transliterate 1:1 `MeterTextParser` +
chính sách phân buổi/chọn ô, và chạy đúng corpus của test JVM. Khi sửa regex
trong Kotlin, cập nhật song song file này để giữ bằng chứng kiểm chứng.

## 6. Giới hạn đã biết (có chủ đích)

* Buổi được suy ra từ **giờ** nên 00:00–04:59 thuộc buổi **Tối** của cùng ngày
  dương lịch (nhật ký chỉ có 4 ô Sáng/Trưa/Chiều/Tối).
* Ô *Trước ăn / Sau ăn 2 giờ* là suy luận theo mốc bữa ăn (6:30, 11:30, 18:30)
  và ưu tiên ô đang trống; người dùng luôn đổi được ở banner trước khi lưu.
* `MM/DD` và `DD/MM` khi cả hai vế ≤ 12 là bất khả phân tích về mặt ngữ nghĩa:
  app chọn quy ước Việt Nam (ngày trước), giảm độ tin cậy và gắn cờ `ambiguous`
  để người dùng kiểm lại.
* Ngày trên máy đo lệch hơn 1 ngày so với điện thoại (hoặc quá cũ 5 năm) bị loại —
  đồng hồ máy đo chạy sai không được phép làm lệch cả chuỗi nhật ký.
* ML Kit chỉ chạy trên thiết bị; `MeterTextParser` (logic được test) nhận đúng
  phần văn bản mà ML Kit trả về, nên mọi hồi quy về parse đều bắt được trên JVM.
