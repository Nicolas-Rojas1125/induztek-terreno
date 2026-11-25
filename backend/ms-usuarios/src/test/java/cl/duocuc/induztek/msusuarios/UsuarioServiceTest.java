package cl.duocuc.induztek.msusuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.CambiarContrasenaRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.CrearUsuarioRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RecuperarResponse;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RestablecerRequest;
import cl.duocuc.induztek.msusuarios.exception.ApiException;
import cl.duocuc.induztek.msusuarios.model.Rol;
import cl.duocuc.induztek.msusuarios.model.Usuario;
import cl.duocuc.induztek.msusuarios.repository.UsuarioRepository;
import cl.duocuc.induztek.msusuarios.service.UsuarioService;
import cl.duocuc.induztek.msusuarios.util.Contrasenas;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UsuarioServiceTest {
    private UsuarioRepository repo;
    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(UsuarioRepository.class);
        servicio = new UsuarioService(repo, true);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(long id, Rol rol, String correo, String clave) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        u.setCorreo(correo);
        u.setNombre("Usuario " + id);
        u.setRut("11.111.111-1");
        u.setActivo(true);
        String sal = Contrasenas.nuevaSal();
        u.setSal(sal);
        u.setPasswordHash(Contrasenas.hash(clave, sal));
        when(repo.findById(id)).thenReturn(Optional.of(u));
        when(repo.findByCorreoIgnoreCase(correo)).thenReturn(Optional.of(u));
        return u;
    }

    private CrearUsuarioRequest solicitud(String rol, Long idCliente) {
        return new CrearUsuarioRequest("76.482.915-8", "Pedro Soto", "pedro@induztek.cl", "+56912345678",
                "Técnico", rol, idCliente, "Envasadora", "Clave2026");
    }

    @Test
    void loginCorrectoDevuelveUsuario() {
        usuario(1, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        assertEquals(1L, servicio.login("tec@induztek.cl", "Clave2026").getId());
    }

    @Test
    void loginConClaveIncorrectaResponde401() {
        usuario(1, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        ApiException e = assertThrows(ApiException.class, () -> servicio.login("tec@induztek.cl", "mala1234"));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void loginDeUsuarioDesactivadoResponde403() {
        usuario(1, Rol.TECNICO, "tec@induztek.cl", "Clave2026").setActivo(false);
        ApiException e = assertThrows(ApiException.class, () -> servicio.login("tec@induztek.cl", "Clave2026"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void ingenieroNoPuedeCrearAdministrador() {
        usuario(2, Rol.INGENIERO, "ing@induztek.cl", "Clave2026");
        ApiException e = assertThrows(ApiException.class, () -> servicio.crear(2L, solicitud("ADMINISTRADOR", null)));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        verify(repo, never()).save(any(Usuario.class));
    }

    @Test
    void tecnicoNoPuedeCrearCuentas() {
        usuario(3, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        ApiException e = assertThrows(ApiException.class, () -> servicio.crear(3L, solicitud("CLIENTE", 5L)));
        assertEquals("Su rol no permite crear cuentas", e.getMessage());
    }

    @Test
    void clienteSinEmpresaEsRechazado() {
        usuario(1, Rol.ADMINISTRADOR, "admin@induztek.cl", "Clave2026");
        ApiException e = assertThrows(ApiException.class, () -> servicio.crear(1L, solicitud("CLIENTE", null)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void crearCuentaGuardaRutFormateadoYContrasenaConHash() {
        usuario(2, Rol.INGENIERO, "ing@induztek.cl", "Clave2026");
        Usuario creado = servicio.crear(2L, solicitud("TECNICO", 9L));
        assertEquals("76.482.915-8", creado.getRut());
        assertEquals(Rol.TECNICO, creado.getRol());
        assertNull(creado.getIdCliente());
        assertEquals(2L, creado.getCreadoPor());
        assertTrue(Contrasenas.verificar("Clave2026", creado.getSal(), creado.getPasswordHash()));
    }

    @Test
    void correoDuplicadoResponde409() {
        usuario(1, Rol.ADMINISTRADOR, "admin@induztek.cl", "Clave2026");
        when(repo.existsByCorreoIgnoreCase("pedro@induztek.cl")).thenReturn(true);
        ApiException e = assertThrows(ApiException.class, () -> servicio.crear(1L, solicitud("TECNICO", null)));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void sinEncabezadoDeActorResponde401() {
        ApiException e = assertThrows(ApiException.class, () -> servicio.crear(null, solicitud("TECNICO", null)));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void cambiarContrasenaExigeLaActualCorrecta() {
        usuario(4, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        ApiException e = assertThrows(ApiException.class,
                () -> servicio.cambiarContrasena(4L, 4L, new CambiarContrasenaRequest("otra2026", "Nueva2026")));
        assertEquals("La contraseña actual no es correcta", e.getMessage());
    }

    @Test
    void cambiarContrasenaDeOtroUsuarioEsProhibido() {
        usuario(4, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        ApiException e = assertThrows(ApiException.class,
                () -> servicio.cambiarContrasena(1L, 4L, new CambiarContrasenaRequest("Clave2026", "Nueva2026")));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void cambiarContrasenaActualizaElHash() {
        Usuario u = usuario(4, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        servicio.cambiarContrasena(4L, 4L, new CambiarContrasenaRequest("Clave2026", "Nueva2026"));
        assertTrue(Contrasenas.verificar("Nueva2026", u.getSal(), u.getPasswordHash()));
    }

    @Test
    void recuperarYRestablecerContrasena() {
        Usuario u = usuario(5, Rol.CLIENTE, "cli@empresa.cl", "Clave2026");
        RecuperarResponse r = servicio.recuperar("cli@empresa.cl");
        assertNotNull(r.codigoDemo());
        servicio.restablecer(new RestablecerRequest("cli@empresa.cl", r.codigoDemo(), "Nueva2026"));
        assertTrue(Contrasenas.verificar("Nueva2026", u.getSal(), u.getPasswordHash()));
        assertNull(u.getCodigoRecuperacion());
    }

    @Test
    void recuperarCorreoInexistenteNoRevelaNada() {
        when(repo.findByCorreoIgnoreCase("nadie@x.cl")).thenReturn(Optional.empty());
        RecuperarResponse r = servicio.recuperar("nadie@x.cl");
        assertNull(r.codigoDemo());
    }

    @Test
    void codigoExpiradoEsRechazado() {
        Usuario u = usuario(5, Rol.CLIENTE, "cli@empresa.cl", "Clave2026");
        u.setCodigoRecuperacion("123456");
        u.setCodigoExpira(System.currentTimeMillis() - 1000);
        ApiException e = assertThrows(ApiException.class,
                () -> servicio.restablecer(new RestablecerRequest("cli@empresa.cl", "123456", "Nueva2026")));
        assertEquals("El código expiró: solicite uno nuevo", e.getMessage());
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdministrador() {
        usuario(1, Rol.ADMINISTRADOR, "admin@induztek.cl", "Clave2026");
        usuario(7, Rol.ADMINISTRADOR, "admin2@induztek.cl", "Clave2026");
        when(repo.countByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(1L);
        ApiException e = assertThrows(ApiException.class, () -> servicio.cambiarEstado(1L, 7L, false));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void ingenieroPuedeDesactivarTecnico() {
        usuario(2, Rol.INGENIERO, "ing@induztek.cl", "Clave2026");
        usuario(3, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        assertFalse(servicio.cambiarEstado(2L, 3L, false).isActivo());
    }

    @Test
    void soloAdministradorEliminaCuentas() {
        usuario(2, Rol.INGENIERO, "ing@induztek.cl", "Clave2026");
        usuario(3, Rol.TECNICO, "tec@induztek.cl", "Clave2026");
        ApiException e = assertThrows(ApiException.class, () -> servicio.eliminar(2L, 3L));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }
}
