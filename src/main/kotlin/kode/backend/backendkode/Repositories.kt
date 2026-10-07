package kode.backend.backendkode

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*


/* ===================== USERS & ROLES ===================== */

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByRoleName(@Param("roleName") roleName: String): Optional<Role>
}

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(@Param("email") email: String): Optional<User>
}

@Repository
interface PrivilegeRepository : JpaRepository<Privilege, Long> {
   // fun findByName(@Param("name") name: String): Optional<Privilege>
}

@Repository
interface UserRoleRepository : JpaRepository<UserRole, UserRoleId> {
  //  fun findByUserId(@Param("userId") userId: Long): List<UserRole>
}

@Repository
interface RolePrivilegeRepository : JpaRepository<RolePrivilege, RolePrivilegeId> {
  //  fun findByRoleId(@Param("roleId") roleId: Long): List<RolePrivilege>
}


// ===================== COURSES & PLANNER =====================
@Repository
interface CourseRepository : JpaRepository<Course, Long> {
    fun findByProfessorId(professorId: Long): List<Course>
    fun findByJoinCode(joinCode: String): Course?
}

@Repository
interface CourseRubricRepository : JpaRepository<CourseRubric, Long> {
    fun findByCourseId(courseId: Long): List<CourseRubric>
}

@Repository
interface EnrollmentRepository : JpaRepository<Enrollment, Long>{
    fun findByStudentId(studentId: Long): List<Enrollment>
    fun findByCourseId(courseId: Long): List<Enrollment>
}

@Repository
interface ClassRepository : JpaRepository<ClassEntity, Long>


@Repository
interface StudentClassPreferenceRepository : JpaRepository<StudentClassPreference, Long>

@Repository
interface AssignmentRepository : JpaRepository<Assignment, Long>

@Repository
interface StudentAssignmentRepository : JpaRepository<StudentAssignment, Long>

@Repository
interface StudentRubricGradeRepository : JpaRepository<StudentRubricGrade, Long> {
    fun findByStudentId(studentId: Long): List<StudentRubricGrade>
    fun findByStudentIdAndRubricId(studentId: Long, rubricId: Long): StudentRubricGrade?
}

@Repository
interface StudyPlanRepository : JpaRepository<StudyPlan, Long>

// ===================== TASKS =====================
@Repository
interface PriorityRepository : JpaRepository<Priority, Long> {
    fun findByLabelIgnoreCase(label: String): Priority?
}

@Repository
interface StatusRepository : JpaRepository<Status, Long> {
    fun findByLabelIgnoreCase(label: String): Status?
}

@Repository
interface TaskRepository : JpaRepository<Task, Long>

@Repository
interface ReminderRepository : JpaRepository<Reminder, ReminderId>

// ===================== NOTES & TOPICS =====================
@Repository
interface NoteRepository : JpaRepository<Note, Long> {
    fun findByStudentIdOrderByCreatedAtDesc(studentId: Long): List<Note>
    fun findByStudentIdAndCourseIdOrderByCreatedAtDesc(studentId: Long, courseId: Long): List<Note>
}

@Repository
interface TopicRepository : JpaRepository<Topic, Long>

// ===================== EXAMS =====================
@Repository
interface ExamRepository : JpaRepository<Exam, Long>

@Repository
interface QuestionRepository : JpaRepository<Question, Long>

@Repository
interface QuestionOptionRepository : JpaRepository<QuestionOption, Long>

@Repository
interface ResponseRepository : JpaRepository<Response, Long>

@Repository
interface EmotionEventRepository : JpaRepository<EmotionEvent, Long>

@Repository
interface ExamSummaryRepository : JpaRepository<ExamSummary, Long>

@Repository
interface StudentCourseProgressRepository : JpaRepository<StudentCourseProgress, Long>

@Repository
interface RecommendationRepository : JpaRepository<Recommendation, Long>


@Repository
interface AlertRepository : JpaRepository<Alert, Long> {
    fun findTopByOrderByCreatedAtDesc(): Alert?

    fun findByProfessorIdAndCreatedAtBetween(
        professorId: Long,
        startOfDay: LocalDateTime,
        endOfDay: LocalDateTime
    ): List<Alert>

}


