# CI/CD GitHub Actions cho Android APK

Tài liệu này hướng dẫn tự động hóa quy trình:

1. Checkout source từ GitHub.

1. Cấu hình JDK 17.

1. Cài Android SDK và các package cần thiết.

1. Chạy unit test liên quan đến OCR.

1. Biên dịch instrumentation test.

1. Build APK debug.

1. Lưu APK và log Gradle thành GitHub Actions artifacts.

Quy trình này dùng được cho branch `fix/camera-ocr-full-height` và có thể mở rộng cho `main`.

---

## 1. Điều kiện cần

Repository cần có:

```
NhatKyDuongHuyet/
├── app/
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle
├── build.gradle
└── app/build.gradle
```

Không commit các file sau lên GitHub:

```
local.properties
*.jks
*.keystore
keystore.properties
```

`local.properties` chỉ dành cho máy local. GitHub Actions sẽ tự tạo cấu hình Android SDK thông qua action `android-actions/setup-android`.

---

## 2. Tạo workflow

Tạo file:

```
.github/workflows/android-apk.yml
```

Nội dung mẫu:

```yaml
name: Android OCR APK CI/CD

on:
  push:
    branches:
      - main
      - fix/camera-ocr-full-height
  pull_request:
    branches:
      - main
  workflow_dispatch:
    inputs:
      build_type:
        description: "Loại build APK"
        required: true
        default: "Debug"
        type: choice
        options:
          - Debug
          - Release

permissions:
  contents: read

concurrency:
  group: android-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

env:
  JAVA_VERSION: "17"
  ANDROID_COMPILE_SDK: "36"
  ANDROID_BUILD_TOOLS: "35.0.0"
  ANDROID_MIN_SDK: "26"

jobs:
  test-and-build:
    name: Test OCR and build APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout source
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ env.JAVA_VERSION }}
          cache: gradle

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Android SDK packages
        run: |
          yes | sdkmanager --licenses >/dev/null || true
          sdkmanager \
            "platform-tools" \
            "platforms;android-${ANDROID_COMPILE_SDK}" \
            "build-tools;${ANDROID_BUILD_TOOLS}"

      - name: Make Gradle executable
        run: chmod +x ./gradlew

      - name: Validate Gradle wrapper
        run: ./gradlew --version

      - name: Run OCR and scanner unit tests
        run: |
          ./gradlew :app:testDebugUnitTest --no-daemon \\
            --tests "com.example.nhatkyduonghuyet.ml.GlucoseScannerTest" \\
            --tests "com.example.nhatkyduonghuyet.ml.MeterTextParserTest" \\
            --tests "com.example.nhatkyduonghuyet.ml.ScanRoiGeometryTest" \\
            --tests "com.example.nhatkyduonghuyet.ml.SevenSegmentDecoderTest" \\
            --tests "com.example.nhatkyduonghuyet.domain.scanner.AutoImportPipelineTest" \\
            --tests "com.example.nhatkyduonghuyet.domain.scanner.FrameAccumulatorTest" \\
            --stacktrace

      - name: Upload unit test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: ocr-unit-test-reports-${{ github.run_number }}
          path: |
            app/build/reports/tests/
            app/build/test-results/
          if-no-files-found: warn
          retention-days: 14

      - name: Compile instrumentation tests
        run: ./gradlew :app:compileDebugAndroidTestKotlin --no-daemon --stacktrace

      - name: Select build type
        id: build-type
        shell: bash
        run: |
          if [[ "${{ github.event_name }}" == "workflow_dispatch" ]]; then
            echo "value=${{ inputs.build_type }}" >> "$GITHUB_OUTPUT"
          else
            echo "value=Debug" >> "$GITHUB_OUTPUT"
          fi

      - name: Build debug APK
        if: steps.build-type.outputs.value == 'Debug'
        run: ./gradlew :app:assembleDebug --no-daemon --stacktrace

      # Release chỉ nên bật sau khi đã cấu hình signing secrets.
      - name: Build release APK
        if: steps.build-type.outputs.value == 'Release'
        run: ./gradlew :app:assembleRelease --no-daemon --stacktrace

      - name: Rename APK with commit and run number
        shell: bash
        run: |
          BUILD_TYPE=$(echo "${{ steps.build-type.outputs.value }}" | tr '[:upper:]' '[:lower:]')
          APK="app/build/outputs/apk/${BUILD_TYPE}/app-${BUILD_TYPE}.apk"
          if [[ ! -f "$APK" ]]; then
            echo "APK not found: $APK"
            exit 1
          fi
          cp "$APK" "app/build/outputs/apk/${BUILD_TYPE}/NhatKyDuongHuyet-${BUILD_TYPE}-${GITHUB_SHA::7}-run-${GITHUB_RUN_NUMBER}.apk"

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: NhatKyDuongHuyet-apk-${{ github.sha }}
          path: app/build/outputs/apk/**/*.apk
          if-no-files-found: error
          retention-days: 30

      - name: Upload Gradle reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: gradle-reports-${{ github.run_number }}
          path: |
            app/build/reports/
            app/build/outputs/logs/
          if-no-files-found: warn
          retention-days: 14
```

> Nếu project dùng `compileSdk` khác 36 hoặc `buildToolsVersion` khác, cập nhật các biến ở phần `env` cho khớp `app/build.gradle`.

### Build phiên bản Widget có lịch đo tùy chỉnh

Workflow trên build toàn bộ module `app`, vì vậy các thay đổi trong widget cũng
được đóng gói tự động vào APK. Phiên bản widget hiện hỗ trợ:

- Chu kỳ mặc định 4 ngày.

- Người dùng chọn chu kỳ từ 1 đến 30 ngày trong **Cài đặt Widget**.

- Khi lưu chu kỳ mới, mốc lịch được tính lại từ ngày hiện tại.

- Widget hiển thị ngày đo kế tiếp và alarm thông báo dùng cùng một cấu hình.

Workflow `build-apk.yml` đã có bước kiểm tra nhanh trước `Build with Gradle`
để CI dừng nếu branch đang build thiếu các thành phần widget mới:

```yaml
      - name: Verify configurable widget schedule
        shell: bash
        run: |
          set -e
          grep -q "DEFAULT_INTERVAL_DAYS = 4" \
            app/src/main/java/com/example/nhatkyduonghuyet/reminder/GlucoseMeasurementSchedule.kt
          grep -q "MIN_INTERVAL_DAYS = 1" \
            app/src/main/java/com/example/nhatkyduonghuyet/reminder/GlucoseMeasurementSchedule.kt
          grep -q "MAX_INTERVAL_DAYS = 30" \
            app/src/main/java/com/example/nhatkyduonghuyet/reminder/GlucoseMeasurementSchedule.kt
          test -f app/src/main/res/layout/glucose_widget.xml
          grep -q "widget_reminder" app/src/main/res/layout/glucose_widget.xml
          grep -q "scheduleEveryFourDaysReminder" \
            app/src/main/java/com/example/nhatkyduonghuyet/reminder/ReminderScheduler.kt
```

> Bước trên là kiểm tra cấu trúc bổ sung; bước `assembleDebug` vẫn là kiểm tra
chính thức vì nó biên dịch Kotlin, resource XML và đóng gói widget vào APK.

---

## 3. Commit workflow

Trên Windows PowerShell:

```
cd G:\NhatKyDuongHuyet_PRO_MAX_FINAL

New-Item -ItemType Directory -Force .github\workflows
notepad .github\workflows\android-apk.yml
```

Sau khi dán nội dung workflow:

```
git add .github/workflows/android-apk.yml
git commit -m "ci: automate OCR tests and APK build"
git push origin fix/camera-ocr-full-height
```

Nếu muốn workflow chạy sau khi merge vào `main`:

```
git switch main
git merge fix/camera-ocr-full-height
git push origin main
```

---

## 4. Kích hoạt workflow

Workflow chạy tự động trong các trường hợp:

- Push vào `main`.

- Push vào `fix/camera-ocr-full-height`.

- Tạo hoặc cập nhật Pull Request vào `main`.

- Chạy thủ công bằng `workflow_dispatch`.

Chạy thủ công trên GitHub:

1. Mở repository trên GitHub.

1. Chọn tab **Actions**.

1. Chọn workflow **Android OCR APK CI/CD**.

1. Chọn **Run workflow**.

1. Chọn branch.

1. Chọn `Debug` hoặc `Release`.

1. Bấm **Run workflow**.

---

## 5. Lấy APK sau khi build

Sau khi workflow hoàn tất:

1. Mở trang **Actions**.

1. Chọn workflow run tương ứng.

1. Kéo xuống phần **Artifacts**.

1. Tải artifact:

```
NhatKyDuongHuyet-apk-<commit-sha>
```

APK debug thường có hai file:

```
app-debug.apk
NhatKyDuongHuyet-debug-<commit>-run-<number>.apk
```

---

## 6. Cấu hình Release APK có ký số

Không nên build release có signing key trong file source. Dùng GitHub Secrets.

### 6.1. Tạo Base64 keystore

Trên Windows PowerShell:

```
$bytes = [System.IO.File]::ReadAllBytes("G:\keys\nhatky-release.jks")
[Convert]::ToBase64String($bytes) | Set-Clipboard
```

### 6.2. Tạo Secrets trên GitHub

Vào:

```
Repository → Settings → Secrets and variables → Actions → New repository secret
```

Tạo các secret:

```
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

### 6.3. Tạo keystore trong runner

Thêm step trước `Build release APK`:

```yaml
      - name: Decode release keystore
        if: steps.build-type.outputs.value == 'Release'
        env:
          ANDROID_KEYSTORE_BASE64: ${{ secrets.ANDROID_KEYSTORE_BASE64 }}
        run: |
          echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$RUNNER_TEMP/release.jks"
```

Trong `app/build.gradle`, dùng biến môi trường hoặc `project.findProperty` thay vì hard-code password:

```
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("ANDROID_KEYSTORE_FILE") ?: "release.jks")
            storePassword System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias System.getenv("ANDROID_KEY_ALIAS")
            keyPassword System.getenv("ANDROID_KEY_PASSWORD")
        }
    }
}
```

Thêm environment vào step build:

```yaml
      - name: Build signed release APK
        if: steps.build-type.outputs.value == 'Release'
        env:
          ANDROID_KEYSTORE_FILE: ${{ runner.temp }}/release.jks
          ANDROID_KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
          ANDROID_KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
        run: ./gradlew :app:assembleRelease --no-daemon --stacktrace
```

Không in các biến secret ra log và không commit keystore vào repository.

---

## 7. Kiểm tra chất lượng trước khi tạo APK

Có thể thêm các bước sau trước bước build:

```yaml
      - name: Check formatting and whitespace
        run: |
          git diff --check
          ./gradlew :app:lintDebug --no-daemon --stacktrace
```

Nếu project có static analysis:

```yaml
      - name: Run static analysis
        run: ./gradlew :app:detekt --no-daemon --stacktrace
```

Nếu task chưa tồn tại, không thêm step tương ứng cho đến khi cấu hình plugin.

---

## 8. Instrumentation/UI test

Workflow trên chỉ biên dịch instrumentation test vì GitHub-hosted runner chưa tự có emulator.

Để chạy UI test thật, thêm emulator action:

```yaml
      - name: Enable KVM
        run: |
          echo 'KERNEL=='$(uname -r)
          sudo apt-get update
          sudo apt-get install -y cpu-checker
          sudo kvm-ok || true

      - name: Run Android emulator tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 35
          target: google_apis
          arch: x86_64
          profile: Pixel_6
          script: ./gradlew :app:connectedDebugAndroidTest --no-daemon --stacktrace
```

Có thể giới hạn test UI bằng runner argument:

```yaml
script: >-
  ./gradlew :app:connectedDebugAndroidTest
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.nhatkyduonghuyet.ui.scanner.ScanOverlayUiTest
  --no-daemon
```

Không dùng `--tests` với `connectedDebugAndroidTest`; tùy chọn đó dành cho JVM test task.

---

## 9. Quy trình đề xuất cho branch OCR

```
Developer push
      ↓
GitHub Actions
      ↓
OCR unit tests
      ↓
Instrumentation compile
      ↓
assembleDebug
      ↓
Upload APK artifact
      ↓
Review / merge main
```

Các bước tối thiểu nên giữ bắt buộc:

- `GlucoseScannerTest`

- `MeterTextParserTest`

- `ScanRoiGeometryTest`

- `SevenSegmentDecoderTest`

- `AutoImportPipelineTest`

- `FrameAccumulatorTest`

- `GlucoseMeasurementSchedule` và widget reminder schedule

- `assembleDebug`

---

## 10. Troubleshooting

### SDK location not found

GitHub Actions không cần `local.properties` nếu có step setup Android SDK. Kiểm tra:

```yaml
- uses: android-actions/setup-android@v3
```

và package:

```yaml
sdkmanager "platforms;android-36" "build-tools;35.0.0"
```

### Gradle permission denied

Thêm:

```yaml
- run: chmod +x ./gradlew
```

### Java version không đúng

Kiểm tra:

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: "17"
```

### `--tests` không được hỗ trợ

Đúng:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example...GlucoseScannerTest"
```

Sai:

```bash
./gradlew :app:connectedDebugAndroidTest --tests "..."
```

Instrumentation dùng:

```bash
-Pandroid.testInstrumentationRunnerArguments.class=com.example...ScanOverlayUiTest
```

### Không thấy APK trong Artifacts

Kiểm tra:

```yaml
path: app/build/outputs/apk/**/*.apk
if-no-files-found: error
```

và đảm bảo bước build đã chạy thành công trước bước upload.

---

## 11. Checklist triển khai

- [ ] Đã tạo `.github/workflows/android-apk.yml`.

- [ ] `gradlew` có trong repository.

- [ ] Java 17 được cấu hình.

- [ ] Android SDK package khớp `compileSdk`.

- [ ] `local.properties` không được commit.

- [ ] OCR unit tests chạy thành công.

- [ ] `compileDebugAndroidTestKotlin` chạy thành công.

- [ ] `assembleDebug` chạy thành công.

- [ ] APK xuất hiện trong Artifacts.

- [ ] Signing secrets chỉ cấu hình khi cần Release.

- [ ] Keystore không nằm trong source hoặc artifact công khai.