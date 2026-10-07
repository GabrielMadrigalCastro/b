package kode.backend.backendkode

import org.json.JSONArray
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Proveedor GRATIS: Google Gemini (free tier de Google AI Studio).
 * Key en la variable de entorno GEMINI_API_KEY. Modelo con GEMINI_MODEL.
 */
@Component
class GeminiProvider(
    @Value("\${GEMINI_API_KEY:dummy-key}") private val apiKey: String,
    @Value("\${GEMINI_MODEL:gemini-flash-latest}") private val model: String
) : AIProvider {

    override val nombre = "gemini"

    override fun disponible(): Boolean = apiKey.isNotBlank() && apiKey != "dummy-key"

    override fun modelosDisponibles(): List<String> =
        if (disponible()) runCatching { listarModelosGemini(apiKey) }.getOrDefault(emptyList()) else emptyList()

    override fun generarTexto(prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            .toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val json = JSONObject(response.body())

        if (json.has("error")) {
            throw RuntimeException("gemini: " + json.getJSONObject("error").optString("message", "error"))
        }
        if (response.statusCode() !in 200..299 || !json.has("candidates")) {
            throw RuntimeException("gemini: respuesta inesperada (HTTP ${response.statusCode()})")
        }
        val texto = json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .optString("text", "")
        if (texto.isBlank()) throw RuntimeException("gemini: respuesta vacía")
        return texto
    }
}
