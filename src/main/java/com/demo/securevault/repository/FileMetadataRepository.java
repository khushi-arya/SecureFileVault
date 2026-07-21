package com.demo.securevault.repository;

import com.demo.securevault.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    Optional<FileMetadata> findBySupabaseKey(String supabaseKey);
}
