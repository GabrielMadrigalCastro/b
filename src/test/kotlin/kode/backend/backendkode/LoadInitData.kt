package kode.backend.backendkode


import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.jdbc.Sql

@Profile("initlocal")
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(
    statements = [
        "DELETE FROM public.recommendations",
        "DELETE FROM public.studentcourseprogress",
        "DELETE FROM public.examsummary",
        "DELETE FROM public.emotionevents",
        "DELETE FROM public.responses",
        "DELETE FROM public.questionoptions",
        "DELETE FROM public.questions",
        "DELETE FROM public.exam_topic",
        "DELETE FROM public.exams",
        "DELETE FROM public.note_topic",
        "DELETE FROM public.topics",
        "DELETE FROM public.notes",
        "DELETE FROM public.reminders",
        "DELETE FROM public.tasks",
        "DELETE FROM public.status",
        "DELETE FROM public.priority",
        "DELETE FROM public.studyplans",
        "DELETE FROM public.studentassignments",
        "DELETE FROM public.assignments",
        "DELETE FROM public.studentclasspreferences",
        "DELETE FROM public.classes",
        "DELETE FROM public.enrollments",
        "DELETE FROM public.courserubrics",
        "DELETE FROM public.courses",
        "DELETE FROM public.role_privilege",
        "DELETE FROM public.user_role",
        "DELETE FROM public.privilegies",
        "DELETE FROM public.users",
        "DELETE FROM public.roles"
    ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = [
        "/import-roles.sql",
        "/import-users.sql",
        "/import-privileges.sql",
        "/import-role-privilege.sql",
        "/import-user-roles.sql",
        "/import-courses.sql",
        "/import-course-rubrics.sql",
        "/import-enrollments.sql",
        "/import-classes.sql",
        "/import-student-class-preferences.sql",
        "/import-assignments.sql",
        "/import-student-assignments.sql",
        "/import-study-plans.sql",
        "/import-notes.sql",
        "/import-topics.sql",
        "/import-note-topic.sql",
        "/import-exams.sql",
        "/import-exam-topic.sql",
        "/import-questions.sql",
        "/import-questionoptions.sql",
        "/import-responses.sql",
        "/import-emotionevents.sql",
        "/import-examsummary.sql",
        "/import-studentcourseprogress.sql",
        "/import-recommendations.sql",
        "/import-priorities.sql",
        "/import-status.sql",
        "/import-tasks.sql",
        "/import-reminders.sql"
    ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)

class LoadInitDataTest(
    @Autowired val roleRepository: RoleRepository,
    @Autowired val userRepository: UserRepository,
    @Autowired val privilegeRepository: PrivilegeRepository,
    @Autowired val userRoleRepository: UserRoleRepository,
    @Autowired val rolePrivilegeRepository: RolePrivilegeRepository,
    @Autowired val courseRepository: CourseRepository,
    @Autowired val courseRubricRepository: CourseRubricRepository,
    @Autowired val enrollmentRepository: EnrollmentRepository,
    @Autowired val classRepository: ClassRepository,
    @Autowired val studentClassPreferenceRepository: StudentClassPreferenceRepository,
    @Autowired val assignmentRepository: AssignmentRepository,
    @Autowired val studentAssignmentRepository: StudentAssignmentRepository,
    @Autowired val studyPlanRepository: StudyPlanRepository,
    @Autowired val priorityRepository: PriorityRepository,
    @Autowired val statusRepository: StatusRepository,
    @Autowired val taskRepository: TaskRepository,
    @Autowired val reminderRepository: ReminderRepository,
    // ========== NOTES & TOPICS ==========
    @Autowired val noteRepository: NoteRepository,
    @Autowired val topicRepository: TopicRepository,

    // ========== EXAMS ==========
    @Autowired val examRepository: ExamRepository,
    @Autowired val questionRepository: QuestionRepository,
    @Autowired val questionOptionRepository: QuestionOptionRepository,
    @Autowired val responseRepository: ResponseRepository,
    @Autowired val emotionEventRepository: EmotionEventRepository,
    @Autowired val examSummaryRepository: ExamSummaryRepository,
    @Autowired val studentCourseProgressRepository: StudentCourseProgressRepository,
    @Autowired val recommendationRepository: RecommendationRepository
) {

    @Test
    fun testRolesLoaded() {
        val roles = roleRepository.findAll()
        assertTrue(roles.isNotEmpty(), "Debe haber roles cargados")
    }

    @Test
    fun testPrivilegesLoaded() {
        val privileges = privilegeRepository.findAll()
        assertTrue(privileges.isNotEmpty(), "Debe haber privilegios cargados")
    }

    @Test
    fun testUserRolesLoaded() {
        val userRoles = userRoleRepository.findAll()
        assertTrue(userRoles.isNotEmpty(), "Debe haber user roles cargados")
    }

    @Test
    fun testRolePrivilegesLoaded() {
        val rolePrivileges = rolePrivilegeRepository.findAll()
        assertTrue(rolePrivileges.isNotEmpty(), "Debe haber role privileges cargados")
    }

    @Test
    fun testCourseRubricsLoaded() {
        val rubrics = courseRubricRepository.findAll()
        assertTrue(rubrics.isNotEmpty(), "Debe haber course rubrics cargadas")
    }

    @Test
    fun testEnrollmentsLoaded() {
        val enrollments = enrollmentRepository.findAll()
        assertTrue(enrollments.isNotEmpty(), "Debe haber enrollments cargados")
    }

    @Test
    fun testStudentClassPreferencesLoaded() {
        val preferences = studentClassPreferenceRepository.findAll()
        assertTrue(preferences.isNotEmpty(), "Debe haber preferencias cargadas")
    }

    @Test
    fun testStudentAssignmentsLoaded() {
        val studentAssignments = studentAssignmentRepository.findAll()
        assertTrue(studentAssignments.isNotEmpty(), "Debe haber student assignments cargadas")
    }

    @Test
    fun testPrioritiesLoaded() {
        val priorities = priorityRepository.findAll()
        assertTrue(priorities.isNotEmpty(), "Debe haber prioridades cargadas")
    }

    @Test
    fun testStatusLoaded() {
        val statuses = statusRepository.findAll()
        assertTrue(statuses.isNotEmpty(), "Debe haber status cargados")
    }

    @Test
    fun testRemindersLoaded() {
        val reminders = reminderRepository.findAll()
        assertTrue(reminders.isNotEmpty(), "Debe haber reminders cargados")
    }

    @Test
    fun testTasksLoaded() {
        val tasks = taskRepository.findAll()
        assertTrue(tasks.isNotEmpty(), "Debe haber tasks cargadas")
    }

    @Test
    fun testUsersLoaded() {
        val users = userRepository.findAll()
        assertTrue(users.isNotEmpty(), "Debe haber users cargados")
    }

    @Test
    fun testCoursesLoaded() {
        val courses = courseRepository.findAll()
        assertTrue(courses.isNotEmpty(), "Debe haber cursos cargados")
    }


    @Test
    fun testClassesLoaded() {
        val classes = classRepository.findAll()
        assertTrue(classes.isNotEmpty(), "Debe haber clases cargadas")
    }

    @Test
    fun testAssignmentsLoaded() {
        val assignments = assignmentRepository.findAll()
        assertTrue(assignments.isNotEmpty(), "Debe haber asignaciones cargadas")
    }

    @Test
    fun testStudyPlansLoaded() {
        val plans = studyPlanRepository.findAll()
        assertTrue(plans.isNotEmpty(), "Debe haber planes cargados")
    }

    @Test
    fun testNotesLoaded() {
        val notes = noteRepository.findAll()
        assertTrue(notes.isNotEmpty(), "Debe haber notas cargadas")
    }

    @Test
    fun testTopicsLoaded() {
        val topics = topicRepository.findAll()
        assertTrue(topics.isNotEmpty(), "Debe haber topics cargados")
    }

    @Test
    fun testExamsLoaded() {
        val exams = examRepository.findAll()
        assertTrue(exams.isNotEmpty(), "Debe haber exámenes cargados")
    }

    @Test
    fun testQuestionsLoaded() {
        val questions = questionRepository.findAll()
        assertTrue(questions.isNotEmpty(), "Debe haber preguntas cargadas")
    }

    @Test
    fun testQuestionOptionsLoaded() {
        val options = questionOptionRepository.findAll()
        assertTrue(options.isNotEmpty(), "Debe haber opciones cargadas")
    }

    @Test
    fun testResponsesLoaded() {
        val responses = responseRepository.findAll()
        assertTrue(responses.isNotEmpty(), "Debe haber respuestas cargadas")
    }

    @Test
    fun testEmotionEventsLoaded() {
        val events = emotionEventRepository.findAll()
        assertTrue(events.isNotEmpty(), "Debe haber eventos emocionales cargados")
    }

    @Test
    fun testExamSummaryLoaded() {
        val summaries = examSummaryRepository.findAll()
        assertTrue(summaries.isNotEmpty(), "Debe haber resúmenes de examen cargados")
    }

    @Test
    fun testStudentCourseProgressLoaded() {
        val progress = studentCourseProgressRepository.findAll()
        assertTrue(progress.isNotEmpty(), "Debe haber progreso cargado")
    }

    @Test
    fun testRecommendationsLoaded() {
        val recs = recommendationRepository.findAll()
        assertTrue(recs.isNotEmpty(), "Debe haber recomendaciones cargadas")
    }
}