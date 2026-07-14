# 02 — UI/UX App Flow (Frontend y Componentes)
> **Proyecto:** MIS - Management Information System  
> **Documentación Activa:** [01_PRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/01_PRD.md) | [02_UI_UX_APP_FLOW](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/02_UI_UX_APP_FLOW.md) | [03_TRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/03_TRD.md) | [04_BACKEND_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/04_BACKEND_SCHEMA.md) | [05_IMPLEMENTATION_PLAN](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/05_IMPLEMENTATION_PLAN.md) | [06_FIGMA_UX_KIT_GUIDE](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/FIGMA/06_FIGMA_UX_KIT_GUIDE.md) | [08_GUIA_SISTEMAS_HIJOS](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/08_GUIA_SISTEMAS_HIJOS.md)  
> **Versión:** 1.5.0  
> **Fecha:** 2026-07-12  
> **Estado:** 🟢 Aprobado — alineado a la implementación

---

## 1. Estructura de Módulos del Host

La arquitectura del Host se organiza en exactamente **3 áreas o módulos funcionales**:

1. **Mi espacio / Dashboard (`/admin/dashboard`)**:
   * Panel principal del Host.
2. **Gestión de Accesos (Usuarios, Roles y sistemas — `/admin/accesos`)**:
   * Módulo unificado para administrar el personal, definir roles y asignar accesos.
3. **Gestión de Sistemas (Configuración de MFEs — `/admin/sistemas`)**:
   * Registro y configuración de los microfrontends remotos.
4. **Gestión de Sistemas Embebidos (`/admin/:remoteName`)**:
   * Envoltorio y cargador dinámico (`RemoteWrapperComponent`) en `core/federation/` que inyecta en tiempo de ejecución los microfrontends remotos (Contabilidad, RRHH, Ventas, Logística).

---

## 2. Mapa de Rutas Real (`app.routes.ts`)

### 2.1 Rutas Internas de la Aplicación

| Ruta | Componente Cargado | Lazy Load | Guard | Acceso / Propósito |
|---|---|---|---|---|
| `/login` | `LoginComponent` | ✅ | — | Autenticación en **2 pasos dentro del mismo componente**: credenciales (Signal Forms) → verificación OTP de 6 dígitos con expiración 03:00 |
| `/admin` | Redirect → `/admin/dashboard` | — | `authGuard` | Todos |
| `/admin/dashboard` | `DashboardComponent` | ✅ | `authGuard` | Todos (Etiqueta: "Mi espacio") |
| `/admin/accesos` | `AccesosShellComponent` | ✅ | `authGuard` + `roleGuard('admin-sistema')` | Admin Sistema (Menú de IAM) |
| `/admin/accesos/usuarios` | `UsuariosListComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/accesos/usuarios/nuevo` | `UsuarioFormComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/accesos/usuarios/:id` | `UsuarioFormComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/accesos/roles` | `RolesListComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/accesos/roles/nuevo` | `RolFormComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/accesos/roles/:id` | `RolDetalleComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/accesos/roles/:id/editar` | `RolFormComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/sistemas` | `SistemasListComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema (Gestión de Sistemas) |
| `/admin/sistemas/nuevo` | `SistemaFormComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/sistemas/:id` | `SistemaDetalleComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/sistemas/:id/editar` | `SistemaFormComponent` | ✅ | `roleGuard('admin-sistema')` | Admin Sistema |
| `/admin/:remoteName/**` | `RemoteWrapperComponent` | ✅ | `authGuard` | Carga de Sistemas Embebidos con **deep-linking**: la ruta es componentless con hijo comodín, cualquier subruta del slug carga el remote (que lee la URL para su vista inicial) |
| `**` | `NotFoundComponent` | ✅ | — | 404 Not Found |

---

## 3. Arquitectura de Componentes del Host

### 3.1 Árbol de Componentes (Standalone)

```
AppComponent (root — Standalone, Zoneless)
├── <p-toast position="top-right"> ← Mensajería global PrimeNG (publicada vía ToastService)
├── LoginComponent (Standalone) ← Paso credenciales + paso OTP MFA en el mismo componente
└── ShellLayoutComponent (Standalone) ← 100% responsable del marco visual de 3 columnas
    ├── HeaderComponent (Standalone, Smart — wordmark "MIS |" + p-breadcrumb derivado de la URL + pill de usuario con dropdown)
    ├── SidebarComponent (Standalone, Smart — Col 1: Tira de sistemas en barra azul con etiquetas cortas)
    └── SidebarNavPanelComponent (Standalone, Smart — Col 2: Menú persistente según sistema activo)
        └── <router-outlet>
            ├── [Área 1: Mi espacio]
            │   └── DashboardComponent (Standalone, Smart)
            ├── [Área 2: Gestión (Usuarios, Roles y Registro de Sistemas)]
            │   ├── UsuariosListComponent (Standalone, Smart)
            │   ├── UsuarioFormComponent (Standalone, Smart)
            │   ├── RolesListComponent (Standalone, Smart)
            │   ├── RolFormComponent (Standalone, Smart)
            │   ├── RolDetalleComponent (Standalone, Smart)
            │   ├── SistemasListComponent (Standalone, Smart)
            │   ├── SistemaFormComponent (Standalone, Smart)
            │   └── SistemaDetalleComponent (Standalone, Smart)
            └── [Área 3: Sistemas Embebidos (Remotes)]
                └── RemoteWrapperComponent (Standalone, Smart)
                    ├── @defer (loading) → RemoteSkeletonComponent
                    ├── @defer (error)   → RemoteErrorComponent
                    └── @defer (main)    → <componente-remoto-inyectado>
```

---

## 4. Estructura y Detalles de los Componentes (Cards + SelectButton)

Para evitar drawers colapsables y layouts sobrecargados, **toda vista de gestión se encapsula en una `p-card` a ancho completo** con dos zonas obligatorias:

- **Header de card** (`pTemplate="header"`): título + descripción a la izquierda y el botón de acción principal a la derecha (Nuevo Usuario / Nuevo Rol / Nuevo Sistema / Editar).
- **Body de card**: buscadores, tablas `p-table`, formularios o pestañas.

Las vistas **no llevan títulos de página ni enlaces "Volver" propios** — el contexto de navegación lo da exclusivamente el breadcrumb del header (regla HD-01, §6). Los formularios y detalles usan el control segmentado `<p-selectButton>` para togglear entre subsecciones de datos:

### 4.1 Gestión de Usuarios: `UsuarioFormComponent`
- **Uso de Formulario:** Permite crear o editar un usuario.
- **Toggles segmentados (`activeTab`):**
  1. **`Información General`**:
     * Código Personal (Sólo lectura si es edición).
     * Nro. Documento (DNI).
     * Nombre Completo.
     * Correo Electrónico (Trabajo).
     * Puesto / Cargo.
     * Unidad / Área.
     * Estado Laboral.
  2. **`Roles y Sistemas`**:
     * Rol del Sistema (dropdown de selección de rol: admin-sistema, supervisor-ventas, etc.).
     * Sistemas Habilitados (check list de remotes en el manifest).
     * Switch de Estado de Acceso (Habilitado/Deshabilitado).

### 4.2 Gestión de Roles: `RolDetalleComponent`
- **Uso de Detalle:** Muestra la configuración completa del rol seleccionado.
- **Toggles segmentados (`tab`):**
  1. **`Información del Rol`**:
     * Código identificador (slug) y Nombre del Rol.
     * Descripción de Funciones.
     * Nivel de Seguridad (dropdown: Nivel 1 Super, Nivel 2 Medio, Nivel 3 Básico).
  2. **`Permisos Asignados`**:
     * Grid de tarjetas de permisos por subsistema (matriz CRUD de microfrontends). Cada tarjeta muestra contadores semánticos individuales de permisos para: *Crear*, *Leer*, *Actualizar* y *Eliminar*.
  3. **`Usuarios Vinculados`**:
     * Tabla con las columnas: *Código Personal*, *Documento*, *Nombre Completo*, *Email*, *Puesto* y *Área*.

### 4.3 Gestión de Sistemas (MFEs): `SistemaDetalleComponent` y `SistemaFormComponent`
- **Uso de Formulario (`SistemaFormComponent`):**
  * Toggles segmentados (`activeTab`):
    1. **`Información de Registro`**: Nombre del Módulo, Ruta Slug (read-only si es edición), y URL del Manifest (remoteEntry).
    2. **`Configuración de Despliegue`**: Proveedor de Infraestructura, Puerto de ejecución local, y Switch de Estado (Online/Offline).
- **Uso de Detalle (`SistemaDetalleComponent`):**
  * Toggles segmentados (`tab`):
    1. **`Información`**: Datos generales del sistema y estado de red.
    2. **`Estructura`**: Árbol jerárquico de Secciones, Subsecciones y Módulos.
    3. **`Permisos`**: Tabla de asignación de accesos por Rol para cada Módulo.

---

## 5. Reglas de Comportamiento del Sidebar

| Regla | Descripción |
|---|---|
| **SB-01** | El primer icono de la barra azul es siempre el sistema de **Inicio** (Host Principal). |
| **SB-02** | Todos los sistemas remotos (remotes) configurados aparecen directamente como iconos con sus etiquetas de texto correspondientes en la barra azul. |
| **SB-03** | La Columna 2 (menú del sistema) es persistente y nunca colapsa. Su contenido se modula al cambiar de sistema. |
| **SB-04** | El Host maneja en su menú las opciones de administración `Gestión de usuarios`, `Gestión de roles` y `Gestión de sistemas` bajo la sección **Accesos [Admin]**; el acceso directo se etiqueta `Mi espacio` y el panel del Host se titula `Host Principal`. |
| **SB-05** | Las acciones de usuario (Perfil y Salir) residen exclusivamente en el dropdown del header en el extremo derecho. |

---

## 6. Reglas de Header y Mensajería

| Regla | Descripción |
|---|---|
| **HD-01** | El **breadcrumb vive únicamente en el header** del layout (`p-breadcrumb` de PrimeNG junto al wordmark `MIS \|`). Se deriva automáticamente de la URL activa; los tramos intermedios son navegables y el ícono 🏠 lleva a `/admin/dashboard`. Ninguna vista renderiza breadcrumbs, títulos de página ni enlaces "Volver" propios. |
| **HD-02** | Para rutas de remotes (`/admin/{slug}/...`), el breadcrumb muestra el **nombre registrado del sistema** y formatea los subsegmentos internos de kebab-case a texto legible (`🏠 / Reportes / Reportes operativos`). |
| **MSG-01** | Toda notificación efímera usa el **Toast de PrimeNG** (`<p-toast position="top-right">` montado una sola vez en el root) publicado a través de `ToastService` (fachada sobre `MessageService`) con severidades `success/info/warn/error` y auto-cierre a los 4.5 s. |
| **MSG-02** | El `roleGuard` emite un toast de severidad `warn` ("Acceso denegado") al redirigir a un usuario sin permisos hacia `/admin/dashboard`. |
| **MSG-03** | Los errores de API dentro de una vista usan `InlineErrorComponent` (con botón Reintentar); los estados vacíos usan `EmptyStateComponent`. Las confirmaciones destructivas usan `p-dialog` modal. |
