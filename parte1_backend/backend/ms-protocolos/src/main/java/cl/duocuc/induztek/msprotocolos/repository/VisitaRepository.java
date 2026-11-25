package cl.duocuc.induztek.msprotocolos.repository;

import cl.duocuc.induztek.msprotocolos.model.Visita;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VisitaRepository extends JpaRepository<Visita, Long>, JpaSpecificationExecutor<Visita> {
    Optional<Visita> findByUuid(String uuid);

    List<Visita> findByActualizadoEnGreaterThanEqual(Long desde);

    List<Visita> findByIdClienteAndFechaHoraCierreIsNotNullAndActualizadoEnGreaterThanEqual(Long idCliente, Long desde);
}
