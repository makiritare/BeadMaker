# BeadMaker

Version en espanol: [README en español](#readme-en-espanol)

BeadMaker is an Android beadweaving pattern editor built with Jetpack Compose. It focuses on fast sketching, stitch-aware layouts, template-assisted tracing, and local-first pattern storage in a single-screen workflow that works well on phones and tablets.

## Overview

The app is designed for planning bead patterns directly on device:

- Paint patterns on a configurable grid.
- Switch between square, peyote, and brick-style layouts.
- Trace over a photo or imported reference image with adjustable opacity.
- Move, rotate, and zoom the template layer, or move and zoom the board itself.
- Save a working draft locally or export a portable `.bm` pattern file.

No account, network sync, or backend service is required.

## Feature Set

### Stitch layouts

- Square Grid
- Peyote (1-Drop)
- Peyote (2-Drop)
- Peyote (3-Drop)
- Brick Grid

Staggered layouts are rendered with row offsets so the editor matches the structure of the target stitch instead of treating everything like pixel art.

### Editing tools

- Tap-to-paint cells in paint mode
- Eraser toggle for quick corrections
- Undo and redo for board edits and grid-setting changes
- Recent color strip inside the palette dialog
- Two bead render styles: `Circle` and `Rounded rectangle`

### Grid controls

- Adjustable grid size from `8x8` to `64x64`
- Independent width and height sliders
- Resize anchoring from left or right, and top or bottom
- Existing beads are preserved when resizing wherever the old and new bounds overlap
- Grid clear action

### Template workflow

- Import a template image from the system photo picker
- Capture a template photo with the device camera
- Adjust template opacity from `0.1` to `1.0`
- Enter template adjustment mode to pan, rotate, and pinch-zoom the image
- Remove or reset the template transform at any time

Template images are copied into app cache storage so the editor can keep using them after import.

### Board navigation

- Separate grid adjustment mode for panning and zooming the working board
- Pinch-to-zoom with pan support
- Reset board transform

### File actions

- `Save`: writes the current board snapshot to the app's private storage
- `Load`: restores that saved in-app snapshot
- `Export`: writes the current pattern to a `.bm` file through the Android document picker

`Save` and `Load` act as a quick local save slot. `Export` produces a portable text file intended for sharing, backup, or future tooling.

### UI and quality-of-life

- Material 3 Compose UI
- Custom light and dark color schemes
- Bead-style adaptive launcher icon with monochrome themed variant
- Edge-to-edge layout
- English and Spanish string resources
- Local state restoration across configuration changes via `rememberSaveable`

## How To Use

1. Pick a paint color from the color button in the top bar.
2. Tap cells in `Paint` mode to build the pattern.
3. Open `Tools` to change stitch type, bead shape, or grid dimensions.
4. Import or photograph a template if you want to trace a reference.
5. Switch to `Template` mode to align, rotate, and scale the image behind the grid.
6. Switch to `Grid` mode if you need to reposition or zoom the whole board.
7. Use `Files` to save, load, or export the pattern.

## Pattern Format

Exported patterns use a plain UTF-8 text format with the `.bm` extension. The file stores:

- format version
- grid width and height
- stitch mode id
- bead shape id
- flattened bead color indices

Current format version: `1`

Example:

```text
beadmaker_format=1
grid_columns=16
grid_rows=16
stitch_mode_id=square
bead_shape_id=circle
beads=-1,-1,0,0,...
```

`-1` represents an empty cell.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Coil Compose
- AndroidX Activity Result APIs
- FileProvider for camera capture output

## Project Layout

- `app/src/main/java/com/example/beadmaker/MainActivity.kt`: app entry point and theming
- `app/src/main/java/com/example/beadmaker/ui/screens/BeadEditorScreen.kt`: main editor UI
- `app/src/main/java/com/example/beadmaker/ui/state/BeadEditorState.kt`: editor state, persistence, resize logic, export format
- `app/src/main/java/com/example/beadmaker/ui/components/BeadGrid.kt`: stitch-aware grid rendering
- `app/src/test/java/com/example/beadmaker/ui/state/BeadEditorStateTest.kt`: unit tests for resize and serialization behavior

## Requirements

- Android Studio with Android SDK `36`
- Android device or emulator running Android `7.0` or newer (`minSdk 24`)
- Gradle support for:
  - Android Gradle Plugin `9.2.0`
  - Kotlin Compose plugin `2.2.20`

Using the JDK bundled with current Android Studio builds is the safest option.

## Build And Run

### Android Studio

1. Clone the repository:

   ```bash
   git clone https://github.com/makiritare/BeadMaker.git
   cd BeadMaker
   ```

2. Open the project in Android Studio.
3. Let Gradle sync finish.
4. Run the `app` configuration on an emulator or physical device.

### Command line

Build a debug APK:

```bash
./gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew test
```



## License

Released under the MIT License. See [LICENSE](LICENSE).

## README en espanol

BeadMaker es un editor de patrones de tejido con cuentas para Android, construido con Jetpack Compose. Se centra en el bocetado rapido, los diseños adaptados al tipo de puntada, el calco asistido con plantillas y el almacenamiento local en un flujo de una sola pantalla que funciona bien en telefonos y tablets.

### Resumen

La aplicacion esta pensada para planificar patrones directamente en el dispositivo:

- Pintar patrones sobre una cuadricula configurable.
- Cambiar entre diseños cuadrados, peyote y tipo brick.
- Calcar una foto o una imagen de referencia importada con opacidad ajustable.
- Mover, girar y hacer zoom sobre la capa de plantilla, o mover y hacer zoom sobre el tablero.
- Guardar un borrador local o exportar un archivo de patron `.bm`.

No requiere cuenta, sincronizacion en red ni servicios backend.

### Funciones

#### Diseños de puntada

- Cuadricula cuadrada
- Peyote (1-Drop)
- Peyote (2-Drop)
- Peyote (3-Drop)
- Cuadricula brick

Los diseños escalonados se renderizan con desplazamientos por fila para que el editor coincida con la estructura real de la puntada y no trate todo como si fuera pixel art.

#### Herramientas de edicion

- Toque para pintar celdas en modo pintura
- Alternar borrador para correcciones rapidas
- Undo y redo para cambios del tablero y de la cuadricula
- Franja de colores recientes dentro del dialogo de paleta
- Dos estilos de cuenta: `Circle` y `Rounded rectangle`

#### Controles de cuadricula

- Tamano ajustable de `8x8` a `64x64`
- Deslizadores independientes para ancho y alto
- Anclaje de redimensionado desde izquierda o derecha, y desde arriba o abajo
- Conservacion de cuentas existentes al redimensionar cuando coinciden los limites
- Accion para limpiar la cuadricula

#### Flujo de plantilla

- Importar una imagen desde el selector de fotos del sistema
- Tomar una foto con la camara del dispositivo
- Ajustar la opacidad de la plantilla entre `0.1` y `1.0`
- Entrar en modo de ajuste para mover, girar y hacer pinch-to-zoom sobre la imagen
- Quitar o reiniciar la transformacion de la plantilla en cualquier momento

Las imagenes de plantilla se copian al cache de la app para que el editor pueda seguir usandolas despues de importarlas.

#### Navegacion del tablero

- Modo separado para ajustar el tablero con paneo y zoom
- Pinch-to-zoom con soporte de desplazamiento
- Reinicio de la transformacion del tablero

#### Acciones de archivo

- `Save`: escribe el estado actual del tablero en el almacenamiento privado de la app
- `Load`: restaura ese guardado rapido interno
- `Export`: escribe el patron actual en un archivo `.bm` mediante el selector de documentos de Android

`Save` y `Load` funcionan como una ranura de guardado rapido local. `Export` genera un archivo de texto portable pensado para compartir, respaldar o usar con futuras herramientas.

#### UI y calidad de vida

- UI en Compose Material 3
- Esquemas de color claros y oscuros personalizados
- Icono adaptive estilo cuentas con variante monocromatica para temas
- Diseño edge-to-edge
- Recursos de texto en ingles y espanol
- Restauracion de estado local entre cambios de configuracion mediante `rememberSaveable`

### Como usar

1. Elige un color desde el boton de color en la barra superior.
2. Toca las celdas en modo `Paint` para construir el patron.
3. Abre `Tools` para cambiar el tipo de puntada, la forma de la cuenta o las dimensiones de la cuadricula.
4. Importa una plantilla o toma una foto si quieres calcar una referencia.
5. Cambia a modo `Template` para alinear, girar y escalar la imagen detras de la cuadricula.
6. Cambia a modo `Grid` si necesitas reposicionar o hacer zoom del tablero completo.
7. Usa `Files` para guardar, cargar o exportar el patron.

### Formato de patron

Los patrones exportados usan un formato de texto plano UTF-8 con extension `.bm`. El archivo guarda:

- version del formato
- ancho y alto de la cuadricula
- identificador del modo de puntada
- identificador de la forma de la cuenta
- indices de color de las cuentas en una lista aplanada

Version actual del formato: `1`

Ejemplo:

```text
beadmaker_format=1
grid_columns=16
grid_rows=16
stitch_mode_id=square
bead_shape_id=circle
beads=-1,-1,0,0,...
```

`-1` representa una celda vacia.

### Stack tecnico

- Kotlin
- Jetpack Compose
- Material 3
- Coil Compose
- APIs de resultados de actividad de AndroidX
- FileProvider para la salida de captura de camara

### Estructura del proyecto

- `app/src/main/java/com/example/beadmaker/MainActivity.kt`: punto de entrada y tema de la app
- `app/src/main/java/com/example/beadmaker/ui/screens/BeadEditorScreen.kt`: UI principal del editor
- `app/src/main/java/com/example/beadmaker/ui/state/BeadEditorState.kt`: estado del editor, persistencia, logica de redimensionado y formato de exportacion
- `app/src/main/java/com/example/beadmaker/ui/components/BeadGrid.kt`: renderizado de cuadricula segun el tipo de puntada
- `app/src/test/java/com/example/beadmaker/ui/state/BeadEditorStateTest.kt`: pruebas unitarias para redimensionado y serializacion

### Requisitos

- Android Studio con Android SDK `36`
- Dispositivo Android o emulador con Android `7.0` o superior (`minSdk 24`)
- Soporte de Gradle para:
  - Android Gradle Plugin `9.2.0`
  - Kotlin Compose plugin `2.2.20`

Usar el JDK incluido en las versiones actuales de Android Studio es la opcion mas segura.

### Compilar y ejecutar

#### Android Studio

1. Clona el repositorio:

   ```bash
   git clone https://github.com/makiritare/BeadMaker.git
   cd BeadMaker
   ```

2. Abre el proyecto en Android Studio.
3. Espera a que termine la sincronizacion de Gradle.
4. Ejecuta la configuracion `app` en un emulador o dispositivo fisico.

#### Linea de comandos

Compilar un APK de debug:

```bash
./gradlew assembleDebug
```

Ejecutar pruebas unitarias:

```bash
./gradlew test
```



### Licencia

Publicado bajo la licencia MIT. Consulta [LICENSE](LICENSE).
