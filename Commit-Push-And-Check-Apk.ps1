[CmdletBinding()]
param(
    [Parameter()]
    [string] $ProjectDir = "G:\NhatKyDuongHuyet_PRO_MAX_FINAL",

    [Parameter()]
    [string] $Branch = "fix/camera-ocr-full-height",

    [Parameter()]
    [string] $Workflow = "build-apk.yml",

    [Parameter()]
    [string] $CommitMessage = "feat(widget): customize glucose measurement cycle",

    [Parameter()]
    [string] $ArtifactName = "NhatKyDuongHuyet-PRO-APKs",

    [Parameter()]
    [string] $ArtifactDir = ".\github-apk-artifacts",

    [Parameter()]
    [switch] $IncludeAllChanges,

    [Parameter()]
    [switch] $NoDownloadArtifact,

    [Parameter()]
    [switch] $NoWatch
)

$ErrorActionPreference = "Stop"

function Invoke-External {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Command,
        [Parameter(Mandatory = $true)]
        [string[]] $Arguments
    )

    Write-Host ("> $Command " + ($Arguments -join " ")) -ForegroundColor DarkCyan
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command thất bại với exit code $LASTEXITCODE"
    }
}

function Get-GhRunsForCommit {
    param(
        [Parameter(Mandatory = $true)]
        [string] $WorkflowName,
        [Parameter(Mandatory = $true)]
        [string] $BranchName,
        [Parameter(Mandatory = $true)]
        [string] $CommitSha
    )

    $json = & gh run list --workflow $WorkflowName --branch $BranchName --limit 20 --json databaseId,headSha,status,conclusion,workflowName,createdAt 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Không thể đọc danh sách GitHub Actions run."
    }

    if ([string]::IsNullOrWhiteSpace(($json -join ""))) {
        return @()
    }

    return @($json | ConvertFrom-Json | Where-Object {
        $_.headSha -eq $CommitSha
    })
}

function Restore-SelectedFilesFromStash {
    param(
        [Parameter(Mandatory = $true)]
        [string] $StashRef,
        [Parameter(Mandatory = $true)]
        [string[]] $Paths
    )

    $untrackedParent = "$StashRef^3"
    $hasUntrackedParent = $true
    & git rev-parse --verify $untrackedParent 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        $hasUntrackedParent = $false
    }

    foreach ($path in ($Paths | Select-Object -Unique)) {
        $source = $null
        $trackedObject = "{0}:{1}" -f $StashRef, $path
        & git cat-file -e $trackedObject 2>$null
        if ($LASTEXITCODE -eq 0) {
            $source = $StashRef
        } elseif ($hasUntrackedParent) {
            $untrackedObject = "{0}:{1}" -f $untrackedParent, $path
            & git cat-file -e $untrackedObject 2>$null
            if ($LASTEXITCODE -eq 0) {
                $source = $untrackedParent
            }
        }

        if ($null -ne $source) {
            Write-Host "Khôi phục có chọn lọc: $path" -ForegroundColor Gray
            Invoke-External "git" @("restore", "--source=$source", "--", $path)
        }
    }
}

$ProjectDir = [System.IO.Path]::GetFullPath($ProjectDir)
$ArtifactDir = if ([System.IO.Path]::IsPathRooted($ArtifactDir)) {
    [System.IO.Path]::GetFullPath($ArtifactDir)
} else {
    [System.IO.Path]::GetFullPath((Join-Path $ProjectDir $ArtifactDir))
}

$widgetFiles = @(
    ".github/workflows/build-apk.yml",
    "GITHUB-ACTIONS-APK-CI-CD.md",
    "app/src/main/java/com/example/nhatkyduonghuyet/NhatKyDuongHuyetApplication.kt",
    "app/src/main/java/com/example/nhatkyduonghuyet/reminder/GlucoseMeasurementSchedule.kt",
    "app/src/main/java/com/example/nhatkyduonghuyet/reminder/ReminderBroadcastReceiver.kt",
    "app/src/main/java/com/example/nhatkyduonghuyet/reminder/ReminderScheduler.kt",
    "app/src/main/java/com/example/nhatkyduonghuyet/ui/dashboard/DashboardScreen.kt",
    ".github/workflows/build-apk.yml",
    "app/src/main/java/com/example/nhatkyduonghuyet/widget/GlucoseWidgetProvider.kt",
    "app/src/main/res/layout/glucose_widget.xml"
)

try {
    if (-not (Test-Path -LiteralPath $ProjectDir -PathType Container)) {
        throw "Không tìm thấy project: $ProjectDir"
    }
    Set-Location $ProjectDir

    if (-not (Test-Path -LiteralPath ".\.git")) {
        throw "Không phải Git repository hoặc worktree: $ProjectDir"
    }
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw "Không tìm thấy git trong PATH."
    }
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        throw "Không tìm thấy GitHub CLI gh trong PATH."
    }

    Write-Host "Kiểm tra GitHub CLI..." -ForegroundColor Cyan
    & gh auth status
    if ($LASTEXITCODE -ne 0) {
        throw "GitHub CLI chưa đăng nhập. Chạy: gh auth login"
    }

    $currentBranch = (git branch --show-current).Trim()
    if ([string]::IsNullOrWhiteSpace($currentBranch)) {
        throw "Workspace đang ở detached HEAD. Hãy chuyển sang branch $Branch trước."
    }
    if ($currentBranch -ne $Branch) {
        $statusBeforeSwitch = @(git status --porcelain)
        $stashBeforeSwitch = $false
        $stashRef = $null
        if ($statusBeforeSwitch.Count -gt 0) {
            Write-Host "Workspace có thay đổi; stash tạm trước khi switch branch..." -ForegroundColor Yellow
            $stashMessage = "auto-backup-before-switch-$([Guid]::NewGuid().ToString('N'))"
            Invoke-External "git" @("stash", "push", "-u", "-m", $stashMessage)
            $stashBeforeSwitch = $true
            $stashRef = (git stash list --format="%gd" -1).Trim()
        }

        Invoke-External "git" @("switch", $Branch)

        if ($stashBeforeSwitch) {
            Write-Host "Không pop toàn bộ stash để tránh xung đột branch." -ForegroundColor Yellow
            Restore-SelectedFilesFromStash -StashRef $stashRef -Paths $widgetFiles
            Write-Host "Stash gốc vẫn được giữ nguyên: $stashRef" -ForegroundColor Yellow
        }
    }

    if ($IncludeAllChanges) {
        Write-Host "Stage toàn bộ thay đổi trong workspace..." -ForegroundColor Yellow
        Invoke-External "git" @("add", "-A")
    } else {
        Write-Host "Stage các thay đổi widget và CI/CD đã xác định..." -ForegroundColor Cyan
        Invoke-External "git" (@("add") + ($widgetFiles | Select-Object -Unique))
    }

    $staged = @(git diff --cached --name-only)
    if ($staged.Count -eq 0) {
        Write-Host "Không có thay đổi mới để commit." -ForegroundColor Yellow
    } else {
        Write-Host "Các file sẽ commit:" -ForegroundColor Cyan
        $staged | ForEach-Object { Write-Host "  $_" }
        Invoke-External "git" @("diff", "--cached", "--check")
        Write-Host "Diff statistic:" -ForegroundColor Cyan
        git diff --cached --stat
        Invoke-External "git" @("commit", "-m", $CommitMessage)
    }

    $commitSha = (git rev-parse HEAD).Trim()
    Write-Host "Commit local: $commitSha" -ForegroundColor Green

    Write-Host "Kiểm tra remote trước khi push..." -ForegroundColor Cyan
    Invoke-External "git" @("fetch", "origin", $Branch)
    & git merge-base --is-ancestor "origin/$Branch" HEAD
    if ($LASTEXITCODE -ne 0) {
        throw "Remote có commit mới chưa có trong local. Hãy xử lý bằng 'git pull --rebase origin $Branch' sau khi stash các thay đổi local."
    }
    $commitSha = (git rev-parse HEAD).Trim()

    Write-Host "Push lên origin/$Branch..." -ForegroundColor Cyan
    Invoke-External "git" @("push", "origin", $Branch)

    $remoteSha = (git rev-parse "origin/$Branch").Trim()
    if ($remoteSha -ne $commitSha) {
        throw "Remote chưa trỏ tới commit local. Local=$commitSha Remote=$remoteSha"
    }
    Write-Host "Remote đã nhận commit $remoteSha" -ForegroundColor Green

    if ($NoWatch) {
        Write-Host "Đã bỏ qua theo dõi GitHub Actions theo tùy chọn -NoWatch." -ForegroundColor Yellow
        exit 0
    }

    Write-Host "Chờ GitHub Actions tạo run cho commit $commitSha..." -ForegroundColor Cyan
    $run = $null
    for ($attempt = 1; $attempt -le 24; $attempt++) {
        $runs = @(Get-GhRunsForCommit -WorkflowName $Workflow -BranchName $Branch -CommitSha $commitSha)
        if ($runs.Count -gt 0) {
            $run = $runs | Sort-Object createdAt -Descending | Select-Object -First 1
            break
        }
        Write-Host "  Chưa thấy run; thử lại sau 5 giây ($attempt/24)..." -ForegroundColor Gray
        Start-Sleep -Seconds 5
    }

    if ($null -eq $run) {
        throw "Không tìm thấy GitHub Actions run cho commit $commitSha. Kiểm tra workflow '$Workflow' và trigger branch '$Branch'."
    }

    Write-Host "Run ID: $($run.databaseId)" -ForegroundColor Green
    Write-Host "Trạng thái ban đầu: $($run.status)" -ForegroundColor Gray

    if (-not $NoWatch) {
        Write-Host "Theo dõi build đến khi hoàn tất..." -ForegroundColor Cyan
        & gh run watch $run.databaseId --exit-status
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Build thất bại. Log step lỗi:" -ForegroundColor Red
            & gh run view $run.databaseId --log-failed
            throw "GitHub Actions build thất bại. Run: https://github.com/hqkhanh62/NhatKyDuongHuyet/actions/runs/$($run.databaseId)"
        }
    }

    Write-Host "GitHub Actions build thành công." -ForegroundColor Green
    Write-Host "Run: https://github.com/hqkhanh62/NhatKyDuongHuyet/actions/runs/$($run.databaseId)" -ForegroundColor Cyan

    if (-not $NoDownloadArtifact) {
        New-Item -ItemType Directory -Force -Path $ArtifactDir | Out-Null
        Write-Host "Tải artifact '$ArtifactName' vào $ArtifactDir..." -ForegroundColor Cyan
        Invoke-External "gh" @("run", "download", "$($run.databaseId)", "-n", $ArtifactName, "-D", $ArtifactDir)
        $apks = @(Get-ChildItem -LiteralPath $ArtifactDir -Filter "*.apk" -Recurse -File)
        if ($apks.Count -eq 0) {
            throw "Artifact tải xong nhưng không tìm thấy APK trong $ArtifactDir"
        }
        Write-Host "APK đã tải:" -ForegroundColor Green
        $apks | ForEach-Object { Write-Host "  $($_.FullName) ($($_.Length) bytes)" }
        Start-Process -FilePath "explorer.exe" -ArgumentList @($ArtifactDir)
    }
}
catch {
    Write-Host ("`nCOMMIT/PUSH/CI FAILED: " + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
