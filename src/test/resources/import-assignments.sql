INSERT INTO public.assignments (assignment_id, course_id, rubric_id, title, description, type, due_date, created_by,
                                created_at)
VALUES (1, 1, 1, 'Homework 1', 'Resolver ejercicios de álgebra básica', 'Tarea', '2025-11-01', 2, now()),
       (2, 1, 2, 'Examen Parcial', 'Evaluación de mitad de curso', 'Examen', '2025-11-15', 2, now());
select * from assignments;
ALTER SEQUENCE assignments_assignment_id_seq RESTART WITH 3;