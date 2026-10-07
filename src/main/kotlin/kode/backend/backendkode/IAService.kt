package kode.backend.backendkode

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * Servicio de planes de estudio (legado, usado por /v1/ia/studyplan).
 * El chat de texto libre se eliminó (la app no lo usa).
 * La generación de planes con IA + respaldo local vive en StudyPlanService.
 */
@Service
class IAService(
    // Planificador local determinista (respaldo siempre disponible, gratis).
    @Qualifier("smartPlannerStrategy") private val plannerStrategy: SmartPlannerStrategy
) {
    fun generarPlanEstudio(
        studentId: Long,
        assignmentId: Long,
        horasDisponiblesPorDia: Int,
        diasAntesDueDate: Int
    ): Any {
        return plannerStrategy.generarPlanEstudio(studentId, assignmentId, horasDisponiblesPorDia, diasAntesDueDate)
    }
}
