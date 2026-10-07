package kode.backend.backendkode

interface IAResponseStrategy {
    fun generarRespuesta(prompt: String): String
    fun generarPlanEstudio(
        studentId: Long,
        assignmentId: Long,
        horasDisponiblesPorDia: Int,
        diasAntesDueDate: Int
    ):  List<StudyPlanResult>
}
