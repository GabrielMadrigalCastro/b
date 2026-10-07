INSERT INTO public.tasks (task_id, user_id, create_date, due_date, notes, title, priority_id, status_id)
VALUES (1, 3, current_date, current_date + interval '7 days', 'Finish Math Homework', 'Math HW', 2, 1),
       (2, 3, current_date, current_date + interval '5 days', 'Prepare presentation slides for Science project',
        'Science Presentation', 1, 2);
