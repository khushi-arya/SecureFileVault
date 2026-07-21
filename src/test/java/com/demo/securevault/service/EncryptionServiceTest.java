package com.demo.securevault.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionServiceTest {

    private final EncryptionService encryptionService = new EncryptionService();

    @Test
    void encryptAndDecryptRoundTripWithPassword() {
        byte[] original = "super secret payload".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = encryptionService.encrypt(original, "my-password");

        assertFalse(Arrays.equals(original, encrypted));
        assertArrayEquals(original, encryptionService.decrypt(encrypted, "my-password"));
    }

    @Test
    void decryptThrowsForWrongPassword() {
        byte[] encrypted = encryptionService.encrypt("payload".getBytes(StandardCharsets.UTF_8), "correct-password");

        assertThrows(IllegalArgumentException.class, () -> encryptionService.decrypt(encrypted, "wrong-password"));
    }

    @Test
    void compressAndDecompressPreserveBytes() {
        byte[] original = "compressed data".getBytes(StandardCharsets.UTF_8);

        byte[] compressed = encryptionService.compress(original);
        byte[] decompressed = encryptionService.decompress(compressed);

        assertFalse(Arrays.equals(original, compressed));
        assertArrayEquals(original, decompressed);
    }
}
