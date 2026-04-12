# HouseNumberClick 1.1.6 Release Notes

## Highlights Since 1.1.5

- Added a postcode analysis layer with vivid colorblind-friendlier colors and an on-map top-5 postcode legend.
- Expanded building completeness analysis, including a dedicated "No Address Data" state and improved completeness legend labeling.
- Added reference-street support with improved fetch diagnostics, cache-aware visibility handling, and automatic refresh integration.
- Improved readback feedback for missing click targets and missing address data.
- Added subtle street-completeness warnings in house-number overview workflows.
- Row-house split remains inline via right-click on a building and uses the configured `Parts` value from the dialog.

## Dialog and UI Changes

- Reordered address fields and display options (auto-zoom first), and aligned help wording/order with current shortcuts.
- Increased the main dialog height and stabilized status panel height to reduce layout jumps.
- Restored last focused dialog input after resume.
- Improved row-house parts controls: larger +/- buttons, field sync with `Alt+1..9`, and deferred field sync to avoid document mutation issues.
- Adjusted dialog defaults and `Next` behavior when no street is preselected.
- Removed global left/right street navigation shortcuts to reduce key handling conflicts.
- Removed the "Load reference" button from overview dialog flows.
- Build and release documentation is cleaned up and aligned with the current Ant/GitHub release flow.

## Stability and Bug Fixes

- Made terrace splitting rollback-safe so partial intermediate splits are reverted on failure.
- Pass `Ctrl+Shift+Click` through to JOSM core shortcuts (no plugin readback on this chord).
- Restrict `Alt+1..9` parts quick-set to plain Alt (ignore Alt+Shift/Ctrl/Meta combinations).
- Invalidate reference-street cache/in-progress state on data source changes to avoid stale reuse.
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

## Documentation and I18N Maintenance

- Interaction model is now fully single-mode: `HouseNumberClickStreetMapMode` remains the only active map mode.
- Translation extraction source list (`i18n/POTFILES.in`) was updated to include all UI classes using translated strings.

## Build Artifacts

- Release artifact: `dist/HouseNumberClick.jar`
- Versioned release artifact: `dist/HouseNumberClick-1.1.6.jar`

