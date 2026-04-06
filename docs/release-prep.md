# HouseNumberClick Release Prep (PluginsSource-First)

## Scope

This checklist prepares a release for externally hosted jars (for example GitHub Releases) that can be consumed by JOSM PluginsSource.

## 1) Update Version and Notes

1. Set `plugin.version` in `build.xml`.
2. Update `RELEASE_NOTES.md` for that version.
3. Keep only one `What's New` block for the current version in `README.md`.

## 2) Build and Validate

```bash
cd /home/oliver/IdeaProjects/housenumberclick
ant clean
ant test
ant release-artifact
```

Expected files:

- `dist/HouseNumberClick.jar`
- `dist/HouseNumberClick-<version>.jar`

## 3) Verify Manifest Metadata

Check the built jar contains:

- `Plugin-Class`
- `Plugin-Version`
- `Plugin-Mainversion`

## 4) GitHub Release

1. Create and push tag `v<version>`.
2. Create GitHub release for `v<version>`.
3. Upload `dist/HouseNumberClick-<version>.jar`.
4. Use release notes from `RELEASE_NOTES.md`.

## 5) PluginsSource URL Pattern

Use the direct release asset URL:

`https://github.com/<owner>/<repo>/releases/download/v<version>/HouseNumberClick-<version>.jar`

