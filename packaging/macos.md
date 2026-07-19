# macOS

```
jpackage --type dmg --name mc-modpack-helper \
  --input target/libs \
  --main-jar mc-modpack-helper-0.1.0-SNAPSHOT.jar \
  --main-class dev.modpackhelper.Main \
  --dest target/dist \
  --app-version 0.1.0
```

Unsigned builds will trip Gatekeeper. For distribution outside your own machine you need an Apple Developer ID and the `--mac-sign` options, or users have to right click and Open the first time. Add `--icon icon.icns` once the project has an icon.
