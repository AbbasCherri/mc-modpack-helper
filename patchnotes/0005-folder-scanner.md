# 0005 Folder scanner

Walks a mods folder and turns every jar into a ModEntry: file stats, sha1 for the online lookup later, and parsed metadata when available. Non jar files are skipped. A jar whose content can't be read still shows up with its file info. Sorted by filename so scans are stable.
