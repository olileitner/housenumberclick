# HouseNumberClick 1.1.1 Release Notes

HouseNumberClick is a JOSM plugin for fast address tagging on buildings with street-focused editing workflows.

## Highlights Since 1.1.0

- Improved map-mode shortcut reliability for `L`, `+`, and `-` when dialogs are open.
- Added text-input safeguards so shortcut keys do not trigger while typing.
- Restored map focus automatically after checkbox interactions in the main dialog to keep map shortcuts immediately available.
- Kept street-overlay/overview behavior in sync when selecting streets from the street-count window.

## Stability and UX Improvements

- Fixed focus/shortcut behavior so global left/right street navigation does not interfere with text input fields.
- Kept keyboard handling consistent between map focus and dialog focus states.
- Preserved existing street-mode behavior while improving shortcut ergonomics.

## Build Artifact

- Release artifact: `dist/HouseNumberClick.jar`

