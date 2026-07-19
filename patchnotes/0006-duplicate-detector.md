# 0006 Duplicate detector

Groups scanned entries by mod id. Two or more jars with the same id get flagged, either as a plain duplicate or as a cross loader conflict if the loaders differ. Written against the loader enum rather than hardcoding NeoForge so it keeps working when more loaders show up. Entries without parsed metadata never conflict, an unknown jar is not evidence of anything.
