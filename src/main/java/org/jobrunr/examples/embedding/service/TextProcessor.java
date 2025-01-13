package org.jobrunr.examples.embedding.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class TextProcessor implements ContentProcessor {
    @Override
    public boolean supportsExtension(String fileExtension) {
        return "txt".equals(fileExtension);
    }

    @Override
    public List<Document> process(Path absolutePath) {
        TextReader textReader = new TextReader(new FileSystemResource(absolutePath));
        return textReader.read();
    }
}
