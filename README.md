# Transacciones bancarias (Spring Boot + COBOL)

Aplicación Spring Boot que delega el cálculo del retiro de dinero a un programa
COBOL (`RETIRAR-DINERO`) compilado como DLL y llamado desde Java con JNA.

## Requisitos

- Windows x64
- JDK 21

No hace falta instalar GnuCOBOL para ejecutar: la carpeta `native/` ya trae
`transaccion.dll` compilada y el runtime de GnuCOBOL (`libcob-4.dll` y sus dependencias).

## Ejecutar

```cmd
mvnw.cmd spring-boot:run
```

Ejecútelo desde la carpeta raíz del proyecto (donde está `pom.xml`), porque las DLL se
buscan en `native/` relativo a esa carpeta. Luego abra http://localhost:8080

## Estructura relevante

| Ruta | Contenido |
|------|-----------|
| `cobol/transaccion.cbl` | Fuente COBOL del programa `RETIRAR-DINERO` |
| `native/transaccion.dll` | Programa COBOL compilado |
| `native/libcob-4.dll` y demás `.dll` | Runtime de GnuCOBOL 3.2 (MSYS2 UCRT64) |
| `compilar-cobol.cmd` | Recompila el `.cbl` y deja la DLL en `native/` |
| `data/banco.json` | Usuarios e historial |

## Modificar el programa COBOL

1. Instale GnuCOBOL. Con MSYS2:
   `pacman -S mingw-w64-ucrt-x86_64-gnucobol`
2. Edite `cobol/transaccion.cbl`.
3. Ejecute `compilar-cobol.cmd`.

Si usa una instalación de GnuCOBOL en otra ruta, defina la variable de entorno
`GNUCOBOL_BIN` con la carpeta que contiene `libcob-4.dll`; tiene prioridad sobre `native/`.
