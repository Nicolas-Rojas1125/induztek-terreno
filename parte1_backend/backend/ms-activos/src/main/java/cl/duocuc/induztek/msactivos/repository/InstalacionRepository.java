package cl.duocuc.induztek.msactivos.repository;

import cl.duocuc.induztek.msactivos.model.Instalacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstalacionRepository extends JpaRepository<Instalacion, Long>, JpaSpecificationExecutor<Instalacion> {
    long countByClienteId(Long idCliente);

    boolean existsByClienteIdAndNombreIgnoreCase(Long idCliente, String nombre);

    boolean existsByClienteIdAndNombreIgnoreCaseAndIdNot(Long idCliente, String nombre, Long id);

    List<Instalacion> findByClienteId(Long idCliente);
}
