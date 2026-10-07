package kode.backend.backendkode

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.time.LocalDateTime
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.LocalDate


// ==================== PRIVILEGE SERVICE ====================






// ==================== USER SERVICE ====================





@Service
@Transactional
class AppUserDetailsService(
    @field:Autowired
    val userRepository: UserRepository,
    @field:Autowired
    val roleRepository: RoleRepository,
) : UserDetailsService {

    /**
     * Carga el usuario por su email y construye el objeto UserDetails
     * utilizado por Spring Security durante la autenticación.
     *
     * @param username El email del usuario a autenticar.
     * @return Un objeto UserDetails con los roles y privilegios del usuario.
     * @throws UsernameNotFoundException Si el usuario no existe en la base de datos.
     */
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        val userAuth: org.springframework.security.core.userdetails.User
        val user: User = userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("User not found with email: $username") }

        userAuth = org.springframework.security.core.userdetails.User(
            user.email,
            user.passwordHash,
            user.cardVerified, // habilitado si la tarjeta está verificada
            true,
            true,
            true,
            getAuthorities(listOfNotNull(user.role))
        )

        return userAuth
    }

    /**
     * Genera la lista de GrantedAuthority a partir de los roles y privilegios del usuario.
     */
    private fun getAuthorities(roles: Collection<Role>): Collection<GrantedAuthority> {
        return roles.flatMap { role ->
            sequenceOf(SimpleGrantedAuthority(role.roleName)) +
                    role.rolePrivileges.map { rp -> SimpleGrantedAuthority(rp.privilege.name) }
        }.toList()
    }

    /**
     * Retorna la información detallada del usuario (incluyendo rol y privilegios)
     * para usar en controladores o respuesta del login.
     */
    fun getUserDetails(username: String): UserResult {
        val user = userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("User not found with email: $username") }

        // Construir lista de privilegios del rol
        val privileges = user.role?.rolePrivileges?.map { rp ->
            PrivilegeDetails(
                id = rp.privilege.id,
                name = rp.privilege.name
            )
        } ?: emptyList()

        // Construir el detalle del rol con privilegios
        val roleDetails = user.role?.let {
            RoleDetails(
                id = it.id,
                name = it.roleName,
                privileges = privileges
            )
        }

        // Devolver el DTO completo
        return UserResult(
            id = user.id!!,
            fullName = user.fullName,
            email = user.email,
            enrollmentCard = user.enrollmentCard,
            cardVerified = user.cardVerified,
            tokenExpired = user.tokenExpired,
            createdAt = user.createdAt,
            role = roleDetails
        )
    }

}

// ==================== TASK SERVICE ====================

@Service
class AbstractTaskService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val priorityRepository: PriorityRepository,
    private val statusRepository: StatusRepository,
    @Qualifier("taskMapperImpl")
    private val mapper: TaskMapper
) {

    fun findAll(): List<TaskResult> {
        val allTasks = taskRepository.findAll()
        // Filtra tareas DONE
        val activeTasks = allTasks.filter {
            it.status?.label?.uppercase() !in listOf("DONE", "COMPLETED", "FINISHED")
        }
        return mapper.toResultList(activeTasks)
    }

    @Throws(NoSuchElementException::class)
    fun findById(id: Long): TaskResult {
        val task = taskRepository.findById(id)
            .orElseThrow { NoSuchElementException("Task with id $id not found!") }
        return mapper.toResult(task)
    }

    @Transactional
    fun create(input: TaskInput): TaskResult {
        val task = mapper.toEntity(input)

        // Validar usuario
        val user = userRepository.findById(input.userId)
            .orElseThrow { NoSuchElementException("User with id ${input.userId} not found!") }
        task.user = user

        // Asignar prioridad y estado
        assignPriorityAndStatus(task, input)

        val saved = taskRepository.save(task)
        return mapper.toResult(saved)
    }

    @Transactional
    @Throws(NoSuchElementException::class)
    fun update(id: Long, input: TaskInput): TaskResult {
        val existing = taskRepository.findById(id)
            .orElseThrow { NoSuchElementException("Task with id $id not found!") }

        // Actualizar campos básicos
        existing.title = input.title
        existing.notes = input.notes
        existing.dueDate = input.dueDate

        // Validar usuario
        existing.user = userRepository.findById(input.userId)
            .orElseThrow { NoSuchElementException("User with id ${input.userId} not found!") }

        // Asignar prioridad y estado (evita código duplicado)
        assignPriorityAndStatus(existing, input)

        val saved = taskRepository.save(existing)
        return mapper.toResult(saved)
    }

    @Throws(NoSuchElementException::class)
    fun deleteById(id: Long) {
        if (!taskRepository.existsById(id)) {
            throw NoSuchElementException("Task with id $id not found!")
        }
        taskRepository.deleteById(id)
    }

    /*
      Asigna prioridad y estado a una tarea según los ID's enviados en el input.
      Este método elimina la duplicación de código entre create() y update().
     */
    private fun assignPriorityAndStatus(task: Task, input: TaskInput) {
        task.priority = when {
            input.priorityId != null -> priorityRepository.findById(input.priorityId).orElse(null)
            !input.priorityLabel.isNullOrBlank() -> priorityRepository.findByLabelIgnoreCase(input.priorityLabel)
            else -> task.priority
        }

        task.status = when {
            input.statusId != null -> statusRepository.findById(input.statusId).orElse(null)
            !input.statusLabel.isNullOrBlank() -> statusRepository.findByLabelIgnoreCase(input.statusLabel)
            else -> task.status
        }
    }
    @Transactional
    fun markTaskAsDone(taskId: Long): TaskResult {
        val task = taskRepository.findById(taskId)
            .orElseThrow { NoSuchElementException("Task with id $taskId not found!") }

        // Evita marcar de nuevo si ya está DONE
        if (task.status?.label?.uppercase() in listOf("DONE", "COMPLETED", "FINISHED")) {
            return mapper.toResult(task) // devuelve la tarea tal cual
        }

        val doneStatus = statusRepository.findByLabelIgnoreCase("DONE")
            ?: throw IllegalStateException("Status DONE no existe")
        task.status = doneStatus

        val saved = taskRepository.save(task)
        return mapper.toResult(saved)
    }

    @Transactional
    fun deleteTaskPermanently(taskId: Long) {
        if (!taskRepository.existsById(taskId)) {
            throw NoSuchElementException("Task with id $taskId not found!")
        }
        taskRepository.deleteById(taskId)
    }


}

// ==================== TASK REMINDER ====================
@Service
class AbstractReminderService(
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    @Qualifier("reminderMapperImpl")
    private val mapper: ReminderMapper
) {

    fun findAll(): List<ReminderDetails> {
        val reminders = reminderRepository.findAll()
        println("DEBUG - Found ${reminders.size} reminders")
        reminders.forEach {
            println("DEBUG - Reminder: taskId=${it.id.taskId}, date=${it.id.reminderDate}")
        }
        return mapper.toDetailsList(reminders)
    }

    @Throws(NoSuchElementException::class)
    fun findById(taskId: Long, date: LocalDateTime): ReminderDetails {
        val reminder = reminderRepository.findById(ReminderId(taskId, date))
            .orElseThrow { NoSuchElementException("Reminder for task $taskId on $date not found!") }
        return mapper.toDetails(reminder)
    }

    @Transactional
    fun create(input: ReminderInput): ReminderDetails {
        println("DEBUG - Creating reminder for taskId: ${input.taskId}, date: ${input.reminderDate}")

        val task = taskRepository.findById(input.taskId)
            .orElseThrow { NoSuchElementException("Task with id ${input.taskId} not found!") }

        println("DEBUG - Task found: ${task.id}")

        // Crear el ReminderId con los valores del input
        val reminderId = ReminderId(
            taskId = input.taskId,
            reminderDate = input.reminderDate
        )

        println("DEBUG - ReminderId created: taskId=${reminderId.taskId}, date=${reminderId.reminderDate}")

        // Crear el Reminder con el ID compuesto y el task
        val reminder = Reminder(
            id = reminderId,
            task = task
        )

        println("DEBUG - Reminder entity created")

        // Guardar
        val saved = reminderRepository.save(reminder)

        println("DEBUG - Reminder saved: taskId=${saved.id.taskId}, date=${saved.id.reminderDate}")

        // Convertir a DTO
        val result = ReminderDetails(
            taskId = saved.id.taskId,
            reminderDate = saved.id.reminderDate
        )

        println("DEBUG - Returning DTO: taskId=${result.taskId}, date=${result.reminderDate}")

        return result
    }

    @Transactional
    @Throws(NoSuchElementException::class)
    fun deleteById(taskId: Long, date: LocalDateTime) {
        val id = ReminderId(taskId, date)
        if (!reminderRepository.existsById(id)) {
            throw NoSuchElementException("Reminder for task $taskId on $date not found!")
        }
        reminderRepository.deleteById(id)
    }
}
@Service
class PriorityService(
    private val repo: PriorityRepository
) {
    fun findAll(): List<PriorityResult> = repo.findAll().map { PriorityResult(it.id!!, it.label) }

    fun findById(id: Long): PriorityResult {
        val p = repo.findById(id).orElseThrow { NoSuchElementException("Priority $id not found") }
        return PriorityResult(p.id!!, p.label)
    }

    @Transactional
    fun create(input: PriorityInput): PriorityResult {
        val entity = Priority(label = input.label)
        val saved = repo.save(entity)
        return PriorityResult(saved.id!!, saved.label)
    }
}

@Service
class StatusService(
    private val repo: StatusRepository
) {
    fun findAll(): List<StatusResult> = repo.findAll().map { StatusResult(it.id!!, it.label) }

    fun findById(id: Long): StatusResult {
        val s = repo.findById(id).orElseThrow { NoSuchElementException("Status $id not found") }
        return StatusResult(s.id!!, s.label)
    }

    @Transactional
    fun create(input: StatusInput): StatusResult {
        val entity = Status(label = input.label)
        val saved = repo.save(entity)
        return StatusResult(saved.id!!, saved.label)
    }
}

// ==================== TOPIC SERVICE ====================
@Service
class TopicService(
    private val topicRepository: TopicRepository,
    private val courseRepository: CourseRepository,
    private val topicMapper: TopicMapper
) {
    fun findAll(): List<TopicResult> =
        topicMapper.toResultList(topicRepository.findAll())

    @Throws(NoSuchElementException::class)
    fun findById(id: Long): TopicResult =
        topicMapper.toResult(
            topicRepository.findById(id).orElseThrow { NoSuchElementException("Topic not found: $id") }
        )

    @Transactional
    fun create(input: TopicInput): TopicResult {
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }

        val entity = Topic(
            id = null,
            course = course,
            name = input.name,
            source = input.source,
            createdAt = LocalDateTime.now()
        )
        val saved = topicRepository.save(entity)
        return topicMapper.toResult(saved)
    }

    @Transactional
    fun update(id: Long, input: TopicInput): TopicResult {
        val current = topicRepository.findById(id)
            .orElseThrow { NoSuchElementException("Topic not found: $id") }
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }

        val updated = Topic(
            id = current.id,
            course = course,
            name = input.name,
            source = input.source,
            createdAt = current.createdAt
        )
        val saved = topicRepository.save(updated)
        return topicMapper.toResult(saved)
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!topicRepository.existsById(id)) throw NoSuchElementException("Topic not found: $id")
        topicRepository.deleteById(id)
    }
}

// ==================== QUESTION OPTION SERVICE ====================
@Service
class QuestionOptionService(
    private val questionOptionRepository: QuestionOptionRepository,
    private val questionRepository: QuestionRepository,
    @Qualifier("questionOptionMapperImpl")
    private val mapper: QuestionOptionMapper
) {
    fun findAll(): List<QuestionOptionResult> =
        mapper.toResultList(questionOptionRepository.findAll())

    @Throws(NoSuchElementException::class)
    fun findById(id: Long): QuestionOptionResult =
        mapper.toResult(
            questionOptionRepository.findById(id).orElseThrow { NoSuchElementException("QuestionOption not found: $id") }
        )

    @Transactional
    fun create(input: QuestionOptionInput): QuestionOptionResult {
        val question = questionRepository.findById(input.questionId)
            .orElseThrow { NoSuchElementException("Question not found: ${input.questionId}") }

        val entity = QuestionOption(
            id = null,
            question = question,
            label = input.label,
            text = input.text
        )
        val saved = questionOptionRepository.save(entity)
        return mapper.toResult(saved)
    }

    @Transactional
    fun update(id: Long, input: QuestionOptionInput): QuestionOptionResult {
        val current = questionOptionRepository.findById(id)
            .orElseThrow { NoSuchElementException("QuestionOption not found: $id") }
        val question = questionRepository.findById(input.questionId)
            .orElseThrow { NoSuchElementException("Question not found: ${input.questionId}") }

        val updated = QuestionOption(
            id = current.id,
            question = question,
            label = input.label,
            text = input.text
        )
        val saved = questionOptionRepository.save(updated)
        return mapper.toResult(saved)
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!questionOptionRepository.existsById(id)) throw NoSuchElementException("QuestionOption not found: $id")
        questionOptionRepository.deleteById(id)
    }
}
// ==================== users ====================


// ==================== PRIVILEGE SERVICE ====================

interface PrivilegeService {
    fun findAll(): List<PrivilegeDetails>?
    fun findById(id: Long): PrivilegeDetails?
}

@Service
class AbstractPrivilegeService(
    @field:Autowired val privilegeRepository: PrivilegeRepository,
    @field:Autowired val privilegeMapper: PrivilegeMapper
) : PrivilegeService {

    override fun findAll(): List<PrivilegeDetails>? {
        return privilegeMapper.privilegeListToPrivilegeDetailsList(
            privilegeRepository.findAll()
        )
    }

    override fun findById(id: Long): PrivilegeDetails? {
        val privilege: Optional<Privilege> = privilegeRepository.findById(id)
        if (privilege.isEmpty) {
            throw NoSuchElementException(String.format("Privilege with id %s not found!", id))
        }
        return privilegeMapper.privilegeToPrivilegeDetails(privilege.get())
    }
}

// ==================== ROLE SERVICE ====================

interface RoleService {
    fun findAll(): List<RoleDetails>?
    fun findById(id: Long): RoleDetails?
}

@Service
class AbstractRoleService(
    @field:Autowired val roleRepository: RoleRepository,
    @field:Autowired val roleMapper: RoleMapper
) : RoleService {

    override fun findAll(): List<RoleDetails>? {
        return roleMapper.roleListToRoleDetailsList(roleRepository.findAll())
    }

    override fun findById(id: Long): RoleDetails? {
        val role: Optional<Role> = roleRepository.findById(id)
        if (role.isEmpty) {
            throw NoSuchElementException(String.format("Role with id %s not found!", id))
        }
        return roleMapper.roleToRoleDetails(role.get())
    }
}

// =====================================================
// USER SERVICE
// =====================================================

interface UserService {
    fun findAll(): List<UserResult>?
    fun findById(id: Long): UserResult?
    fun create(userInput: UserInput): UserResult?
    fun update(id: Long, userInput: UserInput): UserResult?
    fun deleteById(id: Long)
    fun signUp(input: UserSignUpInput): UserResult
    fun findByEmail(email: String): Optional<User>

}

@Service
class AbstractUserService(
    @field:Autowired val userRepository: UserRepository,
    @field:Autowired val userMapper: UserMapper,
    @field:Autowired val roleRepository: RoleRepository
) : UserService {

    override fun findAll(): List<UserResult>? =
        userMapper.userListToUserResultList(userRepository.findAll())

    @Throws(NoSuchElementException::class)
    override fun findById(id: Long): UserResult? {
        val user = userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User with id $id not found!") }
        return userMapper.userToUserResult(user)
    }

    override fun findByEmail(email: String): Optional<User> =
        userRepository.findByEmail(email)

    @Transactional
    override fun create(userInput: UserInput): UserResult? {
        val userEntity = userMapper.userInputToUser(userInput)
        val userWithRole = if (userInput.roleId != null) {
            val role = roleRepository.findById(userInput.roleId!!)
                .orElseThrow { NoSuchElementException("Role with id ${userInput.roleId} not found!") }
            userEntity.copy(role = role)
        } else userEntity

        val savedUser = userRepository.save(userWithRole)
        return userMapper.userToUserResult(savedUser)
    }

    @Transactional
    @Throws(NoSuchElementException::class)
    override fun update(id: Long, userInput: UserInput): UserResult {
        val existing = userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User with id $id not found!") }

        val updated = existing.copy(
            fullName = userInput.fullName ?: existing.fullName,
            email = userInput.email ?: existing.email,
            enrollmentCard = userInput.enrollmentCard ?: existing.enrollmentCard,
            cardVerified = userInput.cardVerified ?: existing.cardVerified,
            role = userInput.roleId?.let {
                roleRepository.findById(it)
                    .orElseThrow { NoSuchElementException("Role with id $it not found!") }
            } ?: existing.role
        )

        val saved = userRepository.save(updated)
        return userMapper.userToUserResult(saved)
    }

    @Throws(NoSuchElementException::class)
    override fun deleteById(id: Long) {
        if (!userRepository.existsById(id))
            throw NoSuchElementException("User with id $id not found!")
        userRepository.deleteById(id)
    }

    // =====================================================
    // Registro Publico de Usuario
    // =====================================================
    @Transactional
    override fun signUp(input: UserSignUpInput): UserResult {
        println("🔍 DEBUG SERVICE - signUp()")
        println("Input recibido en service: $input")

        if (input.email.isNullOrBlank() || input.password.isNullOrBlank()) {
            throw IllegalArgumentException("Email y contraseña son obligatorios")
        }

        if (userRepository.findByEmail(input.email!!).isPresent) {
            throw IllegalArgumentException("Ya existe un usuario con ese email")
        }

        val encoder = BCryptPasswordEncoder()
        val passwordHash = encoder.encode(input.password)

        // ✅ Determinar rol según el role_id enviado desde el frontend
        val roleId = input.roleId ?: 3L // Por defecto: 3 = STUDENT
        println("🔍 roleId extraído del input: ${input.roleId}")
        println("🔍 roleId después del elvis operator: $roleId")

        val selectedRole = roleRepository.findById(roleId)
            .orElseThrow { NoSuchElementException("El rol con id $roleId no existe en la base de datos") }

        println("🔍 Role encontrado en BD: id=${selectedRole.id}, name=${selectedRole.roleName}")

        val user = User(
            fullName = input.fullName ?: "Sin nombre",
            email = input.email!!,
            passwordHash = passwordHash,
            role = selectedRole,
            cardVerified = true
        )

        println("🔍 User antes de guardar: role.id=${user.role?.id}, role.name=${user.role?.roleName}")

        val saved = userRepository.save(user)

        println("🔍 User después de guardar: id=${saved.id}, role.id=${saved.role?.id}, role.name=${saved.role?.roleName}")
        println("========================================")

        return userMapper.userToUserResult(saved)
    }

}


// ==================== COURSE SERVICE ====================
@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository,
    @Qualifier("courseMapperImpl")
    private val mapper: CourseMapper
) {
    fun findAll(): List<CourseResult> =
        mapper.toResultList(courseRepository.findAll())

    fun findById(id: Long): CourseResult =
        mapper.toResult(courseRepository.findById(id).orElseThrow { NoSuchElementException("Course not found: $id") })

    @Transactional
    fun create(input: CourseInput): CourseResult {
        val professor = userRepository.findById(input.professorId)
            .orElseThrow { NoSuchElementException("User (professor) not found: ${input.professorId}") }

        val entity = Course(
            professor = professor,
            courseName = input.courseName,
            courseCode = input.courseCode,
            joinCode = generarCodigoUnico(),
            courseColor = input.courseColor,
            independentStudyHours = input.independentStudyHours
        )
        return mapper.toResult(courseRepository.save(entity))
    }

    /** Devuelve el código del curso; si no tiene (cursos viejos), lo genera y guarda. */
    @Transactional
    fun getOrCreateJoinCode(courseId: Long): String {
        val course = courseRepository.findById(courseId)
            .orElseThrow { NoSuchElementException("Course not found: $courseId") }
        course.joinCode?.let { return it }
        val code = generarCodigoUnico()
        courseRepository.save(course.copy(joinCode = code))
        return code
    }

    /** Genera un código corto y único (sin caracteres ambiguos). */
    private fun generarCodigoUnico(): String {
        val alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(30) {
            val code = (1..6).map { alfabeto.random() }.joinToString("")
            if (courseRepository.findByJoinCode(code) == null) return code
        }
        return "C" + System.nanoTime().toString(36).takeLast(6).uppercase()
    }

    @Transactional
    fun update(id: Long, input: CourseInput): CourseResult {
        val existing = courseRepository.findById(id)
            .orElseThrow { NoSuchElementException("Course not found: $id") }
        val professor = userRepository.findById(input.professorId)
            .orElseThrow { NoSuchElementException("User (professor) not found: ${input.professorId}") }

        val updated = existing.copy(
            professor = professor,
            courseName = input.courseName,
            courseCode = input.courseCode,
            courseColor = input.courseColor,
            independentStudyHours = input.independentStudyHours
        )
        return mapper.toResult(courseRepository.save(updated))
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!courseRepository.existsById(id)) throw NoSuchElementException("Course not found: $id")
        courseRepository.deleteById(id)
    }
    @Transactional
    fun findCoursesByStudentId(studentId: Long): List<CourseResult> {
        val enrollments = enrollmentRepository.findByStudentId(studentId)

        return enrollments.mapNotNull { enrollment ->
            enrollment.course?.let { course ->
                mapper.toResult(course)
            }
        }
    }


}


// ==================== COURSE RUBRIC SERVICE ====================
@Service
class CourseRubricService(
    private val courseRubricRepository: CourseRubricRepository,
    private val courseRepository: CourseRepository,
    @Qualifier("courseRubricMapperImpl")
    private val mapper: CourseRubricMapper
) {
    fun findAll(): List<CourseRubricResult> =
        mapper.toResultList(courseRubricRepository.findAll())

    fun findById(id: Long): CourseRubricResult =
        mapper.toResult(courseRubricRepository.findById(id).orElseThrow { NoSuchElementException("CourseRubric not found: $id") })

    fun findByCourse(courseId: Long): List<CourseRubricResult> =
        mapper.toResultList(courseRubricRepository.findByCourseId(courseId))

    @Transactional
    fun create(input: CourseRubricInput): CourseRubricResult {
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }

        val entity = CourseRubric(
            course = course,
            rubricName = input.rubricName,
            weightPercentage = input.weightPercentage,
            dueDate = input.dueDate
        )
        return mapper.toResult(courseRubricRepository.save(entity))
    }

    @Transactional
    fun update(id: Long, input: CourseRubricInput): CourseRubricResult {
        val existing = courseRubricRepository.findById(id)
            .orElseThrow { NoSuchElementException("CourseRubric not found: $id") }
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }

        val updated = existing.copy(
            course = course,
            rubricName = input.rubricName,
            weightPercentage = input.weightPercentage,
            dueDate = input.dueDate
        )
        return mapper.toResult(courseRubricRepository.save(updated))
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!courseRubricRepository.existsById(id)) throw NoSuchElementException("CourseRubric not found: $id")
        courseRubricRepository.deleteById(id)
    }
}


// ==================== STUDENT RUBRIC GRADE SERVICE ====================
@Service
class StudentRubricGradeService(
    private val gradeRepository: StudentRubricGradeRepository,
    private val userRepository: UserRepository,
    private val courseRubricRepository: CourseRubricRepository
) {
    private fun toResult(e: StudentRubricGrade) = StudentRubricGradeResult(
        id = e.id,
        studentId = e.student.id!!,
        rubricId = e.rubric.id!!,
        grade = e.grade
    )

    fun findByStudent(studentId: Long): List<StudentRubricGradeResult> =
        gradeRepository.findByStudentId(studentId).map(::toResult)

    /** Crea o actualiza la nota del estudiante en esa rúbrica (una sola por par). */
    @Transactional
    fun upsert(input: StudentRubricGradeInput): StudentRubricGradeResult {
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("User not found: ${input.studentId}") }
        val rubric = courseRubricRepository.findById(input.rubricId)
            .orElseThrow { NoSuchElementException("CourseRubric not found: ${input.rubricId}") }

        val existing = gradeRepository.findByStudentIdAndRubricId(input.studentId, input.rubricId)
        val entity = existing?.copy(grade = input.grade, updatedAt = java.time.LocalDateTime.now())
            ?: StudentRubricGrade(student = student, rubric = rubric, grade = input.grade)
        return toResult(gradeRepository.save(entity))
    }

    @Transactional
    fun delete(studentId: Long, rubricId: Long) {
        gradeRepository.findByStudentIdAndRubricId(studentId, rubricId)?.let {
            gradeRepository.deleteById(it.id!!)
        }
    }
}


// ==================== NOTE (Apuntes) SERVICE ====================
@Service
class NoteService(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository
) {
    private fun toResult(n: Note) = NoteResult(
        id = n.id,
        studentId = n.student.id!!,
        courseId = n.course?.id,
        title = n.title,
        content = n.content,
        pinned = n.pinned,
        color = n.color,
        tags = n.tags,
        checklist = n.checklist,
        createdAt = n.createdAt
    )

    fun findByStudent(studentId: Long): List<NoteResult> =
        noteRepository.findByStudentIdOrderByCreatedAtDesc(studentId).map(::toResult)

    fun findByStudentAndCourse(studentId: Long, courseId: Long): List<NoteResult> =
        noteRepository.findByStudentIdAndCourseIdOrderByCreatedAtDesc(studentId, courseId).map(::toResult)

    fun findById(id: Long): NoteResult =
        toResult(noteRepository.findById(id).orElseThrow { NoSuchElementException("Note not found: $id") })

    @Transactional
    fun create(input: NoteInput): NoteResult {
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("User not found: ${input.studentId}") }
        // Curso opcional: si no viene courseId, es un apunte personal.
        val course = input.courseId?.let {
            courseRepository.findById(it).orElseThrow { NoSuchElementException("Course not found: $it") }
        }
        val note = Note(
            student = student, course = course, title = input.title, content = input.content,
            pinned = input.pinned, color = input.color, tags = input.tags, checklist = input.checklist
        )
        return toResult(noteRepository.save(note))
    }

    @Transactional
    fun update(id: Long, input: NoteInput): NoteResult {
        val existing = noteRepository.findById(id)
            .orElseThrow { NoSuchElementException("Note not found: $id") }
        // Curso opcional: si no viene courseId, queda como apunte personal.
        val course = input.courseId?.let {
            courseRepository.findById(it).orElseThrow { NoSuchElementException("Course not found: $it") }
        }
        val updated = existing.copy(
            course = course, title = input.title, content = input.content,
            pinned = input.pinned, color = input.color, tags = input.tags, checklist = input.checklist
        )
        return toResult(noteRepository.save(updated))
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!noteRepository.existsById(id)) throw NoSuchElementException("Note not found: $id")
        noteRepository.deleteById(id)
    }
}


// ==================== ENROLLMENT SERVICE ====================
@Service
class EnrollmentService(
    private val enrollmentRepository: EnrollmentRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    @Qualifier("enrollmentMapperImpl")
    private val mapper: EnrollmentMapper
) {
    fun findAll(): List<EnrollmentResult> =
        mapper.toResultList(enrollmentRepository.findAll())

    fun findById(id: Long): EnrollmentResult =
        mapper.toResult(enrollmentRepository.findById(id).orElseThrow { NoSuchElementException("Enrollment not found: $id") })

    @Transactional
    fun create(input: EnrollmentInput): EnrollmentResult {
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("User (student) not found: ${input.studentId}") }

        val entity = Enrollment(course = course, student = student)
        return mapper.toResult(enrollmentRepository.save(entity))
    }

    /** Un estudiante se une a un curso usando el código del grupo. */
    @Transactional
    fun joinByCode(input: JoinByCodeInput): EnrollmentResult {
        val code = input.code.trim().uppercase()
        val course = courseRepository.findByJoinCode(code)
            ?: throw NoSuchElementException("No existe un curso con el código '$code'.")
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("User (student) not found: ${input.studentId}") }

        // Si ya está matriculado, devolvemos esa matrícula (no duplicamos).
        val existente = enrollmentRepository.findByCourseId(course.id!!)
            .firstOrNull { it.student.id == student.id }
        if (existente != null) return mapper.toResult(existente)

        return mapper.toResult(enrollmentRepository.save(Enrollment(course = course, student = student)))
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!enrollmentRepository.existsById(id)) throw NoSuchElementException("Enrollment not found: $id")
        enrollmentRepository.deleteById(id)
    }
    fun findEnrollmentsByStudentId(studentId: Long): List<EnrollmentResult> {
        val enrollments = enrollmentRepository.findByStudentId(studentId)
        return enrollments.map { e ->
            EnrollmentResult(
                id = e.id ?: 0L,
                courseId = e.course?.id ?: 0L,
                studentId = e.student.id ?: 0L,
                enrolledAt = e.enrolledAt
            )

        }
    }

}


// ==================== CLASS (ClassEntity) SERVICE ====================
@Service
class ClassService(
    private val classRepository: ClassRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    @Qualifier("classMapperImpl")
    private val mapper: ClassMapper
) {
    fun findAll(): List<ClassResult> =
        mapper.toResultList(classRepository.findAll())

    fun findById(id: Long): ClassResult =
        mapper.toResult(classRepository.findById(id).orElseThrow { NoSuchElementException("Class not found: $id") })


    // 🔹 NUEVO MÉTODO: obtener todas las clases de un profesor (vía sus cursos)
    fun findAllByProfessorId(professorId: Long): List<ClassResult> {
        // Obtener todos los cursos que imparte el profesor
        val courses = courseRepository.findAll().filter { it.professor.id == professorId }

        // Obtener todas las clases que pertenecen a esos cursos
        val allClasses = classRepository.findAll()
            .filter { clazz -> courses.any { it.id == clazz.course.id } }

        return mapper.toResultList(allClasses)
    }

    @Transactional
    fun create(input: ClassInput): ClassResult {
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }
        val professor = userRepository.findById(input.professorId)
            .orElseThrow { NoSuchElementException("User (professor) not found: ${input.professorId}") }

        val entity = ClassEntity(
            course = course,
            professor = professor,
            classDate = input.classDate,
            startTime = input.startTime,
            endTime = input.endTime,
            modality = input.modality,
            location = input.location
        )
        return mapper.toResult(classRepository.save(entity))
    }

    @Transactional
    fun update(id: Long, input: ClassInput): ClassResult {
        val existing = classRepository.findById(id)
            .orElseThrow { NoSuchElementException("Class not found: $id") }
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }
        val professor = userRepository.findById(input.professorId)
            .orElseThrow { NoSuchElementException("User (professor) not found: ${input.professorId}") }

        val updated = existing.copy(
            course = course,
            professor = professor,
            classDate = input.classDate,
            startTime = input.startTime,
            endTime = input.endTime,
            modality = input.modality,
            location = input.location
        )
        return mapper.toResult(classRepository.save(updated))
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!classRepository.existsById(id)) throw NoSuchElementException("Class not found: $id")
        classRepository.deleteById(id)
    }
    @Transactional
    fun updateModality(id: Long, modality: String): ClassResult {
        val entity = classRepository.findById(id)
            .orElseThrow { NoSuchElementException("Clase no encontrada con ID: $id") }

        entity.modality = modality
        return mapper.toResult(classRepository.save(entity))
    }


}
// ==================== STUDENT CLASS PREFERENCE SERVICE ====================
@Service
class StudentClassPreferenceService(
    private val preferenceRepository: StudentClassPreferenceRepository,
    private val classRepository: ClassRepository,
    private val userRepository: UserRepository,
    private val mapper: StudentClassPreferenceMapper
) {
    fun findAll(): List<StudentClassPreferenceResult> =
        mapper.toResultList(preferenceRepository.findAll())

    fun findById(id: Long): StudentClassPreferenceResult =
        mapper.toResult(preferenceRepository.findById(id)
            .orElseThrow { NoSuchElementException("StudentClassPreference not found: $id") })

    @Transactional
    fun create(input: StudentClassPreferenceInput): StudentClassPreferenceResult {
        val clazz = classRepository.findById(input.classId)
            .orElseThrow { NoSuchElementException("Class not found: ${input.classId}") }
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("Student not found: ${input.studentId}") }

        val entity = StudentClassPreference(
            clazz = clazz,
            student = student,
            customModality = input.customModality,
            updatedAt = LocalDateTime.now()
        )
        val saved = preferenceRepository.save(entity)
        return mapper.toResult(saved)
    }

    @Transactional
    fun update(id: Long, input: StudentClassPreferenceInput): StudentClassPreferenceResult {
        val existing = preferenceRepository.findById(id)
            .orElseThrow { NoSuchElementException("StudentClassPreference not found: $id") }
        val clazz = classRepository.findById(input.classId)
            .orElseThrow { NoSuchElementException("Class not found: ${input.classId}") }
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("Student not found: ${input.studentId}") }

        val updated = existing.copy(
            clazz = clazz,
            student = student,
            customModality = input.customModality,
            updatedAt = LocalDateTime.now()
        )
        val saved = preferenceRepository.save(updated)
        return mapper.toResult(saved)
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!preferenceRepository.existsById(id)) throw NoSuchElementException("StudentClassPreference not found: $id")
        preferenceRepository.deleteById(id)
    }
}

// ==================== ASSIGNMENT SERVICE ====================
@Service
class AssignmentService(
    private val assignmentRepository: AssignmentRepository,
    private val courseRepository: CourseRepository,
    private val rubricRepository: CourseRubricRepository,
    private val userRepository: UserRepository,
    private val mapper: AssignmentMapper
) {
    fun findAll(): List<AssignmentResult> =
        mapper.toResultList(assignmentRepository.findAll())

    fun findById(id: Long): AssignmentResult =
        mapper.toResult(assignmentRepository.findById(id).orElseThrow { NoSuchElementException("Assignment not found: $id") })

    @Transactional
    fun create(input: AssignmentInput): AssignmentResult {
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }
        val rubric = input.rubricId?.let {
            rubricRepository.findById(it).orElseThrow { NoSuchElementException("Rubric not found: $it") }
        }
        val creator = userRepository.findById(input.createdBy)
            .orElseThrow { NoSuchElementException("User (creator) not found: ${input.createdBy}") }

        val entity = Assignment(
            course = course,
            rubric = rubric,
            title = input.title,
            description = input.description,
            type = input.type,
            dueDate = input.dueDate,
            createdBy = creator,
            createdAt = LocalDateTime.now()
        )
        val saved = assignmentRepository.save(entity)
        return mapper.toResult(saved)
    }

    @Transactional
    fun update(id: Long, input: AssignmentInput): AssignmentResult {
        val existing = assignmentRepository.findById(id)
            .orElseThrow { NoSuchElementException("Assignment not found: $id") }
        val course = courseRepository.findById(input.courseId)
            .orElseThrow { NoSuchElementException("Course not found: ${input.courseId}") }
        val rubric = input.rubricId?.let {
            rubricRepository.findById(it).orElseThrow { NoSuchElementException("Rubric not found: $it") }
        }
        val creator = userRepository.findById(input.createdBy)
            .orElseThrow { NoSuchElementException("User (creator) not found: ${input.createdBy}") }

        val updated = existing.copy(
            course = course,
            rubric = rubric,
            title = input.title,
            description = input.description,
            type = input.type,
            dueDate = input.dueDate,
            createdBy = creator
        )
        val saved = assignmentRepository.save(updated)
        return mapper.toResult(saved)
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!assignmentRepository.existsById(id)) throw NoSuchElementException("Assignment not found: $id")
        assignmentRepository.deleteById(id)
    }
}

// ==================== STUDENT ASSIGNMENT SERVICE ====================
@Service
class StudentAssignmentService(
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val assignmentRepository: AssignmentRepository,
    private val userRepository: UserRepository,
    private val mapper: StudentAssignmentMapper,
    private val studyPlanService: StudyPlanService
) {
    @Value("\${kode.ai.auto-generate-studyplan:true}")
    private val autoGeneratePlans: Boolean = true

    fun findAll(): List<StudentAssignmentResult> =
        mapper.toResultList(studentAssignmentRepository.findAll())

    fun findById(id: Long): StudentAssignmentResult =
        mapper.toResult(studentAssignmentRepository.findById(id).orElseThrow { NoSuchElementException("StudentAssignment not found: $id") })

    @Transactional
    fun create(input: StudentAssignmentInput): StudentAssignmentResult {
        val assignment = assignmentRepository.findById(input.assignmentId)
            .orElseThrow { NoSuchElementException("Assignment not found: ${input.assignmentId}") }
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("Student not found: ${input.studentId}") }

        val entity = StudentAssignment(
            assignment = assignment,
            student = student,
            status = input.status,
            grade = input.grade,
            submittedAt = input.submittedAt
        )
        val saved = studentAssignmentRepository.save(entity)

        try {
            studyPlanService.generarPlanIA(
                GenerarPlanRequest(
                    studentId = student.id!!,
                    assignmentId = assignment.id!!,
                    horasDisponiblesPorDia = 2,
                    diasAntesDueDate = 7
                )
            )
        } catch (e: Exception) {
            println("Error al generar plan de estudio con IA: ${e.message}")
        }

        return mapper.toResult(saved)
    }

    @Transactional
    fun update(id: Long, input: StudentAssignmentInput): StudentAssignmentResult {
        val existing = studentAssignmentRepository.findById(id)
            .orElseThrow { NoSuchElementException("StudentAssignment not found: $id") }
        val assignment = assignmentRepository.findById(input.assignmentId)
            .orElseThrow { NoSuchElementException("Assignment not found: ${input.assignmentId}") }
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("Student not found: ${input.studentId}") }

        val updated = existing.copy(
            assignment = assignment,
            student = student,
            status = input.status,
            grade = input.grade,
            submittedAt = input.submittedAt
        )
        val saved = studentAssignmentRepository.save(updated)
        return mapper.toResult(saved)
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!studentAssignmentRepository.existsById(id)) throw NoSuchElementException("StudentAssignment not found: $id")
        studentAssignmentRepository.deleteById(id)
    }
}
// ==================== STUDY PLAN SERVICE ====================
@Service
class StudyPlanService(
    private val studyPlanRepository: StudyPlanRepository,
    private val userRepository: UserRepository,
    private val assignmentRepository: AssignmentRepository,
    private val mapper: StudyPlanMapper,
    private val iaService: SmartPlannerStrategy,
    // Cadena de proveedores de IA gratis (Gemini -> Groq -> Mistral) con respaldo.
    private val aiRouter: AIRouter
) {

    fun findAll(): List<StudyPlanResult> =
        mapper.toResultList(studyPlanRepository.findAll())

    fun findById(id: Long): StudyPlanResult =
        mapper.toResult(studyPlanRepository.findById(id).orElseThrow {
            NoSuchElementException("StudyPlan not found: $id")
        })

    @Transactional
    fun create(input: StudyPlanInput): StudyPlanResult {
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("Student not found: ${input.studentId}") }
        val assignment = assignmentRepository.findById(input.assignmentId)
            .orElseThrow { NoSuchElementException("Assignment not found: ${input.assignmentId}") }

        val entity = StudyPlan(
            student = student,
            assignment = assignment,
            plannedDate = input.plannedDate,
            startTime = input.startTime,
            endTime = input.endTime,
            status = input.status,
            generatedByAi = input.generatedByAi ?: true
        )
        val saved = studyPlanRepository.save(entity)

        // NUEVA FUNCIONALIDAD: Replanificar después de crear
        iaService.replanificarAlAgregarTarea(student.id!!, assignment.id!!)

        return mapper.toResult(saved)
    }

    @Transactional
    fun update(id: Long, input: StudyPlanInput): StudyPlanResult {
        val existing = studyPlanRepository.findById(id)
            .orElseThrow { NoSuchElementException("StudyPlan not found: $id") }
        val student = userRepository.findById(input.studentId)
            .orElseThrow { NoSuchElementException("Student not found: ${input.studentId}") }
        val assignment = assignmentRepository.findById(input.assignmentId)
            .orElseThrow { NoSuchElementException("Assignment not found: ${input.assignmentId}") }

        val updated = existing.copy(
            student = student,
            assignment = assignment,
            plannedDate = input.plannedDate,
            startTime = input.startTime,
            endTime = input.endTime,
            status = input.status,
            generatedByAi = input.generatedByAi ?: existing.generatedByAi
        )
        val saved = studyPlanRepository.save(updated)
        return mapper.toResult(saved)
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!studyPlanRepository.existsById(id))
            throw NoSuchElementException("StudyPlan not found: $id")
        studyPlanRepository.deleteById(id)
    }

    // ===================== PLAN CON IA (+ RESPALDO LOCAL) =====================
    @Transactional
    fun generarPlanIA(request: GenerarPlanRequest): List<StudyPlanResult> {
        // 1) Intentar con IA (proveedor gratis, con cadena de respaldo).
        runCatching {
            val plan = generarPlanConIA(request)
            if (plan.isNotEmpty()) {
                println("✅ Plan generado con IA (${plan.size} bloques).")
                return plan
            }
        }.onFailure {
            println("⚠️ IA no pudo generar el plan (${it.message}); uso el planificador local.")
        }
        // 2) Respaldo local determinista (siempre funciona, gratis).
        return iaService.generarPlanEstudio(
            studentId = request.studentId,
            assignmentId = request.assignmentId,
            horasDisponiblesPorDia = request.horasDisponiblesPorDia,
            diasAntesDueDate = request.diasAntesDueDate
        )
    }

    /** Le pide a la IA un plan en JSON y lo convierte a List<StudyPlanResult>. */
    private fun generarPlanConIA(request: GenerarPlanRequest): List<StudyPlanResult> {
        // El modo integral (assignmentId = -1) no tiene una sola tarea de contexto.
        if (request.assignmentId <= 0) return emptyList()
        val assignment = assignmentRepository.findById(request.assignmentId).orElse(null)
            ?: return emptyList()

        val hoy = LocalDate.now()
        val entrega = assignment.dueDate?.toString() ?: "sin fecha"
        val titulo = assignment.title ?: "la tarea"
        val prompt = """
            Sos un planificador de estudio. Devolvé SOLO un arreglo JSON (sin texto extra ni markdown)
            con los bloques para preparar "$titulo".
            Contexto: hoy es $hoy; la entrega es $entrega; el estudiante dispone de
            ${request.horasDisponiblesPorDia} horas por día y quiere empezar
            ${request.diasAntesDueDate} días antes de la entrega.
            Cada elemento debe tener EXACTAMENTE estas claves:
            {"plannedDate":"YYYY-MM-DD","startTime":"HH:MM","endTime":"HH:MM","status":"pendiente"}
            Respondé empezando con [ y terminando con ]. Nada más.
        """.trimIndent()

        val raw = aiRouter.generarTexto(prompt)
        val ini = raw.indexOf('[')
        val fin = raw.lastIndexOf(']')
        if (ini < 0 || fin <= ini) return emptyList()
        val arr = org.json.JSONArray(raw.substring(ini, fin + 1))

        val resultado = mutableListOf<StudyPlanResult>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            resultado.add(
                StudyPlanResult(
                    id = null,
                    studentId = request.studentId,
                    assignmentId = request.assignmentId,
                    plannedDate = runCatching { LocalDate.parse(o.getString("plannedDate")) }.getOrNull(),
                    startTime = java.time.LocalTime.parse(o.getString("startTime")),
                    endTime = java.time.LocalTime.parse(o.getString("endTime")),
                    status = o.optString("status", "pendiente"),
                    generatedByAi = true
                )
            )
        }
        return resultado
    }

    // ===================== NUEVO: PLAN INTEGRAL =====================
    @Transactional
    fun generarPlanIntegral(studentId: Long, dias: Int = 14): List<StudyPlanResult> {
        return iaService.generarPlanEstudio(
            studentId = studentId,
            assignmentId = -1, // Indica modo integral
            horasDisponiblesPorDia = 4,
            diasAntesDueDate = dias
        )
    }
}


// ==================== ALERT SERVICE ====================
@Service
class AlertService(
    private val repo: AlertRepository,
    private val userRepository: UserRepository,
    private val mapper: AlertMapper
) {

    fun findAll(): List<AlertResult> = mapper.toResultList(repo.findAll())

    fun findById(id: Long): AlertResult =
        mapper.toResult(repo.findById(id).orElseThrow { NoSuchElementException("Alert $id not found!") })

    @Transactional
    fun create(input: AlertInput): AlertResult {
        val professor = userRepository.findById(input.professorId)
            .orElseThrow { NoSuchElementException("Professor with id ${input.professorId} not found!") }

        val entity = Alert(
            title = input.title,
            message = input.message,
            severity = input.severity,
            professor = professor
        )
        return mapper.toResult(repo.save(entity))
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!repo.existsById(id))
            throw NoSuchElementException("Alert $id not found!")
        repo.deleteById(id)
    }

    fun getAlertOfDay(): AlertResult? =
        repo.findTopByOrderByCreatedAtDesc()?.let { mapper.toResult(it) }


    // 🔹 NUEVO MÉTODO: obtener alertas del profesor según la fecha actual
    fun findAllByProfessorToday(professorId: Long): List<AlertResult> {
        val startOfDay = LocalDate.now().atStartOfDay()
        val endOfDay = startOfDay.plusDays(1).minusNanos(1)

        val alerts = repo.findByProfessorIdAndCreatedAtBetween(professorId, startOfDay, endOfDay)
        return mapper.toResultList(alerts)
    }



}


