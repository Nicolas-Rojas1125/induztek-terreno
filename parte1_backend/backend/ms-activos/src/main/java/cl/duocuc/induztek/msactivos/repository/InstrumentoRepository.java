package cl.duocuc.induztek.msactivos.repository;

import cl.duocuc.induztek.msactivos.model.Instrumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstrumentoRepository extends JpaRepository<Instrumento, Long>, JpaSpecificationExecutor<Instrumento> {
    boolean existsByNumeroSerieIgnoreCase(String numeroSerie);

    boolean existsByNumeroSerieIgnoreCaseAndIdNot(String numeroSerie, Long id);
}
