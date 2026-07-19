# Contributing

Thanks for wanting to help out.

## Setup

You need Java 21 or newer and nothing else. The Maven wrapper handles the rest.

```
./mvnw test        run the tests
./mvnw javafx:run  run the app
```

## Layout

- `src/main/java/dev/modpackhelper/core` is plain logic. No JavaFX imports allowed here, everything in it is unit tested.
- `src/main/java/dev/modpackhelper/ui` is the JavaFX side.
- `patchnotes/` holds a short note per meaningful change. Commit messages stay short, details go there.

## Pull requests

Keep them small and focused. If you add logic to `core`, add a test with it. Parser changes should come with a fixture jar built in the test, not a real mod jar copied into the repo.
