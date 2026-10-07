package kode.backend.backendkode

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Enruta la generación de texto con CADENA DE RESPALDO entre proveedores gratis.
 *
 * Orden configurable por la variable de entorno AI_PROVIDER_ORDER
 * (ej. "gemini,groq,mistral"). Si un proveedor no tiene key o falla (error,
 * timeout, sin cuota), pasa automáticamente al siguiente. Loguea cuál respondió.
 *
 * Ningún proveedor guarda su key en el código: todas vienen de variables de
 * entorno de Render.
 */
@Service
class AIRouter(
    proveedores: List<AIProvider>,
    @Value("\${AI_PROVIDER_ORDER:gemini,groq,mistral}") private val orden: String
) {
    private val porNombre = proveedores.associateBy { it.nombre.lowercase() }

    /** Convierte un texto "gemini,groq" en la lista de proveedores en ese orden. */
    private fun aProveedores(ordenTexto: String): List<AIProvider> =
        ordenTexto.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .mapNotNull { porNombre[it] }

    /** Orden por defecto (AI_PROVIDER_ORDER). */
    private fun secuencia(): List<AIProvider> = aProveedores(orden)

    /**
     * Genera texto probando cada proveedor de la CADENA en orden. Lanza excepción
     * solo si TODOS fallan o ninguno tiene key.
     */
    private fun ejecutar(prompt: String, cadena: List<AIProvider>): String {
        var ultimoError = "sin proveedores configurados"
        for (p in cadena) {
            if (!p.disponible()) {
                println("⏭️ IA: '${p.nombre}' sin API key, se salta.")
                continue
            }
            try {
                val respuesta = p.generarTexto(prompt)
                println("✅ IA: respondió '${p.nombre}'.")
                return respuesta
            } catch (e: Exception) {
                ultimoError = "${p.nombre}: ${e.message}"
                println("⚠️ IA: '${p.nombre}' falló ($ultimoError). Probando el siguiente…")
            }
        }
        throw RuntimeException("Ningún proveedor de IA respondió. Último error: $ultimoError")
    }

    /** Genera texto con el orden por defecto (AI_PROVIDER_ORDER). */
    fun generarTexto(prompt: String): String = ejecutar(prompt, secuencia())

    /**
     * Genera texto con un orden PROPIO de la tarea (ej. el plan usa Groq primero
     * para no gastar la cuota de Gemini, que se reserva para las imágenes/OCR).
     */
    fun generarTexto(prompt: String, ordenTarea: String): String =
        ejecutar(prompt, aProveedores(ordenTarea))

    /** ¿Hay al menos un proveedor con key en el orden configurado? */
    fun hayProveedorDisponible(): Boolean = secuencia().any { it.disponible() }

    /** El orden configurado (para mostrarlo en el diagnóstico). */
    fun orden(): String = orden

    /**
     * Diagnóstico: prueba CADA proveedor con un prompt trivial y reporta si tiene
     * key y si respondió, con el motivo del error si falla. Nunca expone la key.
     */
    fun diagnostico(): List<ProveedorEstado> {
        val enOrden = secuencia()
        val resto = porNombre.values.filterNot { it in enOrden }
        return (enOrden + resto).map { p ->
            val modelos = runCatching { p.modelosDisponibles() }.getOrDefault(emptyList())
            when {
                !p.disponible() -> ProveedorEstado(p.nombre, false, "sin_key", "No hay API key configurada.", modelos)
                else -> try {
                    p.generarTexto("Responde solo con: ok")
                    ProveedorEstado(p.nombre, true, "ok", "Respondió correctamente.", modelos)
                } catch (e: Exception) {
                    ProveedorEstado(p.nombre, true, "error", e.message ?: "error desconocido", modelos)
                }
            }
        }
    }
}

/** Estado de un proveedor de IA para el diagnóstico. */
data class ProveedorEstado(
    val nombre: String,
    val tieneKey: Boolean,
    val estado: String,   // "ok" | "error" | "sin_key"
    val detalle: String,
    val modelosDisponibles: List<String> = emptyList()
)
