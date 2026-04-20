# HouseNumberClick 1.1.8 Release Notes

HouseNumberClick is a JOSM plugin for fast address tagging on buildings with street-focused editing workflows.

## Highlights Since 1.1.7

- Added a full right-sidebar workflow with persistent `Street Counts` and `House Numbers` dialogs, including header-safe ToggleDialog layout integration and synchronized state/hints.
- Added robust dialog lifecycle handling for data-layer transitions: the main dialog now pauses on edit-layer loss, auto-recovers safely, and refreshes visible content when the active dataset is replaced.
- Added persistent advanced dialog behavior: collapsible advanced sections with remembered state and restored window bounds with off-screen fallback.
- Added postcode analysis expansion: three-state postcode overlay cycle (off -> buildings -> schematic areas), deterministic color/legend behavior, and stronger cache invalidation.
- Added country-aware address flow end-to-end (`addr:country` detection, constrained country selection in dialog, and apply/readback propagation).
- Added directly connected driveway highlighting in the house-number overlay while keeping strict exclusion of non-direct/service/parking cases.

## Dialog and UI Changes

- Moved street navigation arrows inline into the Address row and removed global left/right key handling conflicts.
- Grouped `Line Split` and `Row Houses` controls side-by-side in one row and aligned split-panel heights for consistent layout.
- Updated advanced-section toggle visuals (`More`/`Less`) and refined compact spacing/alignment in display and split sections.
- Improved row-house parts controls (field/button sizing, mode-to-dialog sync, and safer deferred updates during document events).
- Clarified sidebar titles to reflect scope: `Street Counts (House Numbers)` and `House Numbers (Base Numbers only)`.

## Stability and Bug Fixes

- Hardened asynchronous reference-street loading lifecycle with generation guards and better stale-callback protection.
- Improved overview and overlay refresh reliability after edits/downloads/undo-redo and after dataset/layer transitions.
- Removed legacy overview window classes in favor of the sidebar architecture and centralized cleanup paths.
- Expanded regression coverage around dialog lifecycle, split layout/wiring, sidebar integration, and dataset replacement behavior.
- Updated release/i18n process documentation and tag-driven automation guards for reproducible non-interactive releases.

## Build Artifact

- Release artifact: `dist/HouseNumberClick-1.1.8.jar`

