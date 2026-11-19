@echo off
REM Recompila cobol\transaccion.cbl y deja la DLL en native\transaccion.dll
REM Requiere GnuCOBOL (cobc). Con MSYS2: pacman -S mingw-w64-ucrt-x86_64-gnucobol
REM
REM Hace dos pasos para poder ver el trabajo del transpilador:
REM   1. cobc -C : solo TRANSPILA COBOL -> C (queda en cobol\generado\)
REM   2. cobc -m : transpila y compila con gcc -> native\transaccion.dll
REM
REM Detenga la aplicacion antes de compilar: si esta corriendo, Windows
REM tiene la DLL bloqueada y no se puede reemplazar.
setlocal
set "MSYS_UCRT=C:\msys64\ucrt64"
where cobc >nul 2>nul
if errorlevel 1 (
  if exist "%MSYS_UCRT%\bin\cobc.exe" (
    set "PATH=%MSYS_UCRT%\bin;%PATH%"
  ) else (
    echo No se encontro cobc. Instale GnuCOBOL o agregue su carpeta bin al PATH.
    exit /b 1
  )
)
REM Fuera de la consola de MSYS2, cobc no encuentra su configuracion
REM ("default.conf: No such file or directory") si no se definen estas rutas.
if not defined COB_CONFIG_DIR if exist "%MSYS_UCRT%\share\gnucobol\config" set "COB_CONFIG_DIR=%MSYS_UCRT%\share\gnucobol\config"
if not defined COB_COPY_DIR if exist "%MSYS_UCRT%\share\gnucobol\copy" set "COB_COPY_DIR=%MSYS_UCRT%\share\gnucobol\copy"

cd /d "%~dp0"
if not exist cobol\generado mkdir cobol\generado

echo [1/2] Transpilando COBOL a C (cobc -C)...
cobc -C -o cobol\generado\transaccion.c cobol\transaccion.cbl
if errorlevel 1 exit /b 1

echo [2/2] Compilando la DLL (cobc -m)...
cobc -m -o native\transaccion.dll cobol\transaccion.cbl
if errorlevel 1 exit /b 1

echo Listo: native\transaccion.dll  (C intermedio en cobol\generado\)
