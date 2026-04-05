# HouseNumberClick

JOSM plugin for fast house-number mapping on buildings.

## Demo
![HouseNumberClick demo](docs/images/housenumberclick-demo.gif)

## Core Features

- Opens a working dialog with `Street`, `Postcode`, optional `Building type`, `House number`, and increment (`-2`, `-1`, `+1`, `+2`).
- Left-click on a building applies address tags (`addr:street`, optional `addr:postcode`, optional `addr:housenumber`) and optionally sets `building=*`.
- House number can auto-advance after successful apply, including letter suffix handling (`12a -> 12b`).
- `Ctrl` + left-click reads existing address values from a building; if no building is hit, nearby street name can be picked.
- Conflict warning protects overwriting existing address values (street/postcode).

## Map Mode Shortcuts

- `+` / `-`: change current house number component.
- `L`: toggle letter suffix (`12 <-> 12a`).
- `Esc`: leave/pause Street Mode.

## Optional Visual Tools

- `Show house number layer`: overlay of house numbers for the selected street.
- `Show connection lines`: connect mapped numbers in sorted order (optionally split even/odd).
- `Show house number overview`: odd/even table including gap markers; click a cell to zoom to the mapped object.
- `Show street house number counts`: list of streets with known house-number counts; click a row to zoom to that street.

## Optional Integration

- Integrates with [`BuildingSplitter`](https://github.com/olileitner/buildingsplitter) when installed.

## Usage

1. Start `HouseNumberClick` in JOSM.
2. Select street and optional postcode/building type/house number.
3. Click buildings to apply addresses.
4. Use optional shortcuts and overview windows as needed.

![HouseNumberClick dialog](docs/images/housenumberclick-dialog.png)

## Build and Test

Prerequisite for compile/dist:
- `buildingsplitter.jar` at `~/.josm/plugins/buildingsplitter.jar`
  or custom path via `-Dbuildingsplitter.jar=/path/to/buildingsplitter.jar`

```bash
ant compile
ant test
ant dist
```

## Local Installation

```bash
mkdir -p ~/.josm/plugins
cp dist/HouseNumberClick.jar ~/.josm/plugins/
```

## License

GNU General Public License v2. See `LICENSE`.
