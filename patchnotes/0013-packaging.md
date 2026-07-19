# 0013 Packaging

The build now drops all runtime jars into target/libs and writes a proper Main-Class manifest, so `java -jar` works from target and jpackage has a clean input folder. Per OS jpackage commands live in packaging/. Built and ran the Linux app image here: launcher works, bundled runtime, no Java install needed on the user's machine.

JavaFX rides along as plain classpath jars, which prints a harmless unsupported configuration warning at startup. Going fully modular to silence it is not worth the contributor overhead, tomlj has no module descriptor anyway.
