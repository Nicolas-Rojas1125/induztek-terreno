package cl.duocuc.induztek.msactivos.controller;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.EquipoRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.EquipoResponse;
import cl.duocuc.induztek.msactivos.service.EquipoService;
import cl.duocuc.induztek.msactivos.dto.PaginaResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/equipos")
public class EquipoController {
    private final EquipoService servicio;

    public EquipoController(EquipoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PaginaResponse<EquipoResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long idInstalacion,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String clasificacion,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return PaginaResponse.de(servicio.listar(buscar, idInstalacion, idCliente, tipo, clasificacion, pagina, tamano), e -> e);
    }

    @GetMapping("/{id}")
    public EquipoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EquipoResponse> crear(@RequestHeader(value = "X-Usuario-Id", required = false) Long actor,
                                                @Valid @RequestBody EquipoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req, actor));
    }

    @PutMapping("/{id}")
    public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest req) {
        return servicio.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
