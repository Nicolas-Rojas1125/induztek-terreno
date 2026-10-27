# Guía para la presentación y defensa

## 1. Rúbrica → evidencia en el proyecto

| Indicador (peso) | Qué mostrar | Archivos |
|---|---|---|
| **IE 2.1.1** Interfaz estructurada y navegación (15 %) | Barra inferior por rol, `PantallaBase` común (barra superior + aviso de red + snackbar), jerarquía con `TarjetaSeccion`, botones de 56 dp, tema claro/oscuro | `ui/components/Marco.kt`, `Tarjetas.kt`, `navigation/AppNavegacion.kt`, `ui/theme/*` |
| **IE 2.1.2** Formularios con validación visual por campo (15 %) | 10 formularios: cada campo con ícono, ⚠ + mensaje o ✓; sacudida al fallar | `ui/components/CamposFormulario.kt`, todas las `*FormScreen` |
| **IE 2.2.1** Validación centralizada y desacoplada (10 %) | `Validadores` en el dominio; el ViewModel expone `Campo(valor, error, tocado)`; los composables no validan | `domain/validacion/Validadores.kt`, `viewmodel/BaseViewModel.kt` (clase `Campo`), cualquier `guardar()` |
| **IE 2.2.2** Animaciones funcionales (10 %) | Transiciones de navegación, sacudida, crossfade de íconos, resultado en vivo, campos que aparecen según el tipo, escalonado, contadores, gráfico animado, pulso | `ui/components/Animaciones.kt`, `GraficoTendencia.kt`, `Botones.kt`, `AppNavegacion.kt` |
| **IE 2.3.1** Modularidad, MVVM y persistencia (15 %) | Capas `domain` / `data` / `viewmodel` / `ui` / `nativo`; repositorios; SQLite 3FN con triggers; SharedPreferences; recarga automática con `NotificadorCambios` | `data/local/InduztekDbHelper.kt`, `data/repository/*`, `AppContenedor.kt` |
| **IE 2.3.2** GitHub y Trello (20 %) | Commits de ambos, PRs, issues cerrados, tablero con responsables | `docs/GUIA_GITHUB.md`, `docs/PLANIFICACION.md` |
| **IE 2.4.1** Recursos nativos (15 %) | Cámara (permiso en tiempo de ejecución + FileProvider), GPS (permiso + coordenadas en el acta), vibración, estado de red, PDF | `nativo/*`, `ui/components/ComponentesNativos.kt`, `AndroidManifest.xml` |

## 2. Preguntas probables y cómo responderlas

**¿Por qué MVVM?** Separa responsabilidades: la pantalla solo dibuja el `UiState` y envía eventos; el ViewModel mantiene el estado (`StateFlow`) y sobrevive a la rotación; el repositorio encapsula SQL. Así las reglas se prueban sin la interfaz (46 pruebas JUnit).

**¿Dónde está la validación y por qué no en el composable?** En `domain/validacion/Validadores.kt`, funciones puras que devuelven `null` (válido) o el mensaje. El ViewModel las llama en cada cambio (`Campo.cambiar`) y al guardar (`Campo.validar`). Si mañana cambia una regla, se cambia en un solo lugar y las pruebas lo detectan.

**¿Cómo se calcula el resultado de una prueba?** `EvaluadorPruebas.evaluar(...)`: IP = 10 min ÷ 1 min (RN-06), error TTR (RN-07), % del máximo de placa (RN-08), % de diferencia de tiempo del relé (RN-08b); devanados es manual (RN-08c). El global es el peor (RN-10), contando solo las realizadas y, si hubo mantenimiento, la medición posterior.

**¿Qué pasa si no hay señal?** Nada cambia: todo se guarda en SQLite al momento. El número del protocolo lleva el código del dispositivo (`PT-2026-0403-A7F3`) y un UUID, para que dos teléfonos sin conexión nunca generen el mismo número (RN-02). Para la EFT, la columna `sincronizado` permitirá enviarlos con Retrofit.

**¿Cómo se asegura que un cliente no vea datos de otro?** El filtro está en el **SQL** (`RepositorioBase.filtroEmpresa`): toda consulta de un usuario cliente agrega `AND id_cliente = ?`. Además no ve borradores ni anulados (RN-19). No depende de ocultar botones.

**¿Cómo se evita modificar un protocolo formalizado?** Dos barreras: la máquina de estados no ofrece la acción y un **trigger** de SQLite aborta cualquier `UPDATE` de un protocolo formalizado o la inserción/borrado de sus pruebas. Si hay un error, se emite un protocolo de reemplazo y el original queda marcado "Reemplazado por nº ___".

**¿Cómo se manejan los permisos de cámara y ubicación?** Se verifica con `ContextCompat.checkSelfPermission`; si no están, se piden con `rememberLauncherForActivityResult(RequestPermission)` solo cuando el usuario toca el botón. Si los niega, la app sigue funcionando y lo informa. La foto se guarda en almacenamiento privado y la cámara recibe un Uri temporal de `FileProvider`.

**¿Por qué esos recursos nativos?** El caso (C-13) pide justificarlos con un problema del negocio: la cámara evita errores al copiar la placa (PB-03); el GPS deja evidencia de que el acta se emitió en terreno y ayuda a llegar a faenas; la vibración ayuda a trabajar con guantes (RNF-04); el estado de red da tranquilidad sin señal (RNF-02).

**¿Por qué SQLiteOpenHelper y no Room?** Es lo trabajado en la asignatura y evita agregar KSP al build con AGP 9. El patrón Repository aísla el acceso a datos: migrar a Room no obliga a tocar ViewModels ni pantallas.

**¿Cómo se guardan las contraseñas?** SHA-256 iterado 10.000 veces con una sal aleatoria por usuario (`HashContrasena`). Nunca en texto plano. En el backend de la EFT se recomienda BCrypt.

**¿Qué falta para la EFT?** Backend Spring Boot + API REST con pruebas, sincronización con Retrofit y las mejoras que surjan de la retroalimentación de esta evaluación.

## 3. Datos útiles para la demo

- Visita abierta de hoy: **OT-2026-1240 · Faena Til Til** (TR-SS-01, INT-52-1) y **OT-2026-1238 · Planta Elevadora Sur** (TR-A).
- Instrumentos vencidos: **Megóhmetro 10 kV** (10-10-2026) y **Micro-ohmímetro** (20-08-2026).
- Variación significativa: **TR-01 · AT-T** 9.800 → 6.100 MΩ (−37,8 %) en **PT-2026-0401**.
- Protocolo observado por corregir: **PT-2026-0389** (Carlos Rondón, CEL-MT-03).
- Protocolos atrasados (RN-16): PT-2026-0401, PT-2026-0402 y PT-2026-0389.
