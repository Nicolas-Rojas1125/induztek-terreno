package cl.duocuc.induztek.msactivos.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> mapeo) {
        List<T> contenido = pagina.getContent().stream().map(mapeo).toList();
        return new PaginaResponse<>(contenido, pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
