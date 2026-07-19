# 0007 UI skeleton

First runnable version of the app. Open a mods folder, get a sortable table: filename, mod id, name, version, loader, MC range, size, modified date and a status column. Duplicate mods get an amber row, cross loader conflicts orange, both done with CSS pseudo classes from a custom row factory. Unparsed jars say "not parsed" and keep their file info.

Scanning runs on a background thread through a javafx Task so the window never freezes. Status bar shows jar, parsed and flagged counts.

Demo jars for poking at the UI land in target/demo-mods when you run the fixture script, they are not committed.
