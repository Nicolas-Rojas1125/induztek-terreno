package cl.duocuc.induztek.msactivos.controller;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.SyncActivosResponse;
import cl.duocuc.induztek.msactivos.service.ClienteService;
import cl.duocuc.induztek.msactivos.service.EquipoService;
import cl.duocuc.induztek.msactivos.service.InstalacionService;
import cl.duocuc.induztek.msactivos.service.InstrumentoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync/activos")
public class SyncController {
    private final ClienteService clientes;
    private final InstalacionService instalaciones;
    private final EquipoService equipos;
    private final InstrumentoService instrumentos;

    public SyncController(ClienteService clientes, InstalacionService instalaciones,
                          EquipoService equipos, InstrumentoService instrumentos) {
        this.clientes = clientes;
        this.instalaciones = instalaciones;
        this.equipos = equipos;
        this.instrumentos = instrumentos;
    }

    @GetMapping
    public SyncActivosResponse exportar(@RequestParam(required = false) Long idCliente) {
        return new SyncActivosResponse(
                clientes.todos(idCliente),
                instalaciones.todas(idCliente),
                equipos.todos(idCliente),
                instrumentos.todos(),
                System.currentTimeMillis());
    }
}
