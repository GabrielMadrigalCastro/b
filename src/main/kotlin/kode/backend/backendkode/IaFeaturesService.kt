package kode.backend.backendkode

import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

// ===================== DTOs de features de IA =====================

/** La app manda el apunte (puede venir con HTML del editor). */
data class ResumenApunteRequest(
    val titulo: String? = null,
    val contenido: String
)

data class ResumenApunteResponse(
    val resumen: String,
    val checklist: List<String> = emptyList(),
    val generadoPor: String = "ia"   // "ia" o "error"
)

/** Situación de notas del estudiante en un curso, para pedir un consejo. */
data class ConsejoNotasRequest(
    val curso: String? = null,
    val notaMeta: Double,            // meta 0-100
    val notaActual: Double,          // acumulado 0-100 ya calificado
    val pesoFaltante: Double,        // % del curso sin calificar todavía
    val promedioNecesario: Double? = null,  // promedio que necesita en lo que falta
    val alcanzable: Boolean = true
)

data class ConsejoNotasResponse(
    val consejo: String,
    val generadoPor: String = "ia"   // "ia" o "error"
)

/**
 * Features puntuales de IA (no chat): resumir un apunte y dar un consejo en la
 * calculadora de notas. Usan la cadena de proveedores gratis vía AIRouter, con
 * un orden que pone a Groq primero y deja a Gemini de último para reservar su
 * cuota a las imágenes/OCR. Si la IA falla, el controlador responde 503 y la app
 * simplemente no muestra el extra.
 */
@Service
class IaFeaturesService(
    private val aiRouter: AIRouter,
    @Value("\${AI_ORDER_TEXTO:groq,mistral,gemini}")
    private val ordenTexto: String
) {

    fun resumirApunte(req: ResumenApunteRequest): ResumenApunteResponse {
        val texto = limpiarHtml(req.contenido)
        if (texto.length < 20) throw RuntimeException("El apunte es muy corto para resumir.")

        val raw = aiRouter.generarTexto(promptResumen(req.titulo, texto), ordenTexto)
        val obj = extraerObjeto(raw)
            ?: throw RuntimeException("La IA no devolvió JSON. Respondió: " + raw.take(160))

        val resumen = obj.optString("resumen").trim()
        val checklist = obj.optJSONArray("checklist")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i).trim().takeIf { it.isNotBlank() }
            }
        }.orEmpty()

        if (resumen.isBlank() && checklist.isEmpty()) {
            throw RuntimeException("La IA devolvió un resumen vacío.")
        }
        return ResumenApunteResponse(resumen = resumen, checklist = checklist)
    }

    fun consejoNotas(req: ConsejoNotasRequest): ConsejoNotasResponse {
        val raw = aiRouter.generarTexto(promptConsejo(req), ordenTexto)
        val consejo = limpiarLinea(raw)
        if (consejo.isBlank()) throw RuntimeException("La IA devolvió un consejo vacío.")
        return ConsejoNotasResponse(consejo = consejo)
    }

    // ===================== Prompts =====================

    private fun promptResumen(titulo: String?, texto: String): String = """
        Sos un asistente de estudio. Resumí el siguiente apunte de un estudiante universitario.
        Devolvé SOLO un objeto JSON válido, sin markdown ni texto extra, con esta forma exacta:
        {"resumen":"2 a 4 frases claras con lo esencial","checklist":["tema o acción para repasar","...","..."]}
        El "checklist" son de 3 a 6 puntos concretos de estudio (temas a repasar o pasos a hacer).
        Escribí en español, claro y breve. No inventes datos que no estén en el apunte.

        Título: ${titulo?.takeIf { it.isNotBlank() } ?: "(sin título)"}
        Apunte:
        $texto
    """.trimIndent()

    private fun promptConsejo(r: ConsejoNotasRequest): String {
        val curso = r.curso?.takeIf { it.isNotBlank() } ?: "el curso"
        val prom = r.promedioNecesario?.let { "%.0f%%".format(it) } ?: "no calculable"
        return """
            Sos un coach de estudio cercano y motivador. Español de Costa Rica, informal pero respetuoso.
            Un estudiante quiere alcanzar su nota meta en $curso. Datos (sobre 100):
            - Nota meta: ${fmt(r.notaMeta)}
            - Nota acumulada hasta ahora: ${fmt(r.notaActual)}
            - Porcentaje del curso que falta calificar: ${fmt(r.pesoFaltante)}%
            - Promedio que necesita en lo que falta: $prom
            - ¿Es alcanzable?: ${if (r.alcanzable) "sí" else "no"}

            Devolvé UNA sola frase (máximo 25 palabras), sin comillas, sin markdown y sin listas,
            con un consejo concreto de en qué enfocarse. Si no es alcanzable, sé honesto pero amable.
        """.trimIndent()
    }

    // ===================== Helpers =====================

    private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)

    /** Quita etiquetas HTML y entidades básicas; colapsa espacios; recorta. */
    private fun limpiarHtml(html: String): String =
        html.replace(Regex("(?s)<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(4000)

    /** Primer objeto JSON dentro del texto (la IA a veces envuelve en ```json). */
    private fun extraerObjeto(texto: String): JSONObject? {
        val ini = texto.indexOf('{')
        val fin = texto.lastIndexOf('}')
        if (ini < 0 || fin <= ini) return null
        return runCatching { JSONObject(texto.substring(ini, fin + 1)) }.getOrNull()
    }

    /** Deja una sola línea limpia (sin comillas ni markdown), acotada. */
    private fun limpiarLinea(texto: String): String =
        texto.trim()
            .removeSurrounding("\"")
            .replace(Regex("^[-*>#\\s]+"), "")
            .lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            .take(240)
}
