-- =========================================================
-- BACKEND DELTA - ESQUEMA COMPLETO (PostgreSQL)
-- =========================================================
-- Orden de creación respeta las dependencias de FK.
-- Ejecutar de corrido crea toda la base sin errores.
-- Los bloques de DROP y de reseteo de secuencias están
-- al final, SEPARADOS, para ejecutarse solo cuando se
-- necesiten (no forman parte de la creación).
-- =========================================================


-- =========================================
-- USERS & ROLES (Global)
-- =========================================

CREATE TABLE roles (
  role_id   SERIAL PRIMARY KEY,
  role_name VARCHAR(50) UNIQUE
);

CREATE TABLE users (
  user_id         SERIAL PRIMARY KEY,
  role_id         INT REFERENCES roles(role_id),
  full_name       VARCHAR(150) NOT NULL,
  email           VARCHAR(100) UNIQUE NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  enrollment_card VARCHAR(255),
  card_verified   BOOLEAN DEFAULT FALSE,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =========================================
-- ACCESS CONTROL
-- =========================================

CREATE TABLE privileges (
  privilege_id SERIAL PRIMARY KEY,
  name         VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE user_role (
  user_id INT REFERENCES users(user_id),
  role_id INT REFERENCES roles(role_id),
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_privilege (
  role_id      INT REFERENCES roles(role_id),
  privilege_id INT REFERENCES privileges(privilege_id),
  PRIMARY KEY (role_id, privilege_id)
);


-- =========================================
-- MODULE 1: PLANNER
-- =========================================

CREATE TABLE courses (
  course_id               SERIAL PRIMARY KEY,
  professor_id            INT REFERENCES users(user_id),
  course_name             VARCHAR(150) NOT NULL,
  course_code             VARCHAR(50),
  course_color            VARCHAR(20),
  independent_study_hours INT,
  created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE courserubrics (
  rubric_id         SERIAL PRIMARY KEY,
  course_id         INT REFERENCES courses(course_id),
  rubric_name       VARCHAR(100),
  weight_percentage DECIMAL(5,2)
);

CREATE TABLE enrollments (
  enrollment_id SERIAL PRIMARY KEY,
  course_id     INT REFERENCES courses(course_id),
  student_id    INT REFERENCES users(user_id),
  enrolled_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE classes (
  class_id     SERIAL PRIMARY KEY,
  course_id    INT REFERENCES courses(course_id),
  professor_id INT REFERENCES users(user_id),
  class_date   DATE,
  start_time   TIME,
  end_time     TIME,
  modality     VARCHAR(20),
  location     VARCHAR(255),
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE studentclasspreferences (
  preference_id   SERIAL PRIMARY KEY,
  class_id        INT REFERENCES classes(class_id),
  student_id      INT REFERENCES users(user_id),
  custom_modality VARCHAR(20),
  updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE assignments (
  assignment_id SERIAL PRIMARY KEY,
  course_id     INT REFERENCES courses(course_id),        -- ✅ columna corregida
  rubric_id     INT REFERENCES courserubrics(rubric_id),
  title         VARCHAR(200),
  description   TEXT,
  type          VARCHAR(50),
  due_date      DATE,
  created_by    INT REFERENCES users(user_id),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE studentassignments (
  student_assignment_id SERIAL PRIMARY KEY,
  assignment_id         INT REFERENCES assignments(assignment_id),
  student_id            INT REFERENCES users(user_id),
  status                VARCHAR(20),
  grade                 DECIMAL(5,2),
  submitted_at          TIMESTAMP
);

CREATE TABLE studyplans (
  plan_id         SERIAL PRIMARY KEY,
  student_id      INT REFERENCES users(user_id),
  assignment_id   INT REFERENCES assignments(assignment_id),
  planned_date    DATE,
  start_time      TIME,
  end_time        TIME,
  status          VARCHAR(20),
  generated_by_ai BOOLEAN DEFAULT FALSE
);


-- =========================================
-- TASKS
-- =========================================

CREATE TABLE priority (
  priority_id SERIAL PRIMARY KEY,
  label       VARCHAR(255) UNIQUE
);

CREATE TABLE status (
  status_id SERIAL PRIMARY KEY,
  label     VARCHAR(255) UNIQUE
);

CREATE TABLE tasks (
  task_id     SERIAL PRIMARY KEY,
  user_id     INT REFERENCES users(user_id),
  create_date DATE DEFAULT CURRENT_DATE,
  due_date    DATE,
  notes       VARCHAR(255),
  title       VARCHAR(255),
  priority_id INT REFERENCES priority(priority_id),
  status_id   INT REFERENCES status(status_id)
);

CREATE TABLE reminders (
  task_id       INT REFERENCES tasks(task_id),
  reminder_date TIMESTAMP,
  PRIMARY KEY (task_id, reminder_date)
);


-- =========================================
-- MODULE 3: MY NOTES
-- =========================================

CREATE TABLE notes (
  note_id    SERIAL PRIMARY KEY,
  student_id INT REFERENCES users(user_id),
  course_id  INT REFERENCES courses(course_id),
  title      VARCHAR(200),
  content    TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =========================================
-- TOPICS
-- =========================================

CREATE TABLE topics (
  topic_id   SERIAL PRIMARY KEY,
  course_id  INT REFERENCES courses(course_id),
  name       VARCHAR(150),
  source     VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE note_topic (
  note_id  INT REFERENCES notes(note_id),
  topic_id INT REFERENCES topics(topic_id),
  PRIMARY KEY (note_id, topic_id)
);


-- =========================================
-- MODULE 2: MOODLEARNING
-- =========================================

CREATE TABLE exams (
  exam_id         SERIAL PRIMARY KEY,
  student_id      INT REFERENCES users(user_id),
  course_id       INT REFERENCES courses(course_id),
  level           INT,
  size_label      VARCHAR(10),
  questions_count INT,
  generated_by_ai BOOLEAN DEFAULT FALSE,
  status          VARCHAR(15),
  started_at      TIMESTAMP,
  completed_at    TIMESTAMP,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exam_topic (
  exam_id  INT REFERENCES exams(exam_id),
  topic_id INT REFERENCES topics(topic_id),
  PRIMARY KEY (exam_id, topic_id)
);

CREATE TABLE questions (
  question_id    SERIAL PRIMARY KEY,
  exam_id        INT REFERENCES exams(exam_id),
  topic_id       INT REFERENCES topics(topic_id),
  order_no       INT,
  text           TEXT,
  difficulty     VARCHAR(20),
  correct_answer TEXT
);

CREATE TABLE questionoptions (
  option_id   SERIAL PRIMARY KEY,
  question_id INT REFERENCES questions(question_id),
  label       CHAR(1),
  text        TEXT
);

CREATE TABLE responses (
  response_id      SERIAL PRIMARY KEY,
  question_id      INT REFERENCES questions(question_id),
  student_id       INT REFERENCES users(user_id),
  chosen_option_id INT REFERENCES questionoptions(option_id),
  is_correct       BOOLEAN,
  answered_at      TIMESTAMP
);

CREATE TABLE emotionevents (
  event_id     SERIAL PRIMARY KEY,
  student_id   INT REFERENCES users(user_id),
  exam_id      INT REFERENCES exams(exam_id),
  question_id  INT REFERENCES questions(question_id),
  context      VARCHAR(20),
  emotion_type VARCHAR(50),
  intensity    INT,
  message      TEXT,
  recorded_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE examsummary (
  exam_id       INT PRIMARY KEY REFERENCES exams(exam_id),
  student_id    INT REFERENCES users(user_id),
  correct_count INT,
  wrong_count   INT,
  accuracy      DECIMAL(5,2),
  passed_level  BOOLEAN,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE studentcourseprogress (
  progress_id       SERIAL PRIMARY KEY,
  student_id        INT REFERENCES users(user_id),
  course_id         INT REFERENCES courses(course_id),
  current_level     INT,
  cumulative_correct INT,
  cumulative_wrong  INT,
  last_exam_id      INT REFERENCES exams(exam_id),
  updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (student_id, course_id)
);

CREATE TABLE recommendations (
  recommendation_id SERIAL PRIMARY KEY,
  course_id         INT REFERENCES courses(course_id),
  topic_id          INT REFERENCES topics(topic_id),
  text              TEXT,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =========================================
-- IMAGE RECOGNITION
-- =========================================

CREATE TABLE uploadedfiles (
  file_id         SERIAL PRIMARY KEY,
  user_id         INT REFERENCES users(user_id),
  file_type       VARCHAR(50),
  file_path       VARCHAR(255),
  recognized_text TEXT,
  uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  verified        BOOLEAN DEFAULT FALSE
);


-- =========================================================
-- FIN DE LA CREACIÓN
-- =========================================================


-- =========================================================
-- BLOQUE DE MANTENIMIENTO (NO ejecutar junto con la creación)
-- =========================================================
-- Estos bloques van por separado. Ejecuta uno u otro según
-- lo que necesites: borrar todo, o resetear secuencias.
-- =========================================================

/*  -- ############  DROP DE TODAS LAS TABLAS  ############
-- Orden inverso a la creación (respeta las FK).

-- Image recognition
DROP TABLE IF EXISTS uploadedfiles CASCADE;

-- Moodlearning
DROP TABLE IF EXISTS recommendations CASCADE;
DROP TABLE IF EXISTS studentcourseprogress CASCADE;
DROP TABLE IF EXISTS examsummary CASCADE;
DROP TABLE IF EXISTS emotionevents CASCADE;
DROP TABLE IF EXISTS responses CASCADE;
DROP TABLE IF EXISTS questionoptions CASCADE;
DROP TABLE IF EXISTS questions CASCADE;
DROP TABLE IF EXISTS exam_topic CASCADE;
DROP TABLE IF EXISTS exams CASCADE;

-- Topics y Notes
DROP TABLE IF EXISTS note_topic CASCADE;
DROP TABLE IF EXISTS topics CASCADE;
DROP TABLE IF EXISTS notes CASCADE;

-- Tasks
DROP TABLE IF EXISTS reminders CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS status CASCADE;
DROP TABLE IF EXISTS priority CASCADE;

-- Planner
DROP TABLE IF EXISTS studyplans CASCADE;
DROP TABLE IF EXISTS studentassignments CASCADE;
DROP TABLE IF EXISTS assignments CASCADE;
DROP TABLE IF EXISTS studentclasspreferences CASCADE;
DROP TABLE IF EXISTS classes CASCADE;
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS courserubrics CASCADE;
DROP TABLE IF EXISTS courses CASCADE;

-- Access control
DROP TABLE IF EXISTS role_privilege CASCADE;
DROP TABLE IF EXISTS user_role CASCADE;
DROP TABLE IF EXISTS privileges CASCADE;

-- Users & Roles
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
*/


/*  -- ############  RESET DE SECUENCIAS  ############
-- Ejecutar SOLO con las tablas ya creadas (nunca después del DROP).

ALTER SEQUENCE users_user_id_seq       RESTART WITH 4;
ALTER SEQUENCE tasks_task_id_seq       RESTART WITH 2;
ALTER SEQUENCE assignments_assignment_id_seq RESTART WITH 3;

SELECT setval(
  pg_get_serial_sequence('studentassignments', 'student_assignment_id'),
  COALESCE(MAX(student_assignment_id), 1)
)
FROM studentassignments;
*/