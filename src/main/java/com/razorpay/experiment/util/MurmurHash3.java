package com.razorpay.experiment.util;

import java.nio.charset.StandardCharsets;

/**
 * MurmurHash3 32-bit implementation for stable rollout bucketing.
 */
public final class MurmurHash3 {

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private MurmurHash3() {
    }

    public static int hash32(String input) {
        if (input == null) {
            return 0;
        }
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        return hash32(data, 0, data.length, 0);
    }

    public static int hash32(byte[] data, int offset, int length, int seed) {
        int hash = seed;
        int roundedEnd = offset + (length & 0xfffffffc);

        for (int i = offset; i < roundedEnd; i += 4) {
            int k = (data[i] & 0xff)
                    | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16)
                    | ((data[i + 3] & 0xff) << 24);
            k *= C1;
            k = Integer.rotateLeft(k, 15);
            k *= C2;
            hash ^= k;
            hash = Integer.rotateLeft(hash, 13);
            hash = hash * 5 + 0xe6546b64;
        }

        int k1 = 0;
        switch (length & 3) {
            case 3:
                k1 ^= (data[roundedEnd + 2] & 0xff) << 16;
            case 2:
                k1 ^= (data[roundedEnd + 1] & 0xff) << 8;
            case 1:
                k1 ^= data[roundedEnd] & 0xff;
                k1 *= C1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= C2;
                hash ^= k1;
            default:
                break;
        }

        hash ^= length;
        return fmix32(hash);
    }

    private static int fmix32(int hash) {
        hash ^= hash >>> 16;
        hash *= 0x85ebca6b;
        hash ^= hash >>> 13;
        hash *= 0xc2b2ae35;
        hash ^= hash >>> 16;
        return hash;
    }

    public static int bucket(String input) {
        return (hash32(input) & 0x7FFFFFFF) % 100;
    }
}
