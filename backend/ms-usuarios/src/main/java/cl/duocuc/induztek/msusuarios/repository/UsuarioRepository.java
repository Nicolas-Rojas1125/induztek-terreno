package cl.duocuc.induztek.msusuarios.repository;

import cl.duocuc.induztek.msusuarios.model.Rol;
import cl.duocuc.induztek.msusuarios.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByRut(String rut);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    long countByRolAndActivoTrue(Rol rol);
}
