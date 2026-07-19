# Packaging

jpackage ships with the JDK and builds a native app with a bundled Java runtime, so users don't need Java installed. It cannot cross compile. A Windows installer has to be built on Windows, a dmg on macOS, a Linux package on Linux.

Same first step everywhere:

```
./mvnw package
cp target/mc-modpack-helper-*.jar target/libs/
```

That leaves target/libs holding the app jar plus every runtime dependency. Then run the jpackage command for your OS, see windows.md, macos.md and linux.md in this folder.

The JavaFX jars from Maven are platform specific. Whatever OS you build on, Maven fetched the right ones, so this just works as long as you package on the same OS you built on.

A CI matrix that builds all three installers per release would be a nice contribution.
