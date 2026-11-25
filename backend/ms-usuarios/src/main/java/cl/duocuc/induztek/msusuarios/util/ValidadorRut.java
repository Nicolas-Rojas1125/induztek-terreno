package cl.duocuc.induztek.msusuarios.util;

public final class ValidadorRut {
    private ValidadorRut() {
    }

    public static String limpiar(String rut) {
        if (rut == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : rut.toUpperCase().toCharArray()) {
            if (Character.isDigit(c) || c == 'K') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static char calcularDv(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }

    public static boolean esValido(String rut) {
        String limpio = limpiar(rut);
        if (limpio.length() < 8 || limpio.length() > 9) {
            return false;
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        for (char c : cuerpo.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return calcularDv(cuerpo) == limpio.charAt(limpio.length() - 1);
    }

    public static String formatear(String rut) {
        String limpio = limpiar(rut);
        if (limpio.length() < 2) {
            return rut == null ? "" : rut.trim();
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        StringBuilder sb = new StringBuilder();
        int contador = 0;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            sb.append(cuerpo.charAt(i));
            contador++;
            if (contador % 3 == 0 && i > 0) {
                sb.append('.');
            }
        }
        return sb.reverse() + "-" + dv;
    }
}
