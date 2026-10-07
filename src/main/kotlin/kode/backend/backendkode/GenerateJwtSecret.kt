package kode.backend.backendkode


import java.util.*

fun main() {
    val key = Base64.getEncoder().encodeToString("MI_CLAVE_SECRETA_SEGURA_SUPER_LARGA_Y_ALEATORIA_1234567890_ABCDEF".toByteArray())
    println("Tu clave JWT codificada en Base64 es:")
    println(key)
}
