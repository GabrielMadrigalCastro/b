package kode.backend.backendkode

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

/**
 * Envía una imagen a Gemini (visión) para leer/transcribir su contenido.
 * Usa [GeminiStrategy.procesarImagen], que arma la petición multimodal real.
 */
@Service
class ImageProcessingService(
    private val gemini: GeminiStrategy
) {
    fun analyzeImage(file: MultipartFile, prompt: String): String {
        require(!file.isEmpty) { "La imagen está vacía" }
        val base64 = Base64.getEncoder().encodeToString(file.bytes)
        val mime = file.contentType?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        return gemini.procesarImagen(base64, prompt, mime)
    }
}
