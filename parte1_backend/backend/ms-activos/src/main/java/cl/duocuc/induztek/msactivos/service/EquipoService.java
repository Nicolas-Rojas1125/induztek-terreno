package cl.duocuc.induztek.msactivos.service;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.EquipoRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.EquipoResponse;
import cl.duocuc.induztek.msactivos.exception.ApiException;
import cl.duocuc.induztek.msactivos.model.Equipo;
import cl.duocuc.induztek.msactivos.model.Instalacion;
import cl.duocuc.induztek.msactivos.model.TipoEquipo;
import cl.duocuc.induztek.msactivos.repository.EquipoRepository;
import cl.duocuc.induztek.msactivos.repository.InstalacionRepository;
import cl.duocuc.induztek.msactivos.util.Validaciones;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipoService {
    static final int MAX_DATOS_TECNICOS = 30;
    static final int LARGO_MAXIMO_VALOR = 80;

    private final EquipoRepository equipos;
    private final InstalacionRepository instalaciones;

    public EquipoService(EquipoRepository equipos, InstalacionRepository instalaciones) {
        this.equipos = equipos;
        this.instalaciones = instalaciones;
    }

    @Transactional(readOnly = true)
    public Page<EquipoResponse> listar(String buscar, Long idInstalacion, Long idCliente, String tipo,
                                       String clasificacion, int pagina, int tamano) {
        String patron = Paginas.patron(buscar);
        TipoEquipo filtroTipo = TipoEquipo.desdeTexto(tipo);
        List<TipoEquipo> tiposClasificacion = clasificacion == null || clasificacion.isBlank() ? null
                : Arrays.stream(TipoEquipo.values())
                    .filter(t -> t.getClasificacion().equalsIgnoreCase(clasificacion.trim()))
                    .toList();
        Specification<Equipo> spec = (root, query, cb) -> {
            List<Predicate> c = new ArrayList<>();
            if (patron != null) {
                c.add(cb.or(
                        cb.like(cb.lower(root.get("tag")), patron),
                        cb.like(cb.lower(root.get("numeroSerie")), patron),
                        cb.like(cb.lower(root.get("marca")), patron),
                        cb.like(cb.lower(root.get("modelo")), patron),
                        cb.like(cb.lower(root.get("instalacion").get("nombre")), patron)));
            }
            if (idInstalacion != null) {
                c.add(cb.equal(root.get("instalacion").get("id"), idInstalacion));
            }
            if (idCliente != null) {
                c.add(cb.equal(root.get("instalacion").get("cliente").get("id"), idCliente));
            }
            if (filtroTipo != null) {
                c.add(cb.equal(root.get("tipo"), filtroTipo));
            }
            if (tiposClasificacion != null) {
                c.add(root.get("tipo").in(tiposClasificacion));
            }
            return cb.and(c.toArray(new Predicate[0]));
        };
        return equipos.findAll(spec, Paginas.de(pagina, tamano, Sort.by("tag"))).map(Mapeo::equipo);
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> todos(Long idCliente) {
        List<Equipo> lista = idCliente == null ? equipos.findAll() : equipos.findByInstalacionClienteId(idCliente);
        return lista.stream().map(Mapeo::equipo).toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(Long id) {
        return Mapeo.equipo(entidad(id));
    }

    @Transactional
    public EquipoResponse crear(EquipoRequest req, Long actor) {
        Instalacion instalacion = instalaciones.findById(req.idInstalacion())
                .orElseThrow(() -> ApiException.noEncontrado("No existe la instalación " + req.idInstalacion()));
        String tag = Validaciones.tag(req.tag());
        if (equipos.existsByInstalacionIdAndTagIgnoreCase(instalacion.getId(), tag)) {
            throw ApiException.conflicto("El TAG ya existe en esta instalación (RN-01)");
        }
        Equipo e = new Equipo();
        e.setInstalacion(instalacion);
        e.setTag(tag);
        e.setCreadoPor(actor);
        aplicar(e, req);
        return Mapeo.equipo(equipos.save(e));
    }

    @Transactional
    public EquipoResponse actualizar(Long id, EquipoRequest req) {
        Equipo e = entidad(id);
        if (!e.getInstalacion().getId().equals(req.idInstalacion())) {
            throw ApiException.invalido("Un equipo no puede cambiar de instalación");
        }
        String tag = Validaciones.tag(req.tag());
        if (equipos.existsByInstalacionIdAndTagIgnoreCaseAndIdNot(req.idInstalacion(), tag, id)) {
            throw ApiException.conflicto("El TAG ya existe en esta instalación (RN-01)");
        }
        e.setTag(tag);
        aplicar(e, req);
        return Mapeo.equipo(equipos.save(e));
    }

    @Transactional
    public void eliminar(Long id) {
        equipos.delete(entidad(id));
    }

    Equipo entidad(Long id) {
        return equipos.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el equipo " + id));
    }

    static Map<String, String> limpiarDatosTecnicos(Map<String, String> datos) {
        Map<String, String> limpio = new LinkedHashMap<>();
        if (datos == null) {
            return limpio;
        }
        for (Map.Entry<String, String> entrada : datos.entrySet()) {
            String clave = entrada.getKey() == null ? "" : entrada.getKey().trim();
            String valor = entrada.getValue() == null ? "" : entrada.getValue().trim();
            if (clave.isEmpty() || valor.isEmpty()) {
                continue;
            }
            if (clave.length() > 40 || valor.length() > LARGO_MAXIMO_VALOR) {
                throw ApiException.invalido("Dato técnico demasiado largo: " + clave);
            }
            limpio.put(clave, valor);
        }
        if (limpio.size() > MAX_DATOS_TECNICOS) {
            throw ApiException.invalido("Máximo " + MAX_DATOS_TECNICOS + " datos técnicos por equipo");
        }
        return limpio;
    }

    private void aplicar(Equipo e, EquipoRequest req) {
        TipoEquipo tipo = TipoEquipo.desdeTexto(req.tipo());
        if (tipo == null) {
            throw ApiException.invalido("Tipo de equipo inválido");
        }
        Validaciones.anio(req.anioFabricacion());
        Validaciones.positivo(req.potenciaKva(), "La potencia");
        Validaciones.positivo(req.tensionPrimariaKv(), "La tensión primaria");
        Validaciones.positivo(req.tensionSecundariaKv(), "La tensión secundaria");
        Validaciones.positivo(req.corrienteNominalA(), "La corriente nominal");
        Validaciones.positivo(req.maxContactosUohm(), "El máximo de contactos");
        if ((tipo == TipoEquipo.INTERRUPTOR || tipo == TipoEquipo.CELDA_MT) && req.maxContactosUohm() == null) {
            throw ApiException.invalido("Indique el máximo de resistencia de contactos de la placa");
        }
        e.setTipo(tipo);
        e.setMarca(req.marca().trim());
        e.setModelo(Validaciones.texto(req.modelo()));
        e.setNumeroSerie(req.numeroSerie().trim().toUpperCase());
        e.setAnioFabricacion(req.anioFabricacion());
        e.setPotenciaKva(req.potenciaKva());
        e.setTensionPrimariaKv(req.tensionPrimariaKv());
        e.setTensionSecundariaKv(req.tensionSecundariaKv());
        e.setCorrienteNominalA(req.corrienteNominalA());
        e.setMaxContactosUohm(req.maxContactosUohm());
        e.setObservaciones(Validaciones.texto(req.observaciones()));
        e.getDatosTecnicos().clear();
        e.getDatosTecnicos().putAll(limpiarDatosTecnicos(req.datosTecnicos()));
    }
}
