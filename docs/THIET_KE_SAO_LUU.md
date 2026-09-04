# Thiết kế sao lưu dữ liệu

Tài liệu này mô tả cách app giữ an toàn dữ liệu người dùng, và **vì sao** lại
thiết kế như vậy.

## Ràng buộc xuất phát

App cố tình giữ `android:allowBackup="false"` và loại database khỏi
`res/xml/data_extraction_rules.xml`. Đây là lựa chọn có chủ đích: dữ liệu sức
khoẻ không được đưa lên đám mây.

Hệ quả phải chấp nhận: **gỡ app là mất sạch dữ liệu**, không có cách nào cứu
bằng kỹ thuật. Vì vậy mọi thứ dưới đây xoay quanh một mục tiêu — làm cho việc
người dùng tự xuất file ra ngoài trở nên dễ và khó quên.

## Ba nguyên nhân mất dữ liệu, ba lớp bảo vệ

| Nguyên nhân | Lớp bảo vệ | Tự động? |
|---|---|---|
| Nâng cấp app làm hỏng schema | Room migration + bản sao theo phiên bản | Có |
| App tự xoá/ghi đè dữ liệu do lỗi | Bản sao cuộn trong `filesDir` | Có |
| Gỡ app, đổi máy, mất máy | Người dùng xuất file CSV ra ngoài | **Không** |

Lớp 1 và 2 nằm hoàn toàn trong bộ nhớ riêng của app, nên chúng **cũng chết theo
khi gỡ app**. Chỉ lớp 3 sống sót. Tài liệu và giao diện đều phải nói thẳng điều
này thay vì để người dùng tưởng mình đã an toàn.

## Những gì sai trong thiết kế cũ

1. **Sao lưu tự động bỏ sót nhật ký đường huyết.** `MedicationBackupRepository`
   chỉ đọc `MedicationDao`. Bảng `log_entries` — dữ liệu cốt lõi của app —
   chưa từng được snapshot lần nào. Đây là lỗ hổng nghiêm trọng nhất.
2. **Ba đường xuất/nhập rời rạc** nằm ở hai màn hình khác nhau, mỗi đường có
   định dạng và cách báo lỗi riêng.
3. **Không có chỗ nào cho người dùng biết dữ liệu có đang an toàn hay không**,
   dù đây mới là thông tin quan trọng nhất khi app không sao lưu đám mây.
4. **Khôi phục không idempotent**: nhập lại cùng một file sinh ra bản trùng.

## Thiết kế mới — `data/backup/`

### `BackupSnapshot` — gom mọi bảng vào một giá trị

Toàn bộ dữ liệu app sở hữu nằm trong **một** data class. Đây là điểm mấu chốt
sửa lỗi số 1: không thể thêm bảng mới mà quên sao lưu, vì mọi thứ đi qua cùng
một kiểu dữ liệu.

### `BackupPart` + `BackupCsv` — một chỗ duy nhất biết về định dạng

`BackupPart` liệt kê ba tập dữ liệu; `BackupCsv` định tuyến sang các bộ mã hoá
sẵn có (`LogEntryCsv`, `MedicationCsv`). Thêm một bảng = thêm một enum và một
nhánh; giao diện và bộ lập lịch sao lưu không đổi.

`BackupCsv.detectPart()` nhận dạng file theo **header**, có dự phòng theo tên
file. Nhờ đó nút "Khôi phục" chỉ cần một cái: người dùng chọn file, app tự biết
đó là nhật ký hay đơn thuốc — kể cả khi file đã bị đổi tên.

### `BackupStorage` — chuyện đĩa

- Ghi **nguyên tử** (ghi file tạm rồi đổi tên), để tiến trình bị kill giữa chừng
  không để lại file cụt đè lên bản sao lưu tốt.
- **Bản cuộn**: một file mỗi tập dữ liệu, luôn mới nhất.
- **Bản theo phiên bản**: một bản có ngày tháng cho mỗi phiên bản app, giữ ~10
  phiên bản gần nhất rồi tự dọn.

### `BackupRepository` — điều phối

- `startAutoBackup()` gộp Flow của cả ba bảng, debounce 1,5 giây rồi ghi bản
  cuộn. Tick nhiều ô liên tiếp chỉ ghi một lần.
- `backupIfAppUpdated()` chụp một bản có ngày tháng **trước khi** code mới chạm
  vào database. Room migration hiện đã giữ được dữ liệu, nhưng đây là bảo hiểm
  rẻ tiền cho trường hợp một migration tương lai ship ra bị lỗi.
- `daysSinceExport` đếm số ngày kể từ lần xuất file gần nhất — nguồn dữ liệu cho
  cảnh báo trên giao diện.

### Khôi phục: cộng thêm và idempotent

Đây là quyết định thiết kế quan trọng nhất của phần khôi phục:

- **Không bao giờ xoá.** Khôi phục chỉ thêm và cập nhật, không đụng vào dữ liệu
  người dùng nhập sau khi bản sao lưu được tạo.
- **Đối chiếu theo khoá tự nhiên**, không theo `id`. Nhật ký khớp theo
  `(date, session)`, thuốc khớp theo `name`. Sau khi database bị xoá, các `id`
  tự tăng trong file sao lưu là vô nghĩa — dùng lại chúng sẽ sinh bản trùng hoặc
  ghi đè nhầm. Riêng lịch sử uống thuốc còn phải ánh xạ lại `medicationId` theo
  tên thuốc.
- **Báo cáo rõ ràng.** `RestoreReport` nói chính xác đã thêm/cập nhật bao nhiêu
  dòng và bỏ qua bao nhiêu dòng lỗi. Nhập vào 0 dòng được báo là *thất bại*, chứ
  không phải "xong" — vì hai trường hợp đó với người dùng là khác nhau hoàn toàn.

### `BackupScreen` — một màn hình duy nhất

Gom mọi thao tác vào một chỗ, và quan trọng hơn là hiển thị **tình trạng**: số
dòng của từng tập dữ liệu, lần sao lưu gần nhất, và một thẻ cảnh báo màu đỏ khi
đã quá 30 ngày (hoặc chưa bao giờ) chưa xuất file ra ngoài.

Cảnh báo này chính là câu trả lời cho ràng buộc ban đầu: đã chọn không sao lưu
đám mây thì phải bù lại bằng cách nhắc người dùng đúng lúc.

## Những gì đã gỡ bỏ

- `util/MedicationBackupManager.kt`
- `data/repository/MedicationBackupRepository.kt`
- `util/CsvExportHelper.kt`

Ba file này bị thay thế hoàn toàn. `LogEntryCsv` và `MedicationCsv` được giữ
nguyên vì đã có unit test và chỉ làm đúng một việc là mã hoá/giải mã.

## Kiểm thử

- `BackupCsvTest` (7 test): round-trip cả ba tập dữ liệu, nhận dạng file theo
  header kể cả khi tên file gây nhiễu, dự phòng theo tên file, snapshot rỗng.
  Có test hồi quy khẳng định huyết áp và nhịp tim không bị mất.
- `RestoreReportTest` (5 test): khôi phục 0 dòng không được coi là thành công.

Toàn bộ là JVM unit test thuần, chạy trong CI. Sandbox không có JDK nên việc
xác minh đi qua GitHub Actions.

> Ghi chú CI: workflow nay in lỗi biên dịch Kotlin ra dạng GitHub annotation.
> Log thô của Actions không tải về được từ môi trường phát triển, nên đây là
> cách duy nhất đọc được lỗi biên dịch.

## Hướng dẫn cho người dùng

1. Mở **Sao lưu & Khôi phục** từ màn hình nhật ký hoặc màn hình thuốc.
2. Xuất cả ba file, lưu vào Google Drive hoặc gửi cho chính mình qua email.
3. Làm lại mỗi tháng — app sẽ hiện cảnh báo đỏ khi đến hạn.
4. **Bắt buộc xuất file trước khi gỡ app hoặc đổi máy.**
5. Trên máy mới: cài app, mở màn hình trên, chọn "Khôi phục" và lần lượt chọn
   từng file.
