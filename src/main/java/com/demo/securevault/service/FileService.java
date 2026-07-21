package com.demo.securevault.service;

import com.demo.securevault.config.SupabaseConfig;
import com.demo.securevault.exception.FileNotFoundException;
import com.demo.securevault.model.FileMetadata;
import com.demo.securevault.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    private final RestTemplate restTemplate;
    private final SupabaseConfig.SupabaseProperties properties;
    private final FileMetadataRepository fileMetadataRepository;
    private final EncryptionService encryptionService;
    private final PdfProtectionService pdfProtectionService;

    public FileService(RestTemplate restTemplate,
                       SupabaseConfig.SupabaseProperties properties) {
        this(restTemplate, properties, null);
    }

    @Autowired
    public FileService(RestTemplate restTemplate,
                       SupabaseConfig.SupabaseProperties properties,
                       FileMetadataRepository fileMetadataRepository) {
        this(restTemplate, properties, fileMetadataRepository,
                new EncryptionService(), new PdfProtectionService());
    }

    public FileService(RestTemplate restTemplate,
                       SupabaseConfig.SupabaseProperties properties,
                       FileMetadataRepository fileMetadataRepository,
                       EncryptionService encryptionService,
                       PdfProtectionService pdfProtectionService) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.fileMetadataRepository = fileMetadataRepository;
        this.encryptionService = encryptionService;
        this.pdfProtectionService = pdfProtectionService;
    }

    public Optional<FileMetadata> findById(UUID fileId) {
        if (fileMetadataRepository == null) {
            return Optional.empty();
        }
        return fileMetadataRepository.findById(fileId);
    }

    // Backward-compatible overload (contentType not provided)
    public FileMetadata saveMetadata(String originalFilename, String supabaseKey, String userEmail) {
        return saveMetadata(originalFilename, supabaseKey, userEmail, null);
    }

    public FileMetadata saveMetadata(String originalFilename, String supabaseKey, String userEmail, String contentType) {
        if (fileMetadataRepository == null) {
            throw new IllegalStateException("FileMetadataRepository is not configured");
        }

        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalFilename(originalFilename);
        metadata.setSupabaseKey(supabaseKey);
        metadata.setUserEmail(userEmail);
        metadata.setContentType(contentType);
        metadata.setUploadTimestamp(LocalDateTime.now());
        return fileMetadataRepository.save(metadata);
    }

    public String uploadToSupabase(MultipartFile file, String uniqueKey) {
        return uploadToSupabase(file, uniqueKey, "");
    }

    public String uploadToSupabase(MultipartFile file, String uniqueKey, String password) {
        try {
            byte[] originalBytes = file.getBytes();
            String effectivePassword = password == null ? "" : password;

            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

            // Only treat as PDF if it actually is one — avoids crashing on non-PDF uploads
            boolean isPdf = "application/pdf".equalsIgnoreCase(contentType) || filename.endsWith(".pdf");

            // Step 1: PDF-internal password lock — ONLY for actual PDF files.
            // (PDFBox's Loader.loadPDF() throws if given non-PDF bytes, so this
            // must never run for images, docs, zips, etc.)
            byte[] bytesToStore = originalBytes;
            if (!effectivePassword.isEmpty() && isPdf) {
                bytesToStore = pdfProtectionService.protectPdf(originalBytes, effectivePassword);
            }

            // Step 2: Compress + AES-encrypt — this runs for EVERY file type,
            // and is what actually makes any format secure/password-protected.
            byte[] processedBytes = encryptionService.encrypt(
                    encryptionService.compress(bytesToStore), effectivePassword);

            String encodedKey = URLEncoder.encode(uniqueKey, StandardCharsets.UTF_8);
            String baseUrl = properties.getUrl().replaceAll("/+$", "");
            baseUrl = baseUrl.replaceAll("/rest/v1/?$", "");
            String url = baseUrl + "/storage/v1/object/" + properties.getBucket() + "/" + encodedKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(properties.getKey());
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(processedBytes, headers);
            ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase upload failed: " + response.getStatusCode());
            }

            return properties.getBucket() + "/" + uniqueKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file for Supabase upload", e);
        } catch (Exception e) {
            throw new RuntimeException("Supabase upload failed", e);
        }
    }

    public byte[] downloadFromSupabase(String supabaseKey) {
        return downloadFromSupabase(supabaseKey, "");
    }

    public byte[] downloadFromSupabase(String supabaseKey, String password) {
        try {
            String encodedKey = URLEncoder.encode(supabaseKey, StandardCharsets.UTF_8);
            String baseUrl = properties.getUrl().replaceAll("/+$", "");
            baseUrl = baseUrl.replaceAll("/rest/v1/?$", "");
            String url = baseUrl + "/storage/v1/object/" + properties.getBucket() + "/" + encodedKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(properties.getKey());

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, requestEntity, byte[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase download failed: " + response.getStatusCode());
            }

            byte[] encryptedBytes = response.getBody();
            String effectivePassword = password == null ? "" : password;
            // Yeh already password-protected bytes return karega (agar upload ke time PDF password diya tha)
            return encryptionService.decompress(encryptionService.decrypt(encryptedBytes, effectivePassword));
        } catch (HttpClientErrorException.NotFound e) {
            throw new FileNotFoundException("File not found in Supabase storage", e);
        } catch (Exception e) {
            throw new RuntimeException("Supabase download failed", e);
        }
    }
}