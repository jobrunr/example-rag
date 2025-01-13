package org.jobrunr.examples.embedding.service;

import org.jobrunr.JobRunrException;
import org.jobrunr.examples.embedding.model.FileMetadata.Status;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.jobrunr.examples.embedding.model.FileMetadata.Status.UPDATED;

@Service
public class EmbeddingManager {

    private final String contentDir;
    private final VectorStore vectorStore;
    private final List<ContentProcessor> processors;

    public EmbeddingManager(@Value("${app.content-dir}") String contentDir, VectorStore vectorStore, List<ContentProcessor> processors) {
        this.contentDir = contentDir;
        this.vectorStore = vectorStore;
        this.processors = processors;
    }

    @Job(name = "Manage embeddings for file %0")
    @Transactional
    public void manage(String relativePath, Status status) {
        deleteAllExistingEmbeddings(relativePath);

        if (status == UPDATED) processDocumentAndSaveEmbeddings(relativePath);
    }

    private void deleteAllExistingEmbeddings(String relativePath) {
        // TODO improve by running an sql delete query to remove the rows in one go.
        Expression filterExpression = new FilterExpressionBuilder().eq("fileRelativePath", relativePath).build();
        SearchRequest searchRequest = SearchRequest.builder().topK(Integer.MAX_VALUE).similarityThresholdAll().filterExpression(filterExpression).build();
        List<Document> matchingDocs = vectorStore.similaritySearch(searchRequest);
        if (!CollectionUtils.isEmpty(matchingDocs)) vectorStore.delete(matchingDocs.stream().map(Document::getId).toList());
    }

    private void processDocumentAndSaveEmbeddings(String relativePath) {
        Path path = Paths.get(contentDir, relativePath);
        ContentProcessor processor = findFirstMatchingProcessor(path);
        List<Document> docs = processor.process(path);
        docs.forEach(doc -> doc.getMetadata().put("fileRelativePath", relativePath));
        vectorStore.add(docs);
    }

    private ContentProcessor findFirstMatchingProcessor(Path absolutePath) {
        return processors.stream()
                .filter(p -> p.supports(absolutePath)).findFirst()
                .orElseThrow(() -> new JobRunrException("No ContentProcessor is able to process " + absolutePath, true));
    }
}
