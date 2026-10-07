package kode.backend.backendkode

import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class GeminiStrategy(
    @Value("\${GEMINI_API_KEY:dummy-key}") private val apiKey: String,
    // Mismo modelo que el resto de la IA; gemini-flash-latest soporta imágenes (OCR).
    @Value("\${GEMINI_MODEL:gemini-flash-latest}") private val model: String,
    // Modelos que intenta el OCR, en orden. Si el primero está saturado, cae al
    // siguiente. Configurable en Render (GEMINI_OCR_MODELS, separados por coma).
    @Value("\${GEMINI_OCR_MODELS:gemini-flash-latest,gemini-flash-lite-latest}")
    private val ocrModelsRaw: String
) : IAResponseStrategy {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    private fun urlDe(modelo: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent"

    private val ocrModels: List<String> =
        ocrModelsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf(model) }

    override fun generarRespuesta(prompt: String): String {
        return try {
            // Construimos el JSON con la librería para escapar bien comillas/saltos de línea
            val part = JSONObject().put("text", prompt)
            val parts = org.json.JSONArray().put(part)
            val content = JSONObject().put("parts", parts)
            val body = JSONObject()
                .put("contents", org.json.JSONArray().put(content))
                .toString()

            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl?key=$apiKey"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val json = JSONObject(response.body())

            when {
                json.has("candidates") -> json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .replace("\\n", "\n")
                // Si Gemini devuelve un error (key inválida, modelo inexistente, cuota...)
                // lo mostramos para poder diagnosticar en vez de un mensaje genérico.
                json.has("error") -> "IA no disponible: " +
                    json.getJSONObject("error").optString("message", "error desconocido")
                else -> "No se pudo generar respuesta"
            }

        } catch (e: Exception) {
            e.printStackTrace()
            "Error al generar respuesta con Gemini: ${e.message}"
        }
    }

    override fun generarPlanEstudio(
        studentId: Long,
        assignmentId: Long,
        horasDisponiblesPorDia: Int,
        diasAntesDueDate: Int
    ): List<StudyPlanResult> {
        // GeminiStrategy no soporta generación de planes de estudio
        // Esta funcionalidad es exclusiva de SmartPlannerStrategy
        throw UnsupportedOperationException(
            "GeminiStrategy no soporta la generación de planes de estudio. " +
                    "Use SmartPlannerStrategy para esta funcionalidad."
        )
    }

    // Resultado de un intento de OCR: listo, reintentable (saturación/red) o fallo definitivo.
    private sealed interface OcrRes {
        data class Ok(val texto: String) : OcrRes
        data class Reintentar(val msg: String) : OcrRes
        data class Fallo(val msg: String) : OcrRes
    }

    /**
     * OCR resiliente: intenta cada modelo de [ocrModels] y, si Gemini está
     * saturado (429/503/"overloaded"/"high demand"), reintenta con una pausa
     * creciente antes de rendirse. Así un pico temporal no tira el escaneo.
     */
    fun procesarImagen(
        base64Image: String,
        prompt: String = "Describe el contenido de la imagen",
        mimeType: String = "image/jpeg"
    ): String {
        val inlineData = JSONObject().put("mime_type", mimeType).put("data", base64Image)
        val parts = org.json.JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(JSONObject().put("inline_data", inlineData))
        val body = JSONObject()
            .put("contents", org.json.JSONArray().put(JSONObject().put("parts", parts)))
            .toString()

        val esperasMs = longArrayOf(1500, 3500) // backoff entre reintentos del mismo modelo
        var ultimoMsg = "No se pudo leer la imagen."

        for (modelo in ocrModels) {
            for (intento in 0..esperasMs.size) {
                when (val r = intentarOcr(modelo, body)) {
                    is OcrRes.Ok -> return r.texto
                    is OcrRes.Fallo -> { ultimoMsg = r.msg; break } // no sirve reintentar este modelo → probar el siguiente
                    is OcrRes.Reintentar -> {
                        ultimoMsg = r.msg
                        println("⏳ OCR '$modelo' saturado (intento ${intento + 1}): $ultimoMsg")
                        if (intento < esperasMs.size) {
                            runCatching { Thread.sleep(esperasMs[intento]) }
                        }
                    }
                }
            }
        }
        return "IA no disponible: $ultimoMsg"
    }

    /** Un intento de OCR contra un modelo. Clasifica la respuesta para decidir si reintentar. */
    private fun intentarOcr(modelo: String, body: String): OcrRes {
        return try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${urlDe(modelo)}?key=$apiKey"))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(25))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            val json = JSONObject(response.body())

            when {
                json.has("candidates") && json.getJSONArray("candidates").length() > 0 -> {
                    val cand = json.getJSONArray("candidates").getJSONObject(0)
                    val partes = cand.optJSONObject("content")?.optJSONArray("parts")
                    val texto = if (partes != null && partes.length() > 0)
                        partes.getJSONObject(0).optString("text", "").replace("\\n", "\n") else ""
                    if (texto.isNotBlank()) OcrRes.Ok(texto)
                    else OcrRes.Fallo("La IA no pudo leer la imagen (motivo: ${cand.optString("finishReason", "desconocido")})")
                }
                json.has("error") -> {
                    val err = json.getJSONObject("error")
                    val msg = err.optString("message", "error desconocido")
                    val estado = err.optString("status", "")
                    if (esSaturacion(status, estado, msg)) OcrRes.Reintentar(msg) else OcrRes.Fallo(msg)
                }
                esSaturacion(status, "", "") -> OcrRes.Reintentar("HTTP $status")
                else -> OcrRes.Fallo("respuesta inesperada: " + response.body().take(200))
            }
        } catch (e: Exception) {
            // Errores de red/timeout: vale la pena reintentar.
            OcrRes.Reintentar(e.message ?: "error de red")
        }
    }

    /** ¿El error es un pico temporal (saturación/cuota/no disponible) que conviene reintentar? */
    private fun esSaturacion(status: Int, estado: String, msg: String): Boolean {
        if (status == 429 || status == 500 || status == 503) return true
        if (estado in setOf("UNAVAILABLE", "RESOURCE_EXHAUSTED", "INTERNAL")) return true
        val m = msg.lowercase()
        return listOf("overloaded", "high demand", "try again", "rate limit", "temporarily", "unavailable")
            .any { it in m }
    }
}