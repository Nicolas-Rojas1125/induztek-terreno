package cl.duocuc.induztek.msactivos.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

final class Paginas {
    private static final int TAMANO_MAXIMO = 100;

    private Paginas() {
    }

    static PageRequest de(int pagina, int tamano, Sort orden) {
        int t = tamano <= 0 ? 10 : Math.min(tamano, TAMANO_MAXIMO);
        return PageRequest.of(Math.max(pagina, 0), t, orden);
    }

    static String patron(String buscar) {
        return buscar == null || buscar.isBlank() ? null : "%" + buscar.trim().toLowerCase() + "%";
    }
}
