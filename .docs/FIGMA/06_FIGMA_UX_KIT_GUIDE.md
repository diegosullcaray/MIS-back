# Guía de Importación y Especificación UX/UI para Figma (MIS - Financiera Confianza)
> **Documentación Activa:** [01_PRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/01_PRD.md) | [02_UI_UX_APP_FLOW](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/02_UI_UX_APP_FLOW.md) | [03_TRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/03_TRD.md) | [04_BACKEND_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/04_BACKEND_SCHEMA.md) | [05_IMPLEMENTATION_PLAN](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/05_IMPLEMENTATION_PLAN.md) | [06_FIGMA_UX_KIT](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/06_FIGMA_UX_KIT.html)  
> **Kit:** v3.1 (2026-07-12) — plantillas en `docs_proyecto/FIGMA/` (login.html · dashboard_interaccion.html · gestion_iam.html)

Este documento sirve como guía para importar las plantillas interactivas de `docs_proyecto/FIGMA/` a Figma, y detalla la especificación técnica de arquitectura de información y UX.

### Cambios v3.1 (alineación con el frontend implementado)
- **Breadcrumb SOLO en el header:** el header glass muestra `MIS | [🏠 / sección / vista]` con `p-breadcrumb` de PrimeNG. Las vistas de gestión **no** llevan títulos de página ni enlaces "Volver" propios.
- **Gestión encapsulada en cards:** cada vista de gestión (listas, formularios, detalles) es una **card a ancho completo** con *header de card* (título + descripción + botón de acción a la derecha) y *body* (tablas, formularios, pestañas SelectButton).
- **Mensajes:** toasts de PrimeNG (`p-toast`) en la esquina superior derecha, severidades success/info/warn/error. El aviso de "Acceso denegado" del roleGuard usa severidad warn.
- **Rutas corregidas** en las URL-bars de las plantillas: `/admin/dashboard` (antes `mi-espacio`), `/admin/sistemas/...` (antes `accesos/sistemas/...`), `/admin/accesos/usuarios/:id` y `/admin/accesos/roles/:id` (antes con `/detalle/`), y `/login` para ambas pantallas de autenticación.
- **Sin catálogos:** el módulo Catálogos se retiró del alcance (PRD v1.1 / UI-UX v1.4); ninguna pantalla debe referenciarlo.

### Cambios v3.0
- **Sin emojis:** todos los íconos de navegación son SVG inline estilo Lucide (stroke, sin relleno, color via CSS)
- **Sidebar limpio:** Col 1 contiene únicamente íconos de sistemas (Inicio + Remotes). Perfil y Cerrar sesión se eliminaron del sidebar.
- **Menú de usuario en Header:** el pill `[avatar + nombre + chevron]` en el header despliega un dropdown con Mi perfil / Preferencias / Cerrar sesión.
- **macOS Aurora Minimalist:** chrome de ventana (semafóforo), header glass 44px, Col 1 navy 56px, Col 2 panel 220px. Sin colores planos, sin elementos de relleno innecesarios.

---

## 📋 Inventario de Frames

### Existentes (en `docs_proyecto/FIGMA/`)

| Archivo | Frames | Ruta real |
|---|---|---|
| `login.html` | Login (credenciales) · Verificación MFA (OTP 6 dígitos, expira 03:00) | `/login` |
| `dashboard_interaccion.html` | Mi espacio (KPIs + tabla de MFEs) · Remote cargando (skeleton) · Remote cargado · Remote en error | `/admin/dashboard`, `/admin/:remote` |
| `gestion_iam.html` | Usuarios (lista) · Usuario editar (2 pestañas) · Roles (lista) · Rol detalle (3 pestañas) · Sistemas (lista) · Sistema detalle (2 frames) | `/admin/accesos/*`, `/admin/sistemas/*` |

### ⏳ Diseños pendientes de crear

| Frame | Ruta | Notas |
|---|---|---|
| Formulario **Nuevo Usuario** | `/admin/accesos/usuarios/nuevo` | Card con pestañas `Información General` / `Roles y Sistemas` (SelectButton) |
| Formulario **Nuevo/Editar Rol** | `/admin/accesos/roles/nuevo` · `roles/:id/editar` | Card única: Detalles del Rol + Accesos Predeterminados |
| Formulario **Registrar/Editar Sistema** | `/admin/sistemas/nuevo` · `:id/editar` | Card con pestañas `Identificación` / `Despliegue` |
| Pestaña **Estructura** del sistema | `/admin/sistemas/:id` (tab 2) | Editor del árbol Secciones → Subsecciones → chips de Módulos |
| **Acceso denegado** | vista compartida | Ícono escudo + mensaje + botón "Volver a Mi espacio" |
| **404 Not Found** | `**` | Página completa fuera del shell |
| **Estados de toast** | overlay global | 4 severidades PrimeNG, top-right, auto-cierre 4.5 s |

---

## 🚀 Cómo importar este diseño en Figma

Para convertir el archivo interactivo en capas editables y componentes nativos de Figma (incluyendo Auto Layout, colores y textos), siga estos pasos:

1. **Abra el archivo en su navegador**:
   - Localice las plantillas en `docs_proyecto/FIGMA/` (`login.html`, `dashboard_interaccion.html`, `gestion_iam.html`).
   - Ábralas en Google Chrome o su navegador preferido.

2. **Instale el plugin "html.to.design" en Figma**:
   - Abra **Figma**.
   - Vaya a la sección de **Plugins** y busque **html.to.design** (o alternativamente *Builder.io - HTML to Figma*).
   - Instale y ejecute el plugin dentro de un nuevo archivo de diseño.

3. **Importar la URL o el Código HTML**:
   - **Opción A (Recomendada)**: Copie la ruta del archivo local de su navegador (ej. `file:///C:/Users/.../mis_figma_ux_kit.html`) y péguela en el cuadro de texto de la URL del plugin en Figma.
   - **Opción B**: Si el plugin no lee rutas locales de archivos directamente, use una extensión de Chrome como **SingleFile** para descargar la página completa en un solo archivo, o simplemente arrastre el código HTML/CSS en Figma usando el plugin en modo de carga de archivos (Upload).
   - Seleccione la resolución de importación deseada (ej. **Desktop 1440px**).
   - Presione **Import**.

4. **Organización del archivo en Figma**:
   - El plugin generará un frame completo con todas las secciones organizadas verticalmente.
   - Podrá extraer los colores (Branding), componentes y layouts para guardarlos como **Figma Components** y **Color/Typography Styles**.

---

## 🎨 Especificaciones del Design System

### 1. Paleta de Colores (Branding)
Diseño alineado con la identidad corporativa y estética macOS Aurora Minimalist:
- **Navy Primario (`#1D396E`)**: Color dominante para el Host. Representa solidez, seguridad y control institucional. Utilizado en el Sidebar principal (Col 1), botones primarios y encabezados importantes.
- **Sky Blue Secundario (`#42ADE0`)**: Color de acento para la interacción. Utilizado para enlaces, estados activos (el ícono seleccionado de la columna 1) y focos visuales (focus-ring).
- **Fondos de Superficie**:
  - General (`#F4F6F9`): Un gris azulado ultra suave que reduce la fatiga visual.
  - Paneles (`#F8FAFC`): Para la columna de navegación secundaria persistente (Col 2).
  - Cards (`#FFFFFF`): El blanco puro resalta los contenedores de datos sobre el fondo gris.

### 2. Tipografía (macOS HIG Stack)
- **Familia**: `-apple-system, BlinkMacSystemFont, 'Inter', 'Segoe UI', sans-serif`.
- **Escala de Jerarquía**:
  - `2xl` (28px - Bold): Títulos de páginas principales del Host.
  - `xl` (22px - SemiBold): Títulos internos de las vistas (ej. formularios o modales).
  - `lg` (17px - SemiBold): Cabeceras de los paneles de navegación de segunda columna.
  - `base` (15px - Regular): Cuerpo de texto para lectura óptima.
  - `sm` (13px - Regular/Medium): Textos secundarios, tablas, inputs y metadatos.
  - `xs` (11px - Bold): Etiquetas de texto en la barra lateral de primer nivel.

### 3. Layout: Estructura de 3 Columnas (Patrón macOS/Gmail)
La interfaz principal (Shell) tiene tres divisiones bien definidas:

1. **Columna 1 — Sidebar de íconos (56px, Navy):** Contiene el logo del MIS en la parte superior y los íconos SVG de sistemas (Inicio + Remotes) en el centro. **No contiene Perfil ni Cerrar sesión.** La zona inferior está vacía y limpia. Nunca se colapsa.

2. **Columna 2 — Panel de navegación secundario (220px):** Muestra la estructura de rutas del subsistema seleccionado actualmente, con íconos SVG pequeños junto a cada ítem. Panel persistente que nunca desaparece.

3. **Zona de Contenido Principal:** Header glass 44px (wordmark + breadcrumb + **pill de usuario** → dropdown de Perfil/Salir) + área de trabajo dinámica.

#### Menú de Usuario (Header)
El pill de usuario `[avatar] Nombre [▾]` en el header despliega un dropdown con:
- Nombre completo y email del usuario
- Mi perfil (ic. SVG usuario)
- Preferencias (ic. SVG engranaje)
- _separador_
- Cerrar sesión (ic. SVG salida) — texto en rojo

> **Regla de diseño:** Perfil y Cerrar sesión **nunca** aparecen en el sidebar. Esta es la convención macOS (ver Finder, Mail, calendarios del sistema).

---

## ⚡ Interacción y Estados Asíncronos

### 1. Carga Dinámica (Zoneless Native Federation)
La comunicación y ciclo de vida de los subsistemas se define a través de:
- **Remote Wrapper**: Componente Angular que captura el slug de la ruta (ej. `/admin/:nombre-subsistema`), ejecuta `loadRemoteModule` y administra los estados mediante directivas `@defer`.
- **RemoteSkeletonComponent**: Plantilla de carga con animaciones pulsantes lineales. Previene saltos bruscos de la UI (*Cumulative Layout Shift*).
- **RemoteErrorComponent**: Se activa cuando un Remote falla en red o cae. Muestra un estado de error contextual que invita al reintento, permitiendo que el Host y otros subsistemas sigan operando al 100%.

### 2. Contrato de Comunicación (Signals)
- El Host expone un servicio `ShellStateService` singleton.
- Los Remotes consumen señales de solo lectura (`usuarioActivo()`, `catalogoActivo()`, `esAdmin()`).
- Se restringe la escritura desde los remotes mediante firmas de solo lectura (`asReadonly()`), garantizando seguridad y consistencia en el estado de la aplicación empresarial.

---

## 🏗️ Notas de Infraestructura y Plataforma

| Capa | Decisión |
|---|---|
| **Backend** | Spring Boot 3 (monolito modular) + PostgreSQL — ver [04_BACKEND_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/04_BACKEND_SCHEMA.md) y [07_DATABASE_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/07_DATABASE_SCHEMA.sql) |
| **Contenedores** | Azure (contenedores, aprovisionados con Terraform) y Google Cloud (VMs con Dokploy) |
| **Frontend** | Angular zoneless + Native Federation, Tailwind v4 + PrimeNG (preset MisTheme), diseño macOS Aurora Minimalist; los subsistemas se embeben como Remotes (web y móvil) |