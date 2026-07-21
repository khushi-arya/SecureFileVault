package com.demo.securevault.service;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;

@Service
public class PdfProtectionService {

    /**
     * PDF ke andar user-password lock laga deta hai.
     * Yeh password bina diye PDF koi bhi reader (Adobe, Chrome, etc.) me nahi khulega.
     */
    public byte[] protectPdf(byte[] originalPdfBytes, String password) {
        try (PDDocument document = Loader.loadPDF(originalPdfBytes)) {
            AccessPermission accessPermission = new AccessPermission();

            // Owner password aur user password same rakh rahe hain simplicity ke liye.
            // User password = file kholne ke liye chahiye
            // Owner password = permissions change karne ke liye chahiye (jaise print/edit block karna)
            StandardProtectionPolicy protectionPolicy =
                    new StandardProtectionPolicy(password, password, accessPermission);
            protectionPolicy.setEncryptionKeyLength(256); // AES-256

            document.protect(protectionPolicy);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to protect PDF with password", e);
        }
    }
}
