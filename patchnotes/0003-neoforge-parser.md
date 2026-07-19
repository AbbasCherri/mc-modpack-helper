# 0003 NeoForge parser

Reads mod metadata out of a jar without extracting it. Checks META-INF/neoforge.mods.toml first, then the older META-INF/mods.toml location. Pulls modId, display name, version and the declared Minecraft version range from the [[dependencies]] tables.

Versions declared as ${file.jarVersion} get resolved from the jar manifest's Implementation-Version, same substitution the loader does at runtime. If that fails the raw placeholder stays visible rather than being blanked.

Tests build tiny fixture jars on the fly, no real mod jars in the repo.
