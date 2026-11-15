package uptc.banco.transacciones.Service;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;

public interface Transaccion extends Library {
    Transaccion INSTANCE = Native.load("transaccion", Transaccion.class);

    void RETIRAR__DINERO(DoubleByReference saldoInicial, DoubleByReference montoRetirar, DoubleByReference saldoActual,
            IntByReference estado);

    default void retirar_dinero(DoubleByReference saldoInicial, DoubleByReference montoRetirar, DoubleByReference saldoActual,
            IntByReference estado) {
        RETIRAR__DINERO(saldoInicial, montoRetirar, saldoActual, estado);
    }
}
