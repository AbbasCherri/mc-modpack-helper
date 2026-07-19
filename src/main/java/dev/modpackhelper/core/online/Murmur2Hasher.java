package dev.modpackhelper.core.online;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CurseForge identifies files by MurmurHash2 (32 bit, seed 1) computed after
 * stripping whitespace bytes (tab, LF, CR, space) from the content. This is
 * their exact variant, not a general purpose hash.
 */
public final class Murmur2Hasher {

    private Murmur2Hasher() {
    }

    public static long fingerprint(Path file) throws IOException {
        return fingerprint(Files.readAllBytes(file));
    }

    public static long fingerprint(byte[] data) {
        return murmur2(stripWhitespace(data), 1);
    }

    private static byte[] stripWhitespace(byte[] data) {
        byte[] out = new byte[data.length];
        int n = 0;
        for (byte b : data) {
            if (b != 0x09 && b != 0x0a && b != 0x0d && b != 0x20) {
                out[n++] = b;
            }
        }
        byte[] trimmed = new byte[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }

    private static long murmur2(byte[] data, int seed) {
        final int m = 0x5bd1e995;
        final int r = 24;
        int length = data.length;
        int h = seed ^ length;
        int i = 0;
        while (length >= 4) {
            int k = (data[i] & 0xff)
                    | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16)
                    | ((data[i + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
            i += 4;
            length -= 4;
        }
        switch (length) {
            case 3:
                h ^= (data[i + 2] & 0xff) << 16;
                // fall through
            case 2:
                h ^= (data[i + 1] & 0xff) << 8;
                // fall through
            case 1:
                h ^= data[i] & 0xff;
                h *= m;
            default:
        }
        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;
        return h & 0xFFFFFFFFL;
    }
}
