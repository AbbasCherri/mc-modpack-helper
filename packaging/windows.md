# Windows

Needs the WiX Toolset installed for msi builds (jpackage tells you if it's missing).

```
jpackage --type msi --name mc-modpack-helper ^
  --input target\libs ^
  --main-jar mc-modpack-helper-0.1.0-SNAPSHOT.jar ^
  --main-class dev.modpackhelper.Main ^
  --dest target\dist ^
  --win-menu --win-shortcut --app-version 0.1.0
```

`--type exe` works too. Add `--icon icon.ico` once the project has an icon.
