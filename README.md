# Induztek Terreno

**Formalización inmediata de protocolos de prueba eléctrica en terreno**
Aplicación móvil Android (Kotlin + Jetpack Compose + Material 3 + MVVM + SQLite) · DSY1105 Desarrollo de Aplicaciones Móviles · Duoc UC · Encargo (Evaluación Parcial 2)

> Todos los datos de la aplicación son **ficticios** (Anexo 9 del caso). No contiene información real de Induztek Ingeniería SpA ni de terceros.

---

## 1. Integrantes

| Integrante | Rol en el equipo | GitHub |
|---|---|---|
| Nicolás Rojas | Desarrollo · Integrante A | `@usuario-github` *(completar)* |
| Vicente Arancibia | Desarrollo · Integrante B | `@usuario-github` *(completar)* |

**Docente:** Víctor Rosendo · **Grupo N°:** 17 · **Sección:** *[completar]*
**Planificación (Trello):** *[pegar enlace público del tablero]*

---

## 2. Problema y solución

Hoy, cuando un técnico de Induztek prueba un transformador, relé, celda, interruptor o cable en la planta del cliente, anota los valores en papel (formulario FT-03). El protocolo oficial se genera **entre 5 y 15 días hábiles después**, con errores de transcripción (kΩ/MΩ, números invertidos), sin trazabilidad del instrumento usado y sin posibilidad de comparar con pruebas anteriores. El cliente se va de la visita **sin ningún respaldo**.

**Induztek Terreno** permite registrar cada prueba en el celular o tablet **en el momento**, incluso sin señal:

- Calcula al instante el resultado de cada prueba (Aprobado / Observado / Rechazado) y el resultado global del equipo.
- Compara con la prueba anterior del mismo equipo y **destaca las variaciones significativas (> 25 %)**.
- Bloquea instrumentos con calibración vencida.
- Al cerrar la visita genera el **Acta de prueba en terreno** ("Pendiente de formalización") en PDF para compartir, y registra quién la recibió.
- El ingeniero revisa y **formaliza** (o devuelve con observaciones), con historial de quién hizo qué y cuándo.
- El **usuario cliente** consulta las actas, los protocolos y la evolución de los equipos de *su* empresa.

---

## 3. Cómo ejecutar el proyecto

**Requisitos:** Android Studio (versión reciente compatible con AGP 9.4), JDK incluido en Android Studio, emulador o teléfono con **Android 8.0 (API 26) o superior**.

1. Clonar el repositorio o descomprimir el .zip.
2. En Android Studio: **File › Open** y elegir la carpeta `InduztekTerreno` (la que contiene `settings.gradle.kts`).
3. Esperar a que termine **Gradle Sync** (descarga dependencias la primera vez).
4. Seleccionar un emulador (recomendado: Pixel con API 34 o superior, **con Google Play** para tener app de cámara y GPS simulado).
5. Presionar **Run ▶**.
6. Ingresar con alguna cuenta de demostración (la pantalla de login tiene botones de acceso rápido):

| Perfil | Correo | Contraseña |
|---|---|---|
| Técnico de terreno (P-01) | `mgodoy@induztek.ejemplo` | `Induztek2026` |
| Ingeniera de pruebas (P-02) | `acontreras@induztek.ejemplo` | `Induztek2026` |
| Jefe técnico (P-03) | `respinoza@induztek.ejemplo` | `Induztek2026` |
| Usuario cliente (P-04, Envasadora del Pacífico) | `mfuentes@envpacifico.ejemplo` | `Induztek2026` |

> Las demás cuentas del Anexo 9 (Javiera, Carlos, Diego, Sebastián, Tomás, Patricio) usan la misma contraseña.

**Pruebas unitarias:** clic derecho en `app/src/test/java` › *Run 'Tests in ...'* (46 pruebas de reglas de negocio y validaciones).

**Probar sin conexión:** activar el *modo avión* en el emulador: aparece el aviso "Sin conexión · todo se guarda en el teléfono" y todo el flujo de terreno sigue funcionando.

**Probar el GPS en el emulador:** *Extended controls (…) › Location*, fijar un punto y presionar *Set location*.

---

## 4. Funcionalidades implementadas

### Por requerimiento del caso

| Requerimiento | Dónde se ve en la app |
|---|---|
| RF-01 Clientes e instalaciones | Más › Clientes › Nuevo cliente / Nueva instalación (con GPS) |
| RF-02 / HU-01 Equipos con TAG y placa | Instalación › Registrar equipo. TAG repetido → aviso y "Usar el equipo registrado" |
| RF-03 / HU-02 Iniciar protocolo | Visita › Agregar equipo → protocolo en **Borrador** |
| RF-04 / RN-05 Pruebas según tipo de equipo | Protocolo › tarjetas por prueba (aislación, TTR, devanados, contactos, relé) |
| RF-05 / RN-11 / HU-04 Instrumento | Formulario de prueba: instrumentos vencidos se rechazan informando la fecha |
| RF-06 / HU-05 Resultado inmediato | Tarjeta "Resultado en vivo" (IP, % error, % de placa, % diferencia) |
| RF-07 / RN-14 Acta en terreno | Visita › Cerrar visita › Ver y compartir el acta (PDF "Pendiente de formalización") |
| RF-08 / HU-08 Recepción | Visita cerrada › Registrar quién recibe el acta (RUT validado) |
| RF-09 / RNF-02 / RNF-03 Sin conexión | Todo se guarda en SQLite al instante; aviso visual de "sin conexión" |
| RF-10 / HU-11 Historial del equipo | Equipo › Historial de protocolos (más reciente primero) |
| RF-11 / HU-06 / RN-09 Comparación | Equipo › Evolución (gráfico + % de variación) y en el formulario de prueba |
| RF-12 / HU-10 Formalizar u observar | Protocolo › Formalizar / Devolver con observaciones (solo ingeniero o jefe) |
| RF-13 Estado y filtros | Pestaña Protocolos: filtros por estado, cliente, técnico y atrasados |
| RF-14 Identificación | Login con contraseña cifrada (hash + sal) y sesión recordada |
| RF-15 / RF-16 / RN-18 / RN-19 Cliente | Cuenta cliente: solo su empresa; ve "Pendiente de formalización"; no ve borradores ni anulados |
| RF-17 Observaciones por equipo | Protocolo › Observaciones del equipo |
| RF-18 Resumen de visita | Visita › Aprobados / Observados / Rechazados |
| RF-19 Instrumentos por vencer | Más › Instrumentos (vencido / vence en ≤ 30 días) e indicador en Inicio |
| RF-20 Buscar equipo | Pestaña Equipos: por TAG, Nº de serie o instalación |
| RF-21 Indicadores | Inicio: pendientes, atrasados (RN-16) y promedio de días hasta formalizar |
| RF-22 Personal | Más › Personal y cuentas: crear y activar/desactivar (no se borra) |
| RN-02 Número único sin conexión | `PT-AAAA-NNNN-DDDD` (DDDD = código del dispositivo) + UUID |
| RN-04 Estados y reemplazo | Máquina de estados + trigger SQLite: un Formalizado no se modifica; "Emitir reemplazo" |
| RN-17 Edición del técnico | Solo el autor edita, y solo en Borrador u Observado |
| Aclaraciones 2, 4, 5, 7, 9 | "No realizada" con motivo · previa/posterior al mantenimiento · 1 protocolo por equipo y visita · ingeniero a cargo obligatorio · último protocolo disponible sin señal |

### Por perfil

- **Técnico:** crea visitas, registra equipos, inicia protocolos, registra mediciones, cierra la visita, entrega el acta y registra la recepción.
- **Ingeniero:** lo mismo que el técnico, más formalizar, observar y anular; debe comentar las variaciones significativas antes de formalizar.
- **Jefe técnico:** gestiona instrumentos y calibraciones, personal y cuentas de cliente; formaliza y anula.
- **Cliente:** solo consulta su empresa: instalaciones, equipos, actas, protocolos y evolución.

---

## 5. Arquitectura (MVVM) y estructura del proyecto

```
UI (Compose)  ──eventos──▶  ViewModel (StateFlow<UiState>)  ──▶  Repositorio  ──▶  SQLite / SharedPreferences
      ▲                              │                                  │
      └────── estado observado ──────┘          Dominio (Kotlin puro): reglas RN, validaciones, catálogos
```

```
app/src/main/java/cl/duocuc/induztek/
├── MainActivity.kt              # única Activity (single-activity)
├── AppContenedor.kt             # inyección de dependencias manual + clase Application
├── domain/                      # Kotlin PURO (sin Android) → probado con JUnit
│   ├── model/                   # catálogos (RN-05), entidades (Anexo 8), roles y permisos (Anexo 7)
│   ├── reglas/                  # EvaluadorPruebas (RN-06..10), ComparadorHistorico (RN-09),
│   │                            # MaquinaEstados (RN-04, RN-17), Plazos (RN-16), instrumentos (RN-11)
│   ├── validacion/              # Validadores centralizados, ValidadorRut (RN-03), HashContrasena
│   └── util/Formato.kt          # formatos chilenos (coma decimal, dd-MM-aaaa)
├── data/
│   ├── local/                   # InduztekDbHelper (esquema 3FN), DatosSemilla (Anexo 9), SesionPreferencias
│   └── repository/              # un repositorio por agregado; consultas en Dispatchers.IO
├── nativo/                      # cámara (archivos), GPS, vibración, conectividad, PDF del acta
├── viewmodel/                   # un ViewModel por pantalla + UiState inmutable
├── navigation/                  # rutas y NavHost con transiciones animadas
└── ui/
    ├── theme/                   # colores, tipografía, medidas (Material 3, claro/oscuro)
    ├── components/              # campos con validación visual, chips, tarjetas, animaciones, gráfico
    └── screens/                 # auth, inicio, clientes, equipos, visitas, protocolos, admin, perfil
```

**Decisiones clave**

- **Validación desacoplada (IE 2.2.1):** los composables no validan. Cada ViewModel llama a `domain/validacion/Validadores.kt` y expone `Campo(valor, error, tocado)`; la UI solo pinta borde rojo, ícono ⚠ / ✓ y mensaje.
- **Reglas de negocio en el dominio:** `EvaluadorPruebas`, `MaquinaEstados`, `ComparadorHistorico` no dependen de Android y se prueban con JUnit.
- **La BD es la única fuente de verdad:** cada escritura avisa a `NotificadorCambios` y las pantallas abiertas se recargan solas.
- **Seguridad en profundidad:** RN-18 (cliente solo ve su empresa) se aplica **en las consultas SQL**, no solo en la interfaz; triggers impiden modificar protocolos formalizados y crear cuentas cliente sin empresa.

---

## 6. Persistencia local

**SQLite (`induztek.db`, `SQLiteOpenHelper`)**: 18 tablas normalizadas según el MER de referencia del caso.

| Grupo | Tablas |
|---|---|
| Seguridad | `rol`, `permiso`, `rol_permiso` (permisos como datos), `usuario`, `cliente` |
| Ubicación | `region`, `comuna` (la región se obtiene por la comuna, 3FN) |
| Activos | `instalacion`, `equipo` (UNIQUE instalación+TAG), `instrumento`, `calibracion` (historial 1:N) |
| Terreno | `visita`, `visita_participante`, `recepcion_acta` (1:0..1), `protocolo`, `historial_estado`, `prueba`, `medicion` (catálogo de parámetros, no columnas fijas) |

Restricciones en la propia BD: claves foráneas activas, `CHECK` de estados/resultados/rangos, índice único parcial (1 protocolo por equipo y visita), triggers de inmutabilidad (RN-04) y de pertenencia del cliente (RN-18). Las contraseñas se guardan como **SHA-256 iterado con sal**, nunca en texto plano.

**SharedPreferences (`induztek_sesion`)**: usuario con sesión iniciada, último correo usado, código del dispositivo (RN-02) y preferencia de tema.

> El caso indica "Room/SQLite". Se usó **SQLite con `SQLiteOpenHelper`** (lo trabajado en la asignatura) para no agregar el procesador KSP al build de AGP 9. La capa de repositorios permite migrar a Room sin tocar ViewModels ni pantallas.

---

## 7. Recursos nativos (IE 2.4.1)

| Recurso | Permiso | Uso en la app y problema que resuelve |
|---|---|---|
| **Cámara** | `CAMERA` (en tiempo de ejecución) | Foto de la **placa de características** al registrar el equipo o desde su detalle. Evita errores al copiar la placa a mano (PB-03). Se guarda en almacenamiento privado y se comparte con la cámara mediante `FileProvider`. |
| **GPS** | `ACCESS_FINE/COARSE_LOCATION` (en tiempo de ejecución) | Coordenadas de cada **instalación** (llegar a faenas rurales) y del **cierre de visita**, que quedan impresas en el acta como evidencia de emisión en terreno. Funciona sin internet. |
| **Vibración** | `VIBRATE` (normal) | Confirmación táctil distinta para éxito, resultado Observado y error: útil con guantes, ruido y poca luz (RNF-04). |
| **Estado de red** | `ACCESS_NETWORK_STATE` (normal) | Aviso permanente de "Sin conexión" para dar tranquilidad: nada se pierde (RNF-02). |
| PDF + compartir | — | El acta se genera con `PdfDocument` y se comparte con la hoja nativa de Android (correo, mensajería, Bluetooth). |

Si el usuario niega un permiso, la app **sigue funcionando** (la foto y las coordenadas son opcionales) y lo informa con un mensaje.

---

## 8. Formularios y validaciones (IE 2.1.2)

Todos los campos tienen **ícono descriptivo**, **borde rojo + ícono ⚠ + mensaje** cuando hay error y **✓ verde** cuando el valor es válido. Al intentar guardar con errores, el formulario se **sacude** y el teléfono vibra.

| Formulario | Validaciones principales |
|---|---|
| Login | Correo con formato válido · contraseña obligatoria · credenciales y usuario activo |
| Cliente | Razón social · **RUT con dígito verificador (RN-03)** y no repetido · giro · contacto con nombre y apellido · teléfono chileno · correo |
| Instalación | Nombre único por cliente · ubicación · región/comuna obligatoria · observaciones (máx. 300) |
| Equipo | **TAG único en la instalación (RN-01, HU-01)** · tipo · marca · Nº de serie · año 1950–actual · datos de placa según el tipo (kVA, kV, A, µΩ con rangos) |
| Visita | **OT con formato `OT-AAAA-NNNN`** y no repetida · instalación · fecha · tipo · ingeniero a cargo · al menos un participante |
| Medición | Elemento · instrumento **vigente a la fecha (RN-11)** · cada valor en su **rango posible** (no negativos, HU-03) · MΩ/GΩ con conversión · entero para el tap · resultado manual en devanados |
| Protocolo | Temperatura −30 a 60 °C · humedad 0–100 % · motivo obligatorio (≥ 10 caracteres) para observar, anular o "No realizada" · comentario obligatorio si hay variación significativa |
| Recepción del acta | Nombre y apellido · RUT válido · cargo |
| Instrumento / calibración | Categoría · Nº de serie único · calibración no futura · **vencimiento posterior a la calibración** |
| Cuenta de usuario | Nombre · RUT · correo único · teléfono · cargo · contraseña ≥ 8 con letras y números · empresa obligatoria si es cliente |

---

## 9. Animaciones (IE 2.2.2)

| Animación | Dónde | Propósito |
|---|---|---|
| Transición deslizar + fundido entre pantallas | `AppNavegacion` (NavHost) | Fluidez y orientación espacial |
| Sacudida del formulario + vibración | Todos los formularios al fallar la validación | Retroalimentación de error |
| Íconos ⚠ / ✓ con `Crossfade` | Cada campo | Estado del campo al escribir |
| Resultado "en vivo" con `AnimatedContent` (escala) y color animado | Formulario de prueba, chips | El técnico ve el resultado "encenderse" (HU-05) |
| Campos de placa que aparecen/desaparecen (`AnimatedVisibility`) | Registro de equipo | Solo se piden los datos que aplican al tipo |
| Aparición escalonada de tarjetas + `animateItem` | Listas | Fluidez al cargar y filtrar |
| Contadores animados | Indicadores de Inicio y resumen de visita | Lectura rápida de cambios |
| Gráfico de evolución que se dibuja de izquierda a derecha | Detalle de equipo | Ver la tendencia del equipo (RF-11) |
| Pulso de la alerta de variación significativa | Protocolo | Llama la atención del ingeniero (RN-09) |
| Botones que se encogen al presionar + spinner animado | Botones principales | Respuesta táctil y estado "guardando" |
| Logo que "respira" y banner de conexión expandible | Login, todas las pantallas | Vida visual y aviso de red |

---

## 10. Guion de demostración (5 minutos)

1. **Técnico (Matías)** → Inicio: visita abierta **OT-2026-1240 · Faena Til Til** ("sin señal en toda la faena"). Activar el **modo avión**.
2. Visita › **Agregar equipo** › TR-SS-01 → protocolo en Borrador. Registrar *Resistencia de aislación* AT-T: intentar el **Megóhmetro 10 kV** → rechazado ("calibración vencida el 10-10-2026"). Elegir el 5 kV, ingresar 5 kV, 4.000 MΩ y **9 GΩ** (selector GΩ) → resultado **Aprobado (IP 2,25)** al instante. "Guardar y siguiente".
3. Marcar *Resistencia de devanados* como **No realizada** (motivo). Intentar **Pasar a Registrado** sin temperatura → errores; completar 27 °C / 18 % → Registrado.
4. **Cerrar visita** (si el GPS está activo, se estampan las coordenadas) → **Ver y compartir el acta** (PDF "Pendiente de formalización") → **Registrar recepción** (probar un RUT con dígito verificador incorrecto).
5. Agregar un equipo nuevo con TAG **TR-SS-01** → aviso "ya existe" (HU-01). Registrar uno nuevo con **foto de la placa** (cámara).
6. **Ingeniera (Andrea)** → Protocolos › **PT-2026-0401 (TR-01)**: alerta **Variación significativa −37,8 %** (pulsante). Intentar formalizar sin comentario → bloqueado; comentar → **Formalizar**. Ver la trazabilidad. Abrir el equipo TR-01 → **gráfico de evolución** 12.500 → 9.800 → 6.100 MΩ.
7. **Jefe técnico (Ricardo)** → Instrumentos: micro-ohmímetro vencido → registrar nueva calibración. Personal: desactivar un técnico e intentar ingresar con él.
8. **Cliente (Marcela)** → solo ve Envasadora del Pacífico: TR-01 y TR-02; PT-2026-0402 aparece como **"Pendiente de formalización"**; no puede editar nada.

---

## 11. Calidad y verificación

- **46 pruebas unitarias JUnit** (`app/src/test`) sobre el dominio: criterios RN-06 a RN-10 con los datos del Anexo 9 y del FT-03, máquina de estados RN-04/RN-17, permisos del Anexo 7, plazos RN-16, instrumentos RN-11/RF-19, visibilidad RN-19, numeración RN-02, RUT RN-03 y validaciones de formularios.
- Las capas de dominio, datos, recursos nativos y ViewModels se compilaron contra la API de Android, y los repositorios se ejecutaron sobre un motor SQLite real con **51 verificaciones de integración** del flujo completo de terreno (crear protocolo → mediciones → cierre → acta → recepción → formalización → reemplazo → vista del cliente).

---

## 12. Alcance de esta entrega

Esta es la entrega intermedia (Encargo) del proyecto que se completará en la Evaluación Final Transversal. **Queda para la EFT** la parte de backend que pide el caso: microservicios **Spring Boot + API REST + pruebas unitarias**, integrados con **Retrofit** para sincronizar los protocolos con la oficina (la columna `sincronizado` y el `uuid` de cada protocolo ya están preparados para esa sincronización). Fuera de alcance según el caso: facturación, agenda de visitas, firma electrónica avanzada, importación desde instrumentos (FA-01 a FA-11).

---

## 13. Planificación y control de versiones

- Tablero Trello con la distribución de tareas por integrante: ver `docs/PLANIFICACION.md` (incluye `docs/tareas_trello.csv` para importar).
- Flujo de trabajo en GitHub (ramas, commits y issues): ver `docs/GUIA_GITHUB.md` y `scripts/crear_issues.sh`.
- Guía para la defensa individual: `docs/DEFENSA.md`.
