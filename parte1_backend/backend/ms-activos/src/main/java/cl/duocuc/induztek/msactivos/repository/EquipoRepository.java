package cl.duocuc.induztek.msactivos.repository;

import cl.duocuc.induztek.msactivos.model.Equipo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EquipoRepository extends JpaRepository<Equipo, Long>, JpaSpecificationExecutor<Equipo> {
    long countByInstalacionId(Long idInstalacion);

    boolean existsByInstalacionIdAndTagIgnoreCase(Long idInstalacion, String tag);

    boolean existsByInstalacionIdAndTagIgnoreCaseAndIdNot(Long idInstalacion, String tag, Long id);

    List<Equipo> findByInstalacionClienteId(Long idCliente);
}
