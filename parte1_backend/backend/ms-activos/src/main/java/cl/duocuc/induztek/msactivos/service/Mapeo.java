package cl.duocuc.induztek.msactivos.service;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.CalibracionResponse;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.ClienteResponse;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.EquipoResponse;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstalacionResponse;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstrumentoResponse;
import cl.duocuc.induztek.msactivos.model.Calibracion;
import cl.duocuc.induztek.msactivos.model.Cliente;
import cl.duocuc.induztek.msactivos.model.Equipo;
import cl.duocuc.induztek.msactivos.model.Instalacion;
import cl.duocuc.induztek.msactivos.model.Instrumento;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;

public final class Mapeo {
    public static final long DIAS_AVISO = 30;

    private Mapeo() {
    }

    public static ClienteResponse cliente(Cliente c, long cantidadInstalaciones) {
        return new ClienteResponse(c.getId(), c.getRut(), c.getRazonSocial(), c.getGiro(), c.getContactoNombre(),
                c.getTelefono(), c.getCorreo(), c.getDireccion(), cantidadInstalaciones, c.getCreadoEn(), c.getActualizadoEn());
    }

    public static InstalacionResponse instalacion(Instalacion i, long cantidadEquipos) {
        return new InstalacionResponse(i.getId(), i.getCliente().getId(), i.getCliente().getRazonSocial(), i.getNombre(),
                i.getUbicacion(), i.getIdComuna(), i.getNombreComuna(), i.getNombreRegion(), i.getObservacionesAcceso(),
                i.getLatitud(), i.getLongitud(), cantidadEquipos, i.getActualizadoEn());
    }

    public static EquipoResponse equipo(Equipo e) {
        Instalacion i = e.getInstalacion();
        return new EquipoResponse(e.getId(), i.getId(), i.getNombre(), i.getCliente().getId(), i.getCliente().getRazonSocial(),
                e.getTag(), e.getTipo().name(), e.getTipo().getEtiqueta(), e.getTipo().getClasificacion(), e.getMarca(),
                e.getModelo(), e.getNumeroSerie(), e.getAnioFabricacion(), e.getPotenciaKva(), e.getTensionPrimariaKv(),
                e.getTensionSecundariaKv(), e.getCorrienteNominalA(), e.getMaxContactosUohm(), e.getObservaciones(),
                new LinkedHashMap<>(e.getDatosTecnicos()), e.getCreadoEn(), e.getActualizadoEn());
    }

    public static CalibracionResponse calibracion(Calibracion c) {
        return new CalibracionResponse(c.getId(), c.getFechaCalibracion().toString(), c.getFechaVencimiento().toString(),
                c.getRegistradaPor());
    }

    public static InstrumentoResponse instrumento(Instrumento i) {
        return new InstrumentoResponse(i.getId(), i.getNombre(), i.getCategoria().name(), i.getMarca(), i.getModelo(),
                i.getNumeroSerie(), texto(i.getUltimaCalibracion()), texto(i.getVencimiento()),
                vigencia(i.getVencimiento(), LocalDate.now()),
                i.getCalibraciones().stream().map(Mapeo::calibracion).toList(), i.getActualizadoEn());
    }

    public static String vigencia(LocalDate vencimiento, LocalDate hoy) {
        if (vencimiento == null) {
            return "SIN_CALIBRACION";
        }
        if (vencimiento.isBefore(hoy)) {
            return "VENCIDO";
        }
        if (ChronoUnit.DAYS.between(hoy, vencimiento) <= DIAS_AVISO) {
            return "POR_VENCER";
        }
        return "VIGENTE";
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }
}
