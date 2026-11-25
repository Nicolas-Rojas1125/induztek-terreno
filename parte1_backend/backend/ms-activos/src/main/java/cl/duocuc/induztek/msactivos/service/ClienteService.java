package cl.duocuc.induztek.msactivos.service;

import cl.duocuc.induztek.msactivos.dto.ActivosDtos.ClienteRequest;
import cl.duocuc.induztek.msactivos.dto.ActivosDtos.ClienteResponse;
import cl.duocuc.induztek.msactivos.exception.ApiException;
import cl.duocuc.induztek.msactivos.model.Cliente;
import cl.duocuc.induztek.msactivos.repository.ClienteRepository;
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
public class ClienteService {
    private final ClienteRepository clientes;
    private final InstalacionRepository instalaciones;

    public ClienteService(ClienteRepository clientes, InstalacionRepository instalaciones) {
        this.clientes = clientes;
        this.instalaciones = instalaciones;
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(String buscar, Long idCliente, int pagina, int tamano) {
        String patron = Paginas.patron(buscar);
        Specification<Cliente> spec = (root, query, cb) -> {
            List<Predicate> c = new ArrayList<>();
            if (patron != null) {
                c.add(cb.or(
                        cb.like(cb.lower(root.get("razonSocial")), patron),
                        cb.like(cb.lower(root.get("rut")), patron),
                        cb.like(cb.lower(root.get("contactoNombre")), patron),
                        cb.like(cb.lower(root.get("giro")), patron)));
            }
            if (idCliente != null) {
                c.add(cb.equal(root.get("id"), idCliente));
            }
            return cb.and(c.toArray(new Predicate[0]));
        };
        return clientes.findAll(spec, Paginas.de(pagina, tamano, Sort.by("razonSocial")))
                .map(this::respuesta);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> todos(Long idCliente) {
        List<Cliente> lista = idCliente == null
                ? clientes.findAll(Sort.by("razonSocial"))
                : clientes.findById(idCliente).map(List::of).orElse(List.of());
        return lista.stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return respuesta(entidad(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest req, Long actor) {
        String rut = Validaciones.rut(req.rut());
        if (clientes.existsByRut(rut)) {
            throw ApiException.conflicto("Ya existe un cliente con ese RUT");
        }
        Cliente c = new Cliente();
        c.setRut(rut);
        c.setCreadoPor(actor);
        aplicar(c, req);
        return respuesta(clientes.save(c));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest req) {
        Cliente c = entidad(id);
        String rut = Validaciones.rut(req.rut());
        if (clientes.existsByRutAndIdNot(rut, id)) {
            throw ApiException.conflicto("Ya existe otro cliente con ese RUT");
        }
        c.setRut(rut);
        aplicar(c, req);
        return respuesta(clientes.save(c));
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente c = entidad(id);
        if (instalaciones.countByClienteId(id) > 0) {
            throw ApiException.conflicto("No se puede eliminar: el cliente tiene instalaciones registradas");
        }
        clientes.delete(c);
    }

    Cliente entidad(Long id) {
        return clientes.findById(id).orElseThrow(() -> ApiException.noEncontrado("No existe el cliente " + id));
    }

    private void aplicar(Cliente c, ClienteRequest req) {
        c.setRazonSocial(req.razonSocial().trim());
        c.setGiro(req.giro().trim());
        c.setContactoNombre(req.contactoNombre().trim());
        c.setTelefono(Validaciones.telefono(req.telefono()));
        c.setCorreo(Validaciones.correo(req.correo()));
        c.setDireccion(Validaciones.texto(req.direccion()));
    }

    private ClienteResponse respuesta(Cliente c) {
        return Mapeo.cliente(c, instalaciones.countByClienteId(c.getId()));
    }
}
