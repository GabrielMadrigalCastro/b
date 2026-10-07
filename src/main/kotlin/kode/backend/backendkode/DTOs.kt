package kode.backend.backendkode

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ========== PRIVILEGES ==========

data class PrivilegeDetails(
    var id: Long? = null,
    var name: String? = null
)

// ========== ROLES ==========

data class RoleDetails(
    var id: Long? = null,
    var name: String? = null,
    var privileges: List<PrivilegeDetails>? = null
)

// ========== USERS ==========

data class UserInput(
    var id: Long? = null,
    var fullName: String? = null,
    var email: String? = null,
    var enrollmentCard: String? = null,
    var cardVerified: Boolean? = null,
    var roleId: Long? = null
)

data class UserLoginInput(
    var email: String = "",
    var password: String = ""
)

data class UserSignUpInput(
    var fullName: String? = null,
    var email: String? = null,
    var password: String? = null,
    var roleId: Long? = null  // ✅ Ahora acepta role_id como Long desde el frontend
)




data class UserResult(
    var id: Long,
    var fullName: String,
    var email: String,
    var enrollmentCard: String?,
    var cardVerified: Boolean,
    var tokenExpired: Boolean?,
    var createdAt: LocalDateTime,
    var role: RoleDetails? = null
)

// ===================== PRIORITY =====================
data class PriorityInput(
    val label: String
)

data class PriorityResult(
    val id: Long,
    val label: String
)

// ===================== STATUS =====================
data class StatusInput(
    val label: String
)

data class StatusResult(
    val id: Long,
    val label: String
)


// ========== REMINDER ==========


data class ReminderDetails(
    val taskId: Long?,
    val reminderDate: LocalDateTime?
)

data class ReminderInput(
    val taskId: Long,
    val reminderDate: LocalDateTime
)

// ========== TASKS ==========

data class TaskInput(
    val userId: Long,
    val title: String? = null,
    val notes: String? = null,
    val dueDate: LocalDate? = null,

    // Opcional: por ID o por texto
    val priorityId: Long? = null,
    val priorityLabel: String? = null,

    val statusId: Long? = null,
    val statusLabel: String? = null,

    val createDate: LocalDate? = null
)


data class TaskResult(
    val id: Long?,
    var title: String?,
    var notes: String?,
    var dueDate: LocalDate?,
    var createDate: LocalDate?,
    var priority: PriorityResult?,
    var status: StatusResult?,
    var reminders: List<ReminderDetails>?
)

// ===== TOPIC DTOs =====

data class TopicInput(
    val name: String,
    val source: String? = null,
    val courseId: Long
)

data class TopicResult(
    val id: Long?,
    val name: String,
    val source: String?,
    val courseId: Long,
    val createdAt: LocalDateTime?
)

// ===== QUESTION OPTION DTOs =====
data class QuestionOptionInput(
    val questionId: Long,
    val label: String,
    val text: String
)

data class QuestionOptionResult(
    val id: Long?,
    val questionId: Long,
    val label: String,
    val text: String
)


// ===== COURSE DTOs =====
data class CourseInput(
    val professorId: Long,
    val courseName: String,
    val courseCode: String? = null,
    val courseColor: String? = null,
    val independentStudyHours: Int? = null
    //c
)

data class CourseResult(
    val id: Long?,
    val professorId: Long,
    val courseName: String,
    val courseCode: String? = null,
    val joinCode: String? = null,
    val courseColor: String? = null,
    val independentStudyHours: Int? = null,
    val createdAt: LocalDateTime
    //clas
)

/** Para que un estudiante se una a un curso con el código del grupo. */
data class JoinByCodeInput(
    val studentId: Long,
    val code: String
)


// ===== COURSE RUBRIC DTOs =====
data class CourseRubricInput(
    val courseId: Long,
    val rubricName: String? = null,
    val weightPercentage: BigDecimal? = null,
    val dueDate: java.time.LocalDate? = null
)

data class CourseRubricResult(
    val id: Long?,
    val courseId: Long,
    val rubricName: String? = null,
    val weightPercentage: BigDecimal? = null,
    val dueDate: java.time.LocalDate? = null
)

// ===== NOTE (Apuntes) DTOs =====
data class NoteInput(
    val studentId: Long,
    val courseId: Long? = null,   // null = apunte personal (sin curso)
    val title: String,
    val content: String,
    val pinned: Boolean = false,
    val color: String? = null,
    val tags: String? = null,
    val checklist: String? = null
)

data class NoteResult(
    val id: Long?,
    val studentId: Long,
    val courseId: Long? = null,   // null = apunte personal (sin curso)
    val title: String,
    val content: String,
    val pinned: Boolean = false,
    val color: String? = null,
    val tags: String? = null,
    val checklist: String? = null,
    val createdAt: java.time.LocalDateTime? = null
)

// ===== STUDENT RUBRIC GRADE DTOs =====
data class StudentRubricGradeInput(
    val studentId: Long,
    val rubricId: Long,
    val grade: BigDecimal? = null
)

data class StudentRubricGradeResult(
    val id: Long?,
    val studentId: Long,
    val rubricId: Long,
    val grade: BigDecimal? = null
)


// ===== ENROLLMENT DTOs =====
data class EnrollmentInput(
    val courseId: Long,
    val studentId: Long
)

data class EnrollmentResult(
    val id: Long?,
    val courseId: Long,
    val studentId: Long,
    val enrolledAt: LocalDateTime
)


// ===== CLASS (ClassEntity) DTOs =====
data class ClassInput(
    val courseId: Long,
    val professorId: Long,
    val classDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val modality: String? = null,
    val location: String? = null
)

data class ClassResult(
    val id: Long?,
    val courseId: Long,
    val professorId: Long,
    val classDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val modality: String? = null,
    val location: String? = null,
    val createdAt: LocalDateTime
)

// ===== STUDENT CLASS PREFERENCES DTOs =====
data class StudentClassPreferenceInput(
    val classId: Long,
    val studentId: Long,
    val customModality: String? = null
)

data class StudentClassPreferenceResult(
    val id: Long? = null,
    val classId: Long,
    val studentId: Long,
    val customModality: String? = null,
    val updatedAt: LocalDateTime
)


// ===== ASSIGNMENTS DTOs =====
data class AssignmentInput(
    val courseId: Long,
    val rubricId: Long? = null,
    val title: String,
    val description: String? = null,
    val type: String? = null,
    val dueDate: LocalDate? = null,
    val createdBy: Long
)

data class AssignmentResult(
    val id: Long? = null,
    val courseId: Long,
    val rubricId: Long? = null,
    val title: String,
    val description: String? = null,
    val type: String? = null,
    val dueDate: LocalDate? = null,
    val createdBy: Long,
    val createdAt: LocalDateTime
)


// ===== STUDENT ASSIGNMENTS DTOs =====
data class StudentAssignmentInput(
    val assignmentId: Long,
    val studentId: Long,
    val status: String? = null,
    val grade: BigDecimal? = null,
    val submittedAt: LocalDateTime? = null
)

data class StudentAssignmentResult(
    val id: Long? = null,
    val assignmentId: Long,
    val studentId: Long,
    val status: String? = null,
    val grade: BigDecimal? = null,
    val submittedAt: LocalDateTime? = null
)


// ===== STUDY PLANS DTOs =====
data class StudyPlanInput(
    val studentId: Long,
    val assignmentId: Long,
    val plannedDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val status: String? = null,
    val generatedByAi: Boolean? = true
)

data class StudyPlanResult(
    val id: Long? = null,
    val studentId: Long,
    val assignmentId: Long,
    val plannedDate: LocalDate?,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val status: String? = null,
    val generatedByAi: Boolean? = true
)

data class GenerarPlanRequest(
    val studentId: Long,
    val assignmentId: Long,
    val horasDisponiblesPorDia: Int = 2,
    val diasAntesDueDate: Int = 7
)

// ===================== ALERT DTOs =====================
data class AlertInput(
    val professorId: Long,
    val title: String,
    val message: String,
    val severity: String
)

data class AlertResult(
    val id: Long,
    val title: String,
    val message: String,
    val severity: String,
    val professorId: Long?,
    val createdAt: LocalDateTime
)
