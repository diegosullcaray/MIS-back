# 01 — Product Requirements Document (PRD)
> **Proyecto:** MIS - Management Information System  
> **Documentación Activa:** [01_PRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/01_PRD.md) | [02_UI_UX_APP_FLOW](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/02_UI_UX_APP_FLOW.md) | [03_TRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/03_TRD.md) | [04_BACKEND_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/04_BACKEND_SCHEMA.md) | [05_IMPLEMENTATION_PLAN](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/05_IMPLEMENTATION_PLAN.md) | [06_FIGMA_UX_KIT_GUIDE](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/FIGMA/06_FIGMA_UX_KIT_GUIDE.md) | [07_DATABASE_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/07_DATABASE_SCHEMA.sql) | [08_GUIA_SISTEMAS_HIJOS](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/08_GUIA_SISTEMAS_HIJOS.md)  
> **Tipo:** Panel Administrador Centralizador (Micro-Frontend Host + Remotes)  
> **Versión:** 1.2.0  
> **Fecha:** 2026-07-12  

---

## 1. Problema de Negocio

Actualmente, operamos con múltiples subsistemas de gestión empresarial que se encuentran desconectados entre sí, lo que obliga a los usuarios a acceder a diferentes plataformas para realizar sus tareas diarias. Esta fragmentación genera ineficiencias operativas, duplicidad de esfuerzos y dificultades en el seguimiento y control de las operaciones. Además, el desarrollo y despliegue de mejoras se ve obstaculizado por la naturaleza monolítica de los sistemas actuales, lo que limita nuestra agilidad para responder a las necesidades del negocio y mantenernos competitivos en el mercado.

- Fricción operativa: los usuarios deben cambiar de contexto entre aplicaciones.
- Duplicación de lógica de autenticación, navegación y estilos.
- Despliegues monolíticos que bloquean la entrega continua de mejoras en subsistemas específicos.

---

## 2. Objetivo y Valor de Negocio

> **"Desarrollar un panel administrador centralizador (tipo sistema NES) que unifique la gestión empresarial."**

| Pilar | Descripción |
|---|---|
| **Centralización** | Un único punto de entrada (`Host`) para todos los subsistemas de gestión. |
| **Escalabilidad** | Cada subsistema (`Remote`) se desarrolla, versiona y despliega de forma completamente independiente. |
| **Continuidad** | El portal central (`Host`) permanece activo aunque un subsistema esté en mantenimiento o falle. |
| **Transparencia** | Los subsistemas se embeben dentro del shell sin que el usuario perciba el cambio de contexto. |

### Valor Principal

El sistema **Host administra la estructura principal y seguridad**, mientras **embebe otros subsistemas (Remotes) de forma transparente**. Esto permite:

1. Escalar el desarrollo asignando equipos dedicados por subsistema.
2. Realizar despliegues independientes sin afectar el portal central.
3. Gestionar el estado compartido mínimo (usuario activo, remotes cargados) a través de un contrato de Signals bien definido.

---

## 3. Alcance del MVP

### ✅ Dentro del Alcance

- **Shell Host Angular**: estructura visual de 3 columnas (Col 1: tira de sistemas azul, Col 2: panel de rutas gris, contenido principal).
- **Carga dinámica de Remotes**: integración de al menos un subsistema embebido a través de Native Federation.
- **Estado compartido mínimo**: servicio Singleton con Signals de solo lectura para comunicar Host ↔ Remote.
- **Estados de UI**: loading skeletons (`@defer`), estado de error elegante cuando un Remote no está disponible.
- **Autenticación con MFA**: formulario de login con contraseña seguido de un paso de verificación de identidad OTP de 6 dígitos.
- **Gestión de Accesos (IAM)**: módulo administrativo para crear/editar/desactivar usuarios, definir roles y asignar permisos de acceso a subsistemas (Remotes) por rol.
- **Gestión de Sistemas**: módulo administrativo para registrar los sistemas embebibles (Remotes), definir su **estructura jerárquica** (`Sistema → Secciones → Subsecciones → Módulos`) y configurar los **permisos de cada rol a nivel de módulo**.
- **Diseño Segmentado (SelectButton)**: pantallas de edición y detalle subdivididas en paneles lógicos mediante el control segmentado de PrimeNG para evitar la saturación visual.

### ❌ Fuera del Alcance (MVP)

- Autenticación/SSO externa (MFA local en el MVP).
- Modo oscuro / temas personalizables.
- Auditoría y trazabilidad detallada de acciones por usuario (segunda fase).

---

## 4. Usuarios Objetivo

| Perfil | Descripción | Capacidades Clave |
|---|---|---|
| **Administrador del Sistema** | Perfil técnico-administrativo responsable de la configuración global de la plataforma MIS. | Crear / editar / desactivar **usuarios**, definir y asignar **roles**, habilitar o restringir el acceso a **subsistemas (Remotes)** por rol, y gestionar los **sistemas** registrados. |
| **Administrador General** | Perfil operativo con acceso completo a todos los módulos habilitados para su organización. | Accede al 100% del Host y todos los Remotes asignados a su rol. No puede modificar la configuración de usuarios ni permisos. |
| **Supervisor de Área** | Perfil operativo con acceso restringido a los subsistemas de su área funcional. | Accede únicamente a los Remotes que el Administrador del Sistema le haya asignado. |

### 4.1 Matriz de Permisos por Módulo

| Módulo / Acción | Administrador del Sistema | Administrador General | Supervisor de Área |
|---|:---:|:---:|:---:|
| Gestión de Usuarios | ✅ CRUD | ❌ | ❌ |
| Gestión de Roles | ✅ CRUD | ❌ | ❌ |
| Gestión de Sistemas (registro, estructura y permisos) | ✅ CRUD | ❌ | ❌ |
| Asignación de Acceso a Subsistemas | ✅ | ❌ | ❌ |
| Mi espacio (Dashboard del Host) | ✅ | ✅ | ✅ |
| Subsistemas / Remotes | ✅ Todos | ✅ Asignados | ✅ Asignados |

---

## 5. Reglas de Negocio Críticas

> Estas reglas son **no negociables** y deben implementarse en la primera versión.

| # | Regla |
|---|---|
| RN-01 | El **Host es el responsable absoluto** de la navegación global y el marco visual. Ningún Remote puede alterar el header, sidebar o la URL base. |
| RN-02 | Los subsistemas embebidos deben funcionar de manera **aislada**: no tienen dependencias rígidas con el Host. |
| RN-03 | La comunicación Host ↔ Remote se realiza **únicamente** a través de un estado global ligero (Signals de solo lectura). |
| RN-04 | **Se prohíbe el uso de `iframes`** para embeber subsistemas. Solo se permite `loadRemoteModule` de Native Federation. |
| RN-05 | La carga de un Remote **no debe recargar el navegador** ni generar conflictos de rendimiento en la interfaz. |
| RN-06 | Si un Remote está caído o inaccesible, el sistema debe mostrar un **estado de error elegante** sin interrumpir el funcionamiento del Host. |
| RN-07 | Los formularios de edición de accesos se presentan divididos por pestañas con `SelectButton` para facilitar la usabilidad en pantallas pequeñas. |

---

## 6. Criterios de Aceptación

| ID | Criterio | Prioridad |
|---|---|---|
| CA-01 | El sistema administrador **carga dinámicamente al menos un subsistema embebido** sin recargar el navegador. | 🔴 Crítico |
| CA-02 | La carga del Remote **no utiliza iframes**. | 🔴 Crítico |
| CA-03 | No se generan conflictos de rendimiento al navegar entre vistas propias del Host y vistas de un Remote. | 🔴 Crítico |
| CA-04 | Cuando un Remote no está disponible, se renderiza un componente de error contextual sin romper la shell. | 🟠 Alto |
| CA-05 | Mientras el Remote carga, se muestra un skeleton de carga coherente con el diseño del sistema. | 🟠 Alto |
| CA-06 | El módulo de Gestión de Usuarios y Roles utiliza Angular Signal Forms para la reactividad en formularios. | 🟠 Alto |
| CA-07 | Se implementa la verificación de identidad MFA tras el inicio de sesión. | 🟠 Alto |

---

## 7. Métricas de Éxito

| Métrica | Meta |
|---|---|
| Tiempo de carga inicial del Host (LCP) | < 2.5 s |
| Tiempo de hidratación de un Remote | < 1.5 s |
| Disponibilidad del Host independiente de un Remote caído | 100% |
| Cobertura de criterios de aceptación en primera entrega | ≥ 80% |

---

## 8. Dependencias y Restricciones

- **Angular 21/22** como framework base (última versión estable disponible).
- **`@angular-architects/native-federation`** como mecanismo de federación (sin Webpack).
- Modo **Zoneless** obligatorio (`provideZonelessChangeDetection`).
- **Spring Boot 3 + PostgreSQL 16** para el backend del Host (ver [04_BACKEND_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/04_BACKEND_SCHEMA.md)); cada Remote lleva su backend propio (ver [08_GUIA_SISTEMAS_HIJOS](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/08_GUIA_SISTEMAS_HIJOS.md)).
- Cada subsistema se desplegará en su propia **imagen Docker** en un registry privado.
- Orquestación de contenedores vía **Dokploy** o **Coolify**.

---

## 9. Estado de Implementación (2026-07-12)

| Alcance del MVP | Estado | Notas |
|---|:---:|---|
| Shell Host de 3 columnas (sidebar navy + panel + contenido) | ✅ | `pages/full-pages/layout/` — breadcrumb PrimeNG en el header |
| Autenticación con MFA (login + OTP 6 dígitos) | ✅ | 2 pasos en `LoginComponent`; OTP demo Fake API: `123456` |
| Gestión de Accesos IAM (usuarios, roles) | ✅ | CRUD completo con Signal Forms, protegido por `roleGuard('admin-sistema')` |
| Gestión de Sistemas (registro, estructura, permisos) | ✅ | Detalle con pestañas Información / Estructura / Permisos |
| Carga dinámica de Remotes + estados loading/error | ✅ | `RemoteWrapperComponent`; soporta deep-linking `/admin/{slug}/**` |
| Diseño segmentado (SelectButton) y vistas en cards | ✅ | Todas las gestiones en `p-card` (header + body) a ancho completo |
| Backend real (Spring Boot) + BD PostgreSQL | 📄 Especificado | Docs 04 y 07 listos para construir; hoy la Fake API sirve el contrato 1:1 |
| Dockerización y CI/CD | ⏳ Pendiente | FASE 6 del plan |

> Fuera de alcance (confirmado): módulo de Catálogos (retirado del producto), SSO externo, modo oscuro.
