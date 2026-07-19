# 05 — Implementation Plan (Ruta Crítica)
> **Proyecto:** MIS - Management Information System  
> **Documentación Activa:** [01_PRD](01_PRD.md) | [02_UI_UX_APP_FLOW](02_UI_UX_APP_FLOW.md) | [03_TRD](03_TRD.md) | [04_BACKEND_SCHEMA](Backend/04_BACKEND_SCHEMA.md) | [06_FIGMA_UX_KIT_GUIDE](FIGMA/06_FIGMA_UX_KIT_GUIDE.md) | [07_DATABASE_SCHEMA](Backend/07_DATABASE_SCHEMA_v2.2.sql) | [08_GUIA_SISTEMAS_HIJOS](08_GUIA_SISTEMAS_HIJOS.md)  
> **Versión:** 2.0.0  
> **Fecha:** 2026-07-12  
> **Estado:** 🟢 Fases 0–5 completadas (frontend con Fake API) · Fases 6–8 pendientes

---

## Resumen Ejecutivo

Plan de ruta crítica para la construcción del **MIS Host**: shell Angular 22 Zoneless con
Native Federation que centraliza navegación, autenticación MFA, IAM y el registro de
sistemas embebidos. **El frontend del Host está completo** operando contra la Fake API
(contrato 1:1 con el backend real). Quedan pendientes la dockerización (FASE 6), la
construcción del backend Spring Boot (FASE 7) y el primer sistema hijo real (FASE 8).

---

## Convenciones de este Documento

| Símbolo | Significado |
|---|---|
| `[ ]` | Tarea pendiente |
| `[/]` | Tarea en progreso / parcial |
| `[x]` | Tarea completada |
| 🔴 | Tarea bloqueante |

---

## ✅ FASE 0 — Scaffolding y Configuración Base 🔴 (COMPLETADA)

- [x] **F0-01** · Workspace Angular 22 standalone (`mis-host`)
- [x] **F0-02** · `@angular-architects/native-federation` como host (`federation.config.mjs`, shareAll singleton + denseChunking)
- [x] **F0-03** · `zone.js` eliminado de polyfills y dependencias
- [x] **F0-04** · `provideZonelessChangeDetection()` en `src/app/app.config.ts`
- [x] **F0-05** · `public/federation.manifest.json` (vacío hasta registrar el primer Remote real)
- [x] **F0-06** · `npm start` levanta el Host en `localhost:4200` sin errores

---

## ✅ FASE 1 — Core Layout (Shell) 🔴 (COMPLETADA)

> La estructura real usa `pages/full-pages/layout/` (no `core/layout/` como se planeó).

- [x] **F1-01** · `app.routes.ts`: `/` → `/admin/dashboard`, `**` → NotFound (lazy)
- [x] **F1-02** · `ShellLayoutComponent` — `pages/full-pages/layout/components/shell-layout/`
- [x] **F1-03** · `HeaderComponent` (Smart) — wordmark `MIS |` + **`p-breadcrumb` derivado de la URL** (HD-01/HD-02 del doc 02) + pill de usuario con dropdown (Perfil / Preferencias / Cerrar sesión)
- [x] **F1-04** · `SidebarComponent` (Smart, Col 1) + `SidebarNavPanelComponent` (Col 2) — menú del Host con "Mi espacio" y sección **Accesos [Admin]** (SB-04)
- [x] **F1-05** · `ShellLayoutComponent` como ruta padre `/admin` con children + `authGuard`
- [x] **F1-06** · `DashboardComponent` — KPIs (Sistemas/Usuarios/Roles/Servidores) + tabla "Estado de Microfrontends Remotos" (Figma dashboard_interaccion.html)
- [x] **F1-07** · `NotFoundComponent` — `pages/full-pages/error/`
- [x] **F1-08** · Navegación básica verificada

---

## ✅ FASE 2 — ShellStateService + Guards 🔴 (COMPLETADA)

- [x] **F2-01** · `ShellStateService` — signals privados + `asReadonly()`: `usuarioActivo`, `menuItemActivo`, `sidebarIconActivo`; computed `esAdmin`, `esAdminSistema`, `subsistemas`, `inicialesUsuario`
- [x] **F2-02** · Interfaces `UsuarioActivo`, `MenuItemActivo` (en el mismo service)
- [x] **F2-03** · Sidebar lee el ícono/menú activo del service
- [x] **F2-04** · Usuario real vía login MFA (reemplazó al usuario de prueba del plan)
- [x] **F2-05** · Computed reaccionan al cambio de rol
- [x] **F2-06** · `roleGuard(rol)` con **jerarquía** admin-sistema > admin-general > supervisor-area; al denegar emite **toast warn** y redirige a `/admin/dashboard`

---

## ✅ FASE 2B — Autenticación MFA (CA-07) (COMPLETADA — no estaba en el plan v1)

- [x] `AuthService`: `login()` → desafío `{mfaToken, email}` (sin sesión) · `verificarOtp()` → `{token, usuario}` · `restaurarSesion()` desde `sessionStorage` (`mis.sesion`) vía `provideAppInitializer`
- [x] `LoginComponent` en 2 pasos: credenciales (Signal Forms) → grid OTP de 6 dígitos con expiración 03:00, email enmascarado y pegado del código (Figma login.html)
- [x] `authInterceptor` adjunta `Authorization` + `X-User-Role`
- [x] Fake API: `POST /auth/login` y `POST /auth/verificar-otp` (OTP demo `123456`)

---

## ✅ FASE 3 — Módulo Gestión de Sistemas (MFEs) (COMPLETADA)

- [x] **F3-01** · Modelos `Sistema`, `Seccion`, `Subseccion`, `Modulo`, `SistemaResumen`, `PermisoRolSistema`
- [x] **F3-02** · `SistemasService` (signals `isLoading/error/sistemas` + computed `totalActivos`)
- [x] **F3-03** · `sistemas.routes.ts` (lista / nuevo / :id / :id/editar) bajo `roleGuard('admin-sistema')`
- [x] **F3-04** · `SistemasListComponent` — tabla en `p-card` con header (título + "Nuevo Sistema")
- [x] **F3-05** · `SistemaFormComponent` — pestañas SelectButton `Identificación` / `Despliegue`
- [x] **F3-06** · `SistemaDetalleComponent` — pestañas `Información` / `Estructura` (editor del árbol) / `Permisos` (módulos por rol)

---

## ✅ FASE 3B — Feature Accesos / IAM 🔴 (COMPLETADA)

- [x] **F3B-01..02** · Modelos IAM + `AccesosService`
- [x] **F3B-03** · `accesos.routes.ts` completo (usuarios y roles con nuevo/:id/editar)
- [x] **F3B-04** · `AccesosShellComponent` — KPIs + tarjetas de acceso a listados
- [x] **F3B-05..06** · `UsuariosListComponent` (búsqueda + paginación + toggle estado) y `UsuarioFormComponent` (pestañas `Información General` / `Roles y Sistemas`)
- [x] **F3B-07..08** · `RolesListComponent` y `RolFormComponent` (slug auto-generado, remotes dinámicos)
- [x] `RolDetalleComponent` — pestañas `Detalle` / `Sistemas` / `Usuarios`
- [x] **F3B-09** · Mensajería global: `ToastService` (fachada sobre `MessageService`) + `<p-toast>` en el root; `AccessDeniedComponent` en `shared/ui`
- [x] **F3B-10..11** · Ruta `/admin/accesos` con `roleGuard('admin-sistema')` verificada

---

## ✅ FASE 4 — Native Federation: Carga Dinámica de Remotes 🔴 (COMPLETADA)

- [x] **F4-01..02** · `RemoteSkeletonComponent` y `RemoteErrorComponent` (con Reintentar)
- [x] **F4-03** · `RemoteWrapperComponent` — 3 estados (loading/loaded/error); **recarga con `effect` sobre `remoteName()`** y descarta respuestas tardías al cambiar de remote
- [x] **F4-04** · Ruta `/admin/:remoteName/**` — **componentless + comodín** (deep-linking; el hijo hereda el parámetro)
- [x] **F4-05..06** · `initFederation('/federation.manifest.json')` en `main.ts` → `bootstrap.ts`
- [/] **F4-07** · Prueba con un Remote real en `localhost:4201` — pendiente de que exista el primer sistema hijo (FASE 8)
- [x] **F4-08** · Fallo de Remote muestra `RemoteErrorComponent` sin romper la shell

---

## ✅ FASE 5 — Shared UI y Polish (COMPLETADA)

- [x] **F5-01** · `InlineErrorComponent` (+ `EmptyStateComponent`, `ListSkeletonComponent`)
- [x] **F5-02** · Confirmaciones destructivas con **`p-dialog`** inline en las listas (sustituye al ConfirmDialogComponent planificado — MSG-03 del doc 02)
- [x] **F5-03** · `styles.css` + `tokens.css`: tokens light/dark completos incl. semánticos (`--mis-success/warning/danger` + light) y `--mis-border-strong`; clase global `.mis-card`
- [x] **F5-04** · Aislamiento de módulos verificado (sin imports cruzados de componentes)
- [x] **F5-05** · Build de producción sin `zone.js` en el bundle

---

## ⏳ FASE 6 — Dockerización y CI/CD (PENDIENTE)

- [ ] **F6-01** · `Dockerfile` multi-stage (node build → nginx) — plantilla en TRD §7
- [ ] **F6-02** · `nginx.conf` SPA (redirect a index.html, caché de assets, proxy `/api` → backend)
- [ ] **F6-03** · `.dockerignore`
- [ ] **F6-04** · `docker-compose.yml` dev (Host + backend + postgres)
- [ ] **F6-05** · Verificar `docker build -t mis-host:local .`
- [ ] **F6-06** · Pipeline en Dokploy/Coolify apuntando al registry privado

---

## 🔧 FASE 7 — Backend Real: Spring Boot + PostgreSQL (EN CURSO — API implementada)

> Especificación completa en [04_BACKEND_SCHEMA](Backend/04_BACKEND_SCHEMA.md) y DDL en [07_DATABASE_SCHEMA_v2.2](Backend/07_DATABASE_SCHEMA_v2.2.sql).

- [x] **F7-01** · Scaffold `mis-backend` (Java 21, Spring Boot 4.1, módulos `core/auth/usuarios/roles/sistemas` — reglas BE-01..BE-08)
- [x] **F7-02** · Flyway `V1__baseline.sql` = doc 07 (esquemas iam/sistemas/auth/auditoria, RLS, roles de BD)
- [x] **F7-03** · Módulo `auth`: login + OTP (TTL 3 min, 5 intentos, lockout) + JWT con `jti` en `auth.sesiones`
- [x] **F7-04** · Módulos `usuarios`, `roles` y `sistemas`: endpoints del contrato §4 del doc 04
- [ ] **F7-05** · Auditoría: `SET LOCAL app.usuario_id/app.trace_id` por transacción (pendiente; la bitácora `auditoria.accesos` ya se escribe vía `AccessLogger`)
- [ ] **F7-06** · Conectar el Host: retirar `fakeApiInterceptor` de `app.config.ts` y validar E2E

---

## ⏳ FASE 8 — Primer Sistema Hijo (PENDIENTE — guía lista)

> Guía completa en [08_GUIA_SISTEMAS_HIJOS](08_GUIA_SISTEMAS_HIJOS.md) (ejemplo: `mis-remote-reportes`, puertos 4205/8085).

- [ ] **F8-01** · Repo `mis-remote-reportes` (frontend Remote NF + backend Spring Boot propio + BD propia)
- [ ] **F8-02** · Frontend expone `./Component`; tema `MisTheme`; lee signals readonly del shell
- [ ] **F8-03** · Backend valida JWT del Host (claim `subsistemas` contiene el slug)
- [ ] **F8-04** · Registro: `federation.manifest.json` + alta en `/admin/sistemas` (estructura + permisos por rol)
- [ ] **F8-05** · Validar CA-01..CA-05 con el remote real (carga, skeleton, error elegante, deep-linking)

---

## Resumen de Ruta Crítica

```
FASE 0 ─► FASE 1 ─► FASE 2 (+2B MFA) ─► FASE 3 / 3B / 4 (paralelas) ─► FASE 5   ✅ HECHO
                                                                          │
                                          ┌───────────────────────────────┤
                                          ▼                               ▼
                                   FASE 6 (Docker)              FASE 7 (Backend real)
                                          │                               │
                                          └───────────► FASE 8 (Primer sistema hijo)
```

---

## Criterios de Aceptación vs. Fases

| Criterio | Fase | Estado |
|---|---|:---:|
| CA-01: Carga dinámica de Remote sin reload | FASE 4 | ✅ (validación final con remote real en F8-05) |
| CA-02: Sin iframes (`loadRemoteModule`) | FASE 4 | ✅ |
| CA-03: Sin conflictos de rendimiento (Zoneless) | FASE 0 + 4 | ✅ |
| CA-04: Error elegante cuando un Remote cae | FASE 4 | ✅ |
| CA-05: Skeleton mientras el Remote carga | FASE 4 | ✅ |
| CA-06: IAM con Angular Signal Forms | FASE 3B | ✅ |
| CA-07: Verificación MFA tras el login | FASE 2B | ✅ |
| roleGuard bloquea `/admin/accesos` a otros roles | FASE 2 + 3B | ✅ |
