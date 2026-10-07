package kode.backend.backendkode


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun main() {
    val encoder = BCryptPasswordEncoder()
    val hash = encoder.encode("1234")
    println("Hash generado:")
    println(hash)
}
