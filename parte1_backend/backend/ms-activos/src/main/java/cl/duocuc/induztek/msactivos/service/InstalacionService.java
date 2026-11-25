package cl.duocuc.induztek.msactivos.service;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstalacionRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.InstalacionResponse;
import cl.duocuc.induztek.msactivos.exception.ApiException;
import cl.duocuc.induztek.msactivos.model.Cliente;
import cl.duocuc.induztek.msactivos.model.Instalacion;
import cl.duocuc.induztek.msactivos.repository.ClienteRepository;
import cl.duocuc.induztek.msactivos.repository.EquipoRepository;
import cl.duocuc.induztek.msactivos.repository.InstalacionRepository;
import cl.duocuc.induztek.msactivos.util.Validaciones;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstalacionService {
    private final InstalacionRepository instalaciones;
    private final ClienteRepository clientes;
    private final EquipoRepository equipos;

    public InstalacionService(InstalacionRepository instalaciones, ClienteRepository clientes, EquipoRepository equipos) {
        this.instalaciones = instalaciones;
        this.clientes = clientes;
        this.equipos = equipos;
    }

    @Transactional(readOnly = true)
    public Page<InstalacionResponse> listar(String buscar, Long idCliente, int pagina, int tamano) {
        String patron = Paginas.patron(buscar);
        Specification<Instalacion> spec = (root, query, cb) -> {
            List<Predicate> c = new ArrayList<>();
            if (patron != null) {
                c.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("ubicacion")), patron),
                        cb.like(cb.lower(root.get("nombreComuna")), patron)));
            }
            if (idCliente != null) {
                c.add(cb.equal(root.get("cliente").get("id"), idCliente));
            }
            return cb.and(c.toArray(new Predicate[0]));
        };
        return instalaciones.findAll(spec, Paginas.de(pagina, tamano, Sort.by("nombre"))).map(this::respuesta);
    }

    @Transactional(readOnly = true)
    public List<InstalacionResponse> todas(Long idCliente) {
        List<Instalacion> lista = idCliente == null ? instalaciones.findAll() : instalaciones.findByClienteId(idCliente);
        return lista.stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public InstalacionResponse obtener(Long id) {
        return respuesta(entidad(id));
    }

    @Transactional
    public InstalacionResponse crear(InstalacionRequest req) {
        Cliente cliente = clientes.findById(req.idCliente())
                .orElseThrow(() -> ApiException.noEncontrado("No existe el cliente " + req.idCliente()));
        if (instalaciones.existsByClienteIdAndNombreIgnoreCase(cliente.getId(), req.nombre().trim())) {
            throw ApiException.conflicto("Ese cliente ya tiene una instalación con ese nombre");
        }
        Instalacion i = new Instalacion();
        i.setCliente(cliente);
        aplicar(i, req);
        return respuesta(instalaciones.save(i));
    }

    @Transactional
    public InstalacionResponse actualizar(Long id, InstalacionRequest req) {
        Instalacion i = entidad(id);
        if (!i.getCliente().getId().equals(req.idCliente())) {
            throw ApiException.invalido("Una instalación no puede cambiar de cliente");
        }
        if (instalaciones.existsByClienteIdAndNombreIgnoreCaseAndIdNot(req.idCliente(), req.nombre().trim(), id)) {
            throw ApiException.conflicto("Ese cliente ya tiene otra instalación con ese nombre");
        }
        aplicar(i, req);
        return respuesta(instalaciones.save(i));
    }

    @Transactional
    public void eliminar(Long id) {
        Instalacion i = entidad(id);
        if (equipos.countByInstalacionId(id) > 0) {
            throw ApiException.conflicto("No se puede eliminar: la instalación tiene equipos registrados");
        }
        instalaciones.delete(i);
    }

    Instalacion entidad(Long id) {
        return instalaciones.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe la instalación " + id));
    }

    private void aplicar(Instalacion i, InstalacionRequest req) {
        Validaciones.coordenadas(req.latitud(), req.longitud());
        i.setNombre(req.nombre().trim());
        i.setUbicacion(req.ubicacion().trim());
        i.setIdComuna(req.idComuna());
        i.setNombreComuna(Validaciones.texto(req.nombreComuna()));
        i.setNombreRegion(Validaciones.texto(req.nombreRegion()));
        i.setObservacionesAcceso(Validaciones.texto(req.observacionesAcceso()));
        i.setLatitud(req.latitud());
        i.setLongitud(req.longitud());
    }

    private InstalacionResponse respuesta(Instalacion i) {
        return Mapeo.instalacion(i, equipos.countByInstalacionId(i.getId()));
    }
}
