# GitHub Release Steps (PluginsSource-First)

This repository is prepared for externally hosted plugin jars (for example via GitHub Releases + JOSM PluginsSource).

## 1) Preflight

- Ensure working tree is clean.
- Ensure local branch is up to date.
- Update target release version in `build.xml` (`plugin.version`) before tagging.
- Do not run commands that open an interactive editor.

```bash
cd /home/oliver/IdeaProjects/housenumberclick
git --no-pager status --short --branch
git pull --rebase
```

## 2) Build + Test

```bash
cd /home/oliver/IdeaProjects/housenumberclick
ant clean
ant test
ant release-artifact
```

Expected artifacts:
- `dist/HouseNumberClick.jar`
- `dist/HouseNumberClick-<version>.jar`

Quick artifact/manifest check:

```bash
cd /home/oliver/IdeaProjects/housenumberclick
ls -lh dist/HouseNumberClick-<version>.jar
unzip -p dist/HouseNumberClick-<version>.jar META-INF/MANIFEST.MF
```

## 3) Tag the Release

```bash
cd /home/oliver/IdeaProjects/housenumberclick
git tag -a v<version> -m "Release v<version>"
git push origin main
git push origin v<version>
```

## 4) Create GitHub Release

```bash
cd /home/oliver/IdeaProjects/housenumberclick
gh release create v<version> dist/HouseNumberClick-<version>.jar \
  --title "HouseNumberClick v<version>" \
  --notes-file RELEASE_NOTES.md
```

Hard rule:
- If a command would open an editor, stop and rerun it with explicit non-interactive flags.

PluginsSource URL pattern for the uploaded asset:

`https://github.com/<owner>/<repo>/releases/download/v<version>/HouseNumberClick-<version>.jar`

## 5) Post-Release Quick Check

- Verify tag exists remotely.
- Verify release notes text and uploaded versioned jar are correct.
- Verify downloaded jar contains manifest entries:
  - `Plugin-Class`
  - `Plugin-Version`
  - `Plugin-Mainversion`

## 6) Local Smoke Test (Recommended)

```bash
cd /home/oliver/IdeaProjects/housenumberclick
cp dist/HouseNumberClick.jar ~/.josm/plugins/
```

- Start JOSM and verify plugin loads.
- Start Street Mode and test:
  - Left click (`apply`)
  - Ctrl+Click (`readback`)
  - Right click (`row-house split`)
  - Alt+Drag (`line split`)

