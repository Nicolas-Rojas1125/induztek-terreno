# Subida a GitHub · EP4 (desde el navegador)

Son **12 commits** alternados: 6 de Vicente y 6 de Nicolás. Cada lote es un `.zip` que ya viene con las carpetas en su ruta correcta.

## Cómo subir cada lote

1. Inicie sesión en GitHub con **la cuenta que indica la tabla**.
2. Descomprima el lote en una carpeta vacía (por ejemplo, `lote01`).
3. Abra el repositorio `induztek-terreno` → **Add file › Upload files**.
4. **Arrastre el contenido** de la carpeta del lote (las carpetas `app`, `backend`, `docs`, etc.), no la carpeta `lote01`. GitHub respeta las rutas y reemplaza los archivos que ya existen.
5. En *Commit changes*, pegue el mensaje de la tabla → **Commit directly to the `main` branch**.
6. Revise que el commit aparezca con su usuario y pase al siguiente lote.

> En Mac, los archivos ocultos (`.gitignore`, `.mvn`) se suben igual al arrastrar la carpeta. Si alguno no aparece, súbalo aparte en el mismo lote.

| Lote | Cuenta | Mensaje del commit |
|---|---|---|
| 01_V | Vicente | `feat(backend): agrega Eureka Server, API Gateway y docker-compose con MySQL` |
| 02_N | Nicolás | `feat(ms-activos): CRUD de clientes, instalaciones, equipos con ficha técnica e instrumentos` |
| 03_V | Vicente | `feat(ms-usuarios): cuentas por rol con login PBKDF2, recuperar y cambiar contraseña` |
| 04_N | Nicolás | `feat(ms-protocolos): visitas y protocolos por UUID con upsert idempotente y formalizado inmutable` |
| 05_V | Vicente | `feat(remoto): integra Retrofit con los microservicios y sincronización offline` |
| 06_N | Nicolás | `refactor(datos): quita datos de ejemplo, agrega ficha técnica, paginador y repositorios en línea` |
| 07_V | Vicente | `feat(viewmodel): cuentas por rol, recuperar clave, perfil, clima y listas paginadas` |
| 08_N | Nicolás | `style(ui): homogeniza colores con roles Material 3 y agrega componentes de listas, ficha y clima` |
| 09_V | Vicente | `feat(ui): pantallas de cuentas, recuperar clave, perfil, servidor y sincronización` |
| 10_N | Nicolás | `feat(ui): búsqueda, filtros y paginación; editar/eliminar; ficha técnica y clima en pantallas` |
| 11_V | Vicente | `test(app): pruebas de ViewModels, remotos con APIs falsas y dominio con coroutines-test` |
| 12_N | Nicolás | `build(release): firma del APK con keystore y documentación EP4` |

## Después de subir

1. Descargue el repositorio (**Code › Download ZIP**), ábralo en Android Studio y ejecute las pruebas y la app con Docker encendido.
2. Genere el **APK firmado** y saque las 2 capturas. Súbalas a `docs/capturas/` (`apk_firmado.png` y `keystore_jks.png`) con el commit `docs(readme): agrega capturas del APK firmado y del keystore`. Puede subirlas cualquiera de los dos.
3. En Trello, importe o copie las tarjetas de `docs/tareas_trello_ep4.csv` y muévalas a **Hecho**.
4. Suba a **AVA grupal** el mismo zip del repositorio. Cada integrante activa su **AVA individual**.
5. **No haga ningún cambio en el repositorio después de la fecha de entrega.**
