package cl.duocuc.induztek.domain.validacion

import java.security.MessageDigest
import java.security.SecureRandom

object HashContrasena {
    private const val ITERACIONES = 10_000

    fun nuevaSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(contrasena: String, sal: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        var resultado = (sal + contrasena).toByteArray(Charsets.UTF_8)
        repeat(ITERACIONES) {
            md.reset()
            resultado = md.digest(resultado)
        }
        return resultado.toHex()
    }

    fun verificar(contrasena: String, sal: String, hashGuardado: String): Boolean =
        MessageDigest.isEqual(hash(contrasena, sal).toByteArray(), hashGuardado.toByteArray())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
