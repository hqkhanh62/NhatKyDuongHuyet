# zip_project.ps1
# Script đóng gói project Android NhatKyDuongHuyet
# Bỏ qua: build, .git, .gradle, .github, .idea

$projectName = "NhatKyDuongHuyet"
$desktop = [Environment]::GetFolderPath("Desktop")
$source = Get-Location
$tempFolder = "$env:TEMP\$projectName`_Clean"
$zipOutput = "$desktop\$projectName.zip"

Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  📦 ĐÓNG GÓI PROJECT: $projectName" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan

# Danh sách thư mục/file cần bỏ qua
$excludeList = @("build", ".git", ".gradle", ".github", ".idea", "captures", "externalNativeBuild", "cxx", "*.iml", "local.properties")

Write-Host ""
Write-Host "🗑️  Các thư mục sẽ bỏ qua:" -ForegroundColor Yellow
foreach ($item in $excludeList) {
    Write-Host "   ❌ $item" -ForegroundColor DarkGray
}

# Xóa thư mục tạm cũ nếu có
if (Test-Path $tempFolder) {
    Write-Host ""
    Write-Host "🧹 Đang dọn dẹp thư mục tạm cũ..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force $tempFolder
}

# Xóa file zip cũ nếu có
if (Test-Path $zipOutput) {
    Write-Host "🗑️  Đang xóa file zip cũ..." -ForegroundColor Yellow
    Remove-Item -Force $zipOutput
}

# Copy toàn bộ project sang thư mục tạm, bỏ qua các thư mục không cần
Write-Host ""
Write-Host "📂 Đang sao chép file (bỏ qua thư mục không cần thiết)..." -ForegroundColor Green

$robocopyArgs = @(
    "$source",
    "$tempFolder",
    "/E",                           # Copy toàn bộ thư mục con
    "/XD", "build", ".git", ".gradle", ".github", ".idea", "captures", "externalNativeBuild", "cxx",
    "/XF", "*.iml", "local.properties",
    "/R:3",                         # Retry 3 lần nếu lỗi
    "/W:1",                         # Chờ 1 giây giữa các retry
    "/MT:8",                        # Multi-threading (8 luồng)
    "/NP",                          # Không hiển thị % progress
    "/NFL",                         # Không liệt kê file
    "/NDL"                          # Không liệt kê thư mục
)

$robocopyResult = Start-Process -FilePath "robocopy" -ArgumentList $robocopyArgs -Wait -PassThru -NoNewWindow

if ($robocopyResult.ExitCode -ge 8) {
    Write-Host ""
    Write-Host "❌ Lỗi khi sao chép file! (Exit code: $($robocopyResult.ExitCode))" -ForegroundColor Red
    exit 1
}

# Nén thư mục tạm thành file zip
Write-Host ""
Write-Host "🗜️  Đang nén thành file .zip..." -ForegroundColor Green
Compress-Archive -Path "$tempFolder\*" -DestinationPath $zipOutput -Force

# Xóa thư mục tạm
Write-Host "🧹 Đang dọn dẹp..." -ForegroundColor Yellow
Remove-Item -Recurse -Force $tempFolder

# Hiển thị kết quả
$zipSize = (Get-Item $zipOutput).Length
$zipSizeMB = [math]::Round($zipSize / 1MB, 2)

Write-Host ""
Write-Host "═══════════════════════════════════════" -ForegroundColor Green
Write-Host "  ✅ ĐÓNG GÓI THÀNH CÔNG!" -ForegroundColor Green
Write-Host "═══════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "📁 File zip: $zipOutput" -ForegroundColor Cyan
Write-Host "📊 Dung lượng: $zipSizeMB MB" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 Để giải nén trên PC khác, chạy:" -ForegroundColor DarkGray
Write-Host "   Expand-Archive -Path '$zipOutput' -DestinationPath 'C:\DuongDan\Moi'" -ForegroundColor DarkGray
Write-Host ""

# Mở thư mục Desktop để dễ tìm file
Start-Process "explorer.exe" -ArgumentList "/select,$zipOutput"
