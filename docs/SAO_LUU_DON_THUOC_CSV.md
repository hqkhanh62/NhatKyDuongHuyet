# Xuất & tự động sao lưu đơn thuốc / lịch sử uống thuốc ra CSV

## Vì sao cần

`AppModule.provideDatabase()` dùng `.fallbackToDestructiveMigration()`. Nghĩa là **mỗi lần bạn bump `version` của Room database rồi cập nhật app, toàn bộ bảng bị xoá sạch** — kể cả đơn thuốc và lịch sử uống thuốc. Tính năng này giữ một bản sao CSV nằm ngoài database nên dữ liệu sống sót qua các lần cập nhật.

## Cách hoạt động

### 1. Sao lưu tự động mỗi khi dữ liệu thay đổi
`MedicationBackupRepository.startAutoBackup()` lắng nghe cả 2 bảng (`medications`, `medication_logs`) và ghi lại snapshot sau mỗi thay đổi (debounce 1.5 giây để tick nhiều checkbox chỉ ghi 1 lần).

Ghi đè vào:
```
/data/data/<package>/files/medication_backups/don_thuoc_latest.csv
/data/data/<package>/files/medication_backups/lich_su_uong_thuoc_latest.csv
```

### 2. Sao lưu theo phiên bản mỗi khi cập nhật app
`NhatKyDuongHuyetApplication.onCreate()` gọi `backupIfAppUpdated(versionName)`. Lần chạy đầu tiên sau khi `versionName` đổi, app ghi thêm một bản có ngày giờ:
```
don_thuoc_v1.2.0_20260904-081530.csv
lich_su_uong_thuoc_v1.2.0_20260904-081530.csv
```
Giữ tối đa 20 file (≈10 phiên bản), cũ hơn sẽ tự xoá.

> Việc này chạy **trước** khi destructive migration kịp xoá dữ liệu, vì Room chỉ mở database khi có truy vấn đầu tiên — chính là truy vấn đọc dữ liệu để sao lưu.

### 3. Xuất thủ công ra nơi người dùng chọn
Màn hình **Nhắc nhở & Nhật ký thuốc** có:
- Nút tải xuống trên thanh tiêu đề → xuất nhanh đơn thuốc
- Menu ⋮ → `Xuất đơn thuốc (CSV)`, `Xuất lịch sử uống thuốc (CSV)`, `Nhập đơn thuốc (CSV)`, `Sao lưu ngay`, `Khôi phục từ bản sao lưu`

File được lưu qua SAF (Storage Access Framework) nên bạn chọn được Downloads, Google Drive… Tên file gợi ý có kèm ngày giờ: `don_thuoc_20260904_0815.csv`.

### 4. Khôi phục sau khi mất dữ liệu
Menu ⋮ → **Khôi phục từ bản sao lưu**. Logic:
- Chỉ nạp lại đơn thuốc khi bảng `medications` đang rỗng (không ghi đè dữ liệu bạn vừa nhập tay).
- Lịch sử được upsert; ràng buộc unique `(medicationId, date, session)` tự khử trùng lặp.
- `medicationId` cũ được ánh xạ lại theo **tên thuốc**, vì Room cấp id mới sau khi nạp lại.

## Định dạng file

Cả hai file đều có **BOM UTF-8** để Excel trên Windows hiển thị đúng tiếng Việt.

**`don_thuoc_*.csv`**
```
ID,Ten thuoc,Ham luong,Lieu dung,Thoi diem
1,Jardiance,25 mg,Sáng 1/2 v,07:00
2,"Insulin Mixtard, FlexPen",100 IU/mL,Trưa 6 đv;chiều 8 đv,
```

**`lich_su_uong_thuoc_*.csv`**
```
Ngay,Buoi,Gio uong,Ten thuoc,Ham luong,So luong,MedicationID,Timestamp
2026-09-04,Sang,07:12,Jardiance,25 mg,0.5,1,1700000000000
2026-09-04,Trua,12:05,"Insulin Mixtard, FlexPen",100 IU/mL,1,2,1700003600000
```

Quy tắc:
- Giá trị chứa `,` hoặc `"` được bọc trong ngoặc kép, `"` bên trong nhân đôi thành `""`.
- Xuống dòng trong ô bị làm phẳng thành `;` để mỗi bản ghi luôn nằm gọn trên 1 dòng.
- Cột `Buoi` ghi nhãn tiếng Việt (Sang/Trua/Chieu/Toi/Truoc khi ngu) nhưng khi nhập lại vẫn hiểu cả nhãn tiếng Việt lẫn mã gốc (`MORNING`…).
- Khi nhập, dòng hỏng bị bỏ qua chứ không làm hỏng cả file; file thiếu dòng tiêu đề vẫn đọc được.

## File đã thêm/sửa

Mới:
```
app/src/main/java/com/example/nhatkyduonghuyet/util/MedicationCsv.kt
app/src/main/java/com/example/nhatkyduonghuyet/util/MedicationBackupManager.kt
app/src/main/java/com/example/nhatkyduonghuyet/data/repository/MedicationBackupRepository.kt
app/src/test/java/com/example/nhatkyduonghuyet/util/MedicationCsvTest.kt
```
Sửa:
```
app/src/main/java/com/example/nhatkyduonghuyet/NhatKyDuongHuyetApplication.kt
app/src/main/java/com/example/nhatkyduonghuyet/data/local/dao/MedicationDao.kt
app/src/main/java/com/example/nhatkyduonghuyet/viewmodel/MedicationViewModel.kt
app/src/main/java/com/example/nhatkyduonghuyet/ui/screens/MedicationScreen.kt
app/src/main/res/values/strings.xml
app/src/main/res/values-en/strings.xml
```

Không thêm dependency mới → không cần sửa `build.gradle`.

## Chạy kiểm thử

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
`MedicationCsvTest` có 11 test bao phủ: round-trip đơn thuốc & lịch sử, escape dấu phẩy/ngoặc kép/xuống dòng, bảng rỗng, dòng hỏng, file thiếu header, nhãn buổi tiếng Việt.

## Kiểm tra trên máy thật

1. Mở màn hình thuốc, tick vài ô → chờ ~2 giây.
2. Menu ⋮ → **Xuất lịch sử uống thuốc (CSV)** → lưu vào Downloads → mở bằng Excel/Google Sheets, kiểm tra tiếng Việt không bị lỗi font.
3. Thử mô phỏng mất dữ liệu: gỡ app rồi cài lại (bản sao lưu trong `filesDir` sẽ mất theo — đây là lý do nên **xuất ra Downloads/Drive định kỳ**), hoặc tăng `version` trong `AppDatabase` rồi build lại để kiểm tra bản sao lưu theo phiên bản được tạo và **Khôi phục từ bản sao lưu** hoạt động.

> Lưu ý: bản sao lưu tự động nằm trong bộ nhớ riêng của app nên **bị xoá khi gỡ cài đặt**. Với dữ liệu quan trọng, hãy dùng “Xuất … (CSV)” để lưu ra Google Drive.
