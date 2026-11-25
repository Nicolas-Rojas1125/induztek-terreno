# Induztek Terreno

**Formalización inmediata de protocolos de prueba eléctrica en terreno**
App Android (Kotlin + Jetpack Compose + Material 3 + MVVM + SQLite + Retrofit) conectada a **microservicios Spring Boot** (Eureka + API Gateway + MySQL en Docker) y a la **API externa Open-Meteo**.
DSY1105 Desarrollo de Aplicaciones Móviles · Duoc UC · **Evaluación Parcial 4**

> Todos los datos que se usan para probar la app son **ficticios**. No contiene información real de Induztek Ingeniería SpA ni de terceros.

---

## 1. Integrantes

| Integrante | GitHub |
|---|---|
| Nicolás Rojas | [@Nicolas-Rojas1125](https://github.com/Nicolas-Rojas1125) |
| Vicente Arancibia | [@viarancibiam-cmd](https://github.com/viarancibiam-cmd) |

**Docente:** Víctor Rosendo · **Grupo N°:** 17 · **Sección:** 011D - 001D
**Trello:** https://trello.com/b/UaDm92a3/grupo-17-induztek-terreno

---

## 2. Contexto

Cuando un técnico de Induztek prueba un transformador, un relé, una celda, un interruptor o un cable en la planta del cliente, hoy anota los valores en papel. El protocolo oficial llega **entre 5 y 15 días después**, con errores de transcripción y sin trazabilidad.

**Induztek Terreno** permite:

- registrar cada prueba en el teléfono **en el momento, incluso sin señal**;
- calcular el resultado al instante;
- entregar un **acta en PDF** al cliente;
- sincronizar todo con la oficina, donde el ingeniero **formaliza** el protocolo.

En esta entrega la app deja de ser solo local. Las cuentas, los clientes, los equipos, los instrumentos, las visitas y los protocolos viven en **microservicios propios**. El teléfono guarda una copia (SQLite) para seguir trabajando sin internet y se sincroniza al volver la conexión.

---

## 3. Funcionalidades

### Novedades de la Evaluación Parcial 4

| Pedido de la pauta / del equipo | Qué se hizo |
|---|---|
| Consumo de API externa | **Open-Meteo** con Retrofit. La tarjeta "Clima en la instalación" (en el detalle de la instalación y en el protocolo) muestra temperatura, humedad y viento según las coordenadas GPS. El botón **"Usar en el protocolo"** copia temperatura y humedad a las condiciones de la prueba. Si no hay internet, muestra un aviso y no bloquea nada. |
| Microservicios propios | `ms-usuarios`, `ms-activos` y `ms-protocolos` (Spring Boot 4 + JPA + MySQL), registrados en **Eureka** y expuestos por un **API Gateway** (puerto 8081). |
| CRUD completo app ↔ microservicios | Cuentas, clientes, instalaciones, equipos e instrumentos: crear, ver, **editar** y **eliminar** en línea. Visitas y protocolos se crean sin conexión y se **sincronizan por UUID**. |
| Crear cuentas según el rol | **Administrador** → crea todos los roles · **Ingeniero** → solo Técnico y Cliente · **Técnico** y **Cliente** → no crean cuentas. Se valida en la app **y** en el servidor (HTTP 403). Solo el administrador elimina cuentas y siempre debe quedar al menos un administrador activo. |
| Cuenta | **Recuperar contraseña** (código de 6 dígitos, válido por 15 minutos), **editar perfil** y **cambiar contraseña**. Las contraseñas se guardan con PBKDF2 + sal. |
| Datos de ejemplo | Se eliminaron. La app parte vacía: solo quedan los catálogos (regiones, comunas, roles y permisos). El servidor crea un único administrador inicial. |
| Listas | **Búsqueda, filtros (chips) y paginación** de 10 en 10 en cuentas, clientes, equipos, visitas, protocolos e instrumentos. |
| Equipos | **Ficha técnica por tipo de equipo** (transformador, interruptor, celda, cable y relé), con validación, rangos, opciones y porcentaje de avance. Se ve en el detalle del equipo. |
| Información en pantalla | Se agregaron la dirección del cliente, las cuentas asociadas, los totales de equipos y visitas, las fechas de creación y modificación, el cliente y la instalación del equipo, las pruebas que aplican, etc. |
| Colores y diseño | Tema único con roles de Material 3 (`surfaceContainer*`, `errorContainer`, `primary`), homogéneo en claro y oscuro. Ninguna pantalla usa colores fijos fuera del tema y de los colores de resultado. |
| Sincronización | Tarjeta de estado en Inicio y pantalla **Más › Sincronización** (cambios pendientes, última sincronización y errores). Sincroniza sola al iniciar sesión, al recuperar la conexión y cada 5 minutos. |
| Pruebas unitarias | **114 pruebas** en la app (ViewModels, repositorios remotos con APIs falsas y dominio) + **47 pruebas** en los microservicios. |
| APK firmado | `signingConfigs.release` en `app/build.gradle.kts`, llave `keystore/induztek-release.jks`. |

### Lo que ya traía la app (EP2)

Protocolos con pruebas según el tipo de equipo y resultado en vivo; comparación con la prueba anterior (> 25 %); bloqueo de instrumentos vencidos; acta PDF; recepción del cliente; formalización y observación con historial; vista restringida del cliente (solo su empresa); cámara (placa), GPS, vibración y aviso sin conexión.

---

## 4. Arquitectura

```
┌──────────── App Android ─────────────┐        ┌──────────────────── Docker ────────────────────┐
│ Compose UI → ViewModel (StateFlow)   │        │                                                │
│        ↓                             │  HTTP  │  API Gateway :8081 ──lb://──▶ ms-usuarios :8082 │──▶ MySQL db_usuarios
│ Repositorios ──▶ Retrofit ───────────┼───────▶│        │                  ├─▶ ms-activos  :8083 │──▶ MySQL db_activos
│      │                               │        │        │                  └─▶ ms-protocolos:8084│──▶ MySQL db_protocolos
│      └──▶ SQLite (caché + offline)   │        │  Eureka :8761 (registro de servicios)          │
│ ClimaRemoto ──▶ Retrofit ────────────┼──▶ api.open-meteo.com (API externa)                     │
└──────────────────────────────────────┘        └────────────────────────────────────────────────┘
```

- **Datos maestros y cuentas** (clientes, instalaciones, equipos, instrumentos, usuarios): primero se escriben en el servidor y, si responde bien, se copian a SQLite.
- **Visitas y protocolos**: se crean en el teléfono, incluso sin señal, con un **UUID**. Triggers de SQLite los marcan como pendientes y `SincronizacionRepositorio` los envía con `PUT /api/visitas/{uuid}` y `PUT /api/protocolos/{uuid}`. El envío es idempotente: repetirlo no duplica nada.
- **Formalizado = inmutable** también en el servidor: si un protocolo ya está formalizado y llega otro estado, el servidor responde **409**.
- Un **cliente** solo descarga los datos de su empresa (`?idCliente=`).

```
app/src/main/java/cl/duocuc/induztek/
├── data/remote/         # Apis.kt (Retrofit), ApiModelos.kt (DTO), Mapeos.kt, Remotos.kt, LlamadaSegura.kt
├── data/repository/     # Sesion, Usuario, Cliente, Equipo, Instrumento, Visita, Protocolo, Sincronizacion
├── data/local/          # SQLite (esquema v2 + triggers de sincronización), preferencias, espejo local
├── domain/              # reglas de negocio, validadores, FichaTecnica, Paginador (Kotlin puro)
├── viewmodel/           # un ViewModel por pantalla (los de cuentas y clima reciben interfaces → testeables)
└── ui/                  # Compose: components (BarraBusqueda, FiltrosChips, ControlPaginacion, TarjetaClima…)

backend/
├── eurekaserver/  apigateway/  ms-usuarios/  ms-activos/  ms-protocolos/
├── docker-compose.yml   init-db.sql
```

---

## 5. Endpoints

Todas las rutas se consumen a través del **API Gateway: `http://localhost:8081`** (desde el emulador: `http://10.0.2.2:8081`).
Las acciones que dependen del rol envían la cabecera **`X-Usuario-Id`** con el id del usuario que actúa. Los errores responden JSON `{ "status", "message", "errores" }`.

### ms-usuarios (8082 · `db_usuarios`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/usuarios/login` | Inicia sesión `{correo, contrasena}` |
| POST | `/api/usuarios/recuperar` | Genera el código de recuperación `{correo}` (en modo demo lo devuelve en `codigoDemo`) |
| POST | `/api/usuarios/restablecer` | `{correo, codigo, nueva}` |
| GET | `/api/usuarios?buscar=&rol=&activo=&idCliente=&pagina=0&tamano=10` | Lista paginada con filtros |
| GET | `/api/usuarios/directorio` | Usuarios activos (para la sincronización) |
| GET | `/api/usuarios/{id}` | Detalle |
| POST | `/api/usuarios` | Crea la cuenta (valida el rol del actor) · `X-Usuario-Id` |
| PUT | `/api/usuarios/{id}` | Edita el perfil · `X-Usuario-Id` |
| PUT | `/api/usuarios/{id}/contrasena` | Cambia la contraseña `{actual, nueva}` · `X-Usuario-Id` |
| PUT | `/api/usuarios/{id}/estado` | Activa o desactiva `{activo}` · `X-Usuario-Id` |
| DELETE | `/api/usuarios/{id}` | Elimina (solo el administrador) · `X-Usuario-Id` |
| GET | `/api/roles` · `/api/roles/creables` | Roles · roles que el actor puede crear (`X-Usuario-Id`) |

### ms-activos (8083 · `db_activos`)

| Método | Ruta | Descripción |
|---|---|---|
| GET/POST | `/api/clientes` | Lista (`buscar, pagina, tamano`) / crea |
| GET/PUT/DELETE | `/api/clientes/{id}` | Detalle / edita / elimina (409 si tiene instalaciones) |
| GET/POST | `/api/instalaciones` | Lista (`idCliente, buscar`) / crea |
| GET/PUT/DELETE | `/api/instalaciones/{id}` | Detalle / edita / elimina (409 si tiene equipos) |
| GET/POST | `/api/equipos` | Lista (`buscar, idInstalacion, idCliente, tipo, clasificacion`) / crea con `datosTecnicos` |
| GET/PUT/DELETE | `/api/equipos/{id}` | Detalle / edita / elimina |
| GET/POST | `/api/instrumentos` | Lista (`buscar, categoria, vigencia`) / crea |
| GET/PUT/DELETE | `/api/instrumentos/{id}` | Detalle / edita / elimina |
| POST | `/api/instrumentos/{id}/calibraciones` | Registra una nueva calibración |
| GET | `/api/sync/activos?idCliente=` | Descarga todos los datos maestros para el teléfono |

### ms-protocolos (8084 · `db_protocolos`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/visitas?buscar=&idCliente=&idInstalacion=&abiertas=` | Lista paginada |
| GET/PUT/DELETE | `/api/visitas/{uuid}` | Detalle / crea o actualiza (idempotente) / elimina (si no tiene protocolos) |
| GET | `/api/protocolos?buscar=&estado=&resultado=&idCliente=&idEquipo=&visitaUuid=` | Lista paginada (el cliente no ve borradores ni anulados) |
| GET | `/api/protocolos/resumen?idCliente=` | Conteo por estado |
| GET/PUT/DELETE | `/api/protocolos/{uuid}` | Detalle / crea o actualiza (409 si ya está formalizado) / elimina (solo borrador) |
| GET | `/api/sync/protocolos?desde={epochMs}&idCliente=` | Cambios desde la última descarga |

### API externa: Open-Meteo

| Método | URL | Uso |
|---|---|---|
| GET | `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&timezone=auto` | Clima actual en la instalación (sin API key) |

Colección para Postman: `docs/postman/Induztek.postman_collection.json`.

---

## 6. Cómo ejecutar

### 6.1 Microservicios (Docker)

Requisito: **Docker Desktop** encendido.

```bash
cd backend
docker compose up --build -d
docker compose ps
```

Espere 1 o 2 minutos a que todos los servicios aparezcan en **Eureka**: http://localhost:8761 (MS-USUARIOS, MS-ACTIVOS, MS-PROTOCOLOS y API-GATEWAY).
Prueba rápida:

```bash
curl -X POST http://localhost:8081/api/usuarios/login -H "Content-Type: application/json" \
     -d '{"correo":"admin@induztek.cl","contrasena":"Induztek2026"}'
```

MySQL queda expuesto en el puerto **3307** (usuario `root`, clave `rootpass`) por si quiere revisar las tablas con Workbench. Para apagar todo: `docker compose down`. Para borrar también los datos: `docker compose down -v`.

### 6.1-b Alternativa sin Docker: Laragon

1. Abra **Laragon › Start All** (MySQL en `localhost:3306`, usuario `root`, sin clave). No hace falta crear las bases: cada microservicio crea la suya y sus tablas al arrancar.
2. Doble clic en `backend/iniciar-laragon.bat`. Abre 5 ventanas en orden: Eureka, los 3 microservicios y el Gateway. Usa Java 21; si no hay `JAVA_HOME`, toma el de Android Studio.
3. En 1 o 2 minutos revise http://localhost:8761 y, en **Laragon › Database** (HeidiSQL), las bases `db_usuarios`, `db_activos` y `db_protocolos` con sus tablas.

Los valores por defecto (`localhost:3306`, `root`, sin clave) ya sirven para Laragon. Si su MySQL tiene clave, defina `DB_PASSWORD` antes de ejecutar el `.bat`. Docker define sus propios valores en `docker-compose.yml`.

### 6.2 App Android

1. Abrir la carpeta raíz del proyecto en **Android Studio** y esperar el Gradle Sync.
2. Ejecutar en un emulador (API 26 o superior). La app apunta por defecto a `http://10.0.2.2:8081/`, que es el `localhost` del PC visto desde el emulador.
   - En un **teléfono físico**: en el login, toque **Configurar servidor** y escriba `http://IP-DEL-PC:8081/` (el PC y el teléfono deben estar en la misma red Wi-Fi).
3. Ingresar con la cuenta inicial:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador (Jefe técnico) | `admin@induztek.cl` | `Induztek2026` |

4. Desde **Más › Cuentas de usuario › Nueva cuenta**, cree un ingeniero, un técnico y, después de crear un cliente, una cuenta de cliente.

### 6.3 Pruebas unitarias

- **App:** clic derecho en `app/src/test/java` › *Run 'Tests in java'* o, para ver la cobertura, *Run with Coverage*. Por consola: `./gradlew testDebugUnitTest` y `./gradlew createDebugUnitTestCoverageReport` (JaCoCo).
  - `viewmodel/`: LoginViewModel, RecuperarClave, CambiarClave, EditarPerfil, Usuarios (lista, formulario y detalle) y Clima, con fuentes falsas y `kotlinx-coroutines-test`.
  - `data/`: UsuariosRemoto, ActivosRemoto y ClimaRemoto con APIs Retrofit falsas (éxito, 401, 403, 409, sin red, JSON inválido).
  - `domain/`: reglas de negocio, roles y permisos, ficha técnica, paginador y validadores.
- **Microservicios:** en cada carpeta `backend/ms-*`, ejecute `./mvnw test` (JUnit 5 + Mockito sobre los servicios: reglas de roles, contraseñas, RUT, filtros, borrados con dependencias, upsert idempotente e inmutabilidad del protocolo formalizado).

---

## 7. APK firmado (release)

Configuración en `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = rootProject.file(propiedadesFirma.getProperty("storeFile", "keystore/induztek-release.jks"))
        storePassword = propiedadesFirma.getProperty("storePassword")
        keyAlias = propiedadesFirma.getProperty("keyAlias")
        keyPassword = propiedadesFirma.getProperty("keyPassword")
    }
}
buildTypes { release { signingConfig = signingConfigs.getByName("release") } }
```

- Llave: `keystore/induztek-release.jks` · alias `induztek` · RSA 2048 · válida por 10.000 días.
- Claves en `keystore.properties`. Se suben al repositorio **solo porque la pauta lo pide**. En un proyecto real este archivo va en `.gitignore`.

**Generar el APK:** *Build › Generate Signed App Bundle / APK › APK* → elegir `keystore/induztek-release.jks` → variante **release**. El archivo queda en `app/release/app-release.apk` (o use `./gradlew assembleRelease`).
**Verificar la firma:** `apksigner verify --print-certs app/release/app-release.apk`.

| APK firmado | Archivo .jks |
|---|---|
| ![APK firmado](docs/capturas/apk_firmado.png) | ![Keystore](docs/capturas/keystore_jks.png) |

---

## 8. Guion de demostración (5 minutos)

1. `docker compose up -d` → mostrar Eureka con los 4 servicios.
2. Login como **admin** → Más › Cuentas › crear **Ingeniero** y **Técnico**. Iniciar sesión como ingeniero e intentar crear un Administrador: **no aparece en la lista** (y el servidor respondería 403).
3. Ingeniero → Clientes › **Nuevo cliente** (con dirección) → Nueva instalación con **GPS** → ver la **tarjeta de clima (Open-Meteo)**.
4. Registrar un **transformador** con la **ficha técnica** (grupo de conexión, impedancia, refrigeración…) y ver el % de avance. Editarlo.
5. Crear una **visita** en **modo avión** → protocolo → mediciones → "Usar en el protocolo" desde el clima (si hay red) → cerrar la visita. Inicio muestra "**N cambios por enviar**".
6. Quitar el modo avión → se sincroniza sola. Mostrar el registro en MySQL (`db_protocolos.protocolo`) o con `GET /api/protocolos`.
7. Listas: buscar, filtrar por chips y paginar.
8. Más › **Cambiar contraseña** · Login › **¿Olvidó su contraseña?** (el código de demostración aparece en pantalla).
9. Crear una **cuenta de cliente**: solo ve su empresa y "Pendiente de formalización".

---

## 9. Planificación y control de versiones

- Trello con las tareas EP4 por integrante: `docs/tareas_trello_ep4.csv`.
- Plan de commits y subida a GitHub: `docs/GUIA_GITHUB_EP4.md`.
- Preguntas probables de la defensa: `docs/DEFENSA_EP4.md`.
