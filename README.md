# HouseNumberClick

HouseNumberClick is a JOSM plugin for fast, street-focused house-number tagging on buildings.

## What's New in 1.1.4

- Street dialog wording is now more consistent and clearer across display options.
- Updated option names in UI and docs (for example `Auto-zoom to selected street`, `Show house number labels`, `Show all street counts`).
- Improved usage guidance with plugin icon and explicit auto-increment note.

## Who This Is For

- Mappers who assign many addresses on one street in a focused workflow.
- Users who want fast apply/readback behavior with optional visual analysis overlays.

## Compatibility

- Java: **17+** (build uses `javac --release 17`).
- JOSM minimum main version: `19481` (see `Plugin-Mainversion` in `build.xml`).

## Demo

![HouseNumberClick demo](docs/images/housenumberclick-demo.gif)

## Core Features

- Working dialog with `Street`, `Postcode`, optional `Building type`, `House number`, and increment (`-2`, `-1`, `+1`, `+2`).
- Left-click applies `addr:street`, optional `addr:postcode`, optional `addr:housenumber`, and optional `building=*`.
- House number auto-advances after successful apply, including suffix handling (`12a -> 12b`).
- `Ctrl+Click` reads existing address values from buildings; if no building is hit, nearby street name can be read.
- Conflict warning protects against unintended overwrite of existing address data.
- Optional `Auto-zoom to selected street` zooms to mapped house-number buildings of the selected street.

## Map Mode Shortcuts

- `+` / `-`: change current house-number component.
- `L`: toggle suffix (`12 <-> 12a`).
- `Esc`: leave/pause Street Mode.
- Left/right street navigation is disabled while typing in text fields.

## Optional Visual Tools

- `Show house number labels`: overlay of house numbers for the selected street.
- `Show connection lines`: connects mapped numbers in sorted order; `Separate even / odd` splits parity paths.
- Duplicate house numbers are highlighted in the overlay.
- `Show overview panel (selected street)`: odd/even table with gap markers (`•` for missing base numbers); table click zooms to target object(s).
- `Show all street counts`: list of all known streets and current counts; row click zooms to selected street.
- `Show overview` / `Hide overview`: building-only overview layer:
  - green = `addr:housenumber` present on building object,
  - subtle yellow/ochre = likely misplaced housenumber on multipolygon outer way,
  - dark gray = no housenumber found.


## Usage

1. Start <img src="images/housenumberclick.svg" alt="HouseNumberClick icon" width="18" /> `HouseNumberClick` in JOSM.
2. Select street and optional postcode/building type/house number.
3. Click buildings to apply addresses. House number increments automatically after each successful click.
4. Use shortcuts and optional overview windows as needed.

![HouseNumberClick dialog](docs/images/housenumberclick-dialog.png)

## Build and Test

```bash
ant clean
ant test
ant dist
```

Artifacts:
- Main plugin jar: `dist/HouseNumberClick.jar`
- Versioned release jar: `dist/HouseNumberClick-<version>.jar` (via `ant release-artifact`)

## Local Installation

```bash
mkdir -p ~/.josm/plugins
cp dist/HouseNumberClick.jar ~/.josm/plugins/
```

## PluginsSource-First Release (GitHub Hosted Jar)

1. Set release version in `build.xml` (`plugin.version`).
2. Build release artifacts:

```bash
ant clean
ant test
ant release-artifact
```

3. Create Git tag `v<version>` and GitHub release.
4. Upload `dist/HouseNumberClick-<version>.jar` as release asset.
5. For PluginsSource, use the direct GitHub release asset URL pattern:
   - `https://github.com/<owner>/<repo>/releases/download/v<version>/HouseNumberClick-<version>.jar`

## License

GNU General Public License v2. See `LICENSE`.
