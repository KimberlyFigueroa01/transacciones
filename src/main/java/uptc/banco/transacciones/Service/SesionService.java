package uptc.banco.transacciones.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uptc.banco.transacciones.Excepciones.SesionInvalidaException;

/**
 * Sesiones con cierre por inactividad.
 *
 *  - Al ingresar se genera un token (UUID) asociado al usuario.
 *  - Cada petición con ese token renueva la "última actividad".
 *  - Si pasa más de banco.sesion.inactividad-segundos sin actividad,
 *    la sesión se elimina y cualquier petición responde 401.
 *
 * La interfaz cierra la sesión por su cuenta al llegar a cero; el servidor
 * lo verifica igual, para que no dependa solo del navegador. Se da un
 * pequeño margen porque la interfaz informa la actividad cada pocos segundos.
 */
@Service
public class SesionService {

    private static final long MARGEN_MS = 5_000;

    private static class Sesion {
        final int usuarioId;
        volatile long ultimaActividad;

        Sesion(int usuarioId) {
            this.usuarioId = usuarioId;
            this.ultimaActividad = System.currentTimeMillis();
        }
    }

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final long inactividadSegundos;

    public SesionService(@Value("${banco.sesion.inactividad-segundos:60}") long inactividadSegundos) {
        this.inactividadSegundos = inactividadSegundos;
    }

    /** Crea una sesión para el usuario y devuelve su token. */
    public String iniciar(int usuarioId) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, new Sesion(usuarioId));
        return token;
    }

    /**
     * Comprueba el token, renueva la actividad y devuelve el id del usuario.
     * Lanza SesionInvalidaException si no hay sesión o expiró.
     */
    public int validar(String token) {
        if (token == null || token.isBlank()) {
            throw new SesionInvalidaException("Debe iniciar sesión");
        }
        Sesion sesion = sesiones.get(token);
        if (sesion == null) {
            throw new SesionInvalidaException("La sesión no existe o ya fue cerrada");
        }
        long ahora = System.currentTimeMillis();
        if (ahora - sesion.ultimaActividad > inactividadSegundos * 1000 + MARGEN_MS) {
            sesiones.remove(token);
            throw new SesionInvalidaException("La sesión se cerró por inactividad");
        }
        sesion.ultimaActividad = ahora;
        return sesion.usuarioId;
    }

    public void cerrar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    public long getInactividadSegundos() {
        return inactividadSegundos;
    }
}
