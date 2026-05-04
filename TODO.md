# BeadMaker TODO

## Auto Pattern (Image -> Bead Pattern)

### Goal
Add an "Auto Pattern" workflow that converts an imported or captured image into a bead pattern mapped to the current bead palette.

### Why this is needed
- Direct image-to-grid conversion without framing can crop/stretch important content.
- A crop/position step is needed before pixelization for predictable results.

### Proposed v1 flow
1. User taps `Auto Pattern`.
2. User imports image or takes photo.
3. App opens crop dialog with pan/zoom.
4. Crop frame is locked to current grid ratio (`gridColumns / gridRows`).
5. User confirms crop.
6. App renders cropped bitmap.
7. App resizes bitmap to grid size (`gridColumns x gridRows`) to pixelize.
8. App quantizes each pixel to nearest color in `BasicPaletteColorValues`.
9. App writes resulting color indexes into `uiState.beads`.

### Core implementation tasks
- Add new action in UI (`Auto Pattern`) in Tools/Files flow.
- Add crop UI (Compose dialog):
  - Pan + zoom image.
  - Fixed ratio crop frame based on current grid.
  - Confirm/cancel actions.
- Add bitmap conversion pipeline:
  - Decode image URI to bitmap.
  - Render transformed crop region into output bitmap.
  - Resize to grid dimensions.
  - Map pixels to nearest palette color index.
- Integrate with `BeadEditorState`:
  - Add function to replace full bead grid from generated color indexes.
  - Push undo snapshot before applying generated pattern.

### Color quantization notes
- Use distance in RGB for v1 (fast and simple).
- Optional v2: switch to perceptual space (Lab/DeltaE) for better matching.
- Optional v2: allow palette limit selection (e.g., 8/12/16 colors).

### Acceptance criteria (v1)
- User can generate a bead pattern from gallery image and camera photo.
- Crop ratio always matches current grid ratio.
- No distortion after conversion.
- Generated pattern fills the entire grid.
- Undo returns to previous pattern state.

### Nice-to-have (v2)
- Dithering toggle (Floyd-Steinberg).
- "Keep transparency as empty bead" option.
- Preview before apply.
- Suggested palettes (warm/cool/high contrast).
