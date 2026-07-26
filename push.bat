@echo off
cd /d "%~dp0"
git add .
git commit -m "fix: escape > in JSX and clean repo - fix Vercel build"
git push -u origin main --force
echo DONE
pause
