@echo off
REM Recompila cobol\transaccion.cbl y deja la DLL en native\transaccion.dll
REM Requiere GnuCOBOL (cobc). Con MSYS2: pacman -S mingw-w64-ucrt-x86_64-gnucobol
setlocal
where cobc >nul 2>nul
if errorlevel 1 (
  if exist "C:\msys64\ucrt64\bin\cobc.exe" (
    set "PATH=C:\msys64\ucrt64\bin;%PATH%"
  ) else (
    echo No se encontro cobc. Instale GnuCOBOL o agregue su carpeta bin al PATH.
    exit /b 1
  )
)
cd /d "%~dp0"
cobc -m -o native\transaccion.dll cobol\transaccion.cbl
if errorlevel 1 exit /b 1
echo Listo: native\transaccion.dll
