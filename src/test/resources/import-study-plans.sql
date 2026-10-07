INSERT INTO public.studyplans (plan_id, student_id, assignment_id, planned_date, start_time, end_time, status,
                               generated_by_ai)
VALUES (1, 3, 1, '2025-10-15', '18:00', '20:00', 'Pendiente', TRUE),
       (2, 3, 2, '2025-10-16', '19:00', '21:00', 'En progreso', TRUE);
-- Ver el máximo ID actual
SELECT MAX(plan_id) FROM studyplans;

-- Ajustar la secuencia al siguiente valor disponible
SELECT setval('studyplans_plan_id_seq', (SELECT MAX(plan_id) FROM studyplans));

-- O si quieres que el próximo sea específico (ej: 100)
SELECT setval('studyplans_plan_id_seq', 100);