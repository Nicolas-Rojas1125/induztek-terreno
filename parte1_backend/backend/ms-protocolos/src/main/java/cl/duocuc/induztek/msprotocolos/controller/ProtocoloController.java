package cl.duocuc.induztek.msprotocolos.controller;

import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ProtocoloDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ProtocoloResumenDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ResumenEstadosDto;
import cl.duocuc.induztek.msprotocolos.service.ProtocoloService;
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
@RequestMapping("/api/protocolos")
public class ProtocoloController {
    private final ProtocoloService servicio;

    public ProtocoloController(ProtocoloService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PaginaResponse<ProtocoloResumenDto> listar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Long idEquipo,
            @RequestParam(required = false) String visitaUuid,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return PaginaResponse.de(
                servicio.listar(buscar, estado, resultado, idCliente, idEquipo, visitaUuid, pagina, tamano), p -> p);
    }

    @GetMapping("/resumen")
    public ResumenEstadosDto resumen(@RequestParam(required = false) Long idCliente) {
        return servicio.resumenEstados(idCliente);
    }

    @GetMapping("/{uuid}")
    public ProtocoloDto obtener(@PathVariable String uuid) {
        return servicio.obtener(uuid);
    }

    @PutMapping("/{uuid}")
    public ProtocoloDto guardar(@PathVariable String uuid, @Valid @RequestBody ProtocoloDto dto) {
        return servicio.guardar(uuid, dto);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> eliminar(@PathVariable String uuid) {
        servicio.eliminar(uuid);
        return ResponseEntity.noContent().build();
    }
}
