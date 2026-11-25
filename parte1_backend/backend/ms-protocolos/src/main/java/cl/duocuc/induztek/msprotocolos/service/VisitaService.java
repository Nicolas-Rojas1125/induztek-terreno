package cl.duocuc.induztek.msprotocolos.service;

import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.RecepcionDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.VisitaDto;
import cl.duocuc.induztek.msprotocolos.exception.ApiException;
import cl.duocuc.induztek.msprotocolos.model.Catalogos;
import cl.duocuc.induztek.msprotocolos.model.Visita;
import cl.duocuc.induztek.msprotocolos.repository.ProtocoloRepository;
import cl.duocuc.induztek.msprotocolos.repository.VisitaRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitaService {
    static final Pattern OT = Pattern.compile("^OT-\\d{4}-\\d{4}$");

    private final VisitaRepository visitas;
    private final ProtocoloRepository protocolos;

    public VisitaService(VisitaRepository visitas, ProtocoloRepository protocolos) {
        this.visitas = visitas;
        this.protocolos = protocolos;
    }

    @Transactional
    public VisitaDto guardar(String uuid, VisitaDto dto) {
        String ot = dto.numeroOt().trim().toUpperCase();
        if (!OT.matcher(ot).matches()) {
            throw ApiException.invalido("Número de OT inválido (formato OT-2026-1187)");
        }
        if (!Catalogos.TIPOS_VISITA.contains(dto.tipoVisita())) {
            throw ApiException.invalido("Tipo de visita inválido");
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(dto.fecha());
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("La fecha debe tener formato AAAA-MM-DD");
        }
        Visita v = visitas.findByUuid(uuid).orElseGet(Visita::new);
        if (v.getUuid() == null) {
            v.setUuid(uuid);
        }
        v.setNumeroOt(ot);
        v.setIdInstalacion(dto.idInstalacion());
        v.setIdCliente(dto.idCliente());
        v.setFecha(fecha);
        v.setTipoVisita(dto.tipoVisita());
        v.setIdIngenieroCargo(dto.idIngenieroCargo());
        if (v.getFechaHoraCierre() == null && dto.fechaHoraCierre() != null) {
            v.setFechaHoraCierre(dto.fechaHoraCierre());
            v.setLatitudCierre(dto.latitudCierre());
            v.setLongitudCierre(dto.longitudCierre());
            v.setIdUsuarioCierra(dto.idUsuarioCierra());
        }
        v.getParticipantes().clear();
        if (dto.participantes() != null) {
            v.getParticipantes().addAll(dto.participantes());
        }
        RecepcionDto r = dto.recepcion();
        if (r != null && v.getFechaHoraRecepcion() == null) {
            if (v.getFechaHoraCierre() == null) {
                throw ApiException.conflicto("No se registra la recepción de una visita sin cerrar");
            }
            v.setReceptorNombre(r.receptorNombre());
            v.setReceptorRut(r.receptorRut());
            v.setReceptorCargo(r.receptorCargo());
            v.setFechaHoraRecepcion(r.fechaHoraRecepcion());
        }
        v.setOrigen(dto.origen());
        v.setActualizadoEn(System.currentTimeMillis());
        return Conversion.visita(visitas.save(v));
    }

    @Transactional(readOnly = true)
    public Page<VisitaDto> listar(String buscar, Long idCliente, Long idInstalacion, Boolean abiertas, int pagina, int tamano) {
        String patron = buscar == null || buscar.isBlank() ? null : "%" + buscar.trim().toUpperCase() + "%";
        Specification<Visita> spec = (root, query, cb) -> {
            List<Predicate> c = new ArrayList<>();
            if (patron != null) {
                c.add(cb.like(cb.upper(root.get("numeroOt")), patron));
            }
            if (idCliente != null) {
                c.add(cb.equal(root.get("idCliente"), idCliente));
            }
            if (idInstalacion != null) {
                c.add(cb.equal(root.get("idInstalacion"), idInstalacion));
            }
            if (Boolean.TRUE.equals(abiertas)) {
                c.add(cb.isNull(root.get("fechaHoraCierre")));
            } else if (Boolean.FALSE.equals(abiertas)) {
                c.add(cb.isNotNull(root.get("fechaHoraCierre")));
            }
            return cb.and(c.toArray(new Predicate[0]));
        };
        int t = tamano <= 0 ? 10 : Math.min(tamano, 100);
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), t, Sort.by(Sort.Direction.DESC, "fecha"));
        return visitas.findAll(spec, pageable).map(Conversion::visita);
    }

    @Transactional(readOnly = true)
    public VisitaDto obtener(String uuid) {
        return Conversion.visita(entidad(uuid));
    }

    @Transactional
    public void eliminar(String uuid) {
        Visita v = entidad(uuid);
        if (protocolos.countByVisitaUuid(uuid) > 0) {
            throw ApiException.conflicto("No se puede eliminar: la visita tiene protocolos");
        }
        visitas.delete(v);
    }

    @Transactional(readOnly = true)
    public List<VisitaDto> cambiadasDesde(long desde, Long idCliente) {
        List<Visita> lista = idCliente == null
                ? visitas.findByActualizadoEnGreaterThanEqual(desde)
                : visitas.findByIdClienteAndFechaHoraCierreIsNotNullAndActualizadoEnGreaterThanEqual(idCliente, desde);
        return lista.stream().map(Conversion::visita).toList();
    }

    Visita entidad(String uuid) {
        return visitas.findByUuid(uuid).orElseThrow(() -> ApiException.noEncontrado("No existe la visita " + uuid));
    }
}
