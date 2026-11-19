package uptc.banco.transacciones.Service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Service;

import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;

import uptc.banco.transacciones.Excepciones.OperacionTimeoutException;
import uptc.banco.transacciones.Model.Movimiento;
import uptc.banco.transacciones.Model.ResultadoOperacion;
import uptc.banco.transacciones.Model.Usuario;
import uptc.banco.transacciones.Repository.BancoRepository;

/**
 * Lógica de negocio de las cuentas.
 *  - Los datos (usuarios, saldo, historial) vienen de BancoRepository (banco.json).
 *  - Las reglas de dinero (¿alcanza el saldo?, ¿el monto es válido?, límites,
 *    comisiones) se calculan en COBOL (transaccion.dll vía JNA), a través de
 *    EjecutorCobol, que aplica el timeout.
 *  - Java solo valida datos que no son de dinero (teléfono destino, número de
 *    celular, tipo de servicio) y guarda el resultado.
 *
 * Las 5 operaciones (cada una es un programa COBOL):
 *   RETIRAR-DINERO · CONSIGNAR-DINERO · TRANSFERIR-DINERO · PAGAR-SERVICIO · RECARGAR-CELULAR
 */
@Service
public class CuentaService {

    /** Lo que devuelve un programa COBOL de una sola cuenta por sus parámetros de salida. */
    private record SalidaCobol(double saldoFinal, double comision, int estado) {}

    /** Lo que devuelve TRANSFERIR-DINERO. */
    private record SalidaTransferencia(double nuevoOrigen, double nuevoDestino, int estado) {}

    public static final Set<String> SERVICIOS = Set.of("AGUA", "LUZ", "GAS", "INTERNET");

    // Textos de los límites. Los valores reales están en la WORKING-STORAGE del COBOL;
    // si se cambian allá, actualice también estos mensajes.
    private static final String LIMITE_CONSIGNACION = "Una consignación no puede superar $10.000,00";
    private static final String LIMITE_RECARGA = "La recarga debe estar entre $5,00 y $500,00";

    private final BancoRepository bancoRepository;
    private final EjecutorCobol ejecutorCobol;
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    public CuentaService(BancoRepository bancoRepository, EjecutorCobol ejecutorCobol) {
        this.bancoRepository = bancoRepository;
        this.ejecutorCobol = ejecutorCobol;
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

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

    // =========================================================
    // 1. RETIRAR
    // =========================================================

    public synchronized ResultadoOperacion retirar(int idUsuario, double monto) {
        Usuario usuario = obtenerUsuario(idUsuario);
        double saldo = usuario.getSaldo();

        return operarCuenta(usuario, "RETIRO", "RETIRAR-DINERO", "Retiro en cajero", null, () -> {
            DoubleByReference refSaldo = new DoubleByReference(saldo);
            DoubleByReference refMonto = new DoubleByReference(monto);
            DoubleByReference refFinal = new DoubleByReference(0);
            IntByReference refEstado = new IntByReference(-1);
            Transaccion.INSTANCE.RETIRAR__DINERO(refSaldo, refMonto, refFinal, refEstado);
            return new SalidaCobol(refFinal.getValue(), 0, refEstado.getValue());
        }, monto, "Retiro exitoso de " + dinero(monto));
    }

    // =========================================================
    // 2. CONSIGNAR
    // =========================================================

    public synchronized ResultadoOperacion consignar(int idUsuario, double monto) {
        Usuario usuario = obtenerUsuario(idUsuario);
        double saldo = usuario.getSaldo();

        return operarCuenta(usuario, "CONSIGNACION", "CONSIGNAR-DINERO", "Consignación a la cuenta",
                LIMITE_CONSIGNACION, () -> {
            DoubleByReference refSaldo = new DoubleByReference(saldo);
            DoubleByReference refMonto = new DoubleByReference(monto);
            DoubleByReference refFinal = new DoubleByReference(0);
            IntByReference refEstado = new IntByReference(-1);
            Transaccion.INSTANCE.CONSIGNAR__DINERO(refSaldo, refMonto, refFinal, refEstado);
            return new SalidaCobol(refFinal.getValue(), 0, refEstado.getValue());
        }, monto, "Consignación exitosa de " + dinero(monto));
    }

    // =========================================================
    // 3. TRANSFERIR (dos cuentas)
    // =========================================================

    public synchronized ResultadoOperacion transferir(int idOrigen, String telefonoDestino, double monto) {
        String telefono = telefonoDestino == null ? "" : telefonoDestino.trim();
        if (telefono.isEmpty()) {
            throw new IllegalArgumentException("Ingrese el teléfono de la cuenta destino");
        }
        Usuario origen = obtenerUsuario(idOrigen);
        Usuario destino = buscarPorTelefono(telefono);
        if (destino.getId() == origen.getId()) {
            throw new IllegalArgumentException("No puede transferirse dinero a su propia cuenta");
        }
        double saldoOrigen = origen.getSaldo();
        double saldoDestino = destino.getSaldo();
        String haciaQuien = "A " + nombreCompleto(destino) + " (" + destino.getTelefono() + ")";

        SalidaTransferencia salida;
        try {
            salida = ejecutorCobol.ejecutar("TRANSFERIR-DINERO", () -> {
                DoubleByReference refOrigen = new DoubleByReference(saldoOrigen);
                DoubleByReference refDestino = new DoubleByReference(saldoDestino);
                DoubleByReference refMonto = new DoubleByReference(monto);
                DoubleByReference refNuevoOrigen = new DoubleByReference(0);
                DoubleByReference refNuevoDestino = new DoubleByReference(0);
                IntByReference refEstado = new IntByReference(-1);
                Transaccion.INSTANCE.TRANSFERIR__DINERO(refOrigen, refDestino, refMonto,
                        refNuevoOrigen, refNuevoDestino, refEstado);
                return new SalidaTransferencia(refNuevoOrigen.getValue(), refNuevoDestino.getValue(),
                        refEstado.getValue());
            });
        } catch (OperacionTimeoutException e) {
            registrarMovimiento(origen, "TRANSFERENCIA_ENVIADA", monto, "TIMEOUT", haciaQuien);
            bancoRepository.actualizar(origen);
            throw e;
        }

        if (salida.estado() == 0) {
            origen.setSaldo(salida.nuevoOrigen());
            destino.setSaldo(salida.nuevoDestino());
            registrarMovimiento(origen, "TRANSFERENCIA_ENVIADA", monto, "EXITOSA", haciaQuien);
            registrarMovimiento(destino, "TRANSFERENCIA_RECIBIDA", monto, "EXITOSA",
                    "De " + nombreCompleto(origen) + " (" + origen.getTelefono() + ")");
            // Las dos cuentas se guardan en una sola escritura del archivo
            bancoRepository.actualizarVarios(origen, destino);
            return new ResultadoOperacion(origen, "EXITOSA", 0,
                    "Transferencia exitosa de " + dinero(monto) + " a " + nombreCompleto(destino));
        }

        String motivo = motivoRechazo(salida.estado(), null);
        registrarMovimiento(origen, "TRANSFERENCIA_ENVIADA", monto, "RECHAZADA", haciaQuien + " · " + motivo);
        bancoRepository.actualizar(origen);
        return new ResultadoOperacion(origen, "RECHAZADA", salida.estado(), "Transferencia rechazada: " + motivo);
    }

    // =========================================================
    // 4. PAGAR SERVICIO
    // =========================================================

    public synchronized ResultadoOperacion pagarServicio(int idUsuario, String servicio, String referencia,
                                                         double valor) {
        String tipoServicio = servicio == null ? "" : servicio.trim().toUpperCase(Locale.ROOT);
        if (!SERVICIOS.contains(tipoServicio)) {
            throw new IllegalArgumentException("Servicio no válido. Opciones: " + String.join(", ", SERVICIOS));
        }
        String ref = referencia == null ? "" : referencia.trim();
        if (ref.isEmpty() || ref.length() > 30) {
            throw new IllegalArgumentException("Ingrese la referencia de la factura (máximo 30 caracteres)");
        }
        Usuario usuario = obtenerUsuario(idUsuario);
        double saldo = usuario.getSaldo();
        String detalle = tipoServicio + " · ref. " + ref;

        return operarCuenta(usuario, "PAGO_SERVICIO", "PAGAR-SERVICIO", detalle, null, () -> {
            DoubleByReference refSaldo = new DoubleByReference(saldo);
            DoubleByReference refValor = new DoubleByReference(valor);
            DoubleByReference refFinal = new DoubleByReference(0);
            DoubleByReference refComision = new DoubleByReference(0);
            IntByReference refEstado = new IntByReference(-1);
            Transaccion.INSTANCE.PAGAR__SERVICIO(refSaldo, refValor, refFinal, refComision, refEstado);
            return new SalidaCobol(refFinal.getValue(), refComision.getValue(), refEstado.getValue());
        }, valor, "Pago de " + tipoServicio + " exitoso por " + dinero(valor));
    }

    // =========================================================
    // 5. RECARGAR CELULAR
    // =========================================================

    public synchronized ResultadoOperacion recargarCelular(int idUsuario, String celular, double monto) {
        String numero = celular == null ? "" : celular.trim();
        if (!numero.matches("3\\d{9}")) {
            throw new IllegalArgumentException("El celular debe tener 10 dígitos y empezar por 3");
        }
        Usuario usuario = obtenerUsuario(idUsuario);
        double saldo = usuario.getSaldo();

        return operarCuenta(usuario, "RECARGA", "RECARGAR-CELULAR", "Celular " + numero, LIMITE_RECARGA, () -> {
            DoubleByReference refSaldo = new DoubleByReference(saldo);
            DoubleByReference refMonto = new DoubleByReference(monto);
            DoubleByReference refFinal = new DoubleByReference(0);
            IntByReference refEstado = new IntByReference(-1);
            Transaccion.INSTANCE.RECARGAR__CELULAR(refSaldo, refMonto, refFinal, refEstado);
            return new SalidaCobol(refFinal.getValue(), 0, refEstado.getValue());
        }, monto, "Recarga de " + dinero(monto) + " al " + numero + " exitosa");
    }

    // =========================================================
    // FLUJO COMÚN DE LAS OPERACIONES DE UNA CUENTA
    // =========================================================

    /**
     *  1. Llama al programa COBOL con timeout.
     *  2. Si ESTADO = 0 actualiza el saldo; si no, lo deja igual.
     *  3. Registra el movimiento (EXITOSA, RECHAZADA o TIMEOUT) y guarda en el JSON.
     *
     * @param mensajeLimite texto para el código 3 (fuera de límites) de esta operación
     * @param monto         monto pedido por el usuario (la comisión, si la hay, se suma)
     */
    private ResultadoOperacion operarCuenta(Usuario usuario, String tipo, String programa, String detalle,
                                            String mensajeLimite, Callable<SalidaCobol> llamada,
                                            double monto, String mensajeExito) {
        SalidaCobol salida;
        try {
            salida = ejecutorCobol.ejecutar(programa, llamada);
        } catch (OperacionTimeoutException e) {
            // El saldo no cambia, pero el intento queda en el historial
            registrarMovimiento(usuario, tipo, monto, "TIMEOUT", detalle);
            bancoRepository.actualizar(usuario);
            throw e;
        }

        double total = monto + salida.comision();
        String detalleFinal = salida.comision() > 0
                ? detalle + " · comisión " + dinero(salida.comision())
                : detalle;

        if (salida.estado() == 0) {
            usuario.setSaldo(salida.saldoFinal());
            registrarMovimiento(usuario, tipo, total, "EXITOSA", detalleFinal);
            String mensaje = salida.comision() > 0
                    ? mensajeExito + " (+ comisión " + dinero(salida.comision()) + ")"
                    : mensajeExito;
            return new ResultadoOperacion(bancoRepository.actualizar(usuario), "EXITOSA", 0, mensaje);
        }

        String motivo = motivoRechazo(salida.estado(), mensajeLimite);
        registrarMovimiento(usuario, tipo, total, "RECHAZADA", detalleFinal + " · " + motivo);
        return new ResultadoOperacion(bancoRepository.actualizar(usuario), "RECHAZADA", salida.estado(),
                "Operación rechazada: " + motivo);
    }

    /** Traduce el código de ESTADO del COBOL a un texto. */
    private String motivoRechazo(int codigo, String mensajeLimite) {
        return switch (codigo) {
            case 1 -> "fondos insuficientes";
            case 2 -> "el monto debe ser mayor que cero";
            case 3 -> mensajeLimite != null ? mensajeLimite : "monto fuera de los límites permitidos";
            default -> "código COBOL desconocido (" + codigo + ")";
        };
    }

    private void registrarMovimiento(Usuario usuario, String tipo, double monto, String estado, String detalle) {
        if (usuario.getHistorial() == null) {
            usuario.setHistorial(new ArrayList<>());
        }
        usuario.getHistorial().add(new Movimiento(new Date(), tipo, monto, estado, usuario.getSaldo(), detalle));
    }

    private String dinero(double valor) {
        return moneda.format(valor);
    }

    private static String nombreCompleto(Usuario u) {
        return u.getNombre() + " " + u.getApellido();
    }
}
