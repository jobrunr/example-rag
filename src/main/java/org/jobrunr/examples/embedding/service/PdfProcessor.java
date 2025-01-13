package org.jobrunr.examples.embedding.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class PdfProcessor implements ContentProcessor {
    @Override
    public boolean supportsExtension(String fileExtension) {
        return "pdf".equals(fileExtension);
    }

    @Override
    public List<Document> process(Path absolutePath) {
        // TODO allow to configure margins?
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder().withNumberOfTopTextLinesToDelete(0).build())
                .withPagesPerDocument(1)
                .build();

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new FileSystemResource(absolutePath), config);
        return pdfReader.read();
    }
}
