INSERT INTO public.responses (response_id, question_id, student_id, chosen_option_id, is_correct, answered_at)
VALUES (1, 1, 3, 2, TRUE, now()),
       (2, 2, 3, NULL, FALSE, now()),
       (3, 3, 3, 5, TRUE, now());
