# QuickAddressFill Stability Manual Tests

These scenarios focus on the recent hardening changes (state reset, fallback cleanup, click handling).

## 1) DataSet Switch Resets Remembered Dialog Context

### Preparation
- Start JOSM with QuickAddressFill plugin enabled.
- Load DataSet A with streets/buildings.
- Open Quick Address Fill dialog and enter distinctive values:
  - Postcode: `99999`
  - Street: pick any street
  - Building type: `warehouse`
  - House number: `77a`
  - Increment: `+2`

### Steps
1. Close the dialog.
2. Switch to another editable layer or open DataSet B.
3. Open Quick Address Fill again.

### Expected
- Remembered values from DataSet A are not reused in DataSet B.
- Default-like state appears (house number starts from default, previous custom values are cleared).

### Relevant Logs
- Optional diagnostic around mode activation can appear in JOSM log if map view is unavailable.

## 2) BuildingSplitter Fallback: Stale Pending Is Cleared

### Preparation
- JOSM closed.
- In preferences, set (or keep) stale values for:
  - `quickaddressfill.buildingsplitter.handoff.pending=true`
  - `quickaddressfill.buildingsplitter.handoff.timestamp=<very old timestamp>`

### Steps
1. Start JOSM and open Quick Address Fill dialog.
2. Trigger `Split building`.

### Expected
- Stale handoff preference data is cleared before activation.

### Relevant Logs
- `QuickAddressFill: Clearing stale BuildingSplitter handoff fallback (differentSession=..., expired=...)`

## 3) BuildingSplitter Reflection Handoff Clears Fallback

### Preparation
- Install a BuildingSplitter version that provides `AddressContextBridge.setAddressContext(String,String)`.

### Steps
1. Open Quick Address Fill, set street/postcode.
2. Trigger `Split building`.

### Expected
- Reflection handoff succeeds and fallback keys are cleared.

### Relevant Logs
- `QuickAddressFill: Address context reflection handoff succeeded.`

## 4) Click Deduplizierung

### Preparation
- Open area with buildings.
- Enable debug logs in JOSM.

### Steps
1. Click one building once normally.
2. Trigger near-identical duplicated release event (for example via touchpad ghost click) if reproducible.
3. Perform two very fast but slightly different clicks at different positions.

### Expected
- True duplicate release is suppressed once.
- Fast distinct clicks are processed independently.

### Relevant Logs
- Duplicate suppression:
  - `QuickAddressFill QuickAddressFillStreetMapMode.mouseReleased: duplicate release suppressed at x,y`
- Slow click diagnostics (only when threshold exceeded):
  - `...slow click handling (... ms)...`

## 5) Candidate Limit Monitoring

### Preparation
- Load a dense urban area with many buildings/relations.
- Enable debug logs in JOSM.
- Optional: set custom limits in advanced preferences:
  - `quickaddressfill.streetmode.relationScanLimit`
  - `quickaddressfill.streetmode.wayScanLimit`

Suggested test values:
- baseline/default: leave both keys unset
- intentionally low: `100` / `100`
- intentionally high: `10000` / `20000`

### Steps
1. Click repeatedly in dense area where nearest-hit fallback scans are likely.
2. Repeat with low and high limit values.

### Expected
- Plugin remains responsive.
- With low limits, fallback misses become more frequent (false negatives).
- With higher limits, misses should decrease, but click-path runtime can increase.

### Relevant Logs
- Full click diagnostic (debug):
  - `QuickAddressFill click-path: outcome=..., source=..., nearestCandidates=..., relationChecked=.../..., wayChecked=.../..., relationLimitReached=..., wayLimitReached=..., ... durationMs=...`
- Slow click diagnostic (debug):
  - `QuickAddressFill QuickAddressFillStreetMapMode.mouseReleased: slow click handling (... ms), source=..., outcome=..., x=..., y=...`

How to identify limits that are too low:
- `relationLimitReached=true` or `wayLimitReached=true` appears frequently.
- `source=no-hit` appears despite likely building targets in dense regions.

