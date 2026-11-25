package cl.duocuc.induztek.msprotocolos.service;

import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.HistorialDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.MedicionDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ProtocoloDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ProtocoloResumenDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.PruebaDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.RecepcionDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.VisitaDto;
import cl.duocuc.induztek.msprotocolos.model.CambioEstado;
import cl.duocuc.induztek.msprotocolos.model.Medicion;
import cl.duocuc.induztek.msprotocolos.model.Protocolo;
import cl.duocuc.induztek.msprotocolos.model.Prueba;
import cl.duocuc.induztek.msprotocolos.model.Visita;
import java.util.ArrayList;
import java.util.List;

public final class Conversion {
    private Conversion() {
    }

    public static VisitaDto visita(Visita v) {
        RecepcionDto recepcion = v.getFechaHoraRecepcion() == null ? null
                : new RecepcionDto(v.getReceptorNombre(), v.getReceptorRut(), v.getReceptorCargo(), v.getFechaHoraRecepcion());
        return new VisitaDto(v.getUuid(), v.getNumeroOt(), v.getIdInstalacion(), v.getIdCliente(), v.getFecha().toString(),
                v.getTipoVisita(), v.getIdIngenieroCargo(), v.getFechaHoraCierre(), v.getLatitudCierre(),
                v.getLongitudCierre(), v.getIdUsuarioCierra(), new ArrayList<>(v.getParticipantes()), recepcion,
                v.getOrigen(), v.getActualizadoEn());
    }

    public static ProtocoloDto protocolo(Protocolo p) {
        List<PruebaDto> pruebas = p.getPruebas().stream().map(Conversion::prueba).toList();
        List<HistorialDto> historial = p.getHistorial().stream()
                .map(h -> new HistorialDto(h.getEstadoAnterior(), h.getEstadoNuevo(), h.getIdUsuario(), h.getFechaHora(), h.getMotivo()))
                .toList();
        return new ProtocoloDto(p.getUuid(), p.getNumero(), p.getVisitaUuid(), p.getIdEquipo(), p.getIdInstalacion(),
                p.getIdCliente(), p.getIdUsuarioRegistra(), p.getFechaHoraPrueba(), p.getTemperaturaC(), p.getHumedadPct(),
                p.getDescripcionMantenimiento(), p.getObservaciones(), p.getComentarioVariacion(), p.getEstado(),
                p.getResultadoGlobal(), p.getReemplazaUuid(), pruebas, historial, p.getActualizadoEn());
    }

    public static ProtocoloResumenDto resumen(Protocolo p) {
        return new ProtocoloResumenDto(p.getUuid(), p.getNumero(), p.getVisitaUuid(), p.getIdEquipo(), p.getIdInstalacion(),
                p.getIdCliente(), p.getIdUsuarioRegistra(), p.getFechaHoraPrueba(), p.getEstado(), p.getResultadoGlobal(),
                p.getPruebas().size(), p.getActualizadoEn());
    }

    static PruebaDto prueba(Prueba p) {
        List<MedicionDto> mediciones = p.getMediciones().stream()
                .map(m -> new MedicionDto(m.getParametro(), m.getValor()))
                .toList();
        return new PruebaDto(p.getTipoPrueba(), p.getElemento(), p.getMomento(), p.isRealizada(), p.getMotivoNoRealizada(),
                p.getIdInstrumento(), p.getResultado(), p.isNoOpera(), p.getComentario(), mediciones);
    }

    static Prueba prueba(PruebaDto d) {
        Prueba p = new Prueba();
        p.setTipoPrueba(d.tipoPrueba());
        p.setElemento(d.elemento());
        p.setMomento(d.momento() == null ? "UNICA" : d.momento());
        p.setRealizada(d.realizada());
        p.setMotivoNoRealizada(texto(d.motivoNoRealizada()));
        p.setIdInstrumento(d.idInstrumento());
        p.setResultado(d.resultado());
        p.setNoOpera(d.noOpera());
        p.setComentario(texto(d.comentario()));
        if (d.mediciones() != null) {
            for (MedicionDto m : d.mediciones()) {
                p.getMediciones().add(new Medicion(m.parametro(), m.valor()));
            }
        }
        return p;
    }

    static CambioEstado cambio(HistorialDto h) {
        return new CambioEstado(h.estadoAnterior(), h.estadoNuevo(), h.idUsuario(), h.fechaHora(), h.motivo());
    }

    static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
