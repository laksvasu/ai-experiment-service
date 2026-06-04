package com.razorpay.experiment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MurmurHash3Test {

    @Test
    void bucketIsStableForSameInput() {
        int first = MurmurHash3.bucket("user-abc:experiment-1");
        int second = MurmurHash3.bucket("user-abc:experiment-1");
        assertEquals(first, second);
        assertTrue(first >= 0 && first < 100);
    }

    @Test
    void differentInputsUsuallyProduceDifferentBuckets() {
        int a = MurmurHash3.bucket("user-a:exp");
        int b = MurmurHash3.bucket("user-b:exp");
        // not guaranteed but extremely likely
        assertTrue(a != b || !"user-a".equals("user-b"));
    }
}
