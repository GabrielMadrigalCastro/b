-- ===============================================
-- 🔧 RESETEO DE TABLAS BÁSICAS
-- ===============================================
TRUNCATE TABLE users RESTART IDENTITY CASCADE;


-- ===============================================
INSERT INTO users (
    user_id,
    role_id,
    full_name,
    email,
    password_hash,
    card_verified,
    created_at,
    token_expired
)
VALUES
    (1, 1, 'Administrador General', 'admin@kode.com',
     '$2a$10$3k4WiWC8QWWcdQHcKY1XxePmSU2V69irifpYdmoa2yAh4Z7kPtrVG',
     TRUE, NOW(), FALSE),

    (2, 2, 'Profesor Ejemplo', 'profesor@kode.com',
     '$2a$10$3k4WiWC8QWWcdQHcKY1XxePmSU2V69irifpYdmoa2yAh4Z7kPtrVG',
     TRUE, NOW(), FALSE),

    (3, 3, 'Estudiante Ejemplo', 'estudiedo@estudiante.com',
     '$2a$10$3k4WiWC8QWWcdQHcKY1XxePmSU2V69irifpYdmoa2yAh4Z7kPtrVG',
     TRUE, NOW(), FALSE);
select * from users;