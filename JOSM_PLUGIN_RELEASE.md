# JOSM PluginsSource Release Preparation

This repository is prepared for a PluginsSource-first release flow using an externally hosted jar (for example GitHub Releases).

## 1) Repository Status (Verified)

From `build.xml` in this repository:

- Metadata in jar manifest is set:
  - `Plugin-Class`
  - `Plugin-Version`
  - `Plugin-Mainversion`
- Build targets available:
  - `clean`, `compile`, `test`, `dist`, `release-artifact`, `i18n-*`
- Release artifact support:
  - `dist/HouseNumberClick.jar`
  - `dist/HouseNumberClick-<version>.jar` via `ant release-artifact`

## 2) Recommended PluginsSource-First Flow

1. Update `plugin.version` in `build.xml` before tagging/release.
2. Use only non-interactive release commands (no editor prompts).
3. Build and verify:

```bash
cd /home/oliver/IdeaProjects/housenumberclick
ant clean
ant test
ant release-artifact
```

   Quick verification:

```bash
ls -lh dist/HouseNumberClick-<version>.jar
unzip -p dist/HouseNumberClick-<version>.jar META-INF/MANIFEST.MF
```

4. Tag and release on GitHub (non-interactive):

```bash
git tag -a v<version> -m "Release v<version>"
git push origin main
git push origin v<version>
gh release create v<version> dist/HouseNumberClick-<version>.jar \
  --title "HouseNumberClick v<version>" \
  --notes-file RELEASE_NOTES.md
```

5. If a command would open an editor, stop and rerun with explicit non-interactive flags.
6. Use the direct release asset URL for PluginsSource.

URL pattern:

`https://github.com/<owner>/<repo>/releases/download/v<version>/HouseNumberClick-<version>.jar`

## 3) Hosting Note and Optional Future Official JOSM Publish Path

This repository intentionally does not include an `ant publish` target; external artifact hosting is the intended release path.

If you later decide to use the official JOSM publish path, that must be done in the JOSM publication context/repository where `ant publish` is supported.

