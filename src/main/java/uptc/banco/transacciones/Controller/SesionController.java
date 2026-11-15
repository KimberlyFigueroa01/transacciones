package uptc.banco.transacciones.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uptc.banco.transacciones.Model.Usuario;
import uptc.banco.transacciones.Service.CuentaService;
import uptc.banco.transacciones.Service.SesionService;

/**
 *   POST   /api/sesion                 → body {"telefono": "..."}; devuelve token y usuario
 *   POST   /api/sesion/ping            → renueva la actividad (header X-Sesion)
 *   DELETE /api/sesion                 → cierra la sesión (header X-Sesion)
 *   GET    /api/sesion/usuarios-demo   → nombres y teléfonos para la pantalla de ingreso
 */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    public record IngresoRequest(String telefono) {}

    public record IngresoResponse(String token, Usuario usuario, long inactividadSegundos) {}

    public record UsuarioDemo(String nombre, String apellido, String telefono) {}

    private final SesionService sesionService;
    private final CuentaService cuentaService;

    public SesionController(SesionService sesionService, CuentaService cuentaService) {
        this.sesionService = sesionService;
        this.cuentaService = cuentaService;
    }

    @PostMapping
    public IngresoResponse ingresar(@RequestBody IngresoRequest request) {
        String telefono = request.telefono() == null ? "" : request.telefono().trim();
        if (telefono.isEmpty()) {
            throw new IllegalArgumentException("Ingrese un número de teléfono");
        }
        Usuario usuario = cuentaService.buscarPorTelefono(telefono);
        String token = sesionService.iniciar(usuario.getId());
        return new IngresoResponse(token, usuario, sesionService.getInactividadSegundos());
    }

    @PostMapping("/ping")
    public ResponseEntity<Void> ping(@RequestHeader(value = "X-Sesion", required = false) String token) {
        sesionService.validar(token);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> salir(@RequestHeader(value = "X-Sesion", required = false) String token) {
        sesionService.cerrar(token);
        return ResponseEntity.noContent().build();
    }

    /** Solo para la demo: ayuda a elegir un usuario en la pantalla de ingreso (sin saldos). */
    @GetMapping("/usuarios-demo")
    public List<UsuarioDemo> usuariosDemo() {
        return cuentaService.listarUsuarios().stream()
                .map(u -> new UsuarioDemo(u.getNombre(), u.getApellido(), u.getTelefono()))
                .toList();
    }

}
