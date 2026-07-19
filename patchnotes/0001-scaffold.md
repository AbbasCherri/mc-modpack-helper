# 0001 Scaffold

Set up the Maven project. Java 21, JavaFX 21, Gson, tomlj, JUnit 5. Maven wrapper included so contributors only need a JDK. Docs and GPLv3 license added. Placeholder Main class prints the project name, real app comes later.

Build is plain classpath, no module-info. tomlj is only an automatic module so full jlink modularity was never on the table. Packaging will use a jlink runtime image plus a classpath app through jpackage.
