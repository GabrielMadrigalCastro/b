INSERT INTO public.exams (exam_id, student_id, course_id, level, size_label, questions_count, generated_by_ai, status,
                          started_at, completed_at, created_at)
VALUES (1, 3, 1, 1, 'S', 3, FALSE, 'COMPLETED', now() - interval '1 hour', now(), now()),
       (2, 3, 1, 2, 'M', 5, TRUE, 'PENDING', NULL, NULL, now());
