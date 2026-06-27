@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "STARSECTOR_DIR=%SCRIPT_DIR%\..\.."
set "SRC_DIR=%SCRIPT_DIR%\src"
set "CLASSES_DIR=%SCRIPT_DIR%\classes"
set "JAR_DIR=%SCRIPT_DIR%\jars"
set "OUTPUT_JAR=%JAR_DIR%\FarsightDrive.jar"
set "SOURCE_LIST=%SCRIPT_DIR%\java_files.txt"
set "MANIFEST_DIR=%TEMP%\fsd_manifest_%RANDOM%"

set "CP=%STARSECTOR_DIR%\starsector-core\starfarer.api.jar"
set "CP=%CP%;%STARSECTOR_DIR%\starsector-core\fs.common_obf.jar"
set "CP=%CP%;%STARSECTOR_DIR%\starsector-core\starfarer_obf.jar"
set "CP=%CP%;%STARSECTOR_DIR%\starsector-core\lwjgl.jar"
set "CP=%CP%;%STARSECTOR_DIR%\starsector-core\lwjgl_util.jar"
set "CP=%CP%;%STARSECTOR_DIR%\starsector-core\json.jar"
set "CP=%CP%;%STARSECTOR_DIR%\starsector-core\log4j-1.2.9.jar"
set "CP=%CP%;%STARSECTOR_DIR%\mods\LazyLib\jars\LazyLib.jar"
set "CP=%CP%;%STARSECTOR_DIR%\mods\LazyLib\jars\LazyLib-Kotlin.jar"
set "CP=%CP%;%STARSECTOR_DIR%\mods\MagicLib-1.5.6\jars\MagicLib.jar"
set "CP=%CP%;%STARSECTOR_DIR%\mods\MagicLib-1.5.6\jars\MagicLib-Kotlin.jar"
set "CP=%CP%;%STARSECTOR_DIR%\mods\zz GraphicsLib-1.12.1\jars\Graphics.jar"
set "CP=%CP%;%STARSECTOR_DIR%\mods\zz BoxUtil-1.4.9\jars\BoxUtilMod.jar"
set "CP=%CP%;%STARSECTOR_DIR%\mods\LunaLib-2.0.5\jars\LunaLib.jar"

 echo ==================================================
echo Building FarsightDrive.jar
echo Source: %SRC_DIR%
echo Output: %OUTPUT_JAR%
echo ==================================================

where javac >nul 2>nul
if errorlevel 1 (
    echo Error: javac was not found on PATH.
    exit /b 1
)

where jar >nul 2>nul
if errorlevel 1 (
    echo Error: jar was not found on PATH.
    exit /b 1
)

if not exist "%SRC_DIR%" (
    echo Error: source directory not found: %SRC_DIR%
    exit /b 1
)

if exist "%CLASSES_DIR%" rmdir /s /q "%CLASSES_DIR%"
mkdir "%CLASSES_DIR%" || exit /b 1
if not exist "%JAR_DIR%" mkdir "%JAR_DIR%" || exit /b 1

dir /s /b "%SRC_DIR%\*.java" > "%SOURCE_LIST%"
for /f %%i in ('type "%SOURCE_LIST%" ^| find /c /v ""') do set "JAVA_COUNT=%%i"
if "%JAVA_COUNT%"=="0" (
    echo Error: no Java sources found.
    del "%SOURCE_LIST%"
    exit /b 1
)

echo Compiling %JAVA_COUNT% Java files...
javac --release 17 -Xlint:deprecation -Xlint:unchecked -encoding UTF-8 -d "%CLASSES_DIR%" -cp "%CP%" @"%SOURCE_LIST%"
if errorlevel 1 (
    del "%SOURCE_LIST%"
    exit /b 1
)
del "%SOURCE_LIST%"

mkdir "%MANIFEST_DIR%" >nul 2>nul
pushd "%MANIFEST_DIR%"
jar xf "%OUTPUT_JAR%" META-INF/MANIFEST.MF >nul 2>nul
popd
if exist "%MANIFEST_DIR%\META-INF\MANIFEST.MF" (
    jar cfm "%OUTPUT_JAR%" "%MANIFEST_DIR%\META-INF\MANIFEST.MF" -C "%CLASSES_DIR%" .
) else (
    jar cf "%OUTPUT_JAR%" -C "%CLASSES_DIR%" .
)
if errorlevel 1 exit /b 1

rmdir /s /q "%CLASSES_DIR%"
if exist "%MANIFEST_DIR%" rmdir /s /q "%MANIFEST_DIR%"

echo Build complete: %OUTPUT_JAR%
endlocal
