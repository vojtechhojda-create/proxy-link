@echo off
setlocal
cd /d "%~dp0"
where git >nul 2>nul
if errorlevel 1 (
  echo git is not installed: https://git-scm.com/download/win
  pause
  exit /b 1
)
if not exist ".git" (
  git init
  git branch -M main
  git remote add origin https://github.com/vojtechhojda-create/proxy-link.git
)
git config user.email >nul 2>nul || git config user.email "vojtechhojda-create@users.noreply.github.com"
git config user.name >nul 2>nul || git config user.name "vojtechhojda-create"
git add -A
git commit -m "build files"
git fetch origin
echo.
echo this REPLACES everything on github (vojtechhojda-create/proxy-link, branch main)
echo with the files in this folder.
choice /m "continue"
if errorlevel 2 exit /b 1
git push --force-with-lease origin main
if errorlevel 1 git push --force origin main
echo.
echo workflow files that github will see:
git ls-files .github
echo.
echo open https://github.com/vojtechhojda-create/proxy-link/actions
pause
