package uptc.banco.transacciones.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uptc.banco.transacciones.Model.ResultadoOperacion;
import uptc.banco.transacciones.Model.Usuario;
import uptc.banco.transacciones.Service.CuentaService;
import uptc.banco.transacciones.Service.SesionService;

/**
 * Operaciones sobre la cuenta del usuario que inició sesión.
 * Todas exigen el header X-Sesion con el token recibido al ingresar.
 *   GET  /api/cuenta                  → datos del usuario (saldo e historial)
 *   POST /api/cuenta/retirar          → {"monto": 250.0}
 *   POST /api/cuenta/consignar        → {"monto": 250.0}
 *   POST /api/cuenta/transferir       → {"telefonoDestino": "3104445566", "monto": 100.0}
 *   POST /api/cuenta/pagar-servicio   → {"servicio": "LUZ", "referencia": "12345", "valor": 80.0}
 *   POST /api/cuenta/recargar         → {"celular": "3001234567", "monto": 20.0}
 * Las operaciones devuelven {usuario, estado, codigoCobol, mensaje}.
 */
@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

    public record MontoRequest(double monto) {}

    public record TransferenciaRequest(String telefonoDestino, double monto) {}

    public record PagoServicioRequest(String servicio, String referencia, double valor) {}

    public record RecargaRequest(String celular, double monto) {}

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
    public ResultadoOperacion retirar(@RequestHeader(value = "X-Sesion", required = false) String token,
                                      @RequestBody MontoRequest request) {
        int idUsuario = sesionService.validar(token);
        return cuentaService.retirar(idUsuario, request.monto());
    }

    @PostMapping("/consignar")
    public ResultadoOperacion consignar(@RequestHeader(value = "X-Sesion", required = false) String token,
                                        @RequestBody MontoRequest request) {
        int idUsuario = sesionService.validar(token);
        return cuentaService.consignar(idUsuario, request.monto());
    }

    @PostMapping("/transferir")
    public ResultadoOperacion transferir(@RequestHeader(value = "X-Sesion", required = false) String token,
                                         @RequestBody TransferenciaRequest request) {
        int idUsuario = sesionService.validar(token);
        return cuentaService.transferir(idUsuario, request.telefonoDestino(), request.monto());
    }

    @PostMapping("/pagar-servicio")
    public ResultadoOperacion pagarServicio(@RequestHeader(value = "X-Sesion", required = false) String token,
                                            @RequestBody PagoServicioRequest request) {
        int idUsuario = sesionService.validar(token);
        return cuentaService.pagarServicio(idUsuario, request.servicio(), request.referencia(), request.valor());
    }

    @PostMapping("/recargar")
    public ResultadoOperacion recargar(@RequestHeader(value = "X-Sesion", required = false) String token,
                                       @RequestBody RecargaRequest request) {
        int idUsuario = sesionService.validar(token);
        return cuentaService.recargarCelular(idUsuario, request.celular(), request.monto());
    }
}
