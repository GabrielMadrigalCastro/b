package kode.backend.backendkode

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ===================== ROLES & USERS =====================
@Entity
@Table(name = "roles")
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    val id: Long? = null,

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    val roleName: String,

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    val userRoles: MutableSet<UserRole> = HashSet(),

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    val rolePrivileges: MutableSet<RolePrivilege> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Role) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Role(id=$id, roleName='$roleName')"
}

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    val role: Role? = null,

    @Column(name = "full_name", nullable = false, length = 150)
    val fullName: String,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String = "TEMP_HASH",

    @Column(name = "enrollment_card")
    val enrollmentCard: String? = null,

    @Column(name = "card_verified", nullable = false)
    val cardVerified: Boolean = false,

    @Column(name = "token_expired")
    var tokenExpired: Boolean? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    val userRoles: MutableSet<UserRole> = HashSet(),

    @OneToMany(mappedBy = "professor", fetch = FetchType.LAZY)
    val coursesTaught: MutableSet<Course> = HashSet(),

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    val enrollments: MutableSet<Enrollment> = HashSet(),

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    val studentAssignments: MutableSet<StudentAssignment> = HashSet(),

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    val studyPlans: MutableSet<StudyPlan> = HashSet(),

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val tasks: MutableSet<Task> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "User(id=$id, fullName='$fullName', email='$email', tokenExpired=$tokenExpired)"
}

// ===================== PRIVILEGES & ACCESS =====================
@Entity
@Table(name = "privilegies")
data class Privilege(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "privilege_id")
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val name: String,

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "privilege", fetch = FetchType.LAZY)
    val rolePrivileges: MutableSet<RolePrivilege> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Privilege) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Privilege(id=$id, name='$name')"
}

@Entity
@Table(name = "user_role")
data class UserRole(
    @EmbeddedId
    val id: UserRoleId = UserRoleId(),

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    val role: Role
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserRole) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Embeddable
data class UserRoleId(
    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "role_id")
    val roleId: Long? = null
) : java.io.Serializable

@Entity
@Table(name = "role_privilege")
data class RolePrivilege(
    @EmbeddedId
    val id: RolePrivilegeId = RolePrivilegeId(),

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    val role: Role,

    @MapsId("privilegeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id")
    val privilege: Privilege
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RolePrivilege) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Embeddable
data class RolePrivilegeId(
    @Column(name = "role_id")
    val roleId: Long? = null,

    @Column(name = "privilege_id")
    val privilegeId: Long? = null
) : java.io.Serializable

// ===================== COURSES & RUBRICS =====================
@Entity
@Table(name = "courses")
data class Course(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    val professor: User,

    @Column(name = "course_name", nullable = false, length = 150)
    val courseName: String,

    @Column(name = "course_code", length = 50)
    val courseCode: String? = null,

    // Código único para que los estudiantes se unan al curso ("grupo").
    @Column(name = "join_code", length = 12, unique = true)
    val joinCode: String? = null,

    @Column(name = "course_color", length = 20)
    val courseColor: String? = null,

    @Column(name = "independent_study_hours")
    val independentStudyHours: Int? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true)
    val rubrics: MutableSet<CourseRubric> = HashSet(),

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true)
    val enrollments: MutableSet<Enrollment> = HashSet(),

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true)
    val classes: MutableSet<ClassEntity> = HashSet(),

    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], orphanRemoval = true)
    val assignments: MutableSet<Assignment> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Course) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Course(id=$id, courseName='$courseName', courseCode='$courseCode')"
}

@Entity
@Table(name = "courserubrics")
data class CourseRubric(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rubric_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @Column(name = "rubric_name", length = 100)
    val rubricName: String? = null,

    @Column(name = "weight_percentage", precision = 5, scale = 2)
    val weightPercentage: BigDecimal? = null,

    @Column(name = "due_date")
    val dueDate: LocalDate? = null,

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "rubric", fetch = FetchType.LAZY)
    val assignments: MutableSet<Assignment> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CourseRubric) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "CourseRubric(id=$id, rubricName='$rubricName')"
}

// ===================== ENROLLMENTS & CLASSES =====================
@Entity
@Table(name = "enrollments")
data class Enrollment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: User,

    @Column(name = "enrolled_at", nullable = false)
    val enrolledAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Enrollment) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

@Entity
@Table(name = "classes")
data class ClassEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    val professor: User,

    @Column(name = "class_date")
    val classDate: LocalDate? = null,

    @Column(name = "start_time")
    val startTime: LocalTime? = null,

    @Column(name = "end_time")
    val endTime: LocalTime? = null,

    @Column(length = 20)
    var modality: String? = null,

    val location: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "clazz", cascade = [CascadeType.ALL], orphanRemoval = true)
    val studentPreferences: MutableSet<StudentClassPreference> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

@Entity
@Table(name = "studentclasspreferences")
data class StudentClassPreference(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    val clazz: ClassEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: User,

    @Column(name = "custom_modality", length = 20)
    val customModality: String? = null,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StudentClassPreference) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

// ===================== ASSIGNMENTS & STUDENT ASSIGNMENTS =====================
@Entity
@Table(name = "assignments")
data class Assignment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = true)  // 👈 CAMBIADO a nullable = true
    val rubric: CourseRubric? = null,  // 👈 CAMBIADO a nullable

    @Column(length = 200)
    val title: String? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(length = 50)
    val type: String? = null,

    @Column(name = "due_date")
    val dueDate: LocalDate? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    val createdBy: User? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "assignment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val studentAssignments: MutableSet<StudentAssignment> = HashSet(),

    @OneToMany(mappedBy = "assignment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val studyPlans: MutableSet<StudyPlan> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Assignment) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

@Entity
@Table(name = "studentassignments")
data class StudentAssignment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_assignment_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    val assignment: Assignment,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: User,

    @Column(length = 20)
    val status: String? = null,

    @Column(precision = 5, scale = 2)
    val grade: BigDecimal? = null,

    @Column(name = "submitted_at")
    val submittedAt: LocalDateTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StudentAssignment) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

// ===================== STUDENT RUBRIC GRADES =====================
// Nota que un estudiante obtuvo en una rúbrica de un curso (para la calculadora).
@Entity
@Table(
    name = "student_rubric_grades",
    uniqueConstraints = [UniqueConstraint(columnNames = ["student_id", "rubric_id"])]
)
data class StudentRubricGrade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_rubric_grade_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    val rubric: CourseRubric,

    @Column(precision = 5, scale = 2)
    val grade: BigDecimal? = null,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StudentRubricGrade) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

// ===================== STUDY PLANS =====================
@Entity
@Table(name = "studyplans")
data class StudyPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    val assignment: Assignment,

    @Column(name = "planned_date")
    val plannedDate: LocalDate? = null,

    @Column(name = "start_time")
    val startTime: LocalTime? = null,

    @Column(name = "end_time")
    val endTime: LocalTime? = null,

    @Column(length = 20)
    val status: String? = null,

    @Column(name = "generated_by_ai", nullable = false)
    val generatedByAi: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StudyPlan) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
// ===================== TASKS, PRIORITY & STATUS =====================
@Entity
@Table(name = "priority")
data class Priority(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "priority_id")
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val label: String,

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "priority", fetch = FetchType.LAZY)
    val tasks: MutableSet<Task> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Priority) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Priority(id=$id, label='$label')"
}

@Entity
@Table(name = "status")
data class Status(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val label: String,

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "status", fetch = FetchType.LAZY)
    val tasks: MutableSet<Task> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Status) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Status(id=$id, label='$label')"
}

@Entity
@Table(name = "tasks")
data class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Column(name = "create_date", nullable = false)
    var createDate: LocalDate = LocalDate.now(),

    @Column(name = "due_date")
    var dueDate: LocalDate? = null,

    var notes: String? = null,

    var title: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id")
    var priority: Priority? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    var status: Status? = null,

    // Relaciones bidireccionales
    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reminders: MutableSet<Reminder> = HashSet()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Task) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Task(id=$id, title='$title', dueDate=$dueDate)"
}

@Entity
@Table(name = "reminders")
data class Reminder(
    @EmbeddedId
    val id: ReminderId = ReminderId(),

    @MapsId("taskId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    val task: Task
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Reminder) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Embeddable
data class ReminderId(
    @Column(name = "task_id")
    val taskId: Long? = null,

    @Column(name = "reminder_date")
    val reminderDate: LocalDateTime? = null
) : java.io.Serializable

// ===================== NOTES & TOPICS =====================
@Entity
@Table(name = "notes")
data class Note(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    val student: User,

    // Curso opcional: si es null, es un apunte personal (fuera de clases).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = true)
    val course: Course? = null,

    val title: String,
    @Column(columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false)
    val pinned: Boolean = false,

    @Column(length = 20)
    val color: String? = null,

    // Etiquetas separadas por coma (ej. "examen,teoría").
    @Column(length = 300)
    val tags: String? = null,

    // Checklist como JSON (ej. [{"t":"...","d":true}]).
    @Column(columnDefinition = "TEXT")
    val checklist: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany
    @JoinTable(
        name = "note_topic",
        joinColumns = [JoinColumn(name = "note_id")],
        inverseJoinColumns = [JoinColumn(name = "topic_id")]
    )
    val topics: MutableSet<Topic> = HashSet()
)

@Entity
@Table(name = "topics")
data class Topic(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    val course: Course,

    val name: String,
    val source: String? = null,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

// ===================== EXAMS =====================
@Entity
@Table(name = "exams")
data class Exam(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    val student: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    val course: Course,

    val level: Int? = null,
    @Column(name = "size_label")
    val sizeLabel: String? = null,
    @Column(name = "questions_count")
    val questionsCount: Int? = null,
    @Column(name = "generated_by_ai")
    val generatedByAi: Boolean = false,
    val status: String? = null,
    @Column(name = "started_at")
    val startedAt: LocalDateTime? = null,
    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany
    @JoinTable(
        name = "exam_topic",
        joinColumns = [JoinColumn(name = "exam_id")],
        inverseJoinColumns = [JoinColumn(name = "topic_id")]
    )
    val topics: MutableSet<Topic> = HashSet()
)

@Entity
@Table(name = "questions")
data class Question(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    val exam: Exam,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    val topic: Topic,

    @Column(name = "order_no")
    val orderNo: Int,
    @Column(columnDefinition = "TEXT")
    val text: String,
    val difficulty: String? = null,
    @Column(name = "correct_answer")
    val correctAnswer: String? = null
)

@Entity
@Table(name = "questionoptions")
data class QuestionOption(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    val question: Question,

    val label: String,
    val text: String
)
//      ****
@Entity
@Table(name = "responses")
data class Response(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    val question: Question,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    val student: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_option_id")
    val chosenOption: QuestionOption? = null,

    @Column(name = "is_correct")
    val isCorrect: Boolean? = null,
    @Column(name = "answered_at")
    val answeredAt: LocalDateTime? = null
)

@Entity
@Table(name = "emotionevents")
data class EmotionEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    val student: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    val exam: Exam,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    val question: Question,

    val context: String,
    @Column(name = "emotion_type")
    val emotionType: String,
    val intensity: Int,
    val message: String? = null,
    @Column(name = "recorded_at")
    val recordedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "examsummary")
data class ExamSummary(
    @Id
    @Column(name = "exam_id")
    val examId: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "exam_id")
    val exam: Exam,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    val student: User,

    @Column(name = "correct_count")
    val correctCount: Int,

    @Column(name = "wrong_count")
    val wrongCount: Int,

    val accuracy: BigDecimal,

    @Column(name = "passed_level")
    val passedLevel: Boolean,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)


@Entity
@Table(name = "studentcourseprogress")
data class StudentCourseProgress(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    val student: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    val course: Course,

    @Column(name = "current_level")
    val currentLevel: Int,
    @Column(name = "cumulative_correct")
    val cumulativeCorrect: Int,
    @Column(name = "cumulative_wrong")
    val cumulativeWrong: Int,
    @Column(name = "last_exam_id")
    val lastExamId: Long? = null,
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "recommendations")
data class Recommendation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    val course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    val topic: Topic,

    val text: String,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

// ===================== ALERTS =====================
@Entity
@Table(name = "alerts")
data class Alert(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    val title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val message: String,

    @Column(nullable = false, length = 20)
    val severity: String, // "info", "warning", "error"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    val professor: User? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)


