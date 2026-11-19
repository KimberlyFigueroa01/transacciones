# Transacciones bancarias (Spring Boot + COBOL)

Aplicación Spring Boot tipo billetera (al estilo Nequi) que delega las reglas de
dinero a 5 programas COBOL compilados en una DLL y llamados desde Java con JNA.

## Operaciones

| Operación | Programa COBOL | Función en la DLL | Endpoint |
|-----------|----------------|-------------------|----------|
| Retirar | `RETIRAR-DINERO` | `RETIRAR__DINERO` | `POST /api/cuenta/retirar` |
| Consignar (tope $10.000) | `CONSIGNAR-DINERO` | `CONSIGNAR__DINERO` | `POST /api/cuenta/consignar` |
| Transferir a otra cuenta | `TRANSFERIR-DINERO` | `TRANSFERIR__DINERO` | `POST /api/cuenta/transferir` |
| Pagar servicio (+ comisión $2) | `PAGAR-SERVICIO` | `PAGAR__SERVICIO` | `POST /api/cuenta/pagar-servicio` |
| Recargar celular ($5 – $500) | `RECARGAR-CELULAR` | `RECARGAR__CELULAR` | `POST /api/cuenta/recargar` |

Todos los programas devuelven un código de estado: `0` aprobada, `1` fondos
insuficientes, `2` monto inválido, `3` fuera de los límites de la operación.

## Cómo se conecta Java con COBOL

```
cobol/transaccion.cbl --(cobc: transpila)--> C --(gcc: compila)--> native/transaccion.dll
                                                                          |
                     Java (Spring) --(JNA llama la función exportada)-----+
```

`cobc` (GnuCOBOL) es un **transpilador**: traduce el COBOL a C y luego usa gcc para
generar la DLL. Como C no admite guiones en los nombres, cada `PROGRAM-ID` se exporta
cambiando `-` por `__` (por ejemplo `RETIRAR-DINERO` → `RETIRAR__DINERO`).
`compilar-cobol.cmd` también guarda el C intermedio en `cobol/generado/` para poder
revisarlo.

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
| `cobol/transaccion.cbl` | Fuente COBOL de los 5 programas (formato fijo) |
| `cobol/generado/` | C intermedio generado por `cobc -C` (no se versiona) |
| `native/transaccion.dll` | Programas COBOL compilados |
| `native/libcob-4.dll` y demás `.dll` | Runtime de GnuCOBOL 3.2 (MSYS2 UCRT64) |
| `compilar-cobol.cmd` | Transpila y recompila el `.cbl`; deja la DLL en `native/` |
| `src/main/java/.../Service/Transaccion.java` | Interfaz JNA con las 5 funciones de la DLL |
| `src/main/java/.../Service/CuentaService.java` | Llama al COBOL (con timeout) y guarda el resultado |
| `data/banco.json` | Usuarios e historial |

## Modificar el programa COBOL

1. Instale GnuCOBOL. Con MSYS2:
   `pacman -S mingw-w64-ucrt-x86_64-gnucobol`
2. Edite `cobol/transaccion.cbl`.
3. Detenga la aplicación (si está corriendo, la DLL queda bloqueada) y ejecute
   `compilar-cobol.cmd`.
4. Si agrega un programa nuevo, declare su función en `Transaccion.java`.

Si usa una instalación de GnuCOBOL en otra ruta, defina la variable de entorno
`GNUCOBOL_BIN` con la carpeta que contiene `libcob-4.dll`; tiene prioridad sobre `native/`.
