package cl.duocuc.induztek.msusuarios.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Contrasenas {
    private static final int ITERACIONES = 65_536;
    private static final int LARGO_BITS = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Contrasenas() {
    }

    public static String nuevaSal() {
        byte[] bytes = new byte[16];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String hash(String contrasena, String sal) {
        try {
            KeySpec spec = new PBEKeySpec(contrasena.toCharArray(), HexFormat.of().parseHex(sal), ITERACIONES, LARGO_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }

    public static boolean verificar(String contrasena, String sal, String hashGuardado) {
        byte[] calculado = hash(contrasena, sal).getBytes();
        return MessageDigest.isEqual(calculado, hashGuardado.getBytes());
    }

    public static String errorFormato(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            return "La contraseña es obligatoria";
        }
        if (contrasena.length() < 8) {
            return "La contraseña debe tener mínimo 8 caracteres";
        }
        boolean letra = contrasena.chars().anyMatch(Character::isLetter);
        boolean digito = contrasena.chars().anyMatch(Character::isDigit);
        if (!letra || !digito) {
            return "La contraseña debe combinar letras y números";
        }
        return null;
    }

    public static String codigoRecuperacion() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }
}
