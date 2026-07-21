package com.demo.securevault.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_LENGTH = 256;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int ITERATIONS = 65536;
    private static final String SECRET_KEY_FACTORY = "PBKDF2WithHmacSHA256";
    private static final String KEY_ALGORITHM = "AES";

    public byte[] encrypt(byte[] data, String password) {
        if (data == null || password == null) {
            throw new IllegalArgumentException("Data and password must not be null");
        }

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey secretKey = deriveKey(password, salt);
        byte[] encrypted = doCipher(Cipher.ENCRYPT_MODE, data, secretKey, iv);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write(salt);
            output.write(iv);
            output.write(encrypted);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build encrypted payload", e);
        }

        return output.toByteArray();
    }

    public byte[] decrypt(byte[] encryptedData, String password) {
        if (encryptedData == null || password == null) {
            throw new IllegalArgumentException("Encrypted data and password must not be null");
        }
        if (encryptedData.length < SALT_LENGTH + IV_LENGTH) {
            throw new IllegalArgumentException("Encrypted data is too short");
        }

        byte[] salt = Arrays.copyOfRange(encryptedData, 0, SALT_LENGTH);
        byte[] iv = Arrays.copyOfRange(encryptedData, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
        byte[] cipherText = Arrays.copyOfRange(encryptedData, SALT_LENGTH + IV_LENGTH, encryptedData.length);

        SecretKey secretKey = deriveKey(password, salt);
        byte[] decrypted;
        try {
            decrypted = doCipher(Cipher.DECRYPT_MODE, cipherText, secretKey, iv);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid password or corrupted data", e);
        }

        return decrypted;
    }

    public byte[] compress(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output)) {
            gzipOutputStream.write(data);
            gzipOutputStream.finish();
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress data", e);
        }
    }

    public byte[] decompress(byte[] compressedData) {
        if (compressedData == null) {
            throw new IllegalArgumentException("Compressed data must not be null");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzipInputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decompress data", e);
        }
    }

    private SecretKey deriveKey(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, AES_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive encryption key", e);
        }
    }

    private byte[] doCipher(int mode, byte[] input, SecretKey secretKey, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(mode, secretKey, spec);
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process cipher operation", e);
        }
    }
}
