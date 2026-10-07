package kode.backend.backendkode

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Al arrancar:
 *  - Siembra los roles base (ADMIN, TEACHER, STUDENT) si la tabla está vacía.
 *  - Si RESET_DATA=true: BORRA TODO y deja solo el par de prueba (profe + alumno)
 *    con un curso, rúbricas, una clase de hoy y el alumno matriculado.
 *    (Poner RESET_DATA=false después para no borrar en cada reinicio.)
 *  - Si no, crea/garantiza el par de prueba sin borrar a nadie.
 */
@Component
class DataSeeder(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val courseRubricRepository: CourseRubricRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val classRepository: ClassRepository,
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${RESET_DATA:false}") private val resetData: Boolean
) : CommandLineRunner {

    private companion object {
        const val PASS_PROF = "Profesor2026"
        const val PASS_ALUMNO = "Alumno2026"
        const val EMAIL_PROF = "profesor.prueba@una.cr"
        const val EMAIL_ALUMNO = "estudiante.prueba@est.una.ac.cr"
    }

    override fun run(vararg args: String?) {
        seedRoles()
        if (resetData) {
            resetAndSeed()
            return
        }
        seedTestUser()
        seedTestProfessorCourse()
        cleanupExampleCourses()
    }

    private fun seedRoles() {
        if (roleRepository.count() == 0L) {
            roleRepository.save(Role(roleName = "ADMIN"))
            roleRepository.save(Role(roleName = "TEACHER"))
            roleRepository.save(Role(roleName = "STUDENT"))
            println("✅ Roles sembrados: ADMIN(1), TEACHER(2), STUDENT(3)")
        }
    }

    /**
     * RESET TOTAL: borra todos los datos (usuarios, cursos, rúbricas, notas,
     * apuntes, matrículas, clases…) conservando las tablas de catálogo (roles,
     * privilegios, prioridad, estado) y vuelve a sembrar el par de prueba.
     *
     * Usa TRUNCATE ... CASCADE: al truncar 'users' se limpian en cadena todas las
     * tablas que dependen de usuarios/cursos, sin pelear con las llaves foráneas.
     */
    private fun resetAndSeed() {
        println("♻️  RESET_DATA=true → BORRANDO TODOS LOS DATOS y sembrando prueba…")
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE")

        seedRoles() // por si el catálogo estuviera vacío (no se trunca, pero por seguridad)
        val teacherRole = roleRepository.findById(2L).orElseThrow { IllegalStateException("Falta rol TEACHER(2)") }
        val studentRole = roleRepository.findById(3L).orElseThrow { IllegalStateException("Falta rol STUDENT(3)") }
        val encoder = BCryptPasswordEncoder()

        val professor = userRepository.save(
            User(
                fullName = "Profe Prueba",
                email = EMAIL_PROF,
                passwordHash = encoder.encode(PASS_PROF),
                role = teacherRole,
                cardVerified = true
            )
        )
        val student = userRepository.save(
            User(
                fullName = "Estudiante Prueba",
                email = EMAIL_ALUMNO,
                passwordHash = encoder.encode(PASS_ALUMNO),
                role = studentRole,
                cardVerified = true
            )
        )

        val course = courseRepository.save(
            Course(
                professor = professor,
                courseName = "Curso de Prueba",
                courseCode = "PR-101",
                joinCode = "PRUEBA",
                courseColor = "#6B46C1"
            )
        )
        courseRubricRepository.save(CourseRubric(course = course, rubricName = "Examen 1", weightPercentage = BigDecimal("30")))
        courseRubricRepository.save(CourseRubric(course = course, rubricName = "Proyecto", weightPercentage = BigDecimal("40")))
        courseRubricRepository.save(CourseRubric(course = course, rubricName = "Quices", weightPercentage = BigDecimal("30")))

        enrollmentRepository.save(Enrollment(course = course, student = student))

        // Una clase de hoy (para probar cambio de modalidad y el plan del día).
        classRepository.save(
            ClassEntity(
                course = course,
                professor = professor,
                classDate = LocalDate.now(),
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(10, 0),
                modality = "Presencial",
                location = "Aula 1"
            )
        )

        println("✅ RESET listo.")
        println("   👨‍🏫 Profesor: $EMAIL_PROF / $PASS_PROF")
        println("   🎓 Alumno:   $EMAIL_ALUMNO / $PASS_ALUMNO (matriculado en 'Curso de Prueba')")
        println("⚠️  IMPORTANTE: poné RESET_DATA=false en Render para no borrar en cada reinicio.")
    }

    /** Crea un usuario de prueba (rol Estudiante) si aún no existe. */
    private fun seedTestUser() {
        val email = "Ariannachaves27@gmail.com"
        if (userRepository.findByEmail(email).isPresent) return

        val student = roleRepository.findById(3L).orElse(null) ?: run {
            println("⚠️ No se pudo crear el usuario de prueba: falta el rol STUDENT(3).")
            return
        }

        val user = User(
            fullName = "Arianna Chaves",
            email = email,
            passwordHash = BCryptPasswordEncoder().encode("prueba123"),
            role = student,
            cardVerified = true
        )
        userRepository.save(user)
        println("✅ Usuario de prueba creado: $email (rol Estudiante).")
    }

    /**
     * Datos de prueba para el rol profesor (sin borrar nada):
     *  - Profesor: profesor.prueba@una.cr
     *  - Estudiante: estudiante.prueba@gmail.com
     *  - Un curso del profe con 3 rúbricas y el estudiante matriculado.
     */
    private fun seedTestProfessorCourse() {
        val teacherRole = roleRepository.findById(2L).orElse(null) ?: return
        val studentRole = roleRepository.findById(3L).orElse(null) ?: return
        val encoder = BCryptPasswordEncoder()

        val professor = userRepository.findByEmail(EMAIL_PROF).orElseGet {
            userRepository.save(
                User(
                    fullName = "Profe Prueba",
                    email = EMAIL_PROF,
                    passwordHash = encoder.encode(PASS_PROF),
                    role = teacherRole,
                    cardVerified = true
                )
            )
        }

        val student = userRepository.findByEmail(EMAIL_ALUMNO).orElseGet {
            userRepository.save(
                User(
                    fullName = "Estudiante Prueba",
                    email = EMAIL_ALUMNO,
                    passwordHash = encoder.encode(PASS_ALUMNO),
                    role = studentRole,
                    cardVerified = true
                )
            )
        }

        val profId = professor.id ?: return
        val course = courseRepository.findByProfessorId(profId)
            .firstOrNull { it.courseName == "Curso de Prueba" }
            ?: courseRepository.save(
                Course(
                    professor = professor,
                    courseName = "Curso de Prueba",
                    courseCode = "PR-101",
                    joinCode = "PRUEBA",
                    courseColor = "#6B46C1"
                )
            )
        val courseId = course.id ?: return

        if (courseRubricRepository.findByCourseId(courseId).isEmpty()) {
            courseRubricRepository.save(CourseRubric(course = course, rubricName = "Examen 1", weightPercentage = BigDecimal("30")))
            courseRubricRepository.save(CourseRubric(course = course, rubricName = "Proyecto", weightPercentage = BigDecimal("40")))
            courseRubricRepository.save(CourseRubric(course = course, rubricName = "Quices", weightPercentage = BigDecimal("30")))
        }

        val yaMatriculado = enrollmentRepository.findByCourseId(courseId).any { it.student.id == student.id }
        if (!yaMatriculado) {
            enrollmentRepository.save(Enrollment(course = course, student = student))
        }

        println("✅ Prueba profesor: $EMAIL_PROF, curso 'Curso de Prueba', $EMAIL_ALUMNO matriculado.")
    }

    /** Borra los cursos demo + sus rúbricas y matrículas (en orden, para no violar FKs). */
    private fun cleanupExampleCourses() {
        val demo = userRepository.findByEmail("profesor.demo@una.cr").orElse(null) ?: return
        val demoId = demo.id ?: return
        val examples = courseRepository.findByProfessorId(demoId)
        if (examples.isEmpty()) return

        examples.forEach { course ->
            val cid = course.id ?: return@forEach
            courseRubricRepository.deleteAll(courseRubricRepository.findByCourseId(cid))
            enrollmentRepository.deleteAll(enrollmentRepository.findByCourseId(cid))
        }
        courseRepository.deleteAll(examples)
        println("🗑️ Cursos de ejemplo eliminados (${examples.size}).")
    }
}
