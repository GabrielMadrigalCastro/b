INSERT INTO public.classes (class_id, course_id, professor_id, class_date, start_time, end_time, modality, location,
                            created_at)
VALUES (1, 1, 2, '2025-10-10', '08:00', '10:00', 'Presencial', 'Aula 101', now()),
       (2, 1, 2, '2025-10-11', '10:00', '12:00', 'Virtual', 'Zoom Link', now());
