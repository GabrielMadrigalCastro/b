INSERT INTO public.emotionevents (event_id, student_id, exam_id, question_id, context, emotion_type, intensity, message,
                                  recorded_at)
VALUES (1, 3, 1, 1, 'EXAM', 'Concentrado', 7, 'Resolviendo operaciones básicas', now()),
       (2, 3, 1, 2, 'EXAM', 'Frustración', 5, 'No recordaba la definición exacta', now());
