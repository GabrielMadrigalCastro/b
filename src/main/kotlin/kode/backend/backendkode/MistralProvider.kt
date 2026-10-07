package kode.backend.backendkode

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Proveedor de respaldo (capa gratuita): Mistral. API compatible con OpenAI.
 * Key en MISTRAL_API_KEY, modelo en MISTRAL_MODEL.
 *
 * Para usar OpenRouter en su lugar: poné la key de OpenRouter en MISTRAL_API_KEY
 * y ajustá MISTRAL_BASE_URL a https://openrouter.ai/api/v1/chat/completions con
 * un modelo :free en MISTRAL_MODEL.
 */
@Component
class MistralProvider(
    @Value("\${MISTRAL_API_KEY:dummy-key}") private val apiKey: String,
    @Value("\${MISTRAL_MODEL:mistral-small-latest}") private val model: String,
    @Value("\${MISTRAL_BASE_URL:https://api.mistral.ai/v1/chat/completions}") private val baseUrl: String
) : AIProvider {

    override val nombre = "mistral"

    override fun disponible(): Boolean = apiKey.isNotBlank() && apiKey != "dummy-key"

    override fun modelosDisponibles(): List<String> =
        if (disponible()) runCatching {
            listarModelosOpenAICompatible(baseUrl.replace("/chat/completions", "/models"), apiKey)
        }.getOrDefault(emptyList()) else emptyList()

    override fun generarTexto(prompt: String): String =
        llamarChatCompletions(
            url = baseUrl,
            apiKey = apiKey,
            model = model,
            prompt = prompt,
            proveedor = "mistral"
        )
}
