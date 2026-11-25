package cl.duocuc.induztek.msactivos.repository;

import cl.duocuc.induztek.msactivos.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    boolean existsByRut(String rut);

    boolean existsByRutAndIdNot(String rut, Long id);
}
