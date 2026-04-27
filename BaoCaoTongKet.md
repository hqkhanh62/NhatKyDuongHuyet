# Báo cáo tổng kết dự án "Nhat ky Duong huyet & Insulin"

## 1. Các hạng mục đã được kiểm thử

Dự án đã được kiểm thử theo các phương pháp sau:

### 1.1. Unit Tests

Các unit test đã được triển khai để kiểm tra các thành phần sau:

*   **DAO methods**: Đảm bảo các thao tác thêm, sửa, xóa và truy vấn dữ liệu trong cơ sở dữ liệu Room hoạt động chính xác.
*   **Basic ViewModel logic for loading and saving entries**: Kiểm tra logic cơ bản của ViewModel trong việc tải và lưu trữ các mục nhật ký.

### 1.2. UI Tests

Các UI test cơ bản đã được viết để xác minh luồng người dùng chính và tương tác giao diện:

*   **Open main list and open day detail**: Kiểm tra khả năng điều hướng từ màn hình danh sách ngày sang màn hình chi tiết ngày.
*   **Edit fields for one session and verify persistence**: Kiểm tra khả năng chỉnh sửa các trường dữ liệu cho một buổi và xác minh dữ liệu được lưu trữ.
*   **Basic flow open notification navigate to today detail**: Kiểm tra luồng cơ bản khi mở thông báo và điều hướng đến màn hình chi tiết ngày hiện tại.
*   **Open charts and verify non-empty data when DB has entries**: Kiểm tra khả năng mở màn hình biểu đồ và xác minh dữ liệu hiển thị không trống khi có dữ liệu trong cơ sở dữ liệu.

### 1.3. Kiểm tra thủ công (Manual Checks)

Các kiểm tra thủ công sau đây được khuyến nghị để đảm bảo chất lượng ứng dụng:

*   **Create/edit entries for multiple days**: Thêm và chỉnh sửa các mục nhật ký cho nhiều ngày khác nhau.
*   **Change reminder times and verify notifications fire daily**: Thay đổi thời gian nhắc nhở và xác minh thông báo được kích hoạt hàng ngày.
*   **Export CSV and open file to verify columns and data**: Xuất dữ liệu ra file CSV và mở file để kiểm tra định dạng cột và nội dung dữ liệu.
*   **Visually verify daily and weekly line charts render without crash**: Kiểm tra trực quan biểu đồ đường huyết hàng ngày và hàng tuần hiển thị mà không gặp sự cố.

## 2. Các hạn chế hoặc vấn đề đã biết

*   **Biểu đồ**: Hiện tại, biểu đồ chỉ hiển thị dữ liệu giả định (placeholder data). Cần tích hợp dữ liệu thực tế từ ViewModel và tinh chỉnh hiển thị biểu đồ để phù hợp với yêu cầu cụ thể về trục X, trục Y và quy tắc tổng hợp.
*   **Notification**: Logic nhắc nhở đã được triển khai bằng WorkManager, nhưng cần kiểm tra kỹ lưỡng trên các thiết bị thực tế để đảm bảo hoạt động ổn định và đúng giờ, đặc biệt là sau khi khởi động lại thiết bị hoặc khi ứng dụng bị đóng.
*   **UI/UX**: Giao diện người dùng được xây dựng ở mức cơ bản. Cần cải thiện thêm về mặt thẩm mỹ và trải nghiệm người dùng để ứng dụng thân thiện hơn.
*   **Xử lý lỗi**: Việc xử lý lỗi trong các thao tác nhập liệu và xuất CSV còn đơn giản, cần bổ sung các thông báo lỗi chi tiết hơn cho người dùng.

## 3. Cách chạy kiểm thử

Để chạy các bài kiểm thử, bạn có thể sử dụng Android Studio hoặc dòng lệnh Gradle:

*   **Chạy Unit Tests**: Trong Android Studio, điều hướng đến thư mục `app/src/test/java/com/example/nhatkyduonghuyet/viewmodel`, mở file `LogEntryViewModelTest.kt` và nhấn vào biểu tượng chạy bên cạnh class hoặc từng hàm test. Hoặc chạy bằng Gradle:
    ```bash
    ./gradlew testDebugUnitTest
    ```

*   **Chạy UI Tests**: Trong Android Studio, điều hướng đến thư mục `app/src/androidTest/java/com/example/nhatkyduonghuyet`, mở file `AppUITest.kt` và nhấn vào biểu tượng chạy bên cạnh class hoặc từng hàm test. Hoặc chạy bằng Gradle:
    ```bash
    ./gradlew connectedDebugAndroidTest
    ```

*   **Kiểm tra thủ công**: Cài đặt ứng dụng lên thiết bị hoặc trình giả lập và thực hiện các bước kiểm tra được liệt kê trong mục 1.3.
