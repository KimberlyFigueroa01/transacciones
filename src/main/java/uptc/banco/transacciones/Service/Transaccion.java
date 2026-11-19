package uptc.banco.transacciones.Service;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * Puente JNA hacia transaccion.dll (generada desde cobol/transaccion.cbl).
 *
 * Cada método corresponde a un PROGRAM-ID de COBOL. El transpilador cobc
 * convierte el nombre COBOL en un nombre C válido cambiando "-" por "__":
 *   RETIRAR-DINERO → RETIRAR__DINERO
 *
 * Tipos (todos los parámetros van por referencia, como en la LINKAGE SECTION):
 *   COMP-2           ↔ double ↔ DoubleByReference
 *   PIC S9(9) COMP-5 ↔ int    ↔ IntByReference
 *
 * Código de estado que devuelven todos los programas:
 *   0 aprobada · 1 fondos insuficientes · 2 monto inválido · 3 fuera de límites
 */
public interface Transaccion extends Library {
    Transaccion INSTANCE = Native.load("transaccion", Transaccion.class);

    /** RETIRAR-DINERO (saldo, monto) → (saldo final, estado) */
    void RETIRAR__DINERO(DoubleByReference saldoInicial, DoubleByReference monto,
                         DoubleByReference saldoFinal, IntByReference estado);

    /** CONSIGNAR-DINERO (saldo, monto) → (saldo final, estado) */
    void CONSIGNAR__DINERO(DoubleByReference saldoInicial, DoubleByReference monto,
                           DoubleByReference saldoFinal, IntByReference estado);

    /** TRANSFERIR-DINERO (saldo origen, saldo destino, monto) → (nuevo origen, nuevo destino, estado) */
    void TRANSFERIR__DINERO(DoubleByReference saldoOrigen, DoubleByReference saldoDestino,
                            DoubleByReference monto, DoubleByReference nuevoOrigen,
                            DoubleByReference nuevoDestino, IntByReference estado);

    /** PAGAR-SERVICIO (saldo, valor factura) → (saldo final, comisión, estado) */
    void PAGAR__SERVICIO(DoubleByReference saldoInicial, DoubleByReference valor,
                         DoubleByReference saldoFinal, DoubleByReference comision,
                         IntByReference estado);

    /** RECARGAR-CELULAR (saldo, monto) → (saldo final, estado) */
    void RECARGAR__CELULAR(DoubleByReference saldoInicial, DoubleByReference monto,
                           DoubleByReference saldoFinal, IntByReference estado);
}
