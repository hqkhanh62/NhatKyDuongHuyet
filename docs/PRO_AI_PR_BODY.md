<!-- Nội dung này để dán vào ô Description khi mở PR trên GitHub.
     Nhánh đã push: arena/01a0a44d-nhatkyduonghuyet (2 commit: 6a543f2, f2f74d9).
     Mở PR: https://github.com/hqkhanh62/NhatKyDuongHuyet/compare/main...arena/01a0a44d-nhatkyduonghuyet -->

## Tóm tắt

Nâng cấp hệ thống quét camera máy đo đường huyết lên bản **Pro AI**: ML Kit OCR **đa tầng** (chỉ số + giờ + ngày), **Auto Import Pipeline** (tự điền ngày/giờ, tự phân loại buổi, tự làm sạch dữ liệu, tự lưu) và **Real-time AI Loop** (Quét → Lưu → Dự báo → Cảnh báo) với **giao diện quét + banner kiểm tra trước khi xác nhận**.

Không thêm dependency mới → không cần sync Gradle.

## Đối chiếu yêu cầu → mã nguồn

| Yêu cầu | Triển khai | Test |
|---|---|---|
| AI Camera OCR (ML Kit) quét trực tiếp màn hình | `ml/GlucoseScanner.kt` (ML Kit `text-recognition` + `SevenSegmentDecoder`, giữ nguyên pipeline hybrid theo độ tin cậy) | `GlucoseScannerTest` (giữ nguyên, vẫn pass) |
| Tự nhận diện số `X.X` mmol/L realtime | `ml/MeterTextParser.kt` – tầng 1: chọn dòng có chữ to nhất, nhận `mg/dL`, khôi phục dấu thập phân bị mất, không nhầm `"Lo"` → 10 | `MeterTextParserTest` |
| **OCR đa tầng** – quét toàn bộ văn bản màn hình | `MeterTextParser.parse()` trả `MeterDisplayFields` (chỉ số, giờ, ngày, văn bản OCR, mã lỗi máy đo) + callback `onOcrFields` cho **mọi** frame | `MeterTextParserTest` |
| Nhận diện **giờ** `HH:mm` | Tầng 2: `08:32`, `8:32`, `09:15:30`, `7:30 AM`, `7.45 PM`; dòng có nhãn `Time:` được ưu tiên | `MeterTextParserTest` |
| Nhận diện **ngày** `DD/MM` / `MM/DD` / `YYYY-MM-DD` | Tầng 3: kiểm tra số ngày trong tháng, tự sửa khi OCR đổi chỗ tháng/ngày, quy ước ngày-trước khi mơ hồ + cờ `ambiguous`, loại ngày lệch > 1 ngày so với điện thoại | `MeterTextParserTest` |
| **Auto Import**: ưu tiên ngày/giờ trên máy đo | `domain/scanner/AutoImportPipeline.kt` → `resolveDate/resolveTime`, nguồn `METER` | `AutoImportPipelineTest` |
| **Auto Import**: fallback ngày/giờ hệ thống | như trên, nhánh `FieldSource.SYSTEM` khi máy đo không hiển thị | `AutoImportPipelineTest` |
| **Tự phân loại Buổi** theo giờ AI vừa quét | `GlucoseSession.fromHour` (Sáng 5–10h, Trưa 11–13h, Chiều 14–17h, Tối 18–4h) + ô **Trước ăn / Sau ăn 2 giờ** theo mốc bữa ăn (`decideSlot`) | `AutoImportPipelineTest` |
| **Auto Clean**: loại nhiễu ngoài ngưỡng an toàn | `AutoImportPipeline.clean`: NaN/∞, ngoài `2.0 – 30.0`, mã lỗi `E-05/HI/LO`, làm tròn 0.1. `DashboardViewModel` (heuristic `hour % 2` + tự sửa ngày) đã được thay bằng pipeline chung | `AutoImportPipelineTest`, `FrameAccumulatorTest` |
| **Tự động lưu** khi xác nhận + kích hoạt luồng dự báo AI | `ui/scanner/ScanViewModel.kt`: `plan()` → `LogRepository.insertLog` → `RealtimePredictor.refresh` → `ScanInsightEngine` → `AIRepository.autoCalibrate` + `checkRetrainStatus`; Widget tự cập nhật qua repository | `AutoImportPipelineTest` (Insert/Update/Duplicate) |
| **Real-time AI Loop**: tính lại HbA1c + Risk Alerts | `ScanInsightEngine.analyze`: HbA1c ước tính (kèm % thay đổi), TIR, dự báo 6h, cảnh báo 3 mức (hạ ≤3.0 / <4.0, >13.0, >16.7, sau ăn 2h >7.8, đói 5.6–6.9, lệch ≥2.0 so với trung bình, HbA1c >7% / ≥9%) | `ScanInsightEngineTest`, `GlucoseMetricsTest` |
| **Giao diện quét hiện đại** có khung căn chỉnh | `ui/scanner/ScanOverlay.kt`: mặt nạ tối ngoài khung, 4 góc neo, dải quét chạy, chấm tiến trình "đang khoá chỉ số", chip giờ/ngày OCR, **tap-to-focus** | review trên máy thật |
| **Banner kết quả tức thì dưới camera** | `ui/scanner/ScanResultBanner.kt`: chỉ số lớn + ±0.1, chip Buổi, ô Trước/Sau ăn, ngày+giờ kèm nguồn, danh sách cảnh báo, HbA1c/TIR/dự báo, "Xem văn bản OCR", **XÁC NHẬN & LƯU** / QUÉT LẠI; banner **ĐÃ LƯU** có **HOÀN TÁC** | review trên máy thật |

## Điểm đáng chú ý về chất lượng

* **Toàn bộ logic nghiệp vụ là Kotlin thuần** (`ml/MeterTextParser`, `domain/scanner/*`, `domain/health/GlucoseMetrics`) → test được trên JVM, không cần thiết bị; 5 lớp test mới với các ca khó (mất dấu thập phân, `MM/DD` vs `DD/MM`, ngày máy đo lệch, nhiễu ngoài ngưỡng, giá trị lật qua lại 5.7/5.1).
* `tools/prototype_meter_text_parser.py` transliterate 1:1 logic sang Python để đối chiếu regex — **ALL PASS** (chạy được cả khi không có JDK).
* Sửa 2 lỗi âm ỉ: (1) HbA1c/TIR sau khi lưu bị **tính hai lần** chính chỉ số vừa quét; (2) `Float → Double` làm DB lưu `5.700000286102295`.
* `GlucoseMetrics` trở thành **một định nghĩa duy nhất** cho HbA1c / trung bình ngày / dãy đưa vào LSTM — Dashboard và luồng quét không thể lệch nhau nữa.
* Hành vi cũ được giữ: ML Kit vẫn là nguồn chính, pixel reader vẫn thắng khi đủ tin cậy, bộ phiếu ổn định vẫn yêu cầu 4 frame trùng nhau.

## Test plan

- [ ] `./gradlew testDebugUnitTest` (CI sẽ chạy; ưu tiên 5 lớp test mới + `GlucoseScannerTest`, `SevenSegmentDecoderTest`)
- [ ] `./gradlew assembleDebug` → cài lên máy, mở **Quét máy đo – AI OCR**
- [ ] Quét máy đo có hiển thị giờ/ngày → banner hiện đúng giờ/ngày và chip "từ máy đo"
- [ ] Che/không có giờ trên máy đo → banner hiện "giờ hệ thống", Buổi vẫn được phân loại đúng
- [ ] Đặt máy đo báo lỗi `E-05`/`HI`/`LO` hoặc che chỉ số → không lưu, có thông báo
- [ ] Bật/tắt **Tự lưu**; bấm **HOÀN TÁC** sau khi lưu → bản ghi biến mất (hoặc trở về giá trị cũ)
- [ ] Sửa tay ở banner (±0.1, đổi Buổi, đổi Trước/Sau ăn) → dữ liệu vào DB đúng ô đã chọn
- [ ] Quét chỉ số cao (>13) → rung SOS + nhấp nháy đỏ + cảnh báo CRITICAL
- [ ] `python3 tools/prototype_meter_text_parser.py` → ALL PASS

## Lưu ý cho reviewer

1. PR này **có kèm commit `09c91f0`** (*fix(ocr): cai thien do chinh xac quet camera duong huyet*) — commit của nhánh cha `arena/01a0775d-nhatkyduonghuyet` mà main chưa merge. Nếu muốn xem **riêng** phần Pro AI, đổi base sang `arena/01a0775d-nhatkyduonghuyet` (lúc đó diff chỉ còn 2 commit `6a543f2` + `f2f74d9`).
2. Chi tiết thiết kế + giới hạn đã biết (00:00–04:59 thuộc buổi *Tối* của cùng ngày; `MM/DD` vs `DD/MM` khi cả hai ≤ 12 là bất khả phân tích nên app gắn cờ để người dùng kiểm) mô tả trong `docs/PRO_AI_CAMERA_OCR.md`.
3. Chưa chạy được `gradlew` trong môi trường agent (không có JDK/Android SDK) → phần xác minh logic dựa vào bộ test JVM mới + prototype Python; CI là bước chốt biên dịch.
