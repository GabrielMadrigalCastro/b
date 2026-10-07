package kode.backend.backendkode

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Proveedor GRATIS: Groq (modelos Llama). API compatible con OpenAI.
 * Key en GROQ_API_KEY, modelo en GROQ_MODEL. Solo texto (no imágenes).
 */
@Component
class GroqProvider(
    @Value("\${GROQ_API_KEY:dummy-key}") private val apiKey: String,
    @Value("\${GROQ_MODEL:qwen/qwen3.8-27b}") private val model: String
) : AIProvider {

    override val nombre = "groq"

    override fun disponible(): Boolean = apiKey.isNotBlank() && apiKey != "dummy-key"

    override fun modelosDisponibles(): List<String> =
        if (disponible()) runCatching {
            listarModelosOpenAICompatible("https://api.groq.com/openai/v1/models", apiKey)
        }.getOrDefault(emptyList()) else emptyList()

    override fun generarTexto(prompt: String): String =
        llamarChatCompletions(
            url = "https://api.groq.com/openai/v1/chat/completions",
            apiKey = apiKey,
            model = model,
            prompt = prompt,
            proveedor = "groq"
        )
}
