# NhatKyDuongHuyet - Security Hardened + Android APK CI

## 1) Push code lên GitHub

```bash
git add .
git commit -m "feat: production security hardening + android apk ci"
git push origin <your-branch>
```

## 2) Cấu hình CI build APK

Workflow: `.github/workflows/android-apk.yml`

Có 2 cách truyền URL web app cho APK shell:

- Cách A (khuyên dùng): vào **GitHub Repo > Settings > Secrets and variables > Actions > Variables**
  - tạo variable: `WEB_APP_URL=https://your-domain.example.com`
- Cách B: chạy thủ công bằng **Run workflow** và nhập `web_app_url`

> Bắt buộc URL phải là HTTPS.

## 3) Chạy build APK

- Vào tab **Actions**
- Chọn workflow **Android APK CI**
- Bấm **Run workflow**
- Sau khi chạy xong, tải artifact `nhatkyduonghuyet-debug-apk`

## 4) Ghi chú

- APK này là shell app Capacitor, nạp nội dung từ `WEB_APP_URL`.
- Không commit `.env` thật hoặc khóa bí mật vào repo.
