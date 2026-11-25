package cl.duocuc.induztek.msactivos.service;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.CalibracionRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstrumentoRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstrumentoResponse;
import cl.duocuc.induztek.msactivos.exception.ApiException;
import cl.duocuc.induztek.msactivos.model.Calibracion;
import cl.duocuc.induztek.msactivos.model.CategoriaInstrumento;
import cl.duocuc.induztek.msactivos.model.Instrumento;
import cl.duocuc.induztek.msactivos.repository.InstrumentoRepository;
import cl.duocuc.induztek.msactivos.util.Validaciones;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentoService {
    private final InstrumentoRepository instrumentos;

    public InstrumentoService(InstrumentoRepository instrumentos) {
        this.instrumentos = instrumentos;
    }

    @Transactional(readOnly = true)
    public Page<InstrumentoResponse> listar(String buscar, String categoria, String vigencia, int pagina, int tamano) {
        String patron = Paginas.patron(buscar);
        CategoriaInstrumento filtroCategoria = CategoriaInstrumento.desdeTexto(categoria);
        String filtroVigencia = vigencia == null ? "" : vigencia.trim().toUpperCase();
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(Mapeo.DIAS_AVISO);
        Specification<Instrumento> spec = (root, query, cb) -> {
            List<Predicate> c = new ArrayList<>();
            if (patron != null) {
                c.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("marca")), patron),
                        cb.like(cb.lower(root.get("modelo")), patron),
                        cb.like(cb.lower(root.get("numeroSerie")), patron)));
            }
            if (filtroCategoria != null) {
                c.add(cb.equal(root.get("categoria"), filtroCategoria));
            }
            switch (filtroVigencia) {
                case "VENCIDO" -> c.add(cb.lessThan(root.<LocalDate>get("vencimiento"), hoy));
                case "POR_VENCER" -> c.add(cb.between(root.<LocalDate>get("vencimiento"), hoy, limite));
                case "VIGENTE" -> c.add(cb.greaterThan(root.<LocalDate>get("vencimiento"), limite));
                case "SIN_CALIBRACION" -> c.add(cb.isNull(root.get("vencimiento")));
                default -> {
                }
            }
            return cb.and(c.toArray(new Predicate[0]));
        };
        Sort orden = Sort.by(Sort.Order.asc("vencimiento"), Sort.Order.asc("nombre"));
        return instrumentos.findAll(spec, Paginas.de(pagina, tamano, orden)).map(Mapeo::instrumento);
    }

    @Transactional(readOnly = true)
    public List<InstrumentoResponse> todos() {
        return instrumentos.findAll(Sort.by("nombre")).stream().map(Mapeo::instrumento).toList();
    }

    @Transactional(readOnly = true)
    public InstrumentoResponse obtener(Long id) {
        return Mapeo.instrumento(entidad(id));
    }

    @Transactional
    public InstrumentoResponse crear(InstrumentoRequest req, Long actor) {
        String serie = req.numeroSerie().trim().toUpperCase();
        if (instrumentos.existsByNumeroSerieIgnoreCase(serie)) {
            throw ApiException.conflicto("Ya existe un instrumento con ese número de serie");
        }
        Instrumento i = new Instrumento();
        i.setNumeroSerie(serie);
        aplicar(i, req);
        if (req.fechaCalibracion() != null && !req.fechaCalibracion().isBlank()) {
            i.agregarCalibracion(nuevaCalibracion(req.fechaCalibracion(), req.fechaVencimiento(), actor, req.registradaPor()));
        }
        return Mapeo.instrumento(instrumentos.save(i));
    }

    @Transactional
    public InstrumentoResponse actualizar(Long id, InstrumentoRequest req) {
        Instrumento i = entidad(id);
        String serie = req.numeroSerie().trim().toUpperCase();
        if (instrumentos.existsByNumeroSerieIgnoreCaseAndIdNot(serie, id)) {
            throw ApiException.conflicto("Ya existe otro instrumento con ese número de serie");
        }
        i.setNumeroSerie(serie);
        aplicar(i, req);
        return Mapeo.instrumento(instrumentos.save(i));
    }

    @Transactional
    public InstrumentoResponse agregarCalibracion(Long id, CalibracionRequest req) {
        Instrumento i = entidad(id);
        i.agregarCalibracion(nuevaCalibracion(req.fechaCalibracion(), req.fechaVencimiento(), req.idUsuarioRegistra(), req.registradaPor()));
        return Mapeo.instrumento(instrumentos.save(i));
    }

    @Transactional
    public void eliminar(Long id) {
        instrumentos.delete(entidad(id));
    }

    Instrumento entidad(Long id) {
        return instrumentos.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el instrumento " + id));
    }

    static Calibracion nuevaCalibracion(String calibracion, String vencimiento, Long idUsuario, String registradaPor) {
        LocalDate fc = Validaciones.fecha(calibracion, "La fecha de calibración");
        LocalDate fv = Validaciones.fecha(vencimiento, "La fecha de vencimiento");
        if (fc.isAfter(LocalDate.now())) {
            throw ApiException.invalido("La fecha de calibración no puede ser futura");
        }
        if (!fv.isAfter(fc)) {
            throw ApiException.invalido("El vencimiento debe ser posterior a la calibración");
        }
        Calibracion c = new Calibracion();
        c.setFechaCalibracion(fc);
        c.setFechaVencimiento(fv);
        c.setIdUsuarioRegistra(idUsuario);
        c.setRegistradaPor(Validaciones.texto(registradaPor));
        return c;
    }

    private void aplicar(Instrumento i, InstrumentoRequest req) {
        CategoriaInstrumento categoria = CategoriaInstrumento.desdeTexto(req.categoria());
        if (categoria == null) {
            throw ApiException.invalido("Categoría de instrumento inválida");
        }
        i.setNombre(req.nombre().trim());
        i.setCategoria(categoria);
        i.setMarca(req.marca().trim());
        i.setModelo(req.modelo().trim());
    }
}
