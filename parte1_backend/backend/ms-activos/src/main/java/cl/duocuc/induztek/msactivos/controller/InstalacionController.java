package cl.duocuc.induztek.msactivos.controller;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstalacionRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstalacionResponse;
import cl.duocuc.induztek.msactivos.service.InstalacionService;
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
@RequestMapping("/api/instalaciones")
public class InstalacionController {
    private final InstalacionService servicio;

    public InstalacionController(InstalacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PaginaResponse<InstalacionResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return PaginaResponse.de(servicio.listar(buscar, idCliente, pagina, tamano), i -> i);
    }

    @GetMapping("/{id}")
    public InstalacionResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<InstalacionResponse> crear(@RequestHeader(value = "X-Usuario-Id", required = false) Long actor,
                                                     @Valid @RequestBody InstalacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req));
    }

    @PutMapping("/{id}")
    public InstalacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody InstalacionRequest req) {
        return servicio.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
