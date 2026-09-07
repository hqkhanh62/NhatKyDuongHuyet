# Review tính năng Camera OCR — Nhật Ký Đường Huyết

> Phạm vi: toàn bộ pipeline quét máy đo đường huyết bằng camera
> (`ml/GlucoseScanner`, `ml/PixelGlucoseReader`, `ml/SevenSegmentDecoder`,
> `ui/scanner/GlucoseCameraPreview`, `ui/scanner/ScannerScreen`,
> `ui/detail/CameraScannerDialog`).

## Tóm tắt

Triết lý thiết kế của pipeline là đúng (pixel reader chuyên cho seven-segment +
ML Kit OCR + lọc ổn định theo thời gian), nhưng **cả 3 tầng đều có lỗi khiến đọc
sai chỉ số**, và lỗi ở tầng pixel reader khiến ML Kit — thành phần yếu nhất với
màn hình seven-segment — trở thành người ra quyết định trên thực tế.

| # | Nguyên nhân gốc | Mức tác động | Trạng thái |
|---|------------------|--------------|------------|
| 1 | Pixel reader dùng **hình học cố định** (3 ô số + 1 ô dấu chấm hard-code theo máy On Call Plus); `localizeDigitBand` lấy bounding-box của *mọi* pixel tối (bao gồm nhãn `mmol/L`, icon, bóng đèn) khiến các ô cố định trượt sang vị trí sai | Rất cao | ✅ Đã sửa |
| 2 | **ML Kit Latin là người quyết định trên thực tế** (pixel reader thường trả null vì lỗi 1), trong khi model Latin đọc rất tệ chữ số seven-segment: 8↔0, 5↔6, 7↔1, mất dấu thập phân | Rất cao | ✅ Đã giảm phụ thuộc |
| 3 | `combineHybrid` trả **null khi hai nguồn bất đồng** → ML Kit đọc lệch *ổn định* (5.7 ↔ 5.1) sẽ chặn vĩnh viễn kết quả đúng, hoặc buộc người dùng quét lại đến khi lỗi lặp lại đủ nhiều | Cao | ✅ Đã sửa |
| 4 | Bộ lọc ổn định dùng **ngưỡng ±0.15 trên giá trị mới nhất**: giá trị trên máy đo chỉ lẻ 0.1 nên 5.6 và 5.7 bị coi là "khớp"; lấy giá trị *mới nhất* thay vì giá trị *đa số* → dãy [5.7, 5.1, 5.2, 5.2] chấp nhận 5.2 | Cao | ✅ Đã sửa |
| 5 | **Auto-focus tự huỷ sau 2 giây** (`setAutoCancelDuration(2s)`, không cấp lại) → cầm máy đo tay ở khoảng cách 15–20 cm, sau 2 giây ảnh bị mờ, mọi frame sau đó phân tích chữ số nhoè | Trung bình | ✅ Đã sửa |
| 6 | Parser văn bản: **mất dấu thập phân** thì bỏ cả frame (`"57 mmol/L"` → null thay vì 5.7); token chỉ chứa chữ vẫn được quy đổi (`"Lo"` → 10.0!) | Trung bình | ✅ Đã sửa |
| 7 | Không xử lý **màn hình nền tối** (đèn backlight tắt/quét ban đêm) và không từ chối frame **phẳng/mờ** (glare tràn, out-of-focus) — cứ "đoán" | Trung bình | ✅ Đã sửa |

Các thay đổi giữ nguyên kiến trúc và **không thêm dependency mới** (không cần
sync Gradle).

---

## Chi tiết từng nguyên nhân và cách sửa

### 1. Pixel reader hình học cố định → decoder connected-component

**Trước đây** (`PixelGlucoseReader` cũ):

- `localizeDigitBand` tính bounding-box của toàn bộ pixel tối trong crop — tức là
  *cả* chữ số lẫn nhãn `mmol/L`, icon pin, icon nhiệt độ, v.v. Sau đó 3 ô số được
  chia theo tỉ lệ cố định (0.05–0.32 / 0.35–0.62 / 0.68–0.95) và vùng dấu chấm
  cố định (0.62–0.68). Chỉ cần nhãn hoặc icon lọt vào crop là các ô lấy mẫu trượt
  sang pixel của chữ khác → đọc sai.
- Dấu chấm thập phân **luôn** giả định nằm trước chữ số cuối → `"12"` phát hiện
  dấu chấm giả thành `"1.2"`.
- Ngưỡng tối cục bộ (`trung bình vùng × 0.85, kẹp 40–180`) không phân biệt được
  "bóng ma" LCD (segment không sáng vẫn hơi tối hơn nền) → sinh số ảo.

**Bây giờ** (`SevenSegmentDecoder` mới, thuần Kotlin trên `IntArray` nên test
được trên JVM):

1. **Otsu threshold** toàn cục + tự phát hiện cực tính (nền sáng/segment tối hay
   ngược lại — màn hình backlight ban đêm). Frame phẳng (mờ/glare tràn) →
   threshold thoát khỏi dịp 8–247 → **từ chối frame thay vì đoán**.
2. **Vùng xám (gray-zone)**: pixel sáng/tối yếu ngay sát ngưỡng bị đánh dấu;
   segment nửa-xám làm 8↔0, 5↔6 mơ hồ → trừ điểm tin cậy, dưới ngưỡng thì từ
   chối chữ số đó (và cả frame) — tránh đúng lớp lỗi người dùng gặp nhất.
3. **Morphological closing** xuyên nối các khe tóc giữa segment của cùng một
   chữ số mà không làm dính hai chữ số kề nhau.
4. **Connected components (8-connectivity)** + lọc theo kích thước/aspect, sau
   đó **gom cột** thành glyph (chữ "1" và "7" vốn tách thành 2 thành phần vì
   thanh dọc không bao giờ chạm nhau ở giữa — lý do cc thuần túy không đủ).
   Nhãn/icon thấp hơn 60% chiều cao chữ số bị loại khỏi hàng số.
5. **Dấu chấm = blob nhỏ ở đáy dòng**, chỉ hợp lệ khi có chữ số ở cả hai bên →
   hết lỗi dấu chấm giả.
6. **Mất dấu chấm thì phục hồi**: `57` → 5.7, `101` → 10.1 (máy mmol/L luôn hiển
   thị 1 chữ số thập phân; số nguyên > 30 kèm đơn vị gần như chắc chắn là mất
   dấu phân cách). Số nguyên hợp lệ ≤ 30 (`25`) vẫn giữ nguyên.
7. Chữ số mờ/tin cậy thấp → **từ chối cả frame** (thà không đọc còn hơn đọc
   thiếu: `8.7` mất chữ 8 thành `7.0` là loại lỗi nguy hiểm nhất).
8. Crop > 640 px được thu nhỏ trước khi phân tích — đủ chi tiết cho segment,
   nhẹ cho vòng lặp 4 fps.

Toàn bộ thuật toán được **thiết kế và kiểm chứng bằng prototype Python**
(`tools/prototype_seven_segment_decoder.py`; parser văn bản được kiểm chứng tương tự bằng `tools/prototype_glucose_text_parser.py`) trên 34 kịch bản: đọc thường,
nghiêng ~4°, nhiễu sensor, blur, bóng ma, glare, nền tối, mất dấu chấm — rồi
chuyển service 1:1 sang Kotlin; bộ test JVM (`SevenSegmentDecoderTest`) render
lại chính các màn hình tổng hợp đó để bắt hồi quy mà không cần máy thật.

### 2–3. Hợp nhất pixel + ML Kit

`combineHybrid` mới, theo thứ tự:

1. Pixel **rất tự tin** (≥ 0.92 — mọi segment rõ nét) → thắng cả khi ML Kit
   lệch. ML Kit Latin hay nhầm 5.7 ↔ 5.1; quy tắc cũ "bất đồng → null" để lỗi
   lặp của ML Kit chặn kết quả đúng vô thời hạn.
2. Pixel tự tin (≥ 0.85) + ML Kit khớp (|Δ| ≤ 0.15 mmol/L) → lấy pixel.
3. Hai nguồn lệch đáng tin cậy tương đương → vẫn null (không đoán).
4. Còn lại → ML Kit.

### 4. Bộ lọc ổn định → biểu quyết đa số tuyệt đối

`StableReadingTracker` mới (dùng chung cho cả `ScannerScreen` và
`CameraScannerDialog` — trước đây 2 file trùng lặp logic):

- Làm tròn mỗi lần đọc về đúng độ phân giải của máy (0.1).
- Cửa sổ 6 lần đọc, cần **4 giá trị giống hệt nhau** (giá trị đa số) mới trả
  kết quả — hết việc gộp 5.6 ≈ 5.7 và hết việc lấy "giá trị mới nhất".
- OCR lắc lư 5.7/5.1 không bao giờ chạm ngưỡng → hết deliver số sai.

### 5. Tập trung liên tục

- Lấy nét/đo sáng ban đầu vẫn nhắm vào tâm khung xanh, nhưng action được giữ lại
  và **cấp lại mỗi 2.5 s** từ analyzer (`FOCUS_REFRESH_INTERVAL_MS`) — hết tình
  trạng sau 2 giây ảnh từ từ mờ dần. `CameraControl` là thread-safe nên gọi từ
  executor của analyzer an toàn.

### 6. Parser văn bản (ML Kit)

- `"57 mmol/L"` / `"101 mmol/L"` → 5.7 / 10.1 (t recovery mất dấu chấm, chỉ khi
  có đơn vị mmol rõ ràng và giá trị ngoài khoảng hợp lý).
- Token **chỉ toàn chữ** (`"Lo"`, `"II"`) chỉ được quy đổi số khi dòng có đơn
  vị — chỉ báo `"Lo"` của máy đo không còn bị đọc thành 10.0. Token lẫn
  chữ+số (`"I0"` → 10) vẫn chấp nhận vì đó là dạng ML Kit trả về từ chữ số thật.
- Dấu `:` giữa 2 chữ số đơn (`"5:7"` → 5.7) được quy về dấu chấm; giờ phút thật
  (`08:32` luôn có phút 2 chữ số) không bị đổi.
- `"5-7"` không còn bị `extractDate` biến thành ngày "2026-07-05" (dạng 2 phần
  phải có ít nhất một vế 2 chữ số).

### 7. Kiểm soát chất lượng frame

Decoder mới từ chối frame phẳng (mờ/glare) ngay từ bước Otsu; glare loá chỉ là
vùng *sáng* nên không tạo thành phần tối giả; bóng ma nằm giữa 2 mode của
histogram nên bị lọc; nếu vẫn lọt (bóng ma đậm) thì giá trị vượt khoảng hợp lý
→ từ chối chứ không đọc bừa.

---

## File thay đổi

**Mới (4)**

```
app/src/main/java/com/example/nhatkyduonghuyet/ml/SevenSegmentDecoder.kt     (decoder 7 đoạn, JVM-testable)
app/src/main/java/com/example/nhatkyduonghuyet/ml/StableReadingTracker.kt    (bộ lọc ổn định dùng chung)
app/src/test/java/com/example/nhatkyduonghuyet/ml/SevenSegmentDecoderTest.kt
app/src/test/java/com/example/nhatkyduonghuyet/ml/StableReadingTrackerTest.kt
tools/prototype_seven_segment_decoder.py   (prototype kiểm chứng decoder)
 tools/prototype_glucose_text_parser.py      (prototype kiểm chứng parser văn bản)
```

**Sửa (7)**

```
ml/GlucoseScanner.kt          (combineHybrid, parser: dấu chấm mất/Lo/":", extractDate)
ml/PixelGlucoseReader.kt      (viết lại: adapter Bitmap mỏng quanh decoder mới)
ml/GlucoseConstants.kt        (+PIXEL_OVERRIDE_CONFIDENCE, dọn hằng số bỏ dùng)
ui/scanner/GlucoseCameraPreview.kt  (cấp lại focus định kỳ)
ui/scanner/ScannerScreen.kt   (dùng StableReadingTracker)
ui/detail/CameraScannerDialog.kt     (dùng StableReadingTracker)
app/src/test/.../GlucoseScannerTest.kt (+13 test: parser & hybrid)
```

Không đổi `build.gradle`, không thêm dependency.

## Chạy kiểm tra

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Bộ test liên quan: `GlucoseScannerTest` (25 test), `SevenSegmentDecoderTest`
(17 test), `StableReadingTrackerTest` (6 test), `ScanRoiGeometryTest` (6 test).

> Lưu ý môi trường dev: máy này (sandbox) không có JDK/Android SDK nên chưa
> chạy được Gradle trực tiếp; logic thuần đã được kiểm chứng bằng prototype
> Python tương đương 1:1 (34/34 PASS). Trên PC có SDK hãy chạy lệnh trên để
> xác nhận biên dịch + test trước khi cài lên máy thật.

## Kiểm thử trên máy thật — trọng tâm

1. So sánh 2 luồng (Dashboard → Quét máy đo và DayDetail → icon camera): phải
   cho kết quả giống nhau.
2. Giữ máy đo **đứng yên > 3 giây** trong khung xanh — trước đây sau ~2 giây ảnh
   mờ dần; giờ focus được cấp lại, số đọc phải ổn định.
3. Thử các giá trị đặc thù: `5.7`, `10.1` (số 1 đứng đầu), `15.2`, `25.0`,
   giá trị > 13.0 (cảnh báo rung/nháy đỏ).
4. Thử ban đêm/tắt đèn (backlight máy đo sáng, nền tối) và bật flash (chú ý
   tránh phản chiếu lên mặt kính).
5. Khi không đọc được, app sẽ **không tự điền số sai** — hãy căn lại khung xanh
   cho chỉ số lấp đầy, tránh bóng loá.

## Khuyến nghị cho giai đoạn sau (chưa làm)

1. **CNN phân loại chữ số seven-segment bằng TFLite** (project đã có dependency
   TensorFlow Lite cho LSTM): huấn luyện trên ảnh tổng hợp + thật, thay bảng
   pattern matching — đây là bước nâng chính xác lớn nhất còn lại. Decoder hiện
   tại đã tách bạch bước "tìm chữ số" (giữ nguyên được) với bước "nhận dạng"
   (thay được bằng model).
2. Cân nhắc thử **ML Kit Chinese/Japanese/Korean recognizer** cho chữ số LCD
   (một số báo cáo cộng đồng cho thấy nhận diện digit kiểu này tốt hơn model
   Latin) — cần thêm 1 dependency và đo trên máy thật.
3. Bắt sự kiện tap-to-focus cho người dùng chủ động lấy nét.
4. Ghi log/telemetry các frame bị từ chối (lý do: mờ/glare/chữ số mơ hồ) để
   đo lường và tinh chỉnh ngưỡng theo dữ liệu thật.
