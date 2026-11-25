package cl.duocuc.induztek.msprotocolos.controller;

import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.VisitaDto;
import cl.duocuc.induztek.msprotocolos.service.VisitaService;
import cl.duocuc.induztek.msprotocolos.dto.PaginaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visitas")
public class VisitaController {
    private final VisitaService servicio;

    public VisitaController(VisitaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PaginaResponse<VisitaDto> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Long idInstalacion,
            @RequestParam(required = false) Boolean abiertas,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return PaginaResponse.de(servicio.listar(buscar, idCliente, idInstalacion, abiertas, pagina, tamano), v -> v);
    }

    @GetMapping("/{uuid}")
    public VisitaDto obtener(@PathVariable String uuid) {
        return servicio.obtener(uuid);
    }

    @PutMapping("/{uuid}")
    public VisitaDto guardar(@PathVariable String uuid, @Valid @RequestBody VisitaDto dto) {
        return servicio.guardar(uuid, dto);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> eliminar(@PathVariable String uuid) {
        servicio.eliminar(uuid);
        return ResponseEntity.noContent().build();
    }
}
