# Planificación del encargo · Induztek Terreno

Tablero sugerido para **Trello** (o GitHub Projects). Encargo asignado en la **semana 6** y entregado en la **semana 11**.

**Grupo 17** · **Integrante A:** Nicolás Rojas · **Integrante B:** Vicente Arancibia · **Docente:** Víctor Rosendo

## Listas del tablero

`Backlog EFT` → `Por hacer` → `En progreso` → `En revisión` → `Hecho`

## Etiquetas

| Etiqueta | Color sugerido | Uso |
|---|---|---|
| Análisis | gris | Lectura del caso, requerimientos |
| Dominio | morado | Reglas de negocio, validaciones |
| Datos | azul | SQLite, repositorios, semilla |
| UI | verde | Pantallas, componentes, navegación |
| Nativo | naranjo | Cámara, GPS, vibración, red, PDF |
| Calidad | rojo | Pruebas |
| Gestión / Documentación | amarillo | Repo, README, entrega |

## Cronograma y distribución

| Semana | Nicolás Rojas (A) | Vicente Arancibia (B) |
|---|---|---|
| 6 | MER 3FN · análisis del caso | Repo + Trello · tema Material 3 y componentes |
| 7 | Catálogos, reglas RN-06..10, máquina de estados, validadores, pruebas JUnit | SQLiteOpenHelper, triggers, datos semilla |
| 8 | Login, sesión, hash de contraseñas | Repositorios · navegación · clientes, instalaciones y equipos |
| 9 | Protocolo, estados, formulario de medición, comparación histórica | Visitas: formulario OT, detalle, cierre |
| 10 | Vibración, red, acta PDF, recepción, vista cliente | Cámara, GPS, administración (instrumentos, personal) |
| 10–11 | Animaciones, pruebas en emulador, README (ambos) | |
| 11 | Entrega AVA grupal + enlace individual (ambos) | |

El detalle de cada tarjeta (descripción, responsable, semana, etiquetas y requerimientos del caso que cubre) está en **`tareas_trello.csv`**. Para cargarlo: en Trello, botón *Importar* (o power-up de CSV), o crear las tarjetas a mano copiando la tabla. Cada integrante debe **moverse sus propias tarjetas** a medida que avanza: la rúbrica (IE 2.3.2) evalúa actividad real y distribuida.

## Definición de "Hecho"

- Compila y corre en el emulador sin errores.
- Formulario con validación por campo (ícono + mensaje) y lógica en el ViewModel/dominio.
- Probado con al menos dos perfiles (por ejemplo técnico y cliente).
- Commit con mensaje técnico y la tarjeta movida a *Hecho*; issue cerrado con `Closes #N`.
