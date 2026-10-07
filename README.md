# KODE — Backend

API REST del planificador universitario **KODE**. Maneja usuarios/roles, cursos, rúbricas, matrículas, clases, tareas, apuntes, planes de estudio y la integración con IA (Gemini).

- **Stack:** Kotlin · Spring Boot 3.5.6 · Spring Data JPA · Spring Security (JWT) · MapStruct
- **Base de datos:** PostgreSQL (Supabase en producción)
- **IA:** Google Gemini
- **Deploy:** Render (Docker) — https://backend-delta.onrender.com/v1/

---

## Requisitos

- **JDK 17** (Temurin 17 recomendado). JDKs más nuevos (21/25) pueden fallar con las versiones de Gradle/Spring de este proyecto.
- **PostgreSQL** local (para correr fuera de producción).
- Gradle Wrapper incluido (`./gradlew`), no hace falta instalar Gradle.

En Windows (PowerShell), apuntá `JAVA_HOME` al JDK 17 antes de compilar:

```bash
$env:JAVA_HOME = "C:\Users\<usuario>\.jdks\temurin-17.0.20"
```

---

## Cómo correrlo localmente

1. Levantá un PostgreSQL local. Por defecto los perfiles locales esperan:
   - URL: `jdbc:postgresql://localhost:5433/KodeBD`
   - Usuario: `postgres`
   - Contraseña: la configurás vos (ver `src/main/resources/application-initlocal.properties`).

2. Corré la app (perfil por defecto `initlocal`):

```bash
./gradlew bootRun
```

La API queda en `http://localhost:8080/v1/`.
Al arrancar se siembran los roles base (ADMIN, TEACHER, STUDENT) y un usuario de prueba.

### Perfiles de Spring

| Perfil | Uso | `ddl-auto` |
|--------|-----|-----------|
| `initlocal` (default) | Local, mantiene datos | `update` |
| `dev` | Local, recrea el esquema | `create` |
| `render` | Producción (Supabase) | `update` |

Para elegir un perfil:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

---

## Variables de entorno (producción / Render)

| Variable | Descripción |
|----------|-------------|
| `PORT` | Puerto del servidor (Render lo inyecta; default 8080). |
| `SPRING_PROFILES_ACTIVE` | `render` en producción. |
| `GEMINI_API_KEY` | API key de Google Gemini (chat IA e imágenes). Sin ella, la IA responde con error controlado. |

Las credenciales de la base (URL, usuario, contraseña de Supabase) están en `application-render.properties`. No subas contraseñas reales a un repo público.

---

## Endpoints principales

Todos bajo el prefijo de versión `/v1`:

| Recurso | Ruta |
|---------|------|
| Registro | `POST /v1/users/signup` |
| Login | `POST /v1/users/login` |
| Salud (pública) | `GET /v1/unsecure/health` |
| Cursos / rúbricas | `/v1/courses`, `/v1/course-rubrics` |
| Matrículas / clases | `/v1/enrollments`, `/v1/classes` |
| Tareas | `/v1/tasks` |
| Apuntes | `/v1/notes` |
| Planes de estudio | `/v1/studyplans` |
| IA (chat) | `/v1/ia` |
| Procesamiento de imagen | `/v1/imageprocessing` |

La mayoría de endpoints requieren un **JWT** (se obtiene en el login) en el header `Authorization: Bearer <token>`.

---

## Compilar / empaquetar

```bash
./gradlew build          # compila y corre tests
./gradlew bootJar        # genera el .jar ejecutable
```

---

## Estructura

- `Entities.kt` — entidades JPA.
- `DTOs.kt` / `Mappers.kt` — DTOs y mapeo (MapStruct + manual).
- `Services.kt` — lógica de negocio.
- `Webservice.kt` — controladores REST.
- `Configuration.kt` / `Security.kt` — seguridad y beans.
- `GeminiStrategy.kt` / `IAService.kt` — integración con Gemini.
- `DataSeeder.kt` — siembra de roles y usuario de prueba al arrancar.
"# b" 
