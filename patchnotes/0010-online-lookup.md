# 0010 Online lookup

The Check online toggle now works. Flow per scan: batch all jar sha1 hashes to Modrinth, batch fetch project names and links for the hits, then fingerprint whatever missed and try CurseForge if an API key is set. Modrinth going down does not stop the CurseForge pass and CurseForge going down does not touch Modrinth results. Off by default, nothing network runs unless the box is ticked.

Update detection needs to know the pack's Minecraft version. That comes from InstanceVersionDetector which reads whatever launcher file sits near the mods folder: Prism and MultiMC mmc-pack.json, CurseForge app minecraftinstance.json, or a vanilla versions dir when there is exactly one version in it. No match means no update check, the status bar says so.

New table columns: online name, author, source, latest compatible version and an update flag. Settings dialog stores the CurseForge key in OS preferences.

Verified the wire format against the live Modrinth API with a real Sodium NeoForge file hash.
