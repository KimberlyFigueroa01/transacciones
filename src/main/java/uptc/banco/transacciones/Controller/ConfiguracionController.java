package uptc.banco.transacciones.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uptc.banco.transacciones.Service.EjecutorCobol;
import uptc.banco.transacciones.Service.SesionService;

/**
 * Valores de timeout que usa la interfaz y panel de pruebas.
 *   GET /api/configuracion           → timeouts actuales
 *   PUT /api/configuracion/retardo   → body {"retardoMs": 4000}; cambia el retardo de prueba
 */
@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    public record Configuracion(long timeoutCobolMs, long retardoCobolMs, long inactividadSegundos) {}

    public record RetardoRequest(long retardoMs) {}

    private final EjecutorCobol ejecutorCobol;
    private final SesionService sesionService;

    public ConfiguracionController(EjecutorCobol ejecutorCobol, SesionService sesionService) {
        this.ejecutorCobol = ejecutorCobol;
        this.sesionService = sesionService;
    }

    @GetMapping
    public Configuracion obtener() {
        return new Configuracion(ejecutorCobol.getTimeoutMs(), ejecutorCobol.getRetardoMs(),
                sesionService.getInactividadSegundos());
    }

    @PutMapping("/retardo")
    public Configuracion cambiarRetardo(@RequestBody RetardoRequest request) {
        ejecutorCobol.setRetardoMs(request.retardoMs());
        return obtener();
    }
}
