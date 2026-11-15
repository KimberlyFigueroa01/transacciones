package uptc.banco.transacciones.Excepciones;

/** No hay sesión, la sesión no existe o se cerró por inactividad. */
public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
