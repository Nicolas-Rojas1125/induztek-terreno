package cl.duocuc.induztek.msusuarios.controller;

import cl.duocuc.induztek.msusuarios.dto.PaginaResponse;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.ActualizarPerfilRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.CambiarContrasenaRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.CrearUsuarioRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.EstadoRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.LoginRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.MensajeResponse;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RecuperarRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RecuperarResponse;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RestablecerRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.UsuarioResponse;
import cl.duocuc.induztek.msusuarios.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    public static final String ACTOR = "X-Usuario-Id";

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest req) {
        return UsuarioResponse.de(servicio.login(req.correo(), req.contrasena()));
    }

    @PostMapping("/recuperar")
    public RecuperarResponse recuperar(@Valid @RequestBody RecuperarRequest req) {
        return servicio.recuperar(req.correo());
    }

    @PostMapping("/restablecer")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerRequest req) {
        servicio.restablecer(req);
        return new MensajeResponse("Contraseña restablecida. Ya puede iniciar sesión");
    }

    @GetMapping
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return PaginaResponse.de(servicio.listar(buscar, rol, activo, idCliente, pagina, tamano), UsuarioResponse::de);
    }

    @GetMapping("/directorio")
    public List<UsuarioResponse> directorio() {
        return servicio.directorio().stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return UsuarioResponse.de(servicio.obtener(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestHeader(value = ACTOR, required = false) Long actor,
                                                 @Valid @RequestBody CrearUsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(servicio.crear(actor, req)));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@RequestHeader(value = ACTOR, required = false) Long actor,
                                      @PathVariable Long id,
                                      @Valid @RequestBody ActualizarPerfilRequest req) {
        return UsuarioResponse.de(servicio.actualizarPerfil(actor, id, req));
    }

    @PutMapping("/{id}/contrasena")
    public MensajeResponse cambiarContrasena(@RequestHeader(value = ACTOR, required = false) Long actor,
                                             @PathVariable Long id,
                                             @Valid @RequestBody CambiarContrasenaRequest req) {
        servicio.cambiarContrasena(actor, id, req);
        return new MensajeResponse("Contraseña actualizada");
    }

    @PutMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@RequestHeader(value = ACTOR, required = false) Long actor,
                                         @PathVariable Long id,
                                         @RequestBody EstadoRequest req) {
        return UsuarioResponse.de(servicio.cambiarEstado(actor, id, req.activo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestHeader(value = ACTOR, required = false) Long actor,
                                         @PathVariable Long id) {
        servicio.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }
}
