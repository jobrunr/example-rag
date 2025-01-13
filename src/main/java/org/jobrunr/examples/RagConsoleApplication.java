package org.jobrunr.examples;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class RagConsoleApplication implements CommandLineRunner {
    private final Scanner scanner = new Scanner(System.in);
    private final double similarityThreshold;
    private final ChatModel chatModel;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Advisor retrievalAugmentationAdvisor;

    public RagConsoleApplication(@Value("${app.similarity-threshold}") double similarityThreshold, ChatModel chatModel, VectorStore vectorStore) {
        this.similarityThreshold = similarityThreshold;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
        this.vectorStore = vectorStore;
        this.retrievalAugmentationAdvisor = createRetrievalAugmentationAdvisor();
    }

    public static void main(String[] args) {
        SpringApplication.run(RagConsoleApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Your RAG assistant! Ask your questions below.");

        while (true) {
            System.out.print("> ");
            String question = scanner.nextLine();

            if (question.isEmpty()) continue;

            String answer = chatClient.prompt()
                    .advisors(retrievalAugmentationAdvisor)
                    .user(question)
                    .call()
                    .content();

            System.out.println(answer);
        }
    }

    private Advisor createRetrievalAugmentationAdvisor() {
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder().topK(10).similarityThreshold(similarityThreshold).vectorStore(vectorStore).build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }

    private QueryTransformer createRewriteQueryTransformer() {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }
}
