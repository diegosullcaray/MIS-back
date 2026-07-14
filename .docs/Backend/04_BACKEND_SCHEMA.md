# 04 — Backend Schema (Spring Boot — API, Arquitectura y Datos)
> **Proyecto:** MIS - Management Information System
> **Documentación Activa:** [01_PRD](../01_PRD.md) | [02_UI_UX_APP_FLOW](../02_UI_UX_APP_FLOW.md) | [03_TRD](../03_TRD.md) | [04_BACKEND_SCHEMA](04_BACKEND_SCHEMA.md) | [05_IMPLEMENTATION_PLAN](../05_IMPLEMENTATION_PLAN.md) | [07_DATABASE_SCHEMA](07_DATABASE_SCHEMA.sql)
> **Versión:** 3.0.0
> **Fecha:** 2026-07-13
> **Estado:** 🟢 Alineado al frontend actual (la Fake API implementa este contrato) y a la estructura real del código (`src/main/java/pe/confianza/mis/`)

---

## 1. Stack Tecnológico del Backend

| Capa | Tecnología | Versión | Notas |
|---|---|---|---|
| Lenguaje | Java | 21 LTS | Records para DTOs, virtual threads habilitados |
| Framework | **Spring Boot** | 4.1 | starters: webmvc, validation, actuator, flyway |
| Seguridad | Spring Security | 7.x | JWT stateless + MFA OTP (CA-07) |
| Persistencia | Spring Data JPA (Hibernate) | — | Un repositorio por agregado |
| Base de datos | **PostgreSQL** | 16+ | DDL en [07_DATABASE_SCHEMA.sql](07_DATABASE_SCHEMA.sql) |
| Migraciones | Flyway | — | `V1__baseline.sql` (= script 07) · `V2__security_hardening` · `V3__seed_dev` |
| Mapeo DTO | MapStruct | 1.6.3 | Entity → DTO en compile-time (`presentation/mapper/`, processor en el pom) |
| Documentación | springdoc-openapi | 3.0 | Swagger UI solo en perfil `dev` |
| Contenedores | Docker (multi-stage) | — | JRE 21 alpine, orquestado con Dokploy/Coolify |

> El frontend consume la API con prefijo **`/api/v1`**. La Fake API del Host
> (`src/app/core/fake-api/`) implementa exactamente este contrato: al desplegar el
> backend real solo se retira `fakeApiInterceptor` de `app.config.ts` (ver §8).

> ⚠️ **Regla de Aislamiento (RN-02/RN-03):** los Remotes nunca llaman APIs del Host ni
> viceversa; cada Remote tiene su propio backend. Este documento cubre únicamente el
> backend del **Host** (auth + IAM + registro de sistemas).

---

## 2. Arquitectura — Monolito Modular (escalable, robusto, extraíble a microservicios)

Un **monolito modular** organizado por *bounded contexts*: `auth`, `usuarios` y `roles`
(IAM) y `sistemas`, más el núcleo transversal `core`. Cada módulo es autocontenido —
presentación, aplicación, dominio e infraestructura propias — y solo se comunica con
otros módulos a través de sus servicios públicos (puertos), nunca tocando repositorios
ajenos. Así, cualquier módulo puede extraerse a microservicio sin reescritura (el mismo
criterio de independencia que los Remotes del frontend).

### 2.1 Anatomía estándar de un módulo

Todo módulo respeta exactamente esta jerarquía de paquetes (sin excepciones):

```
<módulo>/
├── presentation/            ← Exposición (cero lógica de negocio)
│   ├── controller/            Endpoints REST: @Valid + delegar al service + devolver DTO
│   ├── dto/                   Records Request/Response (Java Records, Instant UTC)
│   └── mapper/                Interfaces MapStruct (Entity → DTO en compile-time)
├── application/             ← Orquestación y casos de uso
│   ├── service/               Interfaces: casos de uso + puertos públicos cross-módulo.
│   │                          ÚNICA capa que otro módulo puede inyectar (BE-01).
│   └── impl/                  Implementaciones @Service @Transactional
├── domain/                  ← Negocio core (nunca cruza la frontera del módulo)
│   ├── entity/                Entidades JPA puras
│   └── repository/            Interfaces Spring Data JPA
└── infrastructure/          ← Detalles externos (APIs, correo/SMS, Specifications)
```

### 2.2 Árbol real del proyecto

```
mis-backend/
├── src/main/java/pe/confianza/mis/
│   ├── MisBackendApplication.java
│   │
│   ├── core/                            ← Núcleo transversal (sin lógica de negocio)
│   │   ├── config/                        WebConfig (CORS), OpenApiConfig
│   │   ├── security/                      SecurityConfig, JwtAuthFilter, JwtProvider,
│   │   │                                  JwtProperties, AuthenticatedUser, CurrentUser
│   │   ├── exception/                     ApiError, ApiException + jerarquía (NotFound,
│   │   │                                  Conflict, Validation, Forbidden, Unauthorized),
│   │   │                                  GlobalExceptionHandler
│   │   ├── persistence/                   AuditableEntity (creado_en / actualizado_en)
│   │   └── web/                           PageResponse<T> (contrato de paginación §3.2)
│   │
│   ├── auth/                            ← Módulo: Autenticación + MFA
│   │   ├── presentation/
│   │   │   ├── controller/  AuthController (login, verificar-otp)
│   │   │   └── dto/         AuthDtos: LoginRequest, MfaChallengeResponse,
│   │   │                              VerificarOtpRequest, LoginResponse, UsuarioActivoDto
│   │   ├── application/
│   │   │   ├── service/     AuthService, OtpService · puertos: IdentityVerifier, OtpSender
│   │   │   └── impl/        AuthServiceImpl, OtpServiceImpl, MfaTokenProvider,
│   │   │                    AccessLogger, AuthProperties
│   │   ├── domain/
│   │   │   ├── entity/      OtpDesafio, Sesion
│   │   │   └── repository/  OtpDesafioRepository, SesionRepository
│   │   └── infrastructure/  PasswordIdentityVerifier (dev), GmailIdentityVerifier (prod),
│   │                        LogOtpSender (dev; en prod: correo corporativo)
│   │
│   ├── usuarios/                        ← Módulo: IAM — Usuarios y Credenciales
│   │   ├── presentation/
│   │   │   ├── controller/  UsuarioController
│   │   │   ├── dto/         UsuarioDtos: UsuarioDto/Request, CambiarEstadoRequest
│   │   │   └── mapper/      UsuarioMapper (MapStruct)
│   │   ├── application/
│   │   │   ├── service/     UsuarioService · puertos: UsuarioDirectory,
│   │   │   │                PasswordAuthenticator, UsuarioLookup
│   │   │   └── impl/        UsuarioServiceImpl, UsuarioAuthAdapter, UsuarioLookupImpl
│   │   ├── domain/
│   │   │   ├── entity/      Usuario (rol_id como UUID plano — BE-01), Credencial
│   │   │   └── repository/  UsuarioRepository, CredencialRepository
│   │   └── infrastructure/  (reservado: Specifications de búsqueda/paginación)
│   │
│   ├── roles/                           ← Módulo: IAM — Roles
│   │   ├── presentation/
│   │   │   ├── controller/  RolController
│   │   │   ├── dto/         RolDtos: RolDto/Request
│   │   │   └── mapper/      RolMapper (MapStruct)
│   │   ├── application/
│   │   │   ├── service/     RolService · puerto: RolLookup (existe, contar, RolRef)
│   │   │   └── impl/        RolServiceImpl
│   │   ├── domain/
│   │   │   ├── entity/      Rol
│   │   │   └── repository/  RolRepository
│   │   └── infrastructure/  (reservado)
│   │
│   └── sistemas/                        ← Módulo: Registro de MFEs + Estructura + Permisos
│       ├── presentation/
│       │   ├── controller/  SistemaController
│       │   ├── dto/         SistemaDtos: SistemaDto/Resumen/Request, Seccion/Subseccion/
│       │   │                Modulo (Dto + Input), PermisoRolSistemaDto, GuardarPermisos
│       │   └── mapper/      SistemaMapper (MapStruct: árbol completo → DTO)
│       ├── application/
│       │   ├── service/     SistemaService · puerto: SistemaLookup
│       │   └── impl/        SistemaServiceImpl
│       ├── domain/
│       │   ├── entity/      Sistema, Seccion, Subseccion, Modulo, PermisoRolModulo
│       │   └── repository/  SistemaRepository, ModuloRepository, PermisoRolModuloRepository
│       └── infrastructure/  (reservado)
│
├── src/main/resources/
│   ├── application.yml                  perfiles: dev / prod
│   └── db/migration/                    V1__baseline.sql (= script 07) + V2, V3 (seed dev)
└── pom.xml                              incluye MapStruct (processor en compile-time)
```

### 2.3 Comunicación entre módulos (puertos)

Los módulos se hablan **solo** por interfaces publicadas en `application/service/`:

| Puerto | Lo publica | Lo consume | Para qué |
|---|---|---|---|
| `SistemaLookup` | `sistemas` | `usuarios`, `roles` | Resolver slugs ↔ ids de subsistemas. |
| `RolLookup` | `roles` | `sistemas`, `usuarios` | Validar roles, contar asignaciones y exponer `RolRef` (slug + subsistemas). |
| `UsuarioLookup` | `usuarios` | `roles` | Verificar si un rol tiene usuarios asignados (borrado seguro). |
| `UsuarioDirectory` | `usuarios` | `auth` | Vista `AuthUser` para el JWT y la sesión. |
| `PasswordAuthenticator` | `usuarios` | `auth` | Autenticación BCrypt + lockout (dev). |

### Reglas de modularidad (no negociables)

| # | Regla |
|---|---|
| BE-01 | Un módulo **no importa** `domain/` ni `infrastructure/` de otro módulo; solo interfaces de su `application/service/` (puertos públicos). Verificable con ArchUnit. |
| BE-02 | Los controllers solo orquestan: `@Valid` + delegar al service + devolver DTO. Cero lógica de negocio. Inyectan **interfaces** de `application/service/`, nunca las `*Impl`. |
| BE-03 | Las entidades JPA **nunca** cruzan la frontera del módulo: todo intercambio usa DTOs mapeados por **MapStruct** (`presentation/mapper/`, generado en compile-time). Los datos que requieren lógica cross-módulo (p. ej. slug del rol, slugs de subsistemas) los resuelve el service vía puertos y se pasan al mapper como parámetro. Las FKs a agregados de otro módulo se modelan como **UUID plano** (p. ej. `Usuario.rolId`, `PermisoRolModulo.rolId`). |
| BE-04 | Errores de negocio → excepciones propias de `core/exception` (`NotFoundException`, `ConflictException`, `ValidationException`, `ForbiddenException`, `UnauthorizedException`) traducidas por `GlobalExceptionHandler` al formato `ApiError` (§3.1). |
| BE-05 | Toda tabla lleva auditoría mínima (`creado_en`, `actualizado_en`) heredada de `AuditableEntity`. |
| BE-06 | Cambios de esquema solo via Flyway; Hibernate con `ddl-auto: validate` en todos los perfiles. |
| BE-07 | Operaciones que tocan varios agregados (p. ej. reemplazar estructura + depurar permisos) van en **una sola transacción** de servicio. |
| BE-08 | Toda clase de `application/` se declara como **interfaz en `service/` + implementación en `impl/`**; DTOs siempre como Java Records; fechas siempre `Instant` (ISO-8601 UTC). |

---

## 3. Convenciones Transversales del API

### 3.1 Formato de error (`ApiError`) — el que ya espera el frontend

```json
{ "status": 409, "message": "No se puede eliminar: el rol 'X' tiene usuarios asignados.", "timestamp": "2026-07-12T10:00:00Z" }
```

### 3.2 Paginación (`PageResponse<T>`) — coincide con `acceso.model.ts`

```json
{ "page": 1, "pageSize": 20, "total": 57, "items": [ ... ] }
```

### 3.3 Seguridad

- **JWT stateless** (`Authorization: Bearer <token>`). Claims: `sub` (usuarioId), `rol` (slug), `subsistemas` (string[]).
- Autorización con `@PreAuthorize`; jerarquía `admin-sistema > admin-general > supervisor-area` vía `RoleHierarchy` (paridad con `role.guard.ts`).
- Contraseñas con **BCrypt** (cost ≥ 10). El hash jamás viaja en un DTO.
- **MFA obligatorio** (CA-07): el login con credenciales no emite sesión; emite un desafío OTP (§6).

---

## 4. Contrato de Endpoints

> Contrato 1:1 con la Fake API actual. Los códigos de error indicados ya se manejan
> en los servicios Angular (`AuthService`, `AccesosService`, `SistemasService`).

### 4.1 Módulo `auth`

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/v1/auth/login` | público | Valida credenciales. **No emite token de sesión**: genera OTP (TTL 3 min) y responde `{ mfaRequerido: true, mfaToken, email }`. Errores: `401` credenciales inválidas · `403` usuario desactivado. |
| `POST /api/v1/auth/verificar-otp` | público | Body `{ mfaToken, otp }`. Valida el código de 6 dígitos (máx. 5 intentos). Responde `{ token, usuario }`. Errores: `401` OTP incorrecto o desafío expirado. |

### 4.2 Módulo `usuarios`

| Método y ruta | Rol mínimo | Descripción |
|---|---|---|
| `GET /api/v1/usuarios?page&pageSize&q&activo` | admin-sistema | Listado paginado; `q` busca en nombre/email; `activo` filtra por estado. |
| `GET /api/v1/usuarios/{id}` | admin-sistema | Detalle. `404` si no existe. |
| `POST /api/v1/usuarios` | admin-sistema | Crea usuario. `400` campos requeridos · `409` email duplicado. |
| `PUT /api/v1/usuarios/{id}` | admin-sistema | Actualiza nombre/email/rol/subsistemas; `password` opcional. |
| `PATCH /api/v1/usuarios/{id}/estado` | admin-sistema | Body `{ activo: boolean }`. Activa/desactiva la cuenta. |

### 4.3 Módulo `roles`

| Método y ruta | Rol mínimo | Descripción |
|---|---|---|
| `GET /api/v1/roles` | autenticado | Lista de roles (la consumen selects del Host). |
| `GET /api/v1/roles/{id}` | autenticado | Detalle. |
| `GET /api/v1/roles/{id}/usuarios` | admin-sistema | Usuarios vinculados al rol. |
| `GET /api/v1/roles/{id}/permisos` | admin-sistema | `PermisoRolSistema[]` del rol. |
| `POST /api/v1/roles` | admin-sistema | `400` nombre/slug requeridos · `409` slug duplicado. |
| `PUT /api/v1/roles/{id}` | admin-sistema | El slug es inmutable tras la creación. |
| `DELETE /api/v1/roles/{id}` | admin-sistema | `409` si el rol tiene usuarios asignados. |

### 4.4 Módulo `sistemas`

| Método y ruta | Rol mínimo | Descripción |
|---|---|---|
| `GET /api/v1/sistemas` | autenticado | `SistemaResumen[]` (contadores agregados, sin árbol). Alimenta sidebar y dashboard. |
| `GET /api/v1/sistemas/{idOSlug}` | autenticado | `Sistema` completo con árbol Secciones → Subsecciones → Módulos. |
| `POST /api/v1/sistemas` | admin-sistema | Registra un Remote. `409` slug duplicado. |
| `PUT /api/v1/sistemas/{id}` | admin-sistema | Actualiza datos generales (la estructura tiene su propio endpoint). |
| `DELETE /api/v1/sistemas/{id}` | admin-sistema | `409` si está asignado a algún rol. Cascada sobre estructura y permisos. |
| `PUT /api/v1/sistemas/{id}/estructura` | admin-sistema | Reemplaza el árbol completo (`Seccion[]`) y depura permisos huérfanos en la misma transacción (BE-07). |
| `GET /api/v1/sistemas/{id}/permisos` | admin-sistema | `PermisoRolSistema[]` del sistema. |
| `PUT /api/v1/sistemas/{id}/permisos/{rolId}` | admin-sistema | Body `{ modulos: string[] }`. Upsert de los permisos del rol en el sistema. |

### 4.5 DTOs de referencia

Los DTOs Java replican los modelos del frontend (fuente de verdad del contrato):
`Usuario`, `UsuarioRequest`, `Rol`, `RolRequest`, `PageResponse<T>` (en
`acceso.model.ts`) y `Sistema`, `SistemaResumen`, `SistemaRequest`, `Seccion`,
`Subseccion`, `Modulo`, `PermisoRolSistema` (en `sistema.model.ts`).
Fechas siempre **ISO-8601 UTC** (`Instant` + Jackson).

---

## 5. Modelo de Datos (v2.0)

El DDL completo (PostgreSQL 16) vive en **[07_DATABASE_SCHEMA.sql](07_DATABASE_SCHEMA.sql)** — es también la migración baseline de Flyway. La BD se organiza en **4 esquemas** que espejan los módulos del backend (§2): `iam`, `sistemas`, `auth` y `auditoria` — cada esquema es la costura natural si un módulo se extrae a microservicio.

```
┌── iam ─────────────────────────┐   ┌── sistemas ───────────────────────────┐
│ roles ────< usuarios           │   │ sistemas ─< secciones ─< subsecciones │
│   │            │ 1:1           │   │                              │        │
│   │            └─ credenciales │   │                           modulos     │
│   ├─(rol_sistema)──────────────┼───►  ▲                            ▲       │
│   │  usuarios                  │   │  │                            │       │
│   │    └─(usuario_sistema)─────┼───┼──┘                            │       │
│   └─(permiso_rol_modulo)───────┼───┼───────────────────────────────┘       │
└────────────────────────────────┘   └────────────────────────────────────────┘
┌── auth ────────────────────────┐   ┌── auditoria (append-only, RLS) ───────┐
│ otp_desafios · sesiones (jti)  │   │ eventos (particionada/mes) · accesos  │
└────────────────────────────────┘   └────────────────────────────────────────┘
```

| Esquema.Tabla | Propósito |
|---|---|
| `iam.roles` | Perfiles (`admin-sistema`, `admin-general`, `supervisor-area` + personalizados). |
| `iam.usuarios` | Perfil de la cuenta (email `citext` único, FK a rol, flag `activo`). **Sin secretos.** |
| `iam.credenciales` | 1:1 con usuario: `password_hash`, lockout (`intentos_fallidos`, `bloqueada_hasta`), rotación. |
| `sistemas.sistemas` | Remotes registrados (slug = nombre en `federation.manifest.json`, estado como ENUM). |
| `sistemas.secciones/subsecciones/modulos` | Árbol jerárquico (cascada al eliminar, orden explícito). |
| `iam.rol_sistema` / `iam.usuario_sistema` | Subsistemas habilitados por rol / override por usuario. |
| `iam.permiso_rol_modulo` | Permisos a nivel de módulo; `PermisoRolSistema` se deriva agrupando por sistema. |
| `auth.otp_desafios` | Desafíos MFA: hash del código, TTL 3 min, máx. 5 intentos, un solo uso. |
| `auth.sesiones` | JWTs emitidos (claim `jti`) → revocación inmediata sin esperar expiración. |
| `auditoria.eventos` | Trail de cambios (JSONB antes/después, actor, trace_id), **particionada por mes**, índices BRIN. |
| `auditoria.accesos` | Bitácora de seguridad: logins, OTP, denegaciones, revocaciones (con IP y user-agent). |

### 5.1 Patrón de seguridad de la BD

- **Mínimo privilegio**: roles `mis_owner` (solo Flyway/DDL), `mis_app` (DML de negocio) y `mis_auditor` (solo lectura de auditoría). `REVOKE ALL ... FROM PUBLIC`.
- **Auditoría infalsificable**: los triggers de auditoría son `SECURITY DEFINER` (la app no tiene INSERT directo en `auditoria.eventos`) y las tablas tienen **Row Level Security sin políticas de UPDATE/DELETE** → append-only real. Los secretos (`password_hash`, `codigo_hash`) se excluyen del JSONB auditado.
- **Contexto de actor por transacción**: Spring setea `SET LOCAL app.usuario_id / app.trace_id`; los triggers lo leen con `current_setting()` para correlacionar cada cambio con el usuario y la petición HTTP.
- **Cuenta con lockout**: `iam.credenciales` acumula `intentos_fallidos` y bloquea con `bloqueada_hasta` — el backend registra cada intento en `auditoria.accesos`.
- **Sin redundancias**: dominios `dom_slug` / `dom_email` (una sola definición de formato), un único trigger de `actualizado_en`, vistas sin subconsultas correlacionadas (`sistemas.v_sistemas_resumen`, `iam.v_usuarios`).

---

## 6. Flujo MFA (secuencia)

```
Angular Host                 Spring Boot                       PostgreSQL
    │  POST /auth/login           │                                 │
    ├─────────────────────────────►  valida credenciales + lockout  │
    │                             ├── INSERT auth.otp_desafios ─────►
    │                             ├── INSERT auditoria.accesos ─────►  (login_ok / login_fallido)
    │  ◄── { mfaToken, email } ───┤                                 │
    │                             │                                 │
    │  POST /auth/verificar-otp   │                                 │
    ├─────────────────────────────►  valida otp + intentos ≤ 5      │
    │                             ├── UPDATE usado_en ──────────────►
    │                             ├── INSERT auth.sesiones (jti) ───►
    │                             ├── INSERT auditoria.accesos ─────►  (otp_ok / otp_fallido)
    │  ◄── { token(JWT), usuario }┤                                 │
```

- En `dev`, el OTP se escribe en el log del servidor (paridad con el `123456` de la Fake API).
- En `prod`, `OtpSender` lo envía por el canal corporativo (email/SMS) — **nunca** viaja en la respuesta HTTP.
- El `mfaToken` es un token opaco firmado con TTL de 3 min, de un solo uso.

---

## 7. Despliegue

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/target/mis-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

| Perfil | Base de datos | OTP | Swagger UI |
|---|---|---|---|
| `dev` | PostgreSQL local (docker-compose junto al Host) | log | ✅ |
| `prod` | PostgreSQL gestionado | email / SMS | ❌ |

Variables de entorno: `SPRING_DATASOURCE_URL / USERNAME / PASSWORD`, `MIS_JWT_SECRET`,
`MIS_CORS_ORIGINS`. La imagen se registra en Dokploy/Coolify junto a los Remotes (03_TRD §7).

---

## 8. Conexión del Frontend al Backend Real

1. Desplegar `mis-backend` detrás del mismo dominio del Host: el nginx del Host hace proxy de `/api` → `mis-backend:8080`.
2. En `src/app/app.config.ts`, retirar `fakeApiInterceptor` de `withInterceptors([...])`.
3. Nada más cambia: servicios, modelos y manejo de errores del Host ya cumplen este contrato.
