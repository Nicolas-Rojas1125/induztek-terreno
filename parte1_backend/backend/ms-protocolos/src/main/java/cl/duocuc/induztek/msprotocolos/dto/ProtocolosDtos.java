package cl.duocuc.induztek.msprotocolos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public final class ProtocolosDtos {
    private ProtocolosDtos() {
    }

    public record RecepcionDto(String receptorNombre, String receptorRut, String receptorCargo, Long fechaHoraRecepcion) {
    }

    public record VisitaDto(
            String uuid,
            @NotBlank(message = "El número de OT es obligatorio") String numeroOt,
            @NotNull(message = "La instalación es obligatoria") Long idInstalacion,
            @NotNull(message = "El cliente es obligatorio") Long idCliente,
            @NotBlank(message = "La fecha es obligatoria") String fecha,
            @NotBlank(message = "El tipo de visita es obligatorio") String tipoVisita,
            @NotNull(message = "El ingeniero a cargo es obligatorio") Long idIngenieroCargo,
            Long fechaHoraCierre,
            Double latitudCierre,
            Double longitudCierre,
            Long idUsuarioCierra,
            List<Long> participantes,
            RecepcionDto recepcion,
            String origen,
            Long actualizadoEn) {
    }

    public record MedicionDto(String parametro, Double valor) {
    }

    public record PruebaDto(
            String tipoPrueba, String elemento, String momento, boolean realizada, String motivoNoRealizada,
            Long idInstrumento, String resultado, boolean noOpera, String comentario, List<MedicionDto> mediciones) {
    }

    public record HistorialDto(String estadoAnterior, String estadoNuevo, Long idUsuario, Long fechaHora, String motivo) {
    }

    public record ProtocoloDto(
            String uuid,
            @NotBlank(message = "El número de protocolo es obligatorio") String numero,
            @NotBlank(message = "La visita es obligatoria") String visitaUuid,
            @NotNull(message = "El equipo es obligatorio") Long idEquipo,
            @NotNull(message = "La instalación es obligatoria") Long idInstalacion,
            @NotNull(message = "El cliente es obligatorio") Long idCliente,
            @NotNull(message = "El usuario que registra es obligatorio") Long idUsuarioRegistra,
            @NotNull(message = "La fecha de la prueba es obligatoria") Long fechaHoraPrueba,
            Double temperaturaC,
            Double humedadPct,
            String descripcionMantenimiento,
            String observaciones,
            String comentarioVariacion,
            @NotBlank(message = "El estado es obligatorio") String estado,
            String resultadoGlobal,
            String reemplazaUuid,
            @Valid List<PruebaDto> pruebas,
            List<HistorialDto> historial,
            Long actualizadoEn) {
    }

    public record ProtocoloResumenDto(
            String uuid, String numero, String visitaUuid, Long idEquipo, Long idInstalacion, Long idCliente,
            Long idUsuarioRegistra, Long fechaHoraPrueba, String estado, String resultadoGlobal, int cantidadPruebas,
            Long actualizadoEn) {
    }

    public record SyncProtocolosResponse(List<VisitaDto> visitas, List<ProtocoloDto> protocolos, long servidorMillis) {
    }

    public record ResumenEstadosDto(Map<String, Long> porEstado, long total) {
    }
}
