package cl.duocuc.induztek.msactivos.util;

import cl.duocuc.induztek.msactivos.exception.ApiException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class Validaciones {
    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern TAG = Pattern.compile("^[A-Z0-9][A-Z0-9/.\\-]{0,29}$");

    private Validaciones() {
    }

    public static String rut(String rut) {
        if (!ValidadorRut.esValido(rut)) {
            throw ApiException.invalido("RUT inválido: revise el dígito verificador");
        }
        return ValidadorRut.formatear(rut);
    }

    public static String correo(String correo) {
        String limpio = correo == null ? "" : correo.trim().toLowerCase();
        if (!CORREO.matcher(limpio).matches()) {
            throw ApiException.invalido("El correo no es válido");
        }
        return limpio;
    }

    public static String telefono(String telefono) {
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        String nacional = digitos.startsWith("56") && digitos.length() == 11 ? digitos.substring(2) : digitos;
        if (nacional.length() != 9) {
            throw ApiException.invalido("Teléfono inválido (ej.: +56 9 1234 5678)");
        }
        return telefono.trim();
    }

    public static String tag(String tag) {
        String limpio = tag == null ? "" : tag.trim().toUpperCase();
        if (!TAG.matcher(limpio).matches()) {
            throw ApiException.invalido("TAG inválido: use letras, números y - / . (ej.: TR-01)");
        }
        return limpio;
    }

    public static void anio(Integer anio) {
        if (anio != null && (anio < 1950 || anio > LocalDate.now().getYear())) {
            throw ApiException.invalido("El año de fabricación debe estar entre 1950 y " + LocalDate.now().getYear());
        }
    }

    public static void positivo(Double valor, String campo) {
        if (valor != null && valor <= 0) {
            throw ApiException.invalido(campo + " debe ser mayor a 0");
        }
    }

    public static LocalDate fecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw ApiException.invalido(campo + " es obligatoria");
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.invalido(campo + " debe tener formato AAAA-MM-DD");
        }
    }

    public static void coordenadas(Double latitud, Double longitud) {
        if ((latitud == null) != (longitud == null)) {
            throw ApiException.invalido("Debe indicar latitud y longitud juntas");
        }
        if (latitud != null && (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180)) {
            throw ApiException.invalido("Coordenadas fuera de rango");
        }
    }

    public static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
