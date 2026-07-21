package com.demo.securevault.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "supabase_key", nullable = false, unique = true)
    private String supabaseKey;

    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    // NEW FIELD — stores original MIME type (e.g. "image/png", "application/pdf",
    // "application/zip") so downloads can be served with the correct
    // Content-Type for ANY uploaded file format, not just PDFs.
    @Column(name = "content_type")
    private String contentType;
}