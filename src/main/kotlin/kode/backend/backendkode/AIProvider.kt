package kode.backend.backendkode

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Proveedor de IA de texto. Cada implementación (Gemini, Groq, Mistral…) llama a
 * su propia API. Todos comparten el mismo contrato para poder encadenarlos con
 * respaldo (fallback) en [AIRouter].
 *
 * Regla clave: [generarTexto] DEBE lanzar excepción si falla (error, timeout o
 * sin cuota); así el router pasa automáticamente al siguiente proveedor.
 */
interface AIProvider {
    /** Nombre corto para el orden y los logs: "gemini", "groq", "mistral". */
    val nombre: String

    /** true si tiene API key configurada (no la de por defecto). */
    fun disponible(): Boolean

    /** Genera texto. Lanza excepción si el proveedor falla. */
    fun generarTexto(prompt: String): String

    /** Modelos disponibles para esta key (para el diagnóstico). Vacío si no aplica. */
    fun modelosDisponibles(): List<String> = emptyList()
}

/** Lista modelos de una API compatible con OpenAI (Groq, Mistral): GET /models. */
internal fun listarModelosOpenAICompatible(modelsUrl: String, apiKey: String): List<String> {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(modelsUrl))
        .timeout(Duration.ofSeconds(20))
        .header("Authorization", "Bearer $apiKey")
        .GET()
        .build()
    val response = aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
    val json = JSONObject(response.body())
    if (!json.has("data")) return emptyList()
    val arr = json.getJSONArray("data")
    return (0 until arr.length()).map { arr.getJSONObject(it).optString("id") }.filter { it.isNotBlank() }
}

/** Lista modelos de Gemini que soportan generateContent. */
internal fun listarModelosGemini(apiKey: String): List<String> {
    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey&pageSize=200"))
        .timeout(Duration.ofSeconds(20))
        .GET()
        .build()
    val response = aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
    val json = JSONObject(response.body())
    if (!json.has("models")) return emptyList()
    val arr = json.getJSONArray("models")
    return (0 until arr.length()).mapNotNull { i ->
        val m = arr.getJSONObject(i)
        val methods = m.optJSONArray("supportedGenerationMethods")
        val soportaChat = methods != null && (0 until methods.length()).any { methods.getString(it) == "generateContent" }
        if (soportaChat) m.optString("name").removePrefix("models/").ifBlank { null } else null
    }
}

/** Cliente HTTP compartido con timeout de conexión. */
internal val aiHttpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

/**
 * Llama a una API compatible con OpenAI (Groq, Mistral, OpenRouter…) y devuelve
 * el texto. Lanza excepción si hay error, status no 2xx o respuesta inesperada.
 */
internal fun llamarChatCompletions(
    url: String,
    apiKey: String,
    model: String,
    prompt: String,
    proveedor: String
): String {
    val body = JSONObject()
        .put("model", model)
        .put("max_tokens", 4096)
        .put("temperature", 0.2)
        .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        .toString()

    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(30))
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()

    val response = aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
    val json = JSONObject(response.body())

    if (json.has("error")) {
        val err = json.get("error")
        val msg = if (err is JSONObject) err.optString("message", "error") else err.toString()
        throw RuntimeException("$proveedor: $msg")
    }
    if (response.statusCode() !in 200..299) {
        throw RuntimeException("$proveedor: HTTP ${response.statusCode()}")
    }
    val contenido = json.getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .optString("content", "")
    if (contenido.isBlank()) throw RuntimeException("$proveedor: respuesta vacía")
    return contenido
}
