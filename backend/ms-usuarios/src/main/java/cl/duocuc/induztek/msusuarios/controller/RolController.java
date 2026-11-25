package cl.duocuc.induztek.msusuarios.controller;

import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RolResponse;
import cl.duocuc.induztek.msusuarios.model.Rol;
import cl.duocuc.induztek.msusuarios.service.UsuarioService;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RolController {
    private final UsuarioService servicio;

    public RolController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RolResponse> listar() {
        return Arrays.stream(Rol.values()).map(r -> new RolResponse(r.name(), r.getEtiqueta())).toList();
    }

    @GetMapping("/creables")
    public List<RolResponse> creables(@RequestHeader(value = UsuarioController.ACTOR, required = false) Long actor) {
        return servicio.rolesCreables(actor).stream().map(r -> new RolResponse(r.name(), r.getEtiqueta())).toList();
    }
}
