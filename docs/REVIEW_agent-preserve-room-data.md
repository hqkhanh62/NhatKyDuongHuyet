# Review nhánh `agent/preserve-room-data`

**Kết luận: KHÔNG merge nhánh này.** Ý tưởng đúng, nhưng nhánh mang theo quá nhiều thiệt hại phụ. Tôi đã lấy phần cốt lõi và làm lại đúng cách trên nhánh arena.

---

## 1. Nhánh này không có tổ tiên chung với nhánh hiện tại

```
$ git merge-base arena/01a0673e-nhatkyduonghuyet agent/preserve-room-data
(rỗng — exit code 1)

root của arena :  713be5a  (2026-09-02)
root của agent :  b92c94f  (Initial commit)
```

Đây là **hai lịch sử hoàn toàn tách rời** (unrelated histories). Git không thể merge bình thường; muốn merge phải dùng `--allow-unrelated-histories`, và khi đó mọi file sẽ xung đột hoặc bị ghi đè một cách mù quáng.

## 2. Nội dung nhánh cũ hơn nhánh hiện tại ~1 tháng

| | commit cuối | ngày |
|---|---|---|
| `arena/01a0673e-…` | `7bbc6fa` | 2026-09-04 |
| `agent/preserve-room-data` | `60ab619` | **2026-08-25** |

Commit "Preserve Room data during migrations" (`333655b`) thậm chí từ **2026-08-01**. Nhánh này là ảnh chụp của một giai đoạn cũ của dự án.

## 3. Merge sẽ xoá 37 file mã nguồn và 3 file test

Những thứ sẽ **biến mất** nếu merge:

| Tính năng | File bị xoá |
|---|---|
| Toàn bộ quản lý thuốc | `Medication.kt`, `MedicationLog.kt`, `MedicationDao.kt`, `MedicationRepository.kt`, `MedicationScreen.kt`, `MedicationViewModel.kt` |
| Sao lưu CSV (làm ở lượt trước) | `MedicationCsv.kt`, `MedicationBackupManager.kt`, `MedicationBackupRepository.kt` |
| Cải thiện OCR (làm ở lượt trước) | `ScanRoiGeometry.kt`, `GlucoseCameraPreview.kt`, `PixelGlucoseReader.kt`, `ImageUtils.kt`, `GlucoseConstants.kt` |
| Khác | `PrivacyPolicy.kt`, `GlucosePolicy.kt`, `GeminiBackendClient.kt`, `AIModule.kt`, `RepositoryModule.kt`, `SessionEntryCard.kt`, `SmartInputTextField.kt`, widget provider… |
| Test | `GlucoseScannerTest.kt`, `ScanRoiGeometryTest.kt`, `MedicationCsvTest.kt` |

Trớ trêu: nhánh tên là "preserve data" nhưng lại **xoá sạch tính năng quản lý thuốc** — tức là xoá luôn thứ mà dữ liệu cần được bảo toàn.

## 4. Nhánh chứa 1988 file rác đã commit nhầm

```
app/build/**          (mã Java sinh bởi Hilt/Room, file .class, kapt cache)
.idea/**              (cấu hình IDE cá nhân)
NhatKyDuongHuyet-V2.2.apk   (8.5 MB binary)
```

Không được đưa những thứ này vào Git.

## 5. Bản thân bản sửa thì… đúng, nhưng chưa đủ

Commit `333655b` chỉ có 2 thay đổi nhỏ:

```diff
- .fallbackToDestructiveMigration()   // AppDatabase.kt
- .fallbackToDestructiveMigration()   // AppModule.kt
- context,
+ context.applicationContext,
```

Chẩn đoán **đúng** (`fallbackToDestructiveMigration()` chính là thủ phạm), nhưng cách sửa **nguy hiểm**: bỏ dòng đó mà **không thêm `Migration` nào** thì lần nâng version schema tiếp theo app sẽ **crash ngay khi mở** với:

```
IllegalStateException: A migration from 4 to 5 was required but not found.
```

Đổi từ "mất dữ liệu" thành "không mở được app" — chưa phải là sửa xong.

> Ghi chú: phần `context` → `context.applicationContext` đã có sẵn trên nhánh arena rồi.

---

## Cách đã làm thay thế

Commit `d0e5d0f` trên nhánh arena, giữ nguyên toàn bộ tính năng hiện có:

### `DatabaseMigrations.kt` (mới)
Migration **phòng thủ và idempotent**: vì `exportSchema = false` từ trước nên ta không biết chính xác schema v1/v2/v3 trên máy từng người dùng. Thay vì đoán DDL, migration tự soi database đang chạy và chỉ tạo/thêm thứ còn thiếu:

- `CREATE TABLE IF NOT EXISTS` cho cả 3 bảng + unique index
- `PRAGMA table_info` để tìm cột thiếu, rồi `ALTER TABLE ADD COLUMN`
- Cột `NOT NULL` luôn kèm `DEFAULT` (SQLite từ chối `ADD COLUMN NOT NULL` không default khi bảng đã có dữ liệu)
- Phủ **mọi đường nâng cấp**: 1→2, 1→3, 1→4, 2→3, 2→4, 3→4 — người dùng bỏ qua vài bản cập nhật vẫn giữ được dữ liệu

### `AppModule.kt`
```diff
- .fallbackToDestructiveMigration()
+ .addMigrations(*DatabaseMigrations.ALL)
+ .fallbackToDestructiveMigrationOnDowngrade()
```
Chỉ **hạ cấp** (cài APK cũ đè lên bản mới) mới dựng lại DB. Nâng cấp thì không bao giờ mất dữ liệu.

### `DatabaseMigrationsTest.kt` (mới, 8 test)
Instrumented test không chạy trong CI, nên tôi tách SQL ra thành hàm thuần để test được trên JVM:
- phủ đủ mọi cặp phiên bản
- không có `DROP TABLE` / `DELETE FROM` / `TRUNCATE`
- mọi lệnh `CREATE` đều có `IF NOT EXISTS`
- bảng đã đúng schema → không sinh `ALTER` nào
- bảng cũ thiếu cột → chỉ thêm đúng cột thiếu
- cột `NOT NULL` luôn có `DEFAULT`

Tôi cũng chạy thử SQL này trên SQLite thật (mô phỏng DB v1 có 2 dòng dữ liệu): sau migration dữ liệu còn nguyên, chạy lần 2 không sinh thêm thay đổi nào.

---

## Vẫn còn hạn chế — cần bạn quyết định

### `exportSchema` chưa bật được
Tôi đã thử bật (2 cách: `kapt arguments` và `javaCompileOptions`) nhưng **cả hai đều làm build fail** trên CI. Đã revert để giữ nhánh xanh. Migration hiện tại không phụ thuộc schema JSON nên vẫn an toàn, nhưng bật được thì tốt hơn cho tương lai. Cần điều tra thêm — có thể do xung đột kapt/AGP version.

### Cài lại app (gỡ rồi cài) thì Room migration KHÔNG cứu được
Tên nhánh nhắc tới "cài lại app", nhưng cần nói rõ: **gỡ app là xoá toàn bộ `/data/data/<package>/`**, không migration nào cứu được. Hiện `AndroidManifest.xml` đang có:

```xml
android:allowBackup="false"
```
và `data_extraction_rules.xml` loại trừ luôn database khỏi cloud backup.

**QUYẾT ĐỊNH CỦA CHỦ DỰ ÁN: giữ nguyên `allowBackup="false"`** (phương án 1) — ưu tiên quyền riêng tư, dữ liệu sức khoẻ không lên cloud. Manifest và `data_extraction_rules.xml` **không thay đổi**.

Hệ quả: **xuất CSV là con đường DUY NHẤT** để mang dữ liệu qua lần cài lại app. Xem mục "Hệ quả của phương án 1" bên dưới.

---

## Đề xuất xử lý nhánh cũ

Nhánh `agent/preserve-room-data` nên **xoá hoặc bỏ qua**. Nếu muốn giữ làm tham khảo thì đừng merge — nội dung của nó đã lỗi thời và phần giá trị (chẩn đoán `fallbackToDestructiveMigration`) đã được tiếp thu và làm kỹ hơn trên nhánh arena.

---

# Hệ quả của phương án 1 (giữ `allowBackup="false"`)

Vì đã chốt không dùng cloud backup, CSV trở thành **đường cứu hộ duy nhất**. Điều đó có nghĩa file CSV phải **không mất mát dữ liệu**. Khi rà lại, tôi phát hiện một lỗ hổng nghiêm trọng và đã vá.

## Lỗi phát hiện: CSV nhật ký âm thầm bỏ mất 3 trường

`LogEntry` có 12 trường dữ liệu, nhưng `CsvExportHelper` cũ chỉ ghi ra 8:

| Trường | Có trong CSV cũ? |
|---|---|
| date, session, medType, dose, time, bgBefore, bgAfter, note | ✅ |
| **bpSys** (huyết áp tâm thu) | ❌ **mất** |
| **bpDia** (huyết áp tâm trương) | ❌ **mất** |
| **heartRate** (nhịp tim) | ❌ **mất** |

Đáng chú ý: bản xuất **PDF** vẫn in huyết áp (`PdfExportHelper.kt:179`), nên đây rõ ràng là thiếu sót của CSV chứ không phải chủ ý.

Với phương án 1, lỗi này nghĩa là: người dùng xuất CSV cẩn thận trước khi gỡ app, cài lại, nhập CSV vào — và **toàn bộ lịch sử huyết áp + nhịp tim biến mất vĩnh viễn** mà không có cảnh báo nào.

## Đã sửa

**`LogEntryCsv.kt` (mới)** — tách toàn bộ logic CSV ra khỏi Android để test được trên JVM:
- Xuất đủ **11 cột**, thêm `Huyet ap tam thu`, `Huyet ap tam truong`, `Nhip tim` ở cuối
- Thêm **BOM UTF-8** (trước đây không có → Excel trên Windows hiện sai tiếng Việt)
- Xuống dòng trong ghi chú được làm phẳng thành `;` (trước đây làm vỡ file khi đọc lại)
- Chấp nhận số thập phân kiểu Việt `6,1` lẫn `6.1`
- Dòng hỏng bị bỏ qua thay vì làm hỏng cả lần nhập

**Tương thích ngược:** file CSV 8 cột do bản cũ xuất ra **vẫn nhập được bình thường**; 3 cột thiếu trả về `null` (không phải `0`).

**`CsvExportHelper.kt`** — giữ nguyên API công khai (2 call site trong `DateListScreen` không phải sửa), chỉ còn là lớp mỏng xử lý URI. Bỏ `printStackTrace()` thay bằng `Log.e`.

**`LogEntryCsvTest.kt` (mới, 11 test)** — trong đó có test hồi quy riêng cho đúng lỗi này (`blood pressure and heart rate survive a round trip`), test đọc file định dạng cũ, test dấu phẩy trong ghi chú, test `null` không bị biến thành `0`.

## Khuyến nghị vận hành cho phương án 1

Vì không có lưới an toàn tự động, nên:

1. **Trước khi gỡ/cài lại app**: vào màn hình nhật ký → menu ⋮ → xuất CSV; vào màn hình thuốc → menu ⋮ → xuất cả đơn thuốc và lịch sử uống thuốc. Lưu ra Google Drive hoặc gửi cho chính mình qua Zalo/email.
2. **Định kỳ** (ví dụ mỗi tháng): làm như trên. Bản sao lưu tự động trong `filesDir` chỉ chống được mất dữ liệu khi *nâng cấp*, không chống được *gỡ cài đặt*.
3. Nâng cấp app qua APK mới (không gỡ app) thì **an toàn** — migration đã lo, không cần thao tác gì.

## Việc còn dang dở

- `exportSchema` vẫn chưa bật được (làm fail build, đã revert). Không ảnh hưởng an toàn dữ liệu hiện tại.
- Chưa có nhắc nhở tự động "đã lâu chưa sao lưu". Nếu bạn muốn, tôi có thể thêm banner nhắc khi quá N ngày chưa xuất CSV — với phương án 1 thì tính năng này khá đáng giá.
