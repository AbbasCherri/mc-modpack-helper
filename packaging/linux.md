# Linux

App image, a plain folder with a launcher, no packaging tools needed:

```
jpackage --type app-image --name mc-modpack-helper \
  --input target/libs \
  --main-jar mc-modpack-helper-0.1.0-SNAPSHOT.jar \
  --main-class dev.modpackhelper.Main \
  --dest target/dist
```

Run it with `target/dist/mc-modpack-helper/bin/mc-modpack-helper`.

For a real package use `--type deb` (needs dpkg) or `--type rpm` (needs rpmbuild) instead, plus:

```
  --linux-shortcut --app-version 0.1.0
```
