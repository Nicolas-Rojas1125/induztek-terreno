package cl.duocuc.induztek.msactivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.CalibracionRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.ClienteRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.ClienteResponse;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.EquipoRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.EquipoResponse;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstrumentoResponse;
import cl.duocuc.induztek.msactivos.exception.ApiException;
import cl.duocuc.induztek.msactivos.model.CategoriaInstrumento;
import cl.duocuc.induztek.msactivos.model.Cliente;
import cl.duocuc.induztek.msactivos.model.Equipo;
import cl.duocuc.induztek.msactivos.model.Instalacion;
import cl.duocuc.induztek.msactivos.model.Instrumento;
import cl.duocuc.induztek.msactivos.repository.ClienteRepository;
import cl.duocuc.induztek.msactivos.repository.EquipoRepository;
import cl.duocuc.induztek.msactivos.repository.InstalacionRepository;
import cl.duocuc.induztek.msactivos.repository.InstrumentoRepository;
import cl.duocuc.induztek.msactivos.service.ClienteService;
import cl.duocuc.induztek.msactivos.service.EquipoService;
import cl.duocuc.induztek.msactivos.service.InstrumentoService;
import cl.duocuc.induztek.msactivos.service.Mapeo;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ActivosServiceTest {
    private ClienteRepository clientes;
    private InstalacionRepository instalaciones;
    private EquipoRepository equipos;
    private InstrumentoRepository instrumentos;

    @BeforeEach
    void preparar() {
        clientes = mock(ClienteRepository.class);
        instalaciones = mock(InstalacionRepository.class);
        equipos = mock(EquipoRepository.class);
        instrumentos = mock(InstrumentoRepository.class);
        when(clientes.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipos.save(any(Equipo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instrumentos.save(any(Instrumento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ClienteRequest cliente(String rut) {
        return new ClienteRequest(rut, "Envasadora del Pacífico S.A.", "Envasado", "Marcela Fuentes",
                "+56 9 8765 4321", "CONTACTO@Envasadora.cl", "Av. Industrial 123");
    }

    private Instalacion instalacion() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setRazonSocial("Envasadora");
        Instalacion i = new Instalacion();
        i.setId(10L);
        i.setCliente(c);
        i.setNombre("Planta Til Til");
        when(instalaciones.findById(10L)).thenReturn(Optional.of(i));
        return i;
    }

    private EquipoRequest equipo(String tipo, Double maxContactos, Map<String, String> datos) {
        return new EquipoRequest(10L, " tr-01 ", tipo, "Tusan", "TT-500", "abc123", 2015, 500.0, 13.2, 0.4,
                null, maxContactos, "", datos);
    }

    @Test
    void crearClienteFormateaRutYNormalizaCorreo() {
        ClienteResponse r = new ClienteService(clientes, instalaciones).crear(cliente("764829158"), 1L);
        assertEquals("76.482.915-8", r.rut());
        assertEquals("contacto@envasadora.cl", r.correo());
    }

    @Test
    void clienteConRutInvalidoEsRechazado() {
        ApiException e = assertThrows(ApiException.class,
                () -> new ClienteService(clientes, instalaciones).crear(cliente("76.482.915-9"), 1L));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void clienteConRutDuplicadoResponde409() {
        when(clientes.existsByRut("76.482.915-8")).thenReturn(true);
        ApiException e = assertThrows(ApiException.class,
                () -> new ClienteService(clientes, instalaciones).crear(cliente("76.482.915-8"), 1L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void noSeEliminaClienteConInstalaciones() {
        Cliente c = new Cliente();
        c.setId(1L);
        when(clientes.findById(1L)).thenReturn(Optional.of(c));
        when(instalaciones.countByClienteId(1L)).thenReturn(2L);
        assertThrows(ApiException.class, () -> new ClienteService(clientes, instalaciones).eliminar(1L));
        verify(clientes, never()).delete(any(Cliente.class));
    }

    @Test
    void crearEquipoNormalizaTagYGuardaDatosTecnicos() {
        instalacion();
        EquipoResponse r = new EquipoService(equipos, instalaciones).crear(
                equipo("TRANSFORMADOR", null, Map.of("grupoConexion", " Dyn11 ", "vacio", " ")), 2L);
        assertEquals("TR-01", r.tag());
        assertEquals("ABC123", r.numeroSerie());
        assertEquals("Dyn11", r.datosTecnicos().get("grupoConexion"));
        assertTrue(!r.datosTecnicos().containsKey("vacio"));
        assertEquals("PRIMARIO", r.clasificacion());
    }

    @Test
    void tagRepetidoEnLaInstalacionResponde409() {
        instalacion();
        when(equipos.existsByInstalacionIdAndTagIgnoreCase(10L, "TR-01")).thenReturn(true);
        ApiException e = assertThrows(ApiException.class,
                () -> new EquipoService(equipos, instalaciones).crear(equipo("TRANSFORMADOR", null, null), 2L));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void interruptorExigeMaximoDeContactos() {
        instalacion();
        ApiException e = assertThrows(ApiException.class,
                () -> new EquipoService(equipos, instalaciones).crear(equipo("INTERRUPTOR", null, null), 2L));
        assertEquals("Indique el máximo de resistencia de contactos de la placa", e.getMessage());
    }

    @Test
    void tipoDeEquipoInvalidoEsRechazado() {
        instalacion();
        assertThrows(ApiException.class,
                () -> new EquipoService(equipos, instalaciones).crear(equipo("MOTOR", null, null), 2L));
    }

    @Test
    void vigenciaDeInstrumento() {
        LocalDate hoy = LocalDate.of(2026, 11, 24);
        assertEquals("VENCIDO", Mapeo.vigencia(hoy.minusDays(1), hoy));
        assertEquals("POR_VENCER", Mapeo.vigencia(hoy.plusDays(30), hoy));
        assertEquals("VIGENTE", Mapeo.vigencia(hoy.plusDays(31), hoy));
        assertEquals("SIN_CALIBRACION", Mapeo.vigencia(null, hoy));
    }

    @Test
    void calibracionActualizaUltimaYVencimiento() {
        Instrumento i = new Instrumento();
        i.setId(3L);
        i.setNombre("Megóhmetro");
        i.setCategoria(CategoriaInstrumento.MEGOHMETRO);
        i.setMarca("Megger");
        i.setModelo("MIT525");
        i.setNumeroSerie("MG-1");
        when(instrumentos.findById(3L)).thenReturn(Optional.of(i));
        InstrumentoResponse r = new InstrumentoService(instrumentos)
                .agregarCalibracion(3L, new CalibracionRequest("2026-01-10", "2027-01-10", 1L, "Admin"));
        assertEquals("2026-01-10", r.ultimaCalibracion());
        assertEquals("2027-01-10", r.vencimiento());
        assertEquals(1, r.calibraciones().size());
    }

    @Test
    void vencimientoAnteriorACalibracionEsRechazado() {
        Instrumento i = new Instrumento();
        i.setId(3L);
        when(instrumentos.findById(3L)).thenReturn(Optional.of(i));
        ApiException e = assertThrows(ApiException.class, () -> new InstrumentoService(instrumentos)
                .agregarCalibracion(3L, new CalibracionRequest("2026-01-10", "2025-01-10", 1L, "Admin")));
        assertEquals("El vencimiento debe ser posterior a la calibración", e.getMessage());
    }
}
