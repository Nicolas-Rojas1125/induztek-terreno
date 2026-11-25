package cl.duocuc.induztek.msprotocolos.repository;

import cl.duocuc.induztek.msprotocolos.model.Protocolo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProtocoloRepository extends JpaRepository<Protocolo, Long>, JpaSpecificationExecutor<Protocolo> {
    Optional<Protocolo> findByUuid(String uuid);

    boolean existsByNumeroAndUuidNot(String numero, String uuid);

    long countByVisitaUuid(String visitaUuid);

    List<Protocolo> findByActualizadoEnGreaterThanEqual(Long desde);

    List<Protocolo> findByIdClienteAndEstadoInAndActualizadoEnGreaterThanEqual(Long idCliente, Collection<String> estados, Long desde);

    long countByEstado(String estado);

    long countByIdClienteAndEstado(Long idCliente, String estado);
}
