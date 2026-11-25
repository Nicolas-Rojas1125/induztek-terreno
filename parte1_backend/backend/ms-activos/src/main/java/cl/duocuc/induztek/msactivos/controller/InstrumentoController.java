package cl.duocuc.induztek.msactivos.controller;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.CalibracionRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstrumentoRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstrumentoResponse;
import cl.duocuc.induztek.msactivos.service.InstrumentoService;
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
@RequestMapping("/api/instrumentos")
public class InstrumentoController {
    private final InstrumentoService servicio;

    public InstrumentoController(InstrumentoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PaginaResponse<InstrumentoResponse> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String vigencia,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return PaginaResponse.de(servicio.listar(buscar, categoria, vigencia, pagina, tamano), i -> i);
    }

    @GetMapping("/{id}")
    public InstrumentoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<InstrumentoResponse> crear(@RequestHeader(value = "X-Usuario-Id", required = false) Long actor,
                                                     @Valid @RequestBody InstrumentoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req, actor));
    }

    @PutMapping("/{id}")
    public InstrumentoResponse actualizar(@PathVariable Long id, @Valid @RequestBody InstrumentoRequest req) {
        return servicio.actualizar(id, req);
    }

    @PostMapping("/{id}/calibraciones")
    public ResponseEntity<InstrumentoResponse> calibrar(@PathVariable Long id, @Valid @RequestBody CalibracionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.agregarCalibracion(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
