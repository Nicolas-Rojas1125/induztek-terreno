package cl.duocuc.induztek.msprotocolos.controller;

import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.SyncProtocolosResponse;
import cl.duocuc.induztek.msprotocolos.service.ProtocoloService;
import cl.duocuc.induztek.msprotocolos.service.VisitaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync/protocolos")
public class SyncController {
    private final VisitaService visitas;
    private final ProtocoloService protocolos;

    public SyncController(VisitaService visitas, ProtocoloService protocolos) {
        this.visitas = visitas;
        this.protocolos = protocolos;
    }

    @GetMapping
    public SyncProtocolosResponse cambios(@RequestParam(defaultValue = "0") long desde,
                                          @RequestParam(required = false) Long idCliente) {
        long ahora = System.currentTimeMillis();
        return new SyncProtocolosResponse(
                visitas.cambiadasDesde(desde, idCliente),
                protocolos.cambiadosDesde(desde, idCliente),
                ahora);
    }
}
