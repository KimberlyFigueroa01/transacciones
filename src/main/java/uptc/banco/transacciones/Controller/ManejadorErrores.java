package uptc.banco.transacciones.Controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import uptc.banco.transacciones.Excepciones.OperacionTimeoutException;
import uptc.banco.transacciones.Excepciones.SesionInvalidaException;

/**
 * Convierte las excepciones en respuestas HTTP con {"error": "..."}
 * para todos los controladores.
 *   400 dato inválido · 401 sesión inválida/expirada · 404 no encontrado · 504 timeout
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datoInvalido(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(SesionInvalidaException.class)
    public ResponseEntity<Map<String, String>> sesionInvalida(SesionInvalidaException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException e) {
        return respuesta(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(OperacionTimeoutException.class)
    public ResponseEntity<Map<String, String>> timeout(OperacionTimeoutException e) {
        return respuesta(HttpStatus.GATEWAY_TIMEOUT, e);
    }

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, RuntimeException e) {
        return ResponseEntity.status(estado).body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
