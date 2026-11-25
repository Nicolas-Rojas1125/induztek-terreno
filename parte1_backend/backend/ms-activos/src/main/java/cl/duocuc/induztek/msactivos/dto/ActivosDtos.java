package cl.duocuc.induztek.msactivos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public final class ActivosDtos {
    private ActivosDtos() {
    }

    public record ClienteRequest(
            @NotBlank(message = "El RUT es obligatorio") String rut,
            @NotBlank(message = "La razón social es obligatoria") @Size(min = 3, max = 150, message = "La razón social debe tener entre 3 y 150 caracteres") String razonSocial,
            @NotBlank(message = "El giro es obligatorio") @Size(min = 3, max = 80, message = "El giro debe tener entre 3 y 80 caracteres") String giro,
            @NotBlank(message = "El contacto es obligatorio") @Size(max = 120, message = "El contacto admite máximo 120 caracteres") String contactoNombre,
            @NotBlank(message = "El teléfono es obligatorio") String telefono,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
            @Size(max = 200, message = "La dirección admite máximo 200 caracteres") String direccion) {
    }

    public record ClienteResponse(
            Long id, String rut, String razonSocial, String giro, String contactoNombre, String telefono,
            String correo, String direccion, long cantidadInstalaciones, Long creadoEn, Long actualizadoEn) {
    }

    public record InstalacionRequest(
            @NotNull(message = "El cliente es obligatorio") Long idCliente,
            @NotBlank(message = "El nombre es obligatorio") @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres") String nombre,
            @NotBlank(message = "La ubicación es obligatoria") @Size(max = 200, message = "La ubicación admite máximo 200 caracteres") String ubicacion,
            @NotNull(message = "La comuna es obligatoria") Long idComuna,
            String nombreComuna,
            String nombreRegion,
            @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres") String observacionesAcceso,
            Double latitud,
            Double longitud) {
    }

    public record InstalacionResponse(
            Long id, Long idCliente, String nombreCliente, String nombre, String ubicacion, Long idComuna,
            String nombreComuna, String nombreRegion, String observacionesAcceso, Double latitud, Double longitud,
            long cantidadEquipos, Long actualizadoEn) {
    }

    public record EquipoRequest(
            @NotNull(message = "La instalación es obligatoria") Long idInstalacion,
            @NotBlank(message = "El TAG es obligatorio") String tag,
            @NotBlank(message = "El tipo de equipo es obligatorio") String tipo,
            @NotBlank(message = "La marca es obligatoria") @Size(max = 60, message = "La marca admite máximo 60 caracteres") String marca,
            @Size(max = 60, message = "El modelo admite máximo 60 caracteres") String modelo,
            @NotBlank(message = "El número de serie es obligatorio") @Size(max = 40, message = "El número de serie admite máximo 40 caracteres") String numeroSerie,
            Integer anioFabricacion,
            Double potenciaKva,
            Double tensionPrimariaKv,
            Double tensionSecundariaKv,
            Double corrienteNominalA,
            Double maxContactosUohm,
            @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres") String observaciones,
            Map<String, String> datosTecnicos) {
    }

    public record EquipoResponse(
            Long id, Long idInstalacion, String nombreInstalacion, Long idCliente, String nombreCliente,
            String tag, String tipo, String tipoEtiqueta, String clasificacion, String marca, String modelo,
            String numeroSerie, Integer anioFabricacion, Double potenciaKva, Double tensionPrimariaKv,
            Double tensionSecundariaKv, Double corrienteNominalA, Double maxContactosUohm, String observaciones,
            Map<String, String> datosTecnicos, Long creadoEn, Long actualizadoEn) {
    }

    public record InstrumentoRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 80, message = "El nombre admite máximo 80 caracteres") String nombre,
            @NotBlank(message = "La categoría es obligatoria") String categoria,
            @NotBlank(message = "La marca es obligatoria") String marca,
            @NotBlank(message = "El modelo es obligatorio") String modelo,
            @NotBlank(message = "El número de serie es obligatorio") String numeroSerie,
            String fechaCalibracion,
            String fechaVencimiento,
            String registradaPor) {
    }

    public record CalibracionRequest(
            @NotBlank(message = "La fecha de calibración es obligatoria") String fechaCalibracion,
            @NotBlank(message = "La fecha de vencimiento es obligatoria") String fechaVencimiento,
            Long idUsuarioRegistra,
            String registradaPor) {
    }

    public record CalibracionResponse(Long id, String fechaCalibracion, String fechaVencimiento, String registradaPor) {
    }

    public record InstrumentoResponse(
            Long id, String nombre, String categoria, String marca, String modelo, String numeroSerie,
            String ultimaCalibracion, String vencimiento, String vigencia, List<CalibracionResponse> calibraciones,
            Long actualizadoEn) {
    }

    public record SyncActivosResponse(
            List<ClienteResponse> clientes, List<InstalacionResponse> instalaciones,
            List<EquipoResponse> equipos, List<InstrumentoResponse> instrumentos, long generadoEn) {
    }
}
