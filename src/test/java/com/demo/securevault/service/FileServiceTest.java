package com.demo.securevault.service;

import com.demo.securevault.config.SupabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class FileServiceTest {

    private static final String TEST_PASSWORD = "test-password";

    @Test
    void uploadToSupabaseReturnsStoragePathOnSuccess() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        server.expect(requestTo("https://example.supabase.co/storage/v1/object/secure-vault/test.txt"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        SupabaseConfig.SupabaseProperties properties = new SupabaseConfig.SupabaseProperties(
                "https://example.supabase.co",
                "test-key",
                "secure-vault"
        );

        FileService fileService = new FileService(restTemplate, properties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello world".getBytes(StandardCharsets.UTF_8)
        );

        String storagePath = fileService.uploadToSupabase(file, "test.txt", TEST_PASSWORD);

        assertEquals("secure-vault/test.txt", storagePath);
        server.verify();
    }

    @Test
    void downloadFromSupabaseReturnsBytesOnSuccess() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        EncryptionService encryptionService = new EncryptionService();
        byte[] originalData = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = encryptionService.compress(originalData);
        byte[] encryptedData = encryptionService.encrypt(compressed, TEST_PASSWORD);

        server.expect(requestTo("https://example.supabase.co/storage/v1/object/secure-vault/test.txt"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(encryptedData, MediaType.APPLICATION_OCTET_STREAM));

        SupabaseConfig.SupabaseProperties properties = new SupabaseConfig.SupabaseProperties(
                "https://example.supabase.co",
                "test-key",
                "secure-vault"
        );

        FileService fileService = new FileService(restTemplate, properties);

        byte[] actual = fileService.downloadFromSupabase("test.txt", TEST_PASSWORD);

        assertArrayEquals(originalData, actual);
        server.verify();
    }
}