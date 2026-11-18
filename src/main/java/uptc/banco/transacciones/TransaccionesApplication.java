package uptc.banco.transacciones;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import uptc.banco.transacciones.Service.LibCob;

@SpringBootApplication
public class TransaccionesApplication {

    static {
        System.setProperty("jna.library.path", rutasLibrerias());
    }

    /**
     * Carpetas donde JNA busca libcob-4.dll (runtime de GnuCOBOL) y transaccion.dll.
     * Orden:
     *  1. Variable de entorno GNUCOBOL_BIN (opcional, para usar otra instalación).
     *  2. Carpeta "native" del proyecto (incluida en el repositorio: funciona al clonar).
     *  3. Instalación típica de MSYS2 (C:/msys64/ucrt64/bin).
     *  4. Carpeta actual y superior (compatibilidad con la versión anterior).
     */
    private static String rutasLibrerias() {
        Set<String> rutas = new LinkedHashSet<>();
        String gnucobol = System.getenv("GNUCOBOL_BIN");
        if (gnucobol != null && !gnucobol.isBlank()) {
            rutas.add(gnucobol);
        }
        rutas.add(new File("native").getAbsolutePath());
        rutas.add("C:/msys64/ucrt64/bin");
        rutas.add(".");
        rutas.add("..");
        String previa = System.getProperty("jna.library.path");
        if (previa != null && !previa.isBlank()) {
            rutas.add(previa);
        }
        return String.join(File.pathSeparator, rutas);
    }

    public static void main(String[] args) {
        // El runtime de GnuCOBOL debe inicializarse antes de llamar a cualquier programa COBOL
        LibCob.INSTANCE.cob_init(0, null);
        SpringApplication.run(TransaccionesApplication.class, args);
        System.out.println(">> Interfaz disponible en http://localhost:8080");
    }
}
