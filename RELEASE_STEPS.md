# GitHub Release Steps (PluginsSource-First)

This repository is prepared for externally hosted plugin jars (for example via GitHub Releases + JOSM PluginsSource).

## 1) Preflight

- Ensure working tree is clean.
- Ensure local branch is up to date.
- Set target release version in `build.xml` (`plugin.version`).

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

## 3) Tag the Release

```bash
cd /home/oliver/IdeaProjects/housenumberclick
git tag v<version>
git push origin v<version>
```

## 4) Create GitHub Release

1. Open GitHub repository releases page.
2. Create a new release from tag `v<version>`.
3. Title suggestion: `HouseNumberClick v<version>`.
4. Copy content from `RELEASE_NOTES.md` into the release description.
5. Upload artifact `dist/HouseNumberClick-<version>.jar`.
6. Publish release.

PluginsSource URL pattern for the uploaded asset:

`https://github.com/<owner>/<repo>/releases/download/v<version>/HouseNumberClick-<version>.jar`

## 5) Post-Release Quick Check

- Verify tag exists remotely.
- Verify release notes text and uploaded versioned jar are correct.
- Verify downloaded jar contains manifest entries:
  - `Plugin-Class`
  - `Plugin-Version`
  - `Plugin-Mainversion`

