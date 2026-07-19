# 0011 Mod replacer

Core of the replace feature. Given a mod the online lookup recognized plus a target Minecraft version, findCandidate asks Modrinth for the newest compatible NeoForge file. If Modrinth has nothing compatible the installed jar gets fingerprinted and CurseForge is asked instead, same order as lookups. CurseForge files with a null downloadUrl, meaning the author opted out of API distribution, are treated as not replaceable.

The swap itself is deliberately paranoid. Download lands next to the mods folder as a .part file, gets verified against the hash the platform declared, sha1 for Modrinth and the murmur2 fingerprint for CurseForge, and only after that passes does the old jar move into mods/.modpack-helper-backup/<timestamp>/. A failed check deletes the download and leaves everything untouched. Downloads go through a tiny Downloader interface so tests fake the network.

Also split the two big coordinator methods, the linter was right that they were getting dense.
