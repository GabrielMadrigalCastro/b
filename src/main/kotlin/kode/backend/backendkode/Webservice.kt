package kode.backend.backendkode

import org.springframework.http.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

/* ======================================================
   USER MANAGEMENT CONTROLLERS
   ====================================================== */

/* ---------- PRIVILEGE CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.privileges}")
class PrivilegeController(
    private val privilegeService: PrivilegeService
) {
    @GetMapping fun findAll() = privilegeService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = privilegeService.findById(id)
}

/* ---------- ROLE CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.roles}")
class RoleController(
    private val roleService: RoleService
) {
    @GetMapping fun findAll() = roleService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = roleService.findById(id)
}

/* ---------- USER CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.users}")
class UserController(
    private val userService: UserService
) {
    @GetMapping fun findAll() = userService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = userService.findById(id)

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody userInput: UserInput): UserResult? = userService.create(userInput)

    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun update(@PathVariable id: Long, @RequestBody userInput: UserInput): UserResult? =
        userService.update(id, userInput)

    @DeleteMapping("{id}")
    fun deleteById(@PathVariable id: Long): ResponseEntity<Void> {
        userService.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/signup")
    fun signUp(@RequestBody input: UserSignUpInput): ResponseEntity<UserResult> {
        println("========================================")
        println("🔍 DEBUG SIGNUP ENDPOINT")
        println("========================================")
        println("Request body completo: $input")
        println("fullName recibido: ${input.fullName}")
        println("email recibido: ${input.email}")
        println("roleId recibido: ${input.roleId}")
        println("========================================")
        return ResponseEntity.ok(userService.signUp(input))
    }
    @PostMapping("/login")
    fun login(@RequestBody credentials: UserLoginInput): ResponseEntity<UserResult> {
        println("========================================")
        println("🔐 DEBUG LOGIN ENDPOINT")
        println("========================================")
        println("Email recibido: ${credentials.email}")
        println("Password recibido: ${credentials.password}")
        println("========================================")

        val userOpt = userService.findByEmail(credentials.email)
        if (userOpt.isEmpty) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val user = userOpt.get()
        val encoder = BCryptPasswordEncoder()

        // 🔍 Verificar la contraseña
        if (!encoder.matches(credentials.password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // ✅ Generar respuesta compatible con el frontend
        val roleDetails = user.role?.let {
            RoleDetails(
                id = it.id,
                name = it.roleName,
                privileges = it.rolePrivileges.map { rp ->
                    PrivilegeDetails(
                        id = rp.privilege.id,
                        name = rp.privilege.name
                    )
                }
            )
        }

        val result = UserResult(
            id = user.id!!,
            fullName = user.fullName,
            email = user.email,
            enrollmentCard = user.enrollmentCard,
            cardVerified = user.cardVerified,
            tokenExpired = user.tokenExpired,
            createdAt = user.createdAt,
            role = roleDetails
        )

        println("✅ Login exitoso para ${user.email}, id=${user.id}, role=${user.role?.roleName}")
        return ResponseEntity.ok(result)
    }

}

/* ======================================================
   ACADEMIC MODULE CONTROLLERS
   ====================================================== */

/* ---------- COURSE CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.courses}")
class CourseController(
    private val courseService: CourseService
) {
    @GetMapping fun findAll() = courseService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = courseService.findById(id)
    @GetMapping("/student/{studentId}")
    fun getCoursesByStudent(@PathVariable studentId: Long) =
        courseService.findCoursesByStudentId(studentId)

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: CourseInput) = courseService.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: CourseInput) = courseService.update(id, input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = courseService.deleteById(id)

    /** Código del grupo del curso (para que el profe lo comparta). Lo genera si no tiene. */
    @GetMapping("{id}/join-code")
    fun joinCode(@PathVariable id: Long) = mapOf("joinCode" to courseService.getOrCreateJoinCode(id))
}

/* ---------- COURSE RUBRIC CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.courserubrics}")
class CourseRubricController(
    private val service: CourseRubricService
) {
    @GetMapping fun findAll() = service.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = service.findById(id)
    @GetMapping("course/{courseId}") fun findByCourse(@PathVariable courseId: Long) = service.findByCourse(courseId)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: CourseRubricInput) = service.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: CourseRubricInput) = service.update(id, input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = service.deleteById(id)
}

/* ---------- STUDENT RUBRIC GRADE CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.rubricgrades}")
class StudentRubricGradeController(
    private val service: StudentRubricGradeService
) {
    @GetMapping("/student/{studentId}")
    fun byStudent(@PathVariable studentId: Long) = service.findByStudent(studentId)

    @PutMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun upsert(@RequestBody input: StudentRubricGradeInput) = service.upsert(input)

    @DeleteMapping("/student/{studentId}/rubric/{rubricId}")
    fun delete(@PathVariable studentId: Long, @PathVariable rubricId: Long) =
        service.delete(studentId, rubricId)
}

/* ---------- NOTE (Apuntes) CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.notes}")
class NoteController(
    private val service: NoteService
) {
    @GetMapping("/student/{studentId}")
    fun byStudent(@PathVariable studentId: Long) = service.findByStudent(studentId)

    @GetMapping("/student/{studentId}/course/{courseId}")
    fun byStudentCourse(@PathVariable studentId: Long, @PathVariable courseId: Long) =
        service.findByStudentAndCourse(studentId, courseId)

    @GetMapping("{id}")
    fun findById(@PathVariable id: Long) = service.findById(id)

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody input: NoteInput) = service.create(input)

    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun update(@PathVariable id: Long, @RequestBody input: NoteInput) = service.update(id, input)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.deleteById(id)
}

/* ---------- ENROLLMENT CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.enrollments}")
class EnrollmentController(
    private val service: EnrollmentService
) {
    @GetMapping fun findAll() = service.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = service.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: EnrollmentInput) = service.create(input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = service.deleteById(id)

    /** El estudiante se une a un curso con el código del grupo. */
    @PostMapping("/join", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun join(@RequestBody input: JoinByCodeInput) = service.joinByCode(input)
}

/* ---------- CLASS CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.classes}")
class ClassController(
    private val service: ClassService
) {
    @GetMapping fun findAll() = service.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = service.findById(id)
    @GetMapping("/professor/{professorId}") fun getClassesByProfessor(@PathVariable professorId: Long) = service.findAllByProfessorId(professorId)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: ClassInput) = service.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: ClassInput) = service.update(id, input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = service.deleteById(id)
    @PatchMapping("/{id}/modality")
    fun updateModality(
        @PathVariable id: Long,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<ClassResult> {
        val modality = body["modality"] ?: return ResponseEntity.badRequest().build()
        val updated = service.updateModality(id, modality)
        return ResponseEntity.ok(updated)
    }

}

/* ---------- STUDENT CLASS PREFERENCES ---------- */
@RestController
@RequestMapping("\${url.studentclasspreferences}")
class StudentClassPreferenceController(
    private val service: StudentClassPreferenceService
) {
    @GetMapping fun findAll() = service.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = service.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: StudentClassPreferenceInput) = service.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: StudentClassPreferenceInput) = service.update(id, input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = service.deleteById(id)
}

/* ---------- ASSIGNMENT CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.assignments}")
class AssignmentController(
    private val service: AssignmentService
) {
    @GetMapping fun findAll() = service.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = service.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: AssignmentInput) = service.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: AssignmentInput) = service.update(id, input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = service.deleteById(id)
}

/* ---------- STUDENT ASSIGNMENT CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.studentassignments}")
class StudentAssignmentController(
    private val service: StudentAssignmentService
) {
    @GetMapping fun findAll() = service.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = service.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: StudentAssignmentInput) = service.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: StudentAssignmentInput) = service.update(id, input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = service.deleteById(id)
}

/* ======================================================
   TASK MANAGEMENT CONTROLLERS
   ====================================================== */

/* ---------- TASK CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.tasks}")
class TaskController(
    private val taskService: AbstractTaskService
) {

    @GetMapping fun findAll() = taskService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = taskService.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: TaskInput) = taskService.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: TaskInput) = taskService.update(id, input)
    @DeleteMapping("{id}") fun deleteById(@PathVariable id: Long) = taskService.deleteById(id)
    @PutMapping("{id}/done") fun markDone(@PathVariable id: Long) = taskService.markTaskAsDone(id)
    @DeleteMapping("{id}/permanent") fun deletePermanent(@PathVariable id: Long) = taskService.deleteTaskPermanently(id)
}


/* ---------- REMINDER CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.reminders}")
class ReminderController(
    private val reminderService: AbstractReminderService
) {
    @GetMapping fun findAll() = reminderService.findAll()
    @GetMapping("{taskId}/{reminderDate}")
    fun findById(@PathVariable taskId: Long, @PathVariable reminderDate: LocalDateTime) =
        reminderService.findById(taskId, reminderDate)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: ReminderInput) = reminderService.create(input)
    @DeleteMapping("{taskId}/{reminderDate}") fun deleteById(@PathVariable taskId: Long, @PathVariable reminderDate: LocalDateTime) = reminderService.deleteById(taskId, reminderDate)
}

/* ---------- PRIORITY CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.priorities}")
class PriorityController(private val priorityService: PriorityService) {
    @GetMapping fun findAll() = priorityService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = priorityService.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: PriorityInput) = priorityService.create(input)
}

/* ---------- STATUS CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.statuses}")
class StatusController(private val statusService: StatusService) {
    @GetMapping fun findAll() = statusService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = statusService.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: StatusInput) = statusService.create(input)
}

/* ---------- TOPIC CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.topics}")
class TopicController(private val topicService: TopicService) {
    @GetMapping fun findAll() = topicService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = topicService.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: TopicInput) = topicService.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: TopicInput) = topicService.update(id, input)
    @DeleteMapping("{id}") fun deleteById(@PathVariable id: Long) = topicService.deleteById(id)
}

/* ---------- QUESTION OPTION CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.questionOptions}")
class QuestionOptionController(private val questionOptionService: QuestionOptionService) {
    @GetMapping fun findAll() = questionOptionService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = questionOptionService.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: QuestionOptionInput) = questionOptionService.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: QuestionOptionInput) = questionOptionService.update(id, input)
    @DeleteMapping("{id}") fun deleteById(@PathVariable id: Long) = questionOptionService.deleteById(id)
}

/* ======================================================
   STUDY PLAN & AI CONTROLLERS
   ====================================================== */

/* ---------- STUDY PLAN CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.studyplans}")
class StudyPlanController(
    private val service: StudyPlanService
) {
    @GetMapping fun findAll() = service.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = service.findById(id)
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE]) fun create(@RequestBody input: StudyPlanInput) = service.create(input)
    @PutMapping("{id}", consumes = [MediaType.APPLICATION_JSON_VALUE]) fun update(@PathVariable id: Long, @RequestBody input: StudyPlanInput) = service.update(id, input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = service.deleteById(id)

    @PostMapping("/generar", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun generarPlanIA(@RequestBody request: GenerarPlanRequest) = service.generarPlanIA(request)
}


// ==================== IA CONTROLLER (ACTUALIZADO) ====================
@RestController
@RequestMapping("\${url.ia}")
class IAController(
    private val iaService: IAService
) {
    data class PlanRequest(
        val studentId: Long,
        val assignmentId: Long = -1, // -1 = modo integral
        val horasDisponiblesPorDia: Int = 4,
        val diasAntesDueDate: Int = 14
    )

    // El chat de texto libre (POST /v1/ia) se eliminó: la app no lo usa.

    @PostMapping("/studyplan")
    fun generarStudyPlan(@RequestBody request: PlanRequest): ResponseEntity<Map<String, Any>> =
        try {
            val planes = iaService.generarPlanEstudio(
                studentId = request.studentId,
                assignmentId = request.assignmentId,
                horasDisponiblesPorDia = request.horasDisponiblesPorDia,
                diasAntesDueDate = request.diasAntesDueDate
            )

            val planList = planes as? List<*> ?: emptyList<Any>()

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to if (request.assignmentId == -1L) {
                    "Plan integral generado para todas las tareas"
                } else {
                    "Plan generado para assignment específico"
                } as Any,
                "mode" to if (request.assignmentId == -1L) "integral" else "specific" as Any,
                "totalPlanes" to planList.size as Any,
                "planes" to planes
            ))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "status" to "error",
                "error" to (e.message ?: "Not found") as Any
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                "status" to "error",
                "error" to "Error al generar plan: ${e.message}" as Any
            ))
        }

    @GetMapping("/test")
    fun test(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf(
            "status" to "OK",
            "message" to "Servicio de IA funcionando correctamente",
            "version" to "3.0"
        ))}

/* ---------- PLAN DEL DÍA CON IA ---------- */
@RestController
@RequestMapping("\${url.studyplans}")
class PlanIAController(
    private val planIAService: PlanIAService,
    private val aiRouter: AIRouter
) {
    /**
     * La app manda su contexto (clases, pendientes, comidas, horas de estudio) y
     * la IA devuelve el plan del día respetando las reglas. Si la IA falla,
     * responde 503 con lista vacía para que la app use su planificador local.
     */
    @PostMapping("/dia-ia")
    fun planDiaIA(@RequestBody request: DiaIARequest): ResponseEntity<DiaIAResponse> =
        try {
            ResponseEntity.ok(planIAService.generarPlanDiaIA(request))
        } catch (e: Exception) {
            println("⚠️ Plan del día con IA falló (${e.message}); la app usará su plan local.")
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(DiaIAResponse(generadoPor = "error", bloques = emptyList(), detalle = e.message))
        }

    /**
     * Diagnóstico de las keys de IA: prueba cada proveedor y dice ok/falló y por
     * qué, sin exponer las keys. Útil para saber qué key arreglar.
     */
    @GetMapping("/ia-estado")
    fun iaEstado(): Map<String, Any> = mapOf(
        "orden" to aiRouter.orden(),
        "proveedores" to aiRouter.diagnostico()
    )
}

/* ---------- IA: features puntuales (resumen de apunte, consejo de notas) ---------- */
@RestController
@RequestMapping("\${url.ia}")
class IaFeaturesController(
    private val service: IaFeaturesService
) {
    /** Resumen + checklist de estudio de un apunte. 503 si la IA falla. */
    @PostMapping("/resumen-apunte", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun resumenApunte(@RequestBody req: ResumenApunteRequest): ResponseEntity<ResumenApunteResponse> =
        try {
            ResponseEntity.ok(service.resumirApunte(req))
        } catch (e: Exception) {
            println("⚠️ Resumen de apunte con IA falló (${e.message}).")
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResumenApunteResponse(resumen = "", checklist = emptyList(), generadoPor = "error"))
        }

    /** Una línea de consejo para la calculadora de notas. 503 si la IA falla. */
    @PostMapping("/consejo-notas", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun consejoNotas(@RequestBody req: ConsejoNotasRequest): ResponseEntity<ConsejoNotasResponse> =
        try {
            ResponseEntity.ok(service.consejoNotas(req))
        } catch (e: Exception) {
            println("⚠️ Consejo de notas con IA falló (${e.message}).")
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ConsejoNotasResponse(consejo = "", generadoPor = "error"))
        }
}

/* ---------- PLANNER CONTROLLER (Smart Planning) ---------- */
@RestController
@RequestMapping("\${url.studyplans}")
class PlannerController(
    private val smartPlanner: SmartPlannerStrategy,
    private val studyPlanService: StudyPlanService
) {
    // DTOs
    data class GenerarPlanDiarioRequest(
        val studentId: Long,
        val assignmentId: Long?,
        val fecha: LocalDate? = null
    )
    data class GenerarPlanSemanalRequest(
        val studentId: Long,
        val assignmentId: Long?,
        val fechaInicio: LocalDate? = null,
        val dias: Int = 7
    )
    data class ReplanificarRequest(
        val studentId: Long
    )

    // ===================== PLAN PARA UN DÍA ESPECÍFICO =====================
    @PostMapping("/dia")
    fun generarPlanDia(@RequestBody request: GenerarPlanDiarioRequest): ResponseEntity<Map<String, Any>> =
        try {
            val fecha = request.fecha ?: LocalDate.now()

            val prompt = if (request.assignmentId != null) {
                "student=${request.studentId} assignment=${request.assignmentId} day=${fecha.dayOfWeek.value}"
            } else {
                "student=${request.studentId} day=${fecha.dayOfWeek.value}"
            }

            val resultado = smartPlanner.generarRespuesta(prompt)

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to resultado as Any,
                "fecha" to fecha.toString() as Any,
                "dia" to fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")) as Any,
                "studentId" to request.studentId as Any,
                "assignmentId" to (request.assignmentId ?: "todos") as Any
            ))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "status" to "error",
                "message" to (e.message ?: "Not found") as Any
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                "status" to "error",
                "message" to "Error al generar plan: ${e.message}" as Any
            ))
        }

    // ===================== PLAN SEMANAL (MEJORADO) =====================
    @PostMapping("/semana")
    fun generarPlanSemanal(@RequestBody request: GenerarPlanSemanalRequest): ResponseEntity<Map<String, Any>> =
        try {
            val planes = if (request.assignmentId != null) {
                // Modo específico para un assignment
                smartPlanner.generarPlanEstudio(
                    studentId = request.studentId,
                    assignmentId = request.assignmentId,
                    horasDisponiblesPorDia = 4,
                    diasAntesDueDate = request.dias
                )
            } else {
                // Modo integral: todas las tareas
                studyPlanService.generarPlanIntegral(
                    studentId = request.studentId,
                    dias = request.dias
                )
            }

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Plan generado para ${request.dias} días" as Any,
                "totalBloques" to planes.size as Any,
                "studentId" to request.studentId as Any,
                "assignmentId" to (request.assignmentId ?: "todos") as Any,
                "planes" to planes as Any
            ))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf(
                "status" to "error",
                "message" to (e.message ?: "Not found") as Any
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                "status" to "error",
                "message" to "Error al generar plan: ${e.message}" as Any
            ))
        }

    // ===================== NUEVO: REPLANIFICAR AUTOMÁTICAMENTE =====================
    @PostMapping("/replanificar")
    fun replanificar(@RequestBody request: ReplanificarRequest): ResponseEntity<Map<String, Any>> =
        try {
            val planes = studyPlanService.generarPlanIntegral(
                studentId = request.studentId,
                dias = 14
            )

            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Plan replanificado exitosamente" as Any,
                "totalBloques" to planes.size as Any,
                "studentId" to request.studentId as Any,
                "planes" to planes as Any
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                "status" to "error",
                "message" to "Error al replanificar: ${e.message}" as Any
            ))
        }

    // ===================== OBTENER PLAN DEL DÍA =====================
    @GetMapping("/day-plan")
    fun getDayPlan(
        @RequestParam studentId: Long,
        @RequestParam(required = false) fecha: String?,
        @RequestParam(required = false) hora: String?
    ): ResponseEntity<Any> {
        val date = fecha?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val currentTime = hora?.let { LocalTime.parse(it) } ?: LocalTime.now()

        val planesDelDia = smartPlanner.obtenerPlanParaDia(studentId, date)

        val bloques = planesDelDia.map {
            mapOf(
                "id" to it.id,
                "assignmentTitle" to it.assignment.title,
                "assignmentType" to it.assignment.type,
                "status" to it.status,
                "startTime" to it.startTime.toString(),
                "endTime" to it.endTime.toString(),
                "isActive" to (currentTime >= it.startTime && currentTime <= it.endTime),
                "durationMinutes" to java.time.Duration.between(it.startTime, it.endTime).toMinutes()
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "date" to date.toString(),
                "currentTime" to currentTime.toString(),
                "totalBloques" to bloques.size,
                "bloques" to bloques
            )
        )
    }


    /* ======================================================
       IMAGE PROCESSING CONTROLLER (CLOUD AI)
       ====================================================== */
    @RestController
    @RequestMapping("\${url.imageprocessing}")
    class ImageProcessingController(
        private val imageProcessingService: ImageProcessingService
    ) {
        @PostMapping("/analyze", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
        fun analyzeImage(
            @RequestParam("file") file: MultipartFile,
            @RequestParam("prompt", required = false, defaultValue = "Describe esta imagen") prompt: String
        ): ResponseEntity<Map<String, String>> =
            try {
                ResponseEntity.ok(mapOf("text" to imageProcessingService.analyzeImage(file, prompt)))
            } catch (e: Exception) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to (e.message ?: "Error procesando imagen")))
            }
    }}



/* ---------- ALERT CONTROLLER ---------- */
@RestController
@RequestMapping("\${url.alerts}")
class AlertController(
    private val alertService: AlertService
) {
    @GetMapping fun findAll() = alertService.findAll()
    @GetMapping("{id}") fun findById(@PathVariable id: Long) = alertService.findById(id)
    @GetMapping("/today") fun getAlertOfDay() = alertService.getAlertOfDay()
    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody input: AlertInput) = alertService.create(input)
    @DeleteMapping("{id}") fun delete(@PathVariable id: Long) = alertService.deleteById(id)

    @GetMapping("/professor/{professorId}/today")
    fun getAlertsByProfessorToday(@PathVariable professorId: Long): List<AlertResult> =
        alertService.findAllByProfessorToday(professorId)

}

/* ---------- HEALTH CHECK (público) ---------- */
// Endpoint público para keep-alive/monitoreo (UptimeRobot). Cae bajo
// /v1/unsecure/** que Spring Security permite sin JWT, así devuelve 200.
@RestController
@RequestMapping("/\${vs}/unsecure")
class HealthController {
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("status" to "UP"))
}



