package cl.duocuc.induztek.msactivos.dto;

import java.util.List;

public record ErrorResponse(int status, String message, List<String> errores) {
}
