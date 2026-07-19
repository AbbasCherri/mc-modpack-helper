# 0004 Composite parser

Single entry point for pulling metadata out of a jar. Runs the jar through every known parser, first hit wins. Anything that goes wrong, corrupt zip, broken toml, missing file, comes back as empty instead of an exception so one bad jar can't kill a folder scan. Adding support for another loader later means writing one parser class and adding it to the list.
