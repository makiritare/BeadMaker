# BeadMaker

BeadMaker is an Android beadweaving pattern editor built with Jetpack Compose. It focuses on fast sketching, stitch-aware layouts, template-assisted tracing, and local-first pattern storage in a single-screen workflow that works well on phones and tablets.

## Overview

The app is designed for planning bead patterns directly on device:

- Paint patterns on a configurable grid.
- Switch between square, peyote, and brick-style layouts.
- Trace over a photo or imported reference image with adjustable opacity.
- Move and zoom either the template layer or the board itself.
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
- Enter template adjustment mode to pan and pinch-zoom the image
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
- Edge-to-edge layout
- English and Spanish string resources
- Local state restoration across configuration changes via `rememberSaveable`

## How To Use

1. Pick a paint color from the color button in the top bar.
2. Tap cells in `Paint` mode to build the pattern.
3. Open `Tools` to change stitch type, bead shape, or grid dimensions.
4. Import or photograph a template if you want to trace a reference.
5. Switch to `Template` mode to align the image behind the grid.
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

## Current Scope

BeadMaker currently supports:

- creating and editing patterns locally on device
- one in-app quick save slot
- exporting patterns to `.bm`
- template-assisted drafting

It does not currently include:

- cloud sync
- external `.bm` import through the UI
- palette import/export
- PDF or image pattern export

## License

Released under the MIT License. See [LICENSE](LICENSE).
