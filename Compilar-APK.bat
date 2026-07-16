@echo off
setlocal
set "ANDROID_HOME=%~dp0..\AndroidSDK"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
cd /d "%~dp0"
call gradlew.bat assembleRelease
if errorlevel 1 (
  echo.
  echo Falha ao compilar o APK.
  pause
  exit /b 1
)
copy /y "app\build\outputs\apk\release\app-release.apk" "DIG-Diagnostico-Android.apk" >nul
echo.
echo APK criado em: %~dp0DIG-Diagnostico-Android.apk
pause
