# 08 — Guía de Sistemas Hijos (Remotes): Integración al MIS
> **Proyecto:** MIS - Management Information System
> **Documentación Activa:** [01_PRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/01_PRD.md) | [02_UI_UX_APP_FLOW](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/02_UI_UX_APP_FLOW.md) | [03_TRD](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/03_TRD.md) | [04_BACKEND_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/04_BACKEND_SCHEMA.md) | [07_DATABASE_SCHEMA](file:///f:/FINACIERA%20CONFIANZA/DESARROLLO/mis-host/docs_proyecto/Backend/07_DATABASE_SCHEMA.sql)
> **Versión:** 1.0.0
> **Fecha:** 2026-07-12
> **Ejemplo guía:** `mis-remote-reportes` (Sistema de Reportes)

Esta guía define **cómo se construye e integra un sistema hijo** (Remote / micro-frontend)
al MIS Host. Todo equipo que desarrolle un subsistema (Reportes, Contabilidad, RRHH,
Ventas, Logística…) debe cumplirla. Se usa como ejemplo transversal el
**Sistema de Reportes** (`subsistema-reportes`).

---

## 1. Qué es un Sistema Hijo dentro del MIS

Un sistema hijo es una aplicación **completamente independiente** (repositorio, equipo,
despliegue y base de datos propios) que el Host embebe en tiempo de ejecución mediante
**Native Federation** — nunca con iframes (RN-04).

```
                    ┌────────────────────── MIS HOST (shell) ──────────────────────┐
 Navegador ────────►│  Sidebar (Col 1/2) · Header+Breadcrumb · Auth MFA · IAM      │
                    │                                                              │
                    │  /admin/subsistema-reportes                                  │
                    │  └── RemoteWrapperComponent ── loadRemoteModule ─────────────┼──► remoteEntry.json
                    └──────────────────────────────────────────────────────────────┘         │
                                                                                             ▼
                    ┌───────────────── mis-remote-reportes (sistema hijo) ─────────┐
                    │  frontend/ (Angular Remote)  ──────►  backend/ (Spring Boot) │
                    │                                       BD propia (PostgreSQL) │
                    └───────────────────────────────────────────────────────────────┘
```

### Reglas heredadas de la documentación del MIS (no negociables)

| # | Regla (origen) | Implicación para el sistema hijo |
|---|---|---|
| RN-01 | El Host es dueño de header, sidebar y URL base | El Remote **no** renderiza chrome propio (ni header ni sidebar); solo su área de contenido. |
| RN-02 | Aislamiento total | El Remote no tiene dependencias rígidas con el Host; su backend es propio y **nunca** llama a las APIs del Host (ni viceversa). |
| RN-03 | Comunicación solo por Signals de lectura | El Remote lee `usuarioActivo()`, `esAdmin()`, etc. — jamás muta estado del Host (§4). |
| RN-04 | Prohibido `iframe` | Solo `loadRemoteModule` de `@angular-architects/native-federation`. |
| RN-05 | Sin recarga del navegador | El Remote comparte singletons de Angular (shareAll) — versiones alineadas con el Host. |
| RN-06 | Error elegante | Si el Remote cae, el Host muestra `RemoteErrorComponent`; el hijo no debe romper la shell. |
| PG-06 (TRD) | Tema compartido | El Remote instala PrimeNG y usa el **mismo preset `MisTheme`** + tokens `--mis-*`. |

---

## 2. Ciclo de Integración (visión general)

```
1. Crear repositorio  ──►  2. Desarrollar frontend (Remote NF)  ──►  3. Desarrollar backend propio
        │                                                                     │
        ▼                                                                     ▼
6. Asignar permisos    ◄──  5. Registrar en Gestión de Sistemas  ◄──  4. Dockerizar y desplegar
   por rol (IAM)             (/admin/sistemas/nuevo) + manifest
```

Al completar el paso 6, el sistema aparece automáticamente como ícono en la **Col 1 del
sidebar** para todo usuario cuyo rol/cuenta tenga el subsistema habilitado (SB-02).

---

## 3. Estructura del Repositorio del Sistema Hijo

Un repositorio por sistema hijo, con frontend y backend versionados juntos
(imágenes Docker separadas). Ejemplo para Reportes:

```
mis-remote-reportes/
├── frontend/                              ← Angular 22 Remote (Native Federation)
│   ├── src/
│   │   ├── main.ts                          initFederation() → bootstrap
│   │   ├── bootstrap.ts                     bootstrapApplication(App, appConfig)
│   │   └── app/
│   │       ├── app.config.ts                Zoneless + MisTheme + HttpClient(withFetch)
│   │       ├── remote-root.component.ts     ★ Componente EXPUESTO al Host ('./Component')
│   │       ├── core/
│   │       │   ├── design-system/           mis-theme.ts + tokens.css (copia del Host)
│   │       │   └── shell-contract/          lectura del estado del Host (§4)
│   │       ├── pages/                       vistas internas del subsistema
│   │       │   ├── dashboard-reportes/        KPIs del dominio
│   │       │   ├── reportes-operativos/       listados, filtros, exportación
│   │       │   └── reportes-gerenciales/      consolidados
│   │       └── shared/ui/                   componentes propios del dominio
│   ├── federation.config.mjs              ★ name + exposes './Component'
│   ├── public/                              (sin federation.manifest.json — eso es del Host)
│   ├── Dockerfile
│   └── package.json                         MISMAS versiones de @angular/* que el Host
│
├── backend/                               ← Spring Boot 3 (mismas reglas BE-01..BE-07 del doc 04)
│   ├── src/main/java/pe/confianza/reportes/
│   │   ├── core/                            security (valida JWT del Host), web, config
│   │   ├── catalogo/                        módulo: definiciones de reportes
│   │   ├── generacion/                      módulo: ejecución/exportación (PDF, XLSX)
│   │   └── programacion/                    módulo: reportes programados
│   ├── src/main/resources/db/migration/     Flyway — BD PROPIA (nunca la del Host)
│   ├── Dockerfile
│   └── pom.xml
│
├── docker-compose.yml                     ← dev local: frontend + backend + postgres propio
└── README.md                              ← puertos, slug, cómo probar embebido en el Host
```

### Convención de puertos (desarrollo local)

| Sistema | Slug | Front (dev) | Backend | 
|---|---|---|---|
| Contabilidad | `subsistema-contabilidad` | 4201 | 8081 |
| RRHH | `subsistema-rrhh` | 4202 | 8082 |
| Ventas | `subsistema-ventas` | 4203 | 8083 |
| Logística | `subsistema-logistica` | 4204 | 8084 |
| **Reportes (ejemplo)** | `subsistema-reportes` | **4205** | **8085** |

> El **slug es el identificador universal**: nombre del remote en el manifest, ruta
> `/admin/{slug}`, registro en Gestión de Sistemas y claim de permisos en el JWT.
> Formato: `subsistema-<dominio>` en kebab-case.

---

## 4. Frontend del Sistema Hijo

### 4.1 Scaffolding

```bash
npx -y @angular/cli@latest new reportes-frontend --standalone --style=css --routing=false --skip-git
cd reportes-frontend
npm i @angular-architects/native-federation primeng @primeuix/themes primeicons tailwindcss @tailwindcss/postcss
npx ng add @angular-architects/native-federation --type remote --port 4205
```

Aplicar las mismas obligaciones del TRD que el Host: **eliminar `zone.js`**,
`provideZonelessChangeDetection()`, Tailwind v4 (`@import "tailwindcss"`), Signal Forms
(prohibido `ReactiveFormsModule`), `strict: true`.

### 4.2 `federation.config.mjs` del Remote

```javascript
import { withNativeFederation, shareAll } from '@angular-architects/native-federation/config';

export default withNativeFederation({
  name: 'subsistema-reportes',                 // ★ = slug registrado en el MIS

  exposes: {
    './Component': './src/app/remote-root.component.ts',   // ★ contrato con RemoteWrapper
  },

  shared: {
    ...shareAll({ singleton: true, strictVersion: true, requiredVersion: 'auto', build: 'package' }),
  },

  skip: ['rxjs/ajax', 'rxjs/fetch', 'rxjs/testing', 'rxjs/webSocket', '@primeuix/themes', '@primeng/themes'],
  features: { denseChunking: true },
});
```

> ⚠️ `strictVersion: true` + singleton implica que **las versiones de `@angular/*`,
> `primeng` y `rxjs` deben coincidir con las del Host**. El Host publica su
> `package.json` como referencia; las actualizaciones de framework se coordinan.

### 4.3 Componente raíz expuesto

El `RemoteWrapperComponent` del Host acepta el export `default`, `AppComponent` o
`RemoteRootComponent` del módulo expuesto `./Component`:

```typescript
// src/app/remote-root.component.ts
import { Component, inject, signal } from '@angular/core';

@Component({
  selector: 'reportes-root',
  standalone: true,
  imports: [/* páginas internas */],
  template: `
    <!-- SOLO área de contenido: sin header/sidebar propios (RN-01) -->
    @switch (vista()) {
      @case ('dashboard')   { <reportes-dashboard /> }
      @case ('operativos')  { <reportes-operativos /> }
      @case ('gerenciales') { <reportes-gerenciales /> }
    }
  `,
})
export default class RemoteRootComponent {
  protected readonly vista = signal<'dashboard' | 'operativos' | 'gerenciales'>('dashboard');
}
```

**Navegación interna:** el Host enruta `/admin/{slug}/**` (ruta comodín) al wrapper, así
que **las URLs profundas están soportadas**: `/admin/subsistema-reportes/operativos`
carga el mismo componente raíz del remote. El remote decide su vista inicial leyendo la
URL (`inject(Router).url`) y navega internamente con estado propio (signals) o con
`router.navigate` a subrutas del mismo slug — el breadcrumb del Host las muestra
automáticamente (`🏠 / Reportes / Reportes operativos`).

### 4.4 Tema y estilos

- Copiar `mis-theme.ts` y `tokens.css` del Host (`src/app/core/design-system/`) hasta que
  se publique el paquete compartido (§4.6). `providePrimeNG({ theme: { preset: MisTheme } , ...})`
  con `ripple: false`, idéntico al Host.
- Usar tokens `--mis-*` para colores/espaciado; PrimeNG para componentes complejos y
  Tailwind v4 para layout (reglas PG-01..PG-07 del TRD).

### 4.5 Contrato Host ↔ Remote (Signals de solo lectura)

Gracias a `shareAll` singleton, la instancia de `ShellStateService` del Host es la misma
que inyecta el Remote. El Remote **solo lee**:

| Signal (readonly) | Tipo | Uso típico en el hijo |
|---|---|---|
| `usuarioActivo()` | `UsuarioActivo \| null` | Mostrar autor del reporte, filtrar por área |
| `esAdminSistema()` / `esAdmin()` | `boolean` | Ocultar acciones administrativas |
| `subsistemas()` | `string[]` | Verificación defensiva de acceso propio |
| `menuItemActivo()` | `MenuItemActivo \| null` | Sincronizar vista interna |

```typescript
// core/shell-contract/shell-state.token.ts (en el Remote)
import { inject } from '@angular/core';
import { ShellStateService } from 'mis-host/shell';   // vía paquete compartido (§4.6)

export const useShellState = () => inject(ShellStateService);
```

**Prohibido:** llamar métodos de mutación (`setUsuarioActivo`, `cerrarSesion`, …). El
contrato expone esos setters solo para el Host; los Remotes que los invoquen no pasan
revisión de integración.

### 4.6 Paquete compartido `@confianza/mis-shell` (recomendado, fase 2)

Para no copiar código, el Host debe publicar un paquete npm ligero en el registry privado con:
`ShellStateService` (+ interfaces `UsuarioActivo`, `MenuItemActivo`), `MisTheme`,
`tokens.css`. Host y Remotes lo declaran en `shared` como singleton. Mientras no exista,
la copia local de §4.4 es el mecanismo oficial.

### 4.7 Peticiones HTTP del hijo

- El frontend del hijo consume **únicamente su propio backend**: prefijo
  `/api/reportes/v1/*` (proxy del mismo dominio) — nunca `/api/v1/*` del Host.
- Adjunta el **mismo JWT** emitido por el Host (está disponible en `sessionStorage`
  clave `mis.sesion` o via interceptor propio del remote que lo lea del contrato compartido).

---

## 5. Backend del Sistema Hijo (Spring Boot)

El backend del hijo sigue **la misma arquitectura y reglas del doc 04 §2** (monolito
modular, BE-01..BE-07), pero con su propio dominio, BD y despliegue.

### 5.1 Diferencias respecto al backend del Host

| Aspecto | Backend Host (`mis-backend`) | Backend hijo (`reportes-backend`) |
|---|---|---|
| Responsabilidad | Auth + MFA, IAM, registro de sistemas | Solo su dominio (reportes) |
| Prefijo API | `/api/v1/*` | `/api/reportes/v1/*` |
| Emite JWT | ✅ (login + OTP) | ❌ — **solo valida** el JWT del Host |
| Base de datos | `mis` (doc 07) | `reportes` (propia, Flyway propio) |
| Usuarios/roles | Tablas propias | ❌ — confía en los claims del token |

### 5.2 Seguridad: validar el JWT del Host

El Host y los hijos comparten la verificación de firma (`MIS_JWT_SECRET` común o, mejor,
clave pública/JWKS expuesta por `mis-backend`). El filtro del hijo:

1. Valida firma y expiración del `Bearer` token.
2. Lee claims: `sub` (usuarioId), `rol` (slug), `subsistemas` (string[]).
3. **Rechaza con `403`** si `subsistemas` no contiene `subsistema-reportes`.
4. Autoriza por rol con la misma jerarquía (`admin-sistema > admin-general > supervisor-area`).

```java
// core/security/RequiereSubsistema.java (concepto)
if (!claims.subsistemas().contains("subsistema-reportes")) {
    throw new AccessDeniedException("El usuario no tiene habilitado el subsistema de reportes.");
}
```

### 5.3 API del ejemplo (Sistema de Reportes)

Mismo formato `ApiError` y `PageResponse<T>` del doc 04 §3.

| Método y ruta | Rol mínimo | Descripción |
|---|---|---|
| `GET  /api/reportes/v1/definiciones?page&q&modulo` | supervisor-area | Catálogo de reportes disponibles (filtrado por los módulos permitidos del rol). |
| `GET  /api/reportes/v1/definiciones/{id}` | supervisor-area | Detalle + parámetros del reporte. |
| `POST /api/reportes/v1/ejecuciones` | supervisor-area | Ejecuta un reporte `{ definicionId, parametros }`; responde `{ ejecucionId, estado }`. |
| `GET  /api/reportes/v1/ejecuciones/{id}` | supervisor-area | Estado/resultado (`pendiente → procesando → completado/fallido`). |
| `GET  /api/reportes/v1/ejecuciones/{id}/descarga?formato=pdf\|xlsx` | supervisor-area | Descarga del archivo generado. |
| `POST /api/reportes/v1/programaciones` | admin-general | Programa un reporte recurrente (cron + destinatarios). |
| `DELETE /api/reportes/v1/programaciones/{id}` | admin-general | Elimina la programación. |

### 5.4 BD propia (esquema mínimo del ejemplo)

```sql
-- reportes-backend · V1__baseline.sql (BD independiente de la del Host)
CREATE TABLE definiciones_reporte (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(120) NOT NULL,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    -- slug del módulo del MIS al que pertenece (permiso a nivel módulo, doc 04 §5)
    modulo_mis  VARCHAR(80)  NOT NULL,
    parametros  JSONB        NOT NULL DEFAULT '[]',
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE ejecuciones (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    definicion_id  UUID NOT NULL REFERENCES definiciones_reporte (id),
    usuario_id     UUID NOT NULL,              -- claim `sub` del JWT (no hay FK al Host)
    parametros     JSONB NOT NULL DEFAULT '{}',
    estado         VARCHAR(15) NOT NULL DEFAULT 'pendiente'
                   CHECK (estado IN ('pendiente','procesando','completado','fallido')),
    archivo_url    VARCHAR(300),
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalizado_en  TIMESTAMPTZ
);

CREATE TABLE programaciones (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    definicion_id  UUID NOT NULL REFERENCES definiciones_reporte (id),
    cron           VARCHAR(60)  NOT NULL,
    destinatarios  JSONB        NOT NULL DEFAULT '[]',
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

> Los ids de usuario del Host se guardan **como valor opaco** (claim `sub`), sin FK:
> las BDs no se cruzan (RN-02).

---

## 6. Registro del Sistema Hijo en el MIS

### 6.1 Manifest de federación del Host

```json
// mis-host/public/federation.manifest.json
{
  "subsistema-reportes": "http://localhost:4205/remoteEntry.json"
}
```
En producción, la URL apunta al contenedor desplegado:
`https://reportes.confianza.pe/remoteEntry.json`.

### 6.2 Alta en Gestión de Sistemas (`/admin/sistemas/nuevo` — rol admin-sistema)

| Campo | Valor del ejemplo |
|---|---|
| Nombre | `Reportes` |
| Slug | `subsistema-reportes` (⚠️ idéntico al `name` del federation.config) |
| Descripción | `Generación, exportación y programación de reportes corporativos.` |
| Ícono | `pi pi-file-export` |
| URL del Remote | `http://localhost:4205/remoteEntry.json` |
| Versión / Estado | `1.0.0` / `Activo` |

### 6.3 Estructura jerárquica (pestaña **Estructura** del detalle)

```
Reportes
├── Reportes Operativos          (sección)
│   ├── Diarios                  (subsección)
│   │   ├── Posición de Caja     (módulo)
│   │   └── Operaciones del Día  (módulo)
│   └── Mensuales
│       ├── Cierre Operativo     (módulo)
│       └── Indicadores de Área  (módulo)
└── Reportes Gerenciales
    └── Consolidados
        ├── Tablero Ejecutivo    (módulo)
        └── Comparativo Anual    (módulo)
```

### 6.4 Permisos y visibilidad

1. **Pestaña Permisos** del sistema: marcar los módulos permitidos por rol
   (p. ej. `supervisor-area` → solo Reportes Operativos/Diarios).
2. **Rol** (`/admin/accesos/roles/:id/editar`): habilitar `subsistema-reportes` en
   "Sistemas Embebidos" → alimenta el claim `subsistemas` del JWT.
3. **Usuario** (opcional): override individual en su formulario.

Con esto el ícono "Reportes" aparece en la Col 1 del sidebar (SB-02) y la Col 2 muestra
su estructura al seleccionarlo; el backend hijo autoriza con los mismos claims.

---

## 7. Despliegue

- **Frontend hijo**: mismo Dockerfile nginx del TRD §7 (build Angular → nginx). El nginx
  del hijo debe servir `remoteEntry.json` y assets con **CORS habilitado hacia el dominio del Host**
  (`Access-Control-Allow-Origin: https://mis.confianza.pe`).
- **Backend hijo**: Dockerfile multi-stage del doc 04 §7; expone `/api/reportes/v1`.
- **Orquestación**: Dokploy/Coolify — una app por imagen; el Host solo conoce la URL del
  `remoteEntry.json` (manifest) y la registra en Gestión de Sistemas.
- **docker-compose dev** del repo hijo: frontend (4205) + backend (8085) + postgres propio,
  para probar embebido contra el Host local (`npm start` en mis-host).

---

## 8. Checklist de Integración (definition of done del sistema hijo)

- [ ] Slug definido `subsistema-<dominio>` y consistente en federation.config, manifest y registro IAM.
- [ ] Frontend zoneless, sin `zone.js`, versiones de `@angular/*`/`primeng`/`rxjs` alineadas al Host.
- [ ] Expone `./Component` con export `default` (o `AppComponent`/`RemoteRootComponent`).
- [ ] No renderiza header/sidebar propios; usa `MisTheme` + tokens `--mis-*`.
- [ ] Solo **lee** signals del contrato del shell; cero mutaciones.
- [ ] Backend propio Spring Boot con reglas BE-01..BE-07, valida JWT del Host y el claim `subsistemas`.
- [ ] BD propia con Flyway; ningún acceso a la BD del Host.
- [ ] CORS del `remoteEntry.json` habilitado para el dominio del Host.
- [ ] Registrado en `federation.manifest.json` + alta en `/admin/sistemas` con estructura y permisos por rol.
- [ ] Probado los 3 estados del wrapper: skeleton al cargar, contenido, y error elegante con el hijo apagado (CA-04/CA-05).
