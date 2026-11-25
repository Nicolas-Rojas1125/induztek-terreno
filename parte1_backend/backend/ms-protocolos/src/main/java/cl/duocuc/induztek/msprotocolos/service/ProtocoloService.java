package cl.duocuc.induztek.msprotocolos.service;

import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.HistorialDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.MedicionDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ProtocoloDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ProtocoloResumenDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.PruebaDto;
import cl.duocuc.induztek.msprotocolos.dto.ProtocolosDtos.ResumenEstadosDto;
import cl.duocuc.induztek.msprotocolos.exception.ApiException;
import cl.duocuc.induztek.msprotocolos.model.Catalogos;
import cl.duocuc.induztek.msprotocolos.model.Protocolo;
import cl.duocuc.induztek.msprotocolos.model.Prueba;
import cl.duocuc.induztek.msprotocolos.repository.ProtocoloRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProtocoloService {
    static final Pattern NUMERO = Pattern.compile("^PT-\\d{4}-\\d{4}(-[A-Z0-9]{4})?$");

    private final ProtocoloRepository protocolos;

    public ProtocoloService(ProtocoloRepository protocolos) {
        this.protocolos = protocolos;
    }

    @Transactional
    public ProtocoloDto guardar(String uuid, ProtocoloDto dto) {
        validar(dto);
        Protocolo p = protocolos.findByUuid(uuid).orElse(null);
        if (p != null && Catalogos.FORMALIZADO.equals(p.getEstado())) {
            if (Catalogos.FORMALIZADO.equals(dto.estado())) {
                return Conversion.protocolo(p);
            }
            throw ApiException.conflicto("El protocolo " + p.getNumero() + " está Formalizado y no se puede modificar (RN-04)");
        }
        if (protocolos.existsByNumeroAndUuidNot(dto.numero().trim(), uuid)) {
            throw ApiException.conflicto("Ya existe otro protocolo con el número " + dto.numero());
        }
        if (p == null) {
            p = new Protocolo();
            p.setUuid(uuid);
        }
        p.setNumero(dto.numero().trim());
        p.setVisitaUuid(dto.visitaUuid());
        p.setIdEquipo(dto.idEquipo());
        p.setIdInstalacion(dto.idInstalacion());
        p.setIdCliente(dto.idCliente());
        p.setIdUsuarioRegistra(dto.idUsuarioRegistra());
        p.setFechaHoraPrueba(dto.fechaHoraPrueba());
        p.setTemperaturaC(dto.temperaturaC());
        p.setHumedadPct(dto.humedadPct());
        p.setDescripcionMantenimiento(Conversion.texto(dto.descripcionMantenimiento()));
        p.setObservaciones(Conversion.texto(dto.observaciones()));
        p.setComentarioVariacion(Conversion.texto(dto.comentarioVariacion()));
        p.setEstado(dto.estado());
        p.setResultadoGlobal(dto.resultadoGlobal());
        p.setReemplazaUuid(dto.reemplazaUuid());
        List<Prueba> pruebas = new ArrayList<>();
        if (dto.pruebas() != null) {
            for (PruebaDto d : dto.pruebas()) {
                pruebas.add(Conversion.prueba(d));
            }
        }
        p.reemplazarPruebas(pruebas);
        p.getHistorial().clear();
        if (dto.historial() != null) {
            for (HistorialDto h : dto.historial()) {
                p.getHistorial().add(Conversion.cambio(h));
            }
        }
        p.setActualizadoEn(System.currentTimeMillis());
        return Conversion.protocolo(protocolos.save(p));
    }

    @Transactional(readOnly = true)
    public Page<ProtocoloResumenDto> listar(String buscar, String estado, String resultado, Long idCliente,
                                            Long idEquipo, String visitaUuid, int pagina, int tamano) {
        String patron = buscar == null || buscar.isBlank() ? null : "%" + buscar.trim().toUpperCase() + "%";
        String filtroEstado = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
        String filtroResultado = resultado == null || resultado.isBlank() ? null : resultado.trim().toUpperCase();
        Specification<Protocolo> spec = (root, query, cb) -> {
            List<Predicate> c = new ArrayList<>();
            if (patron != null) {
                c.add(cb.like(cb.upper(root.get("numero")), patron));
            }
            if (filtroEstado != null) {
                c.add(cb.equal(root.get("estado"), filtroEstado));
            }
            if (filtroResultado != null) {
                c.add(cb.equal(root.get("resultadoGlobal"), filtroResultado));
            }
            if (idCliente != null) {
                c.add(cb.equal(root.get("idCliente"), idCliente));
                c.add(root.get("estado").in(Catalogos.ESTADOS_VISIBLES_CLIENTE));
            }
            if (idEquipo != null) {
                c.add(cb.equal(root.get("idEquipo"), idEquipo));
            }
            if (visitaUuid != null && !visitaUuid.isBlank()) {
                c.add(cb.equal(root.get("visitaUuid"), visitaUuid));
            }
            return cb.and(c.toArray(new Predicate[0]));
        };
        int t = tamano <= 0 ? 10 : Math.min(tamano, 100);
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), t, Sort.by(Sort.Direction.DESC, "fechaHoraPrueba"));
        return protocolos.findAll(spec, pageable).map(Conversion::resumen);
    }

    @Transactional(readOnly = true)
    public ProtocoloDto obtener(String uuid) {
        return Conversion.protocolo(entidad(uuid));
    }

    @Transactional
    public void eliminar(String uuid) {
        Protocolo p = entidad(uuid);
        if (!Catalogos.BORRADOR.equals(p.getEstado())) {
            throw ApiException.conflicto("Solo se pueden eliminar protocolos en Borrador; los demás se anulan");
        }
        protocolos.delete(p);
    }

    @Transactional(readOnly = true)
    public List<ProtocoloDto> cambiadosDesde(long desde, Long idCliente) {
        List<Protocolo> lista = idCliente == null
                ? protocolos.findByActualizadoEnGreaterThanEqual(desde)
                : protocolos.findByIdClienteAndEstadoInAndActualizadoEnGreaterThanEqual(idCliente, Catalogos.ESTADOS_VISIBLES_CLIENTE, desde);
        return lista.stream().map(Conversion::protocolo).toList();
    }

    @Transactional(readOnly = true)
    public ResumenEstadosDto resumenEstados(Long idCliente) {
        Map<String, Long> porEstado = new LinkedHashMap<>();
        long total = 0;
        for (String estado : List.of("BORRADOR", "REGISTRADO_TERRENO", "OBSERVADO", "FORMALIZADO", "ANULADO")) {
            if (idCliente != null && !Catalogos.ESTADOS_VISIBLES_CLIENTE.contains(estado)) {
                continue;
            }
            long n = idCliente == null ? protocolos.countByEstado(estado) : protocolos.countByIdClienteAndEstado(idCliente, estado);
            porEstado.put(estado, n);
            total += n;
        }
        return new ResumenEstadosDto(porEstado, total);
    }

    Protocolo entidad(String uuid) {
        return protocolos.findByUuid(uuid).orElseThrow(() -> ApiException.noEncontrado("No existe el protocolo " + uuid));
    }

    static void validar(ProtocoloDto dto) {
        if (!NUMERO.matcher(dto.numero().trim()).matches()) {
            throw ApiException.invalido("Número de protocolo inválido (formato PT-AAAA-NNNN-XXXX)");
        }
        if (!Catalogos.ESTADOS.contains(dto.estado())) {
            throw ApiException.invalido("Estado de protocolo inválido: " + dto.estado());
        }
        if (dto.resultadoGlobal() != null && !Catalogos.RESULTADOS.contains(dto.resultadoGlobal())) {
            throw ApiException.invalido("Resultado global inválido: " + dto.resultadoGlobal());
        }
        if (dto.temperaturaC() != null && (dto.temperaturaC() < -30 || dto.temperaturaC() > 60)) {
            throw ApiException.invalido("La temperatura debe estar entre -30 y 60 °C");
        }
        if (dto.humedadPct() != null && (dto.humedadPct() < 0 || dto.humedadPct() > 100)) {
            throw ApiException.invalido("La humedad debe estar entre 0 y 100 %");
        }
        if (dto.pruebas() != null) {
            for (PruebaDto p : dto.pruebas()) {
                if (!Catalogos.TIPOS_PRUEBA.contains(p.tipoPrueba())) {
                    throw ApiException.invalido("Tipo de prueba inválido: " + p.tipoPrueba());
                }
                if (p.momento() != null && !Catalogos.MOMENTOS.contains(p.momento())) {
                    throw ApiException.invalido("Momento de prueba inválido: " + p.momento());
                }
                if (p.resultado() != null && !Catalogos.RESULTADOS.contains(p.resultado())) {
                    throw ApiException.invalido("Resultado de prueba inválido: " + p.resultado());
                }
                if (p.elemento() == null || p.elemento().isBlank()) {
                    throw ApiException.invalido("Cada prueba debe indicar el elemento medido");
                }
                if (p.mediciones() != null) {
                    for (MedicionDto m : p.mediciones()) {
                        if (m.parametro() == null || m.valor() == null || m.valor() < 0) {
                            throw ApiException.invalido("Medición inválida en " + p.tipoPrueba() + " " + p.elemento());
                        }
                    }
                }
            }
        }
        if (dto.historial() != null) {
            for (HistorialDto h : dto.historial()) {
                if (!Catalogos.ESTADOS.contains(h.estadoNuevo()) || h.idUsuario() == null || h.fechaHora() == null) {
                    throw ApiException.invalido("Historial de estados inválido");
                }
            }
        }
    }
}
