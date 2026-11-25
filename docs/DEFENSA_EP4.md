# Defensa EP4: respuestas cortas

**¿Qué microservicios hicieron?**
Tres: `ms-usuarios` (cuentas y login), `ms-activos` (clientes, instalaciones, equipos e instrumentos) y `ms-protocolos` (visitas y protocolos). Cada uno tiene su propia base MySQL, se registra en Eureka y la app entra por el API Gateway en el puerto 8081.

**¿Para qué sirve Eureka? ¿Y el Gateway?**
Eureka es la "guía telefónica": cada servicio se anota con su nombre. El Gateway es la puerta única. La app llama siempre a `:8081/api/...` y el Gateway reenvía la petición con `lb://ms-xxx` al servicio que corresponde.

**¿Cómo se conecta la app?**
Con Retrofit + Gson. Las interfaces están en `data/remote/Apis.kt`. `LlamadaSegura` convierte las excepciones en `Operacion.Exito` / `Operacion.Fallo` con mensajes en español (401, 403, 404, 409, sin red). Desde el emulador se usa `10.0.2.2`, que apunta al `localhost` del PC.

**¿Y si no hay internet?**
Las visitas y los protocolos se guardan en SQLite con un UUID. Triggers los marcan como pendientes (`sincronizado = 0`). Al volver la red, `SincronizacionRepositorio` los envía con `PUT /{uuid}`, que es idempotente: enviarlo dos veces no duplica nada. Los clientes, equipos y cuentas se editan en línea y se guarda una copia local.

**¿Qué API externa usaron y para qué?**
Open-Meteo (gratis, sin API key). Con las coordenadas GPS de la instalación muestra el clima actual. El botón "Usar en el protocolo" llena la temperatura y la humedad, que son obligatorias para registrar la prueba. Si falla, solo muestra un aviso y nada se bloquea.

**¿Quién puede crear qué cuentas?**
Administrador: todas. Ingeniero: técnico y cliente. Técnico y cliente: ninguna. Se valida dos veces: en la app (`RolCodigo.rolesQuePuedeCrear`) y en el servidor (`Rol.puedeCrear`, que responde 403). Solo el administrador elimina cuentas y no puede borrarse a sí mismo ni dejar el sistema sin administradores.

**¿Cómo se recupera la contraseña?**
`POST /recuperar` genera un código de 6 dígitos válido por 15 minutos. Como no hay servidor de correo, en modo demo se devuelve en `codigoDemo` y se muestra en pantalla. Luego `POST /restablecer` lo valida y guarda la nueva contraseña con PBKDF2 + sal.

**¿Qué prueban las pruebas unitarias?**
App (114): ViewModels con fuentes falsas y `kotlinx-coroutines-test` (login, recuperar clave, cambiar clave, perfil, cuentas, clima), repositorios remotos con APIs Retrofit falsas y reglas del dominio. Backend (47): servicios con Mockito (roles, contraseñas, RUT, borrados con dependencias, protocolo formalizado inmutable).

**¿Cómo se firma el APK?**
`keystore/induztek-release.jks` (alias `induztek`), con las claves en `keystore.properties`. `signingConfigs.release` las lee y `buildTypes.release` usa esa firma. Se genera con *Build › Generate Signed APK* o `./gradlew assembleRelease`.

**¿Cómo agregarías un campo nuevo a la ficha técnica?**
Se agrega un `CampoTecnico` en `FichaTecnica.campos(tipo)`. El formulario, la validación, el detalle y la sincronización lo toman solos, porque viaja dentro del mapa `datosTecnicos`.

**¿Cómo agregarías un endpoint nuevo?**
En el microservicio: un método en el Service y un `@GetMapping`/`@PostMapping` en el Controller. En la app: el método en la interfaz de `Apis.kt`, la función en el `Remoto` y su uso en el repositorio.

**¿Por qué los datos de ejemplo ya no se cargan?**
Porque ahora los datos reales viven en el servidor. El teléfono solo trae catálogos fijos (regiones, comunas, roles y permisos) y el resto se descarga al sincronizar.
