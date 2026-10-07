package kode.backend.backendkode

import org.springframework.stereotype.Component
import java.time.*
import java.time.format.TextStyle
import java.util.*
import kotlin.math.min

@Component("smartPlannerStrategy")
class SmartPlannerStrategy(
    private val classRepository: ClassRepository,
    private val userRepository: UserRepository,
    private val assignmentRepository: AssignmentRepository,
    private val studyPlanRepository: StudyPlanRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository
) : IAResponseStrategy {

    companion object {
        private val INICIO_DIA = LocalTime.of(8, 0)
        private val FIN_DIA = LocalTime.of(22, 0)

        private val RUTINA_MANANA = LocalTime.of(8, 0) to LocalTime.of(10, 0)
        private val ALMUERZO = LocalTime.of(12, 0) to LocalTime.of(13, 0)
        private val CENA = LocalTime.of(20, 0) to LocalTime.of(21, 0)

        private const val MINUTOS_TRANSPORTE = 60
        private val UBICACIONES_SIN_TRANSPORTE = setOf("CASA", "VIRTUAL", "ONLINE", "REMOTO")

        // Nuevas constantes para priorización
        private const val MIN_BLOQUE_MINUTOS = 30
        private const val MAX_BLOQUE_MINUTOS = 180 // 3 horas máximo por sesión
        private const val BLOQUE_OPTIMO_MINUTOS = 90 // 1.5 horas óptimo
    }

    // ========================================
    // MODELO DE DATOS PARA ASSIGNMENTS
    // ========================================
    data class AssignmentInfo(
        val assignment: Assignment,
        val studentAssignment: StudentAssignment?,
        val prioridad: Int, // 1-5 (1 = más urgente)
        val complejidad: Int, // 1-5 (5 = más complejo)
        val minutosEstimados: Int,
        val minutosCompletados: Int = 0
    ) {
        val minutosRestantes: Int get() = minutosEstimados - minutosCompletados
        val porcentajeCompletado: Double get() = if (minutosEstimados > 0)
            (minutosCompletados.toDouble() / minutosEstimados) * 100 else 0.0
        val diasHastaDueDate: Long get() = assignment.dueDate?.let {
            Duration.between(LocalDate.now().atStartOfDay(), it.atStartOfDay()).toDays()
        } ?: Long.MAX_VALUE
    }

    // ========================================
    // IMPLEMENTACIÓN IAResponseStrategy
    // ========================================
    override fun generarRespuesta(prompt: String): String {
        val params = prompt.split(" ").associate {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else it to ""
        }

        val studentId = params["student"]?.toLongOrNull()
            ?: throw IllegalArgumentException("Falta studentId")
        val assignmentId = params["assignment"]?.toLongOrNull()
        val dayOfWeek = params["day"]?.toIntOrNull() ?: LocalDate.now().dayOfWeek.value

        val student = userRepository.findById(studentId)
            .orElseThrow { NoSuchElementException("Student not found: $studentId") }

        if (assignmentId != null) {
            val assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow { NoSuchElementException("Assignment not found: $assignmentId") }
            generarPlanParaDia(student, assignment, LocalDate.now(), dayOfWeek)
        } else {
            generarPlanIntegralParaDia(student, LocalDate.now())
        }

        val dia = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale("es"))
        return "Plan generado para estudiante $studentId ($dia)"
    }

    override fun generarPlanEstudio(
        studentId: Long,
        assignmentId: Long,
        horasDisponiblesPorDia: Int,
        diasAntesDueDate: Int
    ): List<StudyPlanResult> {
        val student = userRepository.findById(studentId)
            .orElseThrow { NoSuchElementException("Student not found: $studentId") }

        // MODO 1: Plan para un assignment específico
        if (assignmentId > 0) {
            val assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow { NoSuchElementException("Assignment not found: $assignmentId") }
            return generarPlanParaAssignment(student, assignment, horasDisponiblesPorDia, diasAntesDueDate)
        }

        // MODO 2: Plan integral para todas las tareas pendientes
        return generarPlanIntegral(student, horasDisponiblesPorDia, diasAntesDueDate)
    }

    // ========================================
    // GENERADOR DE PLAN INTEGRAL (MULTI-ASSIGNMENT)
    // ========================================
    private fun generarPlanIntegral(
        student: User,
        horasDisponiblesPorDia: Int,
        diasPlanificacion: Int
    ): List<StudyPlanResult> {
        // 1. Obtener todas las tareas pendientes con su información
        val assignmentsInfo = obtenerAssignmentsPendientes(student)

        if (assignmentsInfo.isEmpty()) {
            return emptyList()
        }

        // 2. Ordenar por prioridad (urgencia + complejidad)
        val assignmentsOrdenados = priorizarAssignments(assignmentsInfo)

        // 3. Eliminar planes antiguos para regenerar
        val fechaInicio = LocalDate.now()
        val fechaFin = fechaInicio.plusDays(diasPlanificacion.toLong())

        studyPlanRepository.findAll()
            .filter { it.student.id == student.id &&
                    it.plannedDate != null &&
                    !it.plannedDate!!.isBefore(fechaInicio) &&
                    !it.plannedDate!!.isAfter(fechaFin) }
            .forEach { studyPlanRepository.delete(it) }

        // 4. Generar plan día por día
        val planesGenerados = mutableListOf<StudyPlan>()
        var fechaActual = fechaInicio

        while (!fechaActual.isAfter(fechaFin)) {
            val planesDelDia = generarPlanIntegralParaDia(
                student,
                fechaActual,
                assignmentsOrdenados
            )
            planesGenerados.addAll(planesDelDia)
            fechaActual = fechaActual.plusDays(1)
        }

        val guardados = studyPlanRepository.saveAll(planesGenerados)
        return guardados.map { toStudyPlanResult(it) }
    }

    // ========================================
    // OBTENER Y PRIORIZAR ASSIGNMENTS
    // ========================================
    private fun obtenerAssignmentsPendientes(student: User): List<AssignmentInfo> {
        return assignmentRepository.findAll()
            .filter { assignment ->
                // Filtrar assignments del estudiante (a través de enrollments)
                val estaEnCurso = assignment.course.enrollments.any {
                    it.student.id == student.id
                }

                // Verificar si está pendiente
                val studentAssignment = studentAssignmentRepository.findAll()
                    .firstOrNull {
                        it.assignment.id == assignment.id &&
                                it.student.id == student.id
                    }

                val estaPendiente = studentAssignment == null ||
                        studentAssignment.status !in setOf("COMPLETED", "SUBMITTED")

                estaEnCurso && estaPendiente
            }
            .map { assignment ->
                val studentAssignment = studentAssignmentRepository.findAll()
                    .firstOrNull {
                        it.assignment.id == assignment.id &&
                                it.student.id == student.id
                    }

                calcularInfoAssignment(assignment, studentAssignment)
            }
    }

    private fun calcularInfoAssignment(
        assignment: Assignment,
        studentAssignment: StudentAssignment?
    ): AssignmentInfo {
        // Calcular prioridad (1-5, donde 1 es más urgente)
        val diasHastaDueDate = assignment.dueDate?.let {
            Duration.between(LocalDate.now().atStartOfDay(), it.atStartOfDay()).toDays()
        } ?: 999L

        val prioridad = when {
            diasHastaDueDate <= 1 -> 1
            diasHastaDueDate <= 3 -> 2
            diasHastaDueDate <= 7 -> 3
            diasHastaDueDate <= 14 -> 4
            else -> 5
        }

        // Calcular complejidad basada en tipo de assignment
        val complejidad = when (assignment.type?.uppercase()) {
            "EXAM", "EXAMEN", "FINAL" -> 5
            "PROJECT", "PROYECTO" -> 4
            "HOMEWORK", "TAREA", "ASSIGNMENT" -> 3
            "QUIZ", "PRUEBA" -> 2
            else -> 3
        }

        // Estimar minutos según tipo y complejidad
        val minutosEstimados = when (assignment.type?.uppercase()) {
            "EXAM", "EXAMEN", "FINAL" -> 300 // 5 horas
            "PROJECT", "PROYECTO" -> 480 // 8 horas
            "HOMEWORK", "TAREA" -> 120 // 2 horas
            "QUIZ", "PRUEBA" -> 60 // 1 hora
            "ASSIGNMENT" -> 180 // 3 horas
            else -> 120
        }

        // Calcular minutos completados (simulado - podrías guardarlo en BD)
        val minutosCompletados = 0 // TODO: implementar tracking real

        return AssignmentInfo(
            assignment = assignment,
            studentAssignment = studentAssignment,
            prioridad = prioridad,
            complejidad = complejidad,
            minutosEstimados = minutosEstimados,
            minutosCompletados = minutosCompletados
        )
    }

    private fun priorizarAssignments(assignments: List<AssignmentInfo>): List<AssignmentInfo> {
        return assignments.sortedWith(
            compareBy<AssignmentInfo> { it.prioridad } // Primero por urgencia
                .thenByDescending { it.complejidad }    // Luego por complejidad (más difícil primero)
                .thenBy { it.diasHastaDueDate }         // Finalmente por fecha
        )
    }

    // ========================================
    // GENERADOR DE PLAN INTEGRAL PARA UN DÍA
    // ========================================
    private fun generarPlanIntegralParaDia(
        student: User,
        fecha: LocalDate,
        assignmentsOrdenados: List<AssignmentInfo> = obtenerAssignmentsPendientes(student)
    ): List<StudyPlan> {
        val clases = obtenerClasesDelDia(student.id!!, fecha)
        val planes = mutableListOf<StudyPlan>()

        // Bloques fijos
        planes.add(crearBloqueFijo(student, null, fecha, RUTINA_MANANA.first, RUTINA_MANANA.second, "Rutina personal"))
        planes.add(crearBloqueFijo(student, null, fecha, ALMUERZO.first, ALMUERZO.second, "Almuerzo"))
        planes.add(crearBloqueFijo(student, null, fecha, CENA.first, CENA.second, "Cena"))

        // Calcular espacios disponibles
        val espaciosDisponibles = calcularEspaciosDisponibles(clases, fecha)

        // Distribuir assignments en los espacios disponibles
        distribuirAssignmentsEnEspacios(
            student,
            fecha,
            espaciosDisponibles,
            assignmentsOrdenados,
            planes
        )

        return filtrarYValidar(planes)
    }

    private fun calcularEspaciosDisponibles(
        clases: List<ClassEntity>,
        fecha: LocalDate
    ): List<Pair<LocalTime, LocalTime>> {
        val espacios = mutableListOf<Pair<LocalTime, LocalTime>>()

        if (clases.isEmpty()) {
            espacios.add(RUTINA_MANANA.second to ALMUERZO.first)
            espacios.add(ALMUERZO.second to CENA.first)
            espacios.add(CENA.second to FIN_DIA)
            return espacios
        }

        val bloquesBloqueados = mutableListOf<Pair<LocalTime, LocalTime>>()

        // Agregar bloques de clases con transporte
        for (clase in clases.sortedBy { it.startTime }) {
            bloquesBloqueados.add(
                clase.startTime!!.minusMinutes(MINUTOS_TRANSPORTE.toLong()) to
                        clase.endTime!!.plusMinutes(MINUTOS_TRANSPORTE.toLong())
            )
        }

        // Agregar comidas
        bloquesBloqueados.add(ALMUERZO)
        bloquesBloqueados.add(CENA)

        // Encontrar espacios libres
        var cursor = RUTINA_MANANA.second
        for ((inicio, fin) in bloquesBloqueados.sortedBy { it.first }) {
            if (cursor.isBefore(inicio)) {
                val minutosDisponibles = Duration.between(cursor, inicio).toMinutes()
                if (minutosDisponibles >= MIN_BLOQUE_MINUTOS) {
                    espacios.add(cursor to inicio)
                }
            }
            cursor = maxOf(cursor, fin)
        }

        if (cursor.isBefore(FIN_DIA)) {
            val minutosDisponibles = Duration.between(cursor, FIN_DIA).toMinutes()
            if (minutosDisponibles >= MIN_BLOQUE_MINUTOS) {
                espacios.add(cursor to FIN_DIA)
            }
        }

        return espacios
    }

    private fun distribuirAssignmentsEnEspacios(
        student: User,
        fecha: LocalDate,
        espacios: List<Pair<LocalTime, LocalTime>>,
        assignmentsOrdenados: List<AssignmentInfo>,
        planes: MutableList<StudyPlan>
    ) {
        // Filtrar assignments que aún necesitan trabajo
        val assignmentsPendientes = assignmentsOrdenados
            .filter { it.minutosRestantes > 0 }
            .toMutableList()

        for ((inicioEspacio, finEspacio) in espacios) {
            var cursor = inicioEspacio
            val minutosDisponibles = Duration.between(inicioEspacio, finEspacio).toMinutes()

            if (minutosDisponibles < MIN_BLOQUE_MINUTOS) continue

            // Intentar llenar el espacio con assignments
            while (cursor.isBefore(finEspacio) && assignmentsPendientes.isNotEmpty()) {
                val minutosRestantes = Duration.between(cursor, finEspacio).toMinutes()
                if (minutosRestantes < MIN_BLOQUE_MINUTOS) break

                // Seleccionar assignment más prioritario que quepa
                val assignmentInfo = assignmentsPendientes.firstOrNull() ?: break

                // Calcular duración del bloque
                val duracionBloque = min(
                    min(minutosRestantes.toInt(), MAX_BLOQUE_MINUTOS),
                    assignmentInfo.minutosRestantes
                )

                if (duracionBloque < MIN_BLOQUE_MINUTOS) {
                    assignmentsPendientes.removeAt(0)
                    continue
                }

                val finBloque = cursor.plusMinutes(duracionBloque.toLong())

                // Crear bloque de estudio
                planes.add(
                    StudyPlan(
                        id = null,
                        student = student,
                        assignment = assignmentInfo.assignment,
                        plannedDate = fecha,
                        startTime = cursor,
                        endTime = finBloque,
                        status = "Tarea: ${assignmentInfo.assignment.title} (${assignmentInfo.assignment.type})",
                        generatedByAi = true
                    )
                )

                // Actualizar estado
                assignmentInfo.copy(
                    minutosCompletados = assignmentInfo.minutosCompletados + duracionBloque
                )

                if (assignmentInfo.minutosRestantes <= duracionBloque) {
                    assignmentsPendientes.removeAt(0)
                }

                cursor = finBloque

                // Break para evitar saturación
                if (duracionBloque >= BLOQUE_OPTIMO_MINUTOS) {
                    // Agregar pequeño descanso si es bloque largo
                    cursor = cursor.plusMinutes(15)
                }
            }
        }
    }

    // ========================================
    // GENERADOR DE PLAN PARA UN ASSIGNMENT ESPECÍFICO
    // ========================================
    private fun generarPlanParaAssignment(
        student: User,
        assignment: Assignment,
        horasDisponiblesPorDia: Int,
        diasAntesDueDate: Int
    ): List<StudyPlanResult> {
        val fechaFin = assignment.dueDate ?: LocalDate.now().plusDays(diasAntesDueDate.toLong())
        val fechaInicio = fechaFin.minusDays(diasAntesDueDate.toLong())

        val planesGenerados = mutableListOf<StudyPlan>()
        var fechaActual = fechaInicio

        while (!fechaActual.isAfter(fechaFin)) {
            val dayOfWeek = fechaActual.dayOfWeek.value
            planesGenerados.addAll(
                generarPlanParaDia(student, assignment, fechaActual, dayOfWeek)
            )
            fechaActual = fechaActual.plusDays(1)
        }

        val guardados = studyPlanRepository.saveAll(planesGenerados)
        return guardados.map { toStudyPlanResult(it) }
    }

    // ========================================
    // GENERADOR DE PLAN PARA UN DÍA (LEGACY - un solo assignment)
    // ========================================
    private fun generarPlanParaDia(
        student: User,
        assignment: Assignment,
        fecha: LocalDate,
        dayOfWeek: Int
    ): List<StudyPlan> {
        val clases = obtenerClasesDelDia(student.id!!, fecha)
        val planes = mutableListOf<StudyPlan>()

        planes.add(crearBloqueFijo(student, assignment, fecha, RUTINA_MANANA.first, RUTINA_MANANA.second, "Rutina personal"))
        planes.add(crearBloqueFijo(student, assignment, fecha, ALMUERZO.first, ALMUERZO.second, "Almuerzo"))
        planes.add(crearBloqueFijo(student, assignment, fecha, CENA.first, CENA.second, "Cena"))

        if (clases.isEmpty()) {
            planes.addAll(llenarHueco(student, assignment, fecha, RUTINA_MANANA.second, ALMUERZO.first))
            planes.addAll(llenarHueco(student, assignment, fecha, ALMUERZO.second, CENA.first))
            planes.addAll(llenarHueco(student, assignment, fecha, CENA.second, FIN_DIA))
        } else {
            val bloques = mutableListOf<Pair<LocalTime, LocalTime>>()
            val primeraClase = clases.first()
            bloques.add(primeraClase.startTime!!.minusMinutes(MINUTOS_TRANSPORTE.toLong()) to primeraClase.startTime!!)

            for (i in clases.indices) {
                val clase = clases[i]
                bloques.add(clase.startTime!! to clase.endTime!!)
                if (i < clases.lastIndex) {
                    val siguiente = clases[i + 1]
                    if (clase.endTime!!.isBefore(siguiente.startTime!!)) {
                        bloques.add(clase.endTime!! to siguiente.startTime!!)
                    }
                }
            }

            val ultimaClase = clases.last()
            bloques.add(ultimaClase.endTime!! to ultimaClase.endTime!!.plusMinutes(MINUTOS_TRANSPORTE.toLong()))

            var cursor = RUTINA_MANANA.second
            for ((inicio, fin) in bloques.sortedBy { it.first }) {
                if (cursor.isBefore(inicio)) {
                    planes.addAll(llenarHueco(student, assignment, fecha, cursor, inicio))
                }
                planes.add(crearBloqueFijo(student, assignment, fecha, inicio, fin, "Clase o traslado"))
                cursor = fin
            }

            if (cursor.isBefore(FIN_DIA)) {
                planes.addAll(llenarHueco(student, assignment, fecha, cursor, FIN_DIA))
            }
        }

        return filtrarYValidar(planes)
    }

    // ========================================
    // UTILITARIOS
    // ========================================
    private fun obtenerClasesDelDia(studentId: Long, fecha: LocalDate): List<ClassEntity> {
        val dayOfWeek = fecha.dayOfWeek.value
        return classRepository.findAll()
            .filter { clase ->
                val estaInscrito = clase.course.enrollments.any { it.student.id == studentId }
                val mismoDia = clase.classDate?.dayOfWeek?.value == dayOfWeek
                estaInscrito && mismoDia && clase.startTime != null && clase.endTime != null
            }
            .sortedBy { it.startTime }
    }

    private fun llenarHueco(
        student: User,
        assignment: Assignment,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime
    ): List<StudyPlan> {
        val planes = mutableListOf<StudyPlan>()
        var cursor = inicio

        if (cursor.isBefore(ALMUERZO.first) && fin.isAfter(ALMUERZO.first)) {
            planes.add(crearBloqueFijo(student, assignment, fecha, cursor, ALMUERZO.first, "Tarea / Estudio"))
            cursor = ALMUERZO.second
        }
        if (cursor.isBefore(CENA.first) && fin.isAfter(CENA.first)) {
            planes.add(crearBloqueFijo(student, assignment, fecha, cursor, CENA.first, "Tarea / Estudio"))
            cursor = CENA.second
        }

        if (cursor.isBefore(fin) && Duration.between(cursor, fin).toMinutes() >= MIN_BLOQUE_MINUTOS) {
            planes.add(crearBloqueFijo(student, assignment, fecha, cursor, fin, "Tarea / Estudio"))
        }

        return planes
    }

    private fun crearBloqueFijo(
        student: User,
        assignment: Assignment?,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        estado: String
    ): StudyPlan {
        return StudyPlan(
            id = null,
            student = student,
            assignment = assignment ?: assignmentRepository.findAll().firstOrNull()
            ?: throw IllegalStateException("No hay assignments disponibles"),
            plannedDate = fecha,
            startTime = inicio,
            endTime = fin,
            status = estado,
            generatedByAi = true
        )
    }

    private fun filtrarYValidar(planes: List<StudyPlan>): List<StudyPlan> {
        return planes
            .distinctBy { Triple(it.plannedDate, it.startTime, it.endTime) }
            .filter { Duration.between(it.startTime, it.endTime).toMinutes() > 0 }
            .sortedBy { it.startTime }
    }

    private fun toStudyPlanResult(plan: StudyPlan): StudyPlanResult {
        return StudyPlanResult(
            id = plan.id,
            studentId = plan.student.id!!,
            assignmentId = plan.assignment.id!!,
            plannedDate = plan.plannedDate,
            startTime = plan.startTime!!,
            endTime = plan.endTime!!,
            status = plan.status,
            generatedByAi = plan.generatedByAi
        )
    }

    fun obtenerPlanParaDia(studentId: Long, fecha: LocalDate): List<StudyPlan> {
        return studyPlanRepository.findAll()
            .filter { it.student.id == studentId && it.plannedDate == fecha }
            .sortedBy { it.startTime }
    }


    // ========================================
    // REPLANIFICACIÓN AUTOMÁTICA
    // ========================================
    fun replanificarAlAgregarTarea(studentId: Long, assignmentId: Long) {
        val student = userRepository.findById(studentId)
            .orElseThrow { NoSuchElementException("Student not found: $studentId") }

        // Regenerar plan integral desde hoy
        generarPlanIntegral(student, horasDisponiblesPorDia = 4, diasPlanificacion = 14)
    }
}