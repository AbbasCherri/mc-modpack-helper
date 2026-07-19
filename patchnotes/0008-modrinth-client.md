# 0008 Modrinth client

Talks to the Modrinth API: one batched hash lookup for the whole folder, a batched project fetch for names and links, and a per project version list filtered to NeoForge plus a given Minecraft version. Sends a proper User-Agent as their docs ask. Base URL is a constructor arg so tests can stub the server.

JSON parsing lives in static methods tested against captured response shapes, so the wire format is covered without any network in the test suite.

Modrinth does not put an author name on the project object, that would need an extra team members call per project, so author stays blank for Modrinth results for now. CurseForge results will carry authors.
