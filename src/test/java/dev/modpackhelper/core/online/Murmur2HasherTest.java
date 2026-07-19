package dev.modpackhelper.core.online;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class Murmur2HasherTest {

    /**
     * Pinned values cross-checked against an independent murmur2 implementation
     * (32 bit, seed 1). If these break, the hash variant drifted and CurseForge
     * lookups will silently miss everything.
     */
    @Test
    void matchesReferenceValues() {
        assertEquals(1621425345L, Murmur2Hasher.fingerprint(bytes("abc")));
        assertEquals(3469237630L, Murmur2Hasher.fingerprint(bytes("abcde")));
        assertEquals(1540447798L, Murmur2Hasher.fingerprint(new byte[0]));
        assertEquals(3500483126L, Murmur2Hasher.fingerprint(bytes("0123456789abcdef")));
    }

    @Test
    void whitespaceBytesAreIgnored() {
        assertEquals(
                Murmur2Hasher.fingerprint(bytes("abcde")),
                Murmur2Hasher.fingerprint(bytes("a b\nc\rd\te")));
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
