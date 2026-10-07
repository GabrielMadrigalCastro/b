package kode.backend.backendkode

import org.json.JSONArray
import org.json.JSONObject
import org.springframework.stereotype.Service

// ===================== DTOs del plan del día con IA =====================

/** Una clase del día (la manda la app, que ya tiene el horario y la modalidad). */
data class ClaseCtx(
    val curso: String = "Clase",
    val inicio: String,          // HH:MM
    val fin: String,             // HH:MM
    val modalidad: String = "Presencial",
    val lugar: String? = null
)

/** Un pendiente: tarea o examen (rúbrica con fecha). */
data class PendienteCtx(
    val titulo: String,
    val curso: String? = null,
    val fecha: String? = null    // YYYY-MM-DD
)

/** Contexto que la app envía para que la IA arme el plan del día. */
data class DiaIARequest(
    val fecha: String? = null,
    val clases: List<ClaseCtx> = emptyList(),
    val pendientes: List<PendienteCtx> = emptyList(),
    val horasEstudio: Int = 3,
    val estudioInicio: String = "15:00",
    val estudioFin: String = "21:00",
    val desayuno: String? = "07:00-07:30",
    val almuerzo: String? = "12:00-13:00",
    val cena: String? = "19:00-20:00",
    val viajeMinutos: Int = 60
)

/** Un bloque del plan (mismo espíritu que StudyPlanBlock de la app). */
data class BloquePlan(
    val inicio: String,          // HH:MM
    val fin: String,             // HH:MM
    val tipo: String,            // CLASS | TRAVEL | MEAL | STUDY | PERSONAL
    val titulo: String,
    val detalleCurso: String? = null,
    val detalleFecha: String? = null
)

data class DiaIAResponse(
    val generadoPor: String,     // "ia" o "error"
    val bloques: List<BloquePlan>,
    val detalle: String? = null  // motivo/diagnóstico cuando falla (la app lo ignora)
)

/**
 * Arma el "plan del día" con IA (cadena de proveedores gratis vía AIRouter),
 * respetando las reglas de clases, transporte consolidado, modalidad, comidas,
 * estudio y tiempo libre. Si la IA falla, lanza excepción para que la app use
 * su planificador local como respaldo.
 */
@Service
class PlanIAService(
    private val aiRouter: AIRouter,
    // El plan usa su propio orden: Groq primero (texto), y deja a Gemini de
    // último para reservar su cuota para las imágenes/OCR. Configurable en Render.
    @org.springframework.beans.factory.annotation.Value("\${AI_ORDER_PLAN:groq,mistral,gemini}")
    private val ordenPlan: String
) {

    fun generarPlanDiaIA(req: DiaIARequest): DiaIAResponse {
        val raw = aiRouter.generarTexto(construirPrompt(req), ordenPlan) // lanza si TODOS fallan
        println("🧩 Plan IA (raw, 500): " + raw.take(500).replace("\n", " "))

        val ini = raw.indexOf('[')
        val fin = raw.lastIndexOf(']')
        if (ini < 0 || fin <= ini) {
            throw RuntimeException("Sin arreglo JSON. La IA respondió: " + raw.take(160))
        }

        val arr = try {
            JSONArray(raw.substring(ini, fin + 1))
        } catch (e: Exception) {
            throw RuntimeException("JSON inválido (${e.message}). Fragmento: " + raw.substring(ini, minOf(fin + 1, ini + 160)))
        }

        // Parseo tolerante: acepta nombres de campo alternativos y salta bloques inservibles.
        val bloques = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val inicio = primero(o, "inicio", "start", "startTime", "hora_inicio", "horaInicio") ?: return@mapNotNull null
            val finB = primero(o, "fin", "end", "endTime", "hora_fin", "horaFin") ?: return@mapNotNull null
            BloquePlan(
                inicio = inicio,
                fin = finB,
                tipo = (primero(o, "tipo", "type") ?: "PERSONAL").uppercase(),
                titulo = primero(o, "titulo", "title", "descripcion", "description") ?: "",
                detalleCurso = primero(o, "detalleCurso", "curso", "course"),
                detalleFecha = primero(o, "detalleFecha", "fecha", "dueDate")
            )
        }.sortedBy { it.inicio }

        if (bloques.isEmpty()) {
            throw RuntimeException("La IA no devolvió bloques válidos. Claves del 1er ítem: " +
                (arr.optJSONObject(0)?.keySet()?.joinToString(",") ?: "—"))
        }
        return DiaIAResponse(generadoPor = "ia", bloques = bloques)
    }

    private fun optOrNull(o: JSONObject, k: String): String? =
        if (o.has(k) && !o.isNull(k)) o.getString(k).takeIf { it.isNotBlank() } else null

    /** Primer valor no vacío entre varias claves posibles. */
    private fun primero(o: JSONObject, vararg claves: String): String? =
        claves.firstNotNullOfOrNull { optOrNull(o, it) }

    private fun construirPrompt(r: DiaIARequest): String {
        val clasesTxt = if (r.clases.isEmpty()) "ninguna" else r.clases.joinToString("; ") {
            "${it.curso} ${it.inicio}-${it.fin} (${it.modalidad}${it.lugar?.let { l -> ", $l" } ?: ""})"
        }
        val pendTxt = if (r.pendientes.isEmpty()) "ninguno" else r.pendientes.joinToString("; ") {
            "${it.titulo}${it.curso?.let { c -> " ·$c" } ?: ""}${it.fecha?.let { f -> " (entrega $f)" } ?: ""}"
        }
        return """
            Armá el PLAN DEL DÍA de un estudiante universitario. Devolvé SOLO un arreglo JSON,
            sin texto extra ni markdown. Cada bloque:
            {"inicio":"HH:MM","fin":"HH:MM","tipo":"CLASS|TRAVEL|MEAL|STUDY|PERSONAL","titulo":"...","detalleCurso":"...","detalleFecha":"YYYY-MM-DD"}
            (detalleCurso y detalleFecha SOLO en bloques STUDY de una tarea/examen; en los demás omitilos).

            Datos:
            - Clases de hoy: $clasesTxt
            - Pendientes (tareas/exámenes): $pendTxt
            - Horas de estudio a repartir: ${r.horasEstudio}
            - Puede estudiar entre: ${r.estudioInicio} y ${r.estudioFin}
            - Comidas: desayuno ${r.desayuno}, almuerzo ${r.almuerzo}, cena ${r.cena}
            - Duración de un viaje a/desde la U: ${r.viajeMinutos} minutos

            REGLAS (obligatorias):
            1. Las clases van FIJAS en su horario, tipo CLASS (titulo = nombre del curso). No las muevas.
            2. TRANSPORTE (tipo TRAVEL) SOLO por clases PRESENCIALES: poné UN "Camino a la U" de ${r.viajeMinutos} min
               que termine justo cuando empieza la PRIMERA clase presencial, y UN "Viaje de regreso" de ${r.viajeMinutos} min
               que empiece cuando termina la ÚLTIMA clase presencial. Si hay 2 o más clases, NO pongas viajes entre ellas: se queda en la U.
            3. Las clases VIRTUALES no generan transporte.
            4. Si entre dos clases presenciales hay un hueco libre grande (más de ${r.viajeMinutos * 2} minutos),
               podés agregar "Viaje a casa" y luego "Regreso a la U" (tipo TRAVEL) para ir a descansar; si el hueco es corto, se queda en la U.
            5. Comidas (tipo MEAL): ubicá desayuno/almuerzo/cena en su horario; si choca con una clase o viaje, corré la comida a un rato libre cercano.
            6. Estudio (tipo STUDY): repartí ${r.horasEstudio} horas en los ratos libres dentro de ${r.estudioInicio}-${r.estudioFin},
               priorizando los pendientes por fecha de entrega más cercana. Titulo = "Estudiar: <pendiente>", con su detalleCurso y detalleFecha.
            7. El resto del tiempo libre relevante marcalo como PERSONAL con titulo "Tiempo libre".
            8. Sin solapamientos. Ordená por hora de inicio.

            Empezá con [ y terminá con ]. Nada más.
        """.trimIndent()
    }
}
