# 0002 Data model

Records for everything the app passes around. ModEntry ties together the file on disk, whatever metadata we managed to parse out of it, online lookup results, and conflict flags. The last two start empty and get filled in later, so ModEntry has small wither methods that return an updated copy instead of mutating.

OnlineModInfo carries which source it came from (Modrinth or CurseForge) and the project id, because the replace feature needs both later.
