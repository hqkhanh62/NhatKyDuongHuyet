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
