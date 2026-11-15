package uptc.banco.transacciones.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;

import uptc.banco.transacciones.Excepciones.OperacionTimeoutException;
import uptc.banco.transacciones.Model.Movimiento;
import uptc.banco.transacciones.Model.Usuario;
import uptc.banco.transacciones.Repository.BancoRepository;

/**
 * Lógica de negocio de las cuentas.
 *  - Los datos (usuarios, saldo, historial) vienen de BancoRepository (banco.json).
 *  - Las operaciones se calculan en COBOL (DLL vía JNA), a través de EjecutorCobol,
 *    que aplica el timeout.
 */
@Service
public class CuentaService {

    /** Lo que devuelve el COBOL por sus parámetros de salida. */
    private record ResultadoCobol(double nuevoSaldo, int estado) {}

    private final BancoRepository bancoRepository;
    private final EjecutorCobol ejecutorCobol;

    public CuentaService(BancoRepository bancoRepository, EjecutorCobol ejecutorCobol) {
        this.bancoRepository = bancoRepository;
        this.ejecutorCobol = ejecutorCobol;
    }

    public List<Usuario> listarUsuarios() {
        return bancoRepository.listar();
    }

    public Usuario obtenerUsuario(int id) {
        return bancoRepository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe el usuario con id " + id));
    }

    public Usuario buscarPorTelefono(String telefono) {
        return bancoRepository.buscarPorTelefono(telefono)
                .orElseThrow(() -> new NoSuchElementException("No hay ninguna cuenta con el teléfono " + telefono));
    }

    /**
     * Retiro de un usuario:
     *  1. Valida el monto (el COBOL no controla montos negativos ni cero).
     *  2. Llama al COBOL con timeout.
     *  3. Si ESTADO = 0 actualiza el saldo; si no, lo deja igual.
     *  4. Registra el movimiento (EXITOSA, RECHAZADA o TIMEOUT) y guarda en el JSON.
     */
    public synchronized Usuario retirar(int idUsuario, double monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        Usuario usuario = obtenerUsuario(idUsuario);
        double saldoActual = usuario.getSaldo();

        ResultadoCobol resultado;
        try {
            resultado = ejecutorCobol.ejecutar("RETIRAR-DINERO", () -> {
                DoubleByReference refSaldo = new DoubleByReference(saldoActual);
                DoubleByReference refMonto = new DoubleByReference(monto);
                DoubleByReference refNuevoSaldo = new DoubleByReference(0.0);
                IntByReference refEstado = new IntByReference(0);

                Transaccion.INSTANCE.retirar_dinero(refSaldo, refMonto, refNuevoSaldo, refEstado);

                return new ResultadoCobol(refNuevoSaldo.getValue(), refEstado.getValue());
            });
        } catch (OperacionTimeoutException e) {
            // El saldo no cambia, pero el intento queda en el historial
            registrarMovimiento(usuario, "RETIRO", monto, "TIMEOUT");
            bancoRepository.actualizar(usuario);
            throw e;
        }

        boolean exitoso = resultado.estado() == 0;
        if (exitoso) {
            usuario.setSaldo(resultado.nuevoSaldo());
        }
        registrarMovimiento(usuario, "RETIRO", monto, exitoso ? "EXITOSA" : "RECHAZADA");

        return bancoRepository.actualizar(usuario); // aquí se escribe banco.json
    }

    private void registrarMovimiento(Usuario usuario, String tipo, double monto, String estado) {
        if (usuario.getHistorial() == null) {
            usuario.setHistorial(new ArrayList<>());
        }
        usuario.getHistorial().add(new Movimiento(new Date(), tipo, monto, estado, usuario.getSaldo()));
    }
}
