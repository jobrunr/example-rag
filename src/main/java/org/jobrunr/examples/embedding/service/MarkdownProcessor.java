package org.jobrunr.examples.embedding.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class MarkdownProcessor implements ContentProcessor {

    @Override
    public boolean supportsExtension(String fileExtension) {
        return "md".equals(fileExtension);
    }

    @Override
    public List<Document> process(Path absolutePath) {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(new FileSystemResource(absolutePath), config);
        return reader.read();
    }
}
