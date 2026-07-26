@echo off
cd /d "%~dp0"
echo Dang push...
git add .
git commit -m "fix: final clean"
git remote remove origin 2>nul
git remote add origin https://github.com/hqkhanh62/NhatKyDuongHuyet.git
git branch -M main
git push -u origin main --force
echo.
echo ===== DONE! =====
echo Nhan phim bat ky de dong...
pause >nul