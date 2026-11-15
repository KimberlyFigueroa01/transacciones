package uptc.banco.transacciones.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

// Spring Boot 4 trae Jackson 3 (paquete "tools.jackson").
// Si al compilar dice que el paquete no existe, cambia estas dos líneas por
// com.fasterxml.jackson.databind.SerializationFeature y
// com.fasterxml.jackson.databind.json.JsonMapper (Jackson 2).
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import uptc.banco.transacciones.Model.Usuario;

/**
 * Persistencia de los usuarios en un archivo JSON.
 *
 * Funcionamiento:
 *  - Al arrancar, lee el archivo y deja los datos en memoria.
 *    Si el archivo no existe (o está vacío), lo crea con usuarios de ejemplo.
 *  - Las consultas (listar, buscar) se responden desde memoria.
 *  - Cada cambio (crear, actualizar, eliminar) se aplica en memoria y
 *    enseguida se escribe el archivo completo.
 *
 * Estructura del archivo:
 *  { "usuarios": [ { "id": 1, "nombre": ..., "historial": [ ... ] }, ... ] }
 */
@Repository
public class BancoRepository {

    /** Raíz del archivo JSON. */
    public static class DatosBanco {
        private List<Usuario> usuarios = new ArrayList<>();

        public List<Usuario> getUsuarios() {
            return usuarios;
        }

        public void setUsuarios(List<Usuario> usuarios) {
            this.usuarios = (usuarios != null) ? usuarios : new ArrayList<>();
        }
    }

    private final Path archivo;
    private final JsonMapper mapper;
    private DatosBanco datos;

    /**
     * La ruta se toma de application.properties (banco.archivo).
     * Si no está definida, usa data/banco.json relativo a la carpeta
     * desde donde se ejecuta la aplicación.
     */
    public BancoRepository(@Value("${banco.archivo:data/banco.json}") String rutaArchivo) {
        this.archivo = Paths.get(rutaArchivo).toAbsolutePath();
        this.mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT) // JSON legible, con saltos de línea
                .build();
        cargar();
    }

    // =========================================================
    // CONSULTAR
    // =========================================================

    /** Devuelve todos los usuarios. */
    public synchronized List<Usuario> listar() {
        return new ArrayList<>(datos.getUsuarios());
    }

    /** Busca un usuario por su id. */
    public synchronized Optional<Usuario> buscarPorId(int id) {
        return datos.getUsuarios().stream()
                .filter(u -> u.getId() == id)
                .findFirst();
    }

    /** Busca un usuario por su teléfono. */
    public synchronized Optional<Usuario> buscarPorTelefono(String telefono) {
        return datos.getUsuarios().stream()
                .filter(u -> u.getTelefono() != null && u.getTelefono().equals(telefono))
                .findFirst();
    }

    // =========================================================
    // CREAR
    // =========================================================

    /**
     * Agrega un usuario nuevo. El id lo asigna el repositorio
     * (cualquier id que traiga el objeto se ignora).
     */
    public synchronized Usuario crear(Usuario usuario) {
        if (buscarPorTelefono(usuario.getTelefono()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con el teléfono " + usuario.getTelefono());
        }
        usuario.setId(siguienteId());
        if (usuario.getHistorial() == null) {
            usuario.setHistorial(new ArrayList<>());
        }
        datos.getUsuarios().add(usuario);
        guardar();
        return usuario;
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    /**
     * Reemplaza el usuario que tenga el mismo id.
     * Úsalo después de cambiar el saldo o agregar un movimiento al historial.
     */
    public synchronized Usuario actualizar(Usuario usuario) {
        int posicion = posicionDe(usuario.getId());
        if (usuario.getHistorial() == null) {
            usuario.setHistorial(new ArrayList<>());
        }
        datos.getUsuarios().set(posicion, usuario);
        guardar();
        return usuario;
    }

    /**
     * Actualiza varios usuarios con UNA sola escritura del archivo.
     * Pensado para transferencias: el que envía y el que recibe
     * se guardan juntos, o no se guarda ninguno.
     */
    public synchronized void actualizarVarios(Usuario... usuarios) {
        // Primero se valida que todos existan, para no dejar cambios a medias
        int[] posiciones = new int[usuarios.length];
        for (int i = 0; i < usuarios.length; i++) {
            posiciones[i] = posicionDe(usuarios[i].getId());
        }
        for (int i = 0; i < usuarios.length; i++) {
            datos.getUsuarios().set(posiciones[i], usuarios[i]);
        }
        guardar();
    }

    // =========================================================
    // ELIMINAR
    // =========================================================

    /** Elimina el usuario con ese id. Devuelve false si no existía. */
    public synchronized boolean eliminar(int id) {
        boolean eliminado = datos.getUsuarios().removeIf(u -> u.getId() == id);
        if (eliminado) {
            guardar();
        }
        return eliminado;
    }

    // =========================================================
    // MANEJO DEL ARCHIVO
    // =========================================================

    /** Lee el archivo; si no existe o está vacío, lo crea con datos de ejemplo. */
    private void cargar() {
        try {
            if (Files.exists(archivo) && Files.size(archivo) > 0) {
                datos = mapper.readValue(archivo.toFile(), DatosBanco.class);
                if (datos == null) {
                    datos = new DatosBanco();
                }
                System.out.println(">> Datos cargados desde " + archivo
                        + " (" + datos.getUsuarios().size() + " usuarios)");
            } else {
                datos = new DatosBanco();
                datos.getUsuarios().addAll(usuariosIniciales());
                guardar();
                System.out.println(">> Archivo creado con datos de ejemplo: " + archivo);
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(
                    "No se pudo leer " + archivo + ". Revisa que sea un JSON válido: " + e.getMessage(), e);
        }
    }

    /**
     * Escribe todos los datos en el archivo.
     * Se escribe primero en "banco.json.tmp" y luego se reemplaza el original,
     * así un cierre inesperado a mitad de escritura no deja el JSON dañado.
     */
    private void guardar() {
        try {
            Path carpeta = archivo.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            mapper.writeValue(temporal.toFile(), datos);
            try {
                Files.move(temporal, archivo,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo, e);
        }
    }

    /** Posición del usuario en la lista, o excepción si no existe. */
    private int posicionDe(int id) {
        List<Usuario> usuarios = datos.getUsuarios();
        for (int i = 0; i < usuarios.size(); i++) {
            if (usuarios.get(i).getId() == id) {
                return i;
            }
        }
        throw new NoSuchElementException("No existe el usuario con id " + id);
    }

    /** Siguiente id disponible: el mayor actual + 1. */
    private int siguienteId() {
        return datos.getUsuarios().stream()
                .mapToInt(Usuario::getId)
                .max()
                .orElse(0) + 1;
    }

    /** Usuarios con los que se crea el archivo la primera vez. */
    private List<Usuario> usuariosIniciales() {
        List<Usuario> iniciales = new ArrayList<>();
        iniciales.add(new Usuario(1, "Ana", "Gómez", "3001112233", 1000.00, new ArrayList<>()));
        iniciales.add(new Usuario(2, "Luis", "Pérez", "3104445566", 500.00, new ArrayList<>()));
        iniciales.add(new Usuario(3, "María", "Rojas", "3207778899", 1500.00, new ArrayList<>()));
        return iniciales;
    }
}
