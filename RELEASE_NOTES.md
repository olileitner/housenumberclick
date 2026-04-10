# HouseNumberClick 1.1.5 Release Notes

HouseNumberClick is a JOSM plugin for fast address tagging on buildings with street-focused editing workflows.

## Highlights Since 1.1.4

- Interaction model is now fully single-mode: `HouseNumberClickStreetMapMode` remains the only active map mode.
- Line split now runs inline in street mode via `Alt+Drag` (no temporary mode switch).
- Row-house split remains inline via right-click on a building and uses the configured `Parts` value from the dialog.
- `Alt+1..9` now quick-sets row-house `Parts` directly in Street Mode.
- Alt split-state handling is hardened against global modifier shortcuts (`Ctrl+Alt+Shift+...`) and missing key-release edge cases.
- Build and release documentation is cleaned up and aligned with the current Ant/GitHub release flow.

## Documentation and I18N Maintenance

- Translation extraction source list (`i18n/POTFILES.in`) was updated to include all UI classes using translated strings.

## Build Artifact

- Release artifact: `dist/HouseNumberClick.jar`

