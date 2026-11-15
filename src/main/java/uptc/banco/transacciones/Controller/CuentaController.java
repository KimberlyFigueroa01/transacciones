package uptc.banco.transacciones.Controller;

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
 * Operaciones sobre la cuenta del usuario que inició sesión.
 * Todas exigen el header X-Sesion con el token recibido al ingresar.
 *   GET  /api/cuenta           → datos del usuario (saldo e historial)
 *   POST /api/cuenta/retirar   → body {"monto": 250.0}; devuelve el usuario actualizado
 */
@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

    public record RetiroRequest(double monto) {}

    private final CuentaService cuentaService;
    private final SesionService sesionService;

    public CuentaController(CuentaService cuentaService, SesionService sesionService) {
        this.cuentaService = cuentaService;
        this.sesionService = sesionService;
    }

    @GetMapping
    public Usuario miCuenta(@RequestHeader(value = "X-Sesion", required = false) String token) {
        int idUsuario = sesionService.validar(token);
        return cuentaService.obtenerUsuario(idUsuario);
    }

    @PostMapping("/retirar")
    public Usuario retirar(@RequestHeader(value = "X-Sesion", required = false) String token,
                           @RequestBody RetiroRequest request) {
        int idUsuario = sesionService.validar(token);
        return cuentaService.retirar(idUsuario, request.monto());
    }
}
