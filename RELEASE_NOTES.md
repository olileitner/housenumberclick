# HouseNumberClick 1.1.7 Release Notes

HouseNumberClick is a JOSM plugin for fast address tagging on buildings with street-focused editing workflows.

## Highlights Since 1.1.6

- Added city-aware address workflow end-to-end: `Ctrl+Click` readback now restores `addr:city`, left-click apply can write `addr:city`, and overwrite warnings now include city conflicts with independent suppression.
- Improved same-name street readback disambiguation by prioritizing nearest local street clusters and stabilizing seed-way resolution to avoid stale cross-area hints.
- Refined auto-zoom behavior: zoom now frames the full selected street, table-driven zoom honors the AutoZoom option, and automatic zoom is limited to explicit street-selection actions.
- Added and expanded analysis overlays: duplicate overview layer, focused completeness analysis (`Any` mode), postcode legend improvements, and deterministic/clearer analysis color behavior.
- Hardened duplicate detection semantics by scope: global duplicate markers now apply conditional city matching, while local selected-street label duplicate highlighting remains city-agnostic (`street+postcode+housenumber`).
- Improved overlay reliability: relation/outer-way canonicalization prevents self-duplicates, and house-number label overlay now self-heals if it disappears while the option is enabled.

## Dialog and UI Changes

- Synced overview dialog close events back into main dialog checkboxes.
- Moved the duplicate analysis action next to postcode analysis for faster access.
- Reordered address fields and display options (auto-zoom first), and aligned help wording/order with current shortcuts.
- Increased the main dialog height and stabilized status panel height to reduce layout jumps.
- Restored last focused dialog input after resume.
- Improved row-house parts controls: larger +/- buttons, field sync with `Alt+1..9`, and deferred field sync to avoid document mutation issues.
- Adjusted dialog defaults and `Next` behavior when no street is preselected.
- Removed global left/right street navigation shortcuts to reduce key handling conflicts.
- Removed the "Load reference" button from overview dialog flows.

## Stability and Bug Fixes

- Improved undo/redo visual refresh paths and Ctrl+Z reliability in active mapping flow.
- Reduced non-essential plugin logging to keep routine operation quieter.
- Block apply when house number is missing.
- Ensure Street Mode is truly active when opening the dialog.
- Require `Alt+Right-click` for row-house split.
- Group split operations into a single undo step.
- Adjust split cursor hotspot and improve cursor reset on app focus loss.
- Suppress Ctrl magnifier cursor while Shift is pressed.
- Stabilize overlay connection lines while panning.
- Refine street completeness edge heuristic.
- Improve reference-street fetch robustness (including Overpass reader argument order and failure categorization).
- Refresh overview tables after dataset downloads.

## Build Artifact

- Release artifact: `dist/HouseNumberClick-1.1.7.jar`

