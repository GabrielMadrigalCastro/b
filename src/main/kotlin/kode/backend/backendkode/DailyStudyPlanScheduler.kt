package kode.backend.backendkode

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyStudyPlanScheduler(
    private val userRepository: UserRepository,
    private val studyPlanRepository: StudyPlanRepository,
    private val planner: SmartPlannerStrategy
) {

    // Se ejecuta todos los días a las 00:10 AM hora del servidor
    @Scheduled(cron = "0 10 0 * * *", zone = "America/Costa_Rica")
    fun generarPlanesDiarios() {
        println("🧠 [AI Scheduler] Verificando planes diarios...")

        val hoy = LocalDate.now()
        val estudiantes = userRepository.findAll().filter { it.role?.roleName?.equals("STUDENT", ignoreCase = true) == true }

        for (student in estudiantes) {
            val yaExiste = studyPlanRepository.findAll()
                .any { it.student.id == student.id && it.plannedDate == hoy }

            if (!yaExiste) {
                println("📅 Generando plan para estudiante ${student.id} ($hoy)")
                planner.obtenerPlanParaDia(student.id!!, hoy)

            } else {
                println("✅ Ya existe plan para estudiante ${student.id} ($hoy)")
            }
        }
    }
}
