package org.jobrunr.examples.embedding.service;

import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.List;

public interface ContentProcessor {

    default boolean supports(Path absolutePath) {
        return supportsExtension(StringUtils.getFilenameExtension(absolutePath.toString()));
    }

    boolean supportsExtension(String fileExtension);

    List<Document> process(Path absolutePath);
}
