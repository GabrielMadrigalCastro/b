package kode.backend.backendkode

import org.mapstruct.*




@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PriorityMapper {
    fun toEntity(input: PriorityInput): Priority
    fun toDetails(priority: Priority): PriorityResult
    fun toDetailsList(priorities: List<Priority>): List<PriorityResult>
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface StatusMapper {
    fun toEntity(input: StatusInput): Status
    fun toDetails(status: Status): StatusResult
    fun toDetailsList(statuses: List<Status>): List<StatusResult>
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
abstract class ReminderMapper {

    // Manual mapping for entity to DTO
    fun toDetails(reminder: Reminder): ReminderDetails {
        return ReminderDetails(
            taskId = reminder.id.taskId,
            reminderDate = reminder.id.reminderDate
        )
    }

    fun toDetailsList(reminders: List<Reminder>): List<ReminderDetails> {
        return reminders.map { toDetails(it) }
    }
}

    @Mapper(
        componentModel = "spring",
        uses = [PriorityMapper::class, StatusMapper::class, ReminderMapper::class],
        unmappedTargetPolicy = ReportingPolicy.IGNORE
    )

    abstract class TaskMapper {

        abstract fun toResult(task: Task): TaskResult
        abstract fun toResultList(tasks: List<Task>): List<TaskResult>

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "createDate", expression = "java(java.time.LocalDate.now())")
        @Mapping(target = "user", ignore = true)
        @Mapping(target = "priority", ignore = true)
        @Mapping(target = "status", ignore = true)
        @Mapping(target = "reminders", expression = "java(new java.util.HashSet())")
        abstract fun toEntity(input: TaskInput): Task

        @AfterMapping
        protected fun fillRelations(task: Task, @MappingTarget dto: TaskResult) {
            dto.priority = task.priority?.let { PriorityResult(id = it.id!!, label = it.label) }
            dto.status = task.status?.let { StatusResult(id = it.id!!, label = it.label) }
            dto.reminders =
                task.reminders.map { ReminderDetails(taskId = it.id.taskId, reminderDate = it.id.reminderDate) }

        }
    }

    @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
    interface TopicMapper {
        @Mapping(source = "course.id", target = "courseId")
        fun toResult(entity: Topic): TopicResult
        fun toResultList(entities: List<Topic>): List<TopicResult>
    }

    @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
    interface QuestionOptionMapper {
        @Mapping(source = "question.id", target = "questionId")
        fun toResult(entity: QuestionOption): QuestionOptionResult
        fun toResultList(entities: List<QuestionOption>): List<QuestionOptionResult>
    }


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface PrivilegeMapper {
    fun privilegeToPrivilegeDetails(privilege: Privilege): PrivilegeDetails

    fun privilegeListToPrivilegeDetailsList(privileges: List<Privilege>): List<PrivilegeDetails>
}

@Mapper(
    componentModel = "spring",
    uses = [PrivilegeMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
abstract class RoleMapper {

    @Mapping(source = "roleName", target = "name")
    abstract fun roleToRoleDetails(role: Role): RoleDetails

    abstract fun roleListToRoleDetailsList(roles: List<Role>): List<RoleDetails>

    @AfterMapping
    protected fun fillPrivileges(role: Role, @MappingTarget dto: RoleDetails) {
        dto.privileges = role.rolePrivileges.map { rp ->
            PrivilegeDetails(
                id = rp.privilege.id,
                name = rp.privilege.name
            )
        }
    }
}


@Mapper(
    componentModel = "spring",
    uses = [RoleMapper::class],
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
abstract class UserMapper {

    // ====== ENTITY → DTO ======
    abstract fun userToUserResult(user: User): UserResult
    abstract fun userListToUserResultList(users: List<User>): List<UserResult>

    // ====== DTO → ENTITY ======
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "passwordHash", expression = "java(\"TEMP_HASH\")")
    @Mapping(target = "role", ignore = true)  // 👈 Se manejará en el service
    @Mapping(target = "userRoles", expression = "java(new java.util.HashSet())")
    @Mapping(target = "coursesTaught", expression = "java(new java.util.HashSet())")
    @Mapping(target = "enrollments", expression = "java(new java.util.HashSet())")
    @Mapping(target = "studentAssignments", expression = "java(new java.util.HashSet())")
    @Mapping(target = "studyPlans", expression = "java(new java.util.HashSet())")
    @Mapping(target = "tasks", expression = "java(new java.util.HashSet())")
    abstract fun userInputToUser(userInput: UserInput): User

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "role", ignore = true)
    abstract fun updateUserFromInput(userInput: UserInput, @MappingTarget user: User)

    // 👈 NUEVO: Mapea el role con sus privilegios
    @AfterMapping
    protected fun fillRole(user: User, @MappingTarget dto: UserResult) {
        user.role?.let { role ->
            dto.role = RoleDetails(
                id = role.id,
                name = role.roleName,
                privileges = role.rolePrivileges.map { rp ->
                    PrivilegeDetails(
                        id = rp.privilege.id,
                        name = rp.privilege.name
                    )
                }
            )
        }
    }
}

// ===== COURSE MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface CourseMapper {
    @Mappings(
        Mapping(source = "professor.id", target = "professorId")
    )
    fun toResult(entity: Course): CourseResult

    fun toResultList(entities: List<Course>): List<CourseResult>
}


// ===== COURSE RUBRIC MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface CourseRubricMapper {
    @Mappings(
        Mapping(source = "course.id", target = "courseId")
    )
    fun toResult(entity: CourseRubric): CourseRubricResult

    fun toResultList(entities: List<CourseRubric>): List<CourseRubricResult>
}


// ===== ENROLLMENT MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface EnrollmentMapper {
    @Mappings(
        Mapping(source = "course.id", target = "courseId"),
        Mapping(source = "student.id", target = "studentId")
    )
    fun toResult(entity: Enrollment): EnrollmentResult

    fun toResultList(entities: List<Enrollment>): List<EnrollmentResult>
}


// ===== CLASS (ClassEntity) MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ClassMapper {
    @Mappings(
        Mapping(source = "course.id", target = "courseId"),
        Mapping(source = "professor.id", target = "professorId")
    )
    fun toResult(entity: ClassEntity): ClassResult

    fun toResultList(entities: List<ClassEntity>): List<ClassResult>
}





// ===== STUDENT CLASS PREFERENCE MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface StudentClassPreferenceMapper {
    @Mappings(
        Mapping(source = "clazz.id", target = "classId"),
        Mapping(source = "student.id", target = "studentId")
    )
    fun toResult(entity: StudentClassPreference): StudentClassPreferenceResult

    fun toResultList(entities: List<StudentClassPreference>): List<StudentClassPreferenceResult>
}


// ===== ASSIGNMENT MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface AssignmentMapper {

    @Mappings(
        Mapping(target = "courseId", source = "course.id"),
        Mapping(target = "rubricId", source = "rubric.id"),
        Mapping(target = "createdBy", source = "createdBy.id")  // CORRECCIÓN
    )
    fun toResult(entity: Assignment): AssignmentResult

    fun toResultList(entities: List<Assignment>): List<AssignmentResult>
}



// ===== STUDENT ASSIGNMENT MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface StudentAssignmentMapper {
    @Mappings(
        Mapping(source = "assignment.id", target = "assignmentId"),
        Mapping(source = "student.id", target = "studentId")
    )
    fun toResult(entity: StudentAssignment): StudentAssignmentResult

    fun toResultList(entities: List<StudentAssignment>): List<StudentAssignmentResult>
}


// ===== STUDY PLAN MAPPER =====
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface StudyPlanMapper {
    @Mappings(
        Mapping(source = "student.id", target = "studentId"),
        Mapping(source = "assignment.id", target = "assignmentId")
    )
    fun toResult(entity: StudyPlan): StudyPlanResult

    fun toResultList(entities: List<StudyPlan>): List<StudyPlanResult>
}



//=============  ALERTS  ================

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface AlertMapper {
    @Mapping(source = "professor.id", target = "professorId")
    fun toResult(entity: Alert): AlertResult
    fun toResultList(entities: List<Alert>): List<AlertResult>
}
