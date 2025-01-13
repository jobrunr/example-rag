package org.jobrunr.examples.embedding.service;

import org.jobrunr.JobRunrException;
import org.jobrunr.examples.embedding.model.FileMetadata;
import org.jobrunr.examples.embedding.repository.FileMetadataRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Service
public class DirectoryManager {

    private final String contentDir;
    private final EmbeddingManager embeddingManager;
    private final FileMetadataRepository fileMetadataRepository;
    private final JobScheduler jobScheduler;

    public DirectoryManager(
            @Value("${app.content-dir}") String contentDir,
            EmbeddingManager embeddingManager,
            FileMetadataRepository fileMetadataRepository,
            JobScheduler jobScheduler
    ) {
        this.contentDir = contentDir;
        this.embeddingManager = embeddingManager;
        this.fileMetadataRepository = fileMetadataRepository;
        this.jobScheduler = jobScheduler;
    }

    @Recurring(id = "embedding-synchronization", cron = "${app.embedding-synchronization.cron}")
    @Job(name = "Browse content directory and initiate embedding synchronization")
    public void manage(JobContext jobContext) throws IOException {
        jobContext.logger().info("Initiating synchronization of embedding files...");

        jobContext.logger().info("Retrieving metadata from the FileMetadata table...");
        List<FileMetadata> allKnownFileMetadata = fileMetadataRepository.findAll();

        jobContext.logger().info(String.format("Retrieving metadata of files in directory %s...", contentDir));
        List<FileMetadata> allFilesFromDirectory = getAllFiles();

        jobContext.logger().info("Filtering updated files and creating jobs to update their embeddings...");
        List<FileMetadata> updatedFiles = getUpdatedFiles(allKnownFileMetadata, allFilesFromDirectory);
        List<FileMetadata> deletedFiles = getDeletedFiles(allKnownFileMetadata, allFilesFromDirectory);
        jobScheduler.enqueue(Stream.of(updatedFiles, deletedFiles).flatMap(Collection::stream), (file) -> embeddingManager.manage(file.getRelativePath(), file.getStatus()));

        jobContext.logger().info("Updating FileMetadata table...");
        fileMetadataRepository.upsertAll(updatedFiles);
        fileMetadataRepository.deleteAll(deletedFiles);

        jobContext.logger().info("All done! Updating the embeddings will now happen in separate processes.");
    }

    private List<FileMetadata> getAllFiles() throws IOException {
        Path directory = Paths.get(contentDir);
        if (directory.toFile().exists()) {
            try (Stream<Path> stream = Files.walk(directory)) {
                return stream
                        .filter(this::isProcessable)
                        .map(path -> FileMetadata.of(directory.relativize(path).toString(), getLastModifiedTime(path).truncatedTo(ChronoUnit.MILLIS)))
                        .toList();
            }
        }
        throw new JobRunrException("Directory does not exist: " + directory, true);
    }

    private List<FileMetadata> getUpdatedFiles(List<FileMetadata> knownFiles, List<FileMetadata> filesInDirectory) {
        Map<String, Instant> fileRelativePathToLastModifiedTime = mapFileRelativePathToLastModifiedTime(knownFiles);
        return filesInDirectory.stream()
                .filter(file -> file.getLastModified()
                        .isAfter(ofNullable(fileRelativePathToLastModifiedTime.get(file.getRelativePath())).orElse(Instant.EPOCH))
                ).map(FileMetadata::updated)
                .toList();
    }

    private List<FileMetadata> getDeletedFiles(List<FileMetadata> knownFiles, List<FileMetadata> filesInDirectory) {
        Set<String> relativePathOfFilesInDirectory = filesInDirectory.stream().map(FileMetadata::getRelativePath).collect(Collectors.toSet());
        return knownFiles.stream()
                .filter(file -> !relativePathOfFilesInDirectory.contains(file.getRelativePath()))
                .map(FileMetadata::deleted)
                .toList();
    }

    private Instant getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            return Instant.now();
        }
    }

    private Map<String, Instant> mapFileRelativePathToLastModifiedTime(List<FileMetadata> files) {
        return files.stream().collect(Collectors.toMap(FileMetadata::getRelativePath, FileMetadata::getLastModified));
    }

    private boolean isProcessable(Path path) {
        return Files.isRegularFile(path);
    }
}
