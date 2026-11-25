package cl.duocuc.induztek.msprotocolos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.HistorialDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.MedicionDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ProtocoloDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.PruebaDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.RecepcionDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.VisitaDto;
import cl.duocuc.induztek.msprotocolos.exception.ApiException;
import cl.duocuc.induztek.msprotocolos.model.Protocolo;
import cl.duocuc.induztek.msprotocolos.model.Visita;
import cl.duocuc.induztek.msprotocolos.repository.ProtocoloRepository;
import cl.duocuc.induztek.msprotocolos.repository.VisitaRepository;
import cl.duocuc.induztek.msprotocolos.service.ProtocoloService;
import cl.duocuc.induztek.msprotocolos.service.VisitaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProtocolosServiceTest {
    private ProtocoloRepository protocolos;
    private VisitaRepository visitas;
    private ProtocoloService servicio;

    @BeforeEach
    void preparar() {
        protocolos = mock(ProtocoloRepository.class);
        visitas = mock(VisitaRepository.class);
        servicio = new ProtocoloService(protocolos);
        when(protocolos.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(visitas.save(any(Visita.class))).thenAnswer(inv -> inv.getArgument(0));
        when(protocolos.findByUuid(any())).thenReturn(Optional.empty());
        when(visitas.findByUuid(any())).thenReturn(Optional.empty());
    }

    private ProtocoloDto protocolo(String estado, Double temperatura, String tipoPrueba) {
        PruebaDto prueba = new PruebaDto(tipoPrueba, "AT-BT", "UNICA", true, "", 4L, "APROBADO", false, "",
                List.of(new MedicionDto("LECTURA_1MIN", 1500.0), new MedicionDto("LECTURA_10MIN", 3900.0)));
        HistorialDto h = new HistorialDto(null, "BORRADOR", 3L, 1_700_000_000_000L, "");
        return new ProtocoloDto("u-1", "PT-2026-0001-AB12", "v-1", 10L, 5L, 1L, 3L, 1_700_000_000_000L,
                temperatura, 40.0, "", "", "", estado, "APROBADO", null, List.of(prueba), List.of(h), null);
    }

    private VisitaDto visita(String ot, Long cierre, RecepcionDto recepcion) {
        return new VisitaDto("v-1", ot, 5L, 1L, "2026-11-24", "SOLO_PRUEBAS", 2L, cierre, null, null, null,
                List.of(3L, 4L), recepcion, "AB12", null);
    }

    @Test
    void guardarProtocoloNuevoCopiaPruebasYHistorial() {
        ProtocoloDto r = servicio.guardar("u-1", protocolo("REGISTRADO_TERRENO", 27.0, "AISLACION"));
        assertEquals("u-1", r.uuid());
        assertEquals(1, r.pruebas().size());
        assertEquals(2, r.pruebas().get(0).mediciones().size());
        assertEquals(1, r.historial().size());
        assertTrue(r.actualizadoEn() > 0);
    }

    @Test
    void protocoloFormalizadoNoSeModifica() {
        Protocolo existente = new Protocolo();
        existente.setUuid("u-1");
        existente.setNumero("PT-2026-0001-AB12");
        existente.setEstado("FORMALIZADO");
        when(protocolos.findByUuid("u-1")).thenReturn(Optional.of(existente));
        ApiException e = assertThrows(ApiException.class,
                () -> servicio.guardar("u-1", protocolo("OBSERVADO", 27.0, "AISLACION")));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(protocolos, never()).save(any(Protocolo.class));
    }

    @Test
    void reenviarFormalizadoEsIdempotente() {
        Protocolo existente = new Protocolo();
        existente.setUuid("u-1");
        existente.setNumero("PT-2026-0001-AB12");
        existente.setVisitaUuid("v-1");
        existente.setEstado("FORMALIZADO");
        when(protocolos.findByUuid("u-1")).thenReturn(Optional.of(existente));
        assertEquals("FORMALIZADO", servicio.guardar("u-1", protocolo("FORMALIZADO", 27.0, "AISLACION")).estado());
        verify(protocolos, never()).save(any(Protocolo.class));
    }

    @Test
    void temperaturaFueraDeRangoEsRechazada() {
        ApiException e = assertThrows(ApiException.class,
                () -> servicio.guardar("u-1", protocolo("BORRADOR", 80.0, "AISLACION")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void tipoDePruebaDesconocidoEsRechazado() {
        assertThrows(ApiException.class, () -> servicio.guardar("u-1", protocolo("BORRADOR", 20.0, "VIBRACION")));
    }

    @Test
    void numeroDuplicadoEnOtroProtocoloResponde409() {
        when(protocolos.existsByNumeroAndUuidNot("PT-2026-0001-AB12", "u-1")).thenReturn(true);
        ApiException e = assertThrows(ApiException.class,
                () -> servicio.guardar("u-1", protocolo("BORRADOR", 20.0, "AISLACION")));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void soloSeEliminanBorradores() {
        Protocolo existente = new Protocolo();
        existente.setEstado("REGISTRADO_TERRENO");
        when(protocolos.findByUuid("u-1")).thenReturn(Optional.of(existente));
        assertThrows(ApiException.class, () -> servicio.eliminar("u-1"));
    }

    @Test
    void guardarVisitaNormalizaOt() {
        VisitaService vs = new VisitaService(visitas, protocolos);
        VisitaDto r = vs.guardar("v-1", visita("ot-2026-1240", null, null));
        assertEquals("OT-2026-1240", r.numeroOt());
        assertEquals(2, r.participantes().size());
    }

    @Test
    void recepcionSinCierreEsRechazada() {
        VisitaService vs = new VisitaService(visitas, protocolos);
        RecepcionDto rec = new RecepcionDto("Juan Pérez", "11.111.111-1", "Jefe planta", 1L);
        ApiException e = assertThrows(ApiException.class, () -> vs.guardar("v-1", visita("OT-2026-1240", null, rec)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void visitaConOtInvalidaEsRechazada() {
        VisitaService vs = new VisitaService(visitas, protocolos);
        assertThrows(ApiException.class, () -> vs.guardar("v-1", visita("1240", null, null)));
    }

    @Test
    void noSeEliminaVisitaConProtocolos() {
        Visita v = new Visita();
        v.setUuid("v-1");
        when(visitas.findByUuid("v-1")).thenReturn(Optional.of(v));
        when(protocolos.countByVisitaUuid("v-1")).thenReturn(1L);
        assertThrows(ApiException.class, () -> new VisitaService(visitas, protocolos).eliminar("v-1"));
    }
}
