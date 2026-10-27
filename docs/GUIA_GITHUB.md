# Guía de GitHub para el equipo

La rúbrica (IE 2.3.2, 20 %) evalúa **commits distribuidos entre ambos integrantes, issues resueltos y tareas visibles**. Por eso cada integrante debe subir **su propia parte desde su propia cuenta**. Esta guía propone cómo hacerlo de forma ordenada.

> ⚠️ Reglas del encargo: repositorio **público**, con acceso al docente; **no hacer ningún cambio después de la fecha de entrega** (nota 1,0 automática); subir a AVA el **mismo** código del repositorio.

## 1. Crear el repositorio (Vicente, Integrante B)

1. GitHub › *New repository* › nombre `induztek-terreno` › **Public** › sin README (ya viene en el proyecto).
2. *Settings › Collaborators* › invitar al compañero/a y al docente.
3. En la carpeta del proyecto:

```bash
git init
git branch -M main
git remote add origin https://github.com/USUARIO/induztek-terreno.git
```

4. (Opcional) crear issues y etiquetas automáticamente con GitHub CLI: `bash scripts/crear_issues.sh USUARIO/induztek-terreno`

## 2. Ramas

- `main`: versión estable (la que se entrega).
- `develop`: integración.
- `feature/<tema>`: una rama por funcionalidad, que se une a `develop` con un *Pull Request* revisado por el otro integrante.

## 3. Formato de los mensajes de commit

Mensajes técnicos, en imperativo y con el tipo al inicio (Conventional Commits). Referenciar el issue para cerrarlo:

```
feat(dominio): agrega EvaluadorPruebas con criterios RN-06 a RN-08b
test(dominio): cubre historial TR-01 del Anexo 9 y valores del FT-03
fix(semilla): carga pruebas antes de formalizar para respetar trigger RN-04
```

Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`. Evitar mensajes como "cambios", "avance" o "listo".

## 4. Orden sugerido de commits por integrante

Cada integrante agrega (`git add`) y confirma solo los archivos de su parte, en este orden.

| # | Integrante (A = Nicolás, B = Vicente) | Archivos | Mensaje sugerido |
|---|---|---|---|
| 1 | B | `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, `gradlew*`, `gradle.properties`, `.gitignore`, `app/build.gradle.kts`, `app/.gitignore` | `chore: configura proyecto Android con Compose, Material 3 y Navigation` |
| 2 | B | `app/src/main/res/**`, `AndroidManifest.xml` | `feat(res): agrega íconos vectoriales, tema base, permisos y FileProvider` |
| 3 | B | `ui/theme/**` | `feat(ui): define tema Material 3 claro/oscuro con paleta de alto contraste` |
| 4 | A | `domain/model/**`, `domain/util/**` | `feat(dominio): modela catálogos RN-05, entidades del Anexo 8 y permisos del Anexo 7` |
| 5 | A | `domain/validacion/**` | `feat(dominio): centraliza validadores de formularios, RUT (RN-03) y hash de contraseñas` |
| 6 | A | `domain/reglas/**` | `feat(dominio): implementa criterios de resultado, comparación RN-09 y máquina de estados RN-04` |
| 7 | A | `app/src/test/**` | `test(dominio): agrega 46 pruebas unitarias de reglas de negocio y validaciones` |
| 8 | B | `data/local/**` | `feat(datos): crea esquema SQLite 3FN con triggers y datos semilla del Anexo 9` |
| 9 | B | `data/repository/**`, `AppContenedor.kt` | `feat(datos): agrega repositorios con filtro por empresa en SQL (RN-18)` |
| 10 | B | `ui/components/CamposFormulario.kt`, `Botones.kt`, `Tarjetas.kt`, `Retroalimentacion.kt`, `Marco.kt` | `feat(ui): agrega componentes con validación visual por campo` |
| 11 | A | `ui/components/Animaciones.kt`, `GraficoTendencia.kt` | `feat(ui): agrega animaciones de retroalimentación y gráfico de evolución` |
| 12 | A | `viewmodel/BaseViewModel.kt`, `AppViewModel.kt`, `LoginViewModel.kt`, `ui/screens/auth/**` | `feat(auth): implementa inicio de sesión con sesión persistente (RF-14)` |
| 13 | B | `navigation/**`, `MainActivity.kt`, `viewmodel/InicioViewModel.kt`, `ui/screens/inicio/**` | `feat(nav): agrega navegación por rol e indicadores de inicio (RF-21)` |
| 14 | B | `viewmodel/ClientesViewModels.kt`, `ui/screens/clientes/**` | `feat(clientes): registra clientes e instalaciones (RF-01)` |
| 15 | B | `viewmodel/EquiposViewModels.kt`, `ui/screens/equipos/**` | `feat(equipos): registra equipos con TAG único y muestra historial (RF-02, RF-10, HU-01)` |
| 16 | B | `viewmodel/VisitasViewModels.kt`, `ui/screens/visitas/VisitasScreens.kt` | `feat(visitas): crea visitas y valida cierre (RN-13, HU-07)` |
| 17 | A | `viewmodel/ProtocolosViewModels.kt`, `ui/screens/protocolos/ProtocolosScreen.kt`, `ProtocoloScreen.kt` | `feat(protocolos): gestiona estados por rol con trazabilidad (RF-12, RF-13)` |
| 18 | A | `viewmodel/PruebaFormViewModel.kt`, `ui/screens/protocolos/PruebaFormScreen.kt` | `feat(pruebas): registra mediciones con resultado en vivo e instrumento vigente (HU-04, HU-05)` |
| 19 | B | `nativo/ArchivosFoto.kt`, `nativo/ServicioUbicacion.kt`, `ui/components/ComponentesNativos.kt` | `feat(nativo): integra cámara y GPS con permisos en tiempo de ejecución` |
| 20 | A | `nativo/RetroalimentacionHaptica.kt`, `nativo/MonitorConectividad.kt` | `feat(nativo): agrega vibración háptica y aviso de trabajo sin conexión` |
| 21 | A | `nativo/GeneradorActaPdf.kt`, `ui/screens/visitas/ActaScreen.kt` | `feat(acta): genera acta PDF pendiente de formalización y la comparte (RF-07)` |
| 22 | B | `viewmodel/AdminViewModels.kt`, `ui/screens/admin/**` | `feat(admin): gestiona instrumentos, calibraciones y cuentas (RN-12, RF-22)` |
| 23 | A | `ui/screens/perfil/**` | `feat(perfil): agrega menú, permisos del usuario y selector de tema` |
| 24 | Ambos | `README.md`, `docs/**`, `scripts/**` | `docs: agrega README, planificación y guía de defensa` |

Después de cada bloque: `git push`, abrir el *Pull Request* hacia `develop`, que el otro integrante lo revise y lo apruebe, y cerrar el issue correspondiente. Al final, PR de `develop` a `main`.

## 5. Antes de entregar

- [ ] `README.md` con nombres reales, sección, docente y enlace a Trello.
- [ ] El proyecto compila desde un clon limpio (`File › Open` + Run).
- [ ] Todos los issues del encargo cerrados.
- [ ] Mismo código subido a AVA grupal (un integrante) + enlace individual activado por cada uno.
- [ ] No volver a hacer push hasta después de la evaluación.
