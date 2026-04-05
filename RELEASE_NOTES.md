# HouseNumberClick 1.1.0 Release Notes

HouseNumberClick is a JOSM plugin for fast address tagging on buildings with street-focused editing workflows.

## Highlights Since 1.0.4

- Added an optional **Street house number counts** window to show all streets with known house-number counts.
- Added click-to-zoom from the street-count table to jump directly to the selected street extent.
- Added interactive sorting in the street-count table:
  - `Count` supports numeric sorting
  - default sorting is `Count` descending
  - header click toggles sorting
- Added and refined optional overview tooling:
  - house-number overlay
  - optional odd/even split connection lines
  - duplicate house-number highlighting
  - house-number overview window with gap markers and click-to-zoom
- Added/kept **Zoom to selected street** behavior from the main dialog.

## Stability and UX Improvements

- Fixed focus/shortcut behavior so global left/right street navigation does not interfere with text input fields.
- Added defensive `LayerManager`/dataset null checks in controller and overlay paths to avoid NPEs in edge states.
- Centralized addressed-building filtering in `AddressedBuildingMatcher` and switched collectors to the shared matcher.
- Improved addressed street matching robustness with case-insensitive comparison.

## Internal Consistency

- Reduced duplicated collector filter logic by using one matcher entry point.
- Aligned collector comments with the centralized matcher structure.

## Build Artifact

- Release artifact: `dist/HouseNumberClick.jar`

