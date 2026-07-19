# mc-modpack-helper

A desktop tool for Minecraft modpack devs. Point it at a mods folder and it shows you what's actually in there: mod IDs, versions, and Minecraft compatibility read straight out of each jar. It flags duplicate mods and can swap a mod for a version that matches your pack, with the old jar backed up first.

NeoForge packs only for now.

## Running it

You need Java 21 or newer. Then:

```
./mvnw javafx:run
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Building

```
./mvnw package
```

Tests run with `./mvnw test`.

## Contributing

Contributions are welcome, see CONTRIBUTING.md. The code is GPLv3.
