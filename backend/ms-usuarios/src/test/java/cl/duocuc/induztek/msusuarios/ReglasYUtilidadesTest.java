package cl.duocuc.induztek.msusuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.duocuc.induztek.msusuarios.model.Rol;
import cl.duocuc.induztek.msusuarios.util.Contrasenas;
import cl.duocuc.induztek.msusuarios.util.ValidadorRut;
import org.junit.jupiter.api.Test;

class ReglasYUtilidadesTest {

    @Test
    void administradorPuedeCrearTodosLosRoles() {
        for (Rol r : Rol.values()) {
            assertTrue(Rol.ADMINISTRADOR.puedeCrear(r));
        }
    }

    @Test
    void ingenieroSoloCreaTecnicosYClientes() {
        assertTrue(Rol.INGENIERO.puedeCrear(Rol.TECNICO));
        assertTrue(Rol.INGENIERO.puedeCrear(Rol.CLIENTE));
        assertFalse(Rol.INGENIERO.puedeCrear(Rol.INGENIERO));
        assertFalse(Rol.INGENIERO.puedeCrear(Rol.ADMINISTRADOR));
    }

    @Test
    void tecnicoYClienteNoCreanCuentas() {
        assertTrue(Rol.TECNICO.rolesQuePuedeCrear().isEmpty());
        assertTrue(Rol.CLIENTE.rolesQuePuedeCrear().isEmpty());
    }

    @Test
    void rolDesdeTextoIgnoraMayusculasYRechazaDesconocidos() {
        assertEquals(Rol.TECNICO, Rol.desdeTexto(" tecnico "));
        assertNull(Rol.desdeTexto("JEFE"));
        assertNull(Rol.desdeTexto(""));
    }

    @Test
    void validaYFormateaRut() {
        assertTrue(ValidadorRut.esValido("11.111.111-1"));
        assertTrue(ValidadorRut.esValido("76482915-8"));
        assertFalse(ValidadorRut.esValido("76.482.915-9"));
        assertFalse(ValidadorRut.esValido("123"));
        assertEquals("76.482.915-8", ValidadorRut.formatear("764829158"));
    }

    @Test
    void hashDeContrasenaUsaSalYSeVerifica() {
        String sal = Contrasenas.nuevaSal();
        String hash = Contrasenas.hash("Induztek2026", sal);
        assertTrue(Contrasenas.verificar("Induztek2026", sal, hash));
        assertFalse(Contrasenas.verificar("otra12345", sal, hash));
        assertNotEquals(hash, Contrasenas.hash("Induztek2026", Contrasenas.nuevaSal()));
    }

    @Test
    void formatoDeContrasena() {
        assertEquals("La contraseña debe tener mínimo 8 caracteres", Contrasenas.errorFormato("abc12"));
        assertEquals("La contraseña debe combinar letras y números", Contrasenas.errorFormato("abcdefgh"));
        assertNull(Contrasenas.errorFormato("clave2026"));
        assertEquals(6, Contrasenas.codigoRecuperacion().length());
    }
}
