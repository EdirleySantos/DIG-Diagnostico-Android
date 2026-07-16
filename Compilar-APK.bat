@echo off
setlocal
set "ANDROID_HOME=%~dp0..\AndroidSDK"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
cd /d "%~dp0"
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo Falha ao compilar o APK.
  pause
  exit /b 1
)
copy /y "app\build\outputs\apk\debug\app-debug.apk" "DIG-Diagnostico-Android.apk" >nul
echo.
echo APK criado em: %~dp0DIG-Diagnostico-Android.apk
pause
