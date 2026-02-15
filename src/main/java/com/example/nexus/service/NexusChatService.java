package com.example.nexus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class NexusChatService {
    private static final Logger logger = LoggerFactory.getLogger(NexusChatService.class);

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final ChatClient chatClient;

    @Value("${spring.ai.openai.fallback-models:gemini-2.5-pro,gemini-2.0-flash-exp}")
    private String[] fallbackModels;

    public NexusChatService(VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String chat(String message) {
        logger.info("Processing chat: [{}]", message);

        // 1. Check Redis Cache
        try {
            List<Document> similarDocuments = vectorStore.similaritySearch(
                    SearchRequest.builder().query(message).topK(3).similarityThreshold(0.4).build());

            for (Document doc : similarDocuments) {
                // The score is sometimes in metadata or needs to be calculated manually if not
                // provided by the store directly
                // For Redis SimpleVectorStore, we might not get score easily, but let's blindly
                Map<String, Object> metadata = doc.getMetadata();
                logger.info("Found candidate with metadata: {}", metadata);
                if (metadata != null && metadata.containsKey("response")) {
                    String cachedResponse = (String) metadata.get("response");
                    logger.info("Semantic cache hit!");
                    return "⚡ [Cache Hit] " + cachedResponse;
                }
            }
            logger.info("Cache miss. Checked {} candidates.", similarDocuments.size());
        } catch (Exception e) {
            logger.warn("Search failed: {}", e.getMessage());
        }

        // 2. Generate
        String response = generateWithPersistentRetries(message);

        // 3. Save (only if not a system error)
        if (response != null && !response.startsWith("[System]")) {
            try {
                Document newDoc = new Document(message, Map.of("response", response));
                vectorStore.add(List.of(newDoc));
                logger.info("Cached response.");
            } catch (Exception ex) {
                logger.error("Cache save failed: {}", ex.getMessage());
            }
        }

        return response;
    }

    private String generateWithPersistentRetries(String originalMessage) {
        String promptText = "You are a helpful shopping assistant for Nexus E-commerce. User Query: " + originalMessage;
        List<String> models = new ArrayList<>();
        models.add("gemini-2.5-flash");
        if (fallbackModels != null) {
            for (String m : fallbackModels)
                models.add(m);
        }

        for (String modelName : models) {
            try {
                logger.info("Attempting: {}", modelName);
                return chatClient.prompt().user(promptText)
                        .options(OpenAiChatOptions.builder().model(modelName).build())
                        .call().content();
            } catch (Exception e) {
                logger.error("Error calling model {}: {}", modelName, e.getMessage());
            }
        }

        // Final Mock Fallback for high-demand sessions
        String mockResponse = "[Mock Result] (Quota Exceeded) Your prompt was: '" + originalMessage
                + "'. I've cached this mock response!";
        try {
            Document mockDoc = new Document(originalMessage, Map.of("response", mockResponse));
            vectorStore.add(List.of(mockDoc));
            logger.info("Seeded mock to cache.");
        } catch (Exception ex) {
            logger.error("Mock seed failed: {}", ex.getMessage());
        }
        return mockResponse;
    }
}
