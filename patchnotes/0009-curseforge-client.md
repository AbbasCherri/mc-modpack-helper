# 0009 CurseForge client

Backup lookup source for mods that never made it to Modrinth. CurseForge identifies files by a murmur2 fingerprint, 32 bit, seed 1, computed after stripping whitespace bytes from the file. Murmur2Hasher implements exactly that variant, with pinned test values cross checked against an independent implementation, since a subtle bit error here would just make every lookup miss with no error.

The client needs a user supplied API key, sent as x-api-key. The key lives in the OS preferences store through ApiKeyStore, never in the repo or config files. No key means CurseForge is skipped, quietly.

downloadUrl can be JSON null when a mod author opted out of API distribution. That stays null in the model and the replace feature will have to treat those as not replaceable through CurseForge.
