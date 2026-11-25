package cl.duocuc.induztek.msusuarios.dto;

import cl.duocuc.induztek.msusuarios.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UsuarioDtos {
    private UsuarioDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String contrasena) {
    }

    public record CrearUsuarioRequest(
            @NotBlank(message = "El RUT es obligatorio") String rut,
            @NotBlank(message = "El nombre es obligatorio") @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
            @Size(max = 20, message = "El teléfono admite máximo 20 caracteres") String telefono,
            @Size(max = 80, message = "El cargo admite máximo 80 caracteres") String cargo,
            @NotBlank(message = "El rol es obligatorio") String rol,
            Long idCliente,
            String nombreCliente,
            @NotBlank(message = "La contraseña es obligatoria") String contrasena) {
    }

    public record ActualizarPerfilRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
            @Size(max = 20, message = "El teléfono admite máximo 20 caracteres") String telefono,
            @Size(max = 80, message = "El cargo admite máximo 80 caracteres") String cargo) {
    }

    public record CambiarContrasenaRequest(
            @NotBlank(message = "Ingrese su contraseña actual") String actual,
            @NotBlank(message = "Ingrese la nueva contraseña") String nueva) {
    }

    public record RecuperarRequest(
            @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo) {
    }

    public record RecuperarResponse(String message, String codigoDemo, Long expiraEn) {
    }

    public record RestablecerRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "El código es obligatorio") String codigo,
            @NotBlank(message = "Ingrese la nueva contraseña") String nueva) {
    }

    public record EstadoRequest(boolean activo) {
    }

    public record MensajeResponse(String message) {
    }

    public record RolResponse(String codigo, String etiqueta) {
    }

    public record UsuarioResponse(
            Long id, String rut, String nombre, String correo, String telefono, String cargo,
            String rol, String rolEtiqueta, Long idCliente, String nombreCliente, boolean activo,
            Long creadoEn, Long actualizadoEn) {

        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getRut(), u.getNombre(), u.getCorreo(), u.getTelefono(),
                    u.getCargo(), u.getRol().name(), u.getRol().getEtiqueta(), u.getIdCliente(),
                    u.getNombreCliente(), u.isActivo(), u.getCreadoEn(), u.getActualizadoEn());
        }
    }
}
