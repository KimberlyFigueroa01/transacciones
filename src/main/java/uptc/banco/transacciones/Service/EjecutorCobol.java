package uptc.banco.transacciones.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uptc.banco.transacciones.Excepciones.OperacionTimeoutException;

/**
 * Ejecuta las llamadas a la DLL de COBOL con un tiempo máximo (timeout).
 *
 * Cómo funciona:
 *  - La llamada se envía a un hilo aparte (hilo "cobol").
 *  - El hilo que atiende la petición HTTP espera el resultado como máximo
 *    banco.cobol.timeout-ms milisegundos (Future.get con timeout).
 *  - Si se pasa del tiempo, se cancela y se lanza OperacionTimeoutException.
 *
 * Hay un solo hilo para COBOL a propósito: el runtime de GnuCOBOL no está
 * pensado para ejecutar varios programas al mismo tiempo, así que las
 * llamadas se atienden de una en una.
 *
 * Nota: Java no puede detener a la fuerza una función nativa que ya empezó;
 * solo puede dejar de esperarla. Como los programas COBOL solo calculan y
 * no modifican datos (Java es quien guarda), un resultado que llegue tarde
 * simplemente se descarta.
 * El retardo de prueba sí se interrumpe al cancelar (es un Thread.sleep).
 */
@Component
public class EjecutorCobol {

    private static final long RETARDO_MAXIMO_MS = 60_000;

    private final long timeoutMs;
    private volatile long retardoMs;

    private final ExecutorService hiloCobol = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "cobol");
        hilo.setDaemon(true); // no impide que la aplicación se cierre
        return hilo;
    });

    public EjecutorCobol(@Value("${banco.cobol.timeout-ms:3000}") long timeoutMs,
                         @Value("${banco.cobol.retardo-ms:0}") long retardoMs) {
        this.timeoutMs = timeoutMs;
        setRetardoMs(retardoMs);
    }

    /**
     * Ejecuta la llamada con timeout.
     * @param operacion nombre para los mensajes (p. ej. "RETIRAR-DINERO")
     * @param llamada   código que invoca la DLL y devuelve el resultado
     */
    public <T> T ejecutar(String operacion, Callable<T> llamada) {
        Future<T> futuro = hiloCobol.submit(() -> {
            long retardo = retardoMs;
            if (retardo > 0) {
                Thread.sleep(retardo); // retardo de prueba: simula una DLL lenta
            }
            return llamada.call();
        });

        try {
            return futuro.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true); // interrumpe el retardo; el resultado tardío se descarta
            throw new OperacionTimeoutException(operacion, timeoutMs);
        } catch (InterruptedException e) {
            futuro.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La operación " + operacion + " fue interrumpida", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Error ejecutando " + operacion + ": " + causa.getMessage(), causa);
        }
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public long getRetardoMs() {
        return retardoMs;
    }

    /** Permite cambiar el retardo de prueba sin reiniciar (panel de pruebas). */
    public void setRetardoMs(long retardoMs) {
        if (retardoMs < 0 || retardoMs > RETARDO_MAXIMO_MS) {
            throw new IllegalArgumentException("El retardo debe estar entre 0 y " + RETARDO_MAXIMO_MS + " ms");
        }
        this.retardoMs = retardoMs;
    }
}
