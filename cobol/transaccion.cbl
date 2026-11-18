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
