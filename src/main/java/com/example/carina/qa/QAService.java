package com.example.carina.qa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.client.AiClient;
import org.springframework.ai.client.AiResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.prompt.Prompt;
import org.springframework.ai.prompt.SystemPromptTemplate;
import org.springframework.ai.prompt.messages.Message;
import org.springframework.ai.prompt.messages.UserMessage;
import org.springframework.ai.retriever.VectorStoreRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QAService {

    private static final Logger logger = LoggerFactory.getLogger(QAService.class);

    @Value("classpath:/prompts/system-qa.st")
    private Resource qaSystemPromptResource;

    @Value("classpath:/prompts/system-chatbot.st")
    private Resource chatbotSystemPromptResource;

    private final AiClient aiClient;

    private final VectorStoreRetriever vectorStoreRetriever;

    @Autowired
    public QAService(AiClient aiClient, VectorStoreRetriever vectorStoreRetriever) {
        this.aiClient = aiClient;
        this.vectorStoreRetriever = vectorStoreRetriever;
    }

    public String generate(String message, boolean stuffit) {
        Message systemMessage = getSystemMessage(message, stuffit);
        UserMessage userMessage = new UserMessage(message);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        logger.info("Asking AI model to reply to question.");
        AiResponse aiResponse = aiClient.generate(prompt);
        logger.info("AI responded.");
        return aiResponse.getGeneration().getText();
    }

    private Message getSystemMessage(String message, boolean stuffit) {
        if (stuffit) {
            logger.info("Retrieving relevant documents");
            List<Document> similarDocuments = vectorStoreRetriever.retrieve(message);
            logger.info(String.format("Found %s relevant documents.", similarDocuments.size()));
            String documents = similarDocuments.stream().map(entry -> entry.getContent()).collect(Collectors.joining("\n"));
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.qaSystemPromptResource);
            return systemPromptTemplate.createMessage(Map.of("documents", documents));
        } else {
            logger.info("Not stuffing the prompt, using generic prompt");
            return new SystemPromptTemplate(this.chatbotSystemPromptResource).createMessage();
        }
    }
}
