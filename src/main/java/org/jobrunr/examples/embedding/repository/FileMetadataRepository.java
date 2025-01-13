package org.jobrunr.examples.embedding.repository;

import org.jobrunr.examples.embedding.model.FileMetadata;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface FileMetadataRepository extends ListCrudRepository<FileMetadata, UUID>, FileMetadataUpsertRepository {
}
