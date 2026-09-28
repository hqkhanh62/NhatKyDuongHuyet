# Hướng dẫn cập nhật thay đổi Camera/OCR về PC và chạy build

Toàn bộ thay đổi nằm trong **1 commit duy nhất**:

- Nhánh: `arena/01a0673e-nhatkyduonghuyet`
- Commit: `5758fc6` — *Cai thien do chinh xac quet camera: anh xa khung xanh -> ROI phan tich, dung chung 1 camera preview*
- Nhánh gốc: `master`/`main` tại commit `713be5a`

## Danh sách file thay đổi (8 file)

**File mới (3):**
```
app/src/main/java/com/example/nhatkyduonghuyet/ml/ScanRoiGeometry.kt
app/src/main/java/com/example/nhatkyduonghuyet/ui/scanner/GlucoseCameraPreview.kt
app/src/test/java/com/example/nhatkyduonghuyet/ml/ScanRoiGeometryTest.kt
```

**File sửa (5):**
```
app/src/main/java/com/example/nhatkyduonghuyet/ml/GlucoseScanner.kt
app/src/main/java/com/example/nhatkyduonghuyet/ml/ImageUtils.kt
app/src/main/java/com/example/nhatkyduonghuyet/ml/PixelGlucoseReader.kt
app/src/main/java/com/example/nhatkyduonghuyet/ui/detail/CameraScannerDialog.kt
app/src/main/java/com/example/nhatkyduonghuyet/ui/scanner/ScannerScreen.kt
```

Không có thay đổi nào ở `build.gradle` / dependency → **không cần sync Gradle thêm thư viện mới**.

---

## Cách 1 — Kéo trực tiếp từ GitHub (khuyến nghị)

Mở terminal (PowerShell / Git Bash) tại thư mục project trên PC:

```bash
# 1. Lưu lại việc đang làm dở (nếu có)
git status
git stash -u          # bỏ qua nếu cây làm việc đã sạch

# 2. Lấy nhánh mới về
git fetch origin arena/01a0673e-nhatkyduonghuyet

# 3. Chuyển sang nhánh đó
git checkout -b arena/01a0673e-nhatkyduonghuyet origin/arena/01a0673e-nhatkyduonghuyet
# Nếu nhánh đã tồn tại local:
#   git checkout arena/01a0673e-nhatkyduonghuyet && git pull --ff-only origin arena/01a0673e-nhatkyduonghuyet

# 4. Kiểm tra đúng commit
git log --oneline -1        # phải thấy 5758fc6
```

### Nếu muốn giữ nguyên nhánh hiện tại, chỉ lấy commit này về
```bash
git fetch origin arena/01a0673e-nhatkyduonghuyet
git cherry-pick 5758fc6
```

---

## Cách 2 — Dùng Android Studio (không gõ lệnh)

1. `Git` → `Fetch`
2. Mở tab **Git** ở thanh dưới → **Remote** → chuột phải
   `origin/arena/01a0673e-nhatkyduonghuyet` → **Checkout**
3. `File` → **Sync Project with Gradle Files**
4. `Build` → **Rebuild Project**

---

## Cách 3 — Áp patch thủ công (khi PC không kết nối được GitHub)

File patch đã kèm sẵn trong repo: `docs/0001-camera-ocr-improvement.patch`.

Tải file đó về PC rồi tại thư mục project chạy:

```bash
# Cách A: áp và tạo commit luôn
git am docs/0001-camera-ocr-improvement.patch

# Cách B: chỉ áp thay đổi, chưa commit
git apply --3way docs/0001-camera-ocr-improvement.patch

# Xem trước mà không ghi gì (kiểm tra patch có sạch không)
git apply --check docs/0001-camera-ocr-improvement.patch
```

Nếu `git am` bị lỗi giữa chừng: `git am --abort` rồi dùng `git apply --3way`.

---

## Chạy test và build

```bash
# Linux / macOS / Git Bash
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Windows CMD / PowerShell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Lần đầu nên chạy sạch cache Kotlin để tránh lỗi incremental do file bị xoá/đổi:

```bash
./gradlew clean :app:testDebugUnitTest :app:assembleDebug
```

### Kết quả mong đợi

- Unit test: `GlucoseScannerTest` (cũ) + `ScanRoiGeometryTest` (6 test mới) đều PASS.
- Báo cáo test HTML: `app/build/reports/tests/testDebugUnitTest/index.html`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

### Yêu cầu môi trường
- JDK 17
- `local.properties` có `sdk.dir` trỏ tới Android SDK (xem `local.properties.example`)
- Có mạng cho lần build đầu (tải dependency ML Kit / CameraX)

---

## Kiểm thử trên máy thật (quan trọng)

Sau khi cài `app-debug.apk`, so sánh 2 luồng — **chúng phải cho kết quả giống nhau** trên cùng một lần đo:

1. **Dashboard → Quét máy đo** (toàn màn hình)
2. **DayDetail → biểu tượng camera ở ô đường huyết** (dialog)

Trước đây dialog đọc sai nhiều hơn vì khung xanh nhỏ hơn nhưng vùng phân tích lại cố định. Giờ cả hai dùng chung `GlucoseCameraPreview`, khung xanh được ánh xạ đúng vào vùng ảnh phân tích.

Mẹo test:
- Đưa màn hình máy đo **lấp đầy khung xanh**, cách 15–20 cm.
- Thử cả phòng sáng và phòng tối (nút bật/tắt đèn flash ở góc trên phải).
- Thử các giá trị 1 chữ số thập phân (5.7), 2 chữ số (10.1) và giá trị cao (>13.0 để kiểm tra cảnh báo rung/nháy đỏ).

---

## Nếu build lỗi

Gửi lại cho tôi phần log lỗi, đặc biệt các dòng có `e: file://...`. Một số điểm dễ va chạm nếu nhánh của bạn đã đi xa hơn `713be5a`:

- `ImageUtils.NormalizedRect` đã được **chuyển ra top-level** thành `com.example.nhatkyduonghuyet.ml.NormalizedRect` (trong `ScanRoiGeometry.kt`). Nếu code khác của bạn còn tham chiếu `ImageUtils.NormalizedRect`, đổi thành `NormalizedRect`.
- `GlucoseScanner.processHybrid(...)` có thêm tham số thứ 3 `roi: NormalizedRect` (đã có giá trị mặc định nên các lời gọi cũ vẫn biên dịch được nếu dùng named argument cho `onResult`/`onError`).
- `CameraScannerDialog` và `ScannerScreen` không còn tự quản lý CameraX; nếu bạn có bản chỉnh riêng ở 2 file này thì cần merge thủ công.
