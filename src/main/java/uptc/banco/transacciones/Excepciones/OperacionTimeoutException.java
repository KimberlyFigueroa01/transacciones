package uptc.banco.transacciones.Excepciones;

/** La operación COBOL no respondió dentro del tiempo máximo permitido. */
public class OperacionTimeoutException extends RuntimeException {

    public OperacionTimeoutException(String operacion, long timeoutMs) {
        super("La operación " + operacion + " no respondió en " + timeoutMs
                + " ms. Se canceló y no se aplicó ningún cambio.");
    }
}
