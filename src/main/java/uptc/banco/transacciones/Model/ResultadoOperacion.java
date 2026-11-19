package uptc.banco.transacciones.Model;

/**
 * Respuesta de cualquier operación de la cuenta.
 *
 * @param usuario     usuario actualizado (saldo e historial)
 * @param estado      EXITOSA o RECHAZADA
 * @param codigoCobol código que devolvió el programa COBOL (0, 1, 2 o 3)
 * @param mensaje     texto para mostrar en la interfaz
 */
public record ResultadoOperacion(Usuario usuario, String estado, int codigoCobol, String mensaje) {
}
