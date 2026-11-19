      ******************************************************************
      * Author: Kimberly Natalia Figueroa Zapata
      * Date: 25/10/2026
      * Purpose: Java&Cobol
      * Tectonics: cobc
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. RETIRAR-DINERO.
       DATA DIVISION.
       LINKAGE SECTION.
       01 SALDO-INICIAL USAGE COMP-2.
       01 MONTO-RETIRA USAGE COMP-2.
       01 SALDO-ACTUALIZADO USAGE COMP-2.
       01 ESTADO PIC S9(9) COMP-5.

       PROCEDURE DIVISION USING
       SALDO-INICIAL,
       MONTO-RETIRA,
       SALDO-ACTUALIZADO,
       ESTADO.
       MAIN-PROCEDURE.
      *    Agregado: monto cero o negativo -> ESTADO 2 (monto invalido).
      *    Antes esta validacion la hacia Java; ahora la decide COBOL,
      *    igual que en las otras 4 operaciones.
            IF MONTO-RETIRA <= 0 THEN
                MOVE SALDO-INICIAL TO SALDO-ACTUALIZADO
                MOVE 2 TO ESTADO
                GOBACK
            END-IF
            IF MONTO-RETIRA > SALDO-INICIAL THEN
                MOVE SALDO-INICIAL TO SALDO-ACTUALIZADO
                MOVE 1 TO ESTADO
            ELSE
                COMPUTE SALDO-ACTUALIZADO = SALDO-INICIAL - MONTO-RETIRA
                MOVE 0 TO ESTADO
            END-IF
            GOBACK.
            STOP RUN.
       END PROGRAM RETIRAR-DINERO.

      ******************************************************************
      * Operaciones agregadas: CONSIGNAR-DINERO, TRANSFERIR-DINERO,
      *                        PAGAR-SERVICIO, RECARGAR-CELULAR
      * Author: Cristian Eslava
      * Date: 17/11/2026
      *
      * Mismas convenciones que RETIRAR-DINERO:
      *   - Parametros por referencia (LINKAGE SECTION).
      *   - USAGE COMP-2        = double en Java/C.
      *   - PIC S9(9) COMP-5    = int en Java/C.
      *   - Codigos de ESTADO:
      *       0 = aprobada
      *       1 = fondos insuficientes
      *       2 = monto invalido (cero o negativo)
      *       3 = monto fuera de los limites de la operacion
      *   - Si se rechaza, el saldo de salida es igual al de entrada.
      *
      * cobc transpila cada PROGRAM-ID a una funcion C exportada en la
      * DLL, cambiando "-" por "__" (CONSIGNAR-DINERO ->
      * CONSIGNAR__DINERO). Asi la llama Java con JNA.
      ******************************************************************

      ******************************************************************
      * CONSIGNAR-DINERO
      * Regla: una consignacion no puede superar el tope.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CONSIGNAR-DINERO.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 TOPE-CONSIGNACION PIC 9(7)V99 VALUE 10000.00.
       LINKAGE SECTION.
       01 SALDO-INICIAL USAGE COMP-2.
       01 MONTO-CONSIGNA USAGE COMP-2.
       01 SALDO-ACTUALIZADO USAGE COMP-2.
       01 ESTADO PIC S9(9) COMP-5.

       PROCEDURE DIVISION USING
       SALDO-INICIAL,
       MONTO-CONSIGNA,
       SALDO-ACTUALIZADO,
       ESTADO.
       MAIN-PROCEDURE.
            MOVE SALDO-INICIAL TO SALDO-ACTUALIZADO
            EVALUATE TRUE
                WHEN MONTO-CONSIGNA <= 0
                    MOVE 2 TO ESTADO
                WHEN MONTO-CONSIGNA > TOPE-CONSIGNACION
                    MOVE 3 TO ESTADO
                WHEN OTHER
                    COMPUTE SALDO-ACTUALIZADO =
                            SALDO-INICIAL + MONTO-CONSIGNA
                    MOVE 0 TO ESTADO
            END-EVALUATE
            GOBACK.
       END PROGRAM CONSIGNAR-DINERO.

      ******************************************************************
      * TRANSFERIR-DINERO
      * Regla: descuenta al origen y suma al destino en el mismo
      *        calculo, o no toca ninguno de los dos saldos.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. TRANSFERIR-DINERO.
       DATA DIVISION.
       LINKAGE SECTION.
       01 SALDO-ORIGEN USAGE COMP-2.
       01 SALDO-DESTINO USAGE COMP-2.
       01 MONTO-TRANSFIERE USAGE COMP-2.
       01 NUEVO-SALDO-ORIGEN USAGE COMP-2.
       01 NUEVO-SALDO-DESTINO USAGE COMP-2.
       01 ESTADO PIC S9(9) COMP-5.

       PROCEDURE DIVISION USING
       SALDO-ORIGEN,
       SALDO-DESTINO,
       MONTO-TRANSFIERE,
       NUEVO-SALDO-ORIGEN,
       NUEVO-SALDO-DESTINO,
       ESTADO.
       MAIN-PROCEDURE.
            MOVE SALDO-ORIGEN TO NUEVO-SALDO-ORIGEN
            MOVE SALDO-DESTINO TO NUEVO-SALDO-DESTINO
            EVALUATE TRUE
                WHEN MONTO-TRANSFIERE <= 0
                    MOVE 2 TO ESTADO
                WHEN MONTO-TRANSFIERE > SALDO-ORIGEN
                    MOVE 1 TO ESTADO
                WHEN OTHER
                    COMPUTE NUEVO-SALDO-ORIGEN =
                            SALDO-ORIGEN - MONTO-TRANSFIERE
                    COMPUTE NUEVO-SALDO-DESTINO =
                            SALDO-DESTINO + MONTO-TRANSFIERE
                    MOVE 0 TO ESTADO
            END-EVALUATE
            GOBACK.
       END PROGRAM TRANSFERIR-DINERO.

      ******************************************************************
      * PAGAR-SERVICIO (agua, luz, gas, internet)
      * Regla: cobra el valor de la factura mas una comision fija;
      *        el saldo debe alcanzar para ambos.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. PAGAR-SERVICIO.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 COMISION-FIJA PIC 9(5)V99 VALUE 2.00.
       01 TOTAL-A-PAGAR USAGE COMP-2.
       LINKAGE SECTION.
       01 SALDO-INICIAL USAGE COMP-2.
       01 VALOR-FACTURA USAGE COMP-2.
       01 SALDO-ACTUALIZADO USAGE COMP-2.
       01 COMISION USAGE COMP-2.
       01 ESTADO PIC S9(9) COMP-5.

       PROCEDURE DIVISION USING
       SALDO-INICIAL,
       VALOR-FACTURA,
       SALDO-ACTUALIZADO,
       COMISION,
       ESTADO.
       MAIN-PROCEDURE.
            MOVE SALDO-INICIAL TO SALDO-ACTUALIZADO
            MOVE COMISION-FIJA TO COMISION
            COMPUTE TOTAL-A-PAGAR = VALOR-FACTURA + COMISION-FIJA
            EVALUATE TRUE
                WHEN VALOR-FACTURA <= 0
                    MOVE 2 TO ESTADO
                WHEN TOTAL-A-PAGAR > SALDO-INICIAL
                    MOVE 1 TO ESTADO
                WHEN OTHER
                    COMPUTE SALDO-ACTUALIZADO =
                            SALDO-INICIAL - TOTAL-A-PAGAR
                    MOVE 0 TO ESTADO
            END-EVALUATE
            GOBACK.
       END PROGRAM PAGAR-SERVICIO.

      ******************************************************************
      * RECARGAR-CELULAR
      * Regla: la recarga debe estar entre el minimo y el maximo
      *        permitidos, y el saldo debe alcanzar.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. RECARGAR-CELULAR.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 RECARGA-MINIMA PIC 9(5)V99 VALUE 5.00.
       01 RECARGA-MAXIMA PIC 9(5)V99 VALUE 500.00.
       LINKAGE SECTION.
       01 SALDO-INICIAL USAGE COMP-2.
       01 MONTO-RECARGA USAGE COMP-2.
       01 SALDO-ACTUALIZADO USAGE COMP-2.
       01 ESTADO PIC S9(9) COMP-5.

       PROCEDURE DIVISION USING
       SALDO-INICIAL,
       MONTO-RECARGA,
       SALDO-ACTUALIZADO,
       ESTADO.
       MAIN-PROCEDURE.
            MOVE SALDO-INICIAL TO SALDO-ACTUALIZADO
            EVALUATE TRUE
                WHEN MONTO-RECARGA <= 0
                    MOVE 2 TO ESTADO
                WHEN MONTO-RECARGA < RECARGA-MINIMA
                  OR MONTO-RECARGA > RECARGA-MAXIMA
                    MOVE 3 TO ESTADO
                WHEN MONTO-RECARGA > SALDO-INICIAL
                    MOVE 1 TO ESTADO
                WHEN OTHER
                    COMPUTE SALDO-ACTUALIZADO =
                            SALDO-INICIAL - MONTO-RECARGA
                    MOVE 0 TO ESTADO
            END-EVALUATE
            GOBACK.
       END PROGRAM RECARGAR-CELULAR.
