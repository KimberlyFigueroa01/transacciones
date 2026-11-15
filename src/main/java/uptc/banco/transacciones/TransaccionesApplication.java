package uptc.banco.transacciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import uptc.banco.transacciones.Service.LibCob;

@SpringBootApplication
public class TransaccionesApplication {

    static {
        // Dónde busca JNA libcob-4.dll (MSYS2) y transaccion.dll (carpeta actual o superior)
        System.setProperty("jna.library.path", "C:/msys64/ucrt64/bin;.;..");
    }

    public static void main(String[] args) {
        // El runtime de GnuCOBOL debe inicializarse antes de llamar a cualquier programa COBOL
        LibCob.INSTANCE.cob_init(0, null);
        SpringApplication.run(TransaccionesApplication.class, args);
        System.out.println(">> Interfaz disponible en http://localhost:8080");
    }
}
